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

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_MAX_LENGTH;
import static org.eclipse.sw360.datahandler.common.SearchUtils.EMIT_EDGE_N_GRAM_INDEX;
import static org.eclipse.sw360.datahandler.common.SearchUtils.OBJ_TO_DEFAULT_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.convertToFreeSearch;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.sanitizeLuceneString;
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
            "    index('text', 'default', objString);" +
            "  }" +
            "  if (doc.type && typeof(doc.type) == 'string' && doc.type.length > 0) {" +
            "    index('string', 'type', doc.type);" +
            "  }" +
            "}")
            .setFieldAnalyzer(
                    Map.ofEntries(
                            Map.entry("type", "keyword")
                    )
            )
            .setDefaultAnalyzer("standard")
    );

    private static final NouveauIndexDesignDocument luceneFilteredSearchView
        = new NouveauIndexDesignDocument("restrictedSearch", new NouveauIndexFunction(
            "function(doc) {" +
            "  if(!doc.type) return;" +
            OBJ_TO_DEFAULT_INDEX +
            EMIT_EDGE_N_GRAM_INDEX +
            "  var objString = getObjAsString(doc);" +
            "  if (objString && objString.length > 0) {" +
            "    index('text', 'default', objString);" +
            "  }" +
            "  if (doc.type && typeof(doc.type) == 'string' && doc.type.length > 0) {" +
            "    index('string', 'type', doc.type);" +
            "  }" +
            "  if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
            "    index('text', 'name_exact', doc.name);" +
            "    emitEdgeNGrams('name_ngram', doc.name, 2, " + EDGE_NGRAM_MAX_LENGTH + ");" +
            "    index('string', 'name_sort', doc.name.toLowerCase());" +
            "  }" +
            "  if (doc.fullname && typeof(doc.fullname) == 'string' && doc.fullname.length > 0) {" +
            "    index('text', 'fullname_exact', doc.fullname);" +
            "    emitEdgeNGrams('fullname_ngram', doc.fullname, 2, " + EDGE_NGRAM_MAX_LENGTH + ");" +
            "    index('string', 'fullname_sort', doc.fullname.toLowerCase());" +
            "  }" +
            "  if (doc.title && typeof(doc.title) == 'string' && doc.title.length > 0) {" +
            "    index('text', 'title_exact', doc.title);" +
            "    emitEdgeNGrams('title_ngram', doc.title, 2, " + EDGE_NGRAM_MAX_LENGTH + ");" +
            "    index('string', 'title_sort', doc.title.toLowerCase());" +
            "  }" +
            "}")
            .setFieldAnalyzer(
                    Map.ofEntries(
                            Map.entry("type", "keyword"),
                            Map.entry("name_ngram", "whitespace"),
                            Map.entry("fullname_ngram", "whitespace"),
                            Map.entry("title_ngram", "whitespace"),
                            Map.entry("name_sort", "keyword"),
                            Map.entry("fullname_sort", "keyword")
                    )
            )
            .setDefaultAnalyzer("standard")
    );
    private final NouveauLuceneAwareDatabaseConnector connector;

    public AbstractDatabaseSearchHandler(String dbName) throws IOException {
        Cloudant client = DatabaseSettings.getConfiguredClient();
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        // Create the database connector and add the search view to couchDB
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        searchView.addNouveau(luceneFilteredSearchView, gson);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        connector.addDesignDoc(searchView);
    }

    public AbstractDatabaseSearchHandler(Cloudant client, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        // Create the database connector and add the search view to couchDB
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        searchView.addNouveau(luceneFilteredSearchView, gson);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
        connector.addDesignDoc(searchView);
    }

    /**
     * Search the database for a given string
     */
    public List<SearchResult> search(String text, User user) {
        String queryString = convertToFreeSearch(text);
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
                    + sanitizeLuceneString(text);
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
                    + "( " + convertToFreeSearch(text) + " )";
            return getSearchResults(query, user);
        }
        return restrictedSearch(text, typeMask, user);
    }

    public List<SearchResult> restrictedSearch(String text, List<String> typeMask, User user) {
        String query;
        if (CommonUtils.isNullOrEmptyCollection(typeMask)) {
            Map<String, String> subQueryRestrictions = Map.of(
                    "name", text,
                    "fullname", text,
                    "title", text
            );
            // Type check is only on Package. Thus considered irrelevant here.
            query = NouveauLuceneAwareDatabaseConnector.convertToRestrictiveQueryWithOr(User.class, subQueryRestrictions, false);
        } else {
            Map<String, String> subQueryRestrictions = new HashMap<>();
            if (typeMask.contains("project") || typeMask.contains("component") || typeMask.contains("release") || typeMask.contains("package")) {
                subQueryRestrictions.put("name", text);
            }
            if (typeMask.contains("license") || typeMask.contains("user") || typeMask.contains("vendor")) {
                subQueryRestrictions.put("fullname", text);
            }
            if (typeMask.contains("obligations")) {
                subQueryRestrictions.put("title", text);
            }
            String valueQuery = NouveauLuceneAwareDatabaseConnector.convertToRestrictiveQueryWithOr(User.class, subQueryRestrictions, false);
            Map<String, String> typeRestrictions = new HashMap<>();
            for (String typeM : typeMask) {
                typeRestrictions.put("type", typeM);
            }
            String typeQuery = NouveauLuceneAwareDatabaseConnector.convertToRestrictiveQueryWithOr(User.class, typeRestrictions, false);
            query = "( " + valueQuery + " ) AND ( " + typeQuery + " )";
        }
        return getFilteredSearchResults(query, user);
    }

    private @NonNull List<SearchResult> getSearchResults(String queryString, User user) {
        NouveauResult queryLucene = connector.searchView(luceneSearchView.getIndexName(), queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }

    private @NonNull List<SearchResult> getFilteredSearchResults(String queryString, User user) {
        NouveauResult queryLucene = connector.searchView(luceneFilteredSearchView.getIndexName(), queryString);
        return convertLuceneResultAndFilterForVisibility(queryLucene, user);
    }

    private @NonNull List<SearchResult> convertLuceneResultAndFilterForVisibility(NouveauResult queryLucene, User user) {
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
    private static @NonNull SearchResult makeSearchResult(NouveauResult.@NonNull Hits hit) {
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
