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
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceComparatorGenerator;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

public class ProjectSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("projects",
            new NouveauIndexFunction(
                "function(doc) {" +
                OBJ_ARRAY_TO_STRING_INDEX +
                "    if(!doc.type || doc.type != 'project') return;" +
                "    if(doc.businessUnit !== undefined && doc.businessUnit != null && doc.businessUnit.length >0) {" +
                "      index('text', 'businessUnit', doc.businessUnit, {'store': true});" +
                "    }" +
                "    if(doc.projectType !== undefined && doc.projectType != null && doc.projectType.length >0) {" +
                "      index('text', 'projectType', doc.projectType, {'store': true});" +
                "    }" +
                "    if(doc.projectResponsible !== undefined && doc.projectResponsible != null && doc.projectResponsible.length >0) {" +
                "      index('text', 'projectResponsible', doc.projectResponsible, {'store': true});" +
                "    }" +
                "    if(doc.name !== undefined && doc.name != null && doc.name.length >0) {" +
                "      index('text', 'name', doc.name, {'store': true});" +
                "    }" +
                "    if(doc.version !== undefined && doc.version != null && doc.version.length >0) {" +
                "      index('string', 'version', doc.version, {'store': true});" +
                "    }" +
                "    if(doc.state !== undefined && doc.state != null && doc.state.length >0) {" +
                "      index('text', 'state', doc.state, {'store': true});" +
                "    }" +
                "    if(doc.clearingState) {" +
                "      index('text', 'clearingState', doc.clearingState, {'store': true});" +
                "    }" +
                "    if(doc.tag !== undefined && doc.tag != null && doc.tag.length >0) {" +
                "      index('text', 'tag', doc.tag, {'store': true});" +
                "    }" +
                "    arrayToStringIndex(doc.additionalData, 'additionalData');" +
                "    if(doc.releaseRelationNetwork !== undefined && doc.releaseRelationNetwork != null && doc.releaseRelationNetwork.length > 0) {" +
                "      index('text', 'releaseRelationNetwork', doc.releaseRelationNetwork, {'store': true});" +
                "    }" +
                "}")
                    .setFieldAnalyzer(
                            Map.of("version", "keyword")
                    )
    );


    private final NouveauLuceneAwareDatabaseConnector connector;

    public ProjectSearchHandler(Cloudant client, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        connector.addDesignDoc(searchView);
    }

    public Map<PaginationData, List<Project>> search(String text, final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Project>> resultProjectList = connector
                .searchViewWithRestrictions(Project.class,
                        luceneSearchView.getIndexName(), text, subQueryRestrictions,
                        pageData, null, pageData.isAscending());

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
        return connector.searchViewWithRestrictions(Project.class, luceneSearchView.getIndexName(),
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
            projectsByReleaseIds = connector.searchViewWithRestrictions(Project.class, luceneSearchView.getIndexName(),
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
}
