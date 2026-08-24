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
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector;
import org.eclipse.sw360.nouveau.NouveauQuery;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Strings.isNullOrEmpty;


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
    private static final String RANGE_TO = " TO ";

    private final DatabaseConnectorCloudant connector;

    private static final List<Pair<String, String>> LUCENE_ESCAPE_LIST =
            Arrays.asList(
                    Pair.of("([+\\-!\\(\\)\\{\\}\\[\\]\\^\"\\~\\*\\?\\:\\\\/])", "\\\\$1"),
                    Pair.of("&&", "\\\\&&"),
                    Pair.of("\\|\\|", "\\\\||")
            );

    /**
     * Constructor using a Database connector
     */
    public NouveauLuceneAwareDatabaseConnector(@NotNull DatabaseConnectorCloudant dbClient,
                                               String ddoc, String db, Gson gson) throws IOException {
        super(dbClient.getInstance().getClient(), ddoc, db, gson);
        this.connector = dbClient;
    }

    // -------------------------------------------------------------------------
    //  Public interfaces
    // -------------------------------------------------------------------------

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
        if (documentFromDb == null || (documentFromDb.getNouveau() == null && designDocument.getNouveau() != null)) {
            // New design document or existing doc has no nouveau section yet.
            // No merge checks required.
            return putNouveauDesignDocument(designDocument);
        }

        AtomicBoolean requiresUpdate = new AtomicBoolean(false);
        if (!designDocument.equals(documentFromDb)) {
            designDocument.setRev(documentFromDb.getRev());
            if (designDocument.getNouveau() != null) {
                // Adding a brand-new index key must persist the design doc.
                designDocument.getNouveau().asMap().forEach((key, value) -> {
                    if (!documentFromDb.getNouveau().has(key)) {
                        requiresUpdate.set(true);
                    }
                });

                // Add missing indexes from existing DDOC as to not overwrite them
                // Check if any index definition exists but does not match
                documentFromDb.getNouveau().asMap().forEach((key, value) -> {
                    if (! designDocument.getNouveau().has(key)) {
                        designDocument.getNouveau().add(key, value);
                    } else if (!designDocument.getNouveau().get(key).equals(value)) {
                        requiresUpdate.set(true);
                    }
                });
            } else {
                // Incoming design document has no nouveau section while DB has one.
                // Do not overwrite existing indexes.
                return true;
            }
            if (!requiresUpdate.get()) {
                // No changes required.
                return true;
            }
            return putNouveauDesignDocument(designDocument);
        }
        return true;
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
                Object value = tbase.getFieldValue(idEnum);
                if (value != null && value.toString() != null) {
                    id = value.toString();
                }
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
     * Search with lucene with pagination support
     */
    public @Nullable NouveauResult searchView(
            String indexName, String queryString, PaginationData pageData,
            List<String> sortColumns, boolean includeDocs
    ) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        return callLuceneDirectly(indexName, queryString, pageData, sortColumns, includeDocs);
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
    public static @NonNull String buildFieldQuery(
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
        boolean hasOuterQuotes = input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"");
        String sanitized = hasOuterQuotes ? input.substring(1, input.length() - 1) : input;
        for (var replacementPair : LUCENE_ESCAPE_LIST) {
            sanitized = sanitized.replaceAll(replacementPair.getLeft(), replacementPair.getRight());
        }
        if (hasOuterQuotes) {
            sanitized = "\"" + sanitized.trim() + "\"";
        }
        return normalizeRestrictionInput(sanitized.trim());
    }

    /**
     * Check if a string starts and ends with {@code "} and only there. Meaning
     * it needs no sanitization.
     * @param input Input string to check.
     * @return True if the input is a valid phrase, false otherwise.
     */
    public static boolean isValidQuotedPhrase(@NotNull String input) {
        return input.startsWith("\"") && input.endsWith("\"") && countQuotes(input) == 2;
    }

    /**
     * Convert a string to old {@code term*} format for "I don't know search".
     * Useful for default searches (un-restricted).
     */
    public static @NonNull String convertToFreeSearch(@NonNull String input) {
        String sanitized = sanitizeLuceneString(input);
        return "(\"" + String.join("*\" OR \"", sanitized.split("\\s+")) + "*\")";
    }

    public static @NotNull String formatDateNouveauFormat(@NotNull String date) throws ParseException {
        if (date.startsWith("[") && date.toUpperCase().contains(RANGE_TO)) {
            return formatDateRangesNouveauFormat(date);
        }
        return dateToNouveauDouble(date);
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

    public static PaginationData pageDataForAllRecords() {
        return new PaginationData()
                .setRowsPerPage(DatabaseSettings.LUCENE_SEARCH_LIMIT)
                .setDisplayStart(0);
    }

    /**
     * Helper function to convert the return value of a paginated search result
     * into a simple list.
     * @param paginator Result to convert
     * @return A modifiable list
     * @param <T> Type of list data
     */
    public static <T> List<T> convertPaginatorToList(Map<PaginationData, List<T>> paginator) {
        if (CommonUtils.isNullOrEmptyMap(paginator)) {
            return new ArrayList<>();
        }
        return paginator.values().iterator().next();
    }

    // -------------------------------------------------------------------------
    //  Private interfaces and helpers
    // -------------------------------------------------------------------------

    /**
     * Search with lucene for ids with pagination support.
     */
    private <T> @Unmodifiable @NonNull Map<PaginationData, List<String>> searchIds(
            String indexName, String queryString, PaginationData pageData,
            List<String> sortColumns
    ) {
        NouveauResult queryNouveauResult = searchView(
                indexName, queryString, sortColumns, pageData
        );
        if (queryNouveauResult != null) {
            pageData.setTotalRowCount(queryNouveauResult.getTotalHits());
        } else {
            pageData.setTotalRowCount(0);
        }
        return Collections.singletonMap(pageData, getIdsFromResult(queryNouveauResult, pageData));
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

        return callLuceneDirectly(indexName, queryString, pageData, sortColumns, false);
    }

    private @Nullable NouveauResult callLuceneDirectly(
            String indexName, String queryString,
            @NotNull PaginationData pageData, @Nullable List<String> sortColumns,
            boolean includeDocs
    ) {
        final int pageSize = pageData.getRowsPerPage() > 0 ? pageData.getRowsPerPage() : DatabaseSettings.LUCENE_SEARCH_LIMIT;
        final int requiredPage = pageData.getDisplayStart() / pageSize;
        final int limit = calculateFetchLimit(requiredPage + 1, pageSize);

        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(includeDocs);
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

    private static @NotNull String formatDateRangesNouveauFormat(@NotNull String date) throws ParseException {
        String[] dates = date.toUpperCase().substring(1, date.length() - 1).split(RANGE_TO);
        return "[" + dateToNouveauDouble(dates[0]) + RANGE_TO + dateToNouveauDouble(dates[1]) + "]";
    }
}
