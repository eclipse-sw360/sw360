/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseSortColumn;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class LicenseSearchHandler extends BaseNouveauSearchHandler<License> {

    private static final List<IndexField> LICENSE_FIELDS = List.of(
            IndexField.standard("fullname"),
            IndexField.standard("shortname")
    );

    private static final Map<String, String> LICENSE_CUSTOM_ANALYZERS = Map.of(
            "id", "keyword"
    );

    private static final String LICENSE_CUSTOM_JS = INDEX_ID_FIELD;

    private static final BuiltIndexDefinition LICENSE_INDEX_DEFINITION = buildIndexFunction(
            "license",
            "",
            LICENSE_FIELDS,
            LICENSE_CUSTOM_JS,
            LICENSE_CUSTOM_ANALYZERS,
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public LicenseSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(License.class, "licenses", LICENSE_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    public Map<PaginationData, List<License>> searchWithPagination(String searchText, PaginationData pageData) {
        if (isNullEmptyOrWhitespace(searchText)) {
            return connector.searchView(License.class, getIndexName(), "*:*", pageData, getSortColumns(pageData));
        }

        Map<String, Set<String>> subQueryRestrictions = Map.of(
                License._Fields.FULLNAME.getFieldName(), Collections.singleton(searchText.trim()),
                License._Fields.SHORTNAME.getFieldName(), Collections.singleton(searchText.trim())
        );
        return baseSearchWithOr(connector, subQueryRestrictions, pageData);
    }

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        return switch (LicenseSortColumn.findByValue(sortColumnNumber)) {
            case LicenseSortColumn.BY_FULLNAME  -> List.of("fullname_sort", SCORE_SORTING_FIELD, "id_sort");
            case LicenseSortColumn.BY_SHORTNAME -> List.of("shortname_sort", SCORE_SORTING_FIELD, "fullname_sort");
            case null, default                  -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
