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
            if (issuer.getIssuerUri() != null && !issuer.getIssuerUri().isBlank()) {
                OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer.getIssuerUri());
                decoder.setJwtValidator(validator);
            }
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
        return resolveTrustedIssuers(properties, fallbackIssuerUri, null);
    }

    /**
     * Resolve trusted issuers with optional fallback issuer-uri and jwk-set-uri.
     */
    public static @NonNull @Unmodifiable Map<String, JwtIssuer> resolveTrustedIssuers(
            @NonNull Sw360JwtIssuerProperties properties, String fallbackIssuerUri, String fallbackJwkSetUri
    ) {
        Map<String, JwtIssuer> issuers = new LinkedHashMap<>(properties.getEffectiveIssuers());
        boolean hasIssuer = fallbackIssuerUri != null && !fallbackIssuerUri.isBlank();
        boolean hasJwkSet = fallbackJwkSetUri != null && !fallbackJwkSetUri.isBlank();
        if (issuers.isEmpty() && (hasIssuer || hasJwkSet)) {
            JwtIssuer fallback = new JwtIssuer();
            if (hasIssuer) {
                fallback.setIssuerUri(fallbackIssuerUri.trim());
            }
            if (hasJwkSet) {
                fallback.setJwkSetUri(fallbackJwkSetUri.trim());
            }
            String key = hasIssuer ? fallback.getIssuerUri() : fallback.getJwkSetUri();
            issuers.put(key, fallback);
        }
        return Map.copyOf(issuers);
    }
}
