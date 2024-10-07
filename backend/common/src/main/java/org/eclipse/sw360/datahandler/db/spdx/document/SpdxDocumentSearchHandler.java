/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db.spdx.document;

import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class SpdxDocumentSearchHandler {

    private static final LuceneSearchView luceneSearchView
            = new LuceneSearchView("lucene", "SPDXDocument",
            "function(doc) {" +
                    "  if(doc.type == 'SPDXDocument') { " +
                    "      var ret = new Document();" +
                    "      ret.add(doc._id);  " +
                    "      return ret;" +
                    "  }" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public SpdxDocumentSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cClient, dbName);
        connector.addView(luceneSearchView);
    }

    public List<SPDXDocument> search(String searchText) {
        return connector.searchView(SPDXDocument.class, luceneSearchView, prepareWildcardQuery(searchText));
    }
}
