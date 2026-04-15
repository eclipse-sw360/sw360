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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * Admin endpoints for cache management (requires WRITE authority).
 *
 * <p><strong>IMPORTANT:</strong> {@code @PreAuthorize} is applied at the <em>method level</em>,
 * NOT at the class level. This is because this controller implements
 * {@link RepresentationModelProcessor}, whose {@code process()} method is called by
 * Spring Data REST for <em>every</em> API response (to add links to the root resource).
 * A class-level {@code @PreAuthorize("hasAuthority('WRITE')")} would cause the security
 * proxy to check WRITE authority on the {@code process()} call, resulting in 403 Forbidden
 * for all READ-only users on <em>all</em> endpoints — not just admin endpoints.</p>
 */
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@Tag(name = "Admin - Cache", description = "API response cache management (Admin only)")
public class CacheAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String CACHE_ADMIN_URL = "/admin/cache";
    private static final Logger log = LogManager.getLogger(CacheAdminController.class);

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final ApiResponseCacheManager cacheManager;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(CacheAdminController.class).slash("api" + CACHE_ADMIN_URL).withRel("cacheAdmin"));
        return resource;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(summary = "Get all cache statistics (all endpoints, all variants)")
    @GetMapping(CACHE_ADMIN_URL + "/stats")
    public ResponseEntity<List<CacheStatistics>> getAllStats() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestControllerHelper.throwIfNotAdmin(sw360User);
        log.info("Admin: retrieving all cache statistics");
        return ResponseEntity.ok(cacheManager.getAllVariantStatistics());
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(summary = "Get statistics for all variants of a specific cached endpoint)")
    @GetMapping(CACHE_ADMIN_URL + "/stats/{endpoint}")
    public ResponseEntity<List<CacheStatistics>> getEndpointStats(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestControllerHelper.throwIfNotAdmin(sw360User);
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            log.warn("Admin: invalid endpoint name: {}", endpointName);
            return ResponseEntity.badRequest().build();
        }
        log.info("Admin: retrieving cache statistics for endpoint: {}", endpoint);
        List<CacheStatistics> stats = cacheManager.getEndpointVariantStatistics(endpoint);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(summary = "Invalidate all caches")
    @DeleteMapping(CACHE_ADMIN_URL)
    public ResponseEntity<Map<String, String>> invalidateAll() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestControllerHelper.throwIfNotAdmin(sw360User);
        log.info("Admin: invalidate all caches");
        cacheManager.invalidateAll();
        return ResponseEntity.ok(Map.of("status", "success", "message", "All caches invalidated"));
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(summary = "Invalidate all variants of a cached endpoint")
    @DeleteMapping(CACHE_ADMIN_URL + "/{endpoint}")
    public ResponseEntity<Map<String, String>> invalidateEndpoint(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestControllerHelper.throwIfNotAdmin(sw360User);
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            log.warn("Admin: invalid endpoint name: {}", endpointName);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Invalid endpoint: " + endpointName));
        }
        log.info("Admin: invalidate all variants for {}", endpoint);
        cacheManager.invalidate(endpoint);
        return ResponseEntity.ok(Map.of("status", "success", "message", "All variants invalidated for: " + endpoint));
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(summary = "Invalidate a specific variant of a cached endpoint")
    @DeleteMapping(CACHE_ADMIN_URL + "/{endpoint}/{variant}")
    public ResponseEntity<Map<String, String>> invalidateEndpointVariant(
            @Parameter(description = "Endpoint name (e.g., RELEASES_ALL_DETAILS)")
            @PathVariable("endpoint") String endpointName,
            @Parameter(description = "Variant name (e.g., ADMIN, USER)")
            @PathVariable("variant") String variant
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestControllerHelper.throwIfNotAdmin(sw360User);
        CachedEndpoint endpoint = parseEndpoint(endpointName);
        if (endpoint == null) {
            log.warn("Admin: invalid endpoint name: {}", endpointName);
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
