/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.attachments;

import com.google.common.collect.ImmutableSet;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.validateAttachment;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@Service
public class AttachmentHandler {

    private final AttachmentDatabaseHandler handler;

    public AttachmentHandler() throws MalformedURLException {
        handler = new AttachmentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    public AttachmentHandler(Cloudant client, String dbName, String attachmentDbName) throws MalformedURLException {
        handler = new AttachmentDatabaseHandler(client, dbName, attachmentDbName);
    }

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        validateAttachment(attachmentContent);
        assertIdUnset(attachmentContent.getId());
        return handler.add(attachmentContent);
    }

    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        for (AttachmentContent attachmentContent : attachmentContents) {
            validateAttachment(attachmentContent);
            assertIdUnset(attachmentContent.getId());
        }
        return handler.makeAttachmentContents(attachmentContents);
    }

    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        assertNotEmpty(id);
        return handler.getAttachmentContent(id);
    }

    public RequestSummary bulkDelete(List<String> ids) throws TException {
        assertNotNull(ids);
        return handler.bulkDelete(ids);
    }

    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        return handler.deleteAttachmentContent(attachmentId);
    }

    public RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds) throws TException {
        assertUser(user);
        return handler.vacuumAttachmentDB(user, usedIds);
    }

    public String getSha1FromAttachmentContentId(String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertNotEmpty(attachmentContentId);
        return handler.getSha1FromAttachmentContentId(attachmentContentId);
    }

    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertIdUnset(attachmentUsage.getId());

        return handler.makeAttachmentUsage(attachmentUsage);
    }

    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIdsUnset(attachmentUsages, AttachmentUsage::isSetId);

        handler.makeAttachmentUsages(attachmentUsages);
    }

    public AttachmentUsage getAttachmentUsage(String id) throws TException {
        assertNotNull(id);
        assertNotEmpty(id);

        return handler.getAttachmentUsage(id);
    }

    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertId(attachmentUsage.getId());

        return handler.updateAttachmentUsage(attachmentUsage);
    }

    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIds(attachmentUsages, AttachmentUsage::isSetId);

        handler.updateAttachmentUsages(attachmentUsages);
    }

    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        assertNotNull(attachmentUsage);
        assertId(attachmentUsage.getId());

        handler.deleteAttachmentUsage(attachmentUsage);
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        assertNotNull(attachmentUsages);
        assertNotEmpty(attachmentUsages);
        assertIds(attachmentUsages, AttachmentUsage::isSetId);

        handler.deleteAttachmentUsages(attachmentUsages);
    }

    public void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData) throws TException {
        assertNotNull(usedBy);
        assertTrue(usedBy.isSet());
        Set<UsageData._Fields> usageDataTypes = usageData == null ? Collections.emptySet() : ImmutableSet.of(usageData.getSetField());
        handler.deleteAttachmentUsagesByUsageDataTypes(usedBy, usageDataTypes, usageData == null);
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter) throws TException {
        return getAttachmentsUsages(owner, Collections.singleton(attachmentContentId), filter);
    }

    public List<AttachmentUsage> getAttachmentsUsages(Source owner, Set<String> attachmentContentIds, UsageData filter) throws TException {
        assertNotNull(owner);
        assertTrue(owner.isSet());
        assertNotNull(attachmentContentIds);
        if (attachmentContentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return handler.getAttachmentUsages(owner, attachmentContentIds, filter);
    }

    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) throws TException {
        assertNotNull(usedBy);
        assertTrue(usedBy.isSet());

        return handler.getUsedAttachments(usedBy, filter);
    }

    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) throws TException {
        assertNotNull(attachmentId);
        return handler.getUsedAttachmentsById(attachmentId);
    }

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
        handler.deleteAttachmentUsagesByUsageDataTypes(usedBy, typesToReplace, hasEmptyUsageDataType);

        // then save the new ones
        handler.makeAttachmentUsages(attachmentUsages);
    }

    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(Map<Source, Set<String>> attachments, UsageData filter)
            throws TException {
        assertNotNull(attachments);
        return handler.getAttachmentUsageCount(attachments, filter);
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getAttachmentsByIds(ids);
    }
    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) throws TException {
        assertNotEmpty(sha1s);
        return handler.getAttachmentsBySha1s(sha1s);
    }
    public List<Source> getAttachmentOwnersByIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getAttachmentOwnersByIds(ids);
    }

    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId) throws TException {
        assertNotNull(releaseId);
        return handler.getAttachmentUsagesByReleaseId(releaseId);
    }

    public RequestStatus deleteOldAttachmentFromFileSystem() throws TException {
        return handler.deleteOldAttachmentFromFileSystem();
    }

    public AttachmentContent getAttachmentContentById(String attachmentContentId) throws TException {
        assertNotEmpty(attachmentContentId);
        assertNotNull(attachmentContentId);
        return handler.getAttachmentContentById(attachmentContentId);
    }
}
