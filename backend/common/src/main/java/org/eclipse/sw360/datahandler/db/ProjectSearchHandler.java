/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_ADMIN_PRIVATE_ACCESS_ENABLED;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_PROJECT_RELEASE_RELATION_NETWORK;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class ProjectSearchHandler extends BaseNouveauSearchHandler<Project> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    /**
     * Fields common to all projects, grouped by index category.
     *
     * <ul>
     *   <li><b>emptyAware</b>: {@code businessUnit} (ngram 2-10), {@code tag} (ngram 2-10) -
     *       documents with no value are indexed under
     *       {@link SW360Constants#PROJECT_SEARCH_EMPTY_TOKEN} so "no value" filter queries work.</li>
     *   <li><b>standard</b>: {@code name}, {@code version} - full prefix-search support.</li>
     *   <li><b>simple</b>: {@code projectType}, {@code projectResponsible} (email analyzer),
     *       {@code description}, {@code state} (keyword), {@code clearingState} (keyword),
     *       {@code visbility} (keyword), {@code createdBy} (email), {@code leadArchitect} (email) -
     *       used for permission/visibility filter queries.</li>
     *   <li><b>date</b>: {@code createdOn} - stored as sortable yyyyMMdd double.</li>
     * </ul>
     */
    private static final List<IndexField> PROJECT_FIELDS = List.of(
            IndexField.emptyAware("businessUnit", 2, 10),
            IndexField.emptyAware("tag", 2, 10),
            IndexField.standard("name"),
            IndexField.standard("version"),
            IndexField.simple("description"),
            IndexField.simple("projectType"),
            IndexField.simple("projectResponsible", "email"),
            IndexField.simple("state", "keyword"),
            IndexField.simple("clearingState", "keyword"),
            IndexField.simple("visbility", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.simple("leadArchitect", "email"),
            IndexField.date("createdOn")
    );

    /**
     * Handler-specific JS: index {@code additionalData} as a concatenated text blob,
     * index the creator email of every attachment, and index each element of the
     * {@code moderators} and {@code contributors} arrays individually so that
     * per-user visibility queries work correctly.
     */
    private static final String PROJECT_CUSTOM_JS =
            "    arrayToStringIndex(doc.additionalData, 'additionalData');" +
            "    if(doc.attachments && doc.attachments.length > 0) {" +
            "      for(var i in doc.attachments) {" +
            "        if(doc.attachments[i].createdBy) {" +
            "          index('text', 'attachmentCreatedBy', doc.attachments[i].createdBy);" +
            "        }" +
            "      }" +
            "    }" +
            "    if(doc.moderators && doc.moderators.length > 0) {" +
            "      for(var i in doc.moderators) {" +
            "        index('text', 'moderators', doc.moderators[i]);" +
            "      }" +
            "    }" +
            "    if(doc.contributors && doc.contributors.length > 0) {" +
            "      for(var i in doc.contributors) {" +
            "        index('text', 'contributors', doc.contributors[i]);" +
            "      }" +
            "    }" +
            INDEX_PROJECT_RELEASE_RELATION_NETWORK +
            INDEX_ID_FIELD;

    /**
     * Analyzer overrides that are not auto-generated from {@link #PROJECT_FIELDS}.
     * <ul>
     *   <li>{@code attachmentCreatedBy} -> {@code email} (custom JS field)</li>
     *   <li>{@code additionalData_sort} -> {@code keyword} (created by {@code arrayToStringIndex})</li>
     *   <li>{@code moderators} -> {@code email} (custom JS loop, array elements)</li>
     *   <li>{@code contributors} -> {@code email} (custom JS loop, array elements)</li>
     * </ul>
     */
    private static final Map<String, String> PROJECT_CUSTOM_ANALYZERS = Map.of(
            "attachmentCreatedBy", "email",
            "additionalData_sort", "keyword",
            "releaseRelationNetwork", "keyword",
            "moderators", "email",
            "contributors", "email",
            "id", "keyword"
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    private static final BuiltIndexDefinition PROJECT_INDEX_DEFINITION = buildIndexFunction(
            "project",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            PROJECT_FIELDS,
            PROJECT_CUSTOM_JS,
            PROJECT_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    private static final List<Project._Fields> QUICK_FILTER_FIELDS = List.of(
            Project._Fields.ID,
            Project._Fields.NAME,
            Project._Fields.DESCRIPTION,
            Project._Fields.TAG,
            Project._Fields.PROJECT_RESPONSIBLE
    );

    public ProjectSearchHandler(Cloudant client, String dbName) throws IOException {
        super(Project.class, "projects", PROJECT_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API
    // -------------------------------------------------------------------------

    public Map<PaginationData, List<Project>> search(
            final Map<String, Set<String>> subQueryRestrictions,
            @Nullable User user,
            PaginationData pageData
    ) {
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        return baseSearch(connector, subQueryRestrictions, visibilityQuery, pageData);
    }

    public Map<PaginationData, List<Project>> searchFilteredProjects(
            final String searchText,
            @Nullable User user,
            PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (Project._Fields field : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(field.getFieldName(), Collections.singleton(searchText));
        }
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        return baseSearchWithOr(connector, subQueryRestrictions, visibilityQuery, pageData);
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return searchByReleaseIds(Collections.singleton(id), user);
    }

    public Set<Project> searchByReleaseIds(Set<String> ids, User user) {
        PaginationData pageData = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
        Map<String, Set<String>> filterMap = Map.of(
                Project._Fields.RELEASE_RELATION_NETWORK.getFieldName(), ids
        );
        Map<PaginationData, List<Project>> result = search(filterMap, user, pageData);
        List<Project> projectsByReleaseIds = NouveauLuceneAwareDatabaseConnector.convertPaginatorToList(result);
        return new HashSet<>(projectsByReleaseIds);
    }

    // -------------------------------------------------------------------------
    //  Visibility / permission Lucene query
    // -------------------------------------------------------------------------

    /**
     * Build a Lucene query string that enforces project visibility rules for the given user,
     * mirroring the Mango selector produced by
     * {@link ProjectRepository#getAccessibleProjectSelector}.
     *
     * <p>The returned string is suitable for AND-ing with any caller-supplied search query so
     * that Nouveau applies the permission check inside the index rather than as a post-filter.
     * This fixes pagination: every page returned will contain exactly the requested number of
     * accessible projects (no silent gaps caused by post-filter removals).</p>
     *
     * <p>Check the <a href="https://eclipse.dev/sw360/docs/administrationguide/user-management-roles/#project-visibility">
     *     Project Visibility documentation for more.
     * </a></p>
     *
     * @param user The requesting user, or {@code null} (no filter applied when null).
     * @return A Lucene query string, or {@code null} if no restriction should be applied.
     */
    @Nullable
    public static String buildVisibilityLuceneQuery(@Nullable User user) {
        if (user == null) {
            return null;
        }

        boolean isAdmin = PermissionUtils.isAdmin(user);
        boolean isSecurityUser = PermissionUtils.isSecurityUser(user);
        if ((SW360Utils.readConfig(IS_ADMIN_PRIVATE_ACCESS_ENABLED, false) && isAdmin) || isSecurityUser) {
            return null;
        }

        boolean isClearingAdmin = PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
        String email = user.getEmail();
        String primaryBU = SW360Utils.getBUFromOrganisation(user.getDepartment());

        // Collect all BUs (primary + secondary)
        Set<String> allBUs = new HashSet<>();
        allBUs.add(primaryBU);
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user.getSecondaryDepartmentsAndRoles();
        if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
            secondaryDepartmentsAndRoles.keySet().stream()
                    .map(SW360Utils::getBUFromOrganisation)
                    .forEach(allBUs::add);
        }

        // Clause 1: PRIVATE projects owned by the user
        String privateClause = "(visbility:\"PRIVATE\" AND createdBy:\"" + email + "\")";

        // Clause 2: visible to everyone
        String everyoneClause = "visbility:\"EVERYONE\"";

        // Clause 3: ME_AND_MODERATORS - user must be a direct member
        String memberCheck = buildMemberCheckQuery(email);
        String meAndModeratorClause =
                "(visbility:\"ME_AND_MODERATORS\" AND (" + memberCheck + "))";

        // Clause 4: BUISNESSUNIT_AND_MODERATORS
        String buAndModeratorClause;
        if (isClearingAdmin) {
            // Clearing admins can see all BU+Moderator projects regardless of BU match
            buAndModeratorClause = "visbility:\"BUISNESSUNIT_AND_MODERATORS\"";
        } else {
            List<String> buOrMemberParts = new ArrayList<>();
            for (String bu : allBUs) {
                buOrMemberParts.add("businessUnit_exact:\"" + bu + "\"");
            }
            buOrMemberParts.add(memberCheck);
            buAndModeratorClause = "(visbility:\"BUISNESSUNIT_AND_MODERATORS\" AND ("
                    + String.join(" OR ", buOrMemberParts) + "))";
        }

        return privateClause
                + " OR " + everyoneClause
                + " OR " + meAndModeratorClause
                + " OR " + buAndModeratorClause;
    }

    /**
     * Build an OR query that checks whether {@code email} appears in any of the
     * project-member fields: {@code createdBy}, {@code projectResponsible},
     * {@code leadArchitect}, {@code moderators}, {@code contributors}.
     */
    private static @NonNull String buildMemberCheckQuery(@NonNull String email) {
        return "createdBy:\"" + email + "\""
                + " OR projectResponsible:\"" + email + "\""
                + " OR leadArchitect:\"" + email + "\""
                + " OR moderators:\"" + email + "\""
                + " OR contributors:\"" + email + "\"";
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (ProjectSortColumn.findByValue(sortColumnNumber)) {
            case ProjectSortColumn.BY_NAME -> List.of("name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_DESCRIPTION -> List.of("description_sort", SCORE_SORTING_FIELD, revDir + "createdOn");
            case ProjectSortColumn.BY_RESPONSIBLE -> List.of("projectResponsible_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_STATE -> List.of("state_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            case ProjectSortColumn.BY_CREATEDON -> List.of("createdOn");
            case ProjectSortColumn.BY_TYPE -> List.of("projectType_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "version_sort", revDir + "createdOn");
            // Default sort by scoring
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
