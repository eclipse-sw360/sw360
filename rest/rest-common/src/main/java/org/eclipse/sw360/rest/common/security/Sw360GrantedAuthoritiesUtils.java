/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security;

import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared authority mapping helpers used by both REST auth stacks.
 */
public final class Sw360GrantedAuthoritiesUtils {

    private Sw360GrantedAuthoritiesUtils() {
    }

    public static List<GrantedAuthority> generateFromUser(
            User user, UserGroup writeAccessUserGroup, UserGroup adminAccessUserGroup
    ) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("READ"));
        if (user == null) {
            return grantedAuthorities;
        }

        if (PermissionUtils.isUserAtLeast(writeAccessUserGroup, user)) {
            grantedAuthorities.add(new SimpleGrantedAuthority("WRITE"));
        }
        if (PermissionUtils.isUserAtLeast(adminAccessUserGroup, user)) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ADMIN"));
        }
        return grantedAuthorities;
    }
}
