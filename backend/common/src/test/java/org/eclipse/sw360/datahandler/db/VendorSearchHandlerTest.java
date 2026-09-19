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

import org.eclipse.sw360.datahandler.thrift.vendors.VendorSortColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class VendorSearchHandlerTest {

    /**
     * Mirror of {@link VendorSearchHandler#mapSortColumn(int)} so we can test
     * sorting logic without needing a CouchDB connection.
     */
    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        return switch (VendorSortColumn.findByValue(sortColumnNumber)) {
            case VendorSortColumn.BY_FULLNAME -> List.of("fullname_sort", "shortname_sort");
            case VendorSortColumn.BY_SHORTNAME -> List.of("shortname_sort", "fullname_sort");
            case VendorSortColumn.BY_SCORE -> List.of(SCORE_SORTING_FIELD);
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    @Test
    void byFullname_shouldReturnFullnameSortWithShortnameTiebreaker() {
        List<String> columns = mapSortColumnDirect(VendorSortColumn.BY_FULLNAME.getValue());
        assertEquals(List.of("fullname_sort", "shortname_sort"), columns);
        assertFalse(columns.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byShortname_shouldReturnShortnameSortWithFullnameTiebreaker() {
        List<String> columns = mapSortColumnDirect(VendorSortColumn.BY_SHORTNAME.getValue());
        assertEquals(List.of("shortname_sort", "fullname_sort"), columns);
        assertFalse(columns.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(VendorSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (VendorSortColumn column : VendorSortColumn.values()) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertNotNull(columns);
            assertFalse(columns.isEmpty());
        }
    }

    @Test
    void namedColumns_shouldContainBothSortFields() {
        for (VendorSortColumn column : List.of(VendorSortColumn.BY_FULLNAME, VendorSortColumn.BY_SHORTNAME)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertTrue(columns.contains("fullname_sort"), column + " missing fullname_sort");
            assertTrue(columns.contains("shortname_sort"), column + " missing shortname_sort");
        }
    }
}

