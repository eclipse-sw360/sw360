/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.ektorp.http.HttpClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

/**
 * Lucene search for the Release class
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler {

    private static final LuceneSearchView luceneSearchView
            = new LuceneSearchView("lucene", "releases",
            "function(doc) {" +
                    "  if(doc.type == 'release') { " +
                    "      var ret = new Document();" +
                    "      ret.add(doc.name);  " +
                    "      ret.add(doc.version);  " +
                    "      ret.add(doc._id);  " +
                    "      return ret;" +
                    "  }" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public ReleaseSearchHandler(Supplier<HttpClient> httpClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, dbName);
        connector.addView(luceneSearchView);
    }

    public List<Release> search(String searchText) {
        return connector.searchView(Release.class, luceneSearchView, prepareWildcardQuery(searchText));
    }
}
