/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.validateAttachment;

/**
 * Class for accessing the CouchDB database for attachment objects
 *
 * @author: alex.borodin@evosoft.com
 */
public class AttachmentDatabaseHandler {
    private final DatabaseConnectorCloudant db;
    private final AttachmentContentRepository attachmentContentRepository;
    private final AttachmentConnector attachmentConnector;
    private final AttachmentUsageRepository attachmentUsageRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentOwnerRepository attachmentOwnerRepository;
    private final ChecksumRepository checksumRepository;


    private static final Logger log = LogManager.getLogger(AttachmentDatabaseHandler.class);

    public AttachmentDatabaseHandler(Cloudant client, String dbName, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(client, attachmentDbName);
        attachmentConnector = new AttachmentConnector(client, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        attachmentContentRepository = new AttachmentContentRepository(db);
        attachmentUsageRepository = new AttachmentUsageRepository(new DatabaseConnectorCloudant(client, dbName));
        attachmentRepository = new AttachmentRepository(new DatabaseConnectorCloudant(client, dbName));
        attachmentOwnerRepository = new AttachmentOwnerRepository(new DatabaseConnectorCloudant(client, dbName));
        checksumRepository = new ChecksumRepository(new DatabaseConnectorCloudant(client, dbName));
    }

    public AttachmentConnector getAttachmentConnector(){
        return attachmentConnector;
    }

    public AttachmentContent add(AttachmentContent attachmentContent) throws SW360Exception {
        attachmentContentRepository.add(attachmentContent);
        return attachmentContent;
    }
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        final List<DocumentResult> documentOperationResults = attachmentContentRepository.executeBulk(attachmentContents);
        if (documentOperationResults.isEmpty())
            log.error("Failed Attachment store results " + documentOperationResults);

        return attachmentContents.stream().filter(AttachmentContent::isSetId).collect(Collectors.toList());
    }
    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        AttachmentContent attachment = attachmentContentRepository.get(id);
        assertNotNull(attachment, "Cannot find "+ id + " in database.");
        validateAttachment(attachment);

