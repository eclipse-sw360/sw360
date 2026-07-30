/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Registers {@link BackendInternalAuthFilter} for every backend Spring Boot WAR that
 * depends on backend-common (via Spring Boot auto-configuration).
 */
@AutoConfiguration
public class BackendInternalAuthAutoConfiguration {

    @Bean
    public FilterRegistrationBean<BackendInternalAuthFilter> backendInternalAuthFilter() {
        FilterRegistrationBean<BackendInternalAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new BackendInternalAuthFilter());
        registration.addUrlPatterns("/*");
        registration.setName("backendInternalAuthFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
