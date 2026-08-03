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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorSortColumn;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Vendors.
 *
 * <p>Vendors are globally readable (no per-user access control), therefore this
 * handler keeps the legacy text based public API ({@link #search} and
 * {@link #searchIds}) while delegating index construction and query routing to
 * the shared {@link BaseNouveauSearchHandler} DSL infrastructure.</p>
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class VendorSearchHandler extends BaseNouveauSearchHandler<Vendor> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> VENDOR_FIELDS = List.of(
            IndexField.standard("shortname"),
            IndexField.standard("fullname")
    );

    private static final BuiltIndexDefinition VENDOR_INDEX_DEFINITION = buildIndexFunction(
            "vendor",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            VENDOR_FIELDS,
            null,
            Map.of(),
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;
    private final VendorRepository vendorRepository;

    public VendorSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Vendor.class, "vendors", VENDOR_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        // Initialize repository so we have a fallback using the same database and views
        vendorRepository = new VendorRepository(db);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API
    // -------------------------------------------------------------------------

    /**
     * Paginated search matching vendors whose {@code fullname} or {@code shortname}
     * starts with the provided text. Falls back to an in-memory repository search
     * when lucene is unavailable (e.g. in tests).
     */
    public Map<PaginationData, List<Vendor>> search(String searchText, PaginationData pageData) {
        Map<String, Set<String>> subQueryRestrictions = Map.of(
                Vendor._Fields.FULLNAME.getFieldName(), Collections.singleton(searchText),
                Vendor._Fields.SHORTNAME.getFieldName(), Collections.singleton(searchText)
        );
        Map<PaginationData, List<Vendor>> luceneResult = baseSearchWithOr(connector, subQueryRestrictions, pageData);

        if (hasResults(luceneResult)) {
            return luceneResult;
        }

        // Fallback to simple in-memory filtering when lucene is unavailable (e.g. in tests)
        return vendorRepository.searchVendorsWithPagination(searchText, pageData);
    }

    /**
     * Get list of vendor ids whose {@code fullname} or {@code shortname} starts with
     * the provided text.
     */
    public List<String> searchIds(String searchText) {
        String query = buildFullnameOrShortnameQuery(searchText);
        if (query == null) {
            return Collections.emptyList();
        }
        return connector.searchIds(Vendor.class, getIndexName(), query);
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /**
     * Build an OR'd tiered lucene query over the {@code fullname} and {@code shortname}
     * fields, mirroring the routing used by {@link #baseSearchWithOr}.
     *
     * @return the query string, or {@code null} when {@code searchText} produces no
     *         searchable tokens.
     */
    private static String buildFullnameOrShortnameQuery(String searchText) {
        String fullnameQuery = NouveauLuceneAwareDatabaseConnector.buildFieldQuery(
                Vendor._Fields.FULLNAME.getFieldName() + "_exact",
                Vendor._Fields.FULLNAME.getFieldName() + "_ngram", searchText, true);
        String shortnameQuery = NouveauLuceneAwareDatabaseConnector.buildFieldQuery(
                Vendor._Fields.SHORTNAME.getFieldName() + "_exact",
                Vendor._Fields.SHORTNAME.getFieldName() + "_ngram", searchText, true);

        if (fullnameQuery.isEmpty() && shortnameQuery.isEmpty()) {
            return null;
        }
        return "(" + fullnameQuery + ") OR (" + shortnameQuery + ")";
    }

    private static boolean hasResults(Map<PaginationData, List<Vendor>> luceneResult) {
        return luceneResult != null
                && !luceneResult.isEmpty()
                && luceneResult.values().stream().anyMatch(list -> list != null && !list.isEmpty());
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        return switch (VendorSortColumn.findByValue(sortColumnNumber)) {
            case VendorSortColumn.BY_FULLNAME -> List.of("fullname_sort", "shortname_sort");
            case VendorSortColumn.BY_SHORTNAME -> List.of("shortname_sort", "fullname_sort");
            case VendorSortColumn.BY_SCORE -> List.of(SCORE_SORTING_FIELD);
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
