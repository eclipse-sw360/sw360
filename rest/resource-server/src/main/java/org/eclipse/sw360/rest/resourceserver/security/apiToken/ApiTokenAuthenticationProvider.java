/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.apiToken;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static java.lang.Math.min;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.*;

@Profile("!SECURITY_MOCK")
@RequiredArgsConstructor
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LogManager.getLogger(ApiTokenAuthenticationProvider.class);

    @NotNull
    private final Sw360UserService userService;

    @Override
    public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        log.debug("Authenticating for the user with authentication {}", authentication);
        if (authentication.isAuthenticated()) {
            log.trace("Authentication already authenticated");
            return authentication;
        }

        // Get the corresponding sw360 user and restApiToken based on entered token
        String tokenFromAuthentication = (String) authentication.getCredentials();
        if (tokenFromAuthentication == null) {
            throw new AuthenticationServiceException("Your entered API token is not valid.");
        }
        String tokenHash = BCrypt.hashpw(tokenFromAuthentication, API_TOKEN_HASH_SALT);
        User sw360User = getUserFromTokenHash(tokenHash);
        if (sw360User == null || sw360User.isDeactivated()) {
            throw new DisabledException("User is deactivated");
        }
        Optional<RestApiToken> restApiToken = getApiTokenFromUser(tokenHash, sw360User);

        if (restApiToken.isPresent()) {
            if (!isApiTokenExpired(restApiToken.get())) {
                // User authenticated successfully
                log.trace("Valid token authentication for user: " + sw360User.getEmail());
                return authenticatedApiUser(sw360User, tokenFromAuthentication, restApiToken.get());
            } else {
                throw new AuthenticationServiceException("Your entered API token is expired.");
            }
        } else {
            log.trace("Could not load API token form user " + sw360User.getEmail());
            throw new AuthenticationServiceException("Your entered API token is not valid.");
        }
    }

    private User getUserFromTokenHash(String tokenHash) {
        try {
            return UserConverter.toThrift(userService.getUserByApiToken(tokenHash));
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
        if (createdOn == null) {
            throw new AuthenticationServiceException("API Token created incorrectly.");
        }
        Date tokenExpireDate = DateUtils.addDays(createdOn,
                min(restApiToken.getNumberOfDaysValid(), Integer.parseInt(configExpireDays)));
        return tokenExpireDate.before(new Date());
    }

    private PreAuthenticatedAuthenticationToken authenticatedApiUser(User user, String credentials, RestApiToken restApiToken) {
        Set<GrantedAuthority> grantedAuthorities = TokenCapabilityAuthorities.mergeForTokenAuthentication(
                Sw360GrantedAuthoritiesCalculator.generateFromUser(user),
                TokenCapabilityAuthorities.fromAuthorityNames(restApiToken.getAuthorities()));
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken =
                new PreAuthenticatedAuthenticationToken(user.getEmail(), credentials, grantedAuthorities);
        preAuthenticatedAuthenticationToken.setDetails(user);
        preAuthenticatedAuthenticationToken.setAuthenticated(true);
        return preAuthenticatedAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiTokenAuthenticationFilter.ApiTokenAuthentication.class);
    }
}
