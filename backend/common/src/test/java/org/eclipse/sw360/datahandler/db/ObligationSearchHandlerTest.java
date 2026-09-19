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

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationSortColumn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class ObligationSearchHandlerTest {



    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        return switch (ObligationSortColumn.findByValue(sortColumnNumber)) {
            case ObligationSortColumn.BY_TITLE -> List.of("title_sort", SCORE_SORTING_FIELD, "text_sort");
            case ObligationSortColumn.BY_TEXT -> List.of("text_sort", SCORE_SORTING_FIELD, "title_sort");
            case ObligationSortColumn.BY_LEVEL -> List.of("obligationLevel_sort");
            case null, default -> List.of(SCORE_SORTING_FIELD, "title_sort", "text_sort");
        };
    }

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;

    private static ObligationSearchHandler searchHandler;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Obligation o : createSeedObligations()) { db.add(o); }
        searchHandler = new ObligationSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    void byTitle_shouldReturnTitleSortWithScoreAndTextTiebreakers() {
        assertEquals(List.of("title_sort", SCORE_SORTING_FIELD, "text_sort"),
                mapSortColumnDirect(ObligationSortColumn.BY_TITLE.getValue()));
    }

    @Test
    void byText_shouldReturnTextSortWithScoreAndTitleTiebreakers() {
        assertEquals(List.of("text_sort", SCORE_SORTING_FIELD, "title_sort"),
                mapSortColumnDirect(ObligationSortColumn.BY_TEXT.getValue()));
    }

    @Test
    void byLevel_shouldReturnOnlyObligationLevelSort() {
        assertEquals(List.of("obligationLevel_sort"),
                mapSortColumnDirect(ObligationSortColumn.BY_LEVEL.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScoreWithTiebreakers() {
        assertEquals(List.of(SCORE_SORTING_FIELD, "title_sort", "text_sort"),
                mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (ObligationSortColumn column : ObligationSortColumn.values()) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertNotNull(columns);
            assertFalse(columns.isEmpty(), column + " returned empty list");
        }
    }

    @Test
    void namedColumns_shouldContainScoreField() {
        for (ObligationSortColumn column : List.of(
                ObligationSortColumn.BY_TITLE,
                ObligationSortColumn.BY_TEXT)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertTrue(columns.contains(SCORE_SORTING_FIELD), column + " missing score");
        }
    }



    // --- Search / filter tests -----------------------------------------------

    @Test
    void titleSearch_shouldReturnMatchingObligation() {
        var result = searchHandler.searchWithPagination("FT_Attribution", null, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(o -> o.getTitle().contains("Attribution")));
    }

    @Test
    void textSearch_shouldReturnMatchingObligation() {
        var result = searchHandler.searchWithPagination("copyright", null, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(o -> o.getText().toLowerCase().contains("copyright")));
    }

    @Test
    void nonExistentTerm_shouldReturnEmpty() {
        assertTrue(items(searchHandler.searchWithPagination("zzz_nonexistent_999", null, allPages())).isEmpty());
    }

    @Test
    void prefixSearch_shouldMatchViaEdgeNgram() {
        // "FT_Attrib" is an edge-ngram prefix of "FT_Attribution Notice"
        assertTrue(items(searchHandler.searchWithPagination("FT_Attrib", null, allPages()))
                .stream().anyMatch(o -> o.getTitle().contains("Attribution")));
    }

    @Test
    void levelFilter_shouldReturnOnlyMatchingLevel() {
        var result = searchHandler.searchWithPagination(null, ObligationLevel.LICENSE_OBLIGATION, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(o -> assertEquals(ObligationLevel.LICENSE_OBLIGATION, o.getObligationLevel()));
    }

    @Test
    void combinedTextAndLevelFilter_shouldReturnIntersection() {
        var result = searchHandler.searchWithPagination("FT_", ObligationLevel.PROJECT_OBLIGATION, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(o -> assertEquals(ObligationLevel.PROJECT_OBLIGATION, o.getObligationLevel()));
    }

    @Test
    void allLevelsPresent_shouldReturnNonEmptyForEachLevel() {
        for (ObligationLevel level : ObligationLevel.values()) {
            var result = searchHandler.searchWithPagination(null, level, allPages());
            assertFalse(items(result).isEmpty(), "No obligations found for level: " + level);
        }
    }

    // --- Sorting tests -------------------------------------------------------

    @Test
    void sortByTitleAscending_shouldReturnAlphabeticalOrder() {
        var result = searchHandler.searchWithPagination("FT_", null,
                pageSorted(ObligationSortColumn.BY_TITLE.getValue(), true));
        List<String> titles = items(result).stream().map(Obligation::getTitle).toList();
        assertEquals(titles.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), titles);
    }

    @Test
    void sortByTitleDescending_shouldReturnReverseAlphabeticalOrder() {
        var result = searchHandler.searchWithPagination("FT_", null,
                pageSorted(ObligationSortColumn.BY_TITLE.getValue(), false));
        List<String> titles = items(result).stream().map(Obligation::getTitle).toList();
        assertEquals(titles.stream().sorted(String.CASE_INSENSITIVE_ORDER.reversed()).toList(), titles);
    }

    @Test
    void sortByLevelAscending_shouldReturnGroupedByLevel() {
        var result = searchHandler.searchWithPagination("FT_", null,
                pageSorted(ObligationSortColumn.BY_LEVEL.getValue(), true));
        List<String> levels = items(result).stream().map(o -> o.getObligationLevel().name()).toList();
        assertFalse(levels.isEmpty());
        assertEquals(levels.stream().sorted().toList(), levels,
                "Obligation levels should appear in ascending alphabetical order");
    }

    // --- Pagination tests ----------------------------------------------------

    @Test
    void firstPage_shouldReturnRequestedPageSize() {
        assertTrue(items(searchHandler.searchWithPagination("FT_", null, page(0, 3))).size() <= 3);
    }

    @Test
    void paginationAcrossPages_shouldProduceNoDuplicates() {
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Obligation> pg = items(searchHandler.searchWithPagination("FT_", null, page(i, 3)));
            if (pg.isEmpty()) break;
            for (Obligation o : pg) { assertTrue(allIds.add(o.getId()), "Duplicate: " + o.getId()); }
        }
    }

    // --- Edge case tests -----------------------------------------------------

    @Test
    void emptySearchText_shouldReturnAllWhenLevelProvided() {
        var result = searchHandler.searchWithPagination("", ObligationLevel.ORGANISATION_OBLIGATION, allPages());
        assertFalse(items(result).isEmpty());
    }

    @Test
    void nullSearchTextAndLevel_shouldReturnEmptyWhenNoFiltersProvided() {
        var result = searchHandler.searchWithPagination(null, null, allPages());
        assertTrue(items(result).isEmpty(),
                "Empty restrictions should yield no results from complexBaseSearch");
    }

    // =========================================================================
    //  Test data & helpers
    // =========================================================================

    private static <T> List<T> items(Map<PaginationData, List<T>> r) {
        return r.values().iterator().next();
    }

    private PaginationData allPages() {
        return NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
    }

    private PaginationData page(int p, int size) {
        return new PaginationData().setRowsPerPage(size).setDisplayStart(p * size)
                .setAscending(true).setSortColumnNumber(0);
    }

    private PaginationData pageSorted(int col, boolean asc) {
        return new PaginationData().setRowsPerPage(200).setDisplayStart(0)
                .setAscending(asc).setSortColumnNumber(col);
    }

    /**
     * Creates seed obligations programmatically using Thrift objects to avoid
     * data model drift that can occur with external JSON fixtures.
     */
    private static List<Obligation> createSeedObligations() {
        return List.of(
                obl("ft-obl-001", "FT_Attribution Notice", "Include copyright notice in documentation", ObligationLevel.LICENSE_OBLIGATION),
                obl("ft-obl-002", "FT_Binary Distribution", "Provide source code alongside binary distribution", ObligationLevel.COMPONENT_OBLIGATION),
                obl("ft-obl-003", "FT_Change Log Required", "Document all modifications in a change log", ObligationLevel.PROJECT_OBLIGATION),
                obl("ft-obl-004", "FT_Disclose Source", "Make source code publicly available", ObligationLevel.ORGANISATION_OBLIGATION),
                obl("ft-obl-005", "FT_Export Control Check", "Verify export control classification before distribution", ObligationLevel.ORGANISATION_OBLIGATION),
                obl("ft-obl-006", "FT_Include License Text", "Include full license text with the software", ObligationLevel.LICENSE_OBLIGATION),
                obl("ft-obl-007", "FT_No Trademark Use", "Do not use project trademarks without permission", ObligationLevel.COMPONENT_OBLIGATION),
                obl("ft-obl-008", "FT_Patent Grant", "Grant patent rights to downstream recipients", ObligationLevel.LICENSE_OBLIGATION),
                obl("ft-obl-009", "FT_Reciprocal Sharing", "Share modifications under the same license terms", ObligationLevel.PROJECT_OBLIGATION),
                obl("ft-obl-010", "FT_Security Review", "Conduct security review before each release", ObligationLevel.PROJECT_OBLIGATION),
                obl("ft-obl-011", "FT_Vulnerability Scan", "Scan dependencies for known vulnerabilities", ObligationLevel.COMPONENT_OBLIGATION),
                obl("ft-obl-012", "FT_Written Offer", "Provide written offer for source code valid for three years", ObligationLevel.ORGANISATION_OBLIGATION)
        );
    }

    private static Obligation obl(String id, String title, String text, ObligationLevel level) {
        return new Obligation().setId(id).setType("obligation")
                .setTitle(title).setText(text)
                .setObligationLevel(level);
    }
}
