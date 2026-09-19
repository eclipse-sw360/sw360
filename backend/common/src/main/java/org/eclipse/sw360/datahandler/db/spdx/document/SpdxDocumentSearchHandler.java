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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class SpdxDocumentSearchHandler extends BaseNouveauSearchHandler<SPDXDocument> {

    private static final Map<String, String> SPDX_DOC_CUSTOM_ANALYZERS = Map.of(
            "id", "keyword"
    );

    private static final String SPDX_DOC_CUSTOM_JS = INDEX_ID_FIELD;

    private static final BuiltIndexDefinition SPDX_DOC_INDEX_DEFINITION = buildIndexFunction(
            "SPDXDocument",
            "",
            List.of(),
            SPDX_DOC_CUSTOM_JS,
            SPDX_DOC_CUSTOM_ANALYZERS,
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public SpdxDocumentSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(SPDXDocument.class, "SPDXDocument", SPDX_DOC_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    public List<SPDXDocument> search(String searchText) {
        PaginationData pageData = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
        Map<String, Set<String>> subQueryRestrictions = Map.of(
                SPDXDocument._Fields.ID.getFieldName(), Collections.singleton(searchText)
        );
        Map<PaginationData, List<SPDXDocument>> result = baseSearchWithOr(connector, subQueryRestrictions, pageData);
        return NouveauLuceneAwareDatabaseConnector.convertPaginatorToList(result);
    }

    @Override
    protected @NonNull @Unmodifiable List<String> mapSortColumn(int sortColumnNumber) {
        return List.of(SCORE_SORTING_FIELD);
    }
}
