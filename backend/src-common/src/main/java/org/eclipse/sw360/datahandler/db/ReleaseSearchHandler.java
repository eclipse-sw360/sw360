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

import com.cloudant.client.api.CloudantClient;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

/**
 * Lucene search for the Release class
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "/lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("releases",
            new NouveauIndexFunction(
                "function(doc) {" +
                "  if(doc.type == 'release') {" +
                "    if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
                "      index('text', 'name', doc.name, {'store': true});" +
                "    }" +
                "    if (doc.version && typeof(doc.version) == 'string' && doc.version.length > 0) {" +
                "      index('text', 'version', doc.version, {'store': true});" +
                "    }" +
                "    index('text', 'id', doc._id, {'store': true});" +
                "  }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ReleaseSearchHandler(Supplier<CloudantClient> cClient, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, cClient, DDOC_NAME);
        Gson gson = (new DatabaseInstanceCloudant(cClient)).getClient().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    public List<Release> search(String searchText) {
        return connector.searchView(Release.class, luceneSearchView.getIndexName(), prepareWildcardQuery(searchText));
    }
}
