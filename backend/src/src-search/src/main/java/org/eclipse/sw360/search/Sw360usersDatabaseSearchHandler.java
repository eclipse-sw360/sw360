/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.search.db.AbstractDatabaseSearchHandler;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.function.Supplier;

public class Sw360usersDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    public Sw360usersDatabaseSearchHandler() throws IOException {
        super(DatabaseSettings.COUCH_DB_USERS);
    }

    public Sw360usersDatabaseSearchHandler(Supplier<HttpClient> hclient, Supplier<CloudantClient> client, String dbName) throws IOException {
        super(hclient, client, dbName);
    }

    @Override
    protected boolean isVisibleToUser(SearchResult result, User user) {
        return true;
    }

}
