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

package org.eclipse.sw360.licenseinfo.parsers;

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.AdsInformation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ADSParserTest {
    private final AttachmentConnector connector = mock(AttachmentConnector.class);
    private final Attachment attachment = new Attachment().setFilename("example.json").setAttachmentContentId("id");
    private final AttachmentContent content = new AttachmentContent().setFilename("example.json");
    private final User user = new User();
    private final Object context = new Object();
    private final ADSParser parser = new ADSParser(connector, ignored -> content);

    @Test
    void shouldParseReferencesAndAssessment() throws Exception {
        String json = """
                {"candidate_release":{"release_id":"release-1","release_name":"Example","version":"1.0"},
                 "clearing_assessment":{"clearing_required":true,
                   "files_with_license_changes":[{"path":"src/a","change":"modified"}],
                   "deleted_files":["src/b"]}}
                """;
        when(connector.getAttachmentStream(content, user, context))
                .thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        AdsInformation actual = parser.getAdsInformation(attachment, user, context);

        assertEquals("release-1", actual.getCandidateRelease().getReleaseId());
        assertEquals("Example", actual.getCandidateRelease().getReleaseName());
        assertEquals("1.0", actual.getCandidateRelease().getVersion());
        assertEquals(true, actual.getClearingAssessment().isClearingRequired());
        assertEquals(1, actual.getClearingAssessment().getLicenseChangesCount());
        assertEquals(1, actual.getClearingAssessment().getDeletedFilesCount());
        assertEquals(List.of(Map.of("path", "src/a", "change", "modified")), actual.getLicenseChanges());
        assertEquals(List.of(Map.of("value", "src/b")), actual.getDeletedFiles());
    }

    @Test
    void shouldReportMalformedJsonAsBadRequest() throws Exception {
        when(connector.getAttachmentStream(content, user, context))
                .thenReturn(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)));

        SW360Exception exception = assertThrows(SW360Exception.class,
                () -> parser.getAdsInformation(attachment, user, context));
        assertEquals(400, exception.getErrorCode());
    }

    @Test
    void shouldRejectInvalidSectionValues() throws Exception {
        String json = "{\"files_with_license_changes\":[{\"path\":{\"invalid\":\"object\"}}]}";
        when(connector.getAttachmentStream(content, user, context))
                .thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThrows(IllegalArgumentException.class, () -> parser.getAdsInformation(attachment, user, context));
    }
}
