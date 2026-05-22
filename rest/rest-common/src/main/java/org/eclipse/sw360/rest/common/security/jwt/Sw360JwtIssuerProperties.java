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
import lombok.Getter;
import lombok.Setter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration properties driving multi-issuer JWT validation in SW360 services.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "sw360.security.jwt")
public class Sw360JwtIssuerProperties {

    private List<JwtIssuer> issuers = new ArrayList<>();

    /**
     * Optional legacy SW360 authorization-server fallback. The legacy
     * authorization-server mints JWTs without an {@code iss} claim, so they
     * cannot be routed through the multi-issuer resolver.
     */
    private LegacyAuthserver legacyAuthserver;

    /**
     * Resolve issuer entries as map keyed by normalized issuer URI.
     */
    public Map<String, JwtIssuer> getEffectiveIssuers() {
        Map<String, JwtIssuer> resolved = new LinkedHashMap<>();
        if (issuers == null) {
            return resolved;
        }

        for (JwtIssuer entry : issuers) {
            if (entry == null || CommonUtils.isNullEmptyOrWhitespace(entry.getIssuerUri())) {
                continue;
            }

            JwtIssuer normalized = new JwtIssuer();
            normalized.setIssuerUri(entry.getIssuerUri().trim());
            String jwks = entry.getJwkSetUri();
            normalized.setJwkSetUri(jwks == null || jwks.isBlank() ? null : jwks.trim());
            resolved.put(normalized.getIssuerUri(), normalized);
        }
        return resolved;
    }

    /**
     * Return the legacy-authserver fallback configuration only when a usable
     * JWKS URI has been supplied. Whitespace-only values are treated as
     * unconfigured.
     */
    public Optional<LegacyAuthserver> getEffectiveLegacyAuthserver() {
        if (legacyAuthserver == null) {
            return Optional.empty();
        }

        String jwks = legacyAuthserver.getJwkSetUri();
        if (CommonUtils.isNullEmptyOrWhitespace(jwks)) {
            return Optional.empty();
        }

        LegacyAuthserver normalized = new LegacyAuthserver();
        normalized.setJwkSetUri(jwks.trim());
        String aud = legacyAuthserver.getExpectedAudience();
        normalized.setExpectedAudience(CommonUtils.isNullEmptyOrWhitespace(aud) ? null : aud.trim());
        return Optional.of(normalized);
    }

    /**
     * Fallback configuration for the legacy SW360 authorization-server, whose
     * issued JWTs are not OIDC-compliant (no {@code iss} claim).
     */
    @Setter
    @Getter
    public static class LegacyAuthserver {
        /**
         * JWKS endpoint URL of the SW360 authorization-server. Required to
         * enable the legacy fallback. Tokens are signature-verified against
         * the keys exposed at this URL.
         */
        private String jwkSetUri;

        /**
         * Optional audience claim that legacy tokens must contain. When set,
         * tokens missing this value in {@code aud} are rejected. SW360
         * authorization-server tokens typically carry {@code sw360-REST-API}.
         */
        private String expectedAudience;
    }
}
