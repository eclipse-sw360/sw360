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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_TO_DEFAULT_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

/**
 * Class for accessing the Lucene connector on the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public abstract class AbstractDatabaseSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("all", new NouveauIndexFunction(
            "function(doc) {" +
            "  if(!doc.type) return;" +
            OBJ_TO_DEFAULT_INDEX +
            "  var objString = getObjAsString(doc);" +
            "  if (objString && objString.length > 0) {" +
            "    index('text', 'default', objString, {'store': true});" +
            "  }" +
            "  if (doc.type && typeof(doc.type) == 'string' && doc.type.length > 0) {" +
            "    index('text', 'type', doc.type, {'store': true});" +
            "  }" +
            "}"));

    private static final NouveauIndexDesignDocument luceneFilteredSearchView
        = new NouveauIndexDesignDocument("restrictedSearch", new NouveauIndexFunction(
            "function(doc) {" +
            "  if(!doc.type) return;" +
            OBJ_TO_DEFAULT_INDEX +
            "  var objString = getObjAsString(doc);" +
            "  if (objString && objString.length > 0) {" +
            "    index('text', 'default', objString, {'store': true});" +
            "  }" +
            "  if (doc.type && typeof(doc.type) == 'string' && doc.type.length > 0) {" +
            "    index('text', 'type', doc.type, {'store': true});" +
            "  }" +
            "  if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
            "    index('text', 'name', doc.name, {'store': true});" +
            "  }" +
            "  if (doc.fullname && typeof(doc.fullname) == 'string' && doc.fullname.length > 0) {" +
            "    index('text', 'fullname', doc.fullname, {'store': true});" +
            "  }" +
            "  if (doc.title && typeof(doc.title) == 'string' && doc.title.length > 0) {" +
            "    index('text', 'title', doc.title, {'store': true});" +
            "  }" +
            "}"));
    private final NouveauLuceneAwareDatabaseConnector connector;

    @Autowired
    public AbstractDatabaseSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_DATABASE") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        // Create the database connector and add the search view to couchDB
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson(), luceneSearchLimit);
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        searchView.addNouveau(luceneFilteredSearchView, gson);
        connector.setResultLimit(luceneSearchLimit);
        connector.addDesignDoc(searchView);
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
        if (typeMask != null && !typeMask.isEmpty() && typeMask.getLast().equals("document")) {
            if (typeMask.size() == 1) {
                return getSearchResults(query, user);
            }
            typeMask.removeLast();
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
        if (typeMask != null && !typeMask.isEmpty() && typeMask.getLast().equals("document")) {
            if (typeMask.size() == 1) {
                return search(query, user);
            }
            typeMask.removeLast();
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
            if (typeMask.contains("project") || typeMask.contains("component") || typeMask.contains("release") || typeMask.contains("package")) {
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

    private @NotNull List<SearchResult> getSearchResults(String queryString, User user) {
        NouveauResult queryLucene = connector.searchView(luceneSearchView.getIndexName(), queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }

    private @NotNull List<SearchResult> getFilteredSearchResults(String queryString, User user) {
        NouveauResult queryLucene = connector.searchView(luceneFilteredSearchView.getIndexName(), queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }

    private @NotNull List<SearchResult> convertLuceneResultAndFilterForVisibility(NouveauResult queryLucene, User user) {
        List<SearchResult> results = new ArrayList<>();
        if (queryLucene != null) {
            for (NouveauResult.Hits hit : queryLucene.getHits()) {
                SearchResult result = makeSearchResult(hit);
                if (!result.getName().isEmpty() && isVisibleToUser(result, user)) {
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
    private static @NotNull SearchResult makeSearchResult(@NotNull NouveauResult.Hits hit) {
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
