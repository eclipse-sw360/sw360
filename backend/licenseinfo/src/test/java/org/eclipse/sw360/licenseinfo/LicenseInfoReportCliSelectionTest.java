/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.licenseinfo.outputGenerators.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LicenseInfoReportCliSelectionTest {
    @Test
    void reportUsesNonApprovedCliSelectedInAttachmentUsage() throws Exception {
        checkReportSelection(false);
    }

    @Test
    void reportSkipsAmbiguousUsageWithoutFallingBackToApprovedCli() throws Exception {
        checkReportSelection(true);
    }

    private void checkReportSelection(boolean ambiguous) throws Exception {
        AttachmentDatabaseHandler attachments = mock(AttachmentDatabaseHandler.class);
        LicenseInfoHandler handler = spy(new LicenseInfoHandler(attachments, null, null));
        User user = new User().setEmail("report@example.org");
        Attachment approved = cli("approved", CheckStatus.ACCEPTED);
        Attachment selected = cli("selected", CheckStatus.NOTCHECKED);
        Release release = new Release().setId("r1").setAttachments(Set.of(approved, selected));
        Project project = new Project().setId("p1").setName("Project")
                .setReleaseIdToUsage(Map.of("r1", new ProjectReleaseRelationship()));
        Map<String, Map<String, Boolean>> reportSelection = Map.of("r1", Map.of("approved", false, "selected", false));
        doReturn(Map.of(release, reportSelection.get("r1"))).when(handler).mapKeysToReleases(reportSelection, user);
        List<AttachmentUsage> usages = ambiguous
                ? List.of(usage("selected"), usage("approved")) : List.of(usage("selected"));
        when(attachments.getUsedAttachments(Source.projectId("p1"), null)).thenReturn(usages);

        for (Attachment attachment : List.of(approved, selected)) {
            String id = attachment.getAttachmentContentId();
            LicenseInfoParsingResult licenses = new LicenseInfoParsingResult().setAttachmentContentId(id)
                    .setRelease(release).setLicenseInfo(new LicenseInfo().setLicenseNamesWithTexts(Set.of(
                            new LicenseNameWithText().setLicenseName("MIT").setLicenseSpdxId("MIT"))));
            ObligationParsingResult obligations = new ObligationParsingResult().setAttachmentContentId(id)
                    .setObligationsAtProject(List.of(new ObligationAtProject().setTopic(id).setText("Text")
                            .setLicenseIDs(List.of("MIT")).setType("OBLIGATION")));
            doReturn(List.of(licenses)).when(handler).getLicenseInfoForAttachment(release, id, false, user);
            doReturn(List.of(obligations)).when(handler).getObligationsForAttachment(release, id, user);
        }
        JsonGenerator generator = mock(JsonGenerator.class);
        when(generator.getOutputFormatInfo()).thenReturn(new OutputFormatInfo());
        when(generator.generateOutputFile(anyCollection(), eq(project), anyCollection(), eq(user),
                anyMap(), anyMap(), eq("report"), eq(false))).thenAnswer(invocation -> {
                    Map<?, ?> statuses = invocation.getArgument(5);
                    if (ambiguous) {
                        assertTrue(statuses.isEmpty());
                    } else {
                        assertEquals(Set.of("selected"), statuses.keySet());
                    }
                    return "";
                });
        doReturn(generator).when(handler).getOutputGeneratorByClassnameAndVariant("JsonGenerator", OutputFormatVariant.REPORT);
        handler.getLicenseInfoFile(project, user, "JsonGenerator::REPORT", reportSelection, Map.of(), "", "report");
        verify(attachments).getUsedAttachments(Source.projectId("p1"), null);
        verify(generator).generateOutputFile(anyCollection(), eq(project), anyCollection(), eq(user),
                anyMap(), anyMap(), eq("report"), eq(false));
    }

    private Attachment cli(String id, CheckStatus status) {
        return new Attachment().setAttachmentContentId(id).setFilename(id + ".xml")
                .setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML).setCheckStatus(status);
    }

    private AttachmentUsage usage(String id) {
        return new AttachmentUsage(Source.releaseId("r1"), id, Source.projectId("p1"))
                .setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(Set.of())));
    }
}
