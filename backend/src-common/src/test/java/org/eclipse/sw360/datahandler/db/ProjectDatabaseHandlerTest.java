/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDatabaseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;
    private static final String changeLogsDbName = DatabaseSettingsTest.COUCH_CHANGELOGS;

    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");
    private static final User user2 = new User().setEmail("user2").setDepartment("AB CD FE");
    private static final User user3 = new User().setEmail("user3").setDepartment("AB CD EF");


    ProjectModerator moderator = Mockito.mock(ProjectModerator.class);
    ProjectDatabaseHandler handler;
    ComponentDatabaseHandler componentHandler;
    AttachmentDatabaseHandler attachmentDatabaseHandler;

    ReleaseRepository releaseRepository;

    VendorRepository vendorRepository;

    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);

        List<Project> projects = new ArrayList<>();

        Project p1 = new Project().setId("P1").setName("Project1").setBusinessUnit("AB CD EF").setCreatedBy("user1").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseRelationNetwork("[\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r1\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r2\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r3\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r4\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r5\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"releaseId\": \"r6\",\n" +
                    "    \"releaseLink\": [],\n" +
                    "    \"releaseRelationship\": \"CONTAINED\",\n" +
                    "    \"mainlineState\": \"OPEN\",\n" +
                    "    \"comment\": \"\",\n" +
                    "    \"createOn\": \"2022-09-12\",\n" +
                    "    \"createBy\": \"admin@sw360.org\"\n" +
                    "  }\n" +
                    "]");
        projects.add(p1);
        Project p2 = new Project().setId("P2").setName("Project2").setBusinessUnit("AB CD FE").setCreatedBy("user2").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseRelationNetwork("[\n" +
                        "  {\n" +
                        "    \"releaseId\": \"r1\",\n" +
                        "    \"releaseLink\": [],\n" +
                        "    \"releaseRelationship\": \"CONTAINED\",\n" +
                        "    \"mainlineState\": \"OPEN\",\n" +
                        "    \"comment\": \"\",\n" +
                        "    \"createOn\": \"2022-09-12\",\n" +
                        "    \"createBy\": \"admin@sw360.org\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"releaseId\": \"r2\",\n" +
                        "    \"releaseLink\": [],\n" +
                        "    \"releaseRelationship\": \"CONTAINED\",\n" +
                        "    \"mainlineState\": \"OPEN\",\n" +
                        "    \"comment\": \"\",\n" +
                        "    \"createOn\": \"2022-09-12\",\n" +
                        "    \"createBy\": \"admin@sw360.org\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"releaseId\": \"r3\",\n" +
                        "    \"releaseLink\": [],\n" +
                        "    \"releaseRelationship\": \"CONTAINED\",\n" +
                        "    \"mainlineState\": \"OPEN\",\n" +
                        "    \"comment\": \"\",\n" +
                        "    \"createOn\": \"2022-09-12\",\n" +
                        "    \"createBy\": \"admin@sw360.org\"\n" +
                        "  }\n" +
                        "]");

        projects.add(p2);
        projects.get(1).addToContributors("user1");
        projects.add(new Project().setId("P3").setName("Project3").setBusinessUnit("AB CD EF").setCreatedBy("user3").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS));
        Project p4 = new Project().setId("P4").setName("Project4").setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.PRIVATE)
                .setReleaseRelationNetwork("[\n" +
                        "  {\n" +
                        "    \"releaseId\": \"r1\",\n" +
                        "    \"releaseLink\": [],\n" +
                        "    \"releaseRelationship\": \"CONTAINED\",\n" +
                        "    \"mainlineState\": \"OPEN\",\n" +
                        "    \"comment\": \"\",\n" +
                        "    \"createOn\": \"2022-09-12\",\n" +
                        "    \"createBy\": \"admin@sw360.org\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"releaseId\": \"r2\",\n" +
                        "    \"releaseLink\": [\n" +
                        "      {\n" +
                        "        \"releaseId\": \"r3\",\n" +
                        "        \"releaseLink\": [],\n" +
                        "        \"releaseRelationship\": \"CONTAINED\",\n" +
                        "        \"mainlineState\": \"OPEN\",\n" +
                        "        \"comment\": \"\",\n" +
                        "        \"createOn\": \"2022-09-12\",\n" +
                        "        \"createBy\": \"admin@sw360.org\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"releaseRelationship\": \"CONTAINED\",\n" +
                        "    \"mainlineState\": \"OPEN\",\n" +
                        "    \"comment\": \"\",\n" +
                        "    \"createOn\": \"2022-09-12\",\n" +
                        "    \"createBy\": \"admin@sw360.org\"\n" +
                        "  }\n" +
                        "]")
                .setLinkedProjects(ImmutableMap.<String, ProjectProjectRelationship>builder().put("P5", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)).build());
        projects.add(p4);
        projects.add(new Project().setId("P5").setName("Project5").setBusinessUnit("AB CD EF").setCreatedBy("user1").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS));

        List<Release> releases = new ArrayList<>();
        releases.add(new Release().setId("r1").setComponentId("c1"));
        releases.add(new Release().setId("r2").setComponentId("c1"));
        releases.add(new Release().setId("r3").setComponentId("c1"));
        releases.add(new Release().setId("r4").setComponentId("c1"));
        releases.add(new Release().setId("r5").setComponentId("c1"));
        releases.add(new Release().setId("r6").setComponentId("c1"));

        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        // Prepare the database
        DatabaseConnectorCloudant databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Project project : projects) {
            databaseConnector.add(project);
        }

        for (Release r:releases) {
            databaseConnector.add(r);
        }

        databaseConnector.add(new Component("comp1").setId("c1"));

        componentHandler = new ComponentDatabaseHandler(DatabaseSettingsTest.getConfiguredHttpClient(), DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName);
        attachmentDatabaseHandler = new AttachmentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, attachmentsDbName);
        handler = new ProjectDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName, moderator, componentHandler, attachmentDatabaseHandler);

        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }



    @Test
    public void testUpdateProject2_1() throws Exception {
        Project project2 = handler.getProjectById("P2", user1);
        project2.setName("Project2new");

        Mockito.lenient().doReturn(RequestStatus.SENT_TO_MODERATOR).when(moderator).updateProject(project2, user1);

        RequestStatus status = handler.updateProject(project2, user1);

        // Now contributors can also change the project
        assertEquals(RequestStatus.SUCCESS, status);
    }


    @Test
    public void testDeleteProject1_3() throws Exception {
        when(moderator.deleteProject(any(Project.class), eq(user3))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P1", user3);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(4, handler.getMyProjectsSummary(user1.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user2.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user3.getEmail()).size());

        assertEquals(4, handler.getBUProjectsSummary(user1.getDepartment()).size());
        assertEquals(1, handler.getBUProjectsSummary(user2.getDepartment()).size());
        assertEquals(4, handler.getBUProjectsSummary(user3.getDepartment()).size());

        assertEquals(5, handler.getAccessibleProjectsSummary(user1).size());
        assertEquals(1, handler.getAccessibleProjectsSummary(user2).size());
        assertEquals(3, handler.getAccessibleProjectsSummary(user3).size());

        boolean deleted = (handler.getProjectById("P1", user1) == null);
        assertEquals(false, deleted);
    }

    @Test
    public void testDeleteProject2_1() throws Exception {
        when(moderator.deleteProject(any(Project.class), eq(user1))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P2", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(4, handler.getMyProjectsSummary(user1.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user2.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user3.getEmail()).size());

        assertEquals(4, handler.getBUProjectsSummary(user1.getDepartment()).size());
        assertEquals(1, handler.getBUProjectsSummary(user2.getDepartment()).size());
        assertEquals(4, handler.getBUProjectsSummary(user3.getDepartment()).size());

        assertEquals(5, handler.getAccessibleProjectsSummary(user1).size());
        assertEquals(1, handler.getAccessibleProjectsSummary(user2).size());
        assertEquals(3, handler.getAccessibleProjectsSummary(user3).size());

        boolean deleted = (handler.getProjectById("P2", user2) == null);
        assertEquals(false, deleted);
    }


    @Test
    public void testDeleteProject2_3() throws Exception {
        when(moderator.deleteProject(any(Project.class), eq(user3))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P2", user3);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(4, handler.getMyProjectsSummary(user1.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user2.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user3.getEmail()).size());

        assertEquals(4, handler.getBUProjectsSummary(user1.getDepartment()).size());
        assertEquals(1, handler.getBUProjectsSummary(user2.getDepartment()).size());
        assertEquals(4, handler.getBUProjectsSummary(user3.getDepartment()).size());

        assertEquals(5, handler.getAccessibleProjectsSummary(user1).size());
        assertEquals(1, handler.getAccessibleProjectsSummary(user2).size());
        assertEquals(3, handler.getAccessibleProjectsSummary(user3).size());

        boolean deleted = (handler.getProjectById("P2", user2) == null);
        assertEquals(false, deleted);
    }

    @Test
    public void testDeleteProject3_1() throws Exception {
        when(moderator.deleteProject(any(Project.class), eq(user1))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P3", user1);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(4, handler.getMyProjectsSummary(user1.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user2.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user3.getEmail()).size());

        assertEquals(4, handler.getBUProjectsSummary(user1.getDepartment()).size());
        assertEquals(1, handler.getBUProjectsSummary(user2.getDepartment()).size());
        assertEquals(4, handler.getBUProjectsSummary(user3.getDepartment()).size());

        assertEquals(5, handler.getAccessibleProjectsSummary(user1).size());
        assertEquals(1, handler.getAccessibleProjectsSummary(user2).size());
        assertEquals(3, handler.getAccessibleProjectsSummary(user3).size());

        boolean deleted = (handler.getProjectById("P3", user3) == null);
        assertEquals(false, deleted);
    }

    @Test
    public void testDeleteProject3_2() throws Exception {
        when(moderator.deleteProject(any(Project.class), eq(user2))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P3", user2);

        assertEquals(RequestStatus.SENT_TO_MODERATOR, status);

        assertEquals(4, handler.getMyProjectsSummary(user1.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user2.getEmail()).size());
        assertEquals(1, handler.getMyProjectsSummary(user3.getEmail()).size());

        assertEquals(4, handler.getBUProjectsSummary(user1.getDepartment()).size());
        assertEquals(1, handler.getBUProjectsSummary(user2.getDepartment()).size());
        assertEquals(4, handler.getBUProjectsSummary(user3.getDepartment()).size());

        assertEquals(5, handler.getAccessibleProjectsSummary(user1).size());
        assertEquals(1, handler.getAccessibleProjectsSummary(user2).size());
        assertEquals(3, handler.getAccessibleProjectsSummary(user3).size());

        boolean deleted = (handler.getProjectById("P3", user3) == null);
        assertEquals(false, deleted);
    }

    @Ignore("One is no longer able to create duplicate projects via the service, so if you want enable the test, you cannot create the duplicate project via addProject()")
    public void testGetDuplicateProjects() throws Exception {
        String originalProjectId = "P1";
        final Project tmp = handler.getProjectById(originalProjectId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        String newProjectId = handler.addProject(tmp, user1).getId();

        final Map<String, List<String>> duplicateProjects = handler.getDuplicateProjects();

        assertThat(duplicateProjects.size(), is(1));
        assertThat(duplicateProjects.get(printName(tmp)), containsInAnyOrder(newProjectId,originalProjectId));
    }

    public void testAddProjectWithDuplicateFails() throws Exception {
        // given:
        String originalProjectId = "P1";
        final Project tmp = handler.getProjectById(originalProjectId, user1);
        tmp.unsetId();
        tmp.unsetRevision();

        // when:
        AddDocumentRequestSummary addProjectResult = handler.addProject(tmp, user1);

        // then:
        assertThat(addProjectResult.getRequestStatus(), is(RequestStatus.DUPLICATE));
        assertThat(addProjectResult.getId(), is(nullValue()));
    }

    public void testUpdateProjectWithDuplicateFails() throws Exception {
        // given:
        String originalProjectId = "P1";
        String duplicateProjectId = "P2";
        final Project tmp = handler.getProjectById(originalProjectId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        tmp.setId(duplicateProjectId);

        // when:
        RequestStatus updateProjectResult = handler.updateProject(tmp, user1);

        // then:
        assertThat(updateProjectResult, is(RequestStatus.DUPLICATE));
    }

    @Test
    public void testSanityCheckFails() throws Exception {
        Project project = handler.getProjectById("P1", user1);
        project.setReleaseRelationNetwork("[]");
        RequestStatus status = handler.updateProject(project, user1);
        assertThat(status, is(RequestStatus.FAILED_SANITY_CHECK));
    }

    @Test
    public void testSanityCheckSucceeds() throws Exception {
        Project project = handler.getProjectById("P1", user1);
        project.setReleaseRelationNetwork(
                "[\n" +
                "  {\n" +
                "    \"releaseId\": \"r1\",\n" +
                "    \"releaseLink\": [],\n" +
                "    \"releaseRelationship\": \"CONTAINED\",\n" +
                "    \"mainlineState\": \"OPEN\",\n" +
                "    \"comment\": \"\",\n" +
                "    \"createOn\": \"2022-09-12\",\n" +
                "    \"createBy\": \"admin@sw360.org\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"releaseId\": \"r2\",\n" +
                "    \"releaseLink\": [],\n" +
                "    \"releaseRelationship\": \"CONTAINED\",\n" +
                "    \"mainlineState\": \"OPEN\",\n" +
                "    \"comment\": \"\",\n" +
                "    \"createOn\": \"2022-09-12\",\n" +
                "    \"createBy\": \"admin@sw360.org\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"releaseId\": \"r3\",\n" +
                "    \"releaseLink\": [],\n" +
                "    \"releaseRelationship\": \"CONTAINED\",\n" +
                "    \"mainlineState\": \"OPEN\",\n" +
                "    \"comment\": \"\",\n" +
                "    \"createOn\": \"2022-09-12\",\n" +
                "    \"createBy\": \"admin@sw360.org\"\n" +
                "  }\n" +
                "]");

        RequestStatus status = handler.updateProject(project, user1);
        assertThat(status, is(RequestStatus.SUCCESS));

        Project project2 = handler.getProjectById("P2", user1);
        project2.setReleaseRelationNetwork("[]");
        RequestStatus status2 = handler.updateProject(project2, user1);
        assertThat(status2, is(RequestStatus.SUCCESS));
    }

    @Test
    public void testReleaseIdToEmptyProjects() throws Exception {
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = handler.releaseIdToProjects(new Project().setId("p4"), user1);
        Set<String> releaseIds = releaseIdToProjects.keySet();
        assertTrue("Release IDs size", releaseIds.size() == 0);
    }

    @Test
    public void testGetLinkedProjectsOfProject() throws Exception {
        Project p = handler.getProjectById("P4", user1);

        List<ProjectLink> projectLinks = handler.getLinkedProjects(p, false, user1);
        assertThat(projectLinks.size(), is(1));
        assertThat(projectLinks.get(0).getSubprojects().size(), is(1));
        assertThat(projectLinks.get(0).getLinkedReleases().size(), is(2));
    }

    @Test
    public void testGetLinkedProjectsOfProjectForClonedProject() throws Exception {
        Project p = handler.getProjectById("P4", user1);
        Project clone = p.deepCopy();
        clone.unsetRevision();

        List<ProjectLink> projectLinks = handler.getLinkedProjects(clone, false, user1);
        assertThat(projectLinks.size(), is(1));
        assertThat(projectLinks.get(0).getSubprojects().size(), is(1));
        assertThat(projectLinks.get(0).getLinkedReleases().size(), is(2));
    }
}
