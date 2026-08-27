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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cache conditions for {@code GET /api/components}. Two independent buckets
 * ({@link CachedEndpoint#COMPONENTS_ALL_DETAILS} / {@link CachedEndpoint#COMPONENTS_WITHOUT_DETAILS}).
 * Eligible only for the plain first-page listing whose params are all in {@link #SAFE_LISTING_PARAMS}
 * (allow-list: any unknown param bypasses) and that every {@link ComponentCachingCriteria} accepts.
 * Body-affecting params ({@code page_entries}, {@code sort}) are folded into the cache variant
 * (see {@link #listingVariantSuffix(HttpServletRequest)}); per-role isolation is handled by the
 * shared variant mechanism.
 */
@Configuration
public class ComponentCacheCondition {

    private static final Logger log = LogManager.getLogger(ComponentCacheCondition.class);

    private static final String PARAM_PAGE = "page";
    private static final String PARAM_PAGE_ENTRIES = "page_entries";
    private static final String PARAM_SORT = "sort";
    private static final String PARAM_ALL_DETAILS = "allDetails";

    /** Pagination/representation params that don't change which components match; anything else is unsafe to cache. */
    private static final Set<String> SAFE_LISTING_PARAMS =
            Set.of(PARAM_PAGE, PARAM_PAGE_ENTRIES, PARAM_SORT, PARAM_ALL_DETAILS);

    /** True only for the exact collection endpoint, not a sub-resource like {@code /api/components/{id}}. */
    private static boolean isComponentsEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.endsWith("/api/components");
    }

    /** Allow-list check: no params = eligible; any param outside {@link #SAFE_LISTING_PARAMS} = unsafe. */
    private static boolean hasOnlySafeParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (CommonUtils.isNullOrEmptyMap(parameterMap)) {
            return true;
        }
        return SAFE_LISTING_PARAMS.containsAll(parameterMap.keySet());
    }

    /** Only {@code page=0}/absent is cacheable; other pages are different subsets of the single per-variant blob. */
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

    /** Variant suffix {@code sort-<value>} so distinct sort orders get separate cache files; {@code ""} when absent. */
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
     * Variant suffix {@code pe-<size>} so different page sizes (e.g. 1100 vs 20000) don't share one blob —
     * otherwise the first cached size is served for every size. {@code ""} when absent/non-numeric.
     */
    private static String pageEntriesVariantSuffix(HttpServletRequest request) {
        String pageEntries = request.getParameter(PARAM_PAGE_ENTRIES);
        if (CommonUtils.isNullEmptyOrWhitespace(pageEntries)) {
            return "";
        }
        try {
            int parsed = Integer.parseInt(pageEntries.trim());
            return "pe-" + parsed;
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /** Joins the body-affecting discriminators ({@code page_entries}, {@code sort}) into one variant suffix. */
    private static String listingVariantSuffix(HttpServletRequest request) {
        String pagePart = pageEntriesVariantSuffix(request);
        String sortPart = sortVariantSuffix(request);
        return Stream.of(pagePart, sortPart)
                .filter(part -> !CommonUtils.isNullEmptyOrWhitespace(part))
                .collect(Collectors.joining("-"));
    }

    /**
     * Shared eligibility evaluation for both allDetails variants.
     *
     * @param requireAllDetails the {@code allDetails} value this bean's bucket represents.
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

    /** Cache condition for {@code GET /components?allDetails=true}. */
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
                return listingVariantSuffix(request);
            }
        };
    }

    /** Cache condition for {@code GET /components} (allDetails=false or absent). */
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
                return listingVariantSuffix(request);
            }
        };
    }
}
