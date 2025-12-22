/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search;

import com.google.common.collect.ImmutableMap;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.search.db.AbstractDatabaseSearchHandler;
import org.eclipse.sw360.search.db.Sw360dbDatabaseSearchHandler;
import org.eclipse.sw360.search.db.Sw360usersDatabaseSearchHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
@Component
public class SearchHandler implements SearchService.Iface {

    private static final Logger log = LogManager.getLogger(SearchHandler.class);

    @Autowired
    private Sw360dbDatabaseSearchHandler dbSw360db;
    @Autowired
    private Sw360usersDatabaseSearchHandler dbSw360users;
    ImmutableMap<String, String> specialCharToURLEncodedValue = ImmutableMap.of(" ", "%20", "+", "%2B", "@", "%40", "&", "%26", "#", "%23", "?", "%3F");

    @Override
    public List<SearchResult> searchFiltered(String text, User user, List<String> typeMask) throws TException {
        if(text == null) {
            throw new TException("Search text was null.");
        }
        if(text.isEmpty()) {
            return Collections.emptyList();
        }

        // Query user and other database
        Set<SearchResult> results = Sets.newLinkedHashSet();
        if (typeMask.isEmpty() || typeMask.contains(SW360Constants.TYPE_USER)) {
            if (text.contains("pkg:")) {
                dealWithSpecialCharacters(text, user, results, dbSw360users, List.of(SW360Constants.TYPE_USER));
            } else {
                results.addAll(dbSw360users.search(text, List.of(SW360Constants.TYPE_USER), user));
            }
        }
        if(typeMask.isEmpty() || !typeMask.getFirst().equals(SW360Constants.TYPE_USER) || typeMask.size() > 1) {
            if (text.contains("pkg:")) {
                dealWithSpecialCharacters(text, user, results, dbSw360db, typeMask);
            } else {
                results.addAll(dbSw360db.search(text, typeMask, user));
            }
        }

        List<SearchResult> srs = new ArrayList<SearchResult>(results);
        srs.sort(new SearchResultComparator());

        if (log.isTraceEnabled())
            log.trace("Search for {} returned {} results", text, results.size());

        return srs;
    }

    private void dealWithSpecialCharacters(String text, User user, Set<SearchResult> results, AbstractDatabaseSearchHandler dbSearchHandler, final List<String> typeMask) {
        String queryStringQuoted = "\""+text+"\"";
        results.addAll(dbSearchHandler.searchWithoutWildcard(queryStringQuoted, user, typeMask));
        searchWithDifferentCombinations(queryStringQuoted, user, results, dbSearchHandler,
                typeMask, specialCharToURLEncodedValue);

        Map<String, String> urlEncodedValToSpecialChar = specialCharToURLEncodedValue.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        searchWithDifferentCombinations(queryStringQuoted, user, results, dbSearchHandler,
                typeMask, urlEncodedValToSpecialChar);

    }

    private void searchWithDifferentCombinations(String queryStringQuoted, User user,
            Set<SearchResult> results, AbstractDatabaseSearchHandler dbSearchHandler, final List<String> typeMask,
            Map<String, String> specialCharToURLEncodedValue) {

        Set<String> specialChars = specialCharToURLEncodedValue.keySet();
        List<String> spCharsFoundInQuery = new ArrayList<>();

        for (String sc : specialChars) {
            String temp;
            if (queryStringQuoted.contains(sc)) {
                spCharsFoundInQuery.add(sc);
                temp = queryStringQuoted.replaceAll(Pattern.quote(sc), specialCharToURLEncodedValue.get(sc));
                results.addAll(dbSearchHandler.searchWithoutWildcard(temp, user, typeMask));
            }
        }

        String temp = queryStringQuoted;
        for (int i = 0; i < spCharsFoundInQuery.size(); i++) {
            for (int j = i + 1; j < spCharsFoundInQuery.size(); j++) {
                temp = temp
                        .replaceAll(Pattern.quote(spCharsFoundInQuery.get(i)),
                                specialCharToURLEncodedValue.get(spCharsFoundInQuery.get(i)))
                        .replaceAll(Pattern.quote(spCharsFoundInQuery.get(j)),
                                specialCharToURLEncodedValue.get(spCharsFoundInQuery.get(j)));
                results.addAll(dbSearchHandler.searchWithoutWildcard(temp, user, typeMask));
            }
        }
    }

    @Override
    public List<SearchResult> search(String text, User user) throws TException {
        return searchFiltered(text,user,null);
    }


    /**
     * Comparator to provide ordered search results
     */
    public class SearchResultComparator implements Comparator<SearchResult> {

        @Override
        public int compare(SearchResult o1, SearchResult o2) {
            return -Double.compare(o1.getScore(), o2.getScore());
        }

    }

}
