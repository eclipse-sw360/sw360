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

import java.io.InputStream;
import java.util.Optional;

/**
 * Cache interface for storing and retrieving API response data.
 * Provides type-safe access to cached responses with streaming support.
 *
 * <p>Main operations:</p>
 * <ul>
 *   <li>{@link #getAsStream()} - Get cached response as InputStream (for streaming to HTTP response)</li>
 *   <li>{@link #getAsString()} - Get cached response as String</li>
 *   <li>{@link #put(String)} - Store JSON response in cache</li>
 *   <li>{@link #invalidate()} - Delete cached response</li>
 *   <li>{@link #refresh()} - Reload cache using registered response builder</li>
 * </ul>
 *
 * <p>Usage in controllers:</p>
 * <pre>
 * {@code
 * ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);
 *
 * // Try to get from cache
 * Optional<InputStream> cached = cache.getAsStream();
 * if (cached.isPresent()) {
 *     return ResponseEntity.ok()
 *         .contentType(MediaType.parseMediaType("application/hal+json"))
 *         .body(new InputStreamResource(cached.get()));
 * }
 *
 * // Cache miss - build and cache
 * if (cache.tryStartRefresh()) {
 *     try {
 *         cache.refresh();
 *         return ResponseEntity.ok()
 *             .body(new InputStreamResource(cache.getAsStream().get()));
 *     } catch (Exception e) {
 *         cache.refreshComplete();
 *         // Fall through to standard response
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> The type of response data stored in this cache
 */
public interface ResponseCache<T> {

    /**
     * Get the cached endpoint this cache belongs to.
     * @return The cached endpoint
     */
    CachedEndpoint endpoint();

    /**
     * Get cached response as InputStream for streaming.
     * Returns empty if cache is disabled, missing, or expired.
     *
     * <p>The returned InputStream should be closed after use.</p>
     *
     * @return Optional containing InputStream if cache hit, empty otherwise
     */
    Optional<InputStream> getAsStream();

    /**
     * Get cached response as String.
     * Returns empty if cache is disabled, missing, or expired.
     *
     * <p>Note: This loads the entire cache into memory. For large caches,
     * prefer {@link #getAsStream()} with streaming.</p>
     *
     * @return Optional containing cached JSON string if cache hit, empty otherwise
     */
    Optional<String> getAsString();

    /**
     * Update cache with new JSON response data.
     * Performs atomic write (temp file + move) to prevent corruption.
     *
     * @param jsonData The JSON response data to cache
     */
    void put(String jsonData);

    /**
     * Invalidate (delete) the cached response.
     * Called when underlying data changes (create/update/delete operations).
     */
    void invalidate();

    /**
     * Check if caching is enabled for this endpoint.
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Check if cache file exists and is valid.
     * @return true if cache is present and usable
     */
    boolean isPresent();

    /**
     * Get comprehensive statistics for this cache.
     * @return CacheStatistics object with current metrics
     */
    CacheStatistics getStatistics();

    /**
     * Try to acquire refresh lock.
     * Used to prevent concurrent refresh operations.
     *
     * @return true if this caller acquired the lock and should perform refresh
     */
    boolean tryStartRefresh();

    /**
     * Release refresh lock.
     * Call this if refresh fails after acquiring lock with {@link #tryStartRefresh()}.
     */
    void refreshComplete();

    /**
     * Refresh cache using the registered response builder.
     * Throws exception if no builder is registered or refresh fails.
     *
     * @throws Exception if refresh fails
     * @throws IllegalStateException if no builder is registered or refresh already in progress
     */
    void refresh() throws Exception;

    /**
     * Register a response builder for this cache.
     * The builder is called during refresh to generate fresh response data.
     *
     * @param responseBuilder The response builder implementation
     */
    void setResponseBuilder(ResponseBuilder<?> responseBuilder);

    /**
     * Check if a response builder is registered for this cache.
     * @return true if a builder is registered
     */
    boolean hasResponseBuilder();
}
