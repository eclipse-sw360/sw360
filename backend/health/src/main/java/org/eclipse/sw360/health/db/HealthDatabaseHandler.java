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

import com.google.common.collect.ImmutableSet;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.sdk.core.service.exception.ServiceUnavailableException;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Set;

public class HealthDatabaseHandler {

    private final DatabaseInstanceCloudant db;

    public static final Set<String> DATABASES_TO_CHECK = ImmutableSet.of(
            DatabaseSettings.COUCH_DB_ATTACHMENTS,
            DatabaseSettings.COUCH_DB_DATABASE,
            DatabaseSettings.COUCH_DB_USERS);

    public HealthDatabaseHandler(Cloudant client) throws MalformedURLException {
        db = new DatabaseInstanceCloudant(client);
    }

    public Health getHealth() {
        return getHealthOfDbs(DATABASES_TO_CHECK);
    }

    private Health getHealthOfDbs(Set<String> dbsTocheck) {
        final Health health = new Health().setDetails(new HashMap<>());

        for (String database : dbsTocheck) {
            try {
                if (!db.checkIfDbExists(database)) {
                    health.getDetails().put(database, String.format("The database '%s' does not exist.", database));
                }
            } catch (ServiceUnavailableException e) {
                health.getDetails().put(database, e.getMessage());
            }
        }

        if (health.getDetails().isEmpty()) {
            return health.setStatus(Status.UP);
        } else {
            return health.getDetails().size() == dbsTocheck.size() ?
                    health.setStatus(Status.DOWN) :
                    health.setStatus(Status.ERROR);
        }
    }

    public Health getHealthOfSpecificDbs(Set<String> dbsToCheck) {
        return getHealthOfDbs(dbsToCheck);
    }
}
