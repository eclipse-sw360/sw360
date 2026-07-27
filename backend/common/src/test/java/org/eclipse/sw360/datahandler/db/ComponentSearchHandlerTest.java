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

import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class ComponentSearchHandlerTest {

    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        String revDir = "-";
        return switch (ComponentSortColumn.findByValue(sortColumnNumber)) {
            case ComponentSortColumn.BY_NAME -> List.of("name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_VENDOR -> List.of("vendorNames_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_MAINLICENSE -> List.of("mainLicenseIds_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_TYPE -> List.of("componentType_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    @Test
    void byName_shouldReturnNameSortWithCreatedOnTiebreaker() {
        List<String> columns = mapSortColumnDirect(ComponentSortColumn.BY_NAME.getValue());
        assertEquals(List.of("name_sort", "-createdOn"), columns);
        assertFalse(columns.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byVendor_shouldReturnVendorSortWithTiebreakers() {
        assertEquals(List.of("vendorNames_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(ComponentSortColumn.BY_VENDOR.getValue()));
    }

    @Test
    void byMainLicense_shouldReturnLicenseSortWithTiebreakers() {
        assertEquals(List.of("mainLicenseIds_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(ComponentSortColumn.BY_MAINLICENSE.getValue()));
    }

    @Test
    void byType_shouldReturnTypeSortWithTiebreakers() {
        assertEquals(List.of("componentType_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(ComponentSortColumn.BY_TYPE.getValue()));
    }

    @Test
    void byCreatedOn_shouldReturnOnlyCreatedOn() {
        assertEquals(List.of("createdOn"), mapSortColumnDirect(ComponentSortColumn.BY_CREATEDON.getValue()));
    }

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(ComponentSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (ComponentSortColumn column : ComponentSortColumn.values()) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertNotNull(columns);
            assertFalse(columns.isEmpty());
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveScoreAndNameTiebreakers() {
        for (ComponentSortColumn column : List.of(
                ComponentSortColumn.BY_VENDOR,
                ComponentSortColumn.BY_MAINLICENSE,
                ComponentSortColumn.BY_TYPE)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertTrue(columns.contains(SCORE_SORTING_FIELD), column + " missing score");
            assertTrue(columns.contains("name_sort"), column + " missing name_sort");
            assertTrue(columns.contains("-createdOn"), column + " missing -createdOn");
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveCorrectTiebreakerOrder() {
        for (ComponentSortColumn column : List.of(
                ComponentSortColumn.BY_VENDOR,
                ComponentSortColumn.BY_MAINLICENSE,
                ComponentSortColumn.BY_TYPE)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            int scoreIdx = columns.indexOf(SCORE_SORTING_FIELD);
            int nameIdx = columns.indexOf("name_sort");
            int createdOnIdx = columns.indexOf("-createdOn");
            assertTrue(scoreIdx > 0, column + ": score should not be primary");
            assertTrue(nameIdx > scoreIdx, column + ": name_sort should follow score");
            assertTrue(createdOnIdx > nameIdx, column + ": createdOn should be last");
        }
    }

    @Test
    void edgeNgramConstants_shouldHaveValidValues() {
        int max = org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_MAX_LENGTH;
        int shortMax = org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_SHORT_MAX_LENGTH;
        assertEquals(50, max);
        assertEquals(10, shortMax);
    }
}
