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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.thrift.users.RequestedAction.*;
import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.ADMIN;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public abstract class DocumentPermissions<T> {

    protected final T document;
    protected final User user;

    protected DocumentPermissions(T document, User user) {
        this.document = document;
        this.user = user;
    }

    public abstract void fillPermissions(T other, Map<RequestedAction, Boolean> permissions);

    public abstract boolean isActionAllowed(RequestedAction action);

    protected abstract Set<String> getContributors();

    protected abstract Set<String> getModerators();

    public boolean areActionsAllowed(List<RequestedAction> actions) {
        boolean result = true;

        if (actions != null) {
            for (RequestedAction action : actions) {
                if (!isActionAllowed(action)) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    protected boolean isUserInEquivalentToOwnerGroup(){
        return true;
    }

    protected Set<String> getAttachmentContentIds() {
        return Collections.emptySet();
    }

    protected boolean isContributor() {
        return user != null && CommonUtils.contains(user.email, getContributors());
    }

    protected boolean isModerator() {
        return CommonUtils.contains(user.email, getModerators());
    }

    public void fillPermissions() {
        fillPermissions(document, getPermissionMap());
    }

    public void fillPermissionsInOther(T other) {
        fillPermissions(other, getPermissionMap());
    }

    public Map<RequestedAction, Boolean> getPermissionMap() {
        Map<RequestedAction, Boolean> permissions = new EnumMap<>(RequestedAction.class);
        for (RequestedAction action : values()) {
            permissions.put(action, isActionAllowed(action));
        }
        return permissions;
    }

    protected boolean getStandardPermissions(RequestedAction action) {
        switch (action) {
            case READ:
                return true;
            case WRITE:
            case ATTACHMENTS:
                return isClearingAdminOfOwnGroup() || PermissionUtils.isUserAtLeast(ADMIN, user) || isContributor() ;
            case DELETE:
            case USERS:
            case CLEARING:
                return PermissionUtils.isAdmin(user) || isModerator();
            case WRITE_ECC:
                return PermissionUtils.isAdmin(user);
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    protected boolean isClearingAdminOfOwnGroup() {
        return PermissionUtils.isClearingAdmin(user) && isUserInEquivalentToOwnerGroup();
    }

    // useful for tests, maybe this needs to go somewhere else
    public List<RequestedAction> getAllAllowedActions(){
        return ImmutableSet
                .of(READ, WRITE, WRITE_ECC, ATTACHMENTS, DELETE, USERS, CLEARING)
                .stream()
                .filter(this::isActionAllowed)
                .collect(Collectors.toList());
    }

    public boolean isAllowedToDownload(AttachmentContent attachment){
        return isAllowedToDownload(attachment.getId());
    }

    public boolean isAllowedToDownload(String attachmentContentId){
        return nullToEmptySet(getAttachmentContentIds()).contains(attachmentContentId) &&
                isActionAllowed(RequestedAction.READ);
    }
}
