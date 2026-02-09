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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;


/**
 * Generic database connector for handling lucene searches
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class NouveauLuceneAwareDatabaseConnector extends LuceneAwareCouchDbConnector {

    private static final Logger log = LogManager.getLogger(NouveauLuceneAwareDatabaseConnector.class);

    private static final Joiner AND = Joiner.on(" AND ");
    private static final Joiner OR = Joiner.on(" OR ");
    private static final String RANGE_TO = " TO ";

    private final DatabaseConnectorCloudant connector;

    private static final List<String> LUCENE_SPECIAL_CHARACTERS = Arrays.asList("[\\\\\\+\\-\\!\\~\\*\\?\\\"\\^\\:\\(\\)\\{\\}\\[\\]]", "\\&\\&", "\\|\\|", "/", "@");

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
     * indexes. Then puts the design to the DB.
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

        if (!designDocument.equals(documentFromDb)) {
            designDocument.setRev(documentFromDb.getRev());
            if (documentFromDb.getNouveau() != null) {
                // Add missing indexes from existing DDOC as to not overwrite them
                documentFromDb.getNouveau().asMap().forEach((key, value) -> {
                    if (! designDocument.getNouveau().has(key)) {
                        designDocument.getNouveau().add(key, value);
                    }
                });
            }
            return putNouveauDesignDocument(designDocument);
        }
        return true;
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public <T> List<T> searchView(Class<T> type, String indexName, String queryString) {
        return connector.get(type, searchIds(type, indexName, queryString));
    }

    /**
     * Search with lucene with pagination support
     */
    public <T> Map<PaginationData, List<T>> searchView(
            Class<T> type, String indexName, String queryString,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        Map<PaginationData, List<String>> idMap = searchIds(type, indexName, queryString,
                pageData, sortColumn, sortAscending);

        PaginationData respPageData = idMap.keySet().iterator().next();
        List<T> collections = connector.get(type, idMap.values().iterator().next());

        return Collections.singletonMap(respPageData, collections);
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
    public <T> Map<PaginationData, List<String>> searchIds(
            Class<T> type, String indexName, String queryString,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString,
                false, pageData, sortColumn, sortAscending);
        if (queryNouveauResult != null) {
            pageData.setTotalRowCount(queryNouveauResult.getTotalHits());
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
        Gson gson = new Gson();
        for (NouveauResult.Hits hit : hits) {
            if (hit != null && hit.getDoc() != null && !hit.getDoc().isEmpty()) {
                results.add(gson.fromJson(gson.toJson(hit.getDoc()), type));
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
            return Double.compare(order1, order2);
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
    private @Nullable NouveauResult searchView(String indexName, String queryString, boolean includeDocs,
                                               PaginationData pageData, String sortColumn,
                                               boolean sortAscending) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        return callLuceneDirectly(indexName, queryString, includeDocs, pageData, sortColumn, sortAscending);
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

    private @Nullable NouveauResult callLuceneDirectly(String indexName, String queryString, boolean includeDocs,
                                                       @NotNull PaginationData pageData, String sortColumn,
                                                       boolean sortAscending) {
        final int limit = pageData.getRowsPerPage() > 0 ? pageData.getRowsPerPage() : DatabaseSettings.LUCENE_SEARCH_LIMIT;
        final int requiredPage = pageData.getDisplayStart() / limit;

        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(includeDocs);
        if (sortColumn != null && !sortColumn.isEmpty()) {
            query.setSort(sortAscending ? sortColumn : "-" + sortColumn);
        } else {
            query.setSort(null);
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
    private static @NotNull List<String> getIdsFromResult(NouveauResult result) {
        List<String> ids = new ArrayList<>();
        if (result != null) {
            for (NouveauResult.Hits hit : result.getHits()) {
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
     */
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
     */
    public <T> Map<PaginationData, List<T>> searchViewWithRestrictionsWithOr(
            Class<T> type, String indexName, String text,
            final @NotNull Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData, String sortColumn, boolean sortAscending
    ) {
        String query = convertToRestrictiveQueryWithOr(type, text, subQueryRestrictions);
        return searchView(type, indexName, query, pageData, sortColumn, sortAscending);
    }

    private static <T> @NotNull String convertToRestrictiveQueryWithAnd(Class<T> type, String text, @NotNull Map<String, Set<String>> subQueryRestrictions) {
        return AND.join(convertToRestrictiveQuery(type, text, subQueryRestrictions));
    }

    private static <T> @NotNull String convertToRestrictiveQueryWithOr(Class<T> type, String text, @NotNull Map<String, Set<String>> subQueryRestrictions) {
        return OR.join(convertToRestrictiveQuery(type, text, subQueryRestrictions));
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

    private static @NotNull String formatSubquery(@NotNull Set<String> filterSet, final String fieldName) {
        final Function<String, String> addType = input -> {
            // Handle pre-formatted queries from prepareWildcardQuery
            if (input.startsWith("\"") && input.endsWith("\"")) {
                // Exact phrase search - just prepend field name
                return fieldName + ":" + input;
            } else if (input.startsWith("(") && input.contains("\"")) {
                // Wildcard query with parentheses - prepend field name
                return fieldName + ":" + input;
            } else if (fieldName.equals("businessUnit") || fieldName.equals("tag") || fieldName.equals("projectResponsible") || fieldName.equals("createdBy") || fieldName.equals("email")) {
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

        Stream<String> searchFilters = filterSet.stream().map(addType);
        return "( " + OR.join(searchFilters.collect(Collectors.toList())) + " ) ";
    }

    public static @NotNull String prepareWildcardQuery(@NotNull String query) {
        String leadingWildcardChar = DatabaseSettings.LUCENE_LEADING_WILDCARD ? "*" : "";
        if (query.startsWith("\"") && query.endsWith("\"")) {
            // Exact phrase search - strip outer quotes first, sanitize, then add quotes back
            String innerText = query.substring(1, query.length() - 1);
            String sanitized = sanitizeQueryInput(innerText);
            // Return just the quoted text without parentheses for exact phrase search
            return "\"" + sanitized + "\"";
        } else {
            String wildCardQuery = Arrays.stream(sanitizeQueryInput(query)
                    .split(" ")).map(q -> leadingWildcardChar + q + "*")
                    .collect(Collectors.joining(" "));
            return "(\"" + wildCardQuery + "\" " + wildCardQuery + ")";
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
