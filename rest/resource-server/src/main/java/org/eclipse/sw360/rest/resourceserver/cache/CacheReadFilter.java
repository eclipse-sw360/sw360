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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unified cache filter that handles both cache reads (HIT) and writes (MISS).
 *
 * <p>Flow: Check cacheable endpoint → On HIT: stream from file → On MISS: wrap response,
 * proceed to controller, capture HAL JSON, write to cache asynchronously.</p>
 *
 * <p>Non-cacheable endpoints (e.g., /api/projects) are completely bypassed via
 * {@link #shouldNotFilter(HttpServletRequest)} - zero overhead.</p>
 *
 * <p>This filter is registered via {@link CacheFilterConfiguration} using FilterRegistrationBean
 * with order after Spring Security (DEFAULT_FILTER_ORDER + 1) to ensure SecurityContext is populated.</p>
 */
public class CacheReadFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(CacheReadFilter.class);
    private static final String HAL_JSON = "application/hal+json";
    private static final String REQUIRED_AUTHORITY = "READ";
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;

    /** URL patterns that are potentially cacheable. Must sync with CacheCondition implementations. */
    private static final String[] CACHEABLE_PATH_PATTERNS = {
            "/api/releases"
    };

    private final ApiResponseCacheManager cacheManager;
    private final CacheVariantResolver variantResolver;
    private final Executor cacheExecutor;
    private final Map<CachedEndpoint, CacheCondition> conditionMap;

    public CacheReadFilter(ApiResponseCacheManager cacheManager,
                           CacheVariantResolver variantResolver,
                           Executor cacheExecutor,
                           List<CacheCondition> conditions) {
        this.cacheManager = cacheManager;
        this.variantResolver = variantResolver;
        this.cacheExecutor = cacheExecutor;
        this.conditionMap = conditions != null
                ? conditions.stream().collect(Collectors.toMap(CacheCondition::endpoint, Function.identity()))
                : Collections.emptyMap();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !cacheManager.isEnabled()
                || !"GET".equalsIgnoreCase(request.getMethod())
                || !isPotentiallyCacheablePath(request.getRequestURI());
    }

    private boolean isPotentiallyCacheablePath(String path) {
        if (path == null) {
            return false;
        }
        for (String pattern : CACHEABLE_PATH_PATTERNS) {
            if (path.endsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Step 1: Check if this is a cacheable endpoint (fine-grained check)
        CachedEndpoint endpoint = findCacheableEndpoint(request);
        if (endpoint == null) {
            // Request parameters don't match cacheable conditions - pass through
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Security check - only serve/write cache for authenticated users
        if (!isUserAuthorized()) {
            log.debug("User not authenticated/authorized for cached endpoint {} — proceeding to security chain",
                    endpoint);
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Resolve cache variant (e.g., by user role)
        String variant = cacheManager.isPerRoleCachingEnabled(endpoint)
                ? variantResolver.resolve()
                : ApiResponseCacheManager.DEFAULT_VARIANT;

        // Step 4: Check cache for HIT
        ResponseCache<?> cache = cacheManager.getCache(endpoint, variant);
        Optional<InputStream> cachedStream = cache.getAsStream();

        if (cachedStream.isPresent()) {
            // CACHE HIT - stream directly from cache file (no response wrapping needed)
            serveCachedResponse(response, cachedStream.get(), endpoint, variant);
            return;
        }

        // CACHE MISS - wrap response to capture HAL JSON after serialization
        log.info("Cache MISS for {} variant={} — wrapping response for capture", endpoint, variant);
        handleCacheMiss(request, response, filterChain, endpoint, variant);
    }

    /** Stream cached response directly to the client. */
    private void serveCachedResponse(HttpServletResponse response, InputStream cachedStream,
                                     CachedEndpoint endpoint, String variant) throws IOException {
        log.info("Cache HIT for {} variant={} — streaming from file", endpoint, variant);

        response.setContentType(HAL_JSON);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        try (InputStream in = cachedStream;
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    /** Handle cache MISS: wrap response, proceed to controller, capture and cache the response. */
    private void handleCacheMiss(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain filterChain, CachedEndpoint endpoint, String variant)
            throws ServletException, IOException {

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            if (responseWrapper.getStatus() == HttpServletResponse.SC_OK) {
                byte[] content = responseWrapper.getContentAsByteArray();
                if (content.length > 0) {
                    writeToCacheAsync(endpoint, variant, content);
                } else {
                    log.warn("Empty response body for cache write {} variant={}", endpoint, variant);
                }
            } else {
                log.debug("Not caching response for {} variant={} — status: {}",
                        endpoint, variant, responseWrapper.getStatus());
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    /** Write captured response bytes to cache asynchronously. Skips if cache became fresh. */
    private void writeToCacheAsync(CachedEndpoint endpoint, String variant, byte[] content) {
        log.info("Captured HAL response for {} variant={}: {} bytes — scheduling async cache write",
                endpoint, variant, content.length);

        cacheExecutor.execute(() -> {
            try {
                ResponseCache<Object> cache = cacheManager.getCache(endpoint, variant);

                // Check if cache became FRESH while we were processing
                // This prevents duplicate writes from concurrent requests
                if (isCacheFresh(cache)) {
                    log.debug("Cache already FRESH for {} variant={} — skipping write", endpoint, variant);
                    return;
                }

                log.debug("Starting async cache write for {} variant={}", endpoint, variant);
                cache.putBytes(content);
                log.info("Successfully wrote cache for {} variant={}: {} bytes",
                        endpoint, variant, content.length);
            } catch (Exception e) {
                log.error("Failed async cache write for {} variant={}: {}",
                        endpoint, variant, e.getMessage(), e);
                // Clean up any partial write on failure
                try {
                    cacheManager.getCache(endpoint, variant).invalidate();
                    log.debug("Cleaned up failed cache write for {} variant={}", endpoint, variant);
                } catch (Exception cleanupEx) {
                    log.warn("Failed to cleanup after cache write error: {}", cleanupEx.getMessage());
                }
            }
        });
    }

    private boolean isCacheFresh(ResponseCache<?> cache) {
        try {
            CacheStatistics stats = cache.getStatistics();
            return stats != null
                    && stats.isCachePresent()
                    && !stats.isExpired()
                    && !stats.isStale()
                    && stats.getState() == CacheState.FRESH;
        } catch (Exception e) {
            log.warn("Failed to check cache freshness: {}", e.getMessage());
            return false;
        }
    }

    private boolean isUserAuthorized() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (REQUIRED_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

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
