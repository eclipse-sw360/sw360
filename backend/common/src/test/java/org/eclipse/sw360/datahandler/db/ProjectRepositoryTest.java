/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.google.common.collect.ImmutableMap;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.junit.Assert.*;

/**
 * Test class for ProjectRepository, specifically testing the clearing state cache view.
 */
public class ProjectRepositoryTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private ProjectRepository repository;
    private DatabaseConnectorCloudant databaseConnector;

    @Before
    public void setUp() throws Exception {
        assertTestString(dbName);

        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        // Create database connector and repository
        databaseConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        repository = new ProjectRepository(databaseConnector);

        // Add test projects
        List<Project> projects = createTestProjects();
        for (Project project : projects) {
            databaseConnector.add(project);
        }
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    private List<Project> createTestProjects() {
        List<Project> projects = new ArrayList<>();

        // Project with all cache-relevant fields set
        Project p1 = new Project()
                .setId("P1")
                .setName("Project1")
                .setDescription("This is a long description that should NOT be in cache")
                .setBusinessUnit("AB CD EF")
                .setCreatedBy("user1@example.com")
                .setProjectResponsible("responsible1@example.com")
                .setLeadArchitect("architect1@example.com")
                .setModerators(new HashSet<>(Set.of("mod1@example.com", "mod2@example.com")))
                .setContributors(new HashSet<>(Set.of("contrib1@example.com")))
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .put("r2", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE))
                        .build())
                .setLinkedProjects(ImmutableMap.<String, ProjectProjectRelationship>builder()
                        .put("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED))
                        .build());
        projects.add(p1);

        // Project with minimal fields
        Project p2 = new Project()
                .setId("P2")
                .setName("Project2")
                .setVisbility(Visibility.EVERYONE)
                .setCreatedBy("user2@example.com");
        projects.add(p2);

        // Project with linked releases only
        Project p3 = new Project()
                .setId("P3")
                .setName("Project3")
                .setVisbility(Visibility.PRIVATE)
                .setCreatedBy("user3@example.com")
                .setReleaseIdToUsage(ImmutableMap.<String, ProjectReleaseRelationship>builder()
                        .put("r3", new ProjectReleaseRelationship(ReleaseRelationship.REFERRED, MainlineState.OPEN))
                        .build());
        projects.add(p3);

        return projects;
    }

    @Test
    public void testGetAllProjectsForClearingCache_ReturnsAllProjects() {
        List<Project> cachedProjects = repository.getAllProjectsForClearingCache();

        assertNotNull("Cached projects should not be null", cachedProjects);
        assertEquals("Should return all 3 projects", 3, cachedProjects.size());
    }

    @Test
    public void testGetAllProjectsForClearingCache_ContainsRequiredFields() {
        List<Project> cachedProjects = repository.getAllProjectsForClearingCache();

        // Find P1 which has all fields set
        Project p1 = cachedProjects.stream()
                .filter(p -> "P1".equals(p.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("Project P1 should be in cache", p1);

        // Verify cache-relevant fields are present
        assertEquals("P1", p1.getId());
        assertEquals("user1@example.com", p1.getCreatedBy());
        assertEquals("responsible1@example.com", p1.getProjectResponsible());
        assertEquals("architect1@example.com", p1.getLeadArchitect());
        assertEquals("AB CD EF", p1.getBusinessUnit());
        assertEquals(Visibility.BUISNESSUNIT_AND_MODERATORS, p1.getVisbility());

        // Verify collections
        assertNotNull("Moderators should not be null", p1.getModerators());
        assertTrue("Moderators should contain mod1", p1.getModerators().contains("mod1@example.com"));

        assertNotNull("Contributors should not be null", p1.getContributors());
        assertTrue("Contributors should contain contrib1", p1.getContributors().contains("contrib1@example.com"));

        // Verify linked projects map
        assertNotNull("LinkedProjects should not be null", p1.getLinkedProjects());
        assertTrue("LinkedProjects should contain P2", p1.getLinkedProjects().containsKey("P2"));

        // Verify release usage map
        assertNotNull("ReleaseIdToUsage should not be null", p1.getReleaseIdToUsage());
        assertEquals("Should have 2 linked releases", 2, p1.getReleaseIdToUsage().size());
    }

    @Test
    public void testGetAllProjectsForClearingCache_HandlesMinimalProject() {
        List<Project> cachedProjects = repository.getAllProjectsForClearingCache();

        // Find P2 which has minimal fields
        Project p2 = cachedProjects.stream()
                .filter(p -> "P2".equals(p.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("Project P2 should be in cache", p2);
        assertEquals("P2", p2.getId());
        assertEquals("user2@example.com", p2.getCreatedBy());
        assertEquals(Visibility.EVERYONE, p2.getVisbility());

        // Empty collections should be handled gracefully
        assertTrue("LinkedProjects should be empty or null",
                p2.getLinkedProjects() == null || p2.getLinkedProjects().isEmpty());
        assertTrue("ReleaseIdToUsage should be empty or null",
                p2.getReleaseIdToUsage() == null || p2.getReleaseIdToUsage().isEmpty());
    }

    @Test
    public void testGetAllProjectsForClearingCache_CanBeUsedForHierarchyTraversal() {
        List<Project> cachedProjects = repository.getAllProjectsForClearingCache();

        // Simulate what getRefreshedAllProjectsIdMap does
        java.util.Map<String, Project> projectMap = new java.util.HashMap<>();
        for (Project p : cachedProjects) {
            if (p.getId() != null) {
                projectMap.put(p.getId(), p);
            }
        }

        // Verify we can traverse hierarchy starting from P1
        Project p1 = projectMap.get("P1");
        assertNotNull("P1 should be in map", p1);

        if (p1.getLinkedProjects() != null) {
            for (String linkedProjectId : p1.getLinkedProjects().keySet()) {
                Project linkedProject = projectMap.get(linkedProjectId);
                assertNotNull("Linked project " + linkedProjectId + " should be in map", linkedProject);
            }
        }
    }
}
