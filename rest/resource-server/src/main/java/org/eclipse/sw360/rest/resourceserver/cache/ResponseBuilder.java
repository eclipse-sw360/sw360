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
 * Interface for building API response data to be cached.
 * Implement this for each cacheable endpoint to define how the response is generated.
 *
 * <p>The builder is called when:</p>
 * <ul>
 *   <li>Cache is empty and a request comes in</li>
 *   <li>Cache is expired and needs refresh</li>
 *   <li>Admin triggers manual refresh via API</li>
 *   <li>Warm-up runs on application startup</li>
 * </ul>
 *
 * <p>Example implementation for releases:</p>
 * <pre>
 * {@code
 * @PostConstruct
 * public void registerResponseBuilder() {
 *     ResponseCache<?> cache = cacheManager.getCache(CachedEndpoint.RELEASES_ALL_DETAILS);
 *     cache.setResponseBuilder(() -> {
 *         // Fetch all releases with full details
 *         List<Release> releases = releaseService.getAllReleasesWithDetails();
 *         // Build HAL response
 *         return buildHalResponse(releases);
 *     });
 * }
 * }
 * </pre>
 *
 * <p>Example implementation for components (future):</p>
 * <pre>
 * {@code
 * cache.setResponseBuilder(() -> {
 *     List<Component> components = componentService.getAllComponentsWithDetails();
 *     return buildHalResponse(components);
 * });
 * }
 * </pre>
 *
 * @param <T> The type of response data returned by the builder (will be serialized to JSON)
 */
@FunctionalInterface
public interface ResponseBuilder<T> {

    /**
     * Build the API response data to be cached.
     * This method should fetch fresh data from the source (database, service, etc.)
     * and return it in a format ready for JSON serialization.
     *
     * @return The response data to cache
     * @throws Exception if building fails (cache refresh will be aborted)
     */
    T build() throws Exception;
}
