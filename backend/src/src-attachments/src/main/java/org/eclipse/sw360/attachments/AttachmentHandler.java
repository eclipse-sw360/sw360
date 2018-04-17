/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.attachments;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.attachments.db.AttachmentRepository;
import org.eclipse.sw360.attachments.db.AttachmentUsageRepository;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.DocumentOperationResult;

import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.validateAttachment;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class AttachmentHandler implements AttachmentService.Iface {

    public static final String ATTACHMENTS_FIELD_NAME = "attachments";
    private static final Logger log = Logger.getLogger(AttachmentHandler.class);

    private final AttachmentRepository attachmentRepository;
    private final AttachmentUsageRepository attachmentUsageRepository;
    private final AttachmentConnector attachmentConnector;


    public AttachmentHandler() throws MalformedURLException {
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS);
        attachmentConnector = new AttachmentConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS, durationOf(30, TimeUnit.SECONDS));
        attachmentRepository = new AttachmentRepository(databaseConnector);

        DatabaseConnector stdDatabaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(),
                DatabaseSettings.COUCH_DB_DATABASE);
        attachmentUsageRepository = new AttachmentUsageRepository(stdDatabaseConnector);
    }

    @Override
    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        validateAttachment(attachmentContent);
        assertIdUnset(attachmentContent.getId());

        attachmentRepository.add(attachmentContent);
        return attachmentContent;

    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        final List<DocumentOperationResult> documentOperationResults = attachmentRepository.executeBulk(attachmentContents);
        if (!documentOperationResults.isEmpty())
            log.error("Failed Attachment store results " + documentOperationResults);

        return FluentIterable.from(attachmentContents).filter(new Predicate<AttachmentContent>() {
            @Override
            public boolean apply(AttachmentContent input) {
                return input.isSetId();
            }
        }).toList();
    }

    @Override
    public AttachmentContent getAttachmentContent(String id) throws TException {
        assertNotEmpty(id);

        AttachmentContent attachment = attachmentRepository.get(id);
        assertNotNull(attachment, "Cannot find "+ id + " in database.");
        validateAttachment(attachment);

        return attachment;
    }

    @Override
    public void updateAttachmentContent(AttachmentContent attachment) throws TException {
        validateAttachment(attachment);
        attachmentConnector.updateAttachmentContent(attachment);
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) throws TException {
        final List<DocumentOperationResult> documentOperationResults = attachmentRepository.deleteIds(ids);
        return CommonUtils.getRequestSummary(ids, documentOperationResults);

    }

    @Override
    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        attachmentConnector.deleteAttachment(attachmentId);

        return RequestStatus.SUCCESS;
    }

    @Override
    public RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds) throws TException {
        assertUser(user);
        return attachmentRepository.vacuumAttachmentDB(user, usedIds);
    }

    @Override
    public String getSha1FromAttachmentContentId(String attachmentContentId){
        return attachmentConnector.getSha1FromAttachmentContentId(attachmentContentId);
    }

    @Override
    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertIdUnset(attachmentUsage.getId());

        attachmentUsageRepository.add(attachmentUsage);
        return attachmentUsage;
    }

    @Override
    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIdsUnset(attachmentUsages, attachmentUsage -> attachmentUsage.isSetId());

        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(attachmentUsages);
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be created: " + results);
        }
    }

    @Override
    public AttachmentUsage getAttachmentUsage(String id) throws TException {
        assertNotNull(id);
        assertNotEmpty(id);

        AttachmentUsage attachmentUsage = attachmentUsageRepository.get(id);
        assertNotNull(attachmentUsage);

        return attachmentUsage;
    }

    @Override
    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertId(attachmentUsage.getId());

        attachmentUsageRepository.update(attachmentUsage);
        return attachmentUsage;
    }

    @Override
    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIds(attachmentUsages, attachmentUsage -> attachmentUsage.isSetId());

        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(attachmentUsages);
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be updated: " + results);
        }
    }

    @Override
    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertId(attachmentUsage.getId());

        attachmentUsageRepository.remove(attachmentUsage);
    }

    @Override
    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIds(attachmentUsages, AttachmentUsage::isSetId);

        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(
                attachmentUsages.stream().map(attachmentUsage -> BulkDeleteDocument.of(attachmentUsage)).collect(Collectors.toList()));
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be deleted: " + results);
        }
    }

    private void deleteAttachmentUsagesByUsageDataTypes(Source usedBy, Set<UsageData._Fields> typesToReplace, boolean deleteWithEmptyType) throws TException {
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

    @Override
    public void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData) throws TException {
        assertNotNull(usedBy);
        assertTrue(usedBy.isSet());
        Set<UsageData._Fields> usageDataTypes = usageData == null ? Collections.emptySet() : ImmutableSet.of(usageData.getSetField());
        deleteAttachmentUsagesByUsageDataTypes(usedBy, usageDataTypes, usageData == null);
    }

    @Override
    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter) throws TException {
        assertNotNull(owner);
        assertTrue(owner.isSet());
        assertNotNull(attachmentContentId);
        assertNotEmpty(attachmentContentId);

        if (filter != null && filter.isSet()) {
            return attachmentUsageRepository.getUsageForAttachment(owner.getFieldValue().toString(), attachmentContentId,
                    filter.getSetField().toString());
        } else {
            return attachmentUsageRepository.getUsageForAttachment(owner.getFieldValue().toString(), attachmentContentId);
        }
    }

    @Override
    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) throws TException {
        assertNotNull(usedBy);
        assertTrue(usedBy.isSet());

        if (filter != null && filter.isSet()) {
            return attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString(), filter.getSetField().toString());
        } else {
            return attachmentUsageRepository.getUsedAttachments(usedBy.getFieldValue().toString());
        }
    }

    @Override
    public void replaceAttachmentUsages(Source usedBy, List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(usedBy);
        assertTrue(usedBy.isSet());
        assertNotNull(attachmentUsages);

        List<AttachmentUsage> usagesWithNonEmptyType = attachmentUsages.stream()
                .filter(AttachmentUsage::isSetUsageData)
                .collect(Collectors.toList());
        boolean hasEmptyUsageDataType = usagesWithNonEmptyType.size() != attachmentUsages.size();
        Set<UsageData._Fields> typesToReplace = usagesWithNonEmptyType.stream()
                .map(usage -> usage.getUsageData().getSetField())
                .collect(Collectors.toSet());

        // delete all the existing usages of the types given
        deleteAttachmentUsagesByUsageDataTypes(usedBy, typesToReplace, hasEmptyUsageDataType);

        // then save the new ones
        List<DocumentOperationResult> results = attachmentUsageRepository.executeBulk(attachmentUsages);
        if (!results.isEmpty()) {
            throw new SW360Exception("Some of the usage documents could not be updated: " + results);
        }
    }

    @Override
    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(Map<Source, Set<String>> attachments, UsageData filter)
            throws TException {
        Map<String, Source._Fields> idToType = Maps.newHashMap();
        Map<String, Set<String>> queryFor = attachments.entrySet().stream()
                .collect(Collectors.toMap(entry -> {
                    idToType.put(entry.getKey().getFieldValue().toString(), entry.getKey().getSetField());
                    return entry.getKey().getFieldValue().toString();
           }, entry -> entry.getValue()));

        Map<Map<String, String>, Integer> results;
        if (filter != null && filter.isSet()) {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, filter.getSetField().toString());
        } else {
            results = attachmentUsageRepository.getAttachmentUsageCount(queryFor, null);
        }

        return results.entrySet().stream().collect(Collectors.toMap(entry -> {
            Entry<String, String> key = entry.getKey().entrySet().iterator().next();
            return ImmutableMap.of(new Source(idToType.get(key.getKey()), key.getKey()), key.getValue());
        }, entry -> entry.getValue()));
    }
}
