/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Sw360usersDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    @Autowired
    public Sw360usersDatabaseSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_USERS") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_USERS") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        super(db, dbName, luceneSearchLimit);
    }

    @Override
    protected boolean isVisibleToUser(SearchResult result, User user) {
        return true;
    }

}
