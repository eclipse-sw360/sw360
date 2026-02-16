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

/**
 * Listener for API response cache events.
 * Implement this interface to react to cache lifecycle events.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Logging cache activity (see {@link LoggingCacheEventListener})</li>
 *   <li>Exporting metrics to monitoring systems (Prometheus, Grafana)</li>
 *   <li>Triggering alerts on errors</li>
 *   <li>Auditing cache operations</li>
 * </ul>
 *
 * <p>Example custom listener for metrics:</p>
 * <pre>
 * {@code
 * @Component
 * public class MetricsCacheEventListener implements CacheEventListener {
 *
 *     private final MeterRegistry registry;
 *
 *     @Override
 *     public void onCacheHit(CachedEndpoint endpoint) {
 *         registry.counter("sw360.cache.hits", "endpoint", endpoint.name()).increment();
 *     }
 *
 *     @Override
 *     public void onCacheMiss(CachedEndpoint endpoint) {
 *         registry.counter("sw360.cache.misses", "endpoint", endpoint.name()).increment();
 *     }
 * }
 * }
 * </pre>
 *
 * <p>All methods have default empty implementations, so listeners can override
 * only the events they care about.</p>
 */
public interface CacheEventListener {

    /**
     * Called when a cache hit occurs (response served from cache).
     * @param endpoint The cached endpoint that was hit
     */
    default void onCacheHit(CachedEndpoint endpoint) {}

    /**
     * Called when a cache miss occurs (no valid cached response available).
     * @param endpoint The cached endpoint that was missed
     */
    default void onCacheMiss(CachedEndpoint endpoint) {}

    /**
     * Called when a cache refresh operation starts.
     * @param endpoint The cached endpoint being refreshed
     */
    default void onCacheRefreshStart(CachedEndpoint endpoint) {}

    /**
     * Called when a cache refresh operation completes successfully.
     * @param endpoint The cached endpoint that was refreshed
     * @param durationMs Time taken to refresh in milliseconds
     */
    default void onCacheRefreshComplete(CachedEndpoint endpoint, long durationMs) {}

    /**
     * Called when a cache refresh operation fails.
     * @param endpoint The cached endpoint that failed to refresh
     * @param e The exception that caused the failure
     */
    default void onCacheRefreshError(CachedEndpoint endpoint, Exception e) {}

    /**
     * Called when a cached response is invalidated (manually or automatically).
     * @param endpoint The cached endpoint that was invalidated
     */
    default void onCacheInvalidated(CachedEndpoint endpoint) {}
}
