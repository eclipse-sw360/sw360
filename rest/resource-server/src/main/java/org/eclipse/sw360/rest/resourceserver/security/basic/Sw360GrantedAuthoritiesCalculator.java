/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * This class offer helper methods to calculate the {@GrantedAuthority} for a
 * user.
 */
public class Sw360GrantedAuthoritiesCalculator {

    public static List<GrantedAuthority> generateFromUser(User user) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()));
        if (user != null) {
            if (PermissionUtils.isUserAtLeast(Sw360ResourceServer.CONFIG_WRITE_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()));
            }
            if (PermissionUtils.isUserAtLeast(Sw360ResourceServer.CONFIG_ADMIN_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()));
            }
        }

        return grantedAuthorities;
    }
}
