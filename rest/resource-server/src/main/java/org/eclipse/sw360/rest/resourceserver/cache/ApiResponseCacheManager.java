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
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for SW360 API response caching.
 * Supports per-variant (per-role) caching. Each UserGroup gets its own cache file.
 */
@Service
public class ApiResponseCacheManager {

    private static final Logger log = LogManager.getLogger(ApiResponseCacheManager.class);

    /** Default variant when no per-role caching is needed. */
    public static final String DEFAULT_VARIANT = "default";

    /** Separator for composite cache keys (endpoint_variant). */
    private static final String VARIANT_SEPARATOR = "_";

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String CONFIG_GLOBAL_ENABLED = "rest.cache.enabled";
    private static final String CONFIG_GLOBAL_DIRECTORY = "rest.cache.directory";
    private static final String DEFAULT_CACHE_DIRECTORY = "/var/sw360/cache";
    private static final long DEFAULT_TTL_SECONDS = 86400;
    private static final long DEFAULT_MAX_STALE_SECONDS = 300;

    /** Composite key: "ENDPOINT_variant" → cache instance. Created lazily per variant. */
    private final Map<String, ResponseCache<?>> caches = new ConcurrentHashMap<>();

    /** Per-endpoint config (shared across variants). */
    private final Map<CachedEndpoint, CacheConfiguration> configs = new EnumMap<>(CachedEndpoint.class);

    private final ObjectMapper objectMapper;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private Properties cacheProperties;
    private boolean globalCacheEnabled;
    private String cacheDirectory;

    public ApiResponseCacheManager(ObjectMapper objectMapper) {
        objectMapper.findAndRegisterModules();
        this.objectMapper = objectMapper;
    }

    private void ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
              cacheProperties = CommonUtils.loadProperties(
                ApiResponseCacheManager.class, SW360_PROPERTIES_FILE_PATH, true);

            this.globalCacheEnabled = Boolean.parseBoolean(
                cacheProperties.getProperty(CONFIG_GLOBAL_ENABLED, "false").trim());
            this.cacheDirectory = cacheProperties.getProperty(CONFIG_GLOBAL_DIRECTORY, DEFAULT_CACHE_DIRECTORY);

            if (CommonUtils.isNullEmptyOrWhitespace(this.cacheDirectory)) {
                this.cacheDirectory = DEFAULT_CACHE_DIRECTORY;
            }

            if (!globalCacheEnabled) {
                log.info("API Response Cache disabled (set {}=true to enable)", CONFIG_GLOBAL_ENABLED);
                return;
            }

            log.info("Initializing API Response Cache (directory: {})", cacheDirectory);

