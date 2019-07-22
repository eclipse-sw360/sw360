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
 * This class offer helper methods to calculate the {@GrantedAuthority} for a user and/or client. 
 * In addition it can calculate the correct intersection between them! Therefore it has to
 * know how to map the sw360 user groups on rest authorities. This logic is also
 * centralized here implicitly.
 */
public class Sw360GrantedAuthoritiesCalculator {

    private final Logger log = Logger.getLogger(this.getClass());

    public List<GrantedAuthority> generateFromUser(User user) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        
        grantedAuthorities.add(new SimpleGrantedAuthority(READ.getAuthority()));
        if(user != null) {
            if (PermissionUtils.isUserAtLeast(Sw360AuthorizationServer.CONFIG_WRITE_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()));
            }
            if (PermissionUtils.isUserAtLeast(Sw360AuthorizationServer.CONFIG_ADMIN_ACCESS_USERGROUP, user)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()));
            }
        }

        return grantedAuthorities;
    }

    public List<GrantedAuthority> intersectWithClient(List<GrantedAuthority> grantedAuthorities, ClientDetails clientDetails) {
        Set<String> clientScopes = clientDetails.getScope();

        grantedAuthorities = grantedAuthorities.stream()
                .filter(ga -> clientScopes.contains(ga.toString()))
                .collect(Collectors.toList());
        
        return grantedAuthorities;
    }

    public List<GrantedAuthority> mergedAuthoritiesOf(User user, ClientDetails clientDetails) {
        List<GrantedAuthority> grantedAuthorities = generateFromUser(user);

        if(clientDetails != null) {
            log.debug("User " + user.email + " has authorities " + grantedAuthorities + " while used client "
                        + clientDetails.getClientId() + " has scopes " + clientDetails.getScope()
                        + ". Setting intersection as granted authorities for access token!");
            grantedAuthorities = intersectWithClient(grantedAuthorities, clientDetails);
        }

        return grantedAuthorities;
    }
}
