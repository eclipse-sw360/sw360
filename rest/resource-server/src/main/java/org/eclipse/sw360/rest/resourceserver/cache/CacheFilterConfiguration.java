/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Configuration for registering the CacheReadFilter.
 *
 * <p>The filter is registered using FilterRegistrationBean to ensure it runs
 * AFTER Spring Security's filter chain (which runs at order -100).
 * This guarantees SecurityContextHolder contains the authenticated user.</p>
 */
@Configuration
public class CacheFilterConfiguration {

    @Bean
    public FilterRegistrationBean<CacheReadFilter> cacheReadFilterRegistration(
            ApiResponseCacheManager cacheManager,
            CacheVariantResolver variantResolver,
            @Qualifier("cacheExecutor") Executor cacheExecutor,
            List<CacheCondition> conditions) {

        CacheReadFilter filter = new CacheReadFilter(cacheManager, variantResolver, cacheExecutor, conditions);

        FilterRegistrationBean<CacheReadFilter> registration = new FilterRegistrationBean<>(filter);
        // Run after Spring Security filter chain (DEFAULT_FILTER_ORDER = -100)
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        // URL filtering is handled by CacheReadFilter.shouldNotFilter() method
        registration.setName("cacheReadFilter");

        return registration;
    }
}
