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

import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.search.db.AbstractDatabaseSearchHandler;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Sw360usersDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    @Autowired
    public Sw360usersDatabaseSearchHandler(
            Cloudant client,
            @Qualifier("COUCH_DB_USERS") String dbName
    ) throws IOException {
        super(client, dbName);
    }

    @Override
    protected boolean isVisibleToUser(SearchResult result, User user) {
        return true;
    }

}
