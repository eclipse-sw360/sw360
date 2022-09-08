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

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class ProjectSearchHandler {

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "projects",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'project') return ret;" +
                    "    function idx(obj) {" +
                    "        for (var key in obj) {" +
                    "            switch (typeof obj[key]) {" +
                    "                case 'object':" +
                    "                    idx(obj[key]);" +
                    "                    break;" +
                    "                case 'function':" +
                    "                    break;" +
                    "                default:" +
                    "                    ret.add(obj[key]);" +
                    "                    break;" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    idx(doc);" +
                    "    if(doc.businessUnit !== undefined && doc.businessUnit != null && doc.businessUnit.length >0) {  "+
                    "         ret.add(doc.businessUnit, {\"field\": \"businessUnit\"} );" +
                    "    }" +
                    "    if(doc.projectType !== undefined && doc.projectType != null && doc.projectType.length >0) {  "+
                    "      ret.add(doc.projectType, {\"field\": \"projectType\"} );" +
                    "    }" +
                    "    if(doc.projectResponsible !== undefined && doc.projectResponsible != null && doc.projectResponsible.length >0) {  "+
                    "      ret.add(doc.projectResponsible, {\"field\": \"projectResponsible\"} );" +
                    "    }" +
                    "    if(doc.name !== undefined && doc.name != null && doc.name.length >0) {  "+
                    "      ret.add(doc.name, {\"field\": \"name\"} );" +
                    "    }" +
                    "    if(doc.version !== undefined && doc.version != null && doc.version.length >0) {  "+
                    "      ret.add(doc.version, {\"field\": \"version\"} );" +
                    "    }" +
                    "    if(doc.state !== undefined && doc.state != null && doc.state.length >0) {  "+
                    "      ret.add(doc.state, {\"field\": \"state\"} );" +
                    "    }" +
                    "    if(doc.clearingState) {  "+
                    "      ret.add(doc.clearingState, {\"field\": \"clearingState\"} );" +
                    "    }" +
                    "    if(doc.tag !== undefined && doc.tag != null && doc.tag.length >0) {  "+
                    "      ret.add(doc.tag, {\"field\": \"tag\"} );" +
                    "    }" +
                    "    for(var [key, value] in doc.additionalData) {" +
                    "      ret.add(doc.additionalData[key], {\"field\": \"additionalData\"} );" +
                    "    }" +
                    "    if(doc.releaseRelationNetwork !== undefined && doc.releaseRelationNetwork != null && doc.releaseRelationNetwork.length >0) {  "+
                    "      ret.add(doc.releaseRelationNetwork, {\"field\": \"releaseRelationNetwork\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");


    private final LuceneAwareDatabaseConnector connector;

    public ProjectSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cCLient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cCLient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public ProjectSearchHandler(DatabaseConnector databaseConnector, Supplier<CloudantClient> cClient) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new LuceneAwareDatabaseConnector(databaseConnector, cClient);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Project> search(String text, final Map<String , Set<String > > subQueryRestrictions, User user ){
        return connector.searchProjectViewWithRestrictionsAndFilter(luceneSearchView, text, subQueryRestrictions, user);
    }

    public List<Project> search(String searchText) {
        return connector.searchView(Project.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

    public List<Project> search(String text, final Map<String , Set<String > > subQueryRestrictions){
        return connector.searchViewWithRestrictions(Project.class, luceneSearchView, text, subQueryRestrictions);
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return searchByReleaseIds(Collections.singleton(id), user);
    }

    public Set<Project> searchByReleaseIds(Set<String> ids, User user) {
        Map<String, Set<String>> filterMap = getFilterMapForSetReleaseIds(ids);
        List<Project> projectsByReleaseIds;
        if (user != null) {
            projectsByReleaseIds = connector.searchProjectViewWithRestrictionsAndFilter(luceneSearchView, null, filterMap, user);
        } else {
            projectsByReleaseIds = connector.searchViewWithRestrictions(Project.class, luceneSearchView, null, filterMap);
        }
        return new HashSet<>(projectsByReleaseIds);
    }

    public int getCountProjectByReleaseIds(Set<String> ids) {
        Map<String, Set<String>> filterMap = getFilterMapForSetReleaseIds(ids);
        List<Project> projectsByReleaseIds = connector.searchViewWithRestrictions(Project.class, luceneSearchView, null, filterMap);
        return new HashSet<>(projectsByReleaseIds).size();
    }

    private static Map<String, Set<String>> getFilterMapForSetReleaseIds(Set<String> releaseIds) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        Set<String> values = new HashSet<>();
        for(String releaseId : releaseIds) {
            values.add("\"releaseId\":\"" + releaseId + "\"");
            values.add("\"releaseId\": \"" + releaseId + "\"");
        }
        values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
        filterMap.put(Project._Fields.RELEASE_RELATION_NETWORK.getFieldName(), values);
        return filterMap;
    }

    public Set<Project> searchByReleaseId(String id) {
        return searchByReleaseId(id, null);
    }
}
