/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.attachments;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountEntry;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the attachments backend service.
 *
 * Callers use this instead of direct HTTP to {@code /attachments/api/attachments}.
 * Types are service-api POJOs. See {@link AttachmentServiceRestClient} and {@link AttachmentClients}.
 */
public interface AttachmentClient {

    AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent);

    List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents);

    AttachmentContent getAttachmentContent(String id);

    AttachmentContent getAttachmentContentById(String attachmentContentId);

    String getSha1FromAttachmentContentId(String attachmentContentId);

    RequestSummary bulkDelete(List<String> ids);

    RequestStatus deleteAttachmentContent(String attachmentId);

    RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds);

    AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage);

    void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages);

    AttachmentUsage getAttachmentUsage(String id);

    AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage);

    void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages);

    void replaceAttachmentUsages(Source usedBy, List<AttachmentUsage> attachmentUsages);

    void deleteAttachmentUsage(AttachmentUsage attachmentUsage);

    void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages);

    void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData);

    List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter);

    List<AttachmentUsage> getAttachmentsUsages(Source owner, Set<String> attachmentContentIds, UsageData filter);

    List<AttachmentUsageCountEntry> getAttachmentUsageCount(Map<Source, Set<String>> attachments, UsageData filter);

    List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter);

    List<AttachmentUsage> getUsedAttachmentsById(String attachmentId);

    List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId);

    List<Attachment> getAttachmentsByIds(Set<String> ids);

    List<Attachment> getAttachmentsBySha1s(Set<String> sha1s);

    List<Source> getAttachmentOwnersByIds(Set<String> ids);

    RequestStatus deleteOldAttachmentFromFileSystem();
}
