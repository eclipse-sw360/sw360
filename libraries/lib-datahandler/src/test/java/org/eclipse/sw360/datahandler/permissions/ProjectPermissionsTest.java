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
package org.eclipse.sw360.datahandler.permissions;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.permissions.jgivens.GivenProject;
import org.eclipse.sw360.datahandler.permissions.jgivens.ThenHighestAllowedAction;
import org.eclipse.sw360.datahandler.permissions.jgivens.WhenComputePermissions;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.sw360.datahandler.thrift.users.RequestedAction.*;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.*;

/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */

@RunWith(DataProviderRunner.class)
public class ProjectPermissionsTest extends ScenarioTest<GivenProject, WhenComputePermissions, ThenHighestAllowedAction> {

    public static final List<RequestedAction> ALL_ACTIONS = ImmutableList.of(READ, WRITE, WRITE_ECC, ATTACHMENTS, DELETE, USERS, CLEARING);
    public static final List<RequestedAction> ALL_ACTIONS_EXCEPT_ECC = ImmutableList.of(READ, WRITE, ATTACHMENTS, DELETE, USERS, CLEARING);
    public static final List<RequestedAction> READ_ACTION = ImmutableList.of(READ);
    public static final List<RequestedAction> PRIVILEGED_ACTIONS_EXCEPT_ECC = Arrays.asList(READ, WRITE, ATTACHMENTS);
    public static String theUser = "user1";
    public static String theOtherUser = "anotherUser";
    public static final String theDept = "SOME DEPT";
    public static final String theOtherDept = "OTH ER DEPT";


    /**
     * See
     * org.eclipse.sw360.datahandler.permissions.DocumentPermissions.getAllAllowedActions()
     * for relevant cases
     */
    @DataProvider
    public static Object[][] highestAllowedActionProvider() {
        // @formatter:off
        return new Object[][] {
                //own permissions checks
                //very privileged
                {GivenProject.ProjectRole.CREATED_BY, theUser, theUser, USER, theDept, ALL_ACTIONS_EXCEPT_ECC},
                {GivenProject.ProjectRole.MODERATOR, theUser, theUser, USER, theDept, ALL_ACTIONS_EXCEPT_ECC},
                {GivenProject.ProjectRole.PROJECT_RESPONSIBLE, theUser, theUser, USER, theDept, ALL_ACTIONS_EXCEPT_ECC},
                //less privileged
                {GivenProject.ProjectRole.LEAD_ARCHITECT, theUser, theUser, USER, theDept, PRIVILEGED_ACTIONS_EXCEPT_ECC },
                {GivenProject.ProjectRole.CONTRIBUTOR, theUser, theUser, USER, theDept, PRIVILEGED_ACTIONS_EXCEPT_ECC },

                //strangers: rights increase with user group
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, USER, theDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, CLEARING_ADMIN, theDept, PRIVILEGED_ACTIONS_EXCEPT_ECC },
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, CLEARING_ADMIN, theOtherDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ECC_ADMIN, theDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ADMIN, theDept, ALL_ACTIONS},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ADMIN, theOtherDept, ALL_ACTIONS},
        };
        // @formatter:on
    }

    @Test
    @UseDataProvider("highestAllowedActionProvider")
    public void testHighestAllowedAction(GivenProject.ProjectRole role, String user, String requestingUser, UserGroup requestingUserGroup, String requestingUserDept, List<RequestedAction> allowedActions) throws Exception {
        given().a_project_with_$_$(role,user).with_visibility_$_and_business_unit_$(Visibility.EVERYONE, theDept);
        when().the_highest_allowed_action_is_computed_for_user_$_with_user_group_$_and_department_$(requestingUser, requestingUserGroup, requestingUserDept);
        then().the_allowed_actions_should_be(allowedActions);
    }

    @DataProvider
    public static Object[][] highestAllowedActionForClosedProjectProvider() {
        // @formatter:off
        return new Object[][] {
                //own permissions checks
                //very privileged
                {GivenProject.ProjectRole.CREATED_BY, theUser, theUser, USER, theDept, READ_ACTION},
                {GivenProject.ProjectRole.MODERATOR, theUser, theUser, USER, theDept, READ_ACTION},
                {GivenProject.ProjectRole.PROJECT_RESPONSIBLE, theUser, theUser, USER, theDept, READ_ACTION},
                //less privileged
                {GivenProject.ProjectRole.LEAD_ARCHITECT, theUser, theUser, USER, theDept, READ_ACTION},
                {GivenProject.ProjectRole.CONTRIBUTOR, theUser, theUser, USER, theDept, READ_ACTION},

                //strangers: rights increase with user group
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, USER, theDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, CLEARING_ADMIN, theDept, PRIVILEGED_ACTIONS_EXCEPT_ECC },
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, CLEARING_ADMIN, theOtherDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ECC_ADMIN, theDept, READ_ACTION},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ADMIN, theDept, ALL_ACTIONS},
                {GivenProject.ProjectRole.CREATED_BY, theUser, theOtherUser, ADMIN, theOtherDept, ALL_ACTIONS},
        };
        // @formatter:on
    }

    @Test
    @UseDataProvider("highestAllowedActionForClosedProjectProvider")
    public void testHighestAllowedActionForClosedProject(GivenProject.ProjectRole role, String user, String requestingUser, UserGroup requestingUserGroup, String requestingUserDept, List<RequestedAction> allowedActions) throws Exception {
        given().a_closed_project_with_$_$(role,user).with_visibility_$_and_business_unit_$(Visibility.EVERYONE, theDept);
        when().the_highest_allowed_action_is_computed_for_user_$_with_user_group_$_and_department_$(requestingUser, requestingUserGroup, requestingUserDept);
        then().the_allowed_actions_should_be(allowedActions);
    }
}