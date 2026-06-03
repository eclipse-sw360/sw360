/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cyclonedx;

import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CycloneDxBOMExporterTest {

    @Mock
    private ProjectDatabaseHandler projectDatabaseHandler;

    @Mock
    private ComponentDatabaseHandler componentDatabaseHandler;

    @Mock
    private PackageDatabaseHandler packageDatabaseHandler;

    private User user;
    private CycloneDxBOMExporter cycloneDxBOMExporter;

    private static final String PROJECT_ID = "project-001";
    private static final String COMP_ID    = "comp-001";
    private static final String RELEASE_ID = "release-001";

    @Before
    public void setUp() throws Exception {
        user = new User()
                .setEmail("test@sw360.org")
                .setUserGroup(UserGroup.ADMIN);

        cycloneDxBOMExporter = new CycloneDxBOMExporter(
                projectDatabaseHandler,
                componentDatabaseHandler,
                packageDatabaseHandler,
                user
        );
    }

    private Project buildProject(String releaseId) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setName("My Product");
        project.setVersion("1.0");
        project.setReleaseIdToUsage(Collections.singletonMap(
                releaseId,
                new ProjectReleaseRelationship(
                        ReleaseRelationship.CONTAINED,
                        MainlineState.MAINLINE)
        ));
        return project;
    }

    private MockedStatic<SW360Utils> allowExport() {
        MockedStatic<SW360Utils> sw360UtilsMock = mockStatic(SW360Utils.class);
        sw360UtilsMock.when(() -> SW360Utils.isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(any(), any())).thenReturn(true);
        sw360UtilsMock.when(() -> SW360Utils.readConfig(anyString(), anyBoolean())).thenReturn(false);
        sw360UtilsMock.when(SW360Utils::getSW360Version).thenReturn("test");
        sw360UtilsMock.when(() -> SW360Utils.readConfig(anyString(), any(UserGroup.class))).thenReturn(UserGroup.USER);
        return sw360UtilsMock;
    }

    @Test
    public void testExportSbom_shouldOnlyIncludeLinkedReleaseLicenses_notAllComponentLicenses() throws Exception {
        Component component = new Component();
        component.setId(COMP_ID);
        component.setName("jackson-databind");
        component.setMainLicenseIds(Sets.newHashSet("Non-profit", "Apache-2.0"));

        Release release = new Release();
        release.setId(RELEASE_ID);
        release.setName("jackson-databind");
        release.setVersion("5.4.0");
        release.setComponentId(COMP_ID);
        release.setMainLicenseIds(Sets.newHashSet("Apache-2.0"));

        Project project = buildProject(RELEASE_ID);

        when(projectDatabaseHandler.getProjectById(eq(PROJECT_ID), any(User.class))).thenReturn(project);
        when(componentDatabaseHandler.getReleasesByIds(anySet())).thenReturn(Collections.singletonList(release));
        when(componentDatabaseHandler.getComponentsByIds(anySet())).thenReturn(Collections.singletonList(component));

        try (MockedStatic<SW360Utils> sw360UtilsMock = allowExport()) {
            RequestSummary result = cycloneDxBOMExporter.exportSbom(PROJECT_ID, "json", false, user);

            assertNotNull(result);
            assertEquals(RequestStatus.SUCCESS, result.getRequestStatus());

            String sbomJson = result.getMessage();
            assertNotNull(sbomJson);
            assertTrue(sbomJson.contains("Apache-2.0"));
            assertFalse(sbomJson.contains("Non-profit"));
        }
    }

    @Test
    public void testExportSbom_shouldIncludeBothMainAndOtherLicensesOfRelease() throws Exception {
        Component component = new Component();
        component.setId(COMP_ID);
        component.setName("some-lib");
        component.setMainLicenseIds(Sets.newHashSet("GPL-2.0"));

        Release release = new Release();
        release.setId(RELEASE_ID);
        release.setName("some-lib");
        release.setVersion("3.0.0");
        release.setComponentId(COMP_ID);
        release.setMainLicenseIds(Sets.newHashSet("MIT"));
        release.setOtherLicenseIds(Sets.newHashSet("BSD-2-Clause"));

        Project project = buildProject(RELEASE_ID);

        when(projectDatabaseHandler.getProjectById(eq(PROJECT_ID), any(User.class))).thenReturn(project);
        when(componentDatabaseHandler.getReleasesByIds(anySet())).thenReturn(Collections.singletonList(release));
        when(componentDatabaseHandler.getComponentsByIds(anySet())).thenReturn(Collections.singletonList(component));

        try (MockedStatic<SW360Utils> sw360UtilsMock = allowExport()) {
            RequestSummary result = cycloneDxBOMExporter.exportSbom(PROJECT_ID, "json", false, user);

            assertEquals(RequestStatus.SUCCESS, result.getRequestStatus());

            String sbomJson = result.getMessage();
            assertTrue(sbomJson.contains("MIT"));
            assertTrue(sbomJson.contains("BSD-2-Clause"));
            assertFalse(sbomJson.contains("GPL-2.0"));
        }
    }

    @Test
    public void testExportSbom_multipleReleasesOnSameComponent_onlyLinkedOneAppears() throws Exception {
        Component component = new Component();
        component.setId(COMP_ID);
        component.setName("multi-release-lib");
        component.setMainLicenseIds(Sets.newHashSet("Non-profit", "AGPL-3.0", "Apache-2.0"));

        Release linkedRelease = new Release();
        linkedRelease.setId(RELEASE_ID);
        linkedRelease.setName("multi-release-lib");
        linkedRelease.setVersion("5.4.0");
        linkedRelease.setComponentId(COMP_ID);
        linkedRelease.setMainLicenseIds(Sets.newHashSet("Apache-2.0"));

        Project project = buildProject(RELEASE_ID);

        when(projectDatabaseHandler.getProjectById(eq(PROJECT_ID), any(User.class))).thenReturn(project);
        when(componentDatabaseHandler.getReleasesByIds(anySet())).thenReturn(Collections.singletonList(linkedRelease));
        when(componentDatabaseHandler.getComponentsByIds(anySet())).thenReturn(Collections.singletonList(component));

        try (MockedStatic<SW360Utils> sw360UtilsMock = allowExport()) {
            RequestSummary result = cycloneDxBOMExporter.exportSbom(PROJECT_ID, "json", false, user);

            assertEquals(RequestStatus.SUCCESS, result.getRequestStatus());

            String sbomJson = result.getMessage();
            assertTrue(sbomJson.contains("Apache-2.0"));
            assertFalse(sbomJson.contains("Non-profit"));
            assertFalse(sbomJson.contains("AGPL-3.0"));
        }
    }

    @Test
    public void testExportSbom_withNoLinkedReleases_shouldReturnFailedSanityCheck() throws Exception {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Empty Project");
        project.setReleaseIdToUsage(Collections.emptyMap());

        when(projectDatabaseHandler.getProjectById(eq(PROJECT_ID), any(User.class))).thenReturn(project);

        try (MockedStatic<SW360Utils> sw360UtilsMock = allowExport()) {
            RequestSummary result = cycloneDxBOMExporter.exportSbom(PROJECT_ID, "json", false, user);
            assertEquals(RequestStatus.FAILED_SANITY_CHECK, result.getRequestStatus());
        }
    }
}
