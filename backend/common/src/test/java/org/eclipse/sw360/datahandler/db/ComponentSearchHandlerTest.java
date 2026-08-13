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
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
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

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");

    private static ComponentSearchHandler searchHandler;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Component c : createSeedComponents()) { db.add(c); }
        searchHandler = new ComponentSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
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

    // --- Search / filter tests -----------------------------------------------

    @Test
    void exactNameSearch_shouldReturnMatchingComponent() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("FT_Apache Commons")), user1, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(c -> "FT_Apache Commons".equals(c.getName())));
    }

    @Test
    void nonExistentTerm_shouldReturnEmpty() {
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("zzz_nonexistent_999")), user1, allPages())).isEmpty());
    }

    @Test
    void prefixSearch_shouldMatchViaEdgeNgram() {
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("FT_Apa")), user1, allPages()))
                .stream().anyMatch(c -> c.getName().contains("Apache")));
    }

    @Test
    void secondWordPrefixSearch_shouldMatchViaEdgeNgram() {
        // "Commo" is an edge-ngram prefix of "Commons" — must match "FT_Apache Commons"
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("Commo")), null, allPages()))
                .stream().anyMatch(c -> c.getName().contains("Commons")));
    }

    @Test
    void shortPrefixSearch_shouldMatchMultipleComponentsWithSamePrefix() {
        // "FT_B" matches "FT_Bootstrap UI", "FT_Bare Component", "FT_Build Plugin"
        var result = items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("FT_B")), null, allPages()));
        assertTrue(result.size() >= 2);
        result.forEach(c -> assertTrue(c.getName().startsWith("FT_B"),
                "Unexpected match: " + c.getName()));
    }

    @Test
    void caseInsensitiveSearch_shouldMatch() {
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("ft_apache")), user1, allPages()))
                .stream().anyMatch(c -> c.getName().contains("Apache")));
    }

    @Test
    void keywordField_shouldRequireExactMatch() {
        assertFalse(items(searchHandler.searchAccessibleComponents(
                Map.of("componentType", Set.of("OSS")), null, allPages())).isEmpty());
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("componentType", Set.of("OS")), null, allPages())).isEmpty());
    }

    @Test
    void emailField_shouldMatchCreator() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("createdBy", Set.of("user1@test.sw360.org")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(c -> assertEquals("user1@test.sw360.org", c.getCreatedBy()));
    }

    @Test
    void multiFieldAndFilter_shouldReturnIntersection() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("componentType", Set.of("OSS"), "createdBy", Set.of("user2@test.sw360.org")),
                null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(c -> {
            assertEquals("OSS", c.getComponentType().name());
            assertEquals("user2@test.sw360.org", c.getCreatedBy());
        });
    }

    @Test
    void categoriesArraySearch_shouldMatchSingleElement() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("categories", Set.of("Library")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(c -> assertTrue(c.getCategories() != null && c.getCategories().contains("Library")));
    }

    @Test
    void languagesArraySearch_shouldFilterByLanguage() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("languages", Set.of("Java")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(c -> assertTrue(c.getLanguages() != null && c.getLanguages().contains("Java")));
    }

    @Test
    void componentTypeFilter_shouldReturnExactMatch() {
        var result = searchHandler.searchAccessibleComponents(
                Map.of("componentType", Set.of("INNER_SOURCE")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(c -> assertEquals("INNER_SOURCE", c.getComponentType().name()));
    }

    // --- Sorting tests -------------------------------------------------------

    @Test
    void sortByNameAscending_shouldReturnAlphabeticalOrder() {
        var result = searchHandler.searchFilteredComponents("FT_", user1,
                pageSorted(ComponentSortColumn.BY_NAME.getValue(), true));
        List<String> names = items(result).stream().map(Component::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
    }

    @Test
    void sortByNameDescending_shouldReturnReverseAlphabeticalOrder() {
        var result = searchHandler.searchFilteredComponents("FT_", user1,
                pageSorted(ComponentSortColumn.BY_NAME.getValue(), false));
        List<String> names = items(result).stream().map(Component::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER.reversed()).toList(), names);
    }

    @Test
    void sortByTypeAscending_shouldReturnGroupedByTypeAlphabetically() {
        var result = searchHandler.searchFilteredComponents("FT_", null,
                pageSorted(ComponentSortColumn.BY_TYPE.getValue(), true));
        List<String> types = items(result).stream().map(c -> c.getComponentType().name()).toList();
        assertEquals(types.stream().sorted().toList(), types,
                "Component types should appear in ascending alphabetical order");
    }

    @Test
    void sortByCreatedOnAscending_shouldReturnChronologicalOrder() {
        var result = searchHandler.searchFilteredComponents("FT_", null,
                pageSorted(ComponentSortColumn.BY_CREATEDON.getValue(), true));
        List<String> dates = items(result).stream().map(Component::getCreatedOn).toList();
        assertEquals(dates.stream().sorted().toList(), dates,
                "createdOn should be in ascending chronological order");
    }

    @Test
    void sortByCreatedOnDescending_shouldReturnReverseChronologicalOrder() {
        var result = searchHandler.searchFilteredComponents("FT_", null,
                pageSorted(ComponentSortColumn.BY_CREATEDON.getValue(), false));
        List<String> dates = items(result).stream().map(Component::getCreatedOn).toList();
        assertEquals(dates.stream().sorted(Comparator.reverseOrder()).toList(), dates,
                "createdOn should be in descending chronological order");
    }

    // --- Pagination tests ----------------------------------------------------

    @Test
    void firstPage_shouldReturnRequestedPageSize() {
        assertTrue(items(searchHandler.searchFilteredComponents("FT_", user1, page(0, 5))).size() <= 5);
    }

    @Test
    void paginationAcrossPages_shouldProduceNoDuplicates() {
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Component> pg = items(searchHandler.searchFilteredComponents("FT_", user1, page(i, 3)));
            if (pg.isEmpty()) break;
            for (Component c : pg) { assertTrue(allIds.add(c.getId()), "Duplicate: " + c.getId()); }
        }
    }

    // --- Edge case tests -----------------------------------------------------

    @Test
    void specialCharacters_shouldNotCauseException() {
        assertDoesNotThrow(() -> searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("FT_Alpha & Beta")), null, allPages()));
    }

    @Test
    void nullDescription_shouldStillBeIndexed() {
        assertTrue(items(searchHandler.searchAccessibleComponents(
                Map.of("name", Set.of("FT_No Description")), null, allPages()))
                .stream().anyMatch(c -> c.getName().contains("No Description")));
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
     * Creates seed components programmatically using Thrift objects to avoid
     * data model drift that can occur with external JSON fixtures.
     */
    private static List<Component> createSeedComponents() {
        return List.of(
                cmp("ft-cmp-001", "FT_Apache Commons", "Library for utilities", ComponentType.OSS, "user1@test.sw360.org", "2024-01-10", List.of("Library", "Utility"), List.of("Java"), Visibility.EVERYONE, List.of("Apache"), List.of("Apache-2.0")),
                cmp("ft-cmp-002", "FT_Spring Framework", "Enterprise Java framework", ComponentType.OSS, "user1@test.sw360.org", "2024-02-15", List.of("Framework", "Enterprise"), List.of("Java", "Kotlin"), Visibility.EVERYONE, List.of("VMware"), List.of("Apache-2.0")),
                cmp("ft-cmp-003", "FT_React UI", "Frontend UI library", ComponentType.OSS, "user2@test.sw360.org", "2024-03-20", List.of("Library", "Frontend"), List.of("JavaScript"), Visibility.EVERYONE, List.of("Meta"), List.of("MIT")),
                cmp("ft-cmp-004", "FT_Internal Tool", "Internal dev tool", ComponentType.INNER_SOURCE, "user1@test.sw360.org", "2024-04-25", List.of("Tool"), List.of("Python"), Visibility.BUISNESSUNIT_AND_MODERATORS, List.of(), List.of()),
                cmp("ft-cmp-005", "FT_Commercial SDK", "Licensed commercial SDK", ComponentType.COTS, "user1@test.sw360.org", "2024-05-30", List.of("SDK"), List.of("C++"), Visibility.EVERYONE, List.of("Vendor Corp"), List.of("Proprietary")),
                cmp("ft-cmp-006", "FT_Bootstrap UI", "CSS framework", ComponentType.OSS, "user2@test.sw360.org", "2024-06-05", List.of("Framework", "CSS"), List.of("JavaScript"), Visibility.EVERYONE, List.of("Bootstrap Team"), List.of("MIT")),
                cmp("ft-cmp-007", "FT_Logging Library", "Centralized logging", ComponentType.OSS, "user1@test.sw360.org", "2024-07-10", List.of("Library", "Logging"), List.of("Java"), Visibility.EVERYONE, List.of("Apache"), List.of("Apache-2.0")),
                cmp("ft-cmp-008", "FT_Database Driver", "DB connectivity", ComponentType.OSS, "user1@test.sw360.org", "2024-08-15", List.of("Driver"), List.of("Java"), Visibility.EVERYONE, List.of("Oracle"), List.of("GPL-2.0")),
                cmp("ft-cmp-009", "FT_Security Module", "Security utilities", ComponentType.OSS, "user2@test.sw360.org", "2024-09-20", List.of("Library", "Security"), List.of("Java", "Go"), Visibility.EVERYONE, List.of("OWASP"), List.of("Apache-2.0", "MIT")),
                cmp("ft-cmp-010", "FT_CLI Tool", "Command line tool", ComponentType.FREESOFTWARE, "user1@test.sw360.org", "2024-10-25", List.of("Tool", "CLI"), List.of("Rust"), Visibility.EVERYONE, List.of(), List.of("GPL-3.0")),
                cmp("ft-cmp-011", "FT_Bare Component", "No categories", ComponentType.OSS, "user1@test.sw360.org", "2024-11-01", List.of(), List.of(), Visibility.EVERYONE, List.of(), List.of()),
                cmp("ft-cmp-012", "FT_Private Component", "Private", ComponentType.OSS, "user1@test.sw360.org", "2024-11-15", List.of("Library"), List.of("Java"), Visibility.PRIVATE, List.of(), List.of("MIT")),
                cmp("ft-cmp-013", "FT_Config Library", "Config mgmt", ComponentType.OSS, "user1@test.sw360.org", "2024-12-01", List.of("Library"), List.of("Java"), Visibility.EVERYONE, List.of("LightBend"), List.of("Apache-2.0")),
                cmp("ft-cmp-014", "FT_Metrics SDK", "Metrics collection", ComponentType.OSS, "user1@test.sw360.org", "2025-01-05", List.of("SDK"), List.of("Go"), Visibility.EVERYONE, List.of("DataDog"), List.of("Apache-2.0")),
                cmp("ft-cmp-015", "FT_Test Framework", "Testing infrastructure", ComponentType.OSS, "user1@test.sw360.org", "2025-02-10", List.of("Framework"), List.of("Java"), Visibility.EVERYONE, List.of("JUnit Team"), List.of("EPL-2.0")),
                cmp("ft-cmp-016", "FT_Alpha & Beta", "Special chars", ComponentType.OSS, "user1@test.sw360.org", "2025-03-01", List.of("Library"), List.of("Java"), Visibility.EVERYONE, List.of(), List.of("MIT")),
                cmp("ft-cmp-017", "FT_No Description", null, ComponentType.OSS, "user1@test.sw360.org", "2025-03-15", List.of("Library"), List.of("Java"), Visibility.EVERYONE, List.of(), List.of()),
                cmp("ft-cmp-018", "FT_Analytics Engine", "Analytics processing engine", ComponentType.INNER_SOURCE, "user2@test.sw360.org", "2025-04-01", List.of("Engine", "Analytics"), List.of("Python", "Java"), Visibility.EVERYONE, List.of("Analytics Corp"), List.of("BSD-3-Clause")),
                cmp("ft-cmp-019", "FT_Zulu Runtime", "Zulu JDK distribution", ComponentType.COTS, "user1@test.sw360.org", "2025-04-15", List.of("Runtime"), List.of("Java"), Visibility.EVERYONE, List.of("Azul"), List.of("GPL-2.0-with-classpath-exception")),
                cmp("ft-cmp-020", "FT_Build Plugin", "Build automation plugin", ComponentType.FREESOFTWARE, "user1@test.sw360.org", "2025-05-01", List.of("Tool", "Build"), List.of("Groovy", "Java"), Visibility.EVERYONE, List.of("Gradle Inc"), List.of("Apache-2.0"))
        );
    }

    private static Component cmp(String id, String name, String description, ComponentType type,
                                  String createdBy, String createdOn, List<String> categories,
                                  List<String> languages, Visibility visibility,
                                  List<String> vendorNames, List<String> mainLicenseIds) {
        Component c = new Component(name).setId(id).setType("component")
                .setComponentType(type)
                .setCreatedBy(createdBy).setCreatedOn(createdOn)
                .setCategories(new HashSet<>(categories))
                .setLanguages(new HashSet<>(languages))
                .setVisbility(visibility)
                .setBusinessUnit("AB CD EF");
        if (description != null) c.setDescription(description);
        if (!vendorNames.isEmpty()) c.setVendorNames(new HashSet<>(vendorNames));
        if (!mainLicenseIds.isEmpty()) c.setMainLicenseIds(new HashSet<>(mainLicenseIds));
        return c;
    }
}
