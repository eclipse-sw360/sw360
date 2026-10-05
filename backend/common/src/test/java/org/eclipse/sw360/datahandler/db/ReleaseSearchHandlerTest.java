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
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseSearchHandlerTest {

    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        String revDir = "-";
        return switch (ReleaseSortColumn.findByValue(sortColumnNumber)) {
            case ReleaseSortColumn.BY_NAME -> List.of("name_sort", revDir + "version_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_CLEARING_STATE -> List.of("clearingState_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_MAINLINE_STATE -> List.of("mainlineState_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");

    private static ReleaseSearchHandler searchHandler;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(
                DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Release r : createSeedReleases()) { db.add(r); }
        searchHandler = new ReleaseSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    void byName_shouldReturnNameSortWithVersionAndCreatedOnTiebreakers() {
        List<String> columns = mapSortColumnDirect(ReleaseSortColumn.BY_NAME.getValue());
        assertEquals(List.of("name_sort", "-version_sort", "-createdOn"), columns);
        assertFalse(columns.contains(SCORE_SORTING_FIELD));
    }

    @Test
    void byVersion_shouldReturnVersionSortWithNameAndCreatedOnTiebreakers() {
        assertEquals(List.of("version_sort", "name_sort", "-createdOn"),
                mapSortColumnDirect(ReleaseSortColumn.BY_VERSION.getValue()));
    }

    @Test
    void byClearingState_shouldReturnClearingStateSortWithTiebreakers() {
        assertEquals(List.of("clearingState_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(ReleaseSortColumn.BY_CLEARING_STATE.getValue()));
    }

    @Test
    void byMainlineState_shouldReturnMainlineStateSortWithTiebreakers() {
        assertEquals(List.of("mainlineState_sort", SCORE_SORTING_FIELD, "name_sort", "-createdOn"),
                mapSortColumnDirect(ReleaseSortColumn.BY_MAINLINE_STATE.getValue()));
    }

    @Test
    void byCreatedOn_shouldReturnOnlyCreatedOn() {
        assertEquals(List.of("createdOn"), mapSortColumnDirect(ReleaseSortColumn.BY_CREATEDON.getValue()));
    }

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(ReleaseSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (ReleaseSortColumn column : ReleaseSortColumn.values()) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertNotNull(columns);
            assertFalse(columns.isEmpty());
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveScoreAndNameTiebreakers() {
        for (ReleaseSortColumn column : List.of(
                ReleaseSortColumn.BY_CLEARING_STATE,
                ReleaseSortColumn.BY_MAINLINE_STATE)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            assertTrue(columns.contains(SCORE_SORTING_FIELD), column + " missing score");
            assertTrue(columns.contains("name_sort"), column + " missing name_sort");
            assertTrue(columns.contains("-createdOn"), column + " missing -createdOn");
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveCorrectTiebreakerOrder() {
        for (ReleaseSortColumn column : List.of(
                ReleaseSortColumn.BY_CLEARING_STATE,
                ReleaseSortColumn.BY_MAINLINE_STATE)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            int scoreIdx = columns.indexOf(SCORE_SORTING_FIELD);
            int nameIdx = columns.indexOf("name_sort");
            int createdOnIdx = columns.indexOf("-createdOn");
            assertTrue(scoreIdx > 0, column + ": score should not be primary");
            assertTrue(nameIdx > scoreIdx, column + ": name_sort should follow score");
            assertTrue(createdOnIdx > nameIdx, column + ": createdOn should be last");
        }
    }


    // --- Search / filter tests -----------------------------------------------

    @Test
    void exactNameSearch_shouldReturnMatchingRelease() {
        var result = searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("FT_Apache Commons")), user1, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(r -> "FT_Apache Commons".equals(r.getName())));
    }

    @Test
    void nonExistentTerm_shouldReturnEmpty() {
        assertTrue(items(searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("zzz_nonexistent_999")), user1, allPages())).isEmpty());
    }

    @Test
    void prefixSearch_shouldMatchViaEdgeNgram() {
        assertTrue(items(searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("FT_Apa")), user1, allPages()))
                .stream().anyMatch(r -> r.getName().contains("Apache")));
    }

    @Test
    void secondWordPrefixSearch_shouldMatchViaEdgeNgram() {
        // "Frame" is an edge-ngram prefix of "Framework"
        assertTrue(items(searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("Frame")), null, allPages()))
                .stream().anyMatch(r -> r.getName().contains("Framework")));
    }

    @Test
    void caseInsensitiveSearch_shouldMatch() {
        assertTrue(items(searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("ft_apache")), user1, allPages()))
                .stream().anyMatch(r -> r.getName().contains("Apache")));
    }

    @Test
    void keywordField_shouldRequireExactMatch() {
        assertFalse(items(searchHandler.searchAccessibleReleases(
                Map.of("clearingState", Set.of("APPROVED")), user1, allPages())).isEmpty());
        assertTrue(items(searchHandler.searchAccessibleReleases(
                Map.of("clearingState", Set.of("APP")), user1, allPages())).isEmpty());
    }

    @Test
    void clearingStateFilter_shouldReturnOnlyMatchingState() {
        var result = searchHandler.searchAccessibleReleases(
                Map.of("clearingState", Set.of("APPROVED")), user1, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(r -> assertEquals(ClearingState.APPROVED, r.getClearingState()));
    }

    @Test
    void mainlineStateFilter_shouldReturnOnlyMatchingState() {
        var result = searchHandler.searchAccessibleReleases(
                Map.of("mainlineState", Set.of("MAINLINE")), user1, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(r -> assertEquals(MainlineState.MAINLINE, r.getMainlineState()));
    }

    @Test
    void versionSearch_shouldReturnExactVersionMatch() {
        var result = searchHandler.searchAccessibleReleases(
                Map.of("version", Set.of("1.2.3")), user1, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(r -> assertEquals("1.2.3", r.getVersion()));
    }

    // --- Sorting tests -------------------------------------------------------

    @Test
    void sortByNameAscending_shouldReturnAlphabeticalOrder() {
        var result = searchHandler.searchFilteredReleases("FT_", user1,
                pageSorted(ReleaseSortColumn.BY_NAME.getValue(), true));
        List<String> names = items(result).stream().map(Release::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
    }

    @Test
    void sortByNameDescending_shouldReturnReverseAlphabeticalOrder() {
        var result = searchHandler.searchFilteredReleases("FT_", user1,
                pageSorted(ReleaseSortColumn.BY_NAME.getValue(), false));
        List<String> names = items(result).stream().map(Release::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER.reversed()).toList(), names);
    }

    @Test
    void sortByName_sameNameReleasesShouldTieBreakByVersionDescending() {
        var result = searchHandler.searchFilteredReleases("FT_Apache", user1,
                pageSorted(ReleaseSortColumn.BY_NAME.getValue(), true));
        List<String> apacheVersions = items(result).stream()
                .filter(r -> "FT_Apache Commons".equals(r.getName()))
                .map(Release::getVersion)
                .toList();
        assertEquals(List.of("2.0.0", "1.0.0"), apacheVersions,
                "Same-name releases must be ordered by version descending");
    }

    @Test
    void sortByClearingStateAscending_shouldReturnGroupedByState() {
        var result = searchHandler.searchFilteredReleases("FT_", user1,
                pageSorted(ReleaseSortColumn.BY_CLEARING_STATE.getValue(), true));
        List<String> states = items(result).stream().map(r -> r.getClearingState().name()).toList();
        assertEquals(states.stream().sorted().toList(), states,
                "Clearing states should appear in ascending alphabetical order");
    }

    @Test
    void sortByCreatedOnAscending_shouldReturnChronologicalOrder() {
        var result = searchHandler.searchFilteredReleases("FT_", user1,
                pageSorted(ReleaseSortColumn.BY_CREATEDON.getValue(), true));
        List<String> dates = items(result).stream().map(Release::getCreatedOn).toList();
        assertEquals(dates.stream().sorted().toList(), dates,
                "createdOn should be in ascending chronological order");
    }

    @Test
    void sortByCreatedOnDescending_shouldReturnReverseChronologicalOrder() {
        var result = searchHandler.searchFilteredReleases("FT_", user1,
                pageSorted(ReleaseSortColumn.BY_CREATEDON.getValue(), false));
        List<String> dates = items(result).stream().map(Release::getCreatedOn).toList();
        assertEquals(dates.stream().sorted(Comparator.reverseOrder()).toList(), dates,
                "createdOn should be in descending chronological order");
    }

    // --- Pagination tests ----------------------------------------------------

    @Test
    void firstPage_shouldReturnRequestedPageSize() {
        assertTrue(items(searchHandler.searchFilteredReleases("FT_", user1, page(0, 5))).size() <= 5);
    }

    @Test
    void paginationAcrossPages_shouldProduceNoDuplicates() {
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Release> pg = items(searchHandler.searchFilteredReleases("FT_", user1, page(i, 3)));
            if (pg.isEmpty()) break;
            for (Release r : pg) { assertTrue(allIds.add(r.getId()), "Duplicate: " + r.getId()); }
        }
    }

    // --- Edge case tests -----------------------------------------------------

    @Test
    void specialCharacters_shouldNotCauseException() {
        assertDoesNotThrow(() -> searchHandler.searchAccessibleReleases(
                Map.of("name", Set.of("FT_Alpha & Beta")), user1, allPages()));
    }

    // --- Visibility tests ----------------------------------------------------

    @Test
    void visibility_allUsersSeeAllReleases() {
        User outsider = new User().setEmail("outsider@test.com").setDepartment("OTHER_DEPT");
        List<Release> allReleases = items(searchHandler.searchAccessibleReleases(Map.of(), null, allPages()));
        List<Release> user1Releases = items(searchHandler.searchAccessibleReleases(Map.of(), user1, allPages()));
        List<Release> outsiderReleases = items(searchHandler.searchAccessibleReleases(Map.of(), outsider, allPages()));

        assertEquals(allReleases.size(), user1Releases.size());
        assertEquals(allReleases.size(), outsiderReleases.size());
        assertFalse(allReleases.isEmpty());
    }

    @Test
    void buildVisibilityLuceneQuery_returnsNull() {
        assertNull(ReleaseSearchHandler.buildVisibilityLuceneQuery(user1));
        assertNull(ReleaseSearchHandler.buildVisibilityLuceneQuery(null));
    }

    @Test
    void searchAccessibleReleasesFromComponent_shouldFilterByComponentAndOptionalSearchText() {
        User outsider = new User().setEmail("outsider@test.com").setDepartment("OTHER_DEPT");

        // Without searchText
        var byCompNullUser = items(searchHandler.searchAccessibleReleasesFromComponent("ft-c-001", null, null, allPages()));
        var byCompUser1 = items(searchHandler.searchAccessibleReleasesFromComponent("ft-c-001", null, user1, allPages()));
        var byCompOutsider = items(searchHandler.searchAccessibleReleasesFromComponent("ft-c-001", null, outsider, allPages()));

        assertEquals(2, byCompNullUser.size());
        assertEquals(2, byCompUser1.size());
        assertEquals(2, byCompOutsider.size());
        byCompUser1.forEach(r -> assertEquals("ft-c-001", r.getComponentId()));

        // With searchText matching version
        var byCompWithSearch = items(searchHandler.searchAccessibleReleasesFromComponent("ft-c-001", "1.0.0", user1, allPages()));
        assertEquals(1, byCompWithSearch.size());
        assertEquals("1.0.0", byCompWithSearch.getFirst().getVersion());
        assertEquals("ft-c-001", byCompWithSearch.getFirst().getComponentId());
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
     * Creates seed releases programmatically using Thrift objects to avoid
     * data model drift that can occur with external JSON fixtures.
     */
    private static List<Release> createSeedReleases() {
        return List.of(
                rel("ft-rel-001", "FT_Apache Commons", "1.0.0", "ft-c-001", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2024-01-15"),
                rel("ft-rel-002", "FT_Apache Commons", "2.0.0", "ft-c-001", ClearingState.REPORT_AVAILABLE, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-06-15"),
                rel("ft-rel-003", "FT_Spring Framework", "5.3.0", "ft-c-002", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-03-01"),
                rel("ft-rel-004", "FT_Spring Framework", "6.0.0", "ft-c-002", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user2@test.sw360.org", "2024-09-01"),
                rel("ft-rel-005", "FT_React UI", "18.0.0", "ft-c-003", ClearingState.APPROVED, MainlineState.MAINLINE, "user2@test.sw360.org", "2024-04-01"),
                rel("ft-rel-006", "FT_Bootstrap UI", "5.3.0", "ft-c-006", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2024-07-01"),
                rel("ft-rel-007", "FT_Logging Library", "2.20.0", "ft-c-007", ClearingState.UNDER_CLEARING, MainlineState.SPECIFIC, "user1@test.sw360.org", "2024-08-01"),
                rel("ft-rel-008", "FT_Security Module", "1.0.0", "ft-c-009", ClearingState.APPROVED, MainlineState.MAINLINE, "user2@test.sw360.org", "2024-10-01"),
                rel("ft-rel-009", "FT_Internal Tool", "1.0.0", "ft-c-004", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2024-05-01"),
                rel("ft-rel-010", "FT_libA", "1.2.3", "ft-c-007", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-11-01"),
                rel("ft-rel-011", "FT_libB", "1.2.3", "ft-c-007", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-11-02"),
                rel("ft-rel-012", "FT_libM", "1.2.3", "ft-c-007", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-11-03"),
                rel("ft-rel-013", "FT_libZ", "1.2.3", "ft-c-007", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2024-11-04"),
                rel("ft-rel-014", "FT_libK", "2.0.0", "ft-c-007", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2024-11-05"),
                rel("ft-rel-015", "FT_CLI Tool", "0.9.0", "ft-c-010", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2025-01-01"),
                rel("ft-rel-016", "FT_Alpha & Beta", "1.0.0", "ft-c-016", ClearingState.NEW_CLEARING, MainlineState.OPEN, "user1@test.sw360.org", "2025-02-01"),
                rel("ft-rel-017", "FT_Analytics Engine", "3.1.0", "ft-c-018", ClearingState.UNDER_CLEARING, MainlineState.SPECIFIC, "user2@test.sw360.org", "2025-03-01"),
                rel("ft-rel-018", "FT_Zulu Runtime", "21.0.0", "ft-c-019", ClearingState.APPROVED, MainlineState.MAINLINE, "user1@test.sw360.org", "2025-04-01")
        );
    }

    private static Release rel(String id, String name, String version, String componentId,
                                ClearingState clearingState, MainlineState mainlineState,
                                String createdBy, String createdOn) {
        return new Release().setId(id).setType("release").setName(name).setVersion(version)
                .setComponentId(componentId)
                .setClearingState(clearingState)
                .setMainlineState(mainlineState)
                .setCreatedBy(createdBy).setCreatedOn(createdOn);
    }
}
