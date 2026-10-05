/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.db;

import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Turns raw Nouveau hits into Thrift {@link SearchResult} objects.
 *
 * <p>The full text search spans heterogeneous document types, so the hits cannot be deserialized
 * into a single Thrift type. Instead the documents shipped with the hits
 * ({@code include_docs = true}) are parsed generically with {@link SearchDocument}.</p>
 */
public final class SearchResultConverter {

    private SearchResultConverter() {
        // Utility class
    }

    /**
     * Convert all hits of a Nouveau result and keep the ones which are visible.
     *
     * @param queryResult Result of the Nouveau query, may be {@code null} when the query failed.
     * @param isVisible   Predicate deciding whether a result may be shown to the current user.
     * @return Converted and filtered results, never {@code null}.
     */
    public static @NonNull List<SearchResult> convertAndFilter(
            @Nullable NouveauResult queryResult, @NonNull Predicate<SearchResult> isVisible
    ) {
        List<SearchResult> results = new ArrayList<>();
        if (queryResult == null || queryResult.getHits() == null) {
            return results;
        }
        for (NouveauResult.Hits hit : queryResult.getHits()) {
            SearchResult result = makeSearchResult(hit);
            if (!result.getName().isEmpty() && isVisible.test(result)) {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Transforms a Nouveau hit into a Thrift {@link SearchResult} object.
     */
    public static @NonNull SearchResult makeSearchResult(NouveauResult.@NonNull Hits hit) {
        SearchResult result = new SearchResult();

        // Set row properties
        result.id = hit.getId();
        result.score = hit.getScore();

        // Get document and
        SearchDocument parser = new SearchDocument(hit.getDoc());

        // Get basic search results information
        result.type = parser.getType();
        result.name = parser.getName();

        return result;
    }
}
