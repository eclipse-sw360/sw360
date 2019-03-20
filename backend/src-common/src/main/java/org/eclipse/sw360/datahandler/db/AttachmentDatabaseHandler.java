/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.DocumentOperationResult;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
    private final DatabaseConnector db;
    private final AttachmentContentRepository attachmentContentRepository;
    private final AttachmentConnector attachmentConnector;
    private final AttachmentUsageRepository attachmentUsageRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentOwnerRepository attachmentOwnerRepository;


    private static final Logger log = Logger.getLogger(AttachmentDatabaseHandler.class);

    public AttachmentDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnector(httpClient, attachmentDbName);
        attachmentConnector = new AttachmentConnector(httpClient, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        attachmentContentRepository = new AttachmentContentRepository(db);
        attachmentUsageRepository = new AttachmentUsageRepository(new DatabaseConnector(httpClient, dbName));
        attachmentRepository = new AttachmentRepository(new DatabaseConnector(httpClient, dbName));
        attachmentOwnerRepository = new AttachmentOwnerRepository(new DatabaseConnector(httpClient, dbName));
    }

    public AttachmentConnector getAttachmentConnector(){
        return attachmentConnector;
    }

    public AttachmentContent add(AttachmentContent attachmentContent){
        attachmentContentRepository.add(attachmentContent);
        return attachmentContent;
    }
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        final List<DocumentOperationResult> documentOperationResults = attachmentContentRepository.executeBulk(attachmentContents);
        if (!documentOperationResults.isEmpty())
            log.error("Failed Attachment store results " + documentOperationResults);

        return attachmentContents.stream().filter(AttachmentContent::isSetId).collect(Collectors.toList());
    }
    public AttachmentContent getAttachmentContent(String id) throws TException {
        AttachmentContent attachment = attachmentContentRepository.get(id);
        assertNotNull(attachment, "Cannot find "+ id + " in database.");
        validateAttachment(attachment);

        return attachment;
    }
    public void updateAttachmentContent(AttachmentContent attachment) throws TException {
        attachmentConnector.updateAttachmentContent(attachment);
    }
    public RequestSummary bulkDelete(List<String> ids) {
        final List<DocumentOperationResult> documentOperationResults = attachmentContentRepository.deleteIds(ids);
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

    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) {
        attachmentUsageRepository.add(attachmentUsage);
        return attachmentUsage;
    }

    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<AttachmentUsage> sanitizedUsages = distinctAttachmentUsages(attachmentUsages);
        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(sanitizedUsages);
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
                                        : "")))
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
        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(attachmentUsages);
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be updated: " + results);
        }
    }

    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) {
        attachmentUsageRepository.remove(attachmentUsage);
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws SW360Exception {
        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(
                attachmentUsages.stream().map(BulkDeleteDocument::of).collect(Collectors.toList()));
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
}
