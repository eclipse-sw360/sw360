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

import com.google.common.base.Joiner;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationSortColumn;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

public class ObligationSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("obligations",
            new NouveauIndexFunction(
                """
                function(doc) {
                    if(!doc.type || doc.type != 'obligation') return;
                    if(doc.title !== undefined && doc.title != null && doc.title.length >0) {
                      index('text', 'title', doc.title, {'store': true});
                      index('string', 'title_sort', doc.title);
                    }
                    if(doc.text !== undefined && doc.text != null && doc.text.length >0) {
                      index('text', 'text', doc.text, {'store': true});
                      index('string', 'text_sort', doc.text);
                    }
                    if(doc.obligationLevel !== undefined && doc.obligationLevel != null && doc.obligationLevel.length >0) {
                      index('text', 'obligationLevel', doc.obligationLevel, {'store': true});
                      index('string', 'obligationLevel_sort', doc.obligationLevel);
                    }
                }
                """
                ));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ObligationSearchHandler(Cloudant cClient, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Obligation> search(String searchText) {
        return connector.searchView(Obligation.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
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
        String sortColumn = getSortColumnName(pageData);

        Map<String, Map<String, Set<String>>> restrictions = new HashMap<>();

        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText)) {
            Map<String, Set<String>> textFilter = new HashMap<>();

            String queryString = prepareWildcardQuery(searchText);
            textFilter.put(
                    Obligation._Fields.TITLE.getFieldName(),
                    Collections.singleton(queryString));
            textFilter.put(
                    Obligation._Fields.TEXT.getFieldName(),
                    Collections.singleton(queryString));

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

        List<String> queryFilters = NouveauLuceneAwareDatabaseConnector.createComplexQuery(
                Obligation.class, null, restrictions
        );

        final Joiner AND = Joiner.on(" AND ");

        String finalQuery = AND.join(queryFilters);

        Map<PaginationData, List<Obligation>> resultObligationList = connector
                .searchView(Obligation.class,
                        luceneSearchView.getIndexName(), finalQuery,
                        pageData, sortColumn, pageData.isAscending());

        PaginationData respPageData = resultObligationList.keySet().iterator().next();
        List<Obligation> obligationList = resultObligationList.values().iterator().next();

        return Collections.singletonMap(respPageData, obligationList);
    }

    /**
     * Convert sort column number back to sorting column name. This function makes sure to use the string column (with
     * `_sort` suffix) for text indexes.
     * @param pageData Pagination Data from the request.
     * @return Sort column name. Defaults to title
     */
    private static @Nonnull String getSortColumnName(@Nonnull PaginationData pageData) {
        return switch (ObligationSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ObligationSortColumn.BY_TEXT -> "text_sort";
            case ObligationSortColumn.BY_LEVEL -> "obligationLevel_sort";
            case null, default -> "title_sort";
        };
    }
}
