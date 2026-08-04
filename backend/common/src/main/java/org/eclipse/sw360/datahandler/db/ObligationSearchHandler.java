/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationSortColumn;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class ObligationSearchHandler extends BaseNouveauSearchHandler<Obligation> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------
    /**
     * Fields common to all obligations, grouped by index category.
     *
     * <ul>
     *   <li><b>standard</b>: {@code title} - full prefix-search support.</li>
     *   <li><b>simple</b>: {@code obligationLevel}, {@code text}</li>
     * </ul>
     */
    private static final List<IndexField> OBLIGATION_FIELDS = List.of(
            IndexField.standard("title"),
            IndexField.simple("text"),
            IndexField.simple("obligationLevel")
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    private static final BuiltIndexDefinition OBLIGATION_INDEX_DEFINITION = buildIndexFunction(
            "obligation",
            "",
            OBLIGATION_FIELDS,
            null,
            Collections.emptyMap(),
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ObligationSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Obligation.class, "obligations", OBLIGATION_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    /**
     * Search for Obligations in paginated manner based on searchText in title
     * or text or obligationLevel.
     * @param searchText      Text to search
     * @param obligationLevel Obligation level to filter
     * @param pageData        Pagination data
     * @return Paginated filtered obligations list.
     */
    public Map<PaginationData, List<Obligation>> searchWithPagination(
            String searchText, ObligationLevel obligationLevel, PaginationData pageData
    ) {
        Map<String, Map<String, Set<String>>> restrictions = new HashMap<>();

        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText)) {
            Map<String, Set<String>> textFilter = new HashMap<>();

            textFilter.put(
                    Obligation._Fields.TITLE.getFieldName(),
                    Collections.singleton(searchText));
            textFilter.put(
                    Obligation._Fields.TEXT.getFieldName(),
                    Collections.singleton(searchText));

            restrictions.put("OR", textFilter);
        }
        if (obligationLevel != null) {
            Map<String, Set<String>> levelFilter = new HashMap<>();
            levelFilter.put(
                    Obligation._Fields.OBLIGATION_LEVEL.getFieldName(),
                    Collections.singleton(obligationLevel.toString())
            );

            restrictions.put("AND", levelFilter);
        }

        return complexBaseSearch(connector, restrictions, AND, pageData);
    }

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        return switch (ObligationSortColumn.findByValue(sortColumnNumber)) {
            case ObligationSortColumn.BY_TITLE -> List.of("title_sort", SCORE_SORTING_FIELD, "text_sort");
            case ObligationSortColumn.BY_TEXT -> List.of("text_sort", SCORE_SORTING_FIELD, "title_sort");
            case ObligationSortColumn.BY_LEVEL -> List.of("obligationLevel_sort");
            case null, default -> List.of(SCORE_SORTING_FIELD, "title_sort", "text_sort");
        };
    }
}
