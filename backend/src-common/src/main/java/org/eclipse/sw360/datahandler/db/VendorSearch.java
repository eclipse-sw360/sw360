/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Lucence search for the Vendor class
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class VendorSearch {

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

    public VendorSearch(DatabaseConnector databaseConnector) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new LuceneAwareDatabaseConnector(databaseConnector);
        connector.addView(luceneSearchView);
    }

    public List<Vendor> search(String searchText) {
        // Query the search view for the provided text
        return connector.searchView(Vendor.class, luceneSearchView, searchText+"*");
    }

    public Set<String> searchIds(String searchText) {
        // Query the search view for the provided text
        return connector.searchIds(Vendor.class, luceneSearchView, searchText+"*");
    }
}

