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

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * Configuration for a cached API endpoint response.
 * Values are loaded from sw360.properties with fallback to defaults defined in {@link CachedEndpoint}.
 *
 * <p>Used by {@link ApiResponseCacheManager} to configure each {@link FileBasedResponseCache} instance.</p>
 *
 * <p>Configuration properties format:</p>
 * <pre>
 * rest.cache.{cache-key}.enabled=true
 * rest.cache.{cache-key}.ttl.seconds=86400
 * rest.cache.{cache-key}.max.stale.seconds=300
 * rest.cache.{cache-key}.warmup.enabled=true
 * </pre>
 */
@Data
@Builder
public class CacheConfiguration {

    /**
     * The cached endpoint this configuration applies to.
     */
    private CachedEndpoint endpoint;

    /**
     * Whether caching is enabled for this endpoint.
     * Disabled caches always return empty and don't store data.
     */
    private boolean enabled;

    /**
     * Time-to-live in seconds.
     * Cache is considered fresh for this duration after creation.
     */
    private long ttlSeconds;

    /**
     * Maximum stale time in seconds.
     * After TTL expires, cache can still be served for this duration while refresh happens.
     */
    private long maxStaleSeconds;

    /**
     * Whether to warm up this cache on application startup.
     */
    private boolean warmupEnabled;

    /**
     * Directory where cache files are stored.
     */
    private Path cacheDirectory;
}
