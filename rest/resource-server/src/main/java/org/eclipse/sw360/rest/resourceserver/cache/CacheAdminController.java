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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin endpoints for API response cache management.
 * All endpoints require ADMIN authority.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/admin/cache/stats - Get all cache statistics</li>
 *   <li>GET /api/admin/cache/stats/{endpoint} - Get specific endpoint statistics</li>
 *   <li>POST /api/admin/cache/refresh - Refresh all cached endpoints</li>
 *   <li>POST /api/admin/cache/refresh/{endpoint} - Refresh specific endpoint</li>
 *   <li>DELETE /api/admin/cache - Invalidate all caches</li>
 *   <li>DELETE /api/admin/cache/{endpoint} - Invalidate specific endpoint</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/cache")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Cache", description = "API response cache management endpoints (Admin only)")
public class CacheAdminController {

    private static final Logger log = LogManager.getLogger(CacheAdminController.class);

    private final ApiResponseCacheManager cacheManager;
    private final CacheWarmupService warmupService;

    public CacheAdminController(ApiResponseCacheManager cacheManager, CacheWarmupService warmupService) {
        this.cacheManager = cacheManager;
        this.warmupService = warmupService;
    }

    // =========================================================================
    // Statistics Endpoints
    // =========================================================================

    @Operation(
        summary = "Get all cache statistics",
        description = "Returns statistics for all cached API endpoints including hit rates, sizes, and states."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @GetMapping("/stats")
    public ResponseEntity<CacheManagerStatistics> getAllStats() {
        log.debug("Admin request: Get all cache statistics");
        return ResponseEntity.ok(cacheManager.getManagerStatistics());
    }

    @Operation(
        summary = "Get statistics for specific cached endpoint",
        description = "Returns detailed statistics for a single cached API endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid endpoint"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @GetMapping("/stats/{endpoint}")
    public ResponseEntity<CacheStatistics> getEndpointStats(
            @Parameter(description = "Cached endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName) {
        log.debug("Admin request: Get cache statistics for endpoint: {}", endpointName);

        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest().build();
        }

        ResponseCache<?> cache = cacheManager.getCache(endpoint);
        return ResponseEntity.ok(cache.getStatistics());
    }

    // =========================================================================
    // Refresh Endpoints
    // =========================================================================

    @Operation(
        summary = "Refresh all cached endpoints",
        description = "Triggers a refresh for all enabled cached endpoints. Endpoints without response builders are skipped."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresh completed (check results for per-endpoint status)"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        log.info("Admin request: Refresh all cached endpoints");

        Map<String, String> results = new LinkedHashMap<>();

        for (CachedEndpoint endpoint : CachedEndpoint.values()) {
            try {
                ResponseCache<?> cache = cacheManager.getCache(endpoint);
                if (!cache.isEnabled()) {
                    results.put(endpoint.name(), "skipped (disabled)");
                } else if (!cache.hasResponseBuilder()) {
                    results.put(endpoint.name(), "skipped (no response builder)");
                } else {
                    cache.refresh();
                    results.put(endpoint.name(), "success");
                }
            } catch (Exception e) {
                log.error("Failed to refresh cache for endpoint: {}", endpoint, e);
                results.put(endpoint.name(), "error: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "results", results
        ));
    }

    @Operation(
        summary = "Refresh specific cached endpoint",
        description = "Triggers a refresh for a single cached endpoint. Fails if endpoint is disabled or has no response builder."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresh completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid endpoint"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required"),
        @ApiResponse(responseCode = "409", description = "Refresh already in progress"),
        @ApiResponse(responseCode = "500", description = "Refresh failed")
    })
    @PostMapping("/refresh/{endpoint}")
    public ResponseEntity<Map<String, String>> refreshEndpoint(
            @Parameter(description = "Cached endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName) {
        log.info("Admin request: Refresh cache for endpoint: {}", endpointName);

        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid endpoint: " + endpointName));
        }

        try {
            ResponseCache<?> cache = cacheManager.getCache(endpoint);

            if (!cache.isEnabled()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Cache is disabled for endpoint: " + endpoint));
            }

            if (!cache.hasResponseBuilder()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "No response builder registered for endpoint: " + endpoint));
            }

            cache.refresh();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache refresh completed for endpoint: " + endpoint
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to refresh cache for endpoint: {}", endpoint, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Refresh failed: " + e.getMessage()));
        }
    }

    // =========================================================================
    // Invalidation Endpoints
    // =========================================================================

    @Operation(
        summary = "Invalidate all cached endpoints",
        description = "Deletes all cached responses. Caches will be repopulated on next access or warm-up."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All caches invalidated"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @DeleteMapping
    public ResponseEntity<Map<String, String>> invalidateAll() {
        log.info("Admin request: Invalidate all caches");
        cacheManager.invalidateAll();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "All API response caches invalidated"
        ));
    }

    @Operation(
        summary = "Invalidate specific cached endpoint",
        description = "Deletes cached response for a single endpoint. Cache will be repopulated on next access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache invalidated"),
        @ApiResponse(responseCode = "400", description = "Invalid endpoint"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @DeleteMapping("/{endpoint}")
    public ResponseEntity<Map<String, String>> invalidateEndpoint(
            @Parameter(description = "Cached endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName) {
        log.info("Admin request: Invalidate cache for endpoint: {}", endpointName);

        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid endpoint: " + endpointName));
        }

        cacheManager.invalidate(endpoint);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Cache invalidated for endpoint: " + endpoint
        ));
    }

    // =========================================================================
    // Warm-up Endpoints
    // =========================================================================

    @Operation(
        summary = "Trigger cache warm-up",
        description = "Manually triggers cache warm-up for all eligible endpoints. " +
                      "Requires admin authentication as cache building uses the authenticated user context."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warm-up completed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin required")
    })
    @PostMapping("/warmup")
    public ResponseEntity<Map<String, String>> triggerWarmup() {
        log.info("Admin request: Trigger cache warm-up");

        // Run synchronously to preserve authenticated admin context
        // (SW360 does not allow creating fake/system users)
        warmupService.performWarmUp();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Cache warm-up completed"
        ));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Parse endpoint name string to CachedEndpoint enum.
     * Accepts both enum name (RELEASES_ALL_DETAILS) and cache key (releases-all-details).
     */
    private CachedEndpoint parseEndpoint(String endpointName) {
        if (endpointName == null || endpointName.isBlank()) {
            return null;
        }

        // Try exact enum name match
        try {
            return CachedEndpoint.valueOf(endpointName.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Try cache key match
        }

        // Try cache key match
        for (CachedEndpoint endpoint : CachedEndpoint.values()) {
            if (endpoint.getCacheKey().equalsIgnoreCase(endpointName)) {
                return endpoint;
            }
        }

        return null;
    }
}
