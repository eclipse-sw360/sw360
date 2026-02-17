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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Service for warming up API response caches.
 * Warm-up requires authenticated admin user - triggered via POST /api/admin/cache/warmup.
 * Cache is also auto-populated on first request with allDetails=true (async background build).
 */
@Service
public class CacheWarmupService {

    private static final Logger log = LogManager.getLogger(CacheWarmupService.class);

    private final ApiResponseCacheManager cacheManager;

    public CacheWarmupService(ApiResponseCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Warm up all eligible caches (enabled, warmup-enabled, expired/missing).
     * Called from {@link CacheAdminController#triggerWarmup()} with ADMIN authority.
     */
    public void performWarmUp() {
        log.info("Starting API response cache warm-up (admin-triggered)...");

        int warmedUp = 0;
        int skipped = 0;
        int failed = 0;

        for (CachedEndpoint endpoint : CachedEndpoint.values()) {
            try {
                ResponseCache<?> cache = cacheManager.getCache(endpoint);
                CacheStatistics stats = cache.getStatistics();

                // Skip if not enabled or warmup not enabled
                if (!stats.isEnabled()) {
                    log.debug("Skipping warm-up for endpoint {} - cache disabled", endpoint);
                    skipped++;
                    continue;
                }

                // Check if warmup is enabled for this endpoint
                if (!endpoint.isDefaultWarmupEnabled()) {
                    log.debug("Skipping warm-up for endpoint {} - warmup disabled", endpoint);
                    skipped++;
                    continue;
                }

                // Skip if cache already present and not expired
                if (cache.isPresent() && !stats.isExpired()) {
                    log.info("Skipping warm-up for endpoint {} - cache already present and valid (age: {}s)",
                        endpoint, stats.getAgeSeconds());
                    skipped++;
                    continue;
                }

                // Check if response builder is registered
                if (!cache.hasResponseBuilder()) {
                    log.warn("Skipping warm-up for endpoint {} - no response builder registered", endpoint);
                    skipped++;
                    continue;
                }

                // Perform warm-up
                log.info("Warming up cache for endpoint: {}", endpoint);
                long startTime = System.currentTimeMillis();
                cache.refresh();
                long duration = System.currentTimeMillis() - startTime;
                log.info("Cache warm-up completed for endpoint: {} in {}ms", endpoint, duration);
                warmedUp++;

            } catch (Exception e) {
                log.error("Cache warm-up failed for endpoint: {}", endpoint, e);
                failed++;
            }
        }

        log.info("API response cache warm-up process completed. Warmed up: {}, Skipped: {}, Failed: {}",
            warmedUp, skipped, failed);
    }
}
