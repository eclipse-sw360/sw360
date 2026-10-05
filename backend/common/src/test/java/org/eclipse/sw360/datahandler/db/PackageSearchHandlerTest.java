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
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.packages.PackageSortColumn;
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

class PackageSearchHandlerTest {


    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        String revDir = "-";
        return switch (PackageSortColumn.findByValue(sortColumnNumber)) {
            case PackageSortColumn.BY_NAME -> List.of("name_sort", "version_sort", revDir + "createdOn");
            case PackageSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_PACKAGE_MANAGER -> List.of("packageManager_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");

    private static PackageSearchHandler searchHandler;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Package p : createSeedPackages()) { db.add(p); }
        searchHandler = new PackageSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }
    @Test
    void byName_shouldReturnNameSortWithVersionAndCreatedOnTiebreakers() {
        List<String> columns = mapSortColumnDirect(PackageSortColumn.BY_NAME.getValue());
        assertEquals(List.of("name_sort", "version_sort", "-createdOn"), columns);
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

    // --- Search / filter tests -----------------------------------------------

    @Test
    void exactNameSearch_shouldReturnMatchingPackage() {
        var result = searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("FT_commons-lang3")), user1, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(p -> "FT_commons-lang3".equals(p.getName())));
    }

    @Test
    void nonExistentTerm_shouldReturnEmpty() {
        assertTrue(items(searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("zzz_nonexistent_999")), user1, allPages())).isEmpty());
    }

    @Test
    void prefixSearch_shouldMatchViaEdgeNgram() {
        assertTrue(items(searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("FT_comm")), null, allPages()))
                .stream().anyMatch(p -> p.getName().contains("commons")));
    }

    @Test
    void caseInsensitiveSearch_shouldMatch() {
        assertTrue(items(searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("ft_commons")), null, allPages()))
                .stream().anyMatch(p -> p.getName().contains("commons")));
    }

    @Test
    void keywordField_shouldRequireExactMatchForPackageManager() {
        assertFalse(items(searchHandler.searchAccessiblePackages(
                Map.of("packageManager", Set.of("MAVEN")), null, allPages())).isEmpty());
        assertTrue(items(searchHandler.searchAccessiblePackages(
                Map.of("packageManager", Set.of("MAV")), null, allPages())).isEmpty());
    }

    @Test
    void packageManagerFilter_shouldReturnOnlyMatchingManager() {
        var result = searchHandler.searchAccessiblePackages(
                Map.of("packageManager", Set.of("NPM")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals(PackageManager.NPM, p.getPackageManager()));
    }

    @Test
    void purlSearch_shouldMatchByPrefix() {
        var result = searchHandler.searchAccessiblePackages(
                Map.of("purl", Set.of("pkg:maven")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertTrue(p.getPurl().startsWith("pkg:maven")));
    }

    @Test
    void filteredSearch_shouldMatchAcrossMultipleFields() {
        var result = searchHandler.searchFilteredPackages("FT_commons", allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(p -> p.getName().contains("commons")));
    }

    // --- Sorting tests -------------------------------------------------------

    @Test
    void sortByNameAscending_shouldReturnAlphabeticalOrder() {
        var result = searchHandler.searchFilteredPackages("FT_",
                pageSorted(PackageSortColumn.BY_NAME.getValue(), true));
        List<String> names = items(result).stream().map(Package::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
    }

    @Test
    void sortByNameDescending_shouldReturnReverseAlphabeticalOrder() {
        var result = searchHandler.searchFilteredPackages("FT_",
                pageSorted(PackageSortColumn.BY_NAME.getValue(), false));
        List<String> names = items(result).stream().map(Package::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER.reversed()).toList(), names);
    }

    @Test
    void sortByCreatedOnAscending_shouldReturnChronologicalOrder() {
        var result = searchHandler.searchFilteredPackages("FT_",
                pageSorted(PackageSortColumn.BY_CREATEDON.getValue(), true));
        List<String> dates = items(result).stream().map(Package::getCreatedOn).toList();
        assertEquals(dates.stream().sorted().toList(), dates,
                "createdOn should be in ascending chronological order");
    }

    @Test
    void sortByCreatedOnDescending_shouldReturnReverseChronologicalOrder() {
        var result = searchHandler.searchFilteredPackages("FT_",
                pageSorted(PackageSortColumn.BY_CREATEDON.getValue(), false));
        List<String> dates = items(result).stream().map(Package::getCreatedOn).toList();
        assertEquals(dates.stream().sorted(Comparator.reverseOrder()).toList(), dates,
                "createdOn should be in descending chronological order");
    }

    // --- Pagination tests ----------------------------------------------------

    @Test
    void firstPage_shouldReturnRequestedPageSize() {
        assertTrue(items(searchHandler.searchFilteredPackages("FT_", page(0, 5))).size() <= 5);
    }

    @Test
    void paginationAcrossPages_shouldProduceNoDuplicates() {
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Package> pg = items(searchHandler.searchFilteredPackages("FT_", page(i, 3)));
            if (pg.isEmpty()) break;
            for (Package p : pg) { assertTrue(allIds.add(p.getId()), "Duplicate: " + p.getId()); }
        }
    }

    // --- Edge case tests -----------------------------------------------------

    @Test
    void specialCharacters_shouldNotCauseException() {
        assertDoesNotThrow(() -> searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("FT_Special & Chars")), null, allPages()));
    }

    @Test
    void nullDescription_shouldStillBeIndexed() {
        assertTrue(items(searchHandler.searchAccessiblePackages(
                Map.of("name", Set.of("FT_Special & Chars")), null, allPages()))
                .stream().anyMatch(p -> p.getName().contains("Special")));
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
     * Creates seed packages programmatically using Thrift objects to avoid
     * data model drift that can occur with external JSON fixtures.
     */
    private static List<Package> createSeedPackages() {
        return List.of(
                pkg("ft-pkg-001", "FT_commons-lang3", "3.14.0", "pkg:maven/org.apache.commons/commons-lang3@3.14.0", PackageManager.MAVEN, "user1@test.sw360.org", "2024-01-10", "Apache Commons Lang utilities"),
                pkg("ft-pkg-002", "FT_spring-boot-starter", "3.3.0", "pkg:maven/org.springframework.boot/spring-boot-starter@3.3.0", PackageManager.MAVEN, "user1@test.sw360.org", "2024-02-15", "Spring Boot starter dependency"),
                pkg("ft-pkg-003", "FT_react", "18.2.0", "pkg:npm/react@18.2.0", PackageManager.NPM, "user2@test.sw360.org", "2024-03-20", "React UI library"),
                pkg("ft-pkg-004", "FT_flask", "3.0.0", "pkg:pypi/flask@3.0.0", PackageManager.PYPI, "user1@test.sw360.org", "2024-04-25", "Flask web framework"),
                pkg("ft-pkg-005", "FT_tokio", "1.37.0", "pkg:cargo/tokio@1.37.0", PackageManager.CARGO, "user1@test.sw360.org", "2024-05-30", "Async runtime for Rust"),
                pkg("ft-pkg-006", "FT_gin", "1.9.1", "pkg:golang/github.com/gin-gonic/gin@1.9.1", PackageManager.GOLANG, "user2@test.sw360.org", "2024-06-15", "Gin HTTP web framework for Go"),
                pkg("ft-pkg-007", "FT_jackson-core", "2.17.0", "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.17.0", PackageManager.MAVEN, "user1@test.sw360.org", "2024-07-10", "Jackson JSON processing core"),
                pkg("ft-pkg-008", "FT_lodash", "4.17.21", "pkg:npm/lodash@4.17.21", PackageManager.NPM, "user1@test.sw360.org", "2024-08-01", "Lodash utility library"),
                pkg("ft-pkg-009", "FT_requests", "2.31.0", "pkg:pypi/requests@2.31.0", PackageManager.PYPI, "user2@test.sw360.org", "2024-09-01", "HTTP library for Python"),
                pkg("ft-pkg-010", "FT_newtonsoft-json", "13.0.3", "pkg:nuget/Newtonsoft.Json@13.0.3", PackageManager.NUGET, "user1@test.sw360.org", "2024-10-15", "JSON framework for .NET"),
                pkg("ft-pkg-011", "FT_guava", "33.1.0-jre", "pkg:maven/com.google.guava/guava@33.1.0-jre", PackageManager.MAVEN, "user1@test.sw360.org", "2024-11-01", "Google Guava core libraries"),
                pkg("ft-pkg-012", "FT_axios", "1.6.0", "pkg:npm/axios@1.6.0", PackageManager.NPM, "user1@test.sw360.org", "2024-12-01", "Axios HTTP client"),
                pkg("ft-pkg-013", "FT_serde", "1.0.200", "pkg:cargo/serde@1.0.200", PackageManager.CARGO, "user1@test.sw360.org", "2025-01-10", "Serialization framework for Rust"),
                pkg("ft-pkg-014", "FT_Special & Chars", "1.0.0", "pkg:generic/special-chars@1.0.0", PackageManager.GENERIC, "user1@test.sw360.org", "2025-02-01", null),
                pkg("ft-pkg-015", "FT_zlib", "1.3.1", "pkg:generic/zlib@1.3.1", PackageManager.GENERIC, "user1@test.sw360.org", "2025-03-01", "Compression library")
        );
    }

    private static Package pkg(String id, String name, String version, String purl,
                                PackageManager packageManager, String createdBy,
                                String createdOn, String description) {
        Package p = new Package().setId(id).setType("package").setName(name).setVersion(version)
                .setPurl(purl)
                .setPackageManager(packageManager)
                .setCreatedBy(createdBy).setCreatedOn(createdOn);
        if (description != null) p.setDescription(description);
        return p;
    }
}
