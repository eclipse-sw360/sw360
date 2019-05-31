/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer;

import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.READ;

/**
 * Class only offers one single but very important method. It can calculate the
 * correct intersection between user and client authorities! Therefore it has to
 * know how to map the sw360 user groups on rest authorities. This logic is also
 * centralized here implicitly.
 */
public class Sw360UserAndClientAuthoritiesMerger {

    private final Logger log = Logger.getLogger(this.getClass());

    public List<GrantedAuthority> mergeAuthoritiesOf(User user, ClientDetails clientDetails) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority(READ.getAuthority()));

        if (!Objects.isNull(user)) {
            if (PermissionUtils.isUserAtLeast(Sw360AuthorizationServer.CONFIG_WRITE_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()));
            }
            if (PermissionUtils.isUserAtLeast(Sw360AuthorizationServer.CONFIG_ADMIN_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()));
            }
        }

        if (!Objects.isNull(clientDetails)) {
            Set<String> clientScopes = clientDetails.getScope();

            log.debug("User " + user.email + " has authorities " + grantedAuthorities + " while used client "
                    + clientDetails.getClientId() + " has scopes " + clientScopes
                    + ". Setting intersection as granted authorities for access token!");

            grantedAuthorities = grantedAuthorities.stream()
                    .filter(ga -> clientScopes.contains(ga.toString()))
                    .collect(Collectors.toList());
        }

        return grantedAuthorities;
    }
}
