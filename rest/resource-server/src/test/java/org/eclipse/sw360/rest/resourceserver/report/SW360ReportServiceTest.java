/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.report;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.attachment.SW360AttachmentBackendService;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SW360ReportServiceTest {

    @Mock
    private Sw360ProjectService projectService;

    @Mock
    private Sw360LicenseService licenseService;

    @Mock
    private Sw360ComponentService componentService;

    @Mock
    private Sw360AttachmentService attachmentService;

    @Mock
    private SW360AttachmentBackendService attachmentBackendService;

    @Mock
    private Sw360LicenseInfoService licenseInfoService;

    @Mock
    private org.eclipse.sw360.rest.resourceserver.license.LicenseServiceRestAdapter licenseClient;

    @Mock
    private org.eclipse.sw360.rest.resourceserver.component.ComponentServiceRestAdapter componentServiceRestAdapter;

    @Mock
    private org.eclipse.sw360.rest.resourceserver.project.ProjectServiceRestAdapter projectServiceRestAdapter;

    @InjectMocks
    private SW360ReportService sw360ReportService;

    private User testUser;
    private Project parentProject;
    private Project subProject;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUserGroup(UserGroup.USER);

        parentProject = new Project();
        parentProject.setId("parentId");
        parentProject.setName("Parent Project");
        parentProject.setProjectType(ProjectType.CUSTOMER);

        subProject = new Project();
        subProject.setId("subId");
        subProject.setName("Sub Project");
        Map<String, ProjectReleaseRelationship> subReleases = new HashMap<>();
        subReleases.put("release1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE));
        subProject.setReleaseIdToUsage(subReleases);

        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put("subId", new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        parentProject.setLinkedProjects(linkedProjects);
    }

    @Test
    public void should_fetch_attachment_usages_from_subprojects_when_withSubProject_is_true() throws TException {
        // Given: parent project with no direct releases, sub-project has release with CLI attachment
        given(projectService.getProjectForUserById(eq("parentId"), any())).willReturn(parentProject);

        // Mock sub-project attachment usage (has a CLI attached to release1)
        AttachmentUsage subUsage = new AttachmentUsage();
        subUsage.setOwner(Source.releaseId("release1"));
        subUsage.setAttachmentContentId("attachContent1");
        LicenseInfoUsage licInfoUsage = new LicenseInfoUsage();
        licInfoUsage.setExcludedLicenseIds(Collections.emptySet());
        licInfoUsage.setIncludeConcludedLicense(false);
        subUsage.setUsageData(UsageData.licenseInfo(licInfoUsage));

        // Parent has no attachment usages, sub-project has one
        given(attachmentService.getAttachmentUsages("parentId")).willReturn(Collections.emptyList());
        given(attachmentService.getAttachmentUsages("subId")).willReturn(List.of(subUsage));

        // Mock createLinkedProjects to return project links with sub-project's releases
        Attachment cliAttachment = new Attachment();
        cliAttachment.setAttachmentContentId("attachContent1");
        cliAttachment.setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML);
        cliAttachment.setFilename("test-cli.xml");

        ReleaseLink releaseLink = new ReleaseLink();
        releaseLink.setId("release1");
        releaseLink.setAttachments(List.of(cliAttachment));
        releaseLink.setReleaseRelationship(ReleaseRelationship.CONTAINED);

        ProjectLink subProjectLink = new ProjectLink();
        subProjectLink.setId("subId");
        subProjectLink.setLinkedReleases(List.of(releaseLink));

        ProjectLink parentProjectLink = new ProjectLink();
        parentProjectLink.setId("parentId");
        parentProjectLink.setLinkedReleases(Collections.emptyList());

        given(projectService.createLinkedProjects(any(), any(), anyBoolean(), eq(true), any()))
                .willReturn(List.of(parentProjectLink, subProjectLink));

        // Mock component service for release lookup
        Release release = new Release();
        release.setId("release1");
        release.setName("TestRelease");
        release.setVersion("1.0");
        given(componentService.getReleaseById(eq("release1"), any())).willReturn(release);

        // Mock license info parsing
        given(licenseInfoService.getLicenseInfoForAttachment(any(), any(), eq("attachContent1"), anyBoolean()))
                .willReturn(Collections.emptyList());

        // Mock final license info file generation
        LicenseInfoFile licenseInfoFile = new LicenseInfoFile();
        licenseInfoFile.setGeneratedOutput(ByteBuffer.wrap("test content".getBytes()));
        given(licenseInfoService.getLicenseInfoFile(any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .willReturn(licenseInfoFile);

        // When
        SW360ReportBean reportBean = new SW360ReportBean();
        reportBean.setWithSubProject(true);
        reportBean.setGeneratorClassName("DocxGenerator");
        reportBean.setVariant("DISCLOSURE");

        ByteBuffer result = sw360ReportService.getLicenseInfoBuffer(testUser, "parentId", reportBean);

        // Then
        assertNotNull(result, "Report buffer should not be null");
        assertTrue(result.remaining() > 0, "Report buffer should have content");

        // Verify that attachment usages were fetched for BOTH parent and sub-project
        verify(attachmentService, times(1)).getAttachmentUsages("parentId");
        verify(attachmentService, times(1)).getAttachmentUsages("subId");
    }
}
