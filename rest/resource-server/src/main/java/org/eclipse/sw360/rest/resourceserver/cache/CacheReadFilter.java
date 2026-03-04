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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servlet filter for serving cached responses on HIT.
 * On MISS, proceeds to controller where {@link CachedResponseBodyAdvice} handles cache writes.
 */
@Component
public class CacheReadFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(CacheReadFilter.class);
    private static final String HAL_JSON = "application/hal+json";

    private final ApiResponseCacheManager cacheManager;
    private final CacheVariantResolver variantResolver;
    private final Map<CachedEndpoint, CacheCondition> conditionMap;

    public CacheReadFilter(ApiResponseCacheManager cacheManager,
                          CacheVariantResolver variantResolver,
                          List<CacheCondition> conditions) {
        this.cacheManager = cacheManager;
        this.variantResolver = variantResolver;
        this.conditionMap = conditions != null
                ? conditions.stream().collect(Collectors.toMap(CacheCondition::endpoint, Function.identity()))
                : Collections.emptyMap();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !cacheManager.isEnabled() || !"GET".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Find a matching enabled endpoint whose CacheCondition passes
        CachedEndpoint endpoint = findCacheableEndpoint(request);
        if (endpoint == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try cache hit
        String variant = cacheManager.isPerRoleCachingEnabled(endpoint)
                ? variantResolver.resolve()
                : ApiResponseCacheManager.DEFAULT_VARIANT;
        ResponseCache<?> cache = cacheManager.getCache(endpoint, variant);
        Optional<String> cachedJson = cache.getAsString();
        if (cachedJson.isPresent()) {
            log.info("Cache HIT for {} variant={} — serving from file", endpoint, variant);
            response.setContentType(HAL_JSON);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(cachedJson.get());
            response.getWriter().flush();
            return;
        }

        log.info("Cache MISS for {} variant={} — proceeding to controller", endpoint, variant);
        filterChain.doFilter(request, response);
    }

    /**
     * Find the first enabled CachedEndpoint whose CacheCondition matches this request.
     */
    private CachedEndpoint findCacheableEndpoint(HttpServletRequest request) {
        for (Map.Entry<CachedEndpoint, CacheCondition> entry : conditionMap.entrySet()) {
            CachedEndpoint endpoint = entry.getKey();
            if (cacheManager.isEndpointEnabled(endpoint) && entry.getValue().isCacheable(request)) {
                return endpoint;
            }
        }
        return null;
    }
}
