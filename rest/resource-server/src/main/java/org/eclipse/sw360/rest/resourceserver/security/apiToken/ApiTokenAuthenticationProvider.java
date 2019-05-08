/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.security.apiToken;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.*;

@Profile("!SECURITY_MOCK")
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = Logger.getLogger(ApiTokenAuthenticationProvider.class);

    @NotNull
    private final Sw360UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.isAuthenticated()) {
            log.trace("Authentication already authenticated");
            return authentication;
        }

        // Get the corresponding sw360 user and restApiToken based on entered token
        String tokenFromAuthentication = (String) authentication.getCredentials();
        String tokenHash = BCrypt.hashpw(tokenFromAuthentication, API_TOKEN_HASH_SALT);
        User sw360User = getUserFromTokenHash(tokenHash);
        Optional<RestApiToken> restApiToken = getApiTokenFromUser(tokenHash, sw360User);

        if (restApiToken.isPresent()) {
            if (!isApiTokenExpired(restApiToken.get())) {
                // User authenticated successfully
                log.trace("Valid token authentication for user: " + sw360User.getEmail());
                return authenticatedApiUser(sw360User, tokenFromAuthentication, restApiToken.get());
            } else {
                throw new CredentialsExpiredException("Your entered API token is expired.");
            }
        } else {
            log.trace("Could not load API token form user " + sw360User.getEmail());
            throw new AuthenticationServiceException("Your entered API token is not valid.");
        }
    }

    private User getUserFromTokenHash(String tokenHash) {
        try {
            return userService.getUserByApiToken(tokenHash);
        } catch (RuntimeException e) {
            log.debug("Could not find any user for the entered token, hash " + tokenHash);
            throw new AuthenticationServiceException("Your entered API token is not valid.");
        }
    }

    private Optional<RestApiToken> getApiTokenFromUser(String tokenHash, User sw360User) {
        return sw360User.getRestApiTokens()
                .stream()
                .filter(t -> t.getToken().equals(tokenHash))
                .findFirst();
    }

    private boolean isApiTokenExpired(RestApiToken restApiToken) {
        String configExpireDays = restApiToken.getAuthorities().contains("WRITE") ?
                API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS : API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
        Date createdOn = SW360Utils.getDateFromTimeString(restApiToken.createdOn);
        Date tokenExpireDate = DateUtils.addDays(createdOn,
                min(restApiToken.getNumberOfDaysValid(), Integer.parseInt(configExpireDays)));
        return tokenExpireDate.before(new Date());
    }

    private Set<GrantedAuthority> getGrantedAuthoritiesFromApiToken(RestApiToken restApiToken) {
        return restApiToken.getAuthorities()
                .stream()
                .map(a -> new SimpleGrantedAuthority(a))
                .collect(Collectors.toSet());
    }

    private PreAuthenticatedAuthenticationToken authenticatedApiUser(User user, String credentials, RestApiToken restApiToken) {
        Set<GrantedAuthority> grantedAuthorities = getGrantedAuthoritiesFromApiToken(restApiToken);
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken =
                new PreAuthenticatedAuthenticationToken(user.getEmail(), credentials, grantedAuthorities);
        preAuthenticatedAuthenticationToken.setAuthenticated(true);
        return preAuthenticatedAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiTokenAuthenticationFilter.ApiTokenAuthentication.class);
    }
}
