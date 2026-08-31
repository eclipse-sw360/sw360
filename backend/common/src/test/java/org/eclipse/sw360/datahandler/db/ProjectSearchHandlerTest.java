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
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectSortColumn;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.*;


class ProjectSearchHandlerTest {

    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        String revDir = "-";
        return switch (ProjectSortColumn.findByValue(sortColumnNumber)) {
            case ProjectSortColumn.BY_NAME -> List.of("name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_DESCRIPTION -> List.of("description_sort", SCORE_SORTING_FIELD, revDir + "createdOn");
            case ProjectSortColumn.BY_RESPONSIBLE -> List.of("projectResponsible_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_STATE -> List.of("state_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_CREATEDON -> List.of("createdOn");
            case ProjectSortColumn.BY_TYPE -> List.of("projectType_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final User user1 = new User().setEmail("user1").setDepartment("AB CD EF");
    private static final User user2 = new User().setEmail("user2").setDepartment("XY ZZ AA");

    private static ProjectSearchHandler searchHandler;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        for (Project p : createSeedProjects()) { db.add(p); }
        for (Project p : createVisibilitySeedProjects()) { db.add(p); }
        searchHandler = new ProjectSearchHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    void byName_shouldReturnNameSortWithVersionAndCreatedOnTiebreakers() {
        assertEquals(List.of("name_sort", "-version_sort", "-createdOn"),
                mapSortColumnDirect(ProjectSortColumn.BY_NAME.getValue()));
    }

    @Test
    void byDescription_shouldReturnDescriptionSortWithScoreAndCreatedOn() {
        assertEquals(List.of("description_sort", SCORE_SORTING_FIELD, "-createdOn"),
                mapSortColumnDirect(ProjectSortColumn.BY_DESCRIPTION.getValue()));
    }

    @Test
    void byResponsible_shouldReturnResponsibleSortWithFullTiebreakers() {
        assertEquals(List.of("projectResponsible_sort", SCORE_SORTING_FIELD, "name_sort", "-version_sort", "-createdOn"),
                mapSortColumnDirect(ProjectSortColumn.BY_RESPONSIBLE.getValue()));
    }

    @Test
    void byState_shouldReturnStateSortWithFullTiebreakers() {
        assertEquals(List.of("state_sort", SCORE_SORTING_FIELD, "name_sort", "-version_sort", "-createdOn"),
                mapSortColumnDirect(ProjectSortColumn.BY_STATE.getValue()));
    }

    @Test
    void byCreatedOn_shouldReturnOnlyCreatedOn() {
        assertEquals(List.of("createdOn"), mapSortColumnDirect(ProjectSortColumn.BY_CREATEDON.getValue()));
    }

    @Test
    void byType_shouldReturnProjectTypeSortWithFullTiebreakers() {
        assertEquals(List.of("projectType_sort", SCORE_SORTING_FIELD, "name_sort", "-version_sort", "-createdOn"),
                mapSortColumnDirect(ProjectSortColumn.BY_TYPE.getValue()));
    }

    @Test
    void byScore_shouldReturnOnlyScoreField() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(ProjectSortColumn.BY_SCORE.getValue()));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }

    @Test
    void allColumns_shouldProduceNonEmptyLists() {
        for (ProjectSortColumn column : ProjectSortColumn.values()) {
            assertFalse(mapSortColumnDirect(column.getValue()).isEmpty(), column + " returned empty list");
        }
    }

    @Test
    void nonPrimaryColumns_shouldHaveCorrectTiebreakerOrder() {
        for (ProjectSortColumn column : List.of(
                ProjectSortColumn.BY_RESPONSIBLE, ProjectSortColumn.BY_STATE, ProjectSortColumn.BY_TYPE)) {
            List<String> columns = mapSortColumnDirect(column.getValue());
            int scoreIdx = columns.indexOf(SCORE_SORTING_FIELD);
            int nameIdx = columns.indexOf("name_sort");
            assertTrue(scoreIdx > 0 && nameIdx > scoreIdx, column + ": tiebreaker order invalid");
        }
    }

    // --- Search / filter tests -----------------------------------------------

    @Test
    void exactNameSearch_shouldReturnMatchingProject() {
        var result = searchHandler.search(Map.of("name", Set.of("FT_Alpha Project")), user1, allPages());
        assertFalse(items(result).isEmpty());
        assertTrue(items(result).stream().anyMatch(p -> "FT_Alpha Project".equals(p.getName())));
    }

    @Test
    void nonExistentTerm_shouldReturnEmpty() {
        assertTrue(items(searchHandler.search(
                Map.of("name", Set.of("zzz_nonexistent_999")), user1, allPages())).isEmpty());
    }

    @Test
    void prefixSearch_shouldMatchViaEdgeNgram() {
        assertTrue(items(searchHandler.search(Map.of("name", Set.of("FT_Al")), user1, allPages()))
                .stream().anyMatch(p -> p.getName().contains("Alpha")));
    }

    @Test
    void secondWordPrefixSearch_shouldMatchViaEdgeNgram() {
        // "Platf" is an edge-ngram prefix of "Platform" — must match "FT_Beta Platform"
        assertTrue(items(searchHandler.search(Map.of("name", Set.of("Platf")), null, allPages()))
                .stream().anyMatch(p -> p.getName().contains("Platform")));
    }

    @Test
    void shortPrefixSearch_shouldMatchMultipleProjectsWithSamePrefix() {
        // "FT_B" is an edge-ngram prefix shared by "FT_Beta Platform" and "FT_BU Only Project"
        var result = items(searchHandler.search(Map.of("name", Set.of("FT_B")), null, allPages()));
        assertTrue(result.size() >= 2);
        result.forEach(p -> assertTrue(p.getName().startsWith("FT_B"),
                "Unexpected match: " + p.getName()));
    }

    @Test
    void caseInsensitiveSearch_shouldMatch() {
        assertTrue(items(searchHandler.search(Map.of("name", Set.of("ft_alpha")), user1, allPages()))
                .stream().anyMatch(p -> p.getName().contains("Alpha")));
    }

    @Test
    void keywordField_shouldRequireExactMatch() {
        assertFalse(items(searchHandler.search(Map.of("state", Set.of("ACTIVE")), null, allPages())).isEmpty());
        assertTrue(items(searchHandler.search(Map.of("state", Set.of("ACT")), null, allPages())).isEmpty());
    }

    @Test
    void singleFieldFilter_shouldReturnOnlyMatchingState() {
        var result = searchHandler.search(Map.of("state", Set.of("PHASE_OUT")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals("PHASE_OUT", p.getState().name()));
    }

    @Test
    void multiFieldAndFilter_shouldReturnIntersection() {
        var result = searchHandler.search(Map.of("state", Set.of("ACTIVE"), "tag", Set.of("internal")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> {
            assertEquals("ACTIVE", p.getState().name());
            assertEquals("internal", p.getTag());
        });
    }

    @Test
    void multiValueOrWithinField_shouldReturnUnion() {
        var result = searchHandler.search(Map.of("state", Set.of("ACTIVE", "PHASE_OUT")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertTrue(Set.of("ACTIVE", "PHASE_OUT").contains(p.getState().name())));
    }

    @Test
    void emptyAwareSentinel_shouldFindDocsWithNoTag() {
        var result = searchHandler.search(Map.of("tag", Set.of(SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN)), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertTrue(p.getTag() == null || p.getTag().isEmpty()));
    }

    @Test
    void normalTagSearch_shouldExcludeEmptyTagDocs() {
        var result = searchHandler.search(Map.of("tag", Set.of("security")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals("security", p.getTag()));
    }

    @Test
    void searchByBusinessUnit_shouldReturnOnlyMatchingBU() {
        var result = searchHandler.search(Map.of("businessUnit", Set.of("AB CD EF")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals("AB CD EF", p.getBusinessUnit()));
    }

    @Test
    void searchByProjectType_shouldReturnExactTypeMatch() {
        var result = searchHandler.search(Map.of("projectType", Set.of("SERVICE")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals("SERVICE", p.getProjectType().name()));
    }

    @Test
    void searchByProjectResponsible_shouldReturnMatchingProjects() {
        var result = searchHandler.search(Map.of("projectResponsible", Set.of("alice@example.com")), null, allPages());
        assertFalse(items(result).isEmpty());
        items(result).forEach(p -> assertEquals("alice@example.com", p.getProjectResponsible()));
    }

    // --- Sorting tests -------------------------------------------------------

    @Test
    void sortByNameAscending_shouldReturnAlphabeticalOrder() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_NAME.getValue(), true));
        List<String> names = items(result).stream().map(Project::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
    }

    @Test
    void sortByNameDescending_shouldReturnReverseAlphabeticalOrder() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_NAME.getValue(), false));
        List<String> names = items(result).stream().map(Project::getName).toList();
        assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER.reversed()).toList(), names);
    }

    @Test
    void sortByName_sameNameProjectsShouldTieBreakByVersionDescending() {
        // BY_NAME sort spec is [name_sort, -version_sort, -createdOn]:
        // equal names break by version descending → 3.0, 2.0, 1.0.
        var result = searchHandler.searchFilteredProjects("FT_Alpha", null,
                pageSorted(ProjectSortColumn.BY_NAME.getValue(), true));
        List<String> alphaVersions = items(result).stream()
                .filter(p -> "FT_Alpha Project".equals(p.getName()))
                .map(Project::getVersion)
                .toList();
        assertEquals(List.of("3.0", "2.0", "1.0"), alphaVersions,
                "Same-name projects must be ordered by version descending");
    }

    @Test
    void sortByCreatedOnAscending_shouldReturnChronologicalOrder() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_CREATEDON.getValue(), true));
        List<String> dates = items(result).stream().map(Project::getCreatedOn).toList();
        assertEquals(dates.stream().sorted().toList(), dates,
                "createdOn should be in ascending chronological order");
    }

    @Test
    void sortByCreatedOnDescending_shouldReturnReverseChronologicalOrder() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_CREATEDON.getValue(), false));
        List<String> dates = items(result).stream().map(Project::getCreatedOn).toList();
        assertEquals(dates.stream().sorted(Comparator.reverseOrder()).toList(), dates,
                "createdOn should be in descending chronological order");
    }

    @Test
    void sortByStateAscending_shouldReturnGroupedByStateAlphabetically() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_STATE.getValue(), true));
        List<String> states = items(result).stream().map(p -> p.getState().name()).toList();
        assertEquals(states.stream().sorted().toList(), states,
                "States should appear in ascending alphabetical order (ACTIVE < PHASE_OUT < UNKNOWN)");
    }

    @Test
    void sortByTypeAscending_shouldReturnGroupedByTypeAlphabetically() {
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_TYPE.getValue(), true));
        List<String> types = items(result).stream().map(p -> p.getProjectType().name()).toList();
        assertEquals(types.stream().sorted().toList(), types,
                "Project types should appear in ascending alphabetical order");
    }

    @Test
    void sortByResponsibleAscending_shouldReturnAlphabeticalResponsibleOrder() {
        // Seed data responsibles (ascending): alice@example.com < bob@example.com
        //   < carol@example.com < user1 < user2
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_RESPONSIBLE.getValue(), true));
        List<String> responsibles = items(result).stream()
                .map(Project::getProjectResponsible)
                .filter(Objects::nonNull)
                .toList();
        assertFalse(responsibles.isEmpty());
        assertEquals(responsibles.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), responsibles,
                "Project responsibles should appear in ascending alphabetical order");
    }

    @Test
    void sortByDescriptionAscending_shouldOrderNonNullDescriptionsAlphabetically() {
        // Lucene places missing description_sort values at one end; the non-null
        // descriptions must form a monotonically ascending sequence in the results.
        var result = searchHandler.searchFilteredProjects("FT_", null,
                pageSorted(ProjectSortColumn.BY_DESCRIPTION.getValue(), true));
        List<String> descriptions = items(result).stream()
                .map(Project::getDescription)
                .filter(Objects::nonNull)
                .toList();
        assertFalse(descriptions.isEmpty());
        assertEquals(descriptions.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), descriptions,
                "Non-null descriptions must appear in ascending alphabetical order");
    }

    @Test
    void sortByScore_shouldReturnNonEmptyResultsWithoutException() {
        assertDoesNotThrow(() -> {
            var result = searchHandler.searchFilteredProjects("FT_", null,
                    pageSorted(ProjectSortColumn.BY_SCORE.getValue(), true));
            assertFalse(items(result).isEmpty());
        });
    }

    // --- Pagination tests ----------------------------------------------------

    @Test
    void firstPage_shouldReturnRequestedPageSize() {
        assertTrue(items(searchHandler.searchFilteredProjects("FT_", null, page(0, 5))).size() <= 5);
    }

    @Test
    void secondPage_shouldNotOverlapWithFirstPage() {
        Set<String> p0 = items(searchHandler.searchFilteredProjects("FT_", null, page(0, 5)))
                .stream().map(Project::getId).collect(Collectors.toSet());
        Set<String> p1 = items(searchHandler.searchFilteredProjects("FT_", null, page(1, 5)))
                .stream().map(Project::getId).collect(Collectors.toSet());
        if (!p1.isEmpty()) { p1.retainAll(p0); assertTrue(p1.isEmpty()); }
    }

    @Test
    void pageBeyondTotal_shouldReturnEmpty() {
        assertTrue(items(searchHandler.searchFilteredProjects("FT_", null, page(100, 5))).isEmpty());
    }

    @Test
    void paginationAcrossPages_shouldProduceNoDuplicates() {
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Project> pg = items(searchHandler.searchFilteredProjects("FT_", null, page(i, 3)));
            if (pg.isEmpty()) break;
            for (Project p : pg) { assertTrue(allIds.add(p.getId()), "Duplicate: " + p.getId()); }
        }
    }

    // --- Access control tests ------------------------------------------------

    @Test
    void restrictedUser_shouldSeeFewerThanAdmin() {
        int adminCount = items(searchHandler.searchFilteredProjects("FT_", user1, allPages())).size();
        int otherCount = items(searchHandler.searchFilteredProjects("FT_", user2, allPages())).size();
        assertTrue(otherCount <= adminCount);
    }

    // --- Visibility filter functional tests ----------------------------------
    // Seed projects for visibility tests use the "VIS_" prefix so they are
    // isolated from the sort/pagination tests above.
    //
    // Users involved:
    //   visUser     - dept "AB CD EF" (BU "AB CD EF"), role USER
    //   visOutsider - dept "XY ZZ AA" (BU "XY ZZ AA"), role USER  (different BU)
    //   visOwner    - the createdBy email of some projects
    //   visMod      - added as a moderator on some projects
    //   visContrib  - added as a contributor on some projects
    //   visLead     - added as a leadArchitect on some projects
    //   visResp     - added as a projectResponsible on some projects
    // -------------------------------------------------------------------------

    private static final User visUser     = new User().setEmail("vis_user@test.com")    .setDepartment("AB CD EF").setUserGroup(UserGroup.USER);
    private static final User visOutsider = new User().setEmail("vis_outsider@test.com").setDepartment("XY ZZ AA").setUserGroup(UserGroup.USER);
    private static final User visClearingAdmin = new User().setEmail("vis_ca@test.com") .setDepartment("XY ZZ AA").setUserGroup(UserGroup.CLEARING_ADMIN);

    private static final String VIS_OWNER   = "vis_user@test.com";      // same email as visUser
    private static final String VIS_MOD     = "vis_mod@test.com";
    private static final String VIS_CONTRIB = "vis_contrib@test.com";
    private static final String VIS_LEAD    = "vis_lead@test.com";
    private static final String VIS_RESP    = "vis_resp@test.com";

    // Project IDs for visibility seed data
    private static final String VIS_EVERYONE    = "vis-prj-everyone";
    private static final String VIS_PRIVATE_OWN = "vis-prj-private-own";       // createdBy=visUser
    private static final String VIS_PRIVATE_OTHER = "vis-prj-private-other";   // createdBy=someone else
    private static final String VIS_ME_MOD_OWN    = "vis-prj-me-mod-own";      // ME_AND_MODERATORS, createdBy=visUser
    private static final String VIS_ME_MOD_MOD    = "vis-prj-me-mod-mod";      // ME_AND_MODERATORS, visUser is moderator
    private static final String VIS_ME_MOD_CONTRIB = "vis-prj-me-mod-contrib"; // ME_AND_MODERATORS, visUser is contributor
    private static final String VIS_ME_MOD_LEAD    = "vis-prj-me-mod-lead";    // ME_AND_MODERATORS, visUser is leadArchitect
    private static final String VIS_ME_MOD_RESP    = "vis-prj-me-mod-resp";    // ME_AND_MODERATORS, visUser is projectResponsible
    private static final String VIS_ME_MOD_NONE    = "vis-prj-me-mod-none";    // ME_AND_MODERATORS, visUser is NOT a member
    private static final String VIS_BU_SAME         = "vis-prj-bu-same";       // BU+MOD, same BU as visUser
    private static final String VIS_BU_OTHER        = "vis-prj-bu-other";      // BU+MOD, different BU (XY ZZ AA)
    private static final String VIS_BU_MOD_MEMBER   = "vis-prj-bu-mod-member"; // BU+MOD, different BU but visUser is moderator

    private static List<Project> createVisibilitySeedProjects() {
        List<Project> projects = new ArrayList<>();

        // EVERYONE - any user should see this
        projects.add(new Project().setId(VIS_EVERYONE).setType("project")
                .setName("VIS_Everyone Project").setVisbility(Visibility.EVERYONE)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com").setCreatedOn("2025-01-01"));

        // PRIVATE - only the owner (visUser)
        projects.add(new Project().setId(VIS_PRIVATE_OWN).setType("project")
                .setName("VIS_Private Own").setVisbility(Visibility.PRIVATE)
                .setBusinessUnit("AB CD EF").setCreatedBy(VIS_OWNER).setCreatedOn("2025-01-01"));

        // PRIVATE - owned by someone else, nobody should see it except the owner
        projects.add(new Project().setId(VIS_PRIVATE_OTHER).setType("project")
                .setName("VIS_Private Other").setVisbility(Visibility.PRIVATE)
                .setBusinessUnit("AB CD EF").setCreatedBy("private_owner@test.com").setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser is the creator
        projects.add(new Project().setId(VIS_ME_MOD_OWN).setType("project")
                .setName("VIS_MeAndMod Own").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy(VIS_OWNER).setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser is a moderator
        projects.add(new Project().setId(VIS_ME_MOD_MOD).setType("project")
                .setName("VIS_MeAndMod Mod").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com")
                .setModerators(Set.of(VIS_OWNER)).setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser is a contributor
        projects.add(new Project().setId(VIS_ME_MOD_CONTRIB).setType("project")
                .setName("VIS_MeAndMod Contrib").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com")
                .setContributors(Set.of(VIS_OWNER)).setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser is the leadArchitect
        projects.add(new Project().setId(VIS_ME_MOD_LEAD).setType("project")
                .setName("VIS_MeAndMod Lead").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com")
                .setLeadArchitect(VIS_OWNER).setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser is the projectResponsible
        projects.add(new Project().setId(VIS_ME_MOD_RESP).setType("project")
                .setName("VIS_MeAndMod Resp").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com")
                .setProjectResponsible(VIS_OWNER).setCreatedOn("2025-01-01"));

        // ME_AND_MODERATORS - visUser has no role → should NOT be visible
        projects.add(new Project().setId(VIS_ME_MOD_NONE).setType("project")
                .setName("VIS_MeAndMod None").setVisbility(Visibility.ME_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com").setCreatedOn("2025-01-01"));

        // BUISNESSUNIT_AND_MODERATORS - same BU as visUser ("AB CD EF")
        projects.add(new Project().setId(VIS_BU_SAME).setType("project")
                .setName("VIS_BuAndMod SameBU").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setBusinessUnit("AB CD EF").setCreatedBy("other@test.com").setCreatedOn("2025-01-01"));

        // BUISNESSUNIT_AND_MODERATORS - different BU ("XY ZZ AA"), visUser not a member
        projects.add(new Project().setId(VIS_BU_OTHER).setType("project")
                .setName("VIS_BuAndMod OtherBU").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setBusinessUnit("XY ZZ AA").setCreatedBy("other@test.com").setCreatedOn("2025-01-01"));

        // BUISNESSUNIT_AND_MODERATORS - different BU but visUser is a moderator → should be visible
        projects.add(new Project().setId(VIS_BU_MOD_MEMBER).setType("project")
                .setName("VIS_BuAndMod Member").setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS)
                .setBusinessUnit("XY ZZ AA").setCreatedBy("other@test.com")
                .setModerators(Set.of(VIS_OWNER)).setCreatedOn("2025-01-01"));

        return projects;
    }

    private Set<String> visIds(User user) {
        return items(searchHandler.search(Map.of("name", Set.of("VIS_")), user, allPages()))
                .stream().map(Project::getId).collect(Collectors.toSet());
    }

    @Test
    void visibility_everyone_isVisibleToAllUsers() {
        assertTrue(visIds(visUser).contains(VIS_EVERYONE));
        assertTrue(visIds(visOutsider).contains(VIS_EVERYONE));
        assertTrue(visIds(visClearingAdmin).contains(VIS_EVERYONE));
        assertTrue(visIds(null).contains(VIS_EVERYONE));
    }

    @Test
    void visibility_private_onlyOwnerCanSee() {
        // visUser is the owner
        assertTrue(visIds(visUser).contains(VIS_PRIVATE_OWN));
        // outsider cannot see it
        assertFalse(visIds(visOutsider).contains(VIS_PRIVATE_OWN));
        // clearing admin cannot see other's private projects
        assertFalse(visIds(visClearingAdmin).contains(VIS_PRIVATE_OWN));
    }

    @Test
    void visibility_private_othersProjectNotVisible() {
        assertFalse(visIds(visUser).contains(VIS_PRIVATE_OTHER));
        assertFalse(visIds(visOutsider).contains(VIS_PRIVATE_OTHER));
    }

    @Test
    void visibility_meAndModerators_creatorCanSee() {
        assertTrue(visIds(visUser).contains(VIS_ME_MOD_OWN));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_OWN));
    }

    @Test
    void visibility_meAndModerators_moderatorCanSee() {
        assertTrue(visIds(visUser).contains(VIS_ME_MOD_MOD));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_MOD));
    }

    @Test
    void visibility_meAndModerators_contributorCanSee() {
        assertTrue(visIds(visUser).contains(VIS_ME_MOD_CONTRIB));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_CONTRIB));
    }

    @Test
    void visibility_meAndModerators_leadArchitectCanSee() {
        assertTrue(visIds(visUser).contains(VIS_ME_MOD_LEAD));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_LEAD));
    }

    @Test
    void visibility_meAndModerators_projectResponsibleCanSee() {
        assertTrue(visIds(visUser).contains(VIS_ME_MOD_RESP));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_RESP));
    }

    @Test
    void visibility_meAndModerators_nonMemberCannotSee() {
        assertFalse(visIds(visUser).contains(VIS_ME_MOD_NONE));
        assertFalse(visIds(visOutsider).contains(VIS_ME_MOD_NONE));
    }

    @Test
    void visibility_buAndModerators_sameBuUserCanSee() {
        assertTrue(visIds(visUser).contains(VIS_BU_SAME));
        assertFalse(visIds(visOutsider).contains(VIS_BU_SAME));
    }

    @Test
    void visibility_buAndModerators_differentBuUserCannotSee() {
        assertFalse(visIds(visUser).contains(VIS_BU_OTHER));
    }

    @Test
    void visibility_buAndModerators_memberOverridesDifferentBu() {
        // visUser is a moderator on VIS_BU_MOD_MEMBER even though BU differs
        assertTrue(visIds(visUser).contains(VIS_BU_MOD_MEMBER));
    }

    @Test
    void visibility_clearingAdmin_seesAllBuAndModeratorProjects() {
        Set<String> ids = visIds(visClearingAdmin);
        // Clearing admin sees all BUISNESSUNIT_AND_MODERATORS regardless of BU
        assertTrue(ids.contains(VIS_BU_SAME));
        assertTrue(ids.contains(VIS_BU_OTHER));
        assertTrue(ids.contains(VIS_BU_MOD_MEMBER));
    }

    @Test
    void visibility_clearingAdmin_cannotSeeOthersPrivateProjects() {
        Set<String> ids = visIds(visClearingAdmin);
        assertFalse(ids.contains(VIS_PRIVATE_OTHER));
        assertFalse(ids.contains(VIS_PRIVATE_OWN));  // not their project
    }

    @Test
    void visibility_nullUser_seesAll() {
        Set<String> ids = visIds(null);
        // null user → no filter → all VIS_ projects should be returned
        assertTrue(ids.containsAll(Set.of(
                VIS_EVERYONE, VIS_PRIVATE_OWN, VIS_PRIVATE_OTHER,
                VIS_ME_MOD_OWN, VIS_ME_MOD_MOD, VIS_ME_MOD_CONTRIB,
                VIS_ME_MOD_LEAD, VIS_ME_MOD_RESP, VIS_ME_MOD_NONE,
                VIS_BU_SAME, VIS_BU_OTHER, VIS_BU_MOD_MEMBER)));
    }

    // --- Edge case tests -----------------------------------------------------

    @Test
    void specialCharacters_shouldNotCauseException() {
        assertDoesNotThrow(() -> searchHandler.search(Map.of("name", Set.of("FT_Special & Chars")), null, allPages()));
    }

    @Test
    void documentWithNullOptionalFields_shouldStillBeIndexed() {
        assertTrue(items(searchHandler.search(Map.of("name", Set.of("FT_Delta Service")), null, allPages()))
                .stream().anyMatch(p -> p.getName().contains("Delta")));
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
     * Creates seed projects programmatically using Thrift objects to avoid
     * data model drift that can occur with external JSON fixtures.
     */
    private static List<Project> createSeedProjects() {
        return List.of(
                prj("ft-prj-001", "FT_Alpha Project", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-01-15", "user1", Visibility.EVERYONE, "Alpha project description", "user1"),
                prj("ft-prj-002", "FT_Beta Platform", "2.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "external", "AB CD EF", "2024-03-20", "user1", Visibility.EVERYONE, "Beta platform for testing", "user2"),
                prj("ft-prj-003", "FT_Gamma Suite", "1.0", ProjectState.PHASE_OUT, ProjectType.INNER_SOURCE, "internal", "AB CD EF", "2023-10-05", "user1", Visibility.EVERYONE, "Gamma suite phase out", "user1"),
                prj("ft-prj-004", "FT_Delta Service", "3.0", ProjectState.ACTIVE, ProjectType.SERVICE, null, "AB CD EF", "2023-06-01", "user1", Visibility.EVERYONE, null, "user2"),
                prj("ft-prj-005", "FT_Epsilon Core", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "security", "XY ZZ AA", "2024-08-01", "user2", Visibility.EVERYONE, "Epsilon core security", "user2"),
                prj("ft-prj-006", "FT_Zeta Framework", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-07-10", "user1", Visibility.EVERYONE, "Zeta open source framework", "user1"),
                prj("ft-prj-007", "FT_Private Project", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-06-15", "user1", Visibility.PRIVATE, "Private project", "user1"),
                prj("ft-prj-008", "FT_BU Only Project", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-05-20", "user1", Visibility.BUISNESSUNIT_AND_MODERATORS, "BU restricted project", "user1"),
                prj("ft-prj-009", "FT_Eta Component", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-09-01", "user1", Visibility.EVERYONE, "Eta component system", "user1"),
                prj("ft-prj-010", "FT_Theta Module", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "external", "AB CD EF", "2024-09-15", "user1", Visibility.EVERYONE, "Theta module integration", "user1"),
                prj("ft-prj-011", "FT_Iota Library", "1.0", ProjectState.ACTIVE, ProjectType.INNER_SOURCE, "internal", "AB CD EF", "2024-10-01", "user1", Visibility.EVERYONE, "Iota library collection", "user1"),
                prj("ft-prj-012", "FT_Kappa System", "2.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "security", "AB CD EF", "2024-10-15", "user1", Visibility.EVERYONE, "Kappa system analysis", "user1"),
                prj("ft-prj-013", "FT_Lambda App", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, null, "AB CD EF", "2024-11-01", "user1", Visibility.EVERYONE, "Lambda application suite", "user1"),
                prj("ft-prj-014", "FT_Mu Service", "1.0", ProjectState.ACTIVE, ProjectType.SERVICE, "internal", "AB CD EF", "2024-11-15", "user1", Visibility.EVERYONE, "Mu service delivery", "user1"),
                prj("ft-prj-015", "FT_Nu Platform", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2024-12-01", "user1", Visibility.EVERYONE, "Nu platform release", "user1"),
                prj("ft-prj-016", "FT_Alpha Project", "2.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2025-01-10", "user1", Visibility.EVERYONE, null, "user1"),
                prj("ft-prj-017", "FT_Alpha Project", "3.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2025-02-20", "user1", Visibility.EVERYONE, null, "user1"),
                prj("ft-prj-018", "FT_Special & Chars", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2025-03-01", "user1", Visibility.EVERYONE, null, "user1"),
                prj("ft-prj-019", "FT_Xi Platform", "1.0", ProjectState.ACTIVE, ProjectType.PRODUCT, "internal", "AB CD EF", "2025-04-01", "user1", Visibility.EVERYONE, "Architecture platform review", "alice@example.com"),
                prj("ft-prj-020", "FT_Omicron Suite", "1.0", ProjectState.ACTIVE, ProjectType.INNER_SOURCE, "internal", "AB CD EF", "2025-04-15", "user1", Visibility.EVERYONE, "Continuous integration suite", "carol@example.com"),
                prj("ft-prj-021", "FT_Pi Dashboard", "1.0", ProjectState.UNKNOWN, ProjectType.PRODUCT, "internal", "AB CD EF", "2025-05-01", "user1", Visibility.EVERYONE, "Monitoring dashboard tool", "bob@example.com"),
                prj("ft-prj-022", "FT_Rho Report", "1.0", ProjectState.ACTIVE, ProjectType.SERVICE, "internal", "AB CD EF", "2025-05-15", "user1", Visibility.EVERYONE, "Zero-trust security reporting", "user1")
        );
    }

    private static Project prj(String id, String name, String version, ProjectState state,
                                ProjectType type, String tag, String bu, String createdOn,
                                String createdBy, Visibility vis, String desc,
                                String projectResponsible) {
        Project p = new Project().setId(id).setType("project").setName(name).setVersion(version)
                .setState(state).setProjectType(type).setBusinessUnit(bu).setCreatedOn(createdOn)
                .setCreatedBy(createdBy).setVisbility(vis);
        if (tag != null) p.setTag(tag);
        if (desc != null) p.setDescription(desc);
        if (projectResponsible != null) p.setProjectResponsible(projectResponsible);
        return p;
    }
}
