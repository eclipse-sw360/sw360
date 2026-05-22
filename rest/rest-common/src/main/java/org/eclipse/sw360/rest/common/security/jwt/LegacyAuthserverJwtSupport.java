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

import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Helpers to build a {@link JwtDecoder} for legacy SW360 authorization-server
 * tokens. These JWTs are not OIDC-compliant: they carry no {@code iss} claim,
 * so default issuer-based validators cannot be applied. Signature verification
 * is performed against the authorization-server's JWKS endpoint, and an
 * optional {@code aud} claim is enforced when configured.
 */
public final class LegacyAuthserverJwtSupport {

    private LegacyAuthserverJwtSupport() {
    }

    /**
     * Build a {@link JwtDecoder} that validates legacy tokens against the
     * supplied JWKS URI. Standard timestamp checks ({@code exp}/{@code nbf})
     * are always enforced. When an expected audience is configured, the
     * {@code aud} claim must contain that value.
     */
    public static JwtDecoder buildJwtDecoder(Sw360JwtIssuerProperties.@NonNull LegacyAuthserver cfg) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(cfg.getJwkSetUri()).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());

        String expectedAudience = cfg.getExpectedAudience();
        if (expectedAudience != null && !expectedAudience.isBlank()) {
            validators.add(new JwtClaimValidator<Collection<String>>(
                    "aud",
                    aud -> aud != null && aud.stream()
                            .filter(Objects::nonNull)
                            .anyMatch(expectedAudience::equals)
            ));
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }
}
