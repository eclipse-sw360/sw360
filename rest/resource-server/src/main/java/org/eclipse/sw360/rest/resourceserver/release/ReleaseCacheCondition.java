/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.resourceserver.cache.CacheCondition;
import org.eclipse.sw360.rest.resourceserver.cache.CachedEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Release endpoint cache conditions.
 *
 * <p>This class uses {@code @Configuration} with {@code @Bean} methods to allow
 * multiple cache conditions for the same or different Release endpoints.</p>
 *
 * <p><strong>Why @Configuration instead of @Component?</strong></p>
 * <p>Using @Bean methods allows us to define multiple cache conditions in one place,
 * for example: one for {@code allDetails=true}, another for {@code allDetails=false},
 * or different filter combinations - all without creating separate classes.</p>
 *
 * <p>To add a new cached Release endpoint:</p>
 * <ol>
 *   <li>Add the endpoint to {@link CachedEndpoint} enum</li>
 *   <li>Add a new {@code @Bean} method below following the same pattern</li>
 *   <li>Annotate the controller method with {@code @CachedResponse(endpoints = {CachedEndpoint.YOUR_ENDPOINT})}</li>
 *   <li>Invalidate cache in mutation methods (POST/PATCH/DELETE)</li>
 *   <li>Add configuration in sw360.properties</li>
 * </ol>
 */
@Configuration
public class ReleaseCacheCondition {

    /**
     * Check if request has no filters applied (sha1, name, luceneSearch, or newClearing).
     * Shared validation logic for all release cache conditions.
     */
    private static boolean hasNoFilters(HttpServletRequest request) {
        return CommonUtils.isNullEmptyOrWhitespace(request.getParameter("sha1"))
                && CommonUtils.isNullEmptyOrWhitespace(request.getParameter("name"))
                && !"true".equalsIgnoreCase(request.getParameter("luceneSearch"))
                && !"true".equalsIgnoreCase(request.getParameter("isNewClearingWithSourceAvailable"));
    }

    /**
     * Check if request path matches the releases endpoint.
     * Ensures cache conditions only apply to /api/releases, not other endpoints.
     */
    private static boolean isReleasesEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.endsWith("/api/releases");
    }

    /**
     * Cache condition for {@code GET /releases?allDetails=true}.
     * Cacheable only when allDetails=true and no filters are applied.
     */
    @Bean
    public CacheCondition releasesAllDetailsCacheCondition() {
        return new CacheCondition() {
            @Override
            public CachedEndpoint endpoint() {
                return CachedEndpoint.RELEASES_ALL_DETAILS;
            }

            @Override
            public boolean isCacheable(HttpServletRequest request) {
                return isReleasesEndpoint(request)
                        && "true".equalsIgnoreCase(request.getParameter("allDetails"))
                        && hasNoFilters(request);
            }
        };
    }

    /**
     * Cache condition for {@code GET /releases} without allDetails or filters.
     * Cacheable only when allDetails is not set to true and no filters are applied.
     */
    @Bean
    public CacheCondition releasesWithoutDetailsCacheCondition() {
        return new CacheCondition() {
            @Override
            public CachedEndpoint endpoint() {
                return CachedEndpoint.RELEASES_WITHOUT_DETAILS;
            }

            @Override
            public boolean isCacheable(HttpServletRequest request) {
                // Cacheable when allDetails is false, null, or not present
                String allDetails = request.getParameter("allDetails");
                return isReleasesEndpoint(request)
                        && (allDetails == null || "false".equalsIgnoreCase(allDetails))
                        && hasNoFilters(request);
            }
        };
    }
}
