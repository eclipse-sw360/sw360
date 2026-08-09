/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Packages.
 *
 * <p>Packages are access-controlled (per-user readable), therefore this
 * handler keeps the legacy text based public API while delegating index
 * construction and query routing to the shared
 * {@link BaseNouveauSearchHandler} DSL infrastructure.</p>
 */
public class PackageSearchHandler extends BaseNouveauSearchHandler<Package> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> PACKAGE_FIELDS = List.of(
            IndexField.standard("name"),
            IndexField.standard("version"),
            IndexField.standard("purl", 5, EDGE_NGRAM_MAX_LENGTH), // always starts with `pkg:`
            IndexField.simple("packageManager", "keyword"),
            IndexField.simple("packageType", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.simple("vcs"),
            IndexField.simple("releaseId"),
            IndexField.date("createdOn")
    );

    /**
     * Package-specific JS for array-backed fields that should support text
     * and sort lookups via arrayToStringIndex helper.
     */
    private static final String PACKAGE_CUSTOM_JS =
            "    arrayToStringIndex(doc.licenseIds, 'licenseIds');" +
            INDEX_ID_FIELD;

    /**
     * Analyzer overrides for fields created by {@code arrayToStringIndex}.
     * The helper generates {@code <field>_sort} string indexes that require
     * the {@code keyword} analyzer for correct sorting behavior.
     */
    private static final Map<String, String> PACKAGE_CUSTOM_ANALYZERS = Map.of(
            "licenseIds_sort", "keyword",
            "id", "keyword"
    );

    private static final BuiltIndexDefinition PACKAGE_INDEX_DEFINITION = buildIndexFunction(
            "package",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            PACKAGE_FIELDS,
            PACKAGE_CUSTOM_JS,
            PACKAGE_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    private static final List<Package._Fields> QUICK_FILTER_FIELDS = List.of(
            Package._Fields.ID,
            Package._Fields.NAME,
            Package._Fields.VERSION,
            Package._Fields.PURL,
            Package._Fields.PACKAGE_TYPE
    );

    public PackageSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Package.class, "packages", PACKAGE_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API
    // -------------------------------------------------------------------------

    /**
     * Paginated search with access control filtering.
     */
    public Map<PaginationData, List<Package>> searchAccessiblePackages(
            final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        if (CommonUtils.isNullOrEmptyMap(subQueryRestrictions)) {
            return connector.searchView(Package.class,
                    getIndexName(), "*:*", pageData, getSortColumns(pageData));
        }
        return baseSearch(connector, subQueryRestrictions, pageData);
    }

    /**
     * Search Packages with id, name, version, purl, packageType, createdBy or createdOn fields.
     */
    public Map<PaginationData, List<Package>> searchFilteredPackages(
            String searchText, PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (Package._Fields field : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(field.getFieldName(), Collections.singleton(searchText));
        }
        return baseSearchWithOr(connector, subQueryRestrictions, pageData);
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (PackageSortColumn.findByValue(sortColumnNumber)) {
            case PackageSortColumn.BY_NAME -> List.of("name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_PACKAGE_MANAGER -> List.of("packageManager_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
