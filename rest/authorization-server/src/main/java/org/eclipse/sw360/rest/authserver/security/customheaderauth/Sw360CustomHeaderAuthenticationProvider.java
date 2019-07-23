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
import org.eclipse.sw360.rest.authserver.StringTransformer;
import org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;

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
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This {@link AuthenticationProvider} is specialized on requests where the
 * {@link Sw360CustomHeaderAuthenticationFilter} made sure that the user has
 * already been authenticated by some external proxy that set some headers to
 * let us know about the authentication.
 *
 * In addition it is special because it calculates the granted authorities for
 * the user depending on the user's authorities given by his groups and the
 * client's scopes. The result will be the intersection between these two lists.
 * Of course this is only done for an oauth request and not for normal ones
 * (that have nothing to do with clients). And in fact he uses for this task the
 * {@link Sw360GrantedAuthoritiesCalculator}.
 */
public class Sw360CustomHeaderAuthenticationProvider implements AuthenticationProvider {

    private final Logger log = Logger.getLogger(this.getClass());

    @Value("${security.customheader.headername.intermediateauthstore:#{null}}")
    private String customHeaderHeadernameIntermediateAuthStore;

    @Value("${security.customheader.headername.enabled:#{false}}")
    private boolean customHeaderEnabled;

    @Autowired
    private Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator;

    private boolean active;

    @PostConstruct
    public void postSw360CustomHeaderAuthenticationProviderConstruction() {
        if(!customHeaderEnabled) {
            log.info("AuthenticationProvider is NOT active!");
            active = false;
            return;
        }

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
    public boolean supports(Class<?> authentication) {
        return active
                && (authentication.equals(UsernamePasswordAuthenticationToken.class) || authentication.equals(PreAuthenticatedAuthenticationToken.class));
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if(!(authentication.getDetails() instanceof Map<?, ?>)) {
            return null;
        }

        // check if the marker header of our filter is available
        if(!((Map<?, ?>) authentication.getDetails()).containsKey(customHeaderHeadernameIntermediateAuthStore)) {
            return null;
        }
        
        User userDetails = getUserDetails(authentication);
        List<GrantedAuthority> grantedAuthorities = calculateGrantedAuthorities(authentication, userDetails);
        
        return new PreAuthenticatedAuthenticationToken(userDetails.getEmail(), "N/A", grantedAuthorities);
    }

    private User getUserDetails(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        Object externalIds = ((Map<?, ?>)authentication.getDetails()).get(customHeaderHeadernameIntermediateAuthStore);
        String externalId = StringTransformer.transformIntoString(externalIds);
        
        return sw360CustomHeaderUserDetailsProvider.provideUserDetails(email, externalId);
    }

    private List<GrantedAuthority> calculateGrantedAuthorities(Authentication authentication, User userDetails) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            // if we have a UsernamePasswordAuthenticationToken, then we have an OAuth
            // request in which case we only want to keep intersection of user authorities
            // and client scopes
            grantedAuthorities = handleOAuthAuthentication((Map<?, ?>) authentication.getDetails(), userDetails);
        } else {
            // if we have a PreAuthenticationToken (no other case possible, see supports()
            // method), then we have a normal REST request in which case we can grant all
            // authorities calculated from the user profile, so calculate user authorities
            grantedAuthorities = handleRestAuthentication(userDetails.getEmail(), userDetails);
        }

        return grantedAuthorities;
    }

    private List<GrantedAuthority> handleOAuthAuthentication(Map<?, ?> authDetails, User userDetails) {
        String clientId = StringTransformer.transformIntoString(authDetails.get(OAuth2Utils.CLIENT_ID));
        try {
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            log.debug("Found client " + clientDetails + " for id " + clientId + " in authentication details.");

            return sw360UserAndClientAuthoritiesCalculator.mergedAuthoritiesOf(userDetails, clientDetails);
        } catch (ClientRegistrationException e) {
            log.warn("No valid client for id " + clientId + " could be found. It is possible that it is locked,"
                    + " expired, disabled, or invalid for any other reason. So absolutely no authorities granted!");

            return new ArrayList<>();
        }
    }

    private List<GrantedAuthority> handleRestAuthentication(String email, User userDetails) {
        List<GrantedAuthority> grantedAuthorities = 
            sw360UserAndClientAuthoritiesCalculator.generateFromUser(userDetails);

        log.debug("User " + email + " has authorities " + grantedAuthorities
                + " which he will be granted during this request!");

        return grantedAuthorities;
    }

}
