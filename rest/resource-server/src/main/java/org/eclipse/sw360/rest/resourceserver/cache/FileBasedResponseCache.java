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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-based implementation of {@link ResponseCache}.
 * Each variant gets separate .json and .meta files. Thread-safe with atomic writes.
 */
public class FileBasedResponseCache<T> implements ResponseCache<T> {

    private static final Logger log = LogManager.getLogger(FileBasedResponseCache.class);

    private final CachedEndpoint endpoint;
    private final CacheConfiguration config;
    private final ObjectMapper objectMapper;
    private final String variant;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong writeCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile Instant lastAccessTime;

    private final Path cacheFilePath;
    private final Path metaFilePath;
    private final Path tempFilePath;

    public FileBasedResponseCache(CachedEndpoint endpoint, CacheConfiguration config,
                                  ObjectMapper objectMapper, String variant) {
        this.endpoint = endpoint;
        this.config = config;
        this.objectMapper = objectMapper;
        this.variant = variant;

        String baseName = endpoint.getCacheKey() + "-" + variant;
        this.cacheFilePath = config.getCacheDirectory().resolve(baseName + ".json");
        this.metaFilePath = config.getCacheDirectory().resolve(baseName + ".meta");
        this.tempFilePath = config.getCacheDirectory().resolve(baseName + ".json.tmp");

        initializeDirectory();
    }

    private void initializeDirectory() {
        if (!config.isEnabled()) {
            return;
        }
        try {
            Files.createDirectories(config.getCacheDirectory());
        } catch (IOException e) {
            log.error("Failed to create cache directory for {}: {}", endpoint, config.getCacheDirectory(), e);
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
            // Only serve FRESH cache. EXPIRED cache should be regenerated.
            if (state == CacheState.FRESH) {
                hitCount.incrementAndGet();
                log.debug("Cache HIT for {} variant={} state={}", endpoint, variant, state);
                return openCacheFile();
            }
            // EXPIRED or STALE cache - delete and return empty to trigger regeneration
            if (state == CacheState.EXPIRED) {
                log.info("Cache EXPIRED for {} variant={}, deleting old cache", endpoint, variant);
                // Delete expired cache files asynchronously to avoid blocking
                rwLock.readLock().unlock();
                rwLock.writeLock().lock();
                try {
                    deleteExpiredCache();
                } finally {
                    rwLock.writeLock().unlock();
                    rwLock.readLock().lock();
                }
            }
            missCount.incrementAndGet();
            log.debug("Cache MISS for {} variant={} state={}", endpoint, variant, state);
            return Optional.empty();
        } finally {
            rwLock.readLock().unlock();
        }
    }


    private Optional<InputStream> openCacheFile() {
        try {
            return Optional.of(new BufferedInputStream(
                    Files.newInputStream(cacheFilePath, StandardOpenOption.READ)));
        } catch (IOException e) {
            log.error("Failed to open cache file for {}", endpoint, e);
            errorCount.incrementAndGet();
            return Optional.empty();
        }
    }


    @Override
    public void putBytes(byte[] serializedBytes) {
        if (!config.isEnabled() || serializedBytes == null) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            long startTime = System.currentTimeMillis();

            // Write pre-serialized bytes directly to temp file
            Files.write(tempFilePath, serializedBytes,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long sizeBytes = serializedBytes.length;

            // Atomic move: temp file → cache file
            Files.move(tempFilePath, cacheFilePath,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Write metadata
            CacheMetadata metadata = CacheMetadata.now(sizeBytes);
            Files.writeString(metaFilePath, objectMapper.writeValueAsString(metadata),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long duration = System.currentTimeMillis() - startTime;
            writeCount.incrementAndGet();
            log.info("Cache written for {} variant={}, size: {} bytes in {}ms",
                    endpoint, variant, sizeBytes, duration);
        } catch (IOException e) {
            log.error("Failed to write cache for {} variant={}", endpoint, variant, e);
            errorCount.incrementAndGet();
            cleanupTempFile();
            // Re-throw to allow caller to handle cleanup
            throw new RuntimeException("Cache write failed", e);
        } finally {
            rwLock.writeLock().unlock();
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
            log.info("Cache invalidated for {}", endpoint);
        } catch (IOException e) {
            log.error("Failed to invalidate cache for {}", endpoint, e);
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
    public CacheStatistics getStatistics() {
        CacheMetadata metadata = readMetadata().orElse(null);
        CacheState state = calculateState();

        long sizeBytes = 0;
        if (Files.exists(cacheFilePath)) {
            try {
                sizeBytes = Files.size(cacheFilePath);
            } catch (IOException e) {
                log.warn("Failed to get cache file size for {}", endpoint, e);
            }
        }

        return CacheStatistics.builder()
                .endpoint(endpoint)
                .endpointDescription(endpoint.getEndpointDescription())
                .variant(variant)
                .enabled(config.isEnabled())
                .cachePresent(Files.exists(cacheFilePath))
                .createdAt(metadata != null ? metadata.getCreatedAt() : null)
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
                .writeCount(writeCount.get())
                .errorCount(errorCount.get())
                .cacheFilePath(cacheFilePath.toString())
                .state(state)
                .build();
    }

    private CacheState calculateState() {
        if (!Files.exists(cacheFilePath)) {
            return CacheState.EMPTY;
        }
        Optional<CacheMetadata> metadata = readMetadata();
        if (metadata.isEmpty()) {
            return CacheState.ERROR;
        }
        long ageSeconds = Duration.between(metadata.get().createdAtInstant(), Instant.now()).getSeconds();
        if (ageSeconds <= config.getTtlSeconds()) {
            return CacheState.FRESH;
        } else if (ageSeconds <= config.getTtlSeconds() + config.getMaxStaleSeconds()) {
            return CacheState.STALE;
        } else {
            return CacheState.EXPIRED;
        }
    }

    private Optional<CacheMetadata> readMetadata() {
        if (!Files.exists(metaFilePath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(metaFilePath, StandardCharsets.UTF_8);
            return Optional.of(objectMapper.readValue(content, CacheMetadata.class));
        } catch (Exception e) {
            log.warn("Failed to read cache metadata for {}", endpoint, e);
            return Optional.empty();
        }
    }

    private void cleanupTempFile() {
        try {
            Files.deleteIfExists(tempFilePath);
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    private void deleteExpiredCache() {
        try {
            Files.deleteIfExists(cacheFilePath);
            Files.deleteIfExists(metaFilePath);
            log.debug("Deleted expired cache files for {} variant={}", endpoint, variant);
        } catch (IOException e) {
            log.warn("Failed to delete expired cache for {} variant={}", endpoint, variant, e);
        }
    }

    /**
     * Metadata stored alongside the cache file.
     */
    static class CacheMetadata {
        private final String createdAt;
        private final long sizeBytes;

        @JsonCreator
        public CacheMetadata(@JsonProperty("createdAt") String createdAt,
                             @JsonProperty("sizeBytes") long sizeBytes) {
            this.createdAt = createdAt;
            this.sizeBytes = sizeBytes;
        }

        @JsonProperty("createdAt")
        public String getCreatedAt() {
            return createdAt;
        }

        @JsonProperty("sizeBytes")
        public long getSizeBytes() {
            return sizeBytes;
        }

        Instant createdAtInstant() {
            return Instant.parse(createdAt);
        }

        static CacheMetadata now(long sizeBytes) {
            return new CacheMetadata(Instant.now().toString(), sizeBytes);
        }
    }
}
