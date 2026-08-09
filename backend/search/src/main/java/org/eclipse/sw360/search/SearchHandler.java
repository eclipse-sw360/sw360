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

import com.google.common.collect.Sets;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.db.UserSearchHandler;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.eclipse.sw360.search.db.Sw360dbDatabaseSearchHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class SearchHandler implements SearchService.Iface {

    private static final Logger log = LogManager.getLogger(SearchHandler.class);

    private final Sw360dbDatabaseSearchHandler dbSw360db;
    private final UserSearchHandler dbSw360users;

    public SearchHandler() throws IOException {
        dbSw360db = new Sw360dbDatabaseSearchHandler();
        dbSw360users = new UserSearchHandler();
    }

    public SearchHandler(Cloudant client, String dbName) throws IOException {
        dbSw360db = new Sw360dbDatabaseSearchHandler(client, dbName);
        dbSw360users = new UserSearchHandler(client, dbName);
    }

    @Override
    public List<SearchResult> searchFiltered(String text, User user, List<String> typeMasks) throws TException {
        if(text == null) {
            throw new TException("Search text was null.");
        }
        if(text.isEmpty()) {
            return Collections.emptyList();
        }

        Set<SearchResult> results = Sets.newLinkedHashSet();
        if (typeMasks.isEmpty()) {
            typeMasks = new ArrayList<>(List.of("project", "component",
                    "license", "release", "obligation", "user", "vendor",
                    "document"));
        }
        // Query user and other database
        if (typeMasks.contains(SW360Constants.TYPE_USER)) {
            PaginationData pageData = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords()
                    .setAscending(true)
                    .setSortColumnNumber(UserSortColumn.BY_GIVENNAME.getValue());
            Map<PaginationData, List<User>> users = dbSw360users.searchByNameOrEmail(text, pageData);
            if (!CommonUtils.isNullOrEmptyMap(users)) {
                results.addAll(convertUsersToSearchResults(users.values().iterator().next()));
            }
        }
        if (
                (typeMasks.size() == 1 && !typeMasks.contains(SW360Constants.TYPE_USER))
                || (typeMasks.size() > 1)
        ) {
            results.addAll(dbSw360db.search(text, typeMasks, user));
        }

        List<SearchResult> srs = new ArrayList<SearchResult>(results);
        srs.sort(new SearchResultComparator());

        if (log.isTraceEnabled())
            log.trace("Search for {} returned {} results", text, results.size());

        return srs;
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

    private List<SearchResult> convertUsersToSearchResults(List<User> users) {
        return users.stream()
                .map(u -> new SearchResult(u.getId(), u.getType(), u.getFullname(), u.getFullname().charAt(0)))
                .toList();
    }
}
