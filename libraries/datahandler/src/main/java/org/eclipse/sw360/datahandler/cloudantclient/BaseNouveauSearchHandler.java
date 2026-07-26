/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.sanitizeLuceneString;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Base class to provide helpers and other infrastructure which will help in
 * preparing and performing Nouveau search.
 */
public abstract class BaseNouveauSearchHandler<T> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private final Class<T> clazz;
    private final NouveauIndexDesignDocument luceneSearchView;

    /**
     * Setup the basic properties
     * @param clazz Which type of objects we are dealing with?
     * @param luceneSearchView The Lucene search view to use for this handler.
     */
    protected BaseNouveauSearchHandler(
            Class<T> clazz, NouveauIndexDesignDocument luceneSearchView
    ) {
        this.clazz = clazz;
        this.luceneSearchView = luceneSearchView;
    }

    /**
     *
     * @param connector
     * @param db
     * @return
     * @throws IOException
     */
    public NouveauDesignDocument setup(
            NouveauLuceneAwareDatabaseConnector connector,
            DatabaseConnectorCloudant db
    ) throws IOException {
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        connector.addDesignDoc(searchView);
        return searchView;
    }

    /**
     * Perform a base search for a given type of given fields and their values.
     *
     * @param connector            Nouveau Aware DB Connector to use
     * @param subQueryRestrictions Map of fields to search and their respective expected values.
     * @param pageData             Pagination information.
     * @return Result of search, paginated.
     */
    protected final Map<PaginationData, List<T>> baseSearch(
            NouveauLuceneAwareDatabaseConnector connector,
            final Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData
    ) {
        List<String> sortColumns = getSortColumns(pageData);
        Map<String, String> simplifiedSubQueryRestrictions = new HashMap<>();
        for (var restriction : subQueryRestrictions.entrySet()) {
            simplifiedSubQueryRestrictions.put(restriction.getKey(),
                    String.join(" ", restriction.getValue()));
        }
        Map<PaginationData, List<T>> resultProjectList = connector.
                searchViewWithRestrictionsWithAnd(
                        clazz, luceneSearchView.getIndexName(),
                        simplifiedSubQueryRestrictions, pageData, sortColumns
                );
        PaginationData respPageData = resultProjectList.keySet().iterator().next();
        List<T> projectList = resultProjectList.values().iterator().next();
        return Collections.singletonMap(respPageData, projectList);
    }

    public final List<T> search(
            NouveauLuceneAwareDatabaseConnector connector, String searchText
    ) {
        return connector.searchView(clazz, luceneSearchView.getIndexName(),
                sanitizeLuceneString(searchText));
    }

    /**
     * Get the sorting columns for given lucene query.
     *
     * @param pageData Pagination Data from the request.
     * @return Sorting columns with direction ({@code -} prefix for descending)
     * @implNote Not to prefix the score sorting field. It must be sent as-is.
     */
    protected final @NonNull @Unmodifiable List<String> getSortColumns(@NonNull PaginationData pageData) {
        List<String> columns = mapSortColumn(pageData.getSortColumnNumber());
        // Flip direction is not Ascending of sorting columns.
        return columns.stream().map(c -> {
            if (!SCORE_SORTING_FIELD.equals(c) && !pageData.isAscending()) {
                if (c.startsWith("-")) {
                    return c.substring(1);
                }
                return "-" + c;
            }
            return c;
        }).toList();
    }

    protected abstract List<String> mapSortColumn(int sortColumnNumber);
}
