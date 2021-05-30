/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

public class ModerationSearchHandler {
    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "moderations",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'moderation') return ret;" +
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
                    "    for(var i in doc.moderators) {  "+
                    "      ret.add(doc.moderators[i], {\"field\": \"moderators\"} );" +
                    "    }" +
                    "    if(doc.documentName) {  "+
                    "      ret.add(doc.documentName, {\"field\": \"documentName\"} );" +
                    "    }" +
                    "    if(doc.componentType) {  "+
                    "      ret.add(doc.componentType, {\"field\": \"componentType\"} );" +
                    "    }" +
                    "    if(doc.requestingUser) {  "+
                    "      ret.add(doc.requestingUser, {\"field\": \"requestingUser\"} );" +
                    "    }" +
                    "    if(doc.requestingUserDepartment) {  "+
                    "      ret.add(doc.requestingUserDepartment, {\"field\": \"requestingUserDepartment\"} );" +
                    "    }" +
                    "    if(doc.moderationState) {  "+
                    "      ret.add(doc.moderationState, {\"field\": \"moderationState\"} );" +
                    "    }" +
                    "    if(doc.timestamp) {  "+
                    "      var dt = new Date(doc.timestamp); "+
                    "      var formattedDt = dt.getFullYear()+'-'+(dt.getMonth()+1)+'-'+dt.getDate(); "+
                    "      ret.add(formattedDt, {\"field\": \"timestamp\", \"type\": \"date\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");
    private final LuceneAwareDatabaseConnector connector;

    public ModerationSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cClient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<ModerationRequest> search(String text, final Map<String , Set<String > > subQueryRestrictions ) {
        return connector.searchViewWithRestrictions(ModerationRequest.class, luceneSearchView, text, subQueryRestrictions);
    }
}
