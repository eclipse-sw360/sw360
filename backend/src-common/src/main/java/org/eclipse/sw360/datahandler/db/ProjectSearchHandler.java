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
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
                    "    return ret;" +
                    "}");


    private final LuceneAwareDatabaseConnector connector;

    public ProjectSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cCLient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cCLient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Project> search(String text, final Map<String , Set<String > > subQueryRestrictions, User user ){
        return connector.searchProjectViewWithRestrictionsAndFilter(luceneSearchView, text, subQueryRestrictions, user);
    }

    public List<Project> search(String searchText) {
        return connector.searchView(Project.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

}
