/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

/**
 * Lucene search for the Vendor class
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class VendorSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "/lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("vendors",
            new NouveauIndexFunction(
                "function(doc) {" +
                "  if(doc.type == 'vendor') {" +
                "    if (typeof(doc.shortname) == 'string' && doc.shortname.length > 0) {" +
                "      index('text', 'shortname', doc.shortname, {'store': true});" +
                "    }" +
                "    if (typeof(doc.fullname) == 'string' && doc.fullname.length > 0) {" +
                "      index('text', 'fullname', doc.fullname, {'store': true});" +
                "    }" +
                "  }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public VendorSearchHandler(DatabaseConnector databaseConnector, Supplier<CloudantClient> cClient) throws IOException {
        // Creates the database connector and adds the lucene search view
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, databaseConnector.getDbName());
        connector = new NouveauLuceneAwareDatabaseConnector(db, cClient, DDOC_NAME);
        Gson gson = (new DatabaseInstanceCloudant(cClient)).getClient().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    public List<Vendor> search(String searchText) {
        // Query the search view for the provided text
        return connector.searchView(Vendor.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }

    public List<String> searchIds(String searchText) {
        // Query the search view for the provided text
        return connector.searchIds(Vendor.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }
}
