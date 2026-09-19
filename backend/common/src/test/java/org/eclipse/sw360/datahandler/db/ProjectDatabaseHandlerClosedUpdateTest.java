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

import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the update rules applied to projects with clearing state CLOSED.
 */
public class ProjectDatabaseHandlerClosedUpdateTest {

    private static final String MEMBER_EMAIL = "member@sw360.org";
    private static final String STRANGER_EMAIL = "stranger@sw360.org";

    private static User user(String email, UserGroup userGroup) {
        return new User().setEmail(email).setUserGroup(userGroup);
    }

    private static Project closedProject() {
        return new Project()
                .setId("P1")
                .setName("Project1")
                .setClearingState(ProjectClearingState.CLOSED)
                .setState(ProjectState.ACTIVE)
                .setCreatedBy(MEMBER_EMAIL);
    }

    @Test
    public void testOpenProjectIsAlwaysUpdatable() {
        Project actual = closedProject().setClearingState(ProjectClearingState.OPEN);
        Project updated = actual.deepCopy().setName("Project1new");

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(STRANGER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testClearingAdminMayChangeAnyFieldOfClosedProject() {
        Project actual = closedProject();
        Project updated = actual.deepCopy().setName("Project1new");

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(STRANGER_EMAIL, UserGroup.CLEARING_ADMIN), true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(STRANGER_EMAIL, UserGroup.ADMIN), true));
    }

