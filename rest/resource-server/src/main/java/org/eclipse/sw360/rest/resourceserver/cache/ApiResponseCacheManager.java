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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for SW360 API response caching.
 * Manages all cacheable endpoints and provides unified access.
 *
 * <p>This is the main entry point for cache operations:</p>
 * <ul>
 *   <li>Get cache instance: {@link #getCache(CachedEndpoint)}</li>
 *   <li>Get statistics: {@link #getManagerStatistics()}</li>
 *   <li>Invalidate caches: {@link #invalidate(CachedEndpoint)}, {@link #invalidateAll()}</li>
 * </ul>
 *
 * <p>Configuration (sw360.properties):</p>
 * <pre>
 * # Global settings
 * rest.cache.enabled=false                    # Master switch (default: false for community)
 * rest.cache.directory=/var/sw360/cache       # Cache directory
 *
 * # Per-endpoint settings (example for releases-all-details)
 * rest.cache.releases-all-details.enabled=true
 * rest.cache.releases-all-details.ttl.seconds=86400
 * rest.cache.releases-all-details.max.stale.seconds=300
 * rest.cache.releases-all-details.warmup.enabled=true
 * </pre>
 *
 * <p>Usage in controllers:</p>
 * <pre>
 * {@code
 * @Autowired
 * private ApiResponseCacheManager cacheManager;
 *
 * // Get cache for an endpoint
 * ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);
 *
 * // Invalidate on data change
 * cacheManager.invalidate(CachedEndpoint.RELEASES_ALL_DETAILS);
 * }
 * </pre>
 */
@Service
public class ApiResponseCacheManager {

    private static final Logger log = LogManager.getLogger(ApiResponseCacheManager.class);

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String CONFIG_GLOBAL_ENABLED = "rest.cache.enabled";
    private static final String CONFIG_GLOBAL_DIRECTORY = "rest.cache.directory";
    private static final String DEFAULT_CACHE_DIRECTORY = "/var/sw360/cache";

    private final Map<CachedEndpoint, ResponseCache<?>> caches = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final List<CacheEventListener> listeners;

    /** Flag to ensure initialization happens only once */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private Properties cacheProperties;
    private boolean globalCacheEnabled;
    private String cacheDirectory;

    @Autowired
    public ApiResponseCacheManager(ObjectMapper objectMapper,
                                   @Autowired(required = false) List<CacheEventListener> listeners) {
        // Ensure ObjectMapper supports Java 8 date/time types (Instant, LocalDateTime, etc.)
        // Spring Boot usually auto-configures this, but we ensure it explicitly for cache serialization
        objectMapper.findAndRegisterModules();

        this.objectMapper = objectMapper;
        this.listeners = listeners != null ? listeners : Collections.emptyList();
    }

    /**
     * Ensure cache manager is initialized (lazy initialization).
     * Thread-safe, called on first access to any cache operation.
     */
    private void ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            // Load configuration from sw360.properties
            // Loads from bundled resource first, then overrides from SYSTEM_CONFIGURATION_PATH
            cacheProperties = CommonUtils.loadProperties(ApiResponseCacheManager.class, SW360_PROPERTIES_FILE_PATH, true);

            // Load global configuration
            this.globalCacheEnabled = Boolean.parseBoolean(
                cacheProperties.getProperty(CONFIG_GLOBAL_ENABLED, "false").trim());
            this.cacheDirectory = cacheProperties.getProperty(CONFIG_GLOBAL_DIRECTORY, DEFAULT_CACHE_DIRECTORY);

            if (this.cacheDirectory == null || this.cacheDirectory.trim().isEmpty()) {
                this.cacheDirectory = DEFAULT_CACHE_DIRECTORY;
            }

            if (!globalCacheEnabled) {
                log.info("SW360 API Response Cache is disabled (set {}=true to enable)", CONFIG_GLOBAL_ENABLED);
                return;
            }

            log.info("Initializing API Response Cache Manager (directory: {})", cacheDirectory);

            // Initialize cache for each endpoint
            for (CachedEndpoint endpoint : CachedEndpoint.values()) {
                CacheConfiguration config = loadConfiguration(endpoint);
                ResponseCache<?> cache = new FileBasedResponseCache<>(endpoint, config, objectMapper, listeners);
                caches.put(endpoint, cache);
                log.info("Cache endpoint {} initialized (enabled: {}, ttl: {}s)",
                    endpoint, config.isEnabled(), config.getTtlSeconds());
            }
        }
    }

    /**
     * Load configuration for a cached endpoint from properties.
     */
    private CacheConfiguration loadConfiguration(CachedEndpoint endpoint) {
        String prefix = endpoint.getPropertyPrefix() + ".";

        boolean enabled = globalCacheEnabled &&
            Boolean.parseBoolean(cacheProperties.getProperty(prefix + "enabled", "false"));
        long ttlSeconds = Long.parseLong(
            cacheProperties.getProperty(prefix + "ttl.seconds", String.valueOf(endpoint.getDefaultTtlSeconds())));
        long maxStaleSeconds = Long.parseLong(
            cacheProperties.getProperty(prefix + "max.stale.seconds", String.valueOf(endpoint.getDefaultMaxStaleSeconds())));
        boolean warmupEnabled = Boolean.parseBoolean(
            cacheProperties.getProperty(prefix + "warmup.enabled", String.valueOf(endpoint.isDefaultWarmupEnabled())));

        return CacheConfiguration.builder()
            .endpoint(endpoint)
            .enabled(enabled)
            .ttlSeconds(ttlSeconds)
            .maxStaleSeconds(maxStaleSeconds)
            .warmupEnabled(warmupEnabled)
            .cacheDirectory(Paths.get(cacheDirectory))
            .build();
    }

    /**
     * Get cache for a specific endpoint.
     *
     * @param endpoint The cached endpoint
     * @return ResponseCache instance (may be disabled)
     * @throws IllegalArgumentException if endpoint is unknown
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseCache<T> getCache(CachedEndpoint endpoint) {
        ensureInitialized();
        ResponseCache<?> cache = caches.get(endpoint);
        if (cache == null) {
            // Return a disabled cache if not initialized
            log.warn("Cache not initialized for endpoint: {}. Returning disabled cache.", endpoint);
            return new DisabledResponseCache<>(endpoint);
        }
        return (ResponseCache<T>) cache;
    }

    /**
     * Check if caching is enabled for a specific endpoint.
     * Useful for checking before attempting operations.
     *
     * @param endpoint The cached endpoint
     * @return true if the endpoint cache is enabled
     */
    public boolean isEndpointEnabled(CachedEndpoint endpoint) {
        ensureInitialized();
        ResponseCache<?> cache = caches.get(endpoint);
        return cache != null && cache.isEnabled();
    }

    /**
     * Get statistics for all cached endpoints.
     *
     * @return Map of endpoint to statistics
     */
    public Map<CachedEndpoint, CacheStatistics> getAllStatistics() {
        ensureInitialized();
        Map<CachedEndpoint, CacheStatistics> stats = new LinkedHashMap<>();
        for (CachedEndpoint endpoint : CachedEndpoint.values()) {
            ResponseCache<?> cache = caches.get(endpoint);
            if (cache != null) {
                stats.put(endpoint, cache.getStatistics());
            }
        }
        return stats;
    }

    /**
     * Get aggregated statistics summary.
     *
     * @return CacheManagerStatistics with overall metrics
     */
    public CacheManagerStatistics getManagerStatistics() {
        ensureInitialized();
        Collection<CacheStatistics> allStats = getAllStatistics().values();

        return CacheManagerStatistics.builder()
            .globalEnabled(globalCacheEnabled)
            .cacheDirectory(cacheDirectory)
            .totalEndpoints(CachedEndpoint.values().length)
            .enabledEndpoints((int) allStats.stream().filter(CacheStatistics::isEnabled).count())
            .totalHits(allStats.stream().mapToLong(CacheStatistics::getHitCount).sum())
            .totalMisses(allStats.stream().mapToLong(CacheStatistics::getMissCount).sum())
            .totalSizeBytes(allStats.stream().mapToLong(CacheStatistics::getCacheSizeBytes).sum())
            .endpointStatistics(getAllStatistics())
            .build();
    }

    /**
     * Invalidate all caches.
     * Call this when major data changes occur that affect multiple endpoints.
     */
    public void invalidateAll() {
        caches.values().forEach(ResponseCache::invalidate);
        log.info("All API response caches invalidated");
    }

    /**
     * Invalidate specific endpoint cache.
     * Call this when data for that endpoint changes.
     *
     * <p>Example usage in controller:</p>
     * <pre>
     * {@code
     * @PostMapping("/releases")
     * public ResponseEntity<?> createRelease(...) {
     *     // ... create release ...
     *     cacheManager.invalidate(CachedEndpoint.RELEASES_ALL_DETAILS);
     *     return response;
     * }
     * }
     * </pre>
     *
     * @param endpoint The cached endpoint to invalidate
     */
    public void invalidate(CachedEndpoint endpoint) {
        ensureInitialized();
        ResponseCache<?> cache = caches.get(endpoint);
        if (cache != null) {
            cache.invalidate();
        }
    }

    /**
     * Check if caching is globally enabled.
     *
     * @return true if global caching is enabled
     */
    public boolean isEnabled() {
        return globalCacheEnabled;
    }
}
