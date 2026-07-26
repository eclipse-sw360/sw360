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
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.common.utils.SearchUtils.EMIT_EDGE_N_GRAM_INDEX;
import static org.eclipse.sw360.common.utils.SearchUtils.INDEX_DATE_AS_DOUBLE;
import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class ProjectSearchHandler extends BaseNouveauSearchHandler<Project> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("projects",
            new NouveauIndexFunction(
                "function(doc) {" +
                EMIT_EDGE_N_GRAM_INDEX +
                OBJ_ARRAY_TO_STRING_INDEX +
                INDEX_DATE_AS_DOUBLE +
                "    if(!doc.type || doc.type != 'project') return;" +
                "    var businessUnit = '" + SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN + "';" +
                "    if(doc.businessUnit !== undefined && doc.businessUnit != null && doc.businessUnit.length > 0) {" +
                "      businessUnit = doc.businessUnit;" +
                "    }" +
                "    index('text', 'businessUnit_exact', businessUnit);" +
                "    emitEdgeNGrams('businessUnit_ngram', businessUnit, 2, 10);" +
                "    if(doc.projectType !== undefined && doc.projectType != null && doc.projectType.length >0) {" +
                "      index('text', 'projectType', doc.projectType);" +
                "      index('string', 'projectType_sort', doc.projectType);" +
                "    }" +
                "    if(doc.projectResponsible !== undefined && doc.projectResponsible != null && doc.projectResponsible.length >0) {" +
                "      index('text', 'projectResponsible', doc.projectResponsible);" +
                "      index('string', 'projectResponsible_sort', doc.projectResponsible);" +
                "    }" +
                "    if(doc.name !== undefined && doc.name != null && doc.name.length >0) {" +
                "      index('text', 'name_exact', doc.name);" +
                "      emitEdgeNGrams('name_ngram', doc.name, 2, 25);" +
                "      index('string', 'name_sort', doc.name.toLowerCase());" +
                "    }" +
                "    if(doc.description !== undefined && doc.description != null && doc.description.length >0) {" +
                "      index('text', 'description', doc.description);" +
                "      index('string', 'description_sort', doc.description);" +
                "    }" +
                "    if(doc.version !== undefined && doc.version != null && doc.version.length >0) {" +
                "      index('text', 'version_exact', doc.version);" +
                "      emitEdgeNGrams('version_ngram', doc.version, 2, 25);" +
                "      index('string', 'version_sort', doc.version.toLowerCase());" +
                "    }" +
                "    if(doc.state !== undefined && doc.state != null && doc.state.length >0) {" +
                "      index('text', 'state', doc.state);" +
                "      index('string', 'state_sort', doc.state);" +
                "    }" +
                "    if(doc.clearingState) {" +
                "      index('text', 'clearingState', doc.clearingState);" +
                "    }" +
                "    var tag = '" + SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN + "';" +
                "    if(doc.tag !== undefined && doc.tag != null && doc.tag.length > 0) {" +
                "      tag = doc.tag;" +
                "    }" +
                "    index('text', 'tag_exact', tag);" +
                "    emitEdgeNGrams('tag_ngram', tag, 2, 25);" +
                "    index('string', 'tag_sort', tag.toLowerCase());" +
                "    arrayToStringIndex(doc.additionalData, 'additionalData');" +
                "    if (doc.createdOn) {" +
                "      indexDateAsDouble('createdOn', doc.createdOn);" +
                "    }" +
                "    if(doc.attachments && doc.attachments.length > 0) {" +
                "      for(var i in doc.attachments) {" +
                "        if(doc.attachments[i].createdBy) {" +
                "          index('text', 'attachmentCreatedBy', doc.attachments[i].createdBy);" +
                "        }" +
                "      }" +
                "    }" +
                "}")
                    .setFieldAnalyzer(
                            Map.ofEntries(
                                    Map.entry("businessUnit_ngram", "whitespace"),
                                    Map.entry("businessUnit_sort", "keyword"),
                                    Map.entry("version", "keyword"),
                                    Map.entry("projectResponsible", "email"),
                                    Map.entry("name_ngram", "whitespace"),
                                    Map.entry("name_sort", "keyword"),
                                    Map.entry("description_sort", "keyword"),
                                    Map.entry("version_ngram", "whitespace"),
                                    Map.entry("version_sort", "keyword"),
                                    Map.entry("state", "keyword"),
                                    Map.entry("clearingState", "keyword"),
                                    Map.entry("tag_ngram", "whitespace"),
                                    Map.entry("tag_sort", "keyword"),
                                    Map.entry("attachmentCreatedBy", "email")
                            )
                    )
                    .setDefaultAnalyzer("standard")
    );


    private final NouveauLuceneAwareDatabaseConnector connector;

    public ProjectSearchHandler(Cloudant client, String dbName) throws IOException {
        super(Project.class, luceneSearchView);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    public Map<PaginationData, List<Project>> search(final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Project>> resultProjectList = baseSearch(connector, subQueryRestrictions, pageData);
        PaginationData respPageData = resultProjectList.keySet().iterator().next();
        List<Project> projectList = resultProjectList.values().iterator().next();

        projectList = projectList.stream().filter(ProjectPermissions.isVisible(user)).toList();

        return Collections.singletonMap(respPageData, projectList);
    }

    public List<Project> search(String text, final Map<String, Set<String>> subQueryRestrictions, User user) {
        return connector.searchProjectViewWithRestrictionsAndFilter(luceneSearchView.getIndexName(), text,
                subQueryRestrictions, user);
    }

    public List<Project> search(String searchText) {
        return connector.searchView(Project.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }

    public List<Project> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictionsWithAnd(Project.class, luceneSearchView.getIndexName(),
                text, subQueryRestrictions);
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return searchByReleaseIds(Collections.singleton(id), user);
    }

    public Set<Project> searchByReleaseIds(Set<String> ids, User user) {
        Map<String, Set<String>> filterMap = getFilterMapForSetReleaseIds(ids);
        List<Project> projectsByReleaseIds;
        if (user != null) {
            projectsByReleaseIds = connector.searchProjectViewWithRestrictionsAndFilter(luceneSearchView.getIndexName(),
                    null, filterMap, user);
        } else {
            projectsByReleaseIds = connector.searchViewWithRestrictionsWithAnd(Project.class, luceneSearchView.getIndexName(),
                    null, filterMap);
        }
        return new HashSet<>(projectsByReleaseIds);
    }

    private static Map<String, Set<String>> getFilterMapForSetReleaseIds(Set<String> releaseIds) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        Set<String> values = new HashSet<>();
        for(String releaseId : releaseIds) {
            values.add("\"releaseId\":\"" + releaseId + "\"");
            values.add("\"releaseId\": \"" + releaseId + "\"");
        }
        values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
        filterMap.put(Project._Fields.RELEASE_RELATION_NETWORK.getFieldName(), values);
        return filterMap;
    }

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
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
