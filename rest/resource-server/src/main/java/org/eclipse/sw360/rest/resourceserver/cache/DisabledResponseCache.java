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

import java.io.InputStream;
import java.util.Optional;

/**
 * A no-op cache implementation that does nothing.
 * Used when caching is disabled or cache manager is not properly initialized.
 *
 * <p>This implements the Null Object Pattern - all operations are safe no-ops
 * that return empty/false values, allowing callers to use the cache interface
 * without null checks.</p>
 *
 * @param <T> The type parameter (unused in this implementation)
 */
public class DisabledResponseCache<T> implements ResponseCache<T> {

    private final CachedEndpoint endpoint;

    public DisabledResponseCache(CachedEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public CachedEndpoint endpoint() {
        return endpoint;
    }

    @Override
    public Optional<InputStream> getAsStream() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getAsString() {
        return Optional.empty();
    }

    @Override
    public void put(String jsonData) {
        // no-op: cache is disabled
    }

    @Override
    public void invalidate() {
        // no-op: cache is disabled
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public CacheStatistics getStatistics() {
        return CacheStatistics.builder()
                .endpoint(endpoint)
                .enabled(false)
                .state(CacheState.EMPTY)
                .build();
    }

    @Override
    public boolean tryStartRefresh() {
        return false;
    }

    @Override
    public void refreshComplete() {
        // no-op: cache is disabled
    }

    @Override
    public void refresh() {
        throw new IllegalStateException("Cache is disabled for endpoint: " + endpoint);
    }

    @Override
    public void setResponseBuilder(ResponseBuilder<?> responseBuilder) {
        // no-op: cache is disabled
    }

    @Override
    public boolean hasResponseBuilder() {
        return false;
    }
}
