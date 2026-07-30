/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.SW360ExceptionConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.attachments.AttachmentClient;
import org.eclipse.sw360.datahandler.attachments.AttachmentClients;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountEntry;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360AttachmentBackendService {

    @NonNull
    private final AttachmentTypeBridge attachmentTypeBridge;

    private AttachmentClient attachmentClient() {
        return AttachmentClients.get();
    }

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        var pojo = attachmentTypeBridge.toPojo(attachmentContent);
        AttachmentContent result = attachmentTypeBridge.toThrift(attachmentClient().makeAttachmentContent(pojo));
        if (result == null) {
            throw new TException("Failed to create attachment content");
        }
        return result;
    }

    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentContent> pojos = attachmentContents.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        return attachmentClient().makeAttachmentContents(pojos).stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        try {
            return attachmentTypeBridge.toThrift(attachmentClient().getAttachmentContent(id));
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
            throw SW360ExceptionConverter.toThrift(e);
        }
    }

    public AttachmentContent getAttachmentContentById(String attachmentContentId) throws TException {
        return attachmentTypeBridge.toThrift(attachmentClient().getAttachmentContentById(attachmentContentId));
    }

    public String getSha1FromAttachmentContentId(String attachmentContentId) throws TException {
        return attachmentClient().getSha1FromAttachmentContentId(attachmentContentId);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary bulkDelete(List<String> ids) throws TException {
        return attachmentTypeBridge.toThriftRequestSummary(attachmentClient().bulkDelete(ids));
    }

    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        return attachmentTypeBridge.toThriftRequestStatus(attachmentClient().deleteAttachmentContent(attachmentId));
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds)
            throws TException {
        return attachmentTypeBridge.toThriftRequestSummary(
                attachmentClient().vacuumAttachmentDB(UserConverter.fromThrift(user), usedIds));
    }

    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        return attachmentTypeBridge.toThrift(
                attachmentClient().makeAttachmentUsage(attachmentTypeBridge.toPojo(attachmentUsage)));
    }

    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        attachmentClient().makeAttachmentUsages(pojos);
    }

    public AttachmentUsage getAttachmentUsage(String id) throws TException {
        return attachmentTypeBridge.toThrift(attachmentClient().getAttachmentUsage(id));
    }

    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        return attachmentTypeBridge.toThrift(
                attachmentClient().updateAttachmentUsage(attachmentTypeBridge.toPojo(attachmentUsage)));
    }

    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        attachmentClient().updateAttachmentUsages(pojos);
    }

    public void replaceAttachmentUsages(Source usedBy, List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        attachmentClient().replaceAttachmentUsages(attachmentTypeBridge.toPojoSource(usedBy), pojos);
    }

    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        attachmentClient().deleteAttachmentUsage(attachmentTypeBridge.toPojo(attachmentUsage));
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        attachmentClient().deleteAttachmentUsages(pojos);
    }

    public void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData) throws TException {
        attachmentClient().deleteAttachmentUsagesByUsageDataType(
                attachmentTypeBridge.toPojoSource(usedBy), attachmentTypeBridge.toPojo(usageData));
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter)
            throws TException {
        return attachmentClient()
                .getAttachmentUsages(attachmentTypeBridge.toPojoSource(owner), attachmentContentId,
                        attachmentTypeBridge.toPojo(filter))
                .stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public List<AttachmentUsage> getAttachmentsUsages(Source owner, Set<String> attachmentContentIds, UsageData filter)
            throws TException {
        return attachmentClient()
                .getAttachmentsUsages(attachmentTypeBridge.toPojoSource(owner), attachmentContentIds,
                        attachmentTypeBridge.toPojo(filter))
                .stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(
            Map<Source, Set<String>> attachments, UsageData filter) throws TException {
        Map<org.eclipse.sw360.datahandler.services.common.Source, Set<String>> pojoAttachments = attachments
                .entrySet().stream()
                .collect(Collectors.toMap(e -> attachmentTypeBridge.toPojoSource(e.getKey()), Map.Entry::getValue));
        List<AttachmentUsageCountEntry> entries = attachmentClient().getAttachmentUsageCount(pojoAttachments,
                attachmentTypeBridge.toPojo(filter));

        Map<Map<Source, String>, Integer> result = new HashMap<>();
        for (AttachmentUsageCountEntry entry : entries) {
            Source thriftSource = attachmentTypeBridge.toThriftSource(entry.getOwner());
            Map<Source, String> key = Collections.singletonMap(thriftSource, entry.getAttachmentContentId());
            result.put(key, entry.getCount());
        }
        return result;
    }

    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) throws TException {
        return attachmentClient()
                .getUsedAttachments(attachmentTypeBridge.toPojoSource(usedBy), attachmentTypeBridge.toPojo(filter))
                .stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) throws TException {
        return attachmentClient().getUsedAttachmentsById(attachmentId).stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId) throws TException {
        return attachmentClient().getAttachmentUsagesByReleaseId(releaseId).stream()
                .map(attachmentTypeBridge::toThrift)
                .collect(Collectors.toList());
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) throws TException {
        return attachmentClient().getAttachmentsByIds(ids).stream()
                .map(attachmentTypeBridge::toThriftAttachment)
                .collect(Collectors.toList());
    }

    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) throws TException {
        return attachmentClient().getAttachmentsBySha1s(sha1s).stream()
                .map(attachmentTypeBridge::toThriftAttachment)
                .collect(Collectors.toList());
    }

    public List<Source> getAttachmentOwnersByIds(Set<String> ids) throws TException {
        return attachmentClient().getAttachmentOwnersByIds(ids).stream()
                .map(attachmentTypeBridge::toThriftSource)
                .collect(Collectors.toList());
    }

    public RequestStatus deleteOldAttachmentFromFileSystem() throws TException {
        return attachmentTypeBridge.toThriftRequestStatus(attachmentClient().deleteOldAttachmentFromFileSystem());
    }
}
