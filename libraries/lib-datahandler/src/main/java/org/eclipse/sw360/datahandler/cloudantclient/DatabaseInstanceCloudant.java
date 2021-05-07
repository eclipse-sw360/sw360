/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import java.util.function.Supplier;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;

/**
 * Class for connecting to a given CouchDB instance
 */
public class DatabaseInstanceCloudant {

    public DatabaseInstanceCloudant() {
        DatabaseInstanceTrackerCloudant.track(this);
    }

    CloudantClient client = null;

    public DatabaseInstanceCloudant(Supplier<CloudantClient> client) {
        this.client = client.get();
    }

    public Database createDB(String dbName) {
        return checkIfDbExists(dbName) ? client.database(dbName, false) : client.database(dbName, true);
    }

    public boolean checkIfDbExists(String dbName) {
        return client.getAllDbs().contains(dbName);
    }

    public void destroy() {
        client.shutdown();
    }

    public void deleteDatabase(String dbName) {
        client.deleteDB(dbName);
    }
}
