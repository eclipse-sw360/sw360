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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structured trusted-issuer entry for the SW360 resource server.
 *
 * <p>When {@link #jwkSetUri} is left {@code null} or blank, the resource server falls
 * back to OpenID Connect discovery against {@link #issuerUri} (via
 * {@code JwtDecoders.fromIssuerLocation(...)}). This requires the issuer URL to be
 * reachable from the resource server and its TLS certificate chain to be trusted by
 * the JVM truststore.</p>
 *
 * <p>When {@link #jwkSetUri} is supplied, discovery is skipped entirely: a
 * {@code NimbusJwtDecoder} is built directly against the JWKS endpoint, and the issuer
 * claim is validated against {@link #issuerUri}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class JwtIssuer {

    /**
     * Public issuer URL exactly as it appears in the JWT {@code iss} claim. This value is
     * used as the lookup key when validating incoming tokens.
     */
    private String issuerUri;

    /**
     * Optional JWKS endpoint URL. When set, discovery is skipped and the JWKS is fetched
     * directly from this URL.
     */
    private String jwkSetUri;
}
