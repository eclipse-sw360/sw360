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
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDatabaseHandlerTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;
    private static final String changeLogsDbName = DatabaseSettingsTest.COUCH_DB_CHANGELOGS;

    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");
    private static final User user2 = new User().setEmail("user2").setDepartment("AB CD FE");
    private static final User user3 = new User().setEmail("user3").setDepartment("AB CD EF");


    ProjectModerator moderator = Mockito.mock(ProjectModerator.class);
    ProjectDatabaseHandler handler;
    ComponentDatabaseHandler componentHandler;
    AttachmentDatabaseHandler attachmentDatabaseHandler;
    PackageDatabaseHandler packageHandler;

    private DatabaseConnectorCloudant databaseConnector;

    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);

        List<Project> projects = new ArrayList<>();

        Project p1 = new Project().setId("P1").setName("Project1").setBusinessUnit("AB CD EF").setCreatedBy("user1").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r4", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r5", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r6", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build());
        projects.add(p1);
        Project p2 = new Project().setId("P2").setName("Project2").setBusinessUnit("AB CD FE").setCreatedBy("user2").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build());

        projects.add(p2);
        projects.get(1).addToContributors("user1");
        projects.add(new Project().setId("P3").setName("Project3").setBusinessUnit("AB CD EF").setCreatedBy("user3").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS));
        Project p4 = new Project().setId("P4").setName("Project4").setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.PRIVATE)
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build())
                .setLinkedProjects(ImmutableMap.<String, ProjectProjectRelationship>builder().put("P5", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)).build())
                .setReleaseRelationNetwork(
                     """
                         [
                             {
                                  "comment": "",
                                  "releaseLink":[],
                                  "createBy":"admin@sw360.org",
                                  "createOn":"2022-08-15",
                                  "mainlineState":"MAINLINE",
                                  "releaseId":"r1",
                                  "releaseRelationship":"CONTAINED"
                             },
                             {
                                  "comment": "",
                                  "releaseLink":[],
                                  "createBy":"admin@sw360.org",
                                  "createOn":"2022-08-15",
                                  "mainlineState":"MAINLINE",
                                  "releaseId":"r2",
                                  "releaseRelationship":"CONTAINED"
                             }
                         ],
                     """
                );
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
        databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Project project : projects) {
            databaseConnector.add(project);
        }

        for (Release r:releases) {
            databaseConnector.add(r);
        }

        databaseConnector.add(new Component("comp1").setId("c1"));

        componentHandler = new ComponentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName);
        attachmentDatabaseHandler = new AttachmentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, attachmentsDbName);
        packageHandler = new PackageDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName, attachmentDatabaseHandler, componentHandler);
        handler = new ProjectDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName, changeLogsDbName, attachmentsDbName, moderator, componentHandler, packageHandler, attachmentDatabaseHandler);
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
    public void testForceUpdateProject() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        Project project1 = handler.getProjectById("P1", user1);
        project1.setName("Project1new");

        Mockito.lenient().doReturn(RequestStatus.SENT_TO_MODERATOR).when(moderator).updateProject(project1, user2);

        RequestStatus status = handler.updateProject(project1, user2, true);

        // if force option is enabled, the project can be changed.
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
        assertFalse(deleted);
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
        assertFalse(deleted);
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
        assertFalse(deleted);
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
        assertFalse(deleted);
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
        assertFalse(deleted);
    }

    @Test
    public void testForceDeleteProject() throws Exception {
        if (!TestUtils.IS_FORCE_UPDATE_ENABLED) {
            return;
        }
        int expect = handler.getMyProjectsSummary(user1.getEmail()).size() - 1;
        lenient().when(moderator.deleteProject(any(Project.class), eq(user3))).thenReturn(RequestStatus.SENT_TO_MODERATOR);
        RequestStatus status = handler.deleteProject("P1", user3, true);

        assertEquals(RequestStatus.SUCCESS, status);
        // Project can be deleted by a non-owner as force update is enabled
        assertEquals(expect, handler.getMyProjectsSummary(user1.getEmail()).size());

    }

    @Ignore("One is no longer able to create duplicate projects via the service, so if you want enable the test, you cannot create the duplicate project via addProject()")
    public void testGetDuplicateProjects() throws Exception {
        String originalProjectId = "P1";
        final Project tmp = handler.getProjectById(originalProjectId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        String newProjectId = handler.addProject(tmp, user1).getId();

        final Map<String, List<String>> duplicateProjects = handler.getDuplicateProjects();

        Assert.assertEquals(1, duplicateProjects.size());
        Assert.assertTrue(containsInAnyOrder(newProjectId,originalProjectId).matches(duplicateProjects.get(printName(tmp))));
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
        Assert.assertEquals(RequestStatus.DUPLICATE, addProjectResult.getRequestStatus());
        Assert.assertNull(addProjectResult.getId());
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
        Assert.assertEquals(RequestStatus.DUPLICATE, updateProjectResult);
    }

    @Test
    public void testSanityCheckFails() throws Exception {
        Project project = handler.getProjectById("P1", user1);
        project.setReleaseIdToUsage(Collections.emptyMap());
        RequestStatus status = handler.updateProject(project, user1);
        Assert.assertEquals(RequestStatus.FAILED_SANITY_CHECK, status);
    }

    @Test
    public void testSanityCheckSucceeds() throws Exception {
        Project project = handler.getProjectById("P1", user1);
        project.setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                .build());

        RequestStatus status = handler.updateProject(project, user1);
        Assert.assertEquals(RequestStatus.SUCCESS, status);

        Project project2 = handler.getProjectById("P2", user1);
        project2.setReleaseIdToUsage(Collections.emptyMap());
        RequestStatus status2 = handler.updateProject(project2, user1);
        Assert.assertEquals(RequestStatus.SUCCESS, status2);
    }

    @Test
    public void testReleaseIdToEmptyProjects() throws Exception {
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = handler.releaseIdToProjects(new Project().setId("p4"), user1);
        Set<String> releaseIds = releaseIdToProjects.keySet();
        Assert.assertTrue("Release IDs size", releaseIds.isEmpty());
    }

    @Test
    public void testReleaseIdToProjects() throws Exception {
        Project p1 = handler.getProjectById("P1", user1);
        p1.setLinkedProjects(ImmutableMap.<String, ProjectProjectRelationship>builder().put("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)).build());
        handler.updateProject(p1, user1);
        Project p2 = handler.getProjectById("P2", user2);

        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = handler.releaseIdToProjects(p1, user1);

        Set<String> releaseIds = releaseIdToProjects.keySet();

        Assert.assertTrue(containsInAnyOrder("r1", "r2","r3","r4","r5","r6").matches(releaseIds));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1),createTuple(p2)).matches(releaseIdToProjects.get("r1")));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1),createTuple(p2)).matches(releaseIdToProjects.get("r2")));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1),createTuple(p2)).matches(releaseIdToProjects.get("r3")));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1)).matches(releaseIdToProjects.get("r4")));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1)).matches(releaseIdToProjects.get("r5")));
        Assert.assertTrue(containsInAnyOrder(createTuple(p1)).matches(releaseIdToProjects.get("r6")));

    }

    @Test
    public void testGetReleaseClearingStatuses() throws Exception {
        Project p1 = handler.getProjectById("P1", user1);
        p1.setLinkedProjects(ImmutableMap.of("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)));
        handler.updateProject(p1, user1);
        Project p2 = handler.getProjectById("P2", user1);

        List<ReleaseClearingStatusData> statuses = handler.getReleaseClearingStatuses("P1", user1);
        Map<String, ReleaseClearingStatusData> statusesById = new HashMap<>();
        for (ReleaseClearingStatusData status : statuses) {
            statusesById.put(status.getRelease().getId(), status);
        }

        Assert.assertEquals(6, statuses.size());
        Assert.assertTrue(containsInAnyOrder("r1", "r2", "r3", "r4", "r5", "r6").matches(statusesById.keySet()));
        Assert.assertTrue(containsInAnyOrder(printName(p1), printName(p2))
                .matches(Arrays.asList(statusesById.get("r1").getProjectNames().split(", "))));
        Assert.assertEquals("Mainline, Mainline", statusesById.get("r1").getMainlineStates());
        Assert.assertEquals(printName(p1), statusesById.get("r4").getProjectNames());
    }

    @Test
    public void testGetReleaseClearingStatusesWithAccessibility() throws Exception {
        List<ReleaseClearingStatusData> statuses = handler.getReleaseClearingStatusesWithAccessibility("P1", user1);

        Assert.assertEquals(6, statuses.size());
        for (ReleaseClearingStatusData status : statuses) {
            Assert.assertTrue(status.isAccessible());
        }
    }

    @Test
    public void testGetReleasesOfProjectDirectOnly() throws Exception {
        Set<String> releaseIds = handler.getReleasesIdsOfProject("P1", false, user1);
        Assert.assertTrue(containsInAnyOrder("r1", "r2", "r3", "r4", "r5", "r6").matches(releaseIds));
    }

    @Test
    public void testGetReleasesOfProjectTransitiveIncludesLinkedProjects() throws Exception {
        Project p5 = handler.getProjectById("P5", user1);
        p5.setLinkedProjects(ImmutableMap.of("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)));
        handler.updateProject(p5, user1);

        Set<String> transitiveReleaseIds = handler.getReleasesIdsOfProject("P5", true, user1);
        Set<String> directReleaseIds = handler.getReleasesIdsOfProject("P5", false, user1);

        Assert.assertTrue(containsInAnyOrder("r1", "r2", "r3").matches(transitiveReleaseIds));
        Assert.assertTrue(directReleaseIds.isEmpty());
    }

    @Test
    public void testGetReleasesOfProjectReturnsEmptyWhenNoReleasesAndNoLinks() throws Exception {
        Set<String> releaseIds = handler.getReleasesIdsOfProject("P3", true, user3);
        Assert.assertTrue(releaseIds.isEmpty());
    }

    @Test
    public void testGetReleasesOfProjectReturnsEmptyWhenOnlyLinkedProjectsWithoutReleases() throws Exception {
        Project p5 = handler.getProjectById("P5", user1);
        p5.setLinkedProjects(ImmutableMap.of("P3", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)));
        handler.updateProject(p5, user1);

        Set<String> releaseIds = handler.getReleasesIdsOfProject("P5", true, user1);
        Assert.assertTrue(releaseIds.isEmpty());
    }

    @Test
    public void testGetLinkedProjectsOfProject() throws Exception {
        Project p = handler.getProjectById("P4", user1);

        List<ProjectLink> projectLinks = handler.getLinkedProjects(p, false, user1);
        Assert.assertEquals(1, projectLinks.size());
        Assert.assertEquals(1, projectLinks.get(0).getSubprojects().size());
        Assert.assertEquals(2, projectLinks.get(0).getLinkedReleases().size());
    }

    @Test
    public void testGetLinkedProjectsOfProjectForClonedProject() throws Exception {
        Project p = handler.getProjectById("P4", user1);
        Project clone = p.deepCopy();
        clone.unsetRevision();

        List<ProjectLink> projectLinks = handler.getLinkedProjects(clone, false, user1);
        Assert.assertEquals(1, projectLinks.size());
        Assert.assertEquals(1, projectLinks.get(0).getSubprojects().size());
        Assert.assertEquals(2, projectLinks.get(0).getLinkedReleases().size());
    }

    @Test
    public void testGetReleasesForLicenseClearingNoRelease() throws Exception {
        // 1. Project with no release
        List<Release> releases = handler.getReleasesForLicenseClearing("P3", user3, false, null, null, null);
        Assert.assertTrue(releases.isEmpty());
    }

    @Test
    public void testGetReleasesForLicenseClearingTwoReleases() throws Exception {
        // 2. Project with 2 releases
        List<Release> releases = handler.getReleasesForLicenseClearing("P4", user1, false, null, null, null);
        Assert.assertEquals(2, releases.size());
        Set<String> releaseIds = releases.stream().map(Release::getId).collect(Collectors.toSet());
        Assert.assertTrue(containsInAnyOrder("r1", "r2").matches(releaseIds));
    }

    @Test
    public void testGetReleasesForLicenseClearingSubProjectBothHavingOneRelease() throws Exception {
        // 3. Project with 1 sub-project both having 1 release
        Project parent = new Project().setId("P_PARENT").setName("ParentProject")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.of("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)))
                .setLinkedProjects(ImmutableMap.of("P_CHILD", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)));
        Project child = new Project().setId("P_CHILD").setName("ChildProject")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.of("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)));

        databaseConnector.add(parent);
        databaseConnector.add(child);

        // Transitive: should return both r1 and r2
        List<Release> transitiveReleases = handler.getReleasesForLicenseClearing("P_PARENT", user1, true, null, null, null);
        Assert.assertEquals(2, transitiveReleases.size());
        Set<String> releaseIds = transitiveReleases.stream().map(Release::getId).collect(Collectors.toSet());
        Assert.assertTrue(containsInAnyOrder("r1", "r2").matches(releaseIds));

        // Non-transitive: should return only parent's r1
        List<Release> directReleases = handler.getReleasesForLicenseClearing("P_PARENT", user1, false, null, null, null);
        Assert.assertEquals(1, directReleases.size());
        Assert.assertEquals("r1", directReleases.getFirst().getId());
    }

    @Test
    public void testGetReleasesForLicenseClearingDifferentRelationshipsAndFilter() throws Exception {
        // 4. Project with 2 releases using different relationship, then filtering
        Project proj = new Project().setId("P_REL_FILTER").setName("ProjectRelFilter")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.of(
                        "r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE),
                        "r2", new ProjectReleaseRelationship(ReleaseRelationship.DYNAMICALLY_LINKED, MainlineState.MAINLINE)
                ));
        databaseConnector.add(proj);

        // No filter: should return both
        List<Release> allReleases = handler.getReleasesForLicenseClearing("P_REL_FILTER", user1, false, null, null, null);
        Assert.assertEquals(2, allReleases.size());

        // Filter for CONTAINED
        List<Release> contained = handler.getReleasesForLicenseClearing("P_REL_FILTER", user1, false, null, null, ReleaseRelationship.CONTAINED);
        Assert.assertEquals(1, contained.size());
        Assert.assertEquals("r1", contained.getFirst().getId());

        // Filter for DYNAMICALLY_LINKED
        List<Release> dynamic = handler.getReleasesForLicenseClearing("P_REL_FILTER", user1, false, null, null, ReleaseRelationship.DYNAMICALLY_LINKED);
        Assert.assertEquals(1, dynamic.size());
        Assert.assertEquals("r2", dynamic.getFirst().getId());

        // Filter for STANDALONE: 0 match
        List<Release> standalone = handler.getReleasesForLicenseClearing("P_REL_FILTER", user1, false, null, null, ReleaseRelationship.STANDALONE);
        Assert.assertTrue(standalone.isEmpty());
    }

    @Test
    public void testGetReleasesForLicenseClearingSubProjectsWithRelationshipFilter() throws Exception {
        // 5. Same with sub-projects
        Project parent = new Project().setId("P_PARENT_REL").setName("ParentRel")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.of(
                        "r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE),
                        "r2", new ProjectReleaseRelationship(ReleaseRelationship.DYNAMICALLY_LINKED, MainlineState.MAINLINE)
                ))
                .setLinkedProjects(ImmutableMap.of("P_CHILD_REL", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)));

        Project child = new Project().setId("P_CHILD_REL").setName("ChildRel")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.of(
                        "r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE),
                        "r4", new ProjectReleaseRelationship(ReleaseRelationship.DYNAMICALLY_LINKED, MainlineState.MAINLINE)
                ));

        databaseConnector.add(parent);
        databaseConnector.add(child);

        // Transitive without filter: r1, r2, r3, r4
        List<Release> allTransitive = handler.getReleasesForLicenseClearing("P_PARENT_REL", user1, true, null, null, null);
        Assert.assertEquals(4, allTransitive.size());

        // Transitive filtered by CONTAINED: r1, r3
        List<Release> containedTransitive = handler.getReleasesForLicenseClearing("P_PARENT_REL", user1, true, null, null, ReleaseRelationship.CONTAINED);
        Assert.assertEquals(2, containedTransitive.size());
        Set<String> containedIds = containedTransitive.stream().map(Release::getId).collect(Collectors.toSet());
        Assert.assertTrue(containsInAnyOrder("r1", "r3").matches(containedIds));

        // Transitive filtered by DYNAMICALLY_LINKED: r2, r4
        List<Release> dynamicTransitive = handler.getReleasesForLicenseClearing("P_PARENT_REL", user1, true, null, null, ReleaseRelationship.DYNAMICALLY_LINKED);
        Assert.assertEquals(2, dynamicTransitive.size());
        Set<String> dynamicIds = dynamicTransitive.stream().map(Release::getId).collect(Collectors.toSet());
        Assert.assertTrue(containsInAnyOrder("r2", "r4").matches(dynamicIds));
    }

    @Test
    public void testGetReleasesForLicenseClearingSubProjectPrivatePermission() throws Exception {
        // 6. Parent Project visibility to EVERYONE, sub-project visibility PRIVATE. Both have 1 release.
        // user3 (not user1/creator of child) queries parent: should only get parent's release.
        Project parent = new Project().setId("P_EVERYONE").setName("PublicParent")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.EVERYONE)
                .setReleaseIdToUsage(ImmutableMap.of("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)))
                .setLinkedProjects(ImmutableMap.of(
                        "P_PUBLIC_CHILD", new ProjectProjectRelationship(ProjectRelationship.CONTAINED),
                        "P_PRIVATE_CHILD", new ProjectProjectRelationship(ProjectRelationship.CONTAINED)
                ));

        Project publicChild = new Project().setId("P_PUBLIC_CHILD").setName("PublicChild")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.EVERYONE)
                .setReleaseIdToUsage(ImmutableMap.of("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)));

        Project privateChild = new Project().setId("P_PRIVATE_CHILD").setName("PrivateChild")
                .setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.PRIVATE)
                .setReleaseIdToUsage(ImmutableMap.of("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)));

        databaseConnector.add(parent);
        databaseConnector.add(publicChild);
        databaseConnector.add(privateChild);

        // user1 (creator of all) gets r1, r2, r3
        List<Release> creatorReleases = handler.getReleasesForLicenseClearing("P_EVERYONE", user1, true, null, null, null);
        Assert.assertEquals(3, creatorReleases.size());

        // user3 has access to parent (EVERYONE) and publicChild (EVERYONE) but NOT privateChild (PRIVATE to user1) -> gets r1 and r3
        List<Release> user3Releases = handler.getReleasesForLicenseClearing("P_EVERYONE", user3, true, null, null, null);
        Assert.assertEquals(2, user3Releases.size());
        Set<String> user3Ids = user3Releases.stream().map(Release::getId).collect(Collectors.toSet());
        Assert.assertTrue(containsInAnyOrder("r1", "r3").matches(user3Ids));
    }

    private ProjectWithReleaseRelationTuple createTuple(Project p) {
        return new ProjectWithReleaseRelationTuple(p, newDefaultProjectReleaseRelationship());
    }

    private ProjectReleaseRelationship newDefaultProjectReleaseRelationship() {
        return new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE);
    }
}
