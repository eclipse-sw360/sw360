/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.permissions.jgivens.GivenProject;
import org.eclipse.sw360.datahandler.permissions.jgivens.ThenVisible;
import org.eclipse.sw360.datahandler.permissions.jgivens.WhenComputeVisibility;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.eclipse.sw360.datahandler.permissions.jgivens.GivenProject.ProjectRole.*;
import static org.eclipse.sw360.datahandler.thrift.Visibility.*;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.*;

/**
 * @author johannes.najjar@tngtech.com
 */
@RunWith(DataProviderRunner.class)
public class ProjectPermissionsVisibilityTest extends ScenarioTest<GivenProject, WhenComputeVisibility, ThenVisible> {

    public static String theBu = "DE PA RT"; // ME NT
    public static String theDep = "DE PA RT ME NT";
    public static String theOtherDep = "OT TH ER DE";
    public static String theUser = "user1";
    public static String theOtherUser = "anotherUser";

    /**
     * The testing strategy for visibility is as follows:
     * It depends on the UserGroup and Department as well as the user Roles, in the first tests we verify them.
     * We can override a no from these criteria by being in one of the moderator classes. This is the next test block.
     */


    @DataProvider
    public static Object[][] projectVisibilityProvider() {
        // @formatter:off
        return new Object[][] {
                //test otherDeparment
                //test User
                { PRIVATE, theBu, theOtherDep, USER, false },
                { ME_AND_MODERATORS, theBu, theOtherDep, USER, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, USER, false },
                { EVERYONE, theBu, theOtherDep, USER, true },
                //test Clearing Admin
                { PRIVATE, theBu, theOtherDep, CLEARING_ADMIN, false },
                { ME_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, true },
                { EVERYONE, theBu, theOtherDep, CLEARING_ADMIN, true },
                //test  Admin
                { PRIVATE, theBu, theOtherDep, ADMIN, false },
                { ME_AND_MODERATORS, theBu, theOtherDep, ADMIN, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, ADMIN, true },
                { EVERYONE, theBu, theOtherDep, ADMIN, true },
                //test same department
                //test User
                { PRIVATE, theBu, theDep, USER, false },
                { ME_AND_MODERATORS, theBu, theDep, USER, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, USER, true },
                { EVERYONE, theBu, theDep, USER, true },
                //test Clearing Admin
                { PRIVATE, theBu, theDep, CLEARING_ADMIN, false },
                { ME_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, true },
                { EVERYONE, theBu, theDep, CLEARING_ADMIN, true },
                //test  Admin
                { PRIVATE, theBu, theDep, ADMIN, false },
                { ME_AND_MODERATORS, theBu, theDep, ADMIN, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, ADMIN, true },
                { EVERYONE, theBu, theDep, ADMIN, true },
        };
        // @formatter:on
    }

    @Test
    @UseDataProvider("projectVisibilityProvider")
    public void testVisibility(Visibility visibility, String businessUnit, String department, UserGroup userGroup, boolean expectedVisibility) {
        given().a_new_project().with_visibility_$_and_business_unit_$(visibility, businessUnit);
        when().the_visibility_is_computed_for_department_$_and_user_group_$(department, userGroup);
        then().the_visibility_should_be(expectedVisibility);

    }

