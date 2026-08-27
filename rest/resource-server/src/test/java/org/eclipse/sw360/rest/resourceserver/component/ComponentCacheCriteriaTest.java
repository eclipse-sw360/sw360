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
import org.eclipse.sw360.rest.resourceserver.cache.CacheCondition;
import org.eclipse.sw360.rest.resourceserver.cache.CachedEndpoint;
import org.eclipse.sw360.rest.resourceserver.component.cache.ComponentCachingCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Foundational unit tests for the component cache condition/criteria contracts
 * (Phase 2 — Foundational, plus allDetails/sort/first-page follow-ups). Verifies endpoint
 * identity and eligibility wiring for both the {@code componentsWithoutDetailsCacheCondition}
 * and {@code componentsAllDetailsCacheCondition} beans.
 *
 * <p>Uses {@code Strictness.LENIENT} for flexibility across the small number of
 * {@code HttpServletRequest} stubs shared via {@code @BeforeEach}.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComponentCacheCriteriaTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private ComponentCachingCriteria alwaysEligibleCriterion;

    @Mock
    private ComponentCachingCriteria alwaysRejectingCriterion;

    private final ComponentCacheCondition conditionConfig = new ComponentCacheCondition();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/components");
        when(request.getQueryString()).thenReturn(null);
        when(request.getParameterMap()).thenReturn(Map.of());
    }

    @Test
    void withoutDetailsConditionShouldTargetComponentsWithoutDetailsEndpoint() {
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.endpoint()).isEqualTo(CachedEndpoint.COMPONENTS_WITHOUT_DETAILS);
    }

    @Test
    void allDetailsConditionShouldTargetComponentsAllDetailsEndpoint() {
        CacheCondition condition = conditionConfig.componentsAllDetailsCacheCondition(List.of());

        assertThat(condition.endpoint()).isEqualTo(CachedEndpoint.COMPONENTS_ALL_DETAILS);
    }

    @Test
    void shouldBeCacheable_whenPlainListingAndAllCriteriaPass() {
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isTrue();
    }

    @Test
    void shouldBypass_whenAnyCriterionRejects() {
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        when(alwaysRejectingCriterion.shouldCache(request)).thenReturn(false);
        when(alwaysRejectingCriterion.name()).thenReturn("AlwaysRejecting");

        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(
                List.of(alwaysEligibleCriterion, alwaysRejectingCriterion));

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBypass_whenRequestTargetsSubResource() {
        when(request.getRequestURI()).thenReturn("/api/components/1234");
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBypass_whenFilterParametersArePresent() {
        doReturn(Map.of("searchText", new String[]{"apache"})).when(request).getParameterMap();
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBypass_whenLuceneSearchRequested() {
        doReturn(Map.of("luceneSearch", new String[]{"true"})).when(request).getParameterMap();
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBypass_whenUnrecognizedFutureParameterIsPresent() {
        // Acceptance/allow-list guard: a brand-new query parameter this class has never heard
        // of must default to "unsafe" (bypass), not "safe" (cached). This is the behavior a
        // deny-list cannot guarantee once a new filter is added to the controller.
        doReturn(Map.of("someNewFilterAddedLater", new String[]{"x"})).when(request).getParameterMap();
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBeCacheable_whenOnlyPaginationAndSortParamsArePresent() {
        doReturn(Map.of(
                "page", new String[]{"0"},
                "page_entries", new String[]{"5000"},
                "sort", new String[]{"name,asc"}
        )).when(request).getParameterMap();
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isTrue();
    }

    @Test
    void shouldBypass_whenPageIsNotFirstPage() {
        // Only page=0 represents "the whole listing" for the shared single-blob cache; any
        // other page number is a different result subset and must bypass.
        doReturn(Map.of("page", new String[]{"1"})).when(request).getParameterMap();
        when(request.getParameter("page")).thenReturn("1");
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void shouldBeCacheable_whenPageIsExplicitlyZero() {
        doReturn(Map.of("page", new String[]{"0"})).when(request).getParameterMap();
        when(request.getParameter("page")).thenReturn("0");
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isTrue();
    }

    @Test
    void shouldBypass_whenNoCriteriaRegistered() {
        // Per spec edge case: empty criteria list = no positive match = bypass.
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.isCacheable(request)).isFalse();
    }

    // --- allDetails split ---

    @Test
    void withoutDetailsCondition_shouldBypass_whenAllDetailsTrue() {
        doReturn(Map.of("allDetails", new String[]{"true"})).when(request).getParameterMap();
        when(request.getParameter("allDetails")).thenReturn("true");
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isFalse();
    }

    @Test
    void allDetailsCondition_shouldBeCacheable_whenAllDetailsTrue() {
        doReturn(Map.of("allDetails", new String[]{"true"})).when(request).getParameterMap();
        when(request.getParameter("allDetails")).thenReturn("true");
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsAllDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isTrue();
    }

    @Test
    void allDetailsCondition_shouldBypass_whenAllDetailsAbsent() {
        // Absent/false allDetails belongs to the "without details" bucket, not this one.
        when(alwaysEligibleCriterion.shouldCache(request)).thenReturn(true);
        CacheCondition condition = conditionConfig.componentsAllDetailsCacheCondition(List.of(alwaysEligibleCriterion));

        assertThat(condition.isCacheable(request)).isFalse();
    }

    // --- sort-aware cache variant ---

    @Test
    void variantSuffix_shouldBeEmpty_whenNoSortRequested() {
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.variantSuffix(request)).isEmpty();
    }

    @Test
    void variantSuffix_shouldReflectSortParameter() {
        when(request.getParameterValues("sort")).thenReturn(new String[]{"name,asc"});
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        assertThat(condition.variantSuffix(request)).isEqualTo("sort-name-asc");
    }

    @Test
    void variantSuffix_shouldDifferForDifferentSortValues_preventingCollision() {
        CacheCondition condition = conditionConfig.componentsWithoutDetailsCacheCondition(List.of());

        when(request.getParameterValues("sort")).thenReturn(new String[]{"name,asc"});
        String nameAscSuffix = condition.variantSuffix(request);

        when(request.getParameterValues("sort")).thenReturn(new String[]{"createdOn,desc"});
        String createdOnDescSuffix = condition.variantSuffix(request);

        assertThat(nameAscSuffix).isNotEqualTo(createdOnDescSuffix);
    }
}
