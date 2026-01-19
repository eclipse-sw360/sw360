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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorSortColumn;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
            = new NouveauIndexDesignDocument("vendors",
            new NouveauIndexFunction(
                    """
                    function(doc) {
                      if(doc.type == 'vendor') {
                        if (typeof(doc.shortname) == 'string' && doc.shortname.length > 0) {
                          index('text', 'shortname', doc.shortname, {'store': true});
                          index('string', 'shortname_sort', doc.shortname);
                        }
                        if (typeof(doc.fullname) == 'string' && doc.fullname.length > 0) {
                          index('text', 'fullname', doc.fullname, {'store': true});
                          index('string', 'fullname_sort', doc.fullname);
                        }
                      }
                    }
                    """
                    ));

    private final NouveauLuceneAwareDatabaseConnector connector;
    private final VendorRepository vendorRepository;

    public VendorSearchHandler(Cloudant client, String dbName) throws IOException {
        // Creates the database connector and adds the lucene search view
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        // Initialize repository so we have a fallback using the same database and views
        vendorRepository = new VendorRepository(db);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    /**
     * Get the query with index names for searching vendors. Current indexes available are 'shortname' and 'fullname'.
     *
     * @param searchText Search query from user.
     * @return Lucene search query with index names.
     */
    private static @Nonnull String getQueryString(String searchText) {
        final Function<String, String> addField = input -> input + ": (" +
                prepareWildcardQuery(searchText) + " )";
        List<String> typeField = List.of("fullname", "shortname");
        return "( " + Joiner.on(" OR ").join(
                FluentIterable.from(typeField).transform(addField)
        ) + " ) ";
    }

    public Map<PaginationData, List<Vendor>> search(String searchText, PaginationData pageData) {
        // Query the search view for the provided text
        String sortColumn = getSortColumnName(pageData);
        Map<PaginationData, List<Vendor>> luceneResult = connector.searchView(
                Vendor.class,
                luceneSearchView.getIndexName(),
                getQueryString(searchText),
                pageData,
                sortColumn,
                pageData.isAscending());

        if (hasResults(luceneResult)) {
            return luceneResult;
        }

        // Fallback to simple in-memory filtering when lucene is unavailable (e.g. in tests)
        return vendorRepository.searchVendorsWithPagination(searchText, pageData);
    }

    public List<String> searchIds(String searchText) {
        // Query the search view for the provided text
        return connector.searchIds(Vendor.class, luceneSearchView.getIndexName(),
                getQueryString(searchText));
    }



    private static boolean hasResults(Map<PaginationData, List<Vendor>> luceneResult) {
        return luceneResult != null
                && !luceneResult.isEmpty()
                && luceneResult.values().stream().anyMatch(list -> list != null && !list.isEmpty());
    }


    /**
     * Convert sort column number back to sorting column name. This function makes sure to use the string column (with
     * `_sort` suffix) for text indexes.
     * @param pageData Pagination Data from the request.
     * @return Sort column name. Defaults to fullname_sort
     */
    private static @Nonnull String getSortColumnName(@Nonnull PaginationData pageData) {
        return switch (VendorSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case VendorSortColumn.BY_SHORTNAME -> "shortname_sort";
            case VendorSortColumn.BY_FULLNAME -> "fullname_sort";
            case null -> "fullname_sort";
            default -> "fullname_sort";
        };
    }
}
