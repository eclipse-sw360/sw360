/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.permissions;

import static org.eclipse.sw360.datahandler.permissions.jgivens.GivenComponent.ComponentRole.*;
import static org.eclipse.sw360.datahandler.thrift.Visibility.*;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.*;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.permissions.jgivens.GivenComponent;
import org.eclipse.sw360.datahandler.permissions.jgivens.GivenComponent.ComponentRole;
import org.eclipse.sw360.datahandler.permissions.jgivens.ThenVisible;
import org.eclipse.sw360.datahandler.permissions.jgivens.WhenComputeComponentVisibility;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * @author kouki1.hama@toshiba.co.jp
 */
@RunWith(DataProviderRunner.class)
public class ComponentPermissionsVisibilityTest extends ScenarioTest<GivenComponent, WhenComputeComponentVisibility, ThenVisible> {

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
    public static Object[][] componentVisibilityProvider() {
        if (TestUtils.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED) {
            // @formatter:off
            return new Object[][] {
    
                    //test otherDeparment
                    //test User (otherUsers)
                    { PRIVATE, theBu, theOtherDep, USER, false },
                    { ME_AND_MODERATORS, theBu, theOtherDep, USER, false },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, USER, false },
                    { EVERYONE, theBu, theOtherDep, USER, true },
                    //test Clearing Admin (clearingAdmin)
                    { PRIVATE, theBu, theOtherDep, CLEARING_ADMIN, false },
                    { ME_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, false },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, true },
                    { EVERYONE, theBu, theOtherDep, CLEARING_ADMIN, true },
                    //test  Admin (Admin)
                    { PRIVATE, theBu, theOtherDep, ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theOtherDep, ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, ADMIN, true },
                    { EVERYONE, theBu, theOtherDep, ADMIN, true },
    
                    //test same department
                    //test User (sameGroupUsers)
                    { PRIVATE, theDep, theDep, USER, false },
                    { ME_AND_MODERATORS, theDep, theDep, USER, false },
                    { BUISNESSUNIT_AND_MODERATORS, theDep, theDep, USER, true },
                    { EVERYONE, theDep, theDep, USER, true },
                    //test Clearing Admin (clearingAdmin)
                    { PRIVATE, theBu, theDep, CLEARING_ADMIN, false },
                    { ME_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, false },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, true },
                    { EVERYONE, theBu, theDep, CLEARING_ADMIN, true },
                    //test  Admin (Admin)
                    { PRIVATE, theBu, theDep, ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theDep, ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, ADMIN, true },
                    { EVERYONE, theBu, theDep, ADMIN, true },
            };
            // @formatter:on
        } else {
            // @formatter:off
            return new Object[][] {

                    //test otherDeparment
                    //test User (otherUsers)
                    { PRIVATE, theBu, theOtherDep, USER, true },
                    { ME_AND_MODERATORS, theBu, theOtherDep, USER, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, USER, true },
                    { EVERYONE, theBu, theOtherDep, USER, true },
                    //test Clearing Admin (clearingAdmin)
                    { PRIVATE, theBu, theOtherDep, CLEARING_ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, CLEARING_ADMIN, true },
                    { EVERYONE, theBu, theOtherDep, CLEARING_ADMIN, true },
                    //test  Admin (Admin)
                    { PRIVATE, theBu, theOtherDep, ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theOtherDep, ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theOtherDep, ADMIN, true },
                    { EVERYONE, theBu, theOtherDep, ADMIN, true },

                    //test same department
                    //test User (sameGroupUsers)
                    { PRIVATE, theDep, theDep, USER, true },
                    { ME_AND_MODERATORS, theDep, theDep, USER, true },
                    { BUISNESSUNIT_AND_MODERATORS, theDep, theDep, USER, true },
                    { EVERYONE, theDep, theDep, USER, true },
                    //test Clearing Admin (clearingAdmin)
                    { PRIVATE, theBu, theDep, CLEARING_ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, CLEARING_ADMIN, true },
                    { EVERYONE, theBu, theDep, CLEARING_ADMIN, true },
                    //test  Admin (Admin)
                    { PRIVATE, theBu, theDep, ADMIN, true },
                    { ME_AND_MODERATORS, theBu, theDep, ADMIN, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, theDep, ADMIN, true },
                    { EVERYONE, theBu, theDep, ADMIN, true },
                };
                // @formatter:on
        }
    }

    @Test
    @UseDataProvider("componentVisibilityProvider")
    public void testVisibility(Visibility visibility, String businessUnit, String department, UserGroup userGroup, boolean expectedVisibility) {
        given().a_new_component().with_visibility_$_and_business_unit_$(visibility, businessUnit);
        when().the_visibility_is_computed_for_department_$_and_user_group_$(department, userGroup);
        then().the_visibility_should_be(expectedVisibility);

    }

    @DataProvider
    public static Object[][] componentVisibilityByRoleProvider() {
        if (TestUtils.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED) {
            // @formatter:off
            return new Object[][] {
                    //test otherDepartment
                    //created by
                    //test same User (creator)
                    { PRIVATE, theBu, CREATED_BY, null, theUser, theUser, true },
                    { ME_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theUser, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theUser, true },
                    { EVERYONE, theBu, CREATED_BY, null, theUser, theUser, true },
                    //test different User (other users)
                    { PRIVATE, theBu, CREATED_BY, null, theUser, theOtherUser, false },
                    { ME_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theOtherUser, false },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theOtherUser, false },
                    { EVERYONE, theBu, CREATED_BY, null, theUser, theOtherUser, true },
    
                    //test different User (Moderators)
                    { PRIVATE, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, false },
                    { ME_AND_MODERATORS, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
                    { EVERYONE, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
            };
            // @formatter:on
        } else {
            // @formatter:off
            return new Object[][] {
                    //test otherDepartment
                    //created by
                    //test same User (creator)
                    { PRIVATE, theBu, CREATED_BY, null, theUser, theUser, true },
                    { ME_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theUser, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theUser, true },
                    { EVERYONE, theBu, CREATED_BY, null, theUser, theUser, true },
                    //test different User (other users)
                    { PRIVATE, theBu, CREATED_BY, null, theUser, theOtherUser, true },
                    { ME_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theOtherUser, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, CREATED_BY, null, theUser, theOtherUser, true },
                    { EVERYONE, theBu, CREATED_BY, null, theUser, theOtherUser, true },

                    //test different User (Moderators)
                    { PRIVATE, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
                    { ME_AND_MODERATORS, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
                    { BUISNESSUNIT_AND_MODERATORS, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
                    { EVERYONE, theBu, MODERATOR, theOtherUser, theUser, theOtherUser, true },
            };
            // @formatter:on
        }
    }

    @Test
    @UseDataProvider("componentVisibilityByRoleProvider")
    public void testVisibilityForComponent(Visibility visibility, String bu, ComponentRole role, String moderatingUser, String creatingUser, String viewingUser, boolean expectedVisibility) throws Exception {
        if (role==MODERATOR) {
            given().a_component_with_$_$(role, moderatingUser).with_visibility_$_and_business_unit_$(visibility, bu);
        } else {
            given().a_component_with_$_$(role, creatingUser).with_visibility_$_and_business_unit_$(visibility, bu);
        }
        when().the_visibility_is_computed_for_the_wrong_department_and_the_user_$(viewingUser);
        then().the_visibility_should_be(expectedVisibility);
    }

}