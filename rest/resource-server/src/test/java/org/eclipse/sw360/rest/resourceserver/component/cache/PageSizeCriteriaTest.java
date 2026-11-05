/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.component.cache;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PageSizeCriteriaTest {

    private static final int TEST_MIN_PAGE_SIZE = 1000;

    @Mock
    private HttpServletRequest request;

    private final PageSizeCriteria criteria = new PageSizeCriteria(TEST_MIN_PAGE_SIZE);

    @Test
    void shouldReturnFalse_whenPageEntriesParamMissing() {
        when(request.getParameter("page_entries")).thenReturn(null);

        assertThat(criteria.shouldCache(request)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenPageEntriesParamBlank() {
        when(request.getParameter("page_entries")).thenReturn("   ");

        assertThat(criteria.shouldCache(request)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenPageEntriesParamNonNumeric() {
        when(request.getParameter("page_entries")).thenReturn("abc");

        assertThat(criteria.shouldCache(request)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenPageEntriesParamZeroOrNegative() {
        when(request.getParameter("page_entries")).thenReturn("0");
        assertThat(criteria.shouldCache(request)).isFalse();

        when(request.getParameter("page_entries")).thenReturn("-5");
        assertThat(criteria.shouldCache(request)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenPageEntriesBelowThreshold() {
        when(request.getParameter("page_entries")).thenReturn("50");

        assertThat(criteria.shouldCache(request)).isFalse();
    }

    @Test
    void shouldReturnTrue_whenPageEntriesMeetsThreshold() {
        when(request.getParameter("page_entries")).thenReturn(String.valueOf(TEST_MIN_PAGE_SIZE));

        assertThat(criteria.shouldCache(request)).isTrue();
    }

    @Test
    void shouldReturnTrue_whenPageEntriesExceedsThreshold() {
        when(request.getParameter("page_entries")).thenReturn(String.valueOf(TEST_MIN_PAGE_SIZE * 5));

        assertThat(criteria.shouldCache(request)).isTrue();
    }

    @Test
    void shouldExposeStableCriterionName() {
        assertThat(criteria.name()).isEqualTo("PageSizeCriteria");
    }
}
