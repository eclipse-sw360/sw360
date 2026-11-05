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

import jakarta.servlet.http.HttpServletRequest;

/**
 * Determines if an HTTP request is cacheable for a given {@link CachedEndpoint}.
 * Register implementations as Spring beans — the interceptor discovers them by endpoint.
 */
public interface CacheCondition {

    CachedEndpoint endpoint();

    boolean isCacheable(HttpServletRequest request);

    /**
     * Optional additional cache-key discriminator derived from the request, appended (via
     * {@link CacheReadFilter}) to the role-based variant resolved by
     * {@code CacheVariantResolver}.
     *
     * <p>Override this when the cached response body can differ based on a request parameter
     * that is <em>not</em> already covered by a separate {@link CachedEndpoint} constant (e.g.
     * sort order) — without it, requests that only differ by that parameter would incorrectly
     * share (and overwrite) the same cache file.</p>
     *
     * @return a filesystem-safe, non-null suffix (no path separators), or {@code ""} for none.
     */
    default String variantSuffix(HttpServletRequest request) {
        return "";
    }
}
