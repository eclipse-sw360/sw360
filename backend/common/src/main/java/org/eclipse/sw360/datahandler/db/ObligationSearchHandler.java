/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.cloudant.client.api.CloudantClient;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.ektorp.http.HttpClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class ObligationSearchHandler {

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "obligations",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'obligation') return ret;" +
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
                    "    if(doc.title !== undefined && doc.title != null && doc.title.length >0) {  "+
                    "         ret.add(doc.title, {\"field\": \"title\"} );" +
                    "    }" +
                    "    if(doc.text !== undefined && doc.text != null && doc.text.length >0) {  "+
                    "      ret.add(doc.text, {\"field\": \"text\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public ObligationSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cCLient, String dbName) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new LuceneAwareDatabaseConnector(httpClient, cCLient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Obligation> search(String searchText) {
        return connector.searchView(Obligation.class, luceneSearchView, prepareWildcardQuery(searchText));
    }
}
