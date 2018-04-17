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
package org.eclipse.sw360.datahandler.permissions.jgivens;

import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.permissions.DocumentPermissions;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.Quoted;

import java.util.List;

/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class WhenComputePermissions  extends Stage<WhenComputePermissions> {
    @ExpectedScenarioState
    Project project;

    @ProvidedScenarioState
    List<RequestedAction> allowedActions;

    private static String DUMMY_ID = "DAU";

    public WhenComputePermissions the_highest_allowed_action_is_computed_for_user_$_with_user_group_$_and_department_$(@Quoted String userEmail, @TEnumToString UserGroup userGroup, @Quoted String userDept) {
        final User user = new User(DUMMY_ID, userEmail, userDept).setUserGroup(userGroup);

        final DocumentPermissions<Project> projectDocumentPermissions = PermissionUtils.makePermission(project, user);

        allowedActions = projectDocumentPermissions.getAllAllowedActions();
        return self();
    }
}
