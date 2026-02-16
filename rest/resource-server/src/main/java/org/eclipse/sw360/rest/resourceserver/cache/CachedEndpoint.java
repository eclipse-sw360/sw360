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

import lombok.Getter;

/**
 * Enumeration of cacheable REST API endpoints.
 *
 * <p>To add a new cacheable endpoint:</p>
 * <ol>
 *   <li>Add enum constant here</li>
 *   <li>Add a {@link CacheCondition} bean</li>
 *   <li>Annotate controller method with {@link CachedResponse}</li>
 *   <li>Call {@link ApiResponseCacheManager#invalidate} in mutation methods</li>
 *   <li>Configure properties in sw360.properties</li>
 * </ol>
 */
@Getter
public enum CachedEndpoint {

    /**
     * GET /releases?allDetails=true
     * Per-role caching: Different users may see different releases based on permissions.
     */
    RELEASES_ALL_DETAILS("releases-all-details", "GET /releases?allDetails=true", true),
    RELEASES_WITHOUT_DETAILS("releases-without-alldetails", "GET /releases?allDetails=false", true);

    private final String cacheKey;
    private final String endpointDescription;
    private final boolean perRoleCaching;

    CachedEndpoint(String cacheKey, String endpointDescription, boolean perRoleCaching) {
        this.cacheKey = cacheKey;
        this.endpointDescription = endpointDescription;
        this.perRoleCaching = perRoleCaching;
    }

    /**
     * Get the property key prefix for this endpoint.
     */
    public String getPropertyPrefix() {
        return "rest.cache." + cacheKey;
    }
}
