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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-system based implementation of {@link ResponseCache}.
 * Thread-safe with atomic writes, proper locking, and corruption recovery.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Atomic writes via temp file + move</li>
 *   <li>Read-write locking for thread safety</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Comprehensive statistics tracking</li>
 *   <li>Event notifications to listeners</li>
 * </ul>
 *
 * <p>File structure:</p>
 * <pre>
 * {cacheDirectory}/
 *   ├── {cacheKey}.json      # Cached JSON response
 *   ├── {cacheKey}.meta      # Metadata (timestamp, size)
 *   └── {cacheKey}.json.tmp  # Temp file during writes
 * </pre>
 *
 * @param <T> The type of response data stored in this cache
 */
public class FileBasedResponseCache<T> implements ResponseCache<T> {

    private static final Logger log = LogManager.getLogger(FileBasedResponseCache.class);

    private final CachedEndpoint endpoint;
    private final CacheConfiguration config;
    private final ObjectMapper objectMapper;
    private final List<CacheEventListener> listeners;

    // Concurrency control
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Statistics counters
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong refreshCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile Instant lastAccessTime;

    // Response builder for refreshing cache
    private volatile ResponseBuilder<?> responseBuilder;

    // File paths
    private final Path cacheFilePath;
    private final Path metaFilePath;
    private final Path tempFilePath;

    /**
     * Create a new file-based response cache for the given endpoint.
     *
     * @param endpoint The cached endpoint
     * @param config Configuration for this cache
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param listeners Event listeners (can be empty list)
     */
    public FileBasedResponseCache(CachedEndpoint endpoint, CacheConfiguration config,
                                  ObjectMapper objectMapper, List<CacheEventListener> listeners) {
        this.endpoint = endpoint;
        this.config = config;
        this.objectMapper = objectMapper;
        this.listeners = listeners != null ? listeners : Collections.emptyList();

        this.cacheFilePath = config.getCacheDirectory().resolve(endpoint.getCacheFileName());
        this.metaFilePath = config.getCacheDirectory().resolve(endpoint.getMetaFileName());
        this.tempFilePath = config.getCacheDirectory().resolve(endpoint.getCacheFileName() + ".tmp");

        initializeDirectory();
    }

    private void initializeDirectory() {
        if (!config.isEnabled()) {
            return;
        }
        try {
            Files.createDirectories(config.getCacheDirectory());
            log.debug("Cache directory initialized for endpoint {}: {}", endpoint, config.getCacheDirectory());
        } catch (IOException e) {
            log.error("Failed to create cache directory for endpoint {}: {}", endpoint, config.getCacheDirectory(), e);
        }
    }

    @Override
    public CachedEndpoint endpoint() {
        return endpoint;
    }

