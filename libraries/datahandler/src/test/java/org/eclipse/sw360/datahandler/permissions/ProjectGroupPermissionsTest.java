/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectGroupPermissionsTest {
    private static final String BU = "DE PT BU";
    private static final String OTHER_BU = "OT HER BU";
    private static final String EMAIL = "clearing-admin@example.org";

    private static User user(String department, UserGroup role) {
        return new User(EMAIL, department).setUserGroup(role);
    }

    private static Project project(ProjectClearingState state) {
        return new Project().setBusinessUnit(BU).setCreatedBy("creator@example.org")
                .setVisbility(Visibility.EVERYONE).setClearingState(state);
    }

    private static void assertDirectWrite(User user, boolean expected) {
        for (ProjectClearingState state : ProjectClearingState.values()) {
            ProjectPermissions permissions = new ProjectPermissions(project(state), user);
            assertEquals(expected, permissions.isActionAllowed(RequestedAction.WRITE), state.name());
            assertEquals(expected, permissions.isActionAllowed(RequestedAction.ATTACHMENTS), state.name());
            assertFalse(permissions.isActionAllowed(RequestedAction.WRITE_ECC));
        }
    }

    @Test
    void shouldAllowPrimaryClearingAdminWithoutProjectMembership() {
        assertDirectWrite(user(BU + " PRIMARY", UserGroup.CLEARING_ADMIN), true);
    }

    @Test
    void shouldAllowMatchingSecondaryRoleWithoutProjectMembership() {
        assertDirectWrite(user(OTHER_BU, UserGroup.USER)
                .setSecondaryDepartmentsAndRoles(Map.of(BU + " SECONDARY", Set.of(UserGroup.CLEARING_ADMIN))), true);
    }

    @Test
    void shouldEvaluateAllDepartmentsNormalizingToTheSameBusinessUnit() {
        User user = user(BU + " PRIMARY", UserGroup.USER)
                .setSecondaryDepartmentsAndRoles(Map.of(
                        BU + " SECONDARY", Set.of(UserGroup.CLEARING_ADMIN),
                        BU + " OTHER", Set.of(UserGroup.USER)));
        assertEquals(Set.of(BU + " PRIMARY", BU + " SECONDARY", BU + " OTHER"),
                new ProjectPermissions(project(ProjectClearingState.OPEN), user).getUserEquivalentOwnerGroup());
        assertDirectWrite(user, true);
    }

    @Test
    void shouldCheckSecondaryRoleEvenWhenItsKeyEqualsPrimaryDepartment() {
        assertDirectWrite(user(BU, UserGroup.USER)
                .setSecondaryDepartmentsAndRoles(Map.of(BU, Set.of(UserGroup.CLEARING_ADMIN))), true);
    }

    @Test
    void shouldNotUseClearingAdminRoleFromAnUnrelatedGroup() {
        assertDirectWrite(user(OTHER_BU, UserGroup.CLEARING_ADMIN)
                .setSecondaryDepartmentsAndRoles(Map.of(BU, Set.of(UserGroup.USER))), false);
        assertDirectWrite(user(BU, UserGroup.USER)
                .setSecondaryDepartmentsAndRoles(Map.of(OTHER_BU, Set.of(UserGroup.CLEARING_ADMIN))), false);
    }

    @Test
    void shouldHandleMissingRolesWithoutGrantingWrite() {
        assertDirectWrite(user(BU, UserGroup.USER), false);
        Map<String, Set<UserGroup>> secondary = new HashMap<>();
        secondary.put(BU + " EMPTY", Set.of());
        secondary.put(BU + " NULL", null);
        assertDirectWrite(user(BU, UserGroup.USER).setSecondaryDepartmentsAndRoles(secondary), false);
    }

    @Test
    void shouldNotGrantGroupPrivilegeForMissingBusinessUnit() {
        User clearingAdmin = user(BU, UserGroup.CLEARING_ADMIN);
        for (ProjectClearingState state : ProjectClearingState.values()) {
            Project project = project(state);
            project.unsetBusinessUnit();
            assertFalse(new ProjectPermissions(project, clearingAdmin).isActionAllowed(RequestedAction.WRITE));
            project.setBusinessUnit("");
            assertFalse(new ProjectPermissions(project, clearingAdmin).isActionAllowed(RequestedAction.WRITE));
        }
        assertDirectWrite(user("", UserGroup.CLEARING_ADMIN), false);
        assertDirectWrite(user(null, UserGroup.CLEARING_ADMIN), false);
    }

    @Test
    void shouldPreserveProjectMembershipAndClearingExpertPermissions() {
        assertDirectWrite(user(BU, UserGroup.CLEARING_EXPERT), true);
        User member = user(OTHER_BU, UserGroup.USER);
        for (ProjectClearingState state : ProjectClearingState.values()) {
            Project project = project(state).setModerators(Set.of(EMAIL));
            assertTrue(new ProjectPermissions(project, member).isActionAllowed(RequestedAction.WRITE));
        }
    }

    @Test
    void shouldNotGrantVisibilityToRestrictedProjects() {
        User clearingAdmin = user(BU, UserGroup.CLEARING_ADMIN);
        for (Visibility visibility : Set.of(Visibility.PRIVATE, Visibility.ME_AND_MODERATORS)) {
            Project restricted = project(ProjectClearingState.OPEN).setVisbility(visibility);
            assertFalse(new ProjectPermissions(restricted, clearingAdmin).isActionAllowed(RequestedAction.READ));
        }
    }
}