    @Test
    public void testNonMemberMayNotUpdateClosedProject() {
        Project actual = closedProject();
        Project updated = actual.deepCopy().setState(ProjectState.PHASE_OUT);

        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(STRANGER_EMAIL, UserGroup.USER), true));
        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(STRANGER_EMAIL, UserGroup.USER), false));
    }

    @Test
    public void testMemberMayChangeAnyFieldWhenStrictModeIsDisabled() {
        Project actual = closedProject();
        Project updated = actual.deepCopy().setName("Project1new");

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), false));
    }

    @Test
    public void testMemberMayChangeAllowedFieldsInStrictMode() {
        Project actual = closedProject();
        User member = user(MEMBER_EMAIL, UserGroup.USER);

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setState(ProjectState.PHASE_OUT), member, true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setSecurityResponsibles(Collections.singleton(STRANGER_EMAIL)), member, true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setExternalIds(Collections.singletonMap("internal.id", "4711")), member, true));
    }

    @Test
    public void testMemberMayNotChangeOtherFieldsInStrictMode() {
        Project actual = closedProject();
        User member = user(MEMBER_EMAIL, UserGroup.USER);

        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setName("Project1new"), member, true));
        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setClearingState(ProjectClearingState.OPEN), member, true));
        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setDescription("new description"), member, true));
        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual,
                actual.deepCopy().setAdditionalData(Collections.singletonMap("key", "value")), member, true));
    }

    @Test
    public void testMemberMayNotChangeAttachmentsInStrictMode() {
        Project actual = closedProject();
        Project updated = actual.deepCopy();
        updated.addToAttachments(new org.eclipse.sw360.datahandler.thrift.attachments.Attachment()
                .setAttachmentContentId("A1").setFilename("file.txt"));

        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testServerManagedFieldsAreIgnoredInStrictMode() {
        Project actual = closedProject().setModifiedBy("someone@sw360.org").setModifiedOn("2026-01-01");
        Project updated = actual.deepCopy()
                .setModifiedBy(MEMBER_EMAIL)
                .setModifiedOn("2026-02-02")
                .setRevision("2")
                .setState(ProjectState.PHASE_OUT);

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testNullAndEmptyValuesAreTreatedAsEqual() {
        Project actual = closedProject();
        Project updated = actual.deepCopy()
                .setDescription("")
                .setModerators(Collections.emptySet())
                .setExternalUrls(Collections.emptyMap());

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testMembersOtherThanCreatorAreRecognised() {
        User moderator = user("moderator@sw360.org", UserGroup.USER);
        User contributor = user("contributor@sw360.org", UserGroup.USER);
        User leadArchitect = user("architect@sw360.org", UserGroup.USER);
        User projectResponsible = user("responsible@sw360.org", UserGroup.USER);

        Project actual = closedProject()
                .setModerators(Collections.singleton(moderator.getEmail()))
                .setContributors(Collections.singleton(contributor.getEmail()))
                .setLeadArchitect(leadArchitect.getEmail())
                .setProjectResponsible(projectResponsible.getEmail());
        Project updated = actual.deepCopy().setState(ProjectState.PHASE_OUT);

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated, moderator, true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated, contributor, true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated, leadArchitect, true));
        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated, projectResponsible, true));
    }

    @Test
    public void testNestedThriftObjectsWithIgnoredFieldsAreTreatedAsEqual() {
        Project actual = closedProject();
        Map<String, ProjectReleaseRelationship> actualReleases = new HashMap<>();
        actualReleases.put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2026-01-01"));
        actual.setReleaseIdToUsage(actualReleases);

        Map<String, ProjectPackageRelationship> actualPackages = new HashMap<>();
        actualPackages.put("pkg1", new ProjectPackageRelationship()
                .setComment("a package")
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2026-01-01"));
        actual.setPackageIds(actualPackages);

        // Updated project from API does not contain createdBy / createdOn on nested relationships
        Project updated = actual.deepCopy().setState(ProjectState.PHASE_OUT);
        Map<String, ProjectReleaseRelationship> updatedReleases = new HashMap<>();
        updatedReleases.put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE));
        updated.setReleaseIdToUsage(updatedReleases);

        Map<String, ProjectPackageRelationship> updatedPackages = new HashMap<>();
        updatedPackages.put("pkg1", new ProjectPackageRelationship()
                .setComment("a package"));
        updated.setPackageIds(updatedPackages);

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testNestedThriftObjectsWithActualFieldChangesAreDetected() {
        Project actual = closedProject();
        Map<String, ProjectReleaseRelationship> actualReleases = new HashMap<>();
        actualReleases.put("r1", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2026-01-01"));
        actual.setReleaseIdToUsage(actualReleases);

        // Member attempts to change releaseRelationship from CONTAINED to REFERRED on closed project
        Project updated = actual.deepCopy();
        Map<String, ProjectReleaseRelationship> updatedReleases = new HashMap<>();
        updatedReleases.put("r1", new ProjectReleaseRelationship(ReleaseRelationship.REFERRED, MainlineState.MAINLINE));
        updated.setReleaseIdToUsage(updatedReleases);

        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updated,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }

    @Test
    public void testNestedLinkedProjectsWithChangesAreDetected() {
        Project actual = closedProject();
        Map<String, ProjectProjectRelationship> actualLinked = new HashMap<>();
        actualLinked.put("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        actual.setLinkedProjects(actualLinked);

        // Matching linked project allowed
        Project updatedSame = actual.deepCopy().setState(ProjectState.PHASE_OUT);
        Map<String, ProjectProjectRelationship> sameLinked = new HashMap<>();
        sameLinked.put("P2", new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        updatedSame.setLinkedProjects(sameLinked);

        assertTrue(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updatedSame,
                user(MEMBER_EMAIL, UserGroup.USER), true));

        // Modified relationship blocked for member
        Project updatedDiff = actual.deepCopy();
        Map<String, ProjectProjectRelationship> diffLinked = new HashMap<>();
        diffLinked.put("P2", new ProjectProjectRelationship(ProjectRelationship.REFERRED));
        updatedDiff.setLinkedProjects(diffLinked);

        assertFalse(ProjectDatabaseHandler.isProjectUpdateAllowed(actual, updatedDiff,
                user(MEMBER_EMAIL, UserGroup.USER), true));
    }
}
