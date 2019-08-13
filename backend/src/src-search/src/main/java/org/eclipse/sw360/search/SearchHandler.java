/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.search.db.AbstractDatabaseSearchHandler;
import org.eclipse.sw360.search.db.Sw360dbDatabaseSearchHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class SearchHandler implements SearchService.Iface {

    private static final Logger log = Logger.getLogger(SearchHandler.class);

    private final AbstractDatabaseSearchHandler dbSw360db;
    private final AbstractDatabaseSearchHandler dbSw360users;

    public SearchHandler() throws IOException {
        dbSw360db = new Sw360dbDatabaseSearchHandler();
        dbSw360users = new Sw360usersDatabaseSearchHandler();
    }


    @Override
    public List<SearchResult> searchFiltered(String text, User user, List<String> typeMask) throws TException {
        if(text == null) {
            throw new TException("Search text was null.");
        }
        if(text.isEmpty()) {
            return Collections.emptyList();
        }

        // Query user and other database
        List<SearchResult> results = Lists.newArrayList();
        if (typeMask.isEmpty() || typeMask.contains(SW360Constants.TYPE_USER)) {
            results.addAll(dbSw360users.search(text, Arrays.asList(SW360Constants.TYPE_USER), user));
        }
        if(typeMask.isEmpty() || !typeMask.get(0).equals(SW360Constants.TYPE_USER) || typeMask.size() > 1) {
            results.addAll(dbSw360db.search(text, typeMask, user));
        }
        Collections.sort(results, new SearchResultComparator());

        if (log.isTraceEnabled())
            log.trace("Search for " + text + " returned " + results.size() + " results");

        return results;
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