        return attachment;
    }
    public void updateAttachmentContent(AttachmentContent attachment) throws TException {
        attachmentConnector.updateAttachmentContent(attachment);
    }
    public RequestSummary bulkDelete(List<String> ids) {
        final List<DocumentResult> documentOperationResults = attachmentContentRepository.deleteIds(ids);
        return CommonUtils.getRequestSummary(ids, documentOperationResults);
    }
    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        attachmentConnector.deleteAttachment(attachmentId);

        return RequestStatus.SUCCESS;
    }
    public RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds) throws TException {
        return attachmentContentRepository.vacuumAttachmentDB(user, usedIds);
    }
    public String getSha1FromAttachmentContentId(String attachmentContentId){
        return attachmentConnector.getSha1FromAttachmentContentId(attachmentContentId);
    }

    public void deleteUsagesBy(Source usedBy) throws SW360Exception {
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString());
        if (!existingUsages.isEmpty()) {
            deleteAttachmentUsages(existingUsages);
        }
    }

    public void deleteUsagesBy(Source usedBy, Set<Source> owners) throws SW360Exception {
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString());
        List<AttachmentUsage> usagesToDelete = existingUsages.stream()
                .filter(usage -> owners.contains(usage.getOwner()))
                .collect(Collectors.toList());

        if (!usagesToDelete.isEmpty()) {
            deleteAttachmentUsages(usagesToDelete);
        }
    }


    public void deleteAttachmentUsagesByUsageDataTypes(Source usedBy, Set<UsageData._Fields> typesToReplace, boolean deleteWithEmptyType) throws TException {
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString());
        List<AttachmentUsage> usagesToDelete = existingUsages.stream().filter(usage -> {
            if (usage.isSetUsageData()) {
                return typesToReplace.contains(usage.getUsageData().getSetField());
            } else {
                return deleteWithEmptyType;
            }
        }).collect(Collectors.toList());

        if (!usagesToDelete.isEmpty()) {
            deleteAttachmentUsages(usagesToDelete);
        }
    }

    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) throws SW360Exception {
        attachmentUsageRepository.add(attachmentUsage);
        return attachmentUsage;
    }

    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<AttachmentUsage> sanitizedUsages = distinctAttachmentUsages(attachmentUsages);
        List<DocumentResult> results = attachmentUsageRepository.executeBulk(sanitizedUsages);
        results = results.stream().filter(res -> res.getError() != null || !res.isOk())
                .toList();
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be created: " + results);
        }
    }

    @VisibleForTesting
    protected List<AttachmentUsage> distinctAttachmentUsages(List<AttachmentUsage> attachmentUsages) {
        return attachmentUsages.stream()
                .filter(CommonUtils.distinctByKey(au -> ImmutableList.of(
                        au.getOwner(),
                        au.getUsedBy(),
                        au.getAttachmentContentId(),
                        au.isSetUsageData() ? au.getUsageData().getSetField() : "",
                        au.isSetUsageData() && au.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)
                                && au.getUsageData().getLicenseInfo().isSetProjectPath()
                                        ? au.getUsageData().getLicenseInfo().getProjectPath()
                                        : "",
                        au.isSetUsageData() && au.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)
                                ? au.getUsageData().getLicenseInfo().isIncludeConcludedLicense()
                                : false)))
                .collect(Collectors.toList());
    }

    public AttachmentUsage getAttachmentUsage(String id) throws TException {
        AttachmentUsage attachmentUsage = attachmentUsageRepository.get(id);
        assertNotNull(attachmentUsage);

        return attachmentUsage;

    }

    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) {
        attachmentUsageRepository.update(attachmentUsage);
        return attachmentUsage;
    }

    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<DocumentResult> results = attachmentUsageRepository.executeBulk(attachmentUsages);
        results = results.stream().filter(res -> res.getError() != null || !res.isOk())
                .toList();
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be updated: " + results);
        }
    }

    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) throws SW360Exception {
        attachmentUsageRepository.remove(attachmentUsage);
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws SW360Exception {
        List<DocumentResult> results = attachmentUsageRepository.deleteIds(
                attachmentUsages.stream().map(AttachmentUsage::getId).collect(Collectors.toList()));
        results = results.stream().filter(res -> res.getError() != null || !res.isOk())
                .toList();
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be deleted: " + results);
        }
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter) {
        if (filter != null && filter.isSet()) {
            return attachmentUsageRepository.getUsageForAttachment(owner.getFieldValue().toString(), attachmentContentId,
                    filter.getSetField().toString());
        } else {
            return attachmentUsageRepository.getUsageForAttachment(owner.getFieldValue().toString(), attachmentContentId);
        }
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, Set<String> attachmentContentIds, UsageData filter) {
        Map<String, Set<String>> attContentIdsByOwnerId = ImmutableMap.of(owner.getFieldValue().toString(), attachmentContentIds);

        if (filter != null && filter.isSet()) {
            return attachmentUsageRepository.getUsageForAttachments(attContentIdsByOwnerId, filter.getSetField().toString());
        } else {
            return attachmentUsageRepository.getUsageForAttachments(attContentIdsByOwnerId, null);
        }
    }

    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) {
        if (filter != null && filter.isSet()) {
            return attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString(), filter.getSetField().toString());
        } else {
            return attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString());
        }
    }

    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) {
        return attachmentUsageRepository.getUsedAttachmentById(attachmentId);
    }

    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(Map<Source, Set<String>> attachments, UsageData filter) {
        Map<String, Source._Fields> idToType = Maps.newHashMap();
        Map<String, Set<String>> queryFor = attachments.entrySet().stream()
                .collect(Collectors.toMap(entry -> {
                    String sourceId = entry.getKey().getFieldValue().toString();
                    idToType.put(sourceId, entry.getKey().getSetField());
                    return sourceId;
                }, Map.Entry::getValue));

        Map<Map<String, String>, Integer> results;
        if (filter != null && filter.isSet()) {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, filter.getSetField().toString());
        } else {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, null);
        }

        return results.entrySet().stream().collect(Collectors.toMap(entry -> {
            Map.Entry<String, String> key = entry.getKey().entrySet().iterator().next();
            return ImmutableMap.of(new Source(idToType.get(key.getKey()), key.getKey()), key.getValue());
        }, Map.Entry::getValue));
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) {
        return attachmentRepository.getAttachmentsByIds(ids);
    }
    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) {
        return attachmentRepository.getAttachmentsBySha1s(sha1s);
    }
    public List<Source> getAttachmentOwnersByIds(Set<String> ids) {
        return attachmentOwnerRepository.getOwnersByIds(ids);
    }
    
    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId) {
        return attachmentUsageRepository.getUsagesByReleaseId(releaseId);
    }
    public RequestStatus deleteOldAttachmentFromFileSystem() throws TException {
        return DatabaseHandlerUtil.deleteOldAttachmentFromFileSystem();
    }

    public AttachmentContent getAttachmentContentById(String attachmentContentId) {
        return attachmentContentRepository.get(attachmentContentId);
    }

    public List<Attachment> getAttachmentsByChecksum(String checksum, String checksumType) {
        return checksumRepository.getAttachmentsByChecksum(checksum, checksumType);
    }

    public List<Attachment> getAttachmentsByFossologyUploadId(String fossologyUploadId) {
        return checksumRepository.getAttachmentsByFossologyUploadIds(Set.of(fossologyUploadId));
    }

    public String getFossologyUploadIdByChecksum(String checksum, String checksumType) {
        return checksumRepository.getFossologyUploadIdByChecksum(checksum, checksumType);
    }

    public Map<String, String> getChecksumsByFossologyUploadId(String fossologyUploadId) {
        return checksumRepository.getChecksumsByFossologyUploadId(fossologyUploadId);
    }

    public void mapChecksumToFossologyUploadId(String checksum, String checksumType, String fossologyUploadId) {
        log.info("Mapping {} checksum {} to FOSSology upload ID {}", checksumType, checksum, fossologyUploadId);
        
        // Get all attachments with the given checksum across all entities
        List<Attachment> attachments = getAttachmentsByChecksum(checksum, checksumType);
        
        if (attachments.isEmpty()) {
            log.warn("No attachments found with {} checksum: {}", checksumType, checksum);
            return;
        }
        
        // For each attachment, finding its parent entity and updating it
        int updatedCount = 0;
        for (Attachment attachment : attachments) {
            try {
                // Find the owner/parent entities for this attachment
                List<Source> owners = attachmentOwnerRepository.getOwnersByIds(Set.of(attachment.getAttachmentContentId()));
                
                for (Source owner : owners) {
                    boolean updated = updateAttachmentInParentEntity(owner, attachment.getAttachmentContentId(), fossologyUploadId);
                    if (updated) {
                        updatedCount++;
                        log.debug("Updated attachment {} in {} {} with FOSSology upload ID {}", 
                            attachment.getAttachmentContentId(), owner.getSetField(), owner.getFieldValue(), fossologyUploadId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update attachment {} with FOSSology upload ID {}", 
                    attachment.getAttachmentContentId(), fossologyUploadId, e);
            }
        }
        
        log.info("Successfully mapped {} attachment instances with {} checksum {} to FOSSology upload ID {}", 
            updatedCount, checksumType, checksum, fossologyUploadId);
    }

    private boolean updateAttachmentInParentEntity(Source owner, String attachmentContentId, String fossologyUploadId) {
        try {
            String entityId = owner.getFieldValue().toString();
            Source._Fields entityType = owner.getSetField();
            
            switch (entityType) {
                case COMPONENT_ID:
                    return updateComponentAttachment(entityId, attachmentContentId, fossologyUploadId);
                case PROJECT_ID:
                    return updateProjectAttachment(entityId, attachmentContentId, fossologyUploadId);
                case RELEASE_ID:
                    return updateReleaseAttachment(entityId, attachmentContentId, fossologyUploadId);
                default:
                    log.warn("Unsupported entity type for attachment update: {}", entityType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error updating attachment in parent entity", e);
            return false;
        }
    }

    private boolean updateComponentAttachment(String componentId, String attachmentContentId, String fossologyUploadId) {
        
        attachmentConnector.setFossologyUploadIdForAttachment(attachmentContentId, fossologyUploadId);
        return true;
    }

    private boolean updateProjectAttachment(String projectId, String attachmentContentId, String fossologyUploadId) {
        
        attachmentConnector.setFossologyUploadIdForAttachment(attachmentContentId, fossologyUploadId);
        return true;
    }

    private boolean updateReleaseAttachment(String releaseId, String attachmentContentId, String fossologyUploadId) {
        
        attachmentConnector.setFossologyUploadIdForAttachment(attachmentContentId, fossologyUploadId);
        return true;
    }
}
