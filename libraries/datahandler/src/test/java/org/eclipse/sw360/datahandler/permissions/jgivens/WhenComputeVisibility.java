/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.permissions.jgivens;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.Quoted;
import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

/**
 * @author johannes.najjar@tngtech.com
 */
public class WhenComputeVisibility extends Stage<WhenComputeVisibility> {
    @ExpectedScenarioState
    Project project;

    @ProvidedScenarioState
    Boolean isVisible;

    private static String DUMMY_MAIL = "DAU@dau.com";
    private static String DUMMY_DEP = "definitleyTheWrongDepartment YO HO HO";

    public WhenComputeVisibility the_visibility_is_computed_for_department_$_and_user_group_$(@Quoted String department, @TEnumToString UserGroup userGroup) {
        final User user = new User(DUMMY_MAIL, department).setUserGroup(userGroup);

        isVisible = ProjectPermissions.isVisible(user).test(project);
        return self();
    }

    public WhenComputeVisibility the_visibility_is_computed_for_the_wrong_department_and_the_user_$(@Quoted String mail) {
        final User user = new User(mail, DUMMY_DEP).setUserGroup(UserGroup.USER);

        isVisible = ProjectPermissions.isVisible(user).test(project);
        return self();
    }

}
