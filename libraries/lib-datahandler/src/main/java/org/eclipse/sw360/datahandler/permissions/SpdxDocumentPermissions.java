/*
 * Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.thrift.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created on 10/08/2021.
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class SpdxDocumentPermissions extends DocumentPermissions<SPDXDocument> {

    protected SpdxDocumentPermissions(SPDXDocument document, User user) {
        super(document, user);
    }

    @Override
    public void fillPermissions(SPDXDocument spdx, Map<RequestedAction, Boolean> permissions) {
        spdx.permissions = permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        return getStandardPermissions(action);
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