    @Override
    public Optional<InputStream> getAsStream() {
        if (!config.isEnabled()) {
            return Optional.empty();
        }

        lastAccessTime = Instant.now();

        rwLock.readLock().lock();
        try {
            CacheState state = calculateState();

            switch (state) {
                case FRESH:
                    hitCount.incrementAndGet();
                    notifyHit();
                    return openCacheFile();

                case STALE:
                    // Return stale data - caller may trigger async refresh
                    hitCount.incrementAndGet();
                    notifyHit();
                    return openCacheFile();

                case EMPTY:
                case EXPIRED:
                case ERROR:
                default:
                    missCount.incrementAndGet();
                    notifyMiss();
                    return Optional.empty();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<String> getAsString() {
        return getAsStream().map(is -> {
            try (is) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to read cache as string for endpoint {}", endpoint, e);
                return null;
            }
        });
    }

    private Optional<InputStream> openCacheFile() {
        try {
            return Optional.of(new BufferedInputStream(
                Files.newInputStream(cacheFilePath, StandardOpenOption.READ)));
        } catch (IOException e) {
            log.error("Failed to open cache file for endpoint {}", endpoint, e);
            errorCount.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public void put(String jsonData) {
        if (!config.isEnabled()) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            // Write to temp file first
            Files.writeString(tempFilePath, jsonData, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Atomic move to final location
            Files.move(tempFilePath, cacheFilePath,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Write metadata
            CacheMetadata metadata = CacheMetadata.now(jsonData.length());
            Files.writeString(metaFilePath, objectMapper.writeValueAsString(metadata),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            refreshCount.incrementAndGet();
            log.info("Cache updated for endpoint {}, size: {} bytes", endpoint, jsonData.length());

        } catch (IOException e) {
            log.error("Failed to update cache for endpoint {}", endpoint, e);
            errorCount.incrementAndGet();
            cleanupTempFile();
        } finally {
            rwLock.writeLock().unlock();
            refreshInProgress.set(false);
        }
    }

    @Override
    public void invalidate() {
        if (!config.isEnabled()) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            Files.deleteIfExists(cacheFilePath);
            Files.deleteIfExists(metaFilePath);
            cleanupTempFile();
            log.info("Cache invalidated for endpoint {}", endpoint);
            notifyInvalidated();
        } catch (IOException e) {
            log.error("Failed to invalidate cache for endpoint {}", endpoint, e);
            errorCount.incrementAndGet();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public boolean isPresent() {
        return config.isEnabled() && Files.exists(cacheFilePath);
    }

    @Override
    public CacheStatistics getStatistics() {
        CacheMetadata metadata = readMetadata().orElse(null);
        CacheState state = calculateState();

        long sizeBytes = 0;
        if (Files.exists(cacheFilePath)) {
            try {
                sizeBytes = Files.size(cacheFilePath);
            } catch (IOException e) {
                log.warn("Failed to get cache file size for endpoint {}", endpoint, e);
            }
        }

        return CacheStatistics.builder()
            .endpoint(endpoint)
            .endpointDescription(endpoint.getEndpointDescription())
            .enabled(config.isEnabled())
            .cachePresent(Files.exists(cacheFilePath))
            .createdAt(metadata != null ? metadata.createdAt() : null)
            .lastAccessTime(lastAccessTime != null ? lastAccessTime.toString() : null)
            .cacheSizeBytes(sizeBytes)
            .ttlSeconds(config.getTtlSeconds())
            .maxStaleSeconds(config.getMaxStaleSeconds())
            .ageSeconds(metadata != null ?
                Duration.between(metadata.createdAtInstant(), Instant.now()).getSeconds() : 0)
            .stale(state == CacheState.STALE)
            .expired(state == CacheState.EXPIRED)
            .hitCount(hitCount.get())
            .missCount(missCount.get())
            .refreshCount(refreshCount.get())
            .errorCount(errorCount.get())
            .cacheFilePath(cacheFilePath.toString())
            .state(state)
            .build();
    }

    @Override
    public boolean tryStartRefresh() {
        return refreshInProgress.compareAndSet(false, true);
    }

    @Override
    public void refreshComplete() {
        refreshInProgress.set(false);
    }

    @Override
    public void refresh() throws Exception {
        if (responseBuilder == null) {
            throw new IllegalStateException("No response builder registered for endpoint: " + endpoint);
        }

        if (!tryStartRefresh()) {
            throw new IllegalStateException("Refresh already in progress for endpoint: " + endpoint);
        }

        notifyRefreshStart();
        long startTime = System.currentTimeMillis();

        try {
            Object data = responseBuilder.build();
            String json = objectMapper.writeValueAsString(data);
            put(json);
            notifyRefreshComplete(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            refreshComplete();
            notifyRefreshError(e);
            throw e;
        }
    }

    @Override
    public void setResponseBuilder(ResponseBuilder<?> responseBuilder) {
        this.responseBuilder = responseBuilder;
    }

    @Override
    public boolean hasResponseBuilder() {
        return responseBuilder != null;
    }

    /**
     * Calculate the current state of the cache.
     */
    private CacheState calculateState() {
        if (!Files.exists(cacheFilePath)) {
            return CacheState.EMPTY;
        }

        Optional<CacheMetadata> metadata = readMetadata();
        if (metadata.isEmpty()) {
            return CacheState.ERROR;
        }

        long ageSeconds = Duration.between(metadata.get().createdAtInstant(), Instant.now()).getSeconds();

        if (refreshInProgress.get()) {
            return CacheState.REFRESHING;
        } else if (ageSeconds <= config.getTtlSeconds()) {
            return CacheState.FRESH;
        } else if (ageSeconds <= config.getTtlSeconds() + config.getMaxStaleSeconds()) {
            return CacheState.STALE;
        } else {
            return CacheState.EXPIRED;
        }
    }

    /**
     * Read cache metadata from file.
     */
    private Optional<CacheMetadata> readMetadata() {
        if (!Files.exists(metaFilePath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(metaFilePath, StandardCharsets.UTF_8);
            return Optional.of(objectMapper.readValue(content, CacheMetadata.class));
        } catch (Exception e) {
            log.warn("Failed to read cache metadata for endpoint {}", endpoint, e);
            return Optional.empty();
        }
    }

    /**
     * Clean up temp file if it exists.
     */
    private void cleanupTempFile() {
        try {
            Files.deleteIfExists(tempFilePath);
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    // =========================================================================
    // Event notification methods
    // =========================================================================

    private void notifyHit() {
        listeners.forEach(l -> {
            try {
                l.onCacheHit(endpoint);
            } catch (Exception e) {
                log.warn("Cache event listener error on hit", e);
            }
        });
    }

    private void notifyMiss() {
        listeners.forEach(l -> {
            try {
                l.onCacheMiss(endpoint);
            } catch (Exception e) {
                log.warn("Cache event listener error on miss", e);
            }
        });
    }

    private void notifyRefreshStart() {
        listeners.forEach(l -> {
            try {
                l.onCacheRefreshStart(endpoint);
            } catch (Exception e) {
                log.warn("Cache event listener error on refresh start", e);
            }
        });
    }

    private void notifyRefreshComplete(long durationMs) {
        listeners.forEach(l -> {
            try {
                l.onCacheRefreshComplete(endpoint, durationMs);
            } catch (Exception e) {
                log.warn("Cache event listener error on refresh complete", e);
            }
        });
    }

    private void notifyRefreshError(Exception ex) {
        listeners.forEach(l -> {
            try {
                l.onCacheRefreshError(endpoint, ex);
            } catch (Exception e) {
                log.warn("Cache event listener error on refresh error", e);
            }
        });
    }

    private void notifyInvalidated() {
        listeners.forEach(l -> {
            try {
                l.onCacheInvalidated(endpoint);
            } catch (Exception e) {
                log.warn("Cache event listener error on invalidate", e);
            }
        });
    }
}

/**
 * Metadata stored alongside the cache file.
 * Used for TTL calculations and statistics.
 * Uses String for timestamp to avoid Jackson serialization issues with Instant.
 */
record CacheMetadata(String createdAt, long sizeBytes) {

    /**
     * Get createdAt as Instant for calculations.
     */
    Instant createdAtInstant() {
        return Instant.parse(createdAt);
    }

    /**
     * Create metadata with current timestamp.
     */
    static CacheMetadata now(long sizeBytes) {
        return new CacheMetadata(Instant.now().toString(), sizeBytes);
    }
}
