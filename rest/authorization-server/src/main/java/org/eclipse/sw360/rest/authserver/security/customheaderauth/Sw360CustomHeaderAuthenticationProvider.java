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
package org.eclipse.sw360.rest.authserver.security.customheaderauth;

import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This {@link AuthenticationProvider} is specialised on requests where the
 * {@link Sw360CustomHeaderAuthenticationFilter} made sure that the user has
 * already been authenticated by some external proxy that set some headers to
 * let us know about the authentication.
 *
 * In addition it is special because it calculates the granted authorities for
 * the user depending on the user's authorities given by his groups and the
 * client's scopes. The result will be the intersection between these two lists.
 */
public class Sw360CustomHeaderAuthenticationProvider implements AuthenticationProvider {

    private final Logger log = Logger.getLogger(this.getClass());

    @Value("${security.customheader.headername.intermediateauthstore:#{null}}")
    private String customHeaderHeadernameIntermediateAuthStore;

    private boolean active;

    @Autowired
    private Sw360CustomHeaderUserDetailsProvider sw360CustomHeaderUserDetailsProvider;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @PostConstruct
    public void postSw360CustomHeaderAuthenticationProviderConstruction() {
        if (StringUtils.isEmpty(customHeaderHeadernameIntermediateAuthStore)) {
            log.warn("AuthenticationProvider is NOT active! Some configuration is missing. Needed config keys:\n"
                    + "- security.customheader.headername.intermediateauthstore");
            active = false;
        } else {
            log.info("AuthenticationProvider is active!");
            active = true;
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // check if the marker header of our filter is available
        if (authentication.getDetails() instanceof Map<?, ?>
                && ((Map<?, ?>) authentication.getDetails()).containsKey(customHeaderHeadernameIntermediateAuthStore)) {
            Map<?, ?> authDetails = ((Map<?, ?>) authentication.getDetails());

            // get user details
            String email = (String) authentication.getPrincipal();
            String externalId = (String) authDetails.get(customHeaderHeadernameIntermediateAuthStore);
            User userDetails = sw360CustomHeaderUserDetailsProvider.provideUserDetails(email, externalId);

            // calculate user authorities
            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()));
            if (userDetails != null && PermissionUtils
                    .isUserAtLeast(Sw360AuthorizationServer.CONFIG_WRITE_ACCESS_USERGROUP, userDetails)) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Sw360GrantedAuthority.WRITE.getAuthority()));
            }

            // keep only intersection of user authorities and client scopes
            String clientId = (String) authDetails.get(OAuth2Utils.CLIENT_ID);
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
            Set<String> clientScopes = clientDetails.getScope();

            log.debug("User " + email + " has authorities " + grantedAuthorities + " while used client " + clientId
                    + " has scopes " + clientScopes
                    + ". Setting intersection as granted authorities for access token!");

            grantedAuthorities = grantedAuthorities.stream().map(GrantedAuthority::toString)
                    .filter(gas -> clientScopes.contains(gas))
                    .map(gas -> Sw360GrantedAuthority.valueOf(gas)).collect(Collectors.toList());

            return new PreAuthenticatedAuthenticationToken(email, "N/A", grantedAuthorities);
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return active && authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

}
