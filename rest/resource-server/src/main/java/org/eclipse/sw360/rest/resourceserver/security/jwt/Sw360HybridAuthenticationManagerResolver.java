/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.security.jwt;

import com.nimbusds.jwt.JWTParser;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.text.ParseException;

/**
 * Hybrid {@link AuthenticationManagerResolver} that routes incoming bearer
 * tokens based on the presence of an {@code iss} claim:
 * <ul>
 *     <li>Tokens carrying {@code iss} are dispatched to the standard
 *         multi-issuer resolver (Keycloak path).</li>
 *     <li>Tokens without {@code iss} are dispatched to an optional legacy
 *         authentication manager backed by the SW360 authorization-server's
 *         JWKS endpoint.</li>
 * </ul>
 *
 * <p>When no legacy manager is configured and an issuer-less token arrives,
 * the resolver throws {@link InvalidBearerTokenException} to preserve the
 * original "Missing issuer" behavior.
 *
 * <p>Token shape is read with {@link JWTParser} without signature verification;
 * verification still happens inside the resolved {@link AuthenticationManager}.
 * A forged token cannot bypass signature checks via this branching.
 */
public class Sw360HybridAuthenticationManagerResolver
        implements AuthenticationManagerResolver<HttpServletRequest> {

    private static final Logger log = LogManager.getLogger(Sw360HybridAuthenticationManagerResolver.class);

    private final AuthenticationManagerResolver<HttpServletRequest> issuerResolver;
    private final AuthenticationManager legacyAuthenticationManager;
    private final BearerTokenResolver bearerTokenResolver;

    public Sw360HybridAuthenticationManagerResolver(
            @NonNull AuthenticationManagerResolver<HttpServletRequest> issuerResolver,
            @Nullable AuthenticationManager legacyAuthenticationManager
    ) {
        this(issuerResolver, legacyAuthenticationManager, new DefaultBearerTokenResolver());
    }

    Sw360HybridAuthenticationManagerResolver(
            @NonNull AuthenticationManagerResolver<HttpServletRequest> issuerResolver,
            @Nullable AuthenticationManager legacyAuthenticationManager,
            @NonNull BearerTokenResolver bearerTokenResolver
    ) {
        this.issuerResolver = issuerResolver;
        this.legacyAuthenticationManager = legacyAuthenticationManager;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String token;
        try {
            token = bearerTokenResolver.resolve(request);
        } catch (RuntimeException ex) {
            // Let Spring's standard bearer-token error handling apply.
            return issuerResolver.resolve(request);
        }

        if (token == null || token.isBlank()) {
            return issuerResolver.resolve(request);
        }

        String issuer;
        try {
            issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (ParseException ex) {
            log.debug("Failed to parse bearer token for issuer routing; deferring to issuer resolver", ex);
            return issuerResolver.resolve(request);
        }

        if (issuer != null && !issuer.isBlank()) {
            return issuerResolver.resolve(request);
        }

        if (legacyAuthenticationManager == null) {
            throw new InvalidBearerTokenException("Missing issuer");
        }
        return legacyAuthenticationManager;
    }
}