            for (CachedEndpoint endpoint : CachedEndpoint.values()) {
                CacheConfiguration config = loadConfiguration(endpoint);
                configs.put(endpoint, config);
                log.info("Cache {} initialized (enabled={}, ttl={}s)",
                    endpoint, config.isEnabled(), config.getTtlSeconds());
            }
        }
    }

    private CacheConfiguration loadConfiguration(CachedEndpoint endpoint) {
        String prefix = endpoint.getPropertyPrefix() + ".";

        boolean enabled = globalCacheEnabled &&
            Boolean.parseBoolean(cacheProperties.getProperty(prefix + "enabled", "false"));

        // Per-role caching: defaults to enum value, can be overridden via properties
        boolean perRoleCaching = Boolean.parseBoolean(
            cacheProperties.getProperty(prefix + "per.role.caching",
                String.valueOf(endpoint.isPerRoleCaching())));

        long ttlSeconds = Long.parseLong(
            cacheProperties.getProperty(prefix + "ttl.seconds", String.valueOf(DEFAULT_TTL_SECONDS)));
        long maxStaleSeconds = Long.parseLong(
            cacheProperties.getProperty(prefix + "max.stale.seconds", String.valueOf(DEFAULT_MAX_STALE_SECONDS)));

        return CacheConfiguration.builder()
            .endpoint(endpoint)
            .enabled(enabled)
            .perRoleCaching(perRoleCaching)
            .ttlSeconds(ttlSeconds)
            .maxStaleSeconds(maxStaleSeconds)
            .cacheDirectory(Paths.get(cacheDirectory))
            .build();
    }


    /**
     * Get cache for endpoint with a specific variant (e.g. user role).
     * Cache instances are created lazily per variant.
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseCache<T> getCache(CachedEndpoint endpoint, String variant) {
        ensureInitialized();

        CacheConfiguration config = configs.get(endpoint);
        if (config == null || !config.isEnabled()) {
            CacheConfiguration disabledConfig = CacheConfiguration.builder()
                .endpoint(endpoint).enabled(false)
                .cacheDirectory(Paths.get(DEFAULT_CACHE_DIRECTORY))
                .build();
            return new FileBasedResponseCache<>(endpoint, disabledConfig, objectMapper, variant);
        }

        String compositeKey = compositeKey(endpoint, variant);
        return (ResponseCache<T>) caches.computeIfAbsent(compositeKey, k -> {
            log.info("Creating cache instance for {} variant={}", endpoint, variant);
            return new FileBasedResponseCache<>(endpoint, config, objectMapper, variant);
        });
    }

    public boolean isEndpointEnabled(CachedEndpoint endpoint) {
        ensureInitialized();
        CacheConfiguration config = configs.get(endpoint);
        return config != null && config.isEnabled();
    }

    /**
     * Check if per-role caching is enabled for this endpoint.
     * Reads from configuration (property override or enum default).
     */
    public boolean isPerRoleCachingEnabled(CachedEndpoint endpoint) {
        ensureInitialized();
        CacheConfiguration config = configs.get(endpoint);
        return config != null && config.isPerRoleCaching();
    }

    /**
     * Get statistics for all variants of all endpoints by scanning disk.
     */
    public List<CacheStatistics> getAllVariantStatistics() {
        ensureInitialized();
        List<CacheStatistics> allStats = new ArrayList<>();
        for (CachedEndpoint endpoint : CachedEndpoint.values()) {
            allStats.addAll(getEndpointVariantStatistics(endpoint));
        }
        return allStats;
    }

    /**
     * Get statistics for all variants of a specific endpoint by scanning disk.
     */
    public List<CacheStatistics> getEndpointVariantStatistics(CachedEndpoint endpoint) {
        ensureInitialized();

        CacheConfiguration config = configs.get(endpoint);
        if (config == null || !config.isEnabled()) {
            return Collections.emptyList();
        }

        return scanCacheFilesForEndpoint(endpoint, config);
    }


    /**
     * Scan cache directory for files matching endpoint's cacheKey pattern.
     * Returns stats for each variant found on disk.
     */
    private List<CacheStatistics> scanCacheFilesForEndpoint(CachedEndpoint endpoint, CacheConfiguration config) {
        List<CacheStatistics> variantStats = new ArrayList<>();
        Path cacheDir = config.getCacheDirectory();

        if (!java.nio.file.Files.exists(cacheDir)) {
            return variantStats;
        }

        String cacheKeyPrefix = endpoint.getCacheKey() + "-";

        try (var stream = java.nio.file.Files.list(cacheDir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(cacheKeyPrefix))
                  .filter(p -> p.getFileName().toString().endsWith(".json"))
                  .forEach(cacheFile -> {
                      // Extract variant from filename: "releases-all-details-ADMIN.json" → "ADMIN"
                      String fileName = cacheFile.getFileName().toString();
                      String variant = fileName
                              .substring(cacheKeyPrefix.length(), fileName.length() - 5); // remove ".json"

                      // Get or create cache instance for this variant
                      ResponseCache<?> cache = getCache(endpoint, variant);
                      variantStats.add(cache.getStatistics());
                  });
        } catch (Exception e) {
            log.error("Failed to scan cache files for {}: {}", endpoint, e.getMessage(), e);
        }

        return variantStats;
    }


    /**
     * Invalidate ALL variants of all endpoints.
     */
    public void invalidateAll() {
        caches.values().forEach(ResponseCache::invalidate);
        log.info("All API response caches invalidated ({} variants)", caches.size());
    }

    /**
     * Invalidate ALL variants of a specific endpoint.
     */
    public void invalidate(CachedEndpoint endpoint) {
        ensureInitialized();
        String prefix = endpoint.name() + VARIANT_SEPARATOR;
        List<Map.Entry<String, ResponseCache<?>>> entriesToInvalidate = caches.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .toList();

        entriesToInvalidate.forEach(e -> e.getValue().invalidate());
        log.info("Cache invalidated for {} ({} variants)", endpoint, entriesToInvalidate.size());
    }

    /**
     * Invalidate a specific variant of an endpoint.
     */
    public void invalidate(CachedEndpoint endpoint, String variant) {
        ensureInitialized();
        String compositeKey = compositeKey(endpoint, variant);
        ResponseCache<?> cache = caches.get(compositeKey);
        if (cache != null) {
            cache.invalidate();
            log.info("Cache invalidated for {} variant={}", endpoint, variant);
        } else {
            log.warn("No cache instance found for {} variant={}", endpoint, variant);
        }
    }

    public boolean isEnabled() {
        ensureInitialized();
        return globalCacheEnabled;
    }

    private static String compositeKey(CachedEndpoint endpoint, String variant) {
        return endpoint.name() + VARIANT_SEPARATOR + variant;
    }
}
