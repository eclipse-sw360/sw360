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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for cache management (requires ADMIN authority).
 */
@RestController
@RequestMapping("/api/admin/cache")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Cache", description = "API response cache management (Admin only)")
public class CacheAdminController {

    private static final Logger log = LogManager.getLogger(CacheAdminController.class);

    private final ApiResponseCacheManager cacheManager;

    public CacheAdminController(ApiResponseCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Operation(summary = "Get all cache statistics (all endpoints, all variants)")
    @GetMapping("/stats")
    public ResponseEntity<List<CacheStatistics>> getAllStats() {
        return ResponseEntity.ok(cacheManager.getAllVariantStatistics());
    }

    @Operation(summary = "Get statistics for all variants of a specific cached endpoint")
    @GetMapping("/stats/{endpoint}")
    public ResponseEntity<List<CacheStatistics>> getEndpointStats(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName) {
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest().build();
        }
        List<CacheStatistics> stats = cacheManager.getEndpointVariantStatistics(endpoint);
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Invalidate all caches")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> invalidateAll() {
        log.info("Admin: invalidate all caches");
        cacheManager.invalidateAll();
        return ResponseEntity.ok(Map.of("status", "success", "message", "All caches invalidated"));
    }

    @Operation(summary = "Invalidate all variants of a cached endpoint")
    @DeleteMapping("/{endpoint}")
    public ResponseEntity<Map<String, String>> invalidateEndpoint(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName) {
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid endpoint: " + endpointName));
        }
        log.info("Admin: invalidate all variants for {}", endpoint);
        cacheManager.invalidate(endpoint);
        return ResponseEntity.ok(Map.of("status", "success", "message", "All variants invalidated for: " + endpoint));
    }

    @Operation(summary = "Invalidate a specific variant of a cached endpoint")
    @DeleteMapping("/{endpoint}/{variant}")
    public ResponseEntity<Map<String, String>> invalidateEndpointVariant(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName,
            @Parameter(description = "Variant name (e.g., ADMIN, USER)")
            @PathVariable("variant") String variant) {
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "Invalid endpoint: " + endpointName));
        }
        log.info("Admin: invalidate cache for {} variant={}", endpoint, variant);
        cacheManager.invalidate(endpoint, variant);
        return ResponseEntity.ok(Map.of("status", "success",
                "message", "Cache invalidated for: " + endpoint + " variant: " + variant));
    }

    private CachedEndpoint parseEndpoint(String endpointName) {
        if (endpointName == null || endpointName.isBlank()) {
            return null;
        }
        try {
            return CachedEndpoint.valueOf(endpointName.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // fall through to cache key match
        }
        for (CachedEndpoint ep : CachedEndpoint.values()) {
            if (ep.getCacheKey().equalsIgnoreCase(endpointName)) {
                return ep;
            }
        }

        return null;
    }
}
