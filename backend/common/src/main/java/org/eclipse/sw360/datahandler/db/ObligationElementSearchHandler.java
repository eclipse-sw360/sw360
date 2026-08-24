/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class ObligationElementSearchHandler extends BaseNouveauSearchHandler<ObligationElement> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    /**
     * Indexes for Obligation Element objects.
     *
     * <ul>
     *   <li><b>object</b>: Standard index with n_gram search.</li>
     *   <li><b>langElement</b>: Simple index.</li>
     *   <li><b>action</b>: Simple index.</li>
     * </ul>
     */
    private static final List<IndexField> OBLI_ELEMENTS_FIELDS = List.of(
            IndexField.standard("object"),
            IndexField.simple("langElement"),
            IndexField.simple("action")
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    private static final BuiltIndexDefinition OBLI_ELEMENTS_INDEX_DEFINITION = buildIndexFunction(
            "obligationElement",
            "",
            OBLI_ELEMENTS_FIELDS,
            null,
            Map.of(),
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ObligationElementSearchHandler(Cloudant client, String dbName) throws IOException {
        super(ObligationElement.class, "obligationelements", OBLI_ELEMENTS_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    public List<ObligationElement> search(String searchText) {
        PaginationData pageData = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
        Map<String, Set<String>> restrictions = Map.of(
                ObligationElement._Fields.OBJECT.getFieldName(), Collections.singleton(searchText),
                ObligationElement._Fields.ACTION.getFieldName(), Collections.singleton(searchText),
                ObligationElement._Fields.LANG_ELEMENT.getFieldName(), Collections.singleton(searchText)
        );
        Map<PaginationData, List<ObligationElement>> result = baseSearchWithOr(connector, restrictions, pageData);
        return NouveauLuceneAwareDatabaseConnector.convertPaginatorToList(result);
    }

    @Override
    protected @NonNull @Unmodifiable List<String> mapSortColumn(int sortColumnNumber) {
        return List.of(SCORE_SORTING_FIELD);
    }
}
