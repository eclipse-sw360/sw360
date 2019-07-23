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

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;

import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

/**
 * A {@link UserDetailsService} that should only be necessary in the
 * refresh_token workflow (but therefore needs to be configured for all oauth2
 * endpoints).
 */
public class Sw360UserDetailsService implements UserDetailsService {

    private final Logger log = Logger.getLogger(this.getClass());

    private Sw360UserDetailsProvider userProvider;

    private Sw360ClientDetailsService clientProvider;

    private Sw360GrantedAuthoritiesCalculator authoritiesCalculator;

    public Sw360UserDetailsService(Sw360UserDetailsProvider userProvider, Sw360ClientDetailsService clientProvider,
            Sw360GrantedAuthoritiesCalculator authoritiesMerger) {
        this.userProvider = userProvider;
        this.clientProvider = clientProvider;
        this.authoritiesCalculator = authoritiesMerger;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails result = null;

        Authentication clientAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (clientAuthentication != null && clientAuthentication instanceof UsernamePasswordAuthenticationToken) {
            String clientId = ((org.springframework.security.core.userdetails.User) clientAuthentication.getPrincipal())
                    .getUsername();
            try {
                ClientDetails clientDetails = clientProvider.loadClientByClientId(clientId);
                log.debug("Sw360ClientDetailsService returned client " + clientDetails + " for id " + clientId
                        + " from authentication details.");

                User user = userProvider.provideUserDetails(username, null);
                log.debug("Sw360UserDetailsProvider returned user " + user);

                if (clientDetails != null && user != null) {
                    result = new org.springframework.security.core.userdetails.User(user.getEmail(),
                            "PreAuthenticatedPassword", authoritiesCalculator.mergedAuthoritiesOf(user, clientDetails));
                }
            } catch (ClientRegistrationException e) {
                log.warn("No valid client for id " + clientId + " could be found. It is possible that it is "
                        + "locked, expired, disabled, or invalid for any other reason.");
                throw new UsernameNotFoundException("We cannot provide UserDetails for an invalid client: ", e);
            }
        } else {
            log.warn("Called in unwanted case: " + clientAuthentication);
        }

        if (result != null) {
            return result;
        } else {
            throw new UsernameNotFoundException("No user with username " + username + " found in sw360 users.");
        }
    }

}
