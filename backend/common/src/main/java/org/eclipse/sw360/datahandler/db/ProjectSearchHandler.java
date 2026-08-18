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
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     *   <li><b>emptyAware</b>: {@code businessUnit} (ngram 2–10), {@code tag} (ngram 2–10) -
     *       documents with no value are indexed under
     *       {@link SW360Constants#PROJECT_SEARCH_EMPTY_TOKEN} so "no value" filter queries work.</li>
     *   <li><b>standard</b>: {@code name}, {@code version} - full prefix-search support.</li>
     *   <li><b>simple</b>: {@code projectType}, {@code projectResponsible} (email analyzer),
     *       {@code description}, {@code state} (keyword), {@code clearingState} (keyword).</li>
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
            IndexField.date("createdOn")
    );

    /**
     * Handler-specific JS: index {@code additionalData} as a concatenated text blob and
     * index the creator email of every attachment.
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
            INDEX_PROJECT_RELEASE_RELATION_NETWORK +
            INDEX_ID_FIELD;

    /**
     * Analyzer overrides that are not auto-generated from {@link #PROJECT_FIELDS}.
     * <ul>
     *   <li>{@code attachmentCreatedBy} -> {@code email} (custom JS field)</li>
     *   <li>{@code additionalData_sort} -> {@code keyword} (created by {@code arrayToStringIndex})</li>
     * </ul>
     */
    private static final Map<String, String> PROJECT_CUSTOM_ANALYZERS = Map.of(
            "attachmentCreatedBy", "email",
            "additionalData_sort", "keyword",
            "releaseRelationNetwork", "keyword",
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
        Map<PaginationData, List<Project>> resultProjectList = baseSearch(connector, subQueryRestrictions, pageData);
        PaginationData respPageData = resultProjectList.keySet().iterator().next();
        List<Project> projectList = resultProjectList.values().iterator().next();

        if (user != null) {
            projectList = projectList.stream().filter(ProjectPermissions.isVisible(user)).toList();
        }

        return Collections.singletonMap(respPageData, projectList);
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
        Map<PaginationData, List<Project>> resultProjectList = baseSearchWithOr(connector, subQueryRestrictions, pageData);
        PaginationData respPageData = resultProjectList.keySet().iterator().next();
        List<Project> projectList = resultProjectList.values().iterator().next();

        if (user != null) {
            projectList = projectList.stream().filter(ProjectPermissions.isVisible(user)).toList();
        }

        return Collections.singletonMap(respPageData, projectList);
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
