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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserAccess;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter.ApiTokenAuthentication;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter.AuthType;
import org.eclipse.sw360.rest.resourceserver.security.jwksvalidation.JWTValidator;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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
import java.util.stream.Stream;

import static java.lang.Math.min;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.*;

@Profile("!SECURITY_MOCK")
@Component
@RequiredArgsConstructor
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LogManager.getLogger(ApiTokenAuthenticationProvider.class);

    @NotNull
    private final Sw360UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.info("Authenticating for the user with authentication {}", authentication);
        if (authentication.isAuthenticated()) {
            log.trace("Authentication already authenticated");
            return authentication;
        }

        // Get the corresponding sw360 user and restApiToken based on entered token
        String tokenFromAuthentication = (String) authentication.getCredentials();
        if (Sw360ResourceServer.IS_JWKS_VALIDATION_ENABLED && authentication instanceof ApiTokenAuthentication
                && ((ApiTokenAuthentication) authentication).getType() == AuthType.JWKS) {
            JWTValidator validator = new JWTValidator(Sw360ResourceServer.JWKS_ISSUER_URL,
                    Sw360ResourceServer.JWKS_ENDPOINT_URL, Sw360ResourceServer.JWT_CLAIM_AUD);
            JwtClaims jwtClaims = null;
            try {
                jwtClaims = validator.validateJWT(tokenFromAuthentication);
            } catch (InvalidJwtException exp) {
                throw new BadCredentialsException(exp.getMessage());
            }
            Object clientIdAsObject = jwtClaims.getClaimValue("client_id");
            if (clientIdAsObject == null || clientIdAsObject.toString().isBlank()) {
                throw new BadCredentialsException("Client Id cannot be null or empty");
            }

            String clientIdAsStr = clientIdAsObject.toString();
            User sw360User = getUserFromClientId(clientIdAsStr);
            return authenticatedOidcUser(sw360User, clientIdAsStr);
        } else {
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
    }

    private User getUserFromTokenHash(String tokenHash) {
        try {
            return userService.getUserByApiToken(tokenHash);
        } catch (RuntimeException e) {
            log.debug("Could not find any user for the entered token, hash " + tokenHash);
            throw new AuthenticationServiceException("Your entered API token is not valid.");
        }
    }

    private User getUserFromClientId(String clientId) {
        try {
            return userService.getUserFromClientId(clientId);
        } catch (RuntimeException e) {
            log.debug("Could not find any user for the entered clientId " + clientId);
            throw new AuthenticationServiceException(
                    "Your entered OIDC token is not associated with any user for authorization.");
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
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private Set<GrantedAuthority> getGrantedAuthoritiesFromUserAccess(UserAccess userAccess) {
        return Stream.of(userAccess.name().split("_"))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private PreAuthenticatedAuthenticationToken authenticatedApiUser(User user, String credentials, RestApiToken restApiToken) {
        Set<GrantedAuthority> grantedAuthorities = getGrantedAuthoritiesFromApiToken(restApiToken);
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken =
                new PreAuthenticatedAuthenticationToken(user.getEmail(), credentials, grantedAuthorities);
        preAuthenticatedAuthenticationToken.setAuthenticated(true);
        return preAuthenticatedAuthenticationToken;
    }

    private PreAuthenticatedAuthenticationToken authenticatedOidcUser(User user, String credentials) {
        Set<GrantedAuthority> grantedAuthorities = getGrantedAuthoritiesFromUserAccess(
                user.getOidcClientInfos().get(credentials).getAccess());
        PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = new PreAuthenticatedAuthenticationToken(
                user.getEmail(), credentials, grantedAuthorities);
        preAuthenticatedAuthenticationToken.setAuthenticated(true);
        return preAuthenticatedAuthenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ApiTokenAuthenticationFilter.ApiTokenAuthentication.class);
    }
}
