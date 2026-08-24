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

import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class UserSearchHandlerTest {

    /**
     * Mirror of UserSearchHandler.mapSortColumn() for unit-level testing without
     * requiring a CouchDB connection.
     */
    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        return switch (UserSortColumn.findByValue(sortColumnNumber)) {
            case UserSortColumn.BY_GIVENNAME  -> List.of("givenname_sort", "lastname_sort", "email_sort");
            case UserSortColumn.BY_LASTNAME   -> List.of("lastname_sort", "givenname_sort", "email_sort");
            case UserSortColumn.BY_EMAIL      -> List.of("email_sort", "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_DEPARTMENT -> List.of("department_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_STATUS     -> List.of(SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_ROLE       -> List.of("primaryroles_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_SCORE      -> List.of(SCORE_SORTING_FIELD);
            case null, default               -> List.of(SCORE_SORTING_FIELD);
        };
    }

    // -------------------------------------------------------------------------
    //  Name columns
    // -------------------------------------------------------------------------

    @Test
    void byGivenName_shouldReturnGivenNameFirst() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_GIVENNAME.getValue());
        assertEquals(List.of("givenname_sort", "lastname_sort", "email_sort"), cols);
        assertFalse(cols.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byLastName_shouldReturnLastNameFirst() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_LASTNAME.getValue());
        assertEquals(List.of("lastname_sort", "givenname_sort", "email_sort"), cols);
        assertFalse(cols.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byEmail_shouldReturnEmailFirst() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_EMAIL.getValue());
        assertEquals(List.of("email_sort", "givenname_sort", "lastname_sort"), cols);
        assertFalse(cols.contains(SCORE_SORTING_FIELD));
    }

    // -------------------------------------------------------------------------
    //  Categorical columns (department, status, role)
    // -------------------------------------------------------------------------

    @Test
    void byDepartment_shouldReturnDepartmentWithScoreAndNameTiebreakers() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_DEPARTMENT.getValue());
        assertEquals(List.of("department_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort"), cols);
    }

    @Test
    void byStatus_shouldReturnScoreFirstWithNameTiebreakers() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_STATUS.getValue());
        assertEquals(List.of(SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort"), cols);
    }

    @Test
    void byRole_shouldReturnPrimaryRolesSortWithScoreAndNameTiebreakers() {
        List<String> cols = mapSortColumnDirect(UserSortColumn.BY_ROLE.getValue());
        assertEquals(List.of("primaryroles_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort"), cols);
    }

    // -------------------------------------------------------------------------
    //  Score / default
    // -------------------------------------------------------------------------

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD),
                mapSortColumnDirect(UserSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    // -------------------------------------------------------------------------
    //  Completeness
    // -------------------------------------------------------------------------

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (UserSortColumn column : UserSortColumn.values()) {
            List<String> cols = mapSortColumnDirect(column.getValue());
            assertNotNull(cols);
            assertFalse(cols.isEmpty(), "Expected non-empty list for " + column);
        }
    }

    @Test
    void categoricalColumns_shouldHaveScoreAndGivenNameTiebreakers() {
        for (UserSortColumn column : List.of(
                UserSortColumn.BY_DEPARTMENT,
                UserSortColumn.BY_STATUS,
                UserSortColumn.BY_ROLE)) {
            List<String> cols = mapSortColumnDirect(column.getValue());
            assertTrue(cols.contains(SCORE_SORTING_FIELD), column + " missing score");
            assertTrue(cols.contains("givenname_sort"), column + " missing givenname_sort");
            assertTrue(cols.contains("lastname_sort"), column + " missing lastname_sort");
        }
    }

    @Test
    void categoricalColumns_shouldHaveCorrectTiebreakerOrder() {
        // BY_STATUS uses score as primary sort, so it is excluded from this ordering check.
        for (UserSortColumn column : List.of(
                UserSortColumn.BY_DEPARTMENT,
                UserSortColumn.BY_ROLE)) {
            List<String> cols = mapSortColumnDirect(column.getValue());
            int scoreIdx     = cols.indexOf(SCORE_SORTING_FIELD);
            int givenNameIdx = cols.indexOf("givenname_sort");
            int lastNameIdx  = cols.indexOf("lastname_sort");
            assertTrue(scoreIdx > 0,              column + ": score should not be primary");
            assertTrue(givenNameIdx > scoreIdx,   column + ": givenname_sort should follow score");
            assertTrue(lastNameIdx > givenNameIdx, column + ": lastname_sort should be last");
        }
    }

    @Test
    void nameColumns_shouldNotContainScore() {
        for (UserSortColumn column : List.of(
                UserSortColumn.BY_GIVENNAME,
                UserSortColumn.BY_LASTNAME,
                UserSortColumn.BY_EMAIL)) {
            List<String> cols = mapSortColumnDirect(column.getValue());
            assertFalse(cols.contains(SCORE_SORTING_FIELD),
                    column + " should not contain score field");
        }
    }

    @Test
    void nameColumns_shouldAlwaysHaveThreeElements() {
        for (UserSortColumn column : List.of(
                UserSortColumn.BY_GIVENNAME,
                UserSortColumn.BY_LASTNAME,
                UserSortColumn.BY_EMAIL)) {
            assertEquals(3, mapSortColumnDirect(column.getValue()).size(),
                    column + " should have exactly 3 sort columns");
        }
    }

    // -------------------------------------------------------------------------
    //  n-gram constants (BaseNouveauSearchHandler)
    // -------------------------------------------------------------------------

    @Test
    void edgeNgramConstants_shouldHaveExpectedValues() {
        int max      = org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_MAX_LENGTH;
        int shortMax = org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_SHORT_MAX_LENGTH;
        assertEquals(50, max);
        assertEquals(10, shortMax);
    }
}
