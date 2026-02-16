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

import lombok.Getter;

/**
 * Enumeration of all cacheable REST API endpoints in SW360.
 * Each entry represents an API endpoint whose response can be cached.
 *
 * <p>To add a new cacheable endpoint:</p>
 * <ol>
 *   <li>Add a new enum constant with appropriate defaults</li>
 *   <li>Register a ResponseBuilder in the respective controller</li>
 *   <li>Add cache check logic in the controller's GET method</li>
 *   <li>Add cache invalidation in POST/PATCH/DELETE methods</li>
 * </ol>
 *
 * <p>Example for adding COMPONENTS_ALL_DETAILS:</p>
 * <pre>
 * COMPONENTS_ALL_DETAILS(
 *     "components-all-details",
 *     "GET /components?allDetails=true",
 *     86400,  // 24 hours TTL
 *     300,    // 5 min max stale
 *     false   // warm-up disabled by default
 * )
 * </pre>
 */
@Getter
public enum CachedEndpoint {

    /**
     * Cache for GET /releases?allDetails=true endpoint.
     * This is the primary use case - caches all releases with full details.
     * Used by automation scripts that need complete release data.
     */
    RELEASES_ALL_DETAILS(
        "releases-all-details",
        "GET /releases?allDetails=true",
        86400,  // 24 hours TTL (per Div Sync requirement)
        300,    // 5 min max stale while refreshing
        true    // warm-up enabled - pre-populate on startup
    );

    // =========================================================================
    // FUTURE CACHEABLE ENDPOINTS (examples for reference)
    // =========================================================================
    //
    // COMPONENTS_ALL_DETAILS(
    //     "components-all-details",
    //     "GET /components?allDetails=true",
    //     86400,  // 24 hours TTL
    //     300,    // 5 min max stale
    //     false   // warm-up disabled - enable if needed
    // ),
    //
    // PROJECTS_ALL_DETAILS(
    //     "projects-all-details",
    //     "GET /projects?allDetails=true",
    //     43200,  // 12 hours TTL (projects change more frequently)
    //     300,    // 5 min max stale
    //     false   // warm-up disabled
    // ),
    //
    // LICENSES_ALL(
    //     "licenses-all",
    //     "GET /licenses",
    //     604800, // 7 days TTL (licenses rarely change)
    //     600,    // 10 min max stale
    //     true    // warm-up enabled - licenses are frequently accessed
    // ),
    //
    // VENDORS_ALL(
    //     "vendors-all",
    //     "GET /vendors",
    //     86400,  // 24 hours TTL
    //     300,    // 5 min max stale
    //     false   // warm-up disabled
    // );
    // =========================================================================

    /**
     * Unique cache key for this endpoint.
     * Used to construct cache file path: {cacheDirectory}/{cacheKey}.json
     */
    private final String cacheKey;

    /**
     * Human-readable description of the cached endpoint.
     */
    private final String endpointDescription;

    /**
     * Default TTL (Time-To-Live) in seconds.
     * Can be overridden via properties: rest.cache.{cacheKey}.ttl.seconds
     */
    private final long defaultTtlSeconds;

    /**
     * Default max stale time in seconds.
     * Stale cache can be served while refresh is in progress.
     * Can be overridden via properties: rest.cache.{cacheKey}.max.stale.seconds
     */
    private final long defaultMaxStaleSeconds;

    /**
     * Whether warm-up is enabled by default for this endpoint.
     * When enabled, cache is populated on application startup.
     * Can be overridden via properties: rest.cache.{cacheKey}.warmup.enabled
     */
    private final boolean defaultWarmupEnabled;

    CachedEndpoint(String cacheKey, String endpointDescription, long defaultTtlSeconds,
                   long defaultMaxStaleSeconds, boolean defaultWarmupEnabled) {
        this.cacheKey = cacheKey;
        this.endpointDescription = endpointDescription;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.defaultMaxStaleSeconds = defaultMaxStaleSeconds;
        this.defaultWarmupEnabled = defaultWarmupEnabled;
    }

    /**
     * Get the full cache filename with .json extension.
     */
    public String getCacheFileName() {
        return cacheKey + ".json";
    }

    /**
     * Get the metadata filename with .meta extension.
     */
    public String getMetaFileName() {
        return cacheKey + ".meta";
    }

    /**
     * Get the property key prefix for this endpoint.
     * Example: "rest.cache.releases-all-details"
     */
    public String getPropertyPrefix() {
        return "rest.cache." + cacheKey;
    }
}
