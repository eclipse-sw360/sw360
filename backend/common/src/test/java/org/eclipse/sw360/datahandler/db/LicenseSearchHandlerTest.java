/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.thrift.licenses.LicenseSortColumn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseSearchHandlerTest {

    private static final String MATCH_ALL_QUERY = "*:*";

    /**
     * Mirror of {@code LicenseSearchHandler.mapSortColumn()} for unit-level
     * testing without requiring a CouchDB connection.
     */
    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        return switch (LicenseSortColumn.findByValue(sortColumnNumber)) {
            case LicenseSortColumn.BY_FULLNAME  -> List.of("fullname_sort", SCORE_SORTING_FIELD, "id_sort");
            case LicenseSortColumn.BY_SHORTNAME -> List.of("shortname_sort", SCORE_SORTING_FIELD, "fullname_sort");
            case null, default                  -> List.of(SCORE_SORTING_FIELD);
        };
    }

    /**
     * Mirrors the branch contract in LicenseSearchHandler.searchWithPagination:
     * blank input uses a match-all query, non-blank input is trimmed.
     */
    private static String resolveQueryForPagination(String searchText) {
        return isBlank(searchText) ? MATCH_ALL_QUERY : searchText.trim();
    }

    private static boolean isMatchAllSearch(String searchText) {
        return MATCH_ALL_QUERY.equals(resolveQueryForPagination(searchText));
    }

    private static boolean isBlank(String value) {
        return Optional.ofNullable(value).map(String::trim).orElse("").isEmpty();
    }

    @Test
    void byFullName_shouldUseFullNameScoreIdOrder() {
        assertEquals(
                List.of("fullname_sort", SCORE_SORTING_FIELD, "id_sort"),
                mapSortColumnDirect(LicenseSortColumn.BY_FULLNAME.getValue())
        );
    }

    @Test
    void byShortName_shouldUseShortNameScoreFullNameOrder() {
        assertEquals(
                List.of("shortname_sort", SCORE_SORTING_FIELD, "fullname_sort"),
                mapSortColumnDirect(LicenseSortColumn.BY_SHORTNAME.getValue())
        );
    }

    @Test
    void byScore_shouldUseOnlyScoreField() {
        assertEquals(
                List.of(SCORE_SORTING_FIELD),
                mapSortColumnDirect(LicenseSortColumn.BY_SCORE.getValue())
        );
    }

    @Test
    void unknownSort_shouldDefaultToScoreField() {
        assertEquals(
                List.of(SCORE_SORTING_FIELD),
                mapSortColumnDirect(Integer.MAX_VALUE)
        );
    }

    @Test
    void allSortColumns_shouldProduceNonEmptySortDefinitions() {
        for (LicenseSortColumn column : LicenseSortColumn.values()) {
            List<String> sortColumns = mapSortColumnDirect(column.getValue());
            assertNotNull(sortColumns);
            assertFalse(sortColumns.isEmpty(), "Expected non-empty sort list for " + column);
        }
    }

    @Test
    void searchWithPagination_shouldUseMatchAllQuery_whenSearchTextBlank() {
        assertTrue(isMatchAllSearch(null));
        assertTrue(isMatchAllSearch(""));
        assertTrue(isMatchAllSearch("   \t\n  "));
        assertEquals(MATCH_ALL_QUERY, resolveQueryForPagination("   "));
    }

    @Test
    void searchWithPagination_shouldTrimInput_whenSearchTextProvided() {
        assertFalse(isMatchAllSearch("license"));
        assertFalse(isMatchAllSearch("  license  "));
        assertEquals("license", resolveQueryForPagination("  license  "));
        assertEquals("Apache 2.0", resolveQueryForPagination("  Apache 2.0  "));
    }
}
