/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.component;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.resourceserver.cache.CacheCondition;
import org.eclipse.sw360.rest.resourceserver.cache.CachedEndpoint;
import org.eclipse.sw360.rest.resourceserver.component.cache.ComponentCacheDecisionContext;
import org.eclipse.sw360.rest.resourceserver.component.cache.ComponentCachingCriteria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for the {@code GET /api/components} (getAllComponents) cache conditions.
 *
 * <p>Mirrors the {@code allDetails} split used by {@code ReleaseCacheCondition}: two separate
 * {@link CachedEndpoint} constants/cache buckets — {@link CachedEndpoint#COMPONENTS_ALL_DETAILS}
 * and {@link CachedEndpoint#COMPONENTS_WITHOUT_DETAILS} — so both representations can be cached
 * independently instead of one clobbering the other.</p>
 *
 * <p>A request is only cache-eligible when:</p>
 * <ol>
 *   <li>it targets the plain component listing endpoint ({@code /api/components}) with no
 *       filters applied — i.e. the same branch that calls
 *       {@code getRecentComponentsSummaryWithPagination}, and</li>
 *   <li>it requests the first page ({@code page=0} or absent) — pagination beyond the first
 *       page is a different result subset, not just a representation choice, and</li>
 *   <li>its {@code allDetails} value matches the bean's target endpoint, and</li>
 *   <li>every registered {@link ComponentCachingCriteria} (e.g. {@code PageSizeCriteria})
 *       agrees the request should be cached.</li>
 * </ol>
 *
 * <p><b>Allow-list (acceptance) design, on purpose:</b> "no filters applied" is decided by
 * checking that every query parameter on the request belongs to
 * {@link #SAFE_LISTING_PARAMS} — the small, explicit set of parameters known not to change
 * which components are matched. This is the deliberate opposite of a deny-list (checking each
 * known filter parameter is absent): if {@link ComponentController#getComponents} gains a new
 * filter/search parameter in the future and this class is not updated, the new parameter is
 * automatically treated as unsafe and the request bypasses the cache — a forgotten deny-list
 * entry would instead silently make the new filter cacheable and risk serving stale or
 * incorrect results.</p>
 *
 * <p><b>{@code sort} handling:</b> {@code sort} is in the safe/allow-listed set (doesn't cause a
 * bypass) because its value is folded into the cache <em>variant</em> via
 * {@link #sortVariantSuffix(HttpServletRequest)} (see {@link CacheCondition#variantSuffix}), so
 * distinct sort orders get their own cache file instead of overwriting one another.</p>
 *
 * <p>Permission-sensitive isolation is handled by the shared per-role cache variant
 * mechanism ({@code CacheVariantResolver} + {@code *.per.role.caching}), which takes effect only
 * for requests this condition marks eligible. Any request carrying search/filter parameters is
 * rejected here first, before per-role varianting is even considered.</p>
 *
 * <p>To add a new component caching criterion: register a Spring bean implementing
 * {@link ComponentCachingCriteria} — it is auto-included via {@code List<ComponentCachingCriteria>}
 * injection, no changes needed here.</p>
 */
@Configuration
public class ComponentCacheCondition {

    private static final Logger log = LogManager.getLogger(ComponentCacheCondition.class);

    private static final String PARAM_PAGE = "page";
    private static final String PARAM_SORT = "sort";
    private static final String PARAM_ALL_DETAILS = "allDetails";

    /**
     * Query parameters that never change which components are matched for the plain listing
     * branch ({@code getRecentComponentsSummaryWithPagination}) — pagination/representation
     * selectors handled explicitly elsewhere in this class (first-page check, allDetails
     * split, sort-aware variant). Any parameter not in this set (recognized filter or not) is
     * treated as unsafe.
     */
    private static final Set<String> SAFE_LISTING_PARAMS =
            Set.of(PARAM_PAGE, "page_entries", PARAM_SORT, PARAM_ALL_DETAILS);

    /**
     * Check if request path targets the components collection endpoint exactly
     * (not a sub-resource such as {@code /api/components/{id}}).
     */
    private static boolean isComponentsEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.endsWith("/api/components");
    }

    /**
     * Check that every query parameter present on the request is a known pagination-safe
     * parameter (see {@link #SAFE_LISTING_PARAMS}). Mirrors the branch in
     * {@code ComponentController#getComponents} that calls
     * {@code getRecentComponentsSummaryWithPagination} (the plain listing path).
     *
     * <p>Allow-list check: an empty/absent parameter map means "no filters" (eligible); any
     * unrecognized parameter name — whether it's a filter SW360 already knows about or a
     * brand-new one added later — makes the request unsafe to cache.</p>
     */
    private static boolean hasOnlySafeParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (CommonUtils.isNullOrEmptyMap(parameterMap)) {
            return true;
        }
        return SAFE_LISTING_PARAMS.containsAll(parameterMap.keySet());
    }

    /**
     * Only {@code page=0} (or absent, defaulting to the first page) is safe to cache: any other
     * page number is a different result subset, and the shared response cache stores a single
     * blob per (endpoint, variant) — see {@code CacheVariantResolver} — so serving it for a
     * different page would return the wrong data.
     */
    private static boolean isFirstPageOrAbsent(HttpServletRequest request) {
        String page = request.getParameter(PARAM_PAGE);
        if (CommonUtils.isNullEmptyOrWhitespace(page)) {
            return true;
        }
        try {
            return Integer.parseInt(page.trim()) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isAllDetailsRequested(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter(PARAM_ALL_DETAILS));
    }

    private static boolean isPlainComponentsListing(HttpServletRequest request) {
        return isComponentsEndpoint(request)
                && hasOnlySafeParameters(request)
                && isFirstPageOrAbsent(request);
    }

    /**
     * Builds the cache-variant suffix for the {@code sort} parameter so that different sort
     * orders are cached separately instead of sharing (and overwriting) one file. Returns
     * {@code ""} when no sort is requested, so the common case keeps the plain role-based
     * variant name unchanged.
     */
    private static String sortVariantSuffix(HttpServletRequest request) {
        String[] sortValues = request.getParameterValues(PARAM_SORT);
        if (sortValues == null || sortValues.length == 0) {
            return "";
        }
        String sanitized = String.join("_", sortValues)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "-");
        return sanitized.isEmpty() ? "" : "sort-" + sanitized;
    }

    /**
     * Shared eligibility evaluation for both allDetails variants.
     *
     * @param requireAllDetails the {@code allDetails} value this bean's {@link CachedEndpoint}
     *                          bucket represents (e.g. {@code true} for
     *                          {@link CachedEndpoint#COMPONENTS_ALL_DETAILS}).
     */
    private static boolean isCacheable(
            HttpServletRequest request, List<ComponentCachingCriteria> criteriaChain, boolean requireAllDetails) {
        ComponentCacheDecisionContext context = new ComponentCacheDecisionContext(
                request.getRequestURI() + "?" + CommonUtils.nullToEmptyString(request.getQueryString()));

        if (!isPlainComponentsListing(request)) {
            context.markRejected("not-plain-components-listing");
            context.setPermissionSensitive(true);
            log.debug("Component cache bypass for {}: {}",
                    context.getRequestUriWithQuery(), context.getRejectedBy());
            return false;
        }

        if (isAllDetailsRequested(request) != requireAllDetails) {
            context.markRejected("all-details-mismatch");
            log.debug("Component cache bypass for {}: allDetails={} does not match this bucket (requireAllDetails={})",
                    context.getRequestUriWithQuery(), request.getParameter(PARAM_ALL_DETAILS), requireAllDetails);
            return false;
        }

        if (criteriaChain.isEmpty()) {
            // No registered criteria = no positive match = bypass (default to no caching).
            context.markRejected("no-criteria-registered");
            log.debug("Component cache bypass for {}: no criteria beans registered",
                    context.getRequestUriWithQuery());
            return false;
        }

        for (ComponentCachingCriteria criterion : criteriaChain) {
            if (!criterion.shouldCache(request)) {
                context.markRejected(criterion.name());
                log.debug("Component cache bypass for {}: rejected by {}",
                        context.getRequestUriWithQuery(), criterion.name());
                return false;
            }
        }

        context.markEligible();
        log.debug("Component cache eligible for {}", context.getRequestUriWithQuery());
        return true;
    }

    /**
     * Cache condition for {@code GET /components?allDetails=true}.
     */
    @Bean
    public CacheCondition componentsAllDetailsCacheCondition(List<ComponentCachingCriteria> criteriaChain) {
        return new CacheCondition() {
            @Override
            public CachedEndpoint endpoint() {
                return CachedEndpoint.COMPONENTS_ALL_DETAILS;
            }

            @Override
            public boolean isCacheable(HttpServletRequest request) {
                return ComponentCacheCondition.isCacheable(request, criteriaChain, true);
            }

            @Override
            public String variantSuffix(HttpServletRequest request) {
                return sortVariantSuffix(request);
            }
        };
    }

    /**
     * Cache condition for {@code GET /components} (allDetails=false or absent).
     */
    @Bean
    public CacheCondition componentsWithoutDetailsCacheCondition(List<ComponentCachingCriteria> criteriaChain) {
        return new CacheCondition() {
            @Override
            public CachedEndpoint endpoint() {
                return CachedEndpoint.COMPONENTS_WITHOUT_DETAILS;
            }

            @Override
            public boolean isCacheable(HttpServletRequest request) {
                return ComponentCacheCondition.isCacheable(request, criteriaChain, false);
            }

            @Override
            public String variantSuffix(HttpServletRequest request) {
                return sortVariantSuffix(request);
            }
        };
    }
}
