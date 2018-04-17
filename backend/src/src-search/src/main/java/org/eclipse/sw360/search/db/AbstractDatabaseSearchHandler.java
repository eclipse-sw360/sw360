/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search.db;

import com.github.ldriscoll.ektorplucene.LuceneResult;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private final LuceneAwareDatabaseConnector connector;

    public AbstractDatabaseSearchHandler(String dbName) throws IOException {
        // Create the database connector and add the search view to couchDB
        connector = new LuceneAwareDatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    /**
     * Search the database for a given string
     */
    public List<SearchResult> search(String text, User user) {
        String queryString = text + "*";
        return getSearchResults(queryString, user);
    }

    /**
     * Search the database for a given string and types
     */
    public List<SearchResult> search(String text, final List<String> typeMask, User user) {

        if (typeMask == null || typeMask.isEmpty()) {
            return search(text, user);
        }

        final Function<String, String> addType = new Function<String, String>() {
            @Override
            public String apply(String input) {
                return "type:"+input;
            }
        };

        String query  = "( "+ Joiner.on(" OR ").join(FluentIterable.from(typeMask).transform(addType)) + " ) AND " +text+"*";
        return getSearchResults(query, user);
    }

    private List<SearchResult> getSearchResults(String queryString, User user) {
        LuceneResult queryLucene = connector.searchView(luceneSearchView, queryString);
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
