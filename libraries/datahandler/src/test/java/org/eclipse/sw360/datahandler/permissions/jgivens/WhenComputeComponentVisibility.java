/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.permissions.jgivens;

import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.permissions.ComponentPermissions;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.Quoted;

/**
 * @author kouki1.hama@toshiba.co.jp
 */
public class WhenComputeComponentVisibility extends Stage<WhenComputeComponentVisibility> {
	@ExpectedScenarioState
	Component component;

	@ProvidedScenarioState
	Boolean isVisible;

	private static String DUMMY_MAIL = "DAU@dau.com";
	private static String DUMMY_DEP = "definitleyTheWrongDepartment YO HO HO";

	public WhenComputeComponentVisibility the_visibility_is_computed_for_department_$_and_user_group_$(
			@Quoted String department, @TEnumToString UserGroup userGroup) {
		final User user = new User(DUMMY_MAIL, department).setUserGroup(userGroup);

		isVisible = ComponentPermissions.isVisible(user).test(component);
		return self();
	}

	public WhenComputeComponentVisibility the_visibility_is_computed_for_the_wrong_department_and_the_user_$(
			@Quoted String mail) {
		final User user = new User(mail, DUMMY_DEP).setUserGroup(UserGroup.USER);

		isVisible = ComponentPermissions.isVisible(user).test(component);
		return self();
	}

}
