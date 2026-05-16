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
import lombok.Setter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties driving multi-issuer JWT validation in the SW360 resource server.
 *
 * <p>{@code sw360.security.jwt.issuers}): list of {@link JwtIssuer} entries,
 * each with an {@code issuer-uri} and an optional {@code jwk-set-uri}. Use
 * {@code jwk-set-uri} to skip discovery and fetch JWKS directly from a loopback
 * / internal URL (typical behind a reverse proxy).
 * </p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sw360.security.jwt")
public class Sw360JwtIssuerProperties {

    private List<JwtIssuer> issuers = new ArrayList<>();

    /**
     * Resolve the effective issuer set as a map keyed by issuer URI, preserving insertion
     * order. {@code null} or blank issuer URIs are skipped.
     */
    public Map<String, JwtIssuer> getEffectiveIssuers() {
        Map<String, JwtIssuer> resolved = new LinkedHashMap<>();
        if (issuers != null) {
            for (JwtIssuer entry : issuers) {
                if (entry == null || CommonUtils.isNullEmptyOrWhitespace(entry.getIssuerUri())) {
                    continue;
                }
                JwtIssuer normalized = new JwtIssuer();
                normalized.setIssuerUri(entry.getIssuerUri().trim());
                String jwks = entry.getJwkSetUri();
                // unset JWKS results into auto-discovery
                normalized.setJwkSetUri(jwks == null || jwks.isBlank() ? null : jwks.trim());
                resolved.put(normalized.getIssuerUri(), normalized);
            }
        }
        return resolved;
    }
}
