/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.customheaderauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.StringTransformer;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * This {@link AuthenticationProvider} is specialized on requests where the
 * {@link Sw360CustomHeaderAuthenticationFilter} made sure that the user has already been
 * authenticated by some external proxy that set some headers to let us know about the
 * authentication.
 *
 * In addition it is special because it calculates the granted authorities for the user depending on
 * the user's authorities given by his groups and the client's scopes. The result will be the
 * intersection between these two lists. Of course this is only done for an oauth request and not
 * for normal ones (that have nothing to do with clients). And in fact he uses for this task the
 * {@link Sw360GrantedAuthoritiesCalculator}.
 */
// @Component
public class Sw360CustomHeaderAuthenticationProvider implements AuthenticationProvider {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Value("${security.customheader.headername.intermediateauthstore:#{null}}")
    private String customHeaderHeadernameIntermediateAuthStore;

    @Value("${security.customheader.headername.enabled:#{false}}")
    private boolean customHeaderEnabled;

    private final Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider;

    private final RegisteredClientRepository clientDetailsService;

    private final Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator;

    public Sw360CustomHeaderAuthenticationProvider(
            Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider,
            RegisteredClientRepository clientDetailsService,
            Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator
    ) {
        this.sw360CustomHeaderUserDetailsProvider = sw360CustomHeaderUserDetailsProvider;
        this.clientDetailsService = clientDetailsService;
        this.sw360UserAndClientAuthoritiesCalculator = sw360UserAndClientAuthoritiesCalculator;
    }

    private boolean active;

    @PostConstruct
    public void postSw360CustomHeaderAuthenticationProviderConstruction() {
        if (!customHeaderEnabled) {
            log.info("AuthenticationProvider is NOT active!");
            active = false;
            return;
        }

        if (StringUtils.isEmpty(customHeaderHeadernameIntermediateAuthStore)) {
            log.warn(
                    "AuthenticationProvider is NOT active! Some configuration is missing. Needed config keys:\n"
                            + "- security.customheader.headername.intermediateauthstore");
            active = false;
        } else {
            log.info("AuthenticationProvider is active!");
            active = true;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return active && (authentication.equals(UsernamePasswordAuthenticationToken.class)
                || authentication.equals(PreAuthenticatedAuthenticationToken.class));
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        if (!(authentication.getDetails() instanceof Map<?, ?>)) {
            return null;
        }

        // check if the marker header of our filter is available
        if (!((Map<?, ?>) authentication.getDetails())
                .containsKey(customHeaderHeadernameIntermediateAuthStore)) {
            return null;
        }

        User userDetails = getUserDetails(authentication);
        List<GrantedAuthority> grantedAuthorities =
                calculateGrantedAuthorities(authentication, userDetails);

        return new PreAuthenticatedAuthenticationToken(userDetails.getEmail(), "N/A",
                grantedAuthorities);
    }

    private User getUserDetails(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        Object externalIds = ((Map<?, ?>) authentication.getDetails())
                .get(customHeaderHeadernameIntermediateAuthStore);
        String externalId = StringTransformer.transformIntoString(externalIds);

        return sw360CustomHeaderUserDetailsProvider.provideUserDetails(email, externalId);
    }

    private List<GrantedAuthority> calculateGrantedAuthorities(Authentication authentication,
            User userDetails) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            // if we have a UsernamePasswordAuthenticationToken, then we have an OAuth
            // request in which case we only want to keep intersection of user authorities
            // and client scopes
            grantedAuthorities =
                    handleOAuthAuthentication((Map<?, ?>) authentication.getDetails(), userDetails);
        } else {
            // if we have a PreAuthenticationToken (no other case possible, see supports()
            // method), then we have a normal REST request in which case we can grant all
            // authorities calculated from the user profile, so calculate user authorities
            grantedAuthorities = handleRestAuthentication(userDetails.getEmail(), userDetails);
        }

        return grantedAuthorities;
    }

    private List<GrantedAuthority> handleOAuthAuthentication(Map<?, ?> authDetails,
            User userDetails) {
        String clientId = StringTransformer.transformIntoString(authDetails.get("client_id"));
        try {
            RegisteredClient clientDetails = clientDetailsService.findByClientId(clientId);

            log.debug("Found client " + clientDetails + " for id " + clientId
                    + " in authentication details.");

            return sw360UserAndClientAuthoritiesCalculator.mergedAuthoritiesOf(userDetails,
                    clientDetails);
        } catch (Exception e) {
            log.warn("No valid client for id " + clientId
                    + " could be found. It is possible that it is locked,"
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
