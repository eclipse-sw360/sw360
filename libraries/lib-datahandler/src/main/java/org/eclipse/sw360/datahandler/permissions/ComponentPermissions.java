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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toSingletonSet;

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
        //Should depend on permissions of contained releases
        this.createdBy = toSingletonSet(document.createdBy);
        moderators = Sets.union(toSingletonSet(document.createdBy), nullToEmptySet(document.moderators));
        attachmentContentIds = nullToEmptySet(document.getAttachments()).stream()
                .map(a -> a.getAttachmentContentId())
                .collect(Collectors.toSet());
    }

    @Override
    public void fillPermissions(Component other, Map<RequestedAction, Boolean> permissions) {
        other.permissions = permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        return getStandardPermissions(action);
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
    protected Set<String> getUserEquivalentOwnerGroup(){
        Set<String> departments = new HashSet<String>();
        departments.add(user.getDepartment());
        if (!CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())) {
            departments.addAll(user.getSecondaryDepartmentsAndRoles().keySet());
        }

        return departments;
    }
}
