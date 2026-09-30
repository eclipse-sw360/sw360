/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toSingletonSet;

public class SpdxDocumentCreationInfoPermissions extends DocumentPermissions<DocumentCreationInformation> {

    private final Set<String> moderators;
    private final Set<String> createdBy;

    protected SpdxDocumentCreationInfoPermissions(DocumentCreationInformation document, User user) {
        super(document, user);
        this.createdBy = toSingletonSet(document.createdBy);
        moderators = Sets.union(toSingletonSet(document.createdBy), nullToEmptySet(document.moderators));
    }

    @Override
    public void fillPermissions(DocumentCreationInformation spdx, Map<RequestedAction, Boolean> permissions) {
        if (permissions == null) {
            spdx.permissions = null;
            return;
        }
        Map<org.eclipse.sw360.datahandler.thrift.users.RequestedAction, Boolean> thriftPermissions =
                new EnumMap<>(org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class);
        for (Map.Entry<RequestedAction, Boolean> entry : permissions.entrySet()) {
            thriftPermissions.put(
                    org.eclipse.sw360.datahandler.thrift.users.RequestedAction.valueOf(entry.getKey().name()),
                    entry.getValue());
        }
        spdx.permissions = thriftPermissions;
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
    protected Set<String> getSecurityResponsibles() {
        return Collections.emptySet();
    }
}
