/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.cache.ApiResponseCacheManager;
import org.eclipse.sw360.rest.resourceserver.cache.CachedEndpoint;
import org.eclipse.sw360.rest.resourceserver.cache.ResponseCache;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Handles caching for GET /releases?allDetails=true endpoint.
 *
 * <p><b>Strategy:</b> On cache miss, triggers async background cache build while serving current request.
 * Next request will be fast (served from cache).</p>
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseResponseCacheHandler {

    private static final Logger log = LogManager.getLogger(ReleaseResponseCacheHandler.class);

    private final ApiResponseCacheManager cacheManager;
    private final Sw360ReleaseService releaseService;
    private final Sw360VendorService vendorService;
    @SuppressWarnings("rawtypes")
    private final RestControllerHelper restControllerHelper;
    private final ObjectMapper objectMapper;

    /** Flag to ensure response builder is registered only once */
    private final AtomicBoolean responseBuilderRegistered = new AtomicBoolean(false);

    /**
     * Ensure response builder is registered (lazy initialization).
     * Called on first cache access to avoid @PostConstruct timing issues.
     */
    private void ensureResponseBuilderRegistered() {
        if (responseBuilderRegistered.compareAndSet(false, true)) {
            if (!cacheManager.isEnabled()) {
                log.debug("Cache manager is disabled globally");
                return;
            }

            ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);
            if (!cache.isEnabled()) {
                log.debug("RELEASES_ALL_DETAILS cache is disabled");
                return;
            }

            log.info("Registering response builder for RELEASES_ALL_DETAILS endpoint");
            cache.setResponseBuilder(this::buildAllReleasesResponse);
        }
    }

    /**
     * Check if request is cacheable (allDetails=true with no filters).
     */
    public boolean isCacheableRequest(boolean allDetails, String sha1, String name,
                                       boolean luceneSearch, boolean isNewClearingWithSourceAvailable) {
        if (!allDetails) {
            return false;
        }
        // Lazy registration on first cacheable request
        ensureResponseBuilderRegistered();

        return cacheManager.isEndpointEnabled(CachedEndpoint.RELEASES_ALL_DETAILS)
                && (sha1 == null || sha1.isEmpty())
                && (name == null || name.isEmpty())
                && !luceneSearch
                && !isNewClearingWithSourceAvailable;
    }

    /**
     * Get cached response if available. Returns empty on cache miss.
     * Does NOT build cache on demand - use {@link #triggerAsyncCacheBuild} or admin refresh.
     */
    public Optional<ResponseEntity<?>> getCachedResponse() {
        ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);

        Optional<String> cachedJson = cache.getAsString();
        if (cachedJson.isPresent()) {
            log.debug("Serving releases response from cache");
            // Return raw JSON string - Spring MVC will serialize it correctly as application/hal+json
            // Do NOT wrap in InputStreamResource as Jackson cannot serialize streams
            return Optional.of(ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/hal+json"))
                    .body(cachedJson.get()));
        }

        log.debug("Cache not present for RELEASES_ALL_DETAILS");
        return Optional.empty();
    }

    /**
     * Trigger async cache build in background (non-blocking).
     * Called on cache miss - current request proceeds with slow path, next request will be fast.
     * Skips silently if refresh already in progress.
     */
    @Async("cacheExecutor")
    public CompletableFuture<Void> triggerAsyncCacheBuild(User user) {
        ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);

        // Check if refresh is already in progress
        if (!cache.tryStartRefresh()) {
            log.debug("Cache refresh already in progress, skipping async build");
            return CompletableFuture.completedFuture(null);
        }

        log.info("Starting async cache build for RELEASES_ALL_DETAILS (triggered by cache miss)");
        long startTime = System.currentTimeMillis();

        try {
            // Build the response with the provided user context
            CollectionModel<EntityModel<Release>> response = buildAllReleasesResponseForUser(user);
            String json = objectMapper.writeValueAsString(response);

            // Store in cache
            cache.put(json);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Async cache build completed successfully in {}ms, size: {} bytes",
                    duration, json.length());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Async cache build failed: {}", e.getMessage());
            log.debug("Stack trace:", e);
            cache.refreshComplete();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Invalidate the releases cache.
     * Call this when releases are created, updated, or deleted.
     */
    public void invalidateCache() {
        cacheManager.invalidate(CachedEndpoint.RELEASES_ALL_DETAILS);
    }

    /**
     * Build full releases response (requires authenticated admin user).
     * Called by admin refresh/warmup endpoints.
     */
    private CollectionModel<EntityModel<Release>> buildAllReleasesResponse() throws TException {
        log.debug("Building all releases response with details for cache");

        // Get the authenticated user - must be present and should be admin for cache operations
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (sw360User == null) {
            throw new IllegalStateException("Cache build requires authenticated user context. " +
                    "Use admin cache refresh endpoint or ensure proper authentication.");
        }

        return buildAllReleasesResponseForUser(sw360User);
    }

    /**
     * Build full releases response for specific user (used by sync and async operations).
     */
    private CollectionModel<EntityModel<Release>> buildAllReleasesResponseForUser(User user) throws TException {
        log.debug("Building all releases response for user: {}", user.getEmail());

        // Fetch all releases (no pagination for cache - get all releases)
        List<Release> releases = new ArrayList<>(releaseService.getReleasesForUser(user));
        int totalCount = releases.size();

        log.debug("Loaded {} releases, enriching with details...", totalCount);

        // Enrich with vendor details
        for (Release release : releases) {
            if (!CommonUtils.isNullEmptyOrWhitespace(release.getVendorId())) {
                try {
                    Vendor vendor = vendorService.getVendorById(release.getVendorId());
                    release.setVendor(vendor);
                } catch (RuntimeException e) {
                    log.warn("Unable to find vendor with ID {}", release.getVendorId());
                }
            }
        }

        // Set component-dependent fields
        releaseService.setComponentDependentFieldsInRelease(releases, user);

        // Build HAL resources
        List<EntityModel<Release>> releaseResources = new ArrayList<>();
        for (Release release : releases) {
            EntityModel<Release> resource = createHalReleaseResourceWithAllDetails(release);
            releaseResources.add(resource);
        }

        // Build collection with self link
        Link selfLink = linkTo(ReleaseController.class)
                .slash("api/releases?allDetails=true")
                .withSelfRel();

        return CollectionModel.of(releaseResources, selfLink);
    }

    /**
     * Create HAL resource for release with all details.
     */
    @SuppressWarnings("unchecked")
    private EntityModel<Release> createHalReleaseResourceWithAllDetails(Release release) {
        HalResource<Release> halResource = new HalResource<>(release);

        // Add embedded resources if present
        if (release.getModerators() != null && !release.getModerators().isEmpty()) {
            restControllerHelper.addEmbeddedModerators(halResource, release.getModerators());
        }

        if (release.getAttachments() != null && !release.getAttachments().isEmpty()) {
            restControllerHelper.addEmbeddedAttachments(halResource, release.getAttachments());
        }

        // Add self link
        Link selfLink = linkTo(ReleaseController.class)
                .slash("api/releases")
                .slash(release.getId())
                .withSelfRel();
        halResource.add(selfLink);

        return halResource;
    }
}
