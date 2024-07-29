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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
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

    private static final List<String> LUCENE_SPECIAL_CHARACTERS = Arrays.asList("[\\\\\\+\\-\\!\\~\\*\\?\\\"\\^\\:\\(\\)\\{\\}\\[\\]]", "\\&\\&", "\\|\\|", "/");

    /**
     * Maximum number of results to return
     */
    private int resultLimit = 0;

    /**
     * Constructor using a Database connector
     */
    public NouveauLuceneAwareDatabaseConnector(@NotNull DatabaseConnectorCloudant db,
                                               String ddoc) throws IOException {
        super(db.getInstance().getClient(), ddoc);
        setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        this.connector = db;
    }

    public boolean addDesignDoc(@NotNull NouveauDesignDocument designDocument) {
        NouveauDesignDocument documentFromDb = this.getNouveauDesignDocument(designDocument.getId());
        if (documentFromDb == null) {
            return putNouveauDesignDocument(designDocument, this.connector);
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
            return putNouveauDesignDocument(designDocument, this.connector);
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
     * Search with lucene using the previously declared search function only for ids
     */
    public <T> List<String> searchIds(Class<T> type, String indexName, String queryString) {
        NouveauResult queryNouveauResult = searchView(indexName, queryString, false);
        return getIdsFromResult(queryNouveauResult);
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

    private NouveauResult callLuceneDirectly(String indexName, String queryString, boolean includeDocs) {
        NouveauQuery query = new NouveauQuery(queryString);
        query.setIncludeDocs(includeDocs);
        if (resultLimit > 0) {
            query.setLimit(resultLimit);
        }
        return queryNouveau(indexName, query);
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
    public <T> List<T> searchViewWithRestrictions(Class<T> type, String indexName,
                                                  String text,
                                                  final @NotNull Map<String, Set<String>> subQueryRestrictions) {
        List <String> subQueries = new ArrayList<>();
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
        String query = AND.join(subQueries);

        return searchView(type, indexName, query);
    }

    private static @NotNull String formatSubquery(@NotNull Set<String> filterSet, final String fieldName) {
        final Function<String, String> addType = input -> {
            if (fieldName.equals("businessUnit") || fieldName.equals("tag") || fieldName.equals("projectResponsible") || fieldName.equals("createdBy")) {
                return fieldName + ":\"" + input + "\"";
            } if (fieldName.equals("createdOn") || fieldName.equals("timestamp")) {
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
            return "(\"" + sanitizeQueryInput(query) + "\")";
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
        List<Project> projectList = searchViewWithRestrictions(Project.class, indexName, text,
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

    private NouveauDesignDocument getNouveauDesignDocument(String id) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id cannot be empty");
        }
        return this.connector.get(NouveauDesignDocument.class, id);
    }

    private static boolean putNouveauDesignDocument(NouveauDesignDocument designDocument,
                                                    @NotNull DatabaseConnectorCloudant connector) {
        return connector.putDesignDocument(designDocument, designDocument.getId());
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

    public static @NotNull String dateToNouveauDouble(String date) throws ParseException {
        SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat inputFormatterDate = new SimpleDateFormat("yyyy-MM-dd");
        Date parsedDate;
        try {
            parsedDate = inputFormatterDate.parse(date);
        } catch (ParseException e) {
            parsedDate = new Date(Long.parseLong(date));
        } catch (Exception e) {
            throw new ParseException("Date format not recognized", 0);
        }
        return outputFormatter.format(parsedDate.getTime());
    }
}
