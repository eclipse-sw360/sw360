/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Cross-origin configuration for the archival API.
 *
 * <p>This is not authentication. The service currently trusts the caller and
 * takes the acting user from the X-User-Email header. CORS only decides which
 * browser origins the browser itself will let call the API cross-origin, which
 * matters when the frontend is served from a different host/port than archival
 * (e.g. localhost:3000 calling a standalone service on :8081).
 *
 * <p>In a same-origin deployment no origins need to be configured, so this adds
 * no CORS mapping and the property defaults to empty. Set
 * {@code sw360.archival.cors.allowed-origins} for standalone/dev setups.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${sw360.archival.cors.allowed-origins:}")
    private List<String> allowedOrigins = List.of();

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (origins.isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept",
                        "X-User-Email", "X-User-Department", "X-User-Group")
                .exposedHeaders("Content-Disposition")
                .maxAge(3600);
    }
}
