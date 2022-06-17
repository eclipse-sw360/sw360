/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb.lucene;

import com.github.ldriscoll.ektorplucene.EktorpLuceneObjectMapperFactory;
import com.github.ldriscoll.ektorplucene.LuceneAwareCouchDbConnector;
import com.github.ldriscoll.ektorplucene.LuceneQuery;
import com.github.ldriscoll.ektorplucene.LuceneResult;
import com.github.ldriscoll.ektorplucene.util.IndexUploader;
import com.google.common.base.Joiner;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;
import org.ektorp.http.URI;
import org.ektorp.support.DesignDocument;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cloudant.client.api.CloudantClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;


/**
 * Generic database connector for handling lucene searches
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class LuceneAwareDatabaseConnector extends LuceneAwareCouchDbConnector {

    private static final Logger log = LogManager.getLogger(LuceneAwareDatabaseConnector.class);

    private static final Joiner AND = Joiner.on(" AND ");
    private static final Joiner OR = Joiner.on(" OR ");

    private final DatabaseConnectorCloudant connector;

    private static final List<String> LUCENE_SPECIAL_CHARACTERS = Arrays.asList("[\\\\\\+\\-\\!\\~\\*\\?\\\"\\^\\:\\(\\)\\{\\}\\[\\]]", "\\&\\&", "\\|\\|");
    private String dbNameForLuceneSearch;
    /**
     * Maximum number of results to return
     */
    private int resultLimit = 0;

    /**
     * URL/DbName constructor
     */

    public LuceneAwareDatabaseConnector(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        this(new DatabaseConnector(httpClient, dbName), cClient);
    }

    /**
     * Constructor using a Database connector
     */
    public LuceneAwareDatabaseConnector(DatabaseConnector connector, Supplier<CloudantClient> cClient) throws IOException {
        super(connector.getDbName(), connector.getInstance());
        this.dbNameForLuceneSearch = connector.getDbName();
        setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        this.connector = new DatabaseConnectorCloudant(cClient, connector.getDbName());
    }

    public boolean addView(LuceneSearchView function) {
        // make sure that the indexer is up-to-date
        IndexUploader uploader = new IndexUploader();
        return uploader.updateSearchFunctionIfNecessary(this, function.searchView,
                function.searchFunction, function.searchBody);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public <T> List<T> searchView(Class<T> type, LuceneSearchView function, String queryString) {
        return connector.get(type, searchIds(type,function, queryString));
    }

    /**
     * Search with lucene using the previously declared search function only for ids
     */
    public <T> List<String> searchIds(Class<T> type, LuceneSearchView function, String queryString) {
        LuceneResult queryLuceneResult = searchView(function, queryString, false);
        return getIdsFromResult(queryLuceneResult);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public LuceneResult searchView(LuceneSearchView function, String queryString) {
        return searchView(function, queryString, true);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    private LuceneResult searchView(LuceneSearchView function, String queryString, boolean includeDocs) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        try {
            LuceneResult callLuceneDirectly = callLuceneDirectly(function, queryString, includeDocs);
            return callLuceneDirectly;
        } catch (Exception exp) {
            log.error("Error querying Lucene directly.", exp);
        }
        return null;
    }

    private LuceneResult callLuceneDirectly(LuceneSearchView function, String queryString, boolean includeDocs)
            throws IOException {
        URI queryURI = URI.of("/");
        queryURI.append(DEFAULT_LUCENE_INDEX);
        queryURI.append(dbNameForLuceneSearch);
        queryURI.append(function.searchView.startsWith(DesignDocument.ID_PREFIX) ? function.searchView
                : DesignDocument.ID_PREFIX + function.searchView);
        queryURI.append(function.searchFunction);
        queryURI.param("include_docs", new Boolean(includeDocs).toString());
        if (resultLimit > 0) {
            queryURI.param("limit", resultLimit);
        }
        queryURI.param("q", queryString.toString());
        URL luceneResourceUrl = new URL(DatabaseSettings.COUCH_DB_LUCENE_URL + queryURI.toString());
        ObjectMapper objectMapper = new EktorpLuceneObjectMapperFactory().createObjectMapper();
        HttpURLConnection connection = null;
        try {
            connection = makeLuceneRequest(luceneResourceUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return objectMapper.readValue(connection.getInputStream(), LuceneResult.class);
            } else {
                connection.disconnect();
                log.error("Getting error with reponse code = " + responseCode + ".Retrying with stale parameter");
                queryURI.param("stale", "ok");
                luceneResourceUrl = new URL(DatabaseSettings.COUCH_DB_LUCENE_URL + queryURI.toString());
                connection = makeLuceneRequest(luceneResourceUrl);
                responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return objectMapper.readValue(connection.getInputStream(), LuceneResult.class);
                } else {
                    log.error("Retried with stale parameter.Getting error with reponse code=" + responseCode);
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private HttpURLConnection makeLuceneRequest(URL luceneResourceUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) luceneResourceUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection;
    }

    /////////////////////////
    // GETTERS AND SETTERS //
    /////////////////////////

    private void setQueryLimit(LuceneQuery query) {
        if (resultLimit > 0) {
            query.setLimit(resultLimit);
        }
    }

    public void setResultLimit(int limit) {
        if (limit >= 0) {
            resultLimit = limit;
        }
    }

    ////////////////////
    // HELPER METHODS //
    ////////////////////

    private static List<String> getIdsFromResult(LuceneResult result) {
        List<String> ids = new ArrayList<>();
        if (result != null) {
            for (LuceneResult.Row row : result.getRows()) {
                ids.add(row.getId());
            }
        }
        return ids;
    }


    /**
     * Search the database for a given string and types
     */
    public <T> List<T> searchViewWithRestrictions(Class<T> type,LuceneSearchView luceneSearchView, String text, final Map<String , Set<String > > subQueryRestrictions) {
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

        String query  = AND.join(subQueries);
        return searchView(type, luceneSearchView, query);
    }

    public List<Project> searchProjectViewWithRestrictionsAndFilter(LuceneSearchView luceneSearchView, String text,
            final Map<String, Set<String>> subQueryRestrictions, User user) {
        List<Project> projectList = searchViewWithRestrictions(Project.class, luceneSearchView, text,
                subQueryRestrictions);
        return projectList.stream().filter(ProjectPermissions.isVisible(user)).collect(Collectors.toList());
    }

    private static String formatSubquery(Set<String> filterSet, final String fieldName) {
        final Function<String, String> addType = input -> {
            if (fieldName.equals("businessUnit") || fieldName.equals("tag") || fieldName.equals("projectResponsible") || fieldName.equals("createdBy")) {
                return fieldName + ":\"" + input + "\"";
            } if (fieldName.equals("createdOn") || fieldName.equals("timestamp")) {
                return fieldName + "<date>:" + input;
            } else {
                return fieldName + ":" + input;
            }
        };

        Stream<String> searchFilters = filterSet.stream().map(addType);
        return "( " + OR.join(searchFilters.collect(Collectors.toList())) + " ) ";
    }

    public static String prepareWildcardQuery(String query) {
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

    public static String prepareFuzzyQuery(String query) {
        return sanitizeQueryInput(query) + "~";
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
}
