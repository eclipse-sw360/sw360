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

package org.eclipse.sw360.rest.resourceserver.release;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReleaseRequestMapperTest {
    private final Sw360AttachmentService attachments = mock(Sw360AttachmentService.class);
    private final ReleaseRequestMapper mapper = new ReleaseRequestMapper(attachments);

    @Test
    void shouldPreserveOldDownloadUrlAndAttachments() {
        List<Map<String, String>> inputAttachments = List.of(Map.of("attachmentContentId", "attachment-id"));
        Attachment attachment = new Attachment().setAttachmentContentId("attachment-id").setFilename("source.zip");
        when(attachments.getAttachmentsFromRequest(eq(inputAttachments), any())).thenReturn(Set.of(attachment));
        Map<String, Object> input = new HashMap<>(Map.of(
                "name", "Example", "version", "1.0", "downloadurl", "https://example.org/source",
                "attachments", inputAttachments, "comment", "review", "unknown", "ignored"));

        Release result = mapper.setBackwardCompatibleFieldsInRelease(input);

        assertEquals("Example", result.getName());
        assertEquals("1.0", result.getVersion());
        assertEquals("https://example.org/source", result.getSourceCodeDownloadurl());
        assertEquals(Set.of(attachment), result.getAttachments());
    }

    @Test
    void shouldPreferCurrentDownloadUrl() {
        Map<String, Object> input = new HashMap<>(Map.of(
                "downloadurl", "old", "sourceCodeDownloadurl", "current"));
        assertEquals("current", mapper.setBackwardCompatibleFieldsInRelease(input).getSourceCodeDownloadurl());
    }

    @Test
    void shouldPreserveBatchIdsAndRejectInvalidInput() {
        assertEquals(List.of("a", "b", "a"),
                mapper.extractReleaseBatchSummaryIds(Map.of("ids", List.of("a", "b", "a"))));
        assertEquals(List.of("a", "b"),
                new ArrayList<>(mapper.normalizeReleaseBatchSummaryIds(List.of("a", "b", "a"))));
        BadRequestClientException exception = assertThrows(BadRequestClientException.class,
                () -> mapper.extractReleaseBatchSummaryIds(Map.of("ids", Map.of("invalid", "value"))));
        assertEquals("The 'ids' field must be an array of release IDs.", exception.getMessage());
    }
}
