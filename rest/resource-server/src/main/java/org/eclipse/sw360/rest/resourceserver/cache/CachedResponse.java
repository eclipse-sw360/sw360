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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller GET method as cacheable.
 * On cache hit, the method is skipped and cached JSON is returned.
 * On cache miss, the method runs and the response is written to cache asynchronously.
 *
 * <p>Supports multiple endpoints when a single method handles different cache scenarios
 * based on request parameters (e.g., {@code allDetails=true} vs {@code allDetails=false}).
 * The actual endpoint to cache is determined at runtime by matching {@link CacheCondition}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedResponse {
    /**
     * One or more endpoints this method can potentially cache.
     * The actual endpoint used is determined by matching CacheCondition at runtime.
     */
    CachedEndpoint[] endpoints();
}
