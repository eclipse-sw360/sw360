/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.permissions;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.isUserAtLeast;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.ADMIN;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.CLEARING_ADMIN;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ProjectPermissions extends DocumentPermissions<Project> {

    private final Set<String> moderators;
    private final Set<String> contributors;
    private final Set<String> attachmentContentIds;

    protected ProjectPermissions(Project document, User user) {
        super(document, user);

        moderators = new ImmutableSet.Builder<String>()
                .addAll(toSingletonSet(document.getCreatedBy()))
                .addAll(toSingletonSet(document.getProjectResponsible()))
                .addAll(nullToEmptySet(document.getModerators()))
                .build();
        contributors = new ImmutableSet.Builder<String>()
                .addAll(moderators)
                .addAll(nullToEmptySet(document.getContributors()))
                .addAll(toSingletonSet(document.getLeadArchitect()))
                .build();
        attachmentContentIds = nullToEmptySet(document.getAttachments()).stream()
                .map(a -> a.getAttachmentContentId())
                .collect(Collectors.toSet());
    }

    public static boolean isUserInBU(Project document, String bu) {
        final String buFromOrganisation = getBUFromOrganisation(bu);
        return !isNullOrEmpty(bu) && !isNullOrEmpty(buFromOrganisation)
              && !isNullOrEmpty(document.getBusinessUnit()) && document.getBusinessUnit().startsWith(buFromOrganisation);
    }

    public static boolean userIsEquivalentToModeratorinProject(Project input, String user) {
        final HashSet<String> allowedUsers = new HashSet<>();
        if (input.isSetCreatedBy()) allowedUsers.add(input.getCreatedBy());
        if (input.isSetLeadArchitect()) allowedUsers.add(input.getLeadArchitect());
        if (input.isSetProjectResponsible()) allowedUsers.add(input.getProjectResponsible());
        if (input.isSetModerators()) allowedUsers.addAll(input.getModerators());
        if (input.isSetContributors()) allowedUsers.addAll(input.getContributors());

        return allowedUsers.contains(user);
    }

    @NotNull
    public static Predicate<Project> isVisible(final User user) {
        return input -> {
            Visibility visbility = input.getVisbility();
            if (visbility == null) {
                visbility = Visibility.BUISNESSUNIT_AND_MODERATORS; // the current default
            }

            switch (visbility) {
                case PRIVATE:
                    return user.getEmail().equals(input.getCreatedBy());
                case ME_AND_MODERATORS: {
                    return userIsEquivalentToModeratorinProject(input, user.getEmail());
                }
                case BUISNESSUNIT_AND_MODERATORS: {
                    return isUserInBU(input, user.getDepartment()) || userIsEquivalentToModeratorinProject(input, user.getEmail()) || isUserAtLeast(CLEARING_ADMIN, user);
                }
                case EVERYONE:
                    return true;
            }

            return false;
        };
    }

    @Override
    public void fillPermissions(Project other, Map<RequestedAction, Boolean> permissions) {
        other.permissions=permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        if (action == RequestedAction.READ) {
            return isVisible(user).test(document);
        } else if (document.getClearingState() == ProjectClearingState.CLOSED) {
            switch (action) {
                case WRITE:
                case ATTACHMENTS:
                    return isClearingAdminOfOwnGroup() || PermissionUtils.isUserAtLeast(ADMIN, user);
                case DELETE:
                case USERS:
                case CLEARING:
                case WRITE_ECC:
                    return PermissionUtils.isAdmin(user);
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } else {
            return getStandardPermissions(action);
        }
    }

    @Override
    protected boolean isUserInEquivalentToOwnerGroup(){
        return isUserInBU(document, user.getDepartment());
    }

    @Override
    protected Set<String> getContributors() {
        return contributors;
    }

    @Override
    protected Set<String> getModerators() {
        return moderators;
    }

    @Override
    protected Set<String> getAttachmentContentIds() {
        return attachmentContentIds;
    }

}
