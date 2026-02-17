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

import java.util.Map;

/**
 * Aggregated statistics for the API response cache manager.
 * Provides a summary view across all cached endpoints.
 *
 * <p>Used by:</p>
 * <ul>
 *   <li>{@link CacheAdminController} - GET /api/admin/cache/stats</li>
 *   <li>{@link ApiResponseCacheManager#getManagerStatistics()}</li>
 * </ul>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheManagerStatistics {

    /**
     * Whether caching is globally enabled.
     */
    private boolean globalEnabled;

    /**
     * The cache directory path.
     */
    private String cacheDirectory;

    /**
     * Total number of defined cacheable endpoints.
     */
    private int totalEndpoints;

    /**
     * Number of enabled cacheable endpoints.
     */
    private int enabledEndpoints;

    /**
     * Total cache hits across all endpoints.
     */
    private long totalHits;

    /**
     * Total cache misses across all endpoints.
     */
    private long totalMisses;

    /**
     * Total cache size in bytes across all endpoints.
     */
    private long totalSizeBytes;

    /**
     * Per-endpoint statistics.
     */
    private Map<CachedEndpoint, CacheStatistics> endpointStatistics;

    /**
     * Calculate overall hit rate across all endpoints.
     * @return Hit rate between 0.0 and 1.0
     */
    public double getOverallHitRate() {
        long total = totalHits + totalMisses;
        return total > 0 ? (double) totalHits / total : 0.0;
    }
}
