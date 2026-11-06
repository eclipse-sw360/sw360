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

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for VIEWER role handling in PermissionUtils.
 */
public class PermissionUtilsViewerTest {

    private User userWithGroup(UserGroup group) {
        return new User("test@sw360.org", "sw360").setUserGroup(group);
    }

    // ---- isViewer() ----

    @Test
    public void isViewer_should_return_true_for_viewer() {
        assertTrue(PermissionUtils.isViewer(userWithGroup(UserGroup.VIEWER)));
    }

    @Test
    public void isViewer_should_return_false_for_user() {
        assertFalse(PermissionUtils.isViewer(userWithGroup(UserGroup.USER)));
    }

    @Test
    public void isViewer_should_return_false_for_admin() {
        assertFalse(PermissionUtils.isViewer(userWithGroup(UserGroup.ADMIN)));
    }

    @Test
    public void isViewer_should_return_false_for_null() {
        assertFalse(PermissionUtils.isViewer(null));
    }

    @Test
    public void isViewer_should_return_false_when_userGroup_not_set() {
        User user = new User("test@sw360.org", "sw360");
        assertFalse(PermissionUtils.isViewer(user));
    }

    // ---- isUserAtLeast(VIEWER, user) — VIEWER is the lowest role ----

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_viewer() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.VIEWER)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_user() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.USER)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_admin() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.ADMIN)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_clearing_admin() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.CLEARING_ADMIN)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_security_admin() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.SECURITY_ADMIN)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_ecc_admin() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.ECC_ADMIN)));
    }

    @Test
    public void isUserAtLeast_viewer_should_be_true_for_security_user() {
        assertTrue(PermissionUtils.isUserAtLeast(UserGroup.VIEWER, userWithGroup(UserGroup.SECURITY_USER)));
    }

    // ---- isUserAtLeast(USER, viewer) — VIEWER is BELOW USER ----

    @Test
    public void isUserAtLeast_user_should_be_false_for_viewer() {
        assertFalse(PermissionUtils.isUserAtLeast(UserGroup.USER, userWithGroup(UserGroup.VIEWER)));
    }

    // ---- isUserAtLeastDesiredRoleInSecondaryGroup with VIEWER ----

    @Test
    public void secondary_viewer_should_match_viewer_role() {
        Set<UserGroup> secondaryRoles = EnumSet.of(UserGroup.VIEWER);
        assertTrue(PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.VIEWER, secondaryRoles));
    }

    @Test
    public void secondary_user_should_satisfy_viewer_role() {
        Set<UserGroup> secondaryRoles = EnumSet.of(UserGroup.USER);
        assertTrue(PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.VIEWER, secondaryRoles));
    }

    @Test
    public void secondary_admin_should_satisfy_viewer_role() {
        Set<UserGroup> secondaryRoles = EnumSet.of(UserGroup.ADMIN);
        assertTrue(PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.VIEWER, secondaryRoles));
    }

    @Test
    public void empty_secondary_roles_should_not_satisfy_viewer() {
        Set<UserGroup> secondaryRoles = EnumSet.noneOf(UserGroup.class);
        assertFalse(PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.VIEWER, secondaryRoles));
    }
}
