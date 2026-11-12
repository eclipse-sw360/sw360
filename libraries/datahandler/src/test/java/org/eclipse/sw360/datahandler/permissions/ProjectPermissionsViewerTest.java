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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests VIEWER visibility restrictions in ProjectPermissions.
 * VIEWER can only see projects with visibility EVERYONE.
 */
public class ProjectPermissionsViewerTest {

    private static final User VIEWER = new User("viewer@sw360.org", "sw360")
            .setUserGroup(UserGroup.VIEWER)
            .setDepartment("DEPT");

    private static final User NORMAL_USER = new User("user@sw360.org", "sw360")
            .setUserGroup(UserGroup.USER)
            .setDepartment("DEPT");

    private Project projectWithVisibility(Visibility visibility) {
        Project p = new Project().setId("p1").setName("Test").setCreatedBy("admin@sw360.org")
                .setBusinessUnit("DEPT");
        if (visibility != null) {
            p.setVisbility(visibility);
        }
        return p;
    }

    @Test
    public void viewer_can_see_everyone_project() {
        Predicate<Project> isVisible = ProjectPermissions.isVisible(VIEWER);
        assertTrue(isVisible.test(projectWithVisibility(Visibility.EVERYONE)));
    }

    @Test
    public void viewer_cannot_see_private_project() {
        Predicate<Project> isVisible = ProjectPermissions.isVisible(VIEWER);
        assertFalse(isVisible.test(projectWithVisibility(Visibility.PRIVATE)));
    }

    @Test
    public void viewer_cannot_see_business_unit_project() {
        Predicate<Project> isVisible = ProjectPermissions.isVisible(VIEWER);
        assertFalse(isVisible.test(projectWithVisibility(Visibility.BUISNESSUNIT_AND_MODERATORS)));
    }

    @Test
    public void viewer_cannot_see_me_and_moderators_project() {
        Predicate<Project> isVisible = ProjectPermissions.isVisible(VIEWER);
        assertFalse(isVisible.test(projectWithVisibility(Visibility.ME_AND_MODERATORS)));
    }


    @Test
    public void normal_user_can_see_business_unit_project() {
        Predicate<Project> isVisible = ProjectPermissions.isVisible(NORMAL_USER);
        assertTrue(isVisible.test(projectWithVisibility(Visibility.BUISNESSUNIT_AND_MODERATORS)));
    }
}
