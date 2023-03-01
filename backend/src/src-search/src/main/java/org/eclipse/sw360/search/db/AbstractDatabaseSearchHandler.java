/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.db;

import com.cloudant.client.api.CloudantClient;
import com.github.ldriscoll.ektorplucene.LuceneResult;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

/**
 * Class for accessing the Lucene connector on the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public abstract class AbstractDatabaseSearchHandler {

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "all",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    function idx(obj) {" +
                    "        for (var key in obj) {" +
                    "            switch (typeof obj[key]) {" +
                    "                case 'object':" +
                    "                    idx(obj[key]);" +
                    "                    break;" +
                    "                case 'function':" +
                    "                    break;" +
                    "                default:" +
                    "                    ret.add(obj[key]);" +
                    "                    break;" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    idx(doc);" +
                    "    ret.add(doc.type, {\"field\": \"type\"} );" +
                    "    return ret;" +
                    "}");
    private static final LuceneSearchView luceneFilteredSearchView = new LuceneSearchView("lucene", "restrictedSearch",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    function idx(obj) {" +
                    "        for (var key in obj) {" +
                    "            switch (typeof obj[key]) {" +
                    "                case 'object':" +
                    "                    idx(obj[key]);" +
                    "                    break;" +
                    "                case 'function':" +
                    "                    break;" +
                    "                default:" +
                    "                    ret.add(obj[key]);" +
                    "                    break;" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    idx(doc);" +
                    "    ret.add(doc.type, {\"field\": \"type\"} );" +
                    "    if(doc.name && doc.name.length > 0) {  "+
                    "      ret.add(doc.name, {\"field\": \"name\"} );" +
                    "    }" +
                    "    if (doc.fullname && doc.fullname.length > 0) {  "+
                    "      ret.add(doc.fullname, {\"field\": \"fullname\"} );" +
                    "    }" +
                    "    if (doc.title && doc.title.length > 0) {  "+
                    "      ret.add(doc.title, {\"field\": \"title\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");
    private final LuceneAwareDatabaseConnector connector;

    public AbstractDatabaseSearchHandler(String dbName) throws IOException {
        // Create the database connector and add the search view to couchDB
        connector = new LuceneAwareDatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.getConfiguredClient(), dbName);
        connector.addView(luceneSearchView);
        connector.addView(luceneFilteredSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public AbstractDatabaseSearchHandler(Supplier<HttpClient> client, Supplier<CloudantClient> cclient, String dbName) throws IOException {
        // Create the database connector and add the search view to couchDB
        connector = new LuceneAwareDatabaseConnector(client, cclient, dbName);
        connector.addView(luceneSearchView);
        connector.addView(luceneFilteredSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    /**
     * Search the database for a given string
     */
    public List<SearchResult> search(String text, User user) {
        String queryString = prepareWildcardQuery(text);
        return getSearchResults(queryString, user);
    }

    /**
     * Search the database for a given string without wildcard
     */
    public List<SearchResult> searchWithoutWildcard(String text, User user, final List<String> typeMask) {
        String query = text;
        if (typeMask != null && !typeMask.isEmpty() && typeMask.get(typeMask.size() - 1).equals("document")) {
            if (typeMask.size() == 1) {
                return getSearchResults(query, user);
            }
            typeMask.remove(typeMask.size() - 1);
            final Function<String, String> addType = input -> "type:" + input;
            query = "( " + Joiner.on(" OR ").join(FluentIterable.from(typeMask).transform(addType)) + " ) AND "
                    + prepareWildcardQuery(text);
            return getSearchResults(query, user);
        }
        return restrictedSearch(text, typeMask, user);
    }

    /**
     * Search the database for a given string and types
     */
    public List<SearchResult> search(String text, final List<String> typeMask, User user) {
        String query = text;
        if (typeMask != null && !typeMask.isEmpty() && typeMask.get(typeMask.size() - 1).equals("document")) {
            if (typeMask.size() == 1) {
                return search(query, user);
            }
            typeMask.remove(typeMask.size() - 1);
            final Function<String, String> addType = input -> "type:" + input;
            query = "( " + Joiner.on(" OR ").join(FluentIterable.from(typeMask).transform(addType)) + " ) AND "
                    + prepareWildcardQuery(text);
            return getSearchResults(query, user);
        }
        return restrictedSearch(text, typeMask, user);
    }

    public List<SearchResult> restrictedSearch(String text, List<String> typeMask, User user) {
        String query = text;
        final Function<String, String> addType = input -> "type:" + input;
        final Function<String, String> addField = input -> input + ": (" + prepareWildcardQuery(text) + " )";
        List<String> typeField = new ArrayList<String>();
        if (typeMask == null || typeMask.isEmpty()) {
            typeField.add("name");
            typeField.add("fullname");
            typeField.add("title");
            query = "( " + Joiner.on(" OR ").join(FluentIterable.from(typeField).transform(addField)) + " ) ";
        } else {
            if (typeMask.contains("project") || typeMask.contains("component") || typeMask.contains("release")) {
                typeField.add("name");
            }
            if (typeMask.contains("license") || typeMask.contains("user") || typeMask.contains("vendor")) {
                typeField.add("fullname");
            }
            if (typeMask.contains("obligations")) {
                typeField.add("title");
            }
            query = "( " + Joiner.on(" OR ").join(FluentIterable.from(typeMask).transform(addType)) + " ) AND " + "( "
                    + Joiner.on(" OR ").join(FluentIterable.from(typeField).transform(addField)) + " ) ";
        }
        return getFilteredSearchResults(query, user);
    }

    private List<SearchResult> getSearchResults(String queryString, User user) {
        LuceneResult queryLucene = connector.searchView(luceneSearchView, queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }
    
    private List<SearchResult> getFilteredSearchResults(String queryString, User user) {
        LuceneResult queryLucene = connector.searchView(luceneFilteredSearchView, queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }

    private List<SearchResult> convertLuceneResultAndFilterForVisibility(LuceneResult queryLucene, User user) {
        List<SearchResult> results = new ArrayList<>();
        if (queryLucene != null) {
            for (LuceneResult.Row row : queryLucene.getRows()) {
                SearchResult result = makeSearchResult(row);
                if (result != null && !result.getName().isEmpty() && isVisibleToUser(result, user)) {
                    results.add(result);
                }
            }
        }
        return results;
    }

    abstract protected boolean isVisibleToUser(SearchResult result, User user);

    /**
     * Transforms a LuceneResult row into a Thrift SearchResult object
     */
    private static SearchResult makeSearchResult(LuceneResult.Row row) {
        SearchResult result = new SearchResult();

        // Set row properties
        result.id = row.getId();
        result.score = row.getScore();

        // Get document and
        SearchDocument parser = new SearchDocument(row.getDoc());

        // Get basic search results information
        result.type = parser.getType();
        result.name = parser.getName();

        return result;
    }

}