    @DataProvider
    public static Object[][] projectVisibilityByRoleProvider() {
        // @formatter:off
        return new Object[][] {
                //test otherDepartment
                //created by
                //test same User
                { PRIVATE, theBu, CREATED_BY, theUser, theUser, true },
                { ME_AND_MODERATORS, theBu, CREATED_BY, theUser, theUser, true },
                { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, theUser, theUser, true },
                { EVERYONE, theBu, CREATED_BY, theUser, theUser, true },
                //test different User
                { PRIVATE, theBu, CREATED_BY, theUser, theOtherUser, false },
                { ME_AND_MODERATORS, theBu, CREATED_BY, theUser, theOtherUser, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, theUser, theOtherUser, false },
                { EVERYONE, theBu, CREATED_BY, theUser, theOtherUser, true },

                //Lead architect
                //test same User
                { PRIVATE, theBu, LEAD_ARCHITECT, theUser, theUser, false },
                { ME_AND_MODERATORS, theBu, LEAD_ARCHITECT, theUser, theUser, true },
                { BUISNESSUNIT_AND_MODERATORS, theBu, LEAD_ARCHITECT, theUser, theUser, true },
                { EVERYONE, theBu, LEAD_ARCHITECT, theUser, theUser, true },
                //test different User
                { PRIVATE, theBu, LEAD_ARCHITECT, theUser, theOtherUser, false },
                { ME_AND_MODERATORS, theBu, LEAD_ARCHITECT, theUser, theOtherUser, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, LEAD_ARCHITECT, theUser, theOtherUser, false },
                { EVERYONE, theBu, LEAD_ARCHITECT, theUser, theOtherUser, true },

                //Moderator
                //test same User
                { PRIVATE, theBu, MODERATOR, theUser, theUser, false },
                { ME_AND_MODERATORS, theBu, MODERATOR, theUser, theUser, true },
                { BUISNESSUNIT_AND_MODERATORS, theBu, MODERATOR, theUser, theUser, true },
                { EVERYONE, theBu, MODERATOR, theUser, theUser, true },
                //test different User
                { PRIVATE, theBu, MODERATOR, theUser, theOtherUser, false },
                { ME_AND_MODERATORS, theBu, MODERATOR, theUser, theOtherUser, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, MODERATOR, theUser, theOtherUser, false },
                { EVERYONE, theBu, MODERATOR, theUser, theOtherUser, true },

                //Contributor
                //test same User
                { PRIVATE, theBu, CONTRIBUTOR, theUser, theUser, false },
                { ME_AND_MODERATORS, theBu, CONTRIBUTOR, theUser, theUser, true },
                { BUISNESSUNIT_AND_MODERATORS, theBu, CONTRIBUTOR, theUser, theUser, true },
                { EVERYONE, theBu, CONTRIBUTOR, theUser, theUser, true },
                //test different User
                { PRIVATE, theBu, CONTRIBUTOR, theUser, theOtherUser, false },
                { ME_AND_MODERATORS, theBu, CONTRIBUTOR, theUser, theOtherUser, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, CONTRIBUTOR, theUser, theOtherUser, false },
                { EVERYONE, theBu, CONTRIBUTOR, theUser, theOtherUser, true },

                //Project responsible
                //test same User
                { PRIVATE, theBu, PROJECT_RESPONSIBLE, theUser, theUser, false },
                { ME_AND_MODERATORS, theBu, PROJECT_RESPONSIBLE, theUser, theUser, true },
                { BUISNESSUNIT_AND_MODERATORS, theBu, PROJECT_RESPONSIBLE, theUser, theUser, true },
                { EVERYONE, theBu, PROJECT_RESPONSIBLE, theUser, theUser, true },
                //test different User
                { PRIVATE, theBu, PROJECT_RESPONSIBLE, theUser, theOtherUser, false },
                { ME_AND_MODERATORS, theBu, PROJECT_RESPONSIBLE, theUser, theOtherUser, false },
                { BUISNESSUNIT_AND_MODERATORS, theBu, PROJECT_RESPONSIBLE, theUser, theOtherUser, false },
                { EVERYONE, theBu, PROJECT_RESPONSIBLE, theUser, theOtherUser, true },
        };
        // @formatter:on
    }

    @Test
    @UseDataProvider("projectVisibilityByRoleProvider")
    public void testVisibilityForProject(Visibility visibility, String bu, GivenProject.ProjectRole role, String creatingUser, String viewingUser, boolean expectedVisibility) throws Exception {
        given().a_project_with_$_$(role, creatingUser).with_visibility_$_and_business_unit_$(visibility, bu);
        when().the_visibility_is_computed_for_the_wrong_department_and_the_user_$(viewingUser);
        then().the_visibility_should_be(expectedVisibility);
    }

}