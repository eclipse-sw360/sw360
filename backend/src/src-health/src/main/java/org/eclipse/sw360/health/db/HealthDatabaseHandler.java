/*
 * Copyright Bosch.IO 2020.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.health.db;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseInstance;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class HealthDatabaseHandler {

    private final DatabaseInstance db;

    public static final Set<String> DATABASES_TO_CHECK = Collections.unmodifiableSet(new HashSet<String>() {
        {
            add(DatabaseSettings.COUCH_DB_ATTACHMENTS);
            add(DatabaseSettings.COUCH_DB_DATABASE);
            add(DatabaseSettings.COUCH_DB_USERS);
        }
    });

    public HealthDatabaseHandler(Supplier<HttpClient> httpClient) throws MalformedURLException {
        db = new DatabaseInstance(httpClient.get());
    }

    public Health getHealth() {
        final Health health = new Health().setDetails(new HashMap<>());

        for (String database : DATABASES_TO_CHECK) {
            try {
                if (!db.checkIfDbExists(database)) {
                    health.getDetails().put(database, String.format("The database '%s' does not exist.", database));
                }
            } catch (Exception e) {
                health.getDetails().put(database, e.getMessage());
            }
        }

        if (health.getDetails().isEmpty()) {
            return health.setStatus(Status.UP);
        } else {
            return health.getDetails().size() == DATABASES_TO_CHECK.size() ?
                    health.setStatus(Status.DOWN) :
                    health.setStatus(Status.ERROR);
        }
    }
}
