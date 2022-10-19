/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.thrift.users.UserGroup.CLEARING_ADMIN;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicensePermissions extends DocumentPermissions<License> {


    protected LicensePermissions(License document, User user) {
        super(document, user);
    }

    @Override
    public void fillPermissions(License other, Map<RequestedAction, Boolean> permissions) {
        other.permissions = permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        switch (action) {
            case READ:
            case WRITE:
                return true;
            case CLEARING:
            case DELETE:
                return PermissionUtils.isUserAtLeast(CLEARING_ADMIN, user);
            default:
                return false;
        }
    }

    @Override
    protected Set<String> getContributors() {
        return Collections.emptySet();
    }

    @Override
    protected Set<String> getModerators() {
        return Collections.emptySet();
    }

}
