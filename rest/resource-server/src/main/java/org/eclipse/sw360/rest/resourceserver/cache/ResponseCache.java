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

    /** Returns empty if cache is disabled, missing, or expired. */
    Optional<String> getAsString();

    /** Atomic write of JSON response data to cache. */
    void put(String jsonData);

    /** Delete cached response. Called on data mutations. */
    void invalidate();

    boolean isEnabled();

    boolean isPresent();

    CacheStatistics getStatistics();
}
