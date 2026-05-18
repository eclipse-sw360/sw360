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

/**
 * Structured trusted-issuer entry for SW360 JWT validation.
 */
@Setter
@Getter
public class JwtIssuer {

    /**
     * Public issuer URL exactly as it appears in incoming token {@code iss} claim.
     */
    private String issuerUri;

    /**
     * Optional JWKS endpoint URL. When set, OIDC discovery is skipped.
     */
    private String jwkSetUri;

    public JwtIssuer() {
    }

}
