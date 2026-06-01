/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.users;

import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.datahandler.services.users.UserGroup;

public final class UserGroupConverter {

    private UserGroupConverter() {}

    public static UserGroup fromThrift(org.eclipse.sw360.datahandler.thrift.users.UserGroup thrift) {
        return EnumConverter.fromThrift(thrift, UserGroup.class);
    }

    public static org.eclipse.sw360.datahandler.thrift.users.UserGroup toThrift(UserGroup pojo) {
        return EnumConverter.toThrift(pojo, org.eclipse.sw360.datahandler.thrift.users.UserGroup.class);
    }
}
