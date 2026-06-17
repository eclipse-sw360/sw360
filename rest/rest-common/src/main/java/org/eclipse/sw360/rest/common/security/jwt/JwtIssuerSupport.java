/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security.jwt;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared helpers for multi-issuer JWT decoder wiring.
 */
public final class JwtIssuerSupport {

    private JwtIssuerSupport() {
    }

    /**
     * Build a {@link JwtDecoder} for the given trusted issuer.
     */
    public static JwtDecoder buildJwtDecoder(@NonNull JwtIssuer issuer) {
        if (issuer.getJwkSetUri() != null && !issuer.getJwkSetUri().isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer.getJwkSetUri()).build();
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer.getIssuerUri());
            decoder.setJwtValidator(validator);
            return decoder;
        }
        return JwtDecoders.fromIssuerLocation(issuer.getIssuerUri());
    }

    /**
     * Resolve trusted issuers with optional single-issuer fallback.
     */
    public static @NonNull @Unmodifiable Map<String, JwtIssuer> resolveTrustedIssuers(
            @NonNull Sw360JwtIssuerProperties properties, String fallbackIssuerUri
    ) {
        Map<String, JwtIssuer> issuers = new LinkedHashMap<>(properties.getEffectiveIssuers());
        if (issuers.isEmpty() && fallbackIssuerUri != null && !fallbackIssuerUri.isBlank()) {
            JwtIssuer fallback = new JwtIssuer();
            fallback.setIssuerUri(fallbackIssuerUri.trim());
            issuers.put(fallback.getIssuerUri(), fallback);
        }
        return Map.copyOf(issuers);
    }
}
