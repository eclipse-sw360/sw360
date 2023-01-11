/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toSingletonSet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getBUFromOrganisation;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.*;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.ADMIN;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.CLEARING_ADMIN;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentPermissions extends DocumentPermissions<Component> {

	private final Set<String> createdBy;
	private final Set<String> moderators;
	private final Set<String> attachmentContentIds;

	protected ComponentPermissions(Component document, User user) {
		super(document, user);
		// Should depend on permissions of contained releases
		this.createdBy = toSingletonSet(document.createdBy);
		moderators = Sets.union(toSingletonSet(document.createdBy), nullToEmptySet(document.moderators));
		attachmentContentIds = nullToEmptySet(document.getAttachments()).stream().map(a -> a.getAttachmentContentId())
				.collect(Collectors.toSet());
	}

	public static boolean isUserInBU(Component document, String bu) {
		return !isNullOrEmpty(bu) && !isNullOrEmpty(document.getBusinessUnit())
				&& document.getBusinessUnit().equals(bu);
	}

	public static boolean userIsEquivalentToModeratorInComponent(Component input, String user) {
		final HashSet<String> allowedUsers = new HashSet<>();
		if (input.isSetCreatedBy())
			allowedUsers.add(input.getCreatedBy());
		if (input.isSetModerators())
			allowedUsers.addAll(input.getModerators());

		return allowedUsers.contains(user);
	}

	@NotNull
	public static Predicate<Component> isVisible(final User user) {
		return input -> {

			if (!PermissionUtils.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED) {
				return true;
			}

			Visibility visibility = input.getVisbility();
			if (visibility == null) {
				visibility = Visibility.BUISNESSUNIT_AND_MODERATORS; // the current default
			}

			switch (visibility) {
				case PRIVATE :
					return PermissionUtils.isAdmin(user) || user.getEmail().equals(input.getCreatedBy());
				case ME_AND_MODERATORS : {
					return PermissionUtils.isAdmin(user)
							|| userIsEquivalentToModeratorInComponent(input, user.getEmail());
				}
				case BUISNESSUNIT_AND_MODERATORS : {
					boolean isVisibleBasedOnPrimaryCondition = isUserInBU(input, user.getDepartment())
							|| userIsEquivalentToModeratorInComponent(input, user.getEmail())
							|| isUserAtLeast(CLEARING_ADMIN, user);
					boolean isVisibleBasedOnSecondaryCondition = false;
					if (!isVisibleBasedOnPrimaryCondition) {
						Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user
								.getSecondaryDepartmentsAndRoles();
						if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
							if (getDepartmentIfUserInBU(input, secondaryDepartmentsAndRoles.keySet()) != null) {
								isVisibleBasedOnSecondaryCondition = true;
							}
						}
					}

					return isVisibleBasedOnPrimaryCondition || isVisibleBasedOnSecondaryCondition;
				}
				case EVERYONE :
					return true;
			}

			return false;
		};
	}

	@Override
	public void fillPermissions(Component other, Map<RequestedAction, Boolean> permissions) {
		other.permissions = permissions;
	}

	@Override
	public boolean isActionAllowed(RequestedAction action) {
		if (action == RequestedAction.READ) {
			return isVisible(user).test(document);
		} else {
			return getStandardPermissions(action);
		}
	}

	@Override
	protected Set<String> getContributors() {
		return moderators;
	}

	@Override
	protected Set<String> getModerators() {
		return moderators;
	}

	@Override
	protected Set<String> getAttachmentContentIds() {
		return attachmentContentIds;
	}

	@Override
	protected Set<String> getUserEquivalentOwnerGroup() {
		Set<String> departments = new HashSet<String>();
		if (!CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())) {
			departments.addAll(user.getSecondaryDepartmentsAndRoles().keySet());
		}
		departments.add(user.getDepartment());
		if (!PermissionUtils.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED) {
			return departments;
		}
		Set<String> finalDepartments = new HashSet<String>();
		String departmentIfUserInBU = getDepartmentIfUserInBU(document, departments);
		finalDepartments.add(departmentIfUserInBU);
		return departmentIfUserInBU == null ? null : finalDepartments;
	}

	private static String getDepartmentIfUserInBU(Component document, Set<String> BUs) {
		for (String bu : BUs) {
			String buFromOrganisation = getBUFromOrganisation(bu);
			boolean isUserInBU = !isNullOrEmpty(bu) && !isNullOrEmpty(buFromOrganisation)
					&& !isNullOrEmpty(document.getBusinessUnit())
					&& document.getBusinessUnit().equals(buFromOrganisation);
			if (isUserInBU) {
				return bu;
			}
		}

		return null;
	}
}
