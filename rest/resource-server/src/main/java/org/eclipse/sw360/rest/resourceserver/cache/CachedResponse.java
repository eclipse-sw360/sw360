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
 * Marks a controller GET method as cacheable (documentation annotation).
 *
 * <p><strong>Note:</strong> This annotation is purely for documentation purposes.
 * The actual caching logic is handled by {@link CacheReadFilter} using {@link CacheCondition}
 * beans to determine what requests are cacheable.</p>
 *
 * <p>This annotation documents which endpoints a controller method can potentially cache,
 * supporting multiple endpoints when a single method handles different cache scenarios
 * based on request parameters (e.g., {@code allDetails=true} vs {@code allDetails=false}).</p>
 *
 * @see CacheReadFilter
 * @see CacheCondition
 * @see CachedEndpoint
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachedResponse {
    /**
     * One or more endpoints this method can potentially cache.
     * This is for documentation only - actual caching is determined by CacheCondition beans.
     */
    CachedEndpoint[] endpoints();
}
