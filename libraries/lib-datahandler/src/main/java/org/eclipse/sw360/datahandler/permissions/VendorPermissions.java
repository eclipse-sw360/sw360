/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.permissions;


import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class VendorPermissions extends DocumentPermissions<Vendor> {
    private final Set<String> moderators;
    private final Set<String> contributors;

    protected VendorPermissions(Vendor document, User user) {
        super(document, user);

        moderators = Collections.emptySet();
        contributors = Collections.emptySet();

    }

    @Override
    public void fillPermissions(Vendor other, Map<RequestedAction, Boolean> permissions) {
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        return getStandardPermissions(action);
    }

    @Override
    protected Set<String> getContributors() {
        return contributors;
    }

    @Override
    protected Set<String> getModerators() {
        return moderators;
    }
}
