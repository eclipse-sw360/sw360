/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb.lucene;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector;
import org.eclipse.sw360.nouveau.NouveauQuery;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;


/**
 * Generic database connector for handling lucene searches
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class NouveauLuceneAwareDatabaseConnector extends LuceneAwareCouchDbConnector {

    /**
     * Fields in projects which can be searched with
     * ${@code SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN}
     */
    public static final Set<String> EMPTY_SEARCH_FIELDS = Set.of(
            Project._Fields.BUSINESS_UNIT.getFieldName(),
            Project._Fields.TAG.getFieldName()
    );

    private static final Logger log = LogManager.getLogger(NouveauLuceneAwareDatabaseConnector.class);

    private static final Joiner AND = Joiner.on(" AND ");
    private static final Joiner OR = Joiner.on(" OR ");
    private static final String RANGE_TO = " TO ";
    private static final Gson GSON = new Gson();

    private final DatabaseConnectorCloudant connector;

    private static final List<String> LUCENE_SPECIAL_CHARACTERS = Arrays.asList("[\\\\\\+\\-\\!\\~\\*\\?\\\"\\^\\:\\(\\)\\{\\}\\[\\]]", "\\&\\&", "\\|\\|", "/", "@");

    private static final List<Pair<String, String>> LUCENE_ESCAPE_LIST =
            Arrays.asList(
                    Pair.of("([+\\-!\\(\\)\\{\\}\\[\\]\\^\"\\~\\*\\?\\:\\\\/])", "\\\\$1"),
                    Pair.of("&&", "\\\\&&"),
                    Pair.of("\\|\\|", "\\\\||")
            );

    /**
     * Maximum number of results to return
     */
    private int resultLimit = 0;

    /**
     * Constructor using a Database connector
     */
    public NouveauLuceneAwareDatabaseConnector(@NotNull DatabaseConnectorCloudant dbClient,
                                               String ddoc, String db, Gson gson) throws IOException {
        super(dbClient.getInstance().getClient(), ddoc, db, gson);
        setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        this.connector = dbClient;
    }

    /**
     * Update NouveauDesignDocument index for a database. First gets the design
     * from DB if exists and update the index map to not overwrite existing
     * indexes. Then puts the design to the DB. At the same time, check if any
     * of the index exists and does not match. If none match, then return.
     * @param designDocument Design Document to create/add
     * @return True on success.
     * @throws RuntimeException If something goes wrong.
     */
    public boolean addDesignDoc(@NotNull NouveauDesignDocument designDocument)
            throws RuntimeException {
        NouveauDesignDocument documentFromDb = this.getNouveauDesignDocument(designDocument.getId());
        if (documentFromDb == null) {
            return putNouveauDesignDocument(designDocument);
        }

        AtomicBoolean indexMissMatched = new AtomicBoolean(false);
        if (!designDocument.equals(documentFromDb)) {
            designDocument.setRev(documentFromDb.getRev());
            if (documentFromDb.getNouveau() != null) {
                // Add missing indexes from existing DDOC as to not overwrite them
                // Check if any index definition exists but does not match
                documentFromDb.getNouveau().asMap().forEach((key, value) -> {
                    if (! designDocument.getNouveau().has(key)) {
                        designDocument.getNouveau().add(key, value);
                    } else if (!designDocument.getNouveau().get(key).equals(value)) {
                        indexMissMatched.set(true);
                    }
                });
            }
            if (!indexMissMatched.get()) {
                // No miss-match found
                return true;
            }
            return putNouveauDesignDocument(designDocument);
        }
        return true;
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public <T> List<T> searchView(Class<T> type, String indexName, String queryString) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString, false);
        if (queryNouveauResult != null && queryNouveauResult.getHits() != null) {
            // Sort hits by relevance score descending to preserve Nouveau score order
            queryNouveauResult.getHits().sort(new NouveauResultComparator());
        }
        List<String> orderedIds = getIdsFromResult(queryNouveauResult);
        List<T> results = connector.get(type, orderedIds);

        // Reorder results to match score-ordered IDs (connector.get doesn't preserve order)
        if (!orderedIds.isEmpty()) {
            Map<String, T> docMap = new LinkedHashMap<>();
            for (T doc : results) {
                String id = connector.getDocumentFromPojo(doc).getId();
                if (id != null) {
                    docMap.put(id, doc);
                }
            }
            return orderedIds.stream()
                    .map(docMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return results;
    }

    /**
     * Search with lucene with pagination support
     */
    @Deprecated
    public <T> Map<PaginationData, List<T>> searchView(
            Class<T> type, String indexName, String queryString,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        Map<PaginationData, List<String>> idMap = searchIds(type, indexName, queryString,
                pageData, sortColumn, sortAscending);

        PaginationData respPageData = idMap.keySet().iterator().next();
        List<String> orderedIds = idMap.values().iterator().next();
        List<T> collections = connector.get(type, orderedIds);

        // When sorting by score (sortColumn is null), preserve the order returned by Nouveau
        // because connector.get() does not guarantee order preservation
        if (sortColumn == null && !orderedIds.isEmpty()) {
            Map<String, T> docMap = new LinkedHashMap<>();
            for (T doc : collections) {
                String id = connector.getDocumentFromPojo(doc).getId();
                if (id != null) {
                    docMap.put(id, doc);
                }
            }
            List<T> reordered = orderedIds.stream()
                    .map(docMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            collections = reordered;
        }

        return Collections.singletonMap(respPageData, collections);
    }

    /**
     * Search with lucene with pagination support
     */
    public <T> Map<PaginationData, List<T>> searchView(
            Class<T> type, String indexName, String queryString,
            PaginationData pageData, List<String> sortColumns
    ) {
        Map<PaginationData, List<String>> idMap = searchIds(
                indexName, queryString, pageData, sortColumns
        );

        PaginationData respPageData = idMap.keySet().iterator().next();
        List<String> orderedIds = idMap.values().iterator().next();
        List<T> collections = connector.get(type, orderedIds);

        // Reorder results from connector.get to the order returned by `searchIds`
        // since connector.get does not guarantee order preservation because of
        // HashSet used.
        Map<String, T> docMap = new LinkedHashMap<>();
        for (T doc : collections) {
            String id = null;
            if (TBase.class.isAssignableFrom(doc.getClass())) {
                TBase tbase = (TBase) doc;
                TFieldIdEnum idEnum = tbase.fieldForId(1);
                id = tbase.getFieldValue(idEnum).toString();
            }
            if (id != null) {
                docMap.put(id, doc);
            }
        }
        collections = orderedIds.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .toList();

        return Collections.singletonMap(respPageData, collections);
    }

    /**
     * Search with lucene for ids with pagination support.
     */
    public <T> Map<PaginationData, List<String>> searchIds(
            String indexName, String queryString, PaginationData pageData,
            List<String> sortColumns
    ) {
        NouveauResult queryNouveauResult = searchView(
                indexName, queryString, sortColumns, pageData
        );
        if (queryNouveauResult != null) {
            pageData.setTotalRowCount(queryNouveauResult.getTotalHits());
//            queryNouveauResult.getHits().sort(new NouveauResultComparator());
        } else {
            pageData.setTotalRowCount(0);
        }
        return Collections.singletonMap(pageData, getIdsFromResult(queryNouveauResult, pageData));
    }

    /**
     * Search with lucene using the previously declared search function only for ids
     */
    public <T> List<String> searchIds(Class<T> type, String indexName, String queryString) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString, false);
        return getIdsFromResult(queryNouveauResult);
    }

    /**
     * Search with lucene for ids with pagination support.
     */
    @Deprecated
    public <T> Map<PaginationData, List<String>> searchIds(
            Class<T> type, String indexName, String queryString,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString,
                false, pageData, sortColumn, sortAscending);
        if (queryNouveauResult != null) {
            pageData.setTotalRowCount(queryNouveauResult.getTotalHits());
            // When sorting by score (sortColumn is null), sort hits by relevance score descending
            // because Nouveau does not guarantee score-based ordering when sort is null
            if (sortColumn == null && queryNouveauResult.getHits() != null) {
                queryNouveauResult.getHits().sort(new NouveauResultComparator());
            }
        } else {
            pageData.setTotalRowCount(0);
        }
        return Collections.singletonMap(pageData, getIdsFromResult(queryNouveauResult));
    }

    /**
     * Search, sort and translate Lucene Result
     */
    public <T> List<T> searchAndSortByScore(Class<T> type, String indexName, String queryString) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString);
        List<NouveauResult.Hits> hits = queryNouveauResult.getHits();
        hits.sort(new NouveauResultComparator());
        List<T> results = new ArrayList<>();
        for (NouveauResult.Hits hit : hits) {
            if (hit != null && hit.getDoc() != null && !hit.getDoc().isEmpty()) {
                results.add(GSON.fromJson(GSON.toJsonTree(hit.getDoc()), type));
            }
        }
        return results;
    }

    /**
     * Comparator to provide ordered search results
     */
    public class NouveauResultComparator implements Comparator<NouveauResult.Hits> {
        @Override
        public int compare(NouveauResult.Hits o1, NouveauResult.Hits o2) {
            double order1 = 0.0;
            double order2 = 0.0;
            for (LinkedHashMap<String, Object> order : o1.getOrder()) {
                if (order.get("@type").equals("float")) {
                    order1 = Double.parseDouble(String.valueOf(order.get("value")));
                    break;
                }
            }
            for (LinkedHashMap<String, Object> order : o2.getOrder()) {
                if (order.get("@type").equals("float")) {
                    order2 = Double.parseDouble(String.valueOf(order.get("value")));
                    break;
                }
            }
            // Sort by score descending (higher score = more relevant = first)
            int scoreCompare = Double.compare(order2, order1);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // Secondary sort by name ascending when scores are equal
            String name1 = getDocField(o1, "name");
            String name2 = getDocField(o2, "name");
            int nameCompare = name1.compareToIgnoreCase(name2);
            if (nameCompare != 0) {
                return nameCompare;
            }
            // Tertiary sort by version ascending
            String version1 = getDocField(o1, "version");
            String version2 = getDocField(o2, "version");
            return version1.compareToIgnoreCase(version2);
        }

        private String getDocField(NouveauResult.Hits hit, String field) {
            if (hit.getDoc() != null && hit.getDoc().containsKey(field)) {
                Object val = hit.getDoc().get(field);
                return val != null ? val.toString() : "";
            }
            return "";
        }
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public NouveauResult searchView(String indexName, String queryString) {
        return searchView(indexName, queryString, true);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    private @Nullable NouveauResult searchView(String indexName, String queryString, boolean includeDocs) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        return callLuceneDirectly(indexName, queryString, includeDocs);
    }

    /**
     * Search with lucene with pagination support
     */
    @Deprecated
    private @Nullable NouveauResult searchView(String indexName, String queryString, boolean includeDocs,
                                               PaginationData pageData, String sortColumn,
                                               boolean sortAscending) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        return callLuceneDirectly(indexName, queryString, includeDocs, pageData, sortColumn, sortAscending);
    }

    /**
     * Search with lucene with pagination support
     */
    private @Nullable NouveauResult searchView(
            String indexName, String queryString, List<String> sortColumns,
            PaginationData pageData
    ) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        return callLuceneDirectly(indexName, queryString, pageData, sortColumns);
    }

    private @Nullable NouveauResult callLuceneDirectly(String indexName, String queryString, boolean includeDocs) {
        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(includeDocs);
        if (resultLimit > 0) {
            query.setLimit(resultLimit);
        }
        try {
            return queryNouveau(indexName, query);
        } catch (ServiceResponseException e) {
            log.error("Nouveau query failed: {}", e.getResponseBody(), e);
        }
        return null;
    }

    private @Nullable NouveauResult callLuceneDirectly(
            String indexName, String queryString,
            @NotNull PaginationData pageData, List<String> sortColumns
    ) {
        final int pageSize = pageData.getRowsPerPage() > 0 ? pageData.getRowsPerPage() : DatabaseSettings.LUCENE_SEARCH_LIMIT;
        final int requiredPage = pageData.getDisplayStart() / pageSize;
        final int limit = calculateFetchLimit(requiredPage + 1, pageSize);

        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(false);
        query.setSort(sortColumns);
        query.setLimit(limit);
        query.reset();

        NouveauResult result = null;
        try {
            result = queryNouveau(indexName, query);
        } catch (ServiceResponseException e) {
            log.error("Nouveau query failed: {}", e.getResponseBody(), e);
        }
        return result;
    }

    /**
     * Calculates the top-N limit to fetch from CouchDB Nouveau in a single query.
     */
    private static int calculateFetchLimit(int pageNumber, int pageSize) {
        int page = Math.max(1, pageNumber);
        int size = Math.max(1, pageSize);
        return page * size;
    }

    /**
     * Extracts the requested page sublist in Java memory from the single top-N result set.
     */
    private static List<NouveauResult.Hits> extractPageSublist(
            List<NouveauResult.Hits> allHits, int offset, int pageSize
    ) {
        if (CommonUtils.isNullOrEmptyCollection(allHits)) {
            return List.of();
        }
        int startIndex = Math.max(0, offset);
        int size = Math.max(1, pageSize);

        if (startIndex >= allHits.size()) {
            return List.of();
        }
        int endIndex = Math.min(startIndex + size, allHits.size());
        return allHits.subList(startIndex, endIndex);
    }

    @Deprecated
    private @Nullable NouveauResult callLuceneDirectly(String indexName, String queryString, boolean includeDocs,
                                                       @NotNull PaginationData pageData, String sortColumn,
                                                       boolean sortAscending) {
        final int limit = pageData.getRowsPerPage() > 0 ? pageData.getRowsPerPage() : DatabaseSettings.LUCENE_SEARCH_LIMIT;
        final int requiredPage = pageData.getDisplayStart() / limit;

        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(includeDocs);
        if (sortColumn != null && !sortColumn.isEmpty()) {
            query.setSort(sortAscending ? sortColumn : "-" + sortColumn);
        }
        query.setLimit(limit);

        int currentPage = 0;
        String bookmark = "";
        String previousBookmark = "";
        NouveauResult result = null;
        try {
            do {
                if (!bookmark.isEmpty()) {
                    query.reset();
                    query.setBookmark(bookmark);
                    previousBookmark = bookmark;
                }
                result = queryNouveau(indexName, query);
                bookmark = result.getBookmark();
                currentPage += 1;
            } while (moreResultsAvailable(result, previousBookmark) && currentPage <= requiredPage);
        } catch (ServiceResponseException e) {
            log.error("Nouveau query failed: {}", e.getResponseBody(), e);
        }
        return result;
    }

    /**
     * Check if there are more results available based on the current result and bookmark.
     * @param result The result of the previous query.
     * @param bookmark The bookmark from the previous query.
     * @return True if there are more results available, false otherwise.
     */
    private boolean moreResultsAvailable(NouveauResult result, String bookmark) {
        return result != null && result.getHits() != null && !result.getHits().isEmpty()
                && !bookmark.equals(result.getBookmark());
    }

    /////////////////////////
    // GETTERS AND SETTERS //
    /////////////////////////

    public void setResultLimit(int limit) {
        if (limit >= 0) {
            resultLimit = limit;
        }
    }

    ////////////////////
    // HELPER METHODS //
    ////////////////////
    @Deprecated
    private static @NotNull List<String> getIdsFromResult(NouveauResult result) {
        List<String> ids = new ArrayList<>();
        if (result != null) {
            for (NouveauResult.Hits hit : result.getHits()) {
                ids.add(hit.getId());
            }
        }
        return ids;
    }

    private static @NotNull List<String> getIdsFromResult(
            NouveauResult result, PaginationData pageData
    ) {
        List<String> ids = new ArrayList<>();
        if (result != null) {
            List<NouveauResult.Hits> hits = extractPageSublist(
                    result.getHits(), pageData.getDisplayStart(), pageData.getRowsPerPage()
            );
            for (NouveauResult.Hits hit : hits) {
                ids.add(hit.getId());
            }
        }
        return ids;
    }

    /**
     * Search the database for a given string and types
     */
    public <T> List<T> searchViewWithRestrictionsWithAnd(
            Class<T> type, String indexName, String text, final @NotNull Map<String, Set<String>> subQueryRestrictions
    ) {
        String query = convertToRestrictiveQueryWithAnd(type, text, subQueryRestrictions);

        return searchView(type, indexName, query);
    }

    /**
     * Search the database for a given string and types, with pagination support.
     * This function uses `AND` to join the restrictions.
     * @deprecated Use the other one instead.
     */
    @Deprecated
    public <T> Map<PaginationData, List<T>> searchViewWithRestrictionsWithAnd(
            Class<T> type, String indexName, String text,
            final @NotNull Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        String query = convertToRestrictiveQueryWithAnd(type, text, subQueryRestrictions);
        return searchView(type, indexName, query, pageData, sortColumn, sortAscending);
    }

    /**
     * Search the database for a given string and types, with pagination support.
     * This function uses `OR` to join the restrictions.
     * @deprecated Use the other one instead.
     */
    @Deprecated
    public <T> Map<PaginationData, List<T>> searchViewWithRestrictionsWithOr(
            Class<T> type, String indexName, String text,
            final @NotNull Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        String query = convertToRestrictiveQueryWithOr(type, text, subQueryRestrictions);
        return searchView(type, indexName, query, pageData, sortColumn, sortAscending);
    }

    @Deprecated
    private static <T> @NotNull String convertToRestrictiveQueryWithAnd(Class<T> type, String text, @NotNull Map<String, Set<String>> subQueryRestrictions) {
        return AND.join(convertToRestrictiveQuery(type, text, subQueryRestrictions));
    }

    @Deprecated
    private static <T> @NotNull String convertToRestrictiveQueryWithOr(Class<T> type, String text, @NotNull Map<String, Set<String>> subQueryRestrictions) {
        return OR.join(convertToRestrictiveQuery(type, text, subQueryRestrictions));
    }

    /**
     * Search the database for a given string and types, with pagination support.
     * This function uses `AND` to join the restrictions.
     */
    public <T> Map<PaginationData, List<T>> searchViewWithRestrictionsWithAnd(
            Class<T> type, String indexName,
            final @NotNull Map<String, String> subQueryRestrictions,
            PaginationData pageData, List<String> sortColumns
    ) {
        String query = convertToRestrictiveQueryWithAnd(type, subQueryRestrictions, true);
        return searchView(type, indexName, query, pageData, sortColumns);
    }

    /**
     * Search the database for a given string and types, with pagination support.
     * This function uses `OR` to join the restrictions.
     */
    public <T> Map<PaginationData, List<T>> searchViewWithRestrictionsWithOr(
            Class<T> type, String indexName,
            final @NotNull Map<String, String> subQueryRestrictions,
            PaginationData pageData, List<String> sortColumns
    ) {
        String query = convertToRestrictiveQueryWithOr(type, subQueryRestrictions, true);
        return searchView(type, indexName, query, pageData, sortColumns);
    }

    private static <T> @NotNull String convertToRestrictiveQueryWithAnd(Class<T> type, @NotNull Map<String, String> subQueryRestrictions, boolean isExactSearch) {
        return AND.join(convertToRestrictiveQuery(type, subQueryRestrictions, isExactSearch));
    }

    public static <T> @NotNull String convertToRestrictiveQueryWithOr(Class<T> type, @NotNull Map<String, String> subQueryRestrictions, boolean isExactSearch) {
        return OR.join(convertToRestrictiveQuery(type, subQueryRestrictions, isExactSearch));
    }

    private static <T> @NotNull List<String> convertToRestrictiveQuery(
            Class<T> type, @NotNull Map<String, String> subQueryRestrictions,
            boolean isExactSearch
    ) {
        List<String> subQueries = new ArrayList<>();
        for (Map.Entry<String, String> restriction : subQueryRestrictions.entrySet()) {
            final String filterValue = restriction.getValue();

            if (CommonUtils.isNotNullEmptyOrWhitespace(filterValue)) {
                final String fieldName = restriction.getKey();
                subQueries.add(createAQueryRestriction(fieldName, filterValue, isExactSearch));
            }
        }

        if (type == Package.class && subQueryRestrictions.containsKey("orphanPackageCheckBox")) {
            // get all packages with name field and then negate with releaseId field to find orphan packages
            subQueries.add("((name:*) NOT (releaseId:*))");
        }
        return subQueries;
    }

    private static @NotNull String createAQueryRestriction(
            @NotNull final String fieldName, @NotNull String filterValue,
            boolean isExactSearch
    ) {
        if (EMPTY_SEARCH_FIELDS.contains(fieldName)
                && SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN.equals(filterValue)) {
            return fieldName + ":\"" + SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN + "\"";
        }

        String sanitized = sanitizeLuceneString(filterValue);
        boolean quoted = isValidQuotedPhrase(filterValue);
        // Handle pre-formatted queries from prepareWildcardQuery
        final String baseSearch = "+(" + fieldName + (quoted ? ":" : ":\"") + sanitized + (quoted ? "" : "\"") + ")";
        return switch (fieldName) {
            // Field which have `_exact`, `_ngram` and `_sort` indexes.
            case "businessUnit", "name", "version", "tag"
                    -> {
                if (!quoted) {
                    // Non-exact match
                    yield "+(" + buildFieldQuery(fieldName + "_exact", fieldName + "_ngram", filterValue, isExactSearch) + ")";
                }
                // Exact match with quoted input
                yield "+(" + fieldName + "_sort:" + sanitized.toLowerCase(Locale.ROOT) + ")";
            }
            case "createdOn"
                    -> {
                try {
                    yield "+(" + fieldName + ":\"" + formatDateNouveauFormat(sanitized) + "\")";
                } catch (ParseException e) {
                    yield baseSearch;
                }
            }
            // Other fields which does not have `_exact` and `_ngram` indexes.
            // Encase them in `"` to make sure string is not mangled because of spaces.
            default -> baseSearch;
        };
    }

    /**
     * Constructs a tiered Lucene query string for a target field.
     * <p>
     *     If the input is a quoted string, returns a simple exact match query
     *     like: {@code +<fieldExact>:<input>}.
     * </p>
     * <p>
     *     If the input contains single token, returns
     *     {@code (<fieldExact>:<input>^100 OR <fieldNgram>:<input>)}.
     * </p>
     * <p>
     *     Otherwise, creates a 2 tiered query with exact match query boost
     *     which looks like following. This will boost the exact match to top
     *     and then score everything based on Lucene internal scoring.
     * </p>
     * <pre>
     *   (
     *     <fieldExact>:"<input>"^100
     *     OR
     *     (
     *       <fieldExact>:<token[0]> AND <fieldExact>:<token[1]> AND <fieldNgram>:<token[n]>
     *     )
     *   )
     * </pre>
     */
    private static @NonNull String buildFieldQuery(
            @NonNull String fieldExact, @NonNull String fieldNgram, String rawInput,
            boolean isExactSearch
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(rawInput)) {
            return "";
        }

        String trimmed = rawInput.trim();

        // Exact phrase search if quoted, no magic
        if (isValidQuotedPhrase(trimmed) && trimmed.length() > 2) {
            // + name_exact:"My test project"
            return "+" + fieldExact + ":" + sanitizeLuceneString(trimmed);
        }

        List<String> tokens = Arrays.stream(trimmed.split("\\s+"))
                .map(NouveauLuceneAwareDatabaseConnector::sanitizeLuceneString)
                .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                .toList();

        if (tokens.isEmpty()) {
            return "";
        }

        // Single-word query pattern
        if (tokens.size() == 1) {
            String token = tokens.getFirst().toLowerCase(Locale.ROOT);
            if (!isValidQuotedPhrase(token)) {
                token = "\"" + token + "\"";
            }
            // (name_exact:"my"^100 OR name_ngram:"my")
            return String.format("(%s:%s^100 OR %s:%s)", fieldExact, token, fieldNgram, token);
        }

        // Multi-word tiered query pattern
        String fullPhrase = String.join(" ", tokens);
        String phraseClause = fieldExact + ":\"" + fullPhrase + "\"^100";

        StringBuilder andClause = new StringBuilder();
        andClause.append("(");
        List<String> clauses = new ArrayList<>();
        for (Iterator<String> it = tokens.iterator(); it.hasNext();) {
            String token = it.next();
            String fieldName = isExactSearch ? fieldExact : fieldNgram;
            if (!it.hasNext()) {
                // Last token always uses n-gram for prefix matching
                fieldName = fieldNgram;
            }
            clauses.add(fieldName + ":\"" + token.toLowerCase(Locale.ROOT) + "\"");
        }
        AND.appendTo(andClause, clauses);
        andClause.append(")");

        /*
         * ( name_exact:"My test project"^100 OR ( name_exact:"my" AND name_exact:"test" AND name_ngram:"project" ) )
         */
        return String.format("(%s OR %s)", phraseClause, andClause);
    }

    /**
     * Sanitize the input so it can be parsed by Lucene following:
     * <ol>
     *     <li>Escape all characters from Nouveau docs.</li>
     *     <li>Normalize input for stray double quotes while preserving ones at start and end.</li>
     * </ol>
     * @see <a href="https://archive.softwareheritage.org/swh:1:cnt:7cbaec66195ec3aa965637c3f79acde0434e2ad2;origin=https://github.com/apache/couchdb;path=/src/docs/src/ddocs/nouveau.rst;lines=645">Nouveau Lucene Escaping</a>
     * @param input Input string to normalize and sanitize
     * @return Normalized and sanitized string which can be used in Nouveau query.
     */
    public static @NonNull String sanitizeLuceneString(String input) {
        if (CommonUtils.isNullEmptyOrWhitespace(input)) {
            return "";
        }
        String sanitized = input;
        for (var replacementPair : LUCENE_ESCAPE_LIST) {
            sanitized = sanitized.replaceAll(replacementPair.getLeft(), replacementPair.getRight());
        }
        return normalizeRestrictionInput(sanitized.trim());
    }

    /**
     * Check if a string starts and ends with {@code "} and only there. Meaning
     * it needs no sanitization.
     * @param input Input string to check.
     * @return True if the input is a valid phrase, false otherwise.
     */
    private static boolean isValidQuotedPhrase(@NotNull String input) {
        return input.startsWith("\"") && input.endsWith("\"") && countQuotes(input) == 2;
    }

    private static int countQuotes(@NotNull String input) {
        return (int) input.chars().filter(ch -> ch == '"').count();
    }

    /**
     * Sanitize the search input for rogue quotes {@code "}
     *
     * <ol>
     *   <li>Check if input does not contain quotes or is valid, return as is.</li>
     *   <li>Check if input starts and ends with quotes (exact match needed), trim it.</li>
     *   <li>Replace all {@code "} with {@code \"}.</li>
     *   <li>If the input was trimmed, add quotes back to the start and end.</li>
     * </ol>
     *
     * @param input Input to sanitize.
     * @return Sanitized string.
     */
    private static @NotNull String normalizeRestrictionInput(@NotNull String input) {
        if (!input.contains("\"") || isValidQuotedPhrase(input)) {
            return input;
        }

        boolean hasOuterQuotes = input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"");
        String inputToEscape = hasOuterQuotes ? input.substring(1, input.length() - 1) : input;
        String escaped = inputToEscape.replaceAll("\"", "\\\\\"");

        if (hasOuterQuotes) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Convert a string to old {@code term*} format for "I don't know search".
     * Useful for default searches (un-restricted).
     */
    public static @NonNull String convertToFreeSearch(@NonNull String input) {
        String sanitized = sanitizeLuceneString(input);
        return "(\"" + String.join("*\" OR \"", sanitized.split("\\s+")) + "*\")";
    }

    /**
     * Create query for complex scenarios based on "OR" or "AND" keys for Nouveau queries.
     * For example input like bellow:<br />
     * <pre>
     * {@code
     * {
     *   "OR": {
     *     "field1": ["val1"],
     *     "field2": ["val1"]
     *   },
     *   "AND": {
     *     "field3": ["val2", "val3"]
     *   }
     * }
     * }
     * </pre>
     * You will get the output:
     * <pre>
     * {@code
     * List<String> query = [
     *   "(( field1:"val1*" val1* ) OR ( field2:"val1*" val1* ))",
     *   "( field3:"val2*" val2* AND field3:"val3*" val3* )"
     * ]
     * }
     * </pre>
     * This can later be joined using another joiner like and to get final query:
     * {@code (( field1:"val1*" val1* ) OR ( field2:"val1*" val1* )) AND
     *  ( field3:"val2*" val2* AND field3:"val3*" val3* )}
     * @param type Class for which filtering
     * @param text Text to search
     * @param subQueryRestrictions Restrictions with joiner. See description for example.
     * @return List of queries
     * @param <T> Class for which filtering
     */
    public static <T> @NotNull List<String> createComplexQuery(
            Class<T> type, String text,
            @NotNull Map<String, Map<String, Set<String>>> subQueryRestrictions
    ) {
        List<String> subQueries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Set<String>>> restriction : subQueryRestrictions.entrySet()) {
            boolean isPlural = restriction.getValue().size() > 1;
            String query = switch (restriction.getKey()) {
                case "OR" -> convertToRestrictiveQueryWithOr(type, text, restriction.getValue());
                case "AND" -> convertToRestrictiveQueryWithAnd(type, text, restriction.getValue());
                default -> null;
            };
            if (query == null) {
                continue;
            }
            if (isPlural) {
                subQueries.add("(" + query + ")");
            } else {
                subQueries.add(query);
            }
        }
        return subQueries;
    }

    @Deprecated
    private static <T> @NotNull List<String> convertToRestrictiveQuery(Class<T> type, String text, @NotNull Map<String, Set<String>> subQueryRestrictions) {
        List<String> subQueries = new ArrayList<>();
        for (Map.Entry<String, Set<String>> restriction : subQueryRestrictions.entrySet()) {
            final Set<String> filterSet = restriction.getValue();

            if (!filterSet.isEmpty()) {
                final String fieldName = restriction.getKey();
                String subQuery = formatSubquery(filterSet, fieldName);
                subQueries.add(subQuery);
            }
        }

        if (!isNullOrEmpty(text)) {
            subQueries.add(prepareWildcardQuery(text));
        }

        if (type == Package.class && subQueryRestrictions.containsKey("orphanPackageCheckBox")) {
            // get all packages with name field and then negate with releaseId field to find orphan packages
            subQueries.add("(name:*) NOT (releaseId:*)");
        }
        return subQueries;
    }

    @Deprecated
    private static @NotNull String formatSubquery(@NotNull Set<String> filterSet, final String fieldName) {
        List<String> queryParts = new ArrayList<>();
        if (EMPTY_SEARCH_FIELDS.contains(fieldName)
                && filterSet.contains(SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN)) {
            queryParts.add("%s:\"%s\"".formatted(fieldName, SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN));
        }

        final Function<String, String> addType = input -> {
            // Handle pre-formatted queries from prepareWildcardQuery
            if (input.startsWith("\"") && input.endsWith("\"")) {
                // Exact phrase search - just prepend field name
                return fieldName + ":" + input;
            } else if (input.startsWith("(") && input.contains("\"")) {
                // Wildcard query with parentheses - prepend field name
                return fieldName + ":" + input;
            } else if (fieldName.equals("version")) {
                // Keep wildcard behavior for version prefix searches (e.g. 2.5 -> 2.5.x).
                return fieldName + ":" + prepareWildcardQuery(input);
            } else if (fieldName.equals("businessUnit") || fieldName.equals("tag") || fieldName.equals("projectResponsible")
                    || fieldName.equals("createdBy") || fieldName.equals("email")
                    || fieldName.equals("moderators") || fieldName.equals("requestingUser")) {
                return fieldName + ":\"" + input + "\"";
            } else if (fieldName.equals("createdOn") || fieldName.equals("timestamp")) {
                try {
                    return fieldName + ":" + formatDateNouveauFormat(input);
                } catch (ParseException e) {
                    return fieldName + ":" + input;
                }
            } else {
                return fieldName + ":" + input;
            }
        };

        queryParts.addAll(filterSet.stream()
                .filter(value -> !SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN.equals(value))
                .map(addType)
                .toList());
        return "( " + OR.join(queryParts) + " ) ";
    }

    public static @NotNull String prepareWildcardQuery(@NotNull String query) {
        // Note: Nouveau does NOT support leading wildcards (*term), only trailing (term*)
        //
        // IMPORTANT - avoid bare TermQuery (plain exact-term) clauses wherever possible.
        // Lucene 10.x's MemorySegmentIndexInput.prefetch() calls madvise(MADV_WILLNEED) on
        // mmap'd index files when TermQuery.createWeight() resolves terms via TermStates.build()
        // -> SegmentTermsEnum.prepareSeekExact() -> prefetchBlock().  On some Linux kernel
        // configurations (seccomp profiles, hardened kernels) madvise() returns EPERM, causing
        // a non-deterministic IOException that propagates as HTTP 500 from Nouveau.
        // WildcardQuery/PrefixQuery (term*) and PhraseQuery expand differently and do NOT go
        // through TermStates.build(), so they are safe to use here.
        if (query.startsWith("\"") && query.endsWith("\"")) {
            // Exact phrase search - strip outer quotes first, sanitize, then add quotes back
            String innerText = query.substring(1, query.length() - 1);
            String sanitized = sanitizeQueryInput(innerText);
            return "(\"" + sanitized + "\")";
        } else {
            String sanitized = sanitizeQueryInput(query);
            String[] words = sanitized.split(" ");

            if (words.length > 1) {
                // Multi-word query: prioritize exact phrase, then require all words as prefixes,
                // then fall back to any word as a prefix.
                //
                // The former (obli AND test)^15 clause (exact TermQuery per word) is intentionally
                // omitted: each bare TermQuery triggers TermStates.build() -> madvise(), which
                // fails intermittently with EPERM on some Linux kernels (see note above).
                // The phrase boost (^20) already covers the "all exact words" ranking intent.

                // Highest boost: exact phrase match (PhraseQuery - does not use madvise path)
                String exactPhrase = "\"" + sanitized.toLowerCase() + "\"^20";

                // Medium boost: all words present as prefix matches (WildcardQuery AND)
                String allWordsRequired = "(" + Arrays.stream(words)
                        .map(w -> w.toLowerCase() + "*")
                        .collect(Collectors.joining(" AND ")) + ")^5";

                // Fallback: any word as a prefix match (WildcardQuery OR)
                String wildCardWords = "(" + Arrays.stream(words)
                        .map(w -> w.toLowerCase() + "*")
                        .collect(Collectors.joining(" OR ")) + ")";

                return "(" + exactPhrase + " OR " + allWordsRequired + " OR " + wildCardWords + ")";
            } else {
                // Single word: boosted exact term + wildcard for partial matches.
                // One TermQuery per request has an acceptably low madvise failure rate; the boost
                // is preserved so exact hits rank above prefix matches (e.g. "obli" > "obligation").
                String lower = sanitized.toLowerCase();
                return "(" + lower + "^5 OR " + lower + "*)";
            }
        }
    }

    public static @NotNull String prepareFuzzyQuery(String query) {
        return sanitizeQueryInput(query) + "~";
    }

    public List<Project> searchProjectViewWithRestrictionsAndFilter(String indexName, String text,
                                                                    final Map<String, Set<String>> subQueryRestrictions,
                                                                    User user) {
        List<Project> projectList = searchViewWithRestrictionsWithAnd(Project.class, indexName, text,
                subQueryRestrictions);
        return projectList.stream().filter(ProjectPermissions.isVisible(user)).collect(Collectors.toList());
    }

    @Deprecated
    private static String sanitizeQueryInput(String input) {
        if (isNullOrEmpty(input)) {
            return nullToEmpty(input);
        } else {
            for (String removeStr : LUCENE_SPECIAL_CHARACTERS) {
                input = input.replaceAll(removeStr, " ");
            }
            return input.replaceAll("\\s+", " ").trim();
        }
    }

    private static @NotNull String formatDateNouveauFormat(@NotNull String date) throws ParseException {
        if (date.startsWith("[") && date.toUpperCase().contains(RANGE_TO)) {
            return formatDateRangesNouveauFormat(date);
        }
        return dateToNouveauDouble(date);
    }

    private static @NotNull String formatDateRangesNouveauFormat(@NotNull String date) throws ParseException {
        String[] dates = date.toUpperCase().substring(1, date.length() - 1).split(RANGE_TO);
        return "[" + dateToNouveauDouble(dates[0]) + RANGE_TO + dateToNouveauDouble(dates[1]) + "]";
    }

    /**
     * Parse dates from String in (yyyy-MM-dd) format to Nouveau format (yyyyMMdd) which is used as a double in queries.
     * @param date Date to convert
     * @return Parsed date for Nouveau
     * @throws ParseException If input date cannot be parsed
     * @see #dateToNouveauFormat(Date)
     */
    public static @NotNull String dateToNouveauDouble(String date) throws ParseException {
        SimpleDateFormat inputFormatterDate = new SimpleDateFormat("yyyy-MM-dd");
        Date parsedDate;
        try {
            parsedDate = inputFormatterDate.parse(date);
        } catch (ParseException e) {
            parsedDate = new Date(Long.parseLong(date));
        } catch (Exception e) {
            throw new ParseException("Date format not recognized", 0);
        }
        return dateToNouveauFormat(parsedDate);
    }

    /**
     * Convert a java.util.Date object to Nouveau format (yyyyMMdd) which is used as a double in queries.
     * @param date Date to convert
     * @return Parsed date for Nouveau
     * @see #dateToNouveauDouble(String)
     */
    public static @NotNull String dateToNouveauFormat(Date date) {
        SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyyMMdd");
        return outputFormatter.format(date.getTime());
    }
}
