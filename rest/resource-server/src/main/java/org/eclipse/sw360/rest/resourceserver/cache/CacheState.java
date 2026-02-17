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

/**
 * Represents the current state of a cache region.
 * Used by {@link CacheStatistics} and internally by {@link FileBasedCache}.
 */
public enum CacheState {

    /**
     * No cache file exists for this region.
     * Requests will trigger a cache population.
     */
    EMPTY,

    /**
     * Cache exists and is within TTL.
     * Requests are served from cache.
     */
    FRESH,

    /**
     * Cache exists but is past TTL, within max stale time.
     * Stale data is served while refresh happens in background.
     */
    STALE,

    /**
     * Cache exists but is past both TTL and max stale time.
     * Cache is unusable; requests trigger fresh data fetch.
     */
    EXPIRED,

    /**
     * Cache refresh is currently in progress.
     * Existing cache (if any) may be served during refresh.
     */
    REFRESHING,

    /**
     * Cache is in an error state (e.g., corrupted metadata).
     * Will attempt recovery on next access.
     */
    ERROR
}
