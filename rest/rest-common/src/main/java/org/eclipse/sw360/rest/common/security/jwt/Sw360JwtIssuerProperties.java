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

/**
 * Configuration properties driving multi-issuer JWT validation in SW360 services.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "sw360.security.jwt")
public class Sw360JwtIssuerProperties {

    private List<JwtIssuer> issuers = new ArrayList<>();

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
}
