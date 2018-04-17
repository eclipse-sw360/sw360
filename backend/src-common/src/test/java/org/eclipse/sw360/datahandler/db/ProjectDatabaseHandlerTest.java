/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.db;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDatabaseHandlerTest {

    private static final String dbName = DatabaseSettings.COUCH_DB_DATABASE;
    private static final String attachmentDbName = DatabaseSettings.COUCH_DB_ATTACHMENTS;
    private static final String attachmentsDbName = DatabaseSettings.COUCH_DB_ATTACHMENTS;

    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");
    private static final User user2 = new User().setEmail("user2").setDepartment("AB CD FE");
    private static final User user3 = new User().setEmail("user3").setDepartment("AB CD EF");


    ProjectModerator moderator = Mockito.mock(ProjectModerator.class);
    ProjectDatabaseHandler handler;
    ComponentDatabaseHandler componentHandler;
    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);

        List<Project> projects = new ArrayList<>();

        Project p1 = new Project().setId("P1").setName("Project1").setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r4", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r5", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r6", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build());
        projects.add(p1);
        Project p2 = new Project().setId("P2").setName("Project2").setBusinessUnit("AB CD FE").setCreatedBy("user2")
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build());

        projects.add(p2);
        projects.get(1).addToContributors("user1");
        projects.add(new Project().setId("P3").setName("Project3").setBusinessUnit("AB CD EF").setCreatedBy("user3"));
        Project p4 = new Project().setId("P4").setName("Project4").setBusinessUnit("AB CD EF").setCreatedBy("user1")
                .setVisbility(Visibility.PRIVATE)
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build())
                .setLinkedProjects(ImmutableMap.<String, ProjectRelationship>builder().put("P5", ProjectRelationship.CONTAINED).build());
        projects.add(p4);
        projects.add(new Project().setId("P5").setName("Project5").setBusinessUnit("AB CD EF").setCreatedBy("user1"));

        List<Release> releases = new ArrayList<>();
        releases.add(new Release().setId("r1").setComponentId("c1"));
        releases.add(new Release().setId("r2").setComponentId("c1"));
        releases.add(new Release().setId("r3").setComponentId("c1"));
        releases.add(new Release().setId("r4").setComponentId("c1"));
        releases.add(new Release().setId("r5").setComponentId("c1"));
        releases.add(new Release().setId("r6").setComponentId("c1"));

        // Create the database
        TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);

        // Prepare the database
        DatabaseConnector databaseConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);
        for (Project project : projects) {
            databaseConnector.add(project);
        }

        for (Release r:releases) {
            databaseConnector.add(r);
        }

        databaseConnector.add(new Component("comp1").setId("c1"));

        componentHandler = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), dbName, attachmentsDbName);
        handler = new ProjectDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), dbName, attachmentDbName, moderator, componentHandler);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);
    }



    @Test
    public void testUpdateProject2_1() throws Exception {
        Project project2 = handler.getProjectById("P2", user1);
        project2.setName("Project2new");

        Mockito.doReturn(RequestStatus.SENT_TO_MODERATOR).when(moderator).updateProject(project2, user1);

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


    @Ignore("One is no longer able to create duplicate projects in the db")
    public void testDuplicateProjectIsFound() throws Exception {

        String originalProjectId = "P1";
        final Project tmp = handler.getProjectById(originalProjectId, user1);
        tmp.unsetId();
        tmp.unsetRevision();
        String newProjectId = handler.addProject(tmp, user1).getId();

        final Map<String, List<String>> duplicateProjects = handler.getDuplicateProjects();

        assertThat(duplicateProjects.size(), is(1));
        assertThat(duplicateProjects.get(printName(tmp)), containsInAnyOrder(newProjectId,originalProjectId));
    }

    @Test
    public void testSanityCheckFails() throws Exception {
        Project project = handler.getProjectById("P1", user1);
        project.setReleaseIdToUsage(Collections.emptyMap());
        RequestStatus status = handler.updateProject(project, user1);
        assertThat(status, is(RequestStatus.FAILED_SANITY_CHECK));
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
        assertThat(status, is(RequestStatus.SUCCESS));

        Project project2 = handler.getProjectById("P2", user1);
        project2.setReleaseIdToUsage(Collections.emptyMap());
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
    public void testReleaseIdToProjects() throws Exception {
        Project p1 = handler.getProjectById("P1", user1);
        p1.setLinkedProjects(ImmutableMap.<String, ProjectRelationship>builder().put("P2", ProjectRelationship.CONTAINED).build());
        handler.updateProject(p1, user1);
        Project p2 = handler.getProjectById("P2", user2);

        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = handler.releaseIdToProjects(p1, user1);

        Set<String> releaseIds = releaseIdToProjects.keySet();

        assertThat(releaseIds, containsInAnyOrder("r1", "r2","r3","r4","r5","r6"));
        assertThat(releaseIdToProjects.get("r1"), containsInAnyOrder(createTuple(p1),createTuple(p2)));
        assertThat(releaseIdToProjects.get("r2"), containsInAnyOrder(createTuple(p1),createTuple(p2)));
        assertThat(releaseIdToProjects.get("r3"), containsInAnyOrder(createTuple(p1),createTuple(p2)));
        assertThat(releaseIdToProjects.get("r4"), containsInAnyOrder(createTuple(p1)));
        assertThat(releaseIdToProjects.get("r5"), containsInAnyOrder(createTuple(p1)));
        assertThat(releaseIdToProjects.get("r6"), containsInAnyOrder(createTuple(p1)));

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
        clone.unsetId();
        clone.unsetRevision();

        List<ProjectLink> projectLinks = handler.getLinkedProjects(clone, false, user1);
        assertThat(projectLinks.size(), is(1));
        assertThat(projectLinks.get(0).getSubprojects().size(), is(1));
        assertThat(projectLinks.get(0).getLinkedReleases().size(), is(2));
    }

    private ProjectWithReleaseRelationTuple createTuple(Project p) {
        return new ProjectWithReleaseRelationTuple(p, newDefaultProjectReleaseRelationship());
    }

    private ProjectReleaseRelationship newDefaultProjectReleaseRelationship() {
        return new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE);
    }
}
