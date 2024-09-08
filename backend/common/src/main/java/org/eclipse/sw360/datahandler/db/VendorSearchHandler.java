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

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

/**
 * Lucene search for the Vendor class
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class VendorSearchHandler {

    private static final LuceneSearchView luceneSearchView
            = new LuceneSearchView("lucene", "vendors",
            "function(doc) {" +
                    "  if(doc.type == 'vendor') { " +
                    "      var ret = new Document();" +
                    "      ret.add(doc.shortname);  " +
                    "      ret.add(doc.fullname);  " +
                    "      return ret;" +
                    "  }" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public VendorSearchHandler(DatabaseConnector databaseConnector, Supplier<CloudantClient> cClient) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new LuceneAwareDatabaseConnector(databaseConnector, cClient);
        connector.addView(luceneSearchView);
    }

    public List<Vendor> search(String searchText) {
        // Query the search view for the provided text
        return connector.searchView(Vendor.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

    public List<String> searchIds(String searchText) {
        // Query the search view for the provided text
        return connector.searchIds(Vendor.class, luceneSearchView, prepareWildcardQuery(searchText));
    }
}
