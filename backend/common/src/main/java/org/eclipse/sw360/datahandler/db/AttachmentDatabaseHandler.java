/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 *
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
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.attachments.UsageDataUnion;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.eclipse.sw360.datahandler.services.common.SourceUnion;
import org.eclipse.sw360.datahandler.services.users.User;

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


    private static final Logger log = LogManager.getLogger(AttachmentDatabaseHandler.class);

    public AttachmentDatabaseHandler(Cloudant client, String dbName, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(client, attachmentDbName);
        attachmentConnector = new AttachmentConnector(client, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        attachmentContentRepository = new AttachmentContentRepository(db);
        attachmentUsageRepository = new AttachmentUsageRepository(new DatabaseConnectorCloudant(client, dbName));
        attachmentRepository = new AttachmentRepository(new DatabaseConnectorCloudant(client, dbName));
        attachmentOwnerRepository = new AttachmentOwnerRepository(new DatabaseConnectorCloudant(client, dbName));
    }

    public AttachmentConnector getAttachmentConnector(){
        return attachmentConnector;
    }

    public AttachmentContent add(AttachmentContent attachmentContent) {
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
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(SourceUnion.idOf(usedBy));
        if (!existingUsages.isEmpty()) {
            deleteAttachmentUsages(existingUsages);
        }
    }

    public void deleteUsagesBy(Source usedBy, Set<Source> owners) throws SW360Exception {
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(SourceUnion.idOf(usedBy));
        List<AttachmentUsage> usagesToDelete = existingUsages.stream()
                .filter(usage -> owners.contains(usage.getOwner()))
                .collect(Collectors.toList());

        if (!usagesToDelete.isEmpty()) {
            deleteAttachmentUsages(usagesToDelete);
        }
    }


    public void deleteAttachmentUsagesByUsageDataTypes(Source usedBy, Set<String> typesToReplace, boolean deleteWithEmptyType) throws TException {
        List<AttachmentUsage> existingUsages = attachmentUsageRepository.getUsedAttachments(SourceUnion.idOf(usedBy));
        List<AttachmentUsage> usagesToDelete = existingUsages.stream().filter(usage -> {
            if (usage.getUsageData() != null) {
                return typesToReplace.contains(UsageDataUnion.storedTypeOf(usage.getUsageData()));
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
                        au.getUsageData() != null ? UsageDataUnion.storedTypeOf(au.getUsageData()) : "",
                        au.getUsageData() != null && au.getUsageData().getLicenseInfo() != null
                                && au.getUsageData().getLicenseInfo().getProjectPath() != null
                                        ? au.getUsageData().getLicenseInfo().getProjectPath()
                                        : "",
                        au.getUsageData() != null && au.getUsageData().getLicenseInfo() != null
                                ? !Boolean.FALSE.equals(
                                        au.getUsageData().getLicenseInfo().getIncludeConcludedLicense())
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
        if (filter != null && UsageDataUnion.storedTypeOf(filter) != null) {
            return attachmentUsageRepository.getUsageForAttachment(SourceUnion.idOf(owner), attachmentContentId,
                    UsageDataUnion.storedTypeOf(filter));
        } else {
            return attachmentUsageRepository.getUsageForAttachment(SourceUnion.idOf(owner), attachmentContentId);
        }
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, Set<String> attachmentContentIds, UsageData filter) {
        Map<String, Set<String>> attContentIdsByOwnerId = ImmutableMap.of(SourceUnion.idOf(owner), attachmentContentIds);

        if (filter != null && UsageDataUnion.storedTypeOf(filter) != null) {
            return attachmentUsageRepository.getUsageForAttachments(attContentIdsByOwnerId, UsageDataUnion.storedTypeOf(filter));
        } else {
            return attachmentUsageRepository.getUsageForAttachments(attContentIdsByOwnerId, null);
        }
    }

    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) {
        if (filter != null && UsageDataUnion.storedTypeOf(filter) != null) {
            return attachmentUsageRepository.getUsedAttachments(SourceUnion.idOf(usedBy), UsageDataUnion.storedTypeOf(filter));
        } else {
            return attachmentUsageRepository.getUsedAttachments(SourceUnion.idOf(usedBy));
        }
    }

    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) {
        return attachmentUsageRepository.getUsedAttachmentById(attachmentId);
    }

    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(Map<Source, Set<String>> attachments, UsageData filter) {
        Map<String, Source> idToSource = Maps.newHashMap();
        Map<String, Set<String>> queryFor = attachments.entrySet().stream()
                .collect(Collectors.toMap(entry -> {
                    String sourceId = SourceUnion.idOf(entry.getKey());
                    idToSource.put(sourceId, entry.getKey());
                    return sourceId;
                }, Map.Entry::getValue));

        Map<Map<String, String>, Integer> results;
        if (filter != null && UsageDataUnion.storedTypeOf(filter) != null) {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, UsageDataUnion.storedTypeOf(filter));
        } else {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, null);
        }

        return results.entrySet().stream().collect(Collectors.toMap(entry -> {
            Map.Entry<String, String> key = entry.getKey().entrySet().iterator().next();
            return ImmutableMap.of(idToSource.get(key.getKey()), key.getValue());
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
}
