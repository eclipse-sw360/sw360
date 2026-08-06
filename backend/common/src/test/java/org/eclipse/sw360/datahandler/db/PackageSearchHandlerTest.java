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

import org.eclipse.sw360.datahandler.thrift.packages.PackageSortColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class PackageSearchHandlerTest {

    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        String revDir = "-";
        return switch (PackageSortColumn.findByValue(sortColumnNumber)) {
            case PackageSortColumn.BY_NAME -> List.of("name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_PACKAGE_MANAGER -> List.of("packageManager_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    @Test
    void byName_shouldReturnNameSortWithCreatedOnTiebreaker() {
        List<String> columns = mapSortColumnDirect(PackageSortColumn.BY_NAME.getValue());
        assertEquals(List.of("name_sort", "-createdOn"), columns);
        assertFalse(columns.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byVersion_shouldReturnVersionSortWithNameAndCreatedOnTiebreakers() {
        assertEquals(List.of("version_sort", "name_sort", "-createdOn"),
                mapSortColumnDirect(PackageSortColumn.BY_VERSION.getValue()));
    }

    @Test
    void byPackageManager_shouldReturnPackageManagerSortWithTiebreakers() {
        assertEquals(List.of("packageManager_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(PackageSortColumn.BY_PACKAGE_MANAGER.getValue()));
    }

    @Test
    void byCreatedOn_shouldReturnOnlyCreatedOn() {
        assertEquals(List.of("createdOn"), mapSortColumnDirect(PackageSortColumn.BY_CREATEDON.getValue()));
    }

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(PackageSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (PackageSortColumn column : PackageSortColumn.values()) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertNotNull(columns);
            assertFalse(columns.isEmpty());
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveScoreAndNameTiebreakers() {
        for (PackageSortColumn column : List.of(PackageSortColumn.BY_PACKAGE_MANAGER)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertTrue(columns.contains(SCORE_SORTING_FIELD), column + " missing score");
            assertTrue(columns.contains("name_sort"), column + " missing name_sort");
            assertTrue(columns.contains("-createdOn"), column + " missing -createdOn");
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveCorrectTiebreakerOrder() {
        List<String> columns = mapSortColumnDirect(PackageSortColumn.BY_PACKAGE_MANAGER.getValue());
        int scoreIdx = columns.indexOf(SCORE_SORTING_FIELD);
        int nameIdx = columns.indexOf("name_sort");
        int createdOnIdx = columns.indexOf("-createdOn");
        assertTrue(scoreIdx < nameIdx, "score should come before name_sort");
        assertTrue(nameIdx < createdOnIdx, "name_sort should come before -createdOn");
    }
}
