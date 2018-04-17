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

import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

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
public class ReleasePermissions extends DocumentPermissions<Release> {

    private final Set<String> moderators;
    private final Set<String> contributors;
    private final Set<String> attachmentContentIds;

    protected ReleasePermissions(Release document, User user) {
        super(document, user);

        moderators = Sets.union(toSingletonSet(document.createdBy), nullToEmptySet(document.moderators));
        contributors = Sets.union(moderators, nullToEmptySet(document.contributors));
        attachmentContentIds = nullToEmptySet(document.getAttachments()).stream()
                .map(a -> a.getAttachmentContentId())
                .collect(Collectors.toSet());

    }

    @Override
    public void fillPermissions(Release other, Map<RequestedAction, Boolean> permissions) {
        other.permissions = permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        if (action == RequestedAction.WRITE_ECC) {
            return PermissionUtils.isUserAtLeast(UserGroup.ECC_ADMIN, user);
        } else {
            return getStandardPermissions(action);
        }
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
