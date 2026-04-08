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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Per-endpoint cache statistics. Returned by admin endpoints.
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheStatistics {

    private CachedEndpoint endpoint;
    private String endpointDescription;
    private String variant;  // NEW: ADMIN, USER, SW360_ADMIN, etc.
    private boolean enabled;
    private boolean cachePresent;
    private String createdAt;
    private String lastAccessTime;
    private long cacheSizeBytes;
    private long ttlSeconds;
    private long maxStaleSeconds;
    private long ageSeconds;
    private boolean stale;
    private boolean expired;
    private long hitCount;
    private long missCount;
    private long writeCount;
    private long errorCount;
    private CacheState state;

    @JsonIgnore
    private String cacheFilePath;

    public double getHitRate() {
        long total = hitCount + missCount;
        return total > 0 ? (double) hitCount / total : 0.0;
    }
}
