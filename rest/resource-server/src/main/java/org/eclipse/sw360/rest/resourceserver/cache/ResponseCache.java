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
 * Interface for storing and retrieving cached API responses.
 */
public interface ResponseCache<T> {

    CachedEndpoint endpoint();

    /** Returns empty if cache is disabled, missing, or expired. */
    Optional<InputStream> getAsStream();

    /**
     * Atomic write of pre-serialized bytes to cache.
     * The caller must serialize the response to bytes before calling this method.
     * This approach prevents HTTP response stream timeouts from affecting cache writes,
     * as the serialization happens BEFORE the async cache write operation.
     *
     * @param serializedBytes the pre-serialized JSON bytes to cache
     */
    void putBytes(byte[] serializedBytes);

    /** Delete cached response. Called on data mutations. */
    void invalidate();

    boolean isEnabled();


    CacheStatistics getStatistics();
}
