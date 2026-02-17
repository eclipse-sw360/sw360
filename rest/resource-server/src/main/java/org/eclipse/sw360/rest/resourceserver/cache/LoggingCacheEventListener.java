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
import org.springframework.stereotype.Component;

/**
 * Default cache event listener that logs all events.
 * Registered as a Spring component and auto-injected into {@link ApiResponseCacheManager}.
 *
 * <p>Log levels:</p>
 * <ul>
 *   <li>DEBUG: hits, misses (high frequency)</li>
 *   <li>INFO: refresh start/complete, invalidation</li>
 *   <li>ERROR: refresh failures</li>
 * </ul>
 *
 * <p>To add additional listeners (e.g., for metrics), create another class
 * implementing {@link CacheEventListener} and annotate with @Component.</p>
 */
@Component
public class LoggingCacheEventListener implements CacheEventListener {

    private static final Logger log = LogManager.getLogger(LoggingCacheEventListener.class);

    @Override
    public void onCacheHit(CachedEndpoint endpoint) {
        log.debug("Cache HIT for endpoint: {}", endpoint);
    }

    @Override
    public void onCacheMiss(CachedEndpoint endpoint) {
        log.debug("Cache MISS for endpoint: {}", endpoint);
    }

    @Override
    public void onCacheRefreshStart(CachedEndpoint endpoint) {
        log.info("Cache refresh STARTED for endpoint: {}", endpoint);
    }

    @Override
    public void onCacheRefreshComplete(CachedEndpoint endpoint, long durationMs) {
        log.info("Cache refresh COMPLETED for endpoint: {} in {}ms", endpoint, durationMs);
    }

    @Override
    public void onCacheRefreshError(CachedEndpoint endpoint, Exception e) {
        log.error("Cache refresh FAILED for endpoint: {}", endpoint, e);
    }

    @Override
    public void onCacheInvalidated(CachedEndpoint endpoint) {
        log.info("Cache INVALIDATED for endpoint: {}", endpoint);
    }
}
