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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Statistics for a cached API endpoint response.
 * Returned by admin endpoints and used for operational monitoring.
 *
 * <p>Statistics include:</p>
 * <ul>
 *   <li>Cache state and presence</li>
 *   <li>Hit/miss counters and hit rate</li>
 *   <li>Size and age information</li>
 *   <li>Refresh and error counts</li>
 * </ul>
 *
 * <p>Used by:</p>
 * <ul>
 *   <li>{@link CacheAdminController} - GET /api/admin/cache/stats</li>
 *   <li>{@link ApiResponseCacheManager#getAllStatistics()}</li>
 * </ul>
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheStatistics {

    /**
     * The cached endpoint these statistics are for.
     */
    private CachedEndpoint endpoint;

    /**
     * Description of the cached endpoint.
     */
    private String endpointDescription;

    /**
     * Whether caching is enabled for this endpoint.
     */
    private boolean enabled;

    /**
     * Whether a cache file currently exists.
     */
    private boolean cachePresent;

    /**
     * When the cache was created/last refreshed (ISO-8601 format).
     */
    private String createdAt;

    /**
     * When the cache was last accessed (ISO-8601 format).
     */
    private String lastAccessTime;

    /**
     * Size of the cache file in bytes.
     */
    private long cacheSizeBytes;

    /**
     * Configured TTL in seconds.
     */
    private long ttlSeconds;

    /**
     * Configured max stale time in seconds.
     */
    private long maxStaleSeconds;

    /**
     * Current age of the cache in seconds.
     */
    private long ageSeconds;

    /**
     * Whether the cache is past TTL but within max stale.
     */
    private boolean stale;

    /**
     * Whether the cache is past both TTL and max stale.
     */
    private boolean expired;

    /**
     * Number of cache hits since startup.
     */
    private long hitCount;

    /**
     * Number of cache misses since startup.
     */
    private long missCount;

    /**
     * Number of successful cache refreshes since startup.
     */
    private long refreshCount;

    /**
     * Number of errors since startup.
     */
    private long errorCount;

    /**
     * Path to the cache file.
     */
    private String cacheFilePath;

    /**
     * Current state of the cache.
     */
    private CacheState state;

    /**
     * Calculate the cache hit rate.
     * @return Hit rate between 0.0 and 1.0, or 0.0 if no requests
     */
    public double getHitRate() {
        long total = hitCount + missCount;
        return total > 0 ? (double) hitCount / total : 0.0;
    }
}
