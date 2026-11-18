/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.importer;

import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentImportConflictTest {
    private final ComponentService.Iface components = mock(ComponentService.Iface.class);
    private final AttachmentService.Iface attachments = mock(AttachmentService.Iface.class);
    private final User user = new User().setEmail("importer@example.org");
    private final ComponentAttachmentCSVRecord record = new ComponentAttachmentCSVRecord(
            "component", "release", "1.0", "new", "report.pdf", "SOURCE", "", "", user.getEmail());

    @BeforeEach
    void setUp() throws Exception {
        Release release = new Release().setName("release").setVersion("1.0").setAttachments(new HashSet<>(Set.of(
                new Attachment().setAttachmentContentId("old").setFilename("report.pdf"))));
        when(components.getComponentDetailedSummaryForExport()).thenReturn(List.of(
                new Component().setName("component").setReleases(List.of(release))));
        when(components.getUsedAttachmentContentIds()).thenReturn(Set.of());
        when(attachments.getAttachmentContent("new")).thenReturn(new AttachmentContent().setRemoteUrl("https://example.org/report"));
        when(attachments.getAttachmentContent("old")).thenReturn(new AttachmentContent().setRemoteUrl("https://example.org/report"));
    }

    @Test
    void shouldStopWithoutDeletingAttachmentStubs_whenReleaseBatchIsRejected() throws Exception {
        when(components.updateReleases(any(), any())).thenReturn(conflict());

        RequestSummary result = ComponentImportUtils.writeAttachmentsToDatabase(
                FluentIterable.from(List.of(record)), user, components, attachments);

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, result.getRequestStatus());
        assertEquals(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE, result.getMessage());
        verify(components, never()).updateComponents(any(), any());
        verify(attachments, never()).bulkDelete(any());
    }

    @Test
    void shouldPreserveConflictWithoutDeletingAttachmentStubs_whenComponentBatchIsRejected() throws Exception {
        when(components.updateReleases(any(), any())).thenReturn(
                new RequestSummary().setRequestStatus(RequestStatus.SUCCESS).setTotalElements(1).setTotalAffectedElements(1));
        when(components.updateComponents(any(), any())).thenReturn(conflict());
        ComponentAttachmentCSVRecord componentRecord = new ComponentAttachmentCSVRecord(
                "component", "", "", "component-new", "report.pdf", "SOURCE", "", "", user.getEmail());

        RequestSummary result = ComponentImportUtils.writeAttachmentsToDatabase(
                FluentIterable.from(List.of(record, componentRecord)), user, components, attachments);

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, result.getRequestStatus());
        assertTrue(result.getMessage().contains(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE));
        verify(attachments, never()).bulkDelete(any());
    }

    private RequestSummary conflict() {
        return new RequestSummary().setRequestStatus(RequestStatus.DUPLICATE_ATTACHMENT)
                .setTotalElements(1).setTotalAffectedElements(0)
                .setMessage(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE);
    }
}
