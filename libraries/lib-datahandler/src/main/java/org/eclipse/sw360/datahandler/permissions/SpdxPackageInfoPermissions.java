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

import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Map;
import java.util.Set;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toSingletonSet;

public class SpdxPackageInfoPermissions extends DocumentPermissions<PackageInformation> {

    private final Set<String> moderators;
    private final Set<String> createdBy;

    protected SpdxPackageInfoPermissions(PackageInformation packageInfo, User user) {
        super(packageInfo, user);
        this.createdBy = toSingletonSet(packageInfo.createdBy);
        moderators = toSingletonSet(packageInfo.createdBy);
    }

    @Override
    public void fillPermissions(PackageInformation packageInfo, Map<RequestedAction, Boolean> permissions) {
        packageInfo.permissions = permissions;
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
}
