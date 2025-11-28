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
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;

@Component
public class HealthDatabaseHandler {

    private final DatabaseInstanceCloudant db;

    public final Set<String> DATABASES_TO_CHECK;

    @Autowired
    public HealthDatabaseHandler(
            Cloudant client,
            @Qualifier("COUCH_DB_DATABASE") String dbName,
            @Qualifier("COUCH_DB_ATTACHMENTS") String attachmentDbName,
            @Qualifier("COUCH_DB_USERS") String usersDbName
    ) {
        db = new DatabaseInstanceCloudant(client);
        DATABASES_TO_CHECK = ImmutableSet.of(
                attachmentDbName,
                dbName,
                usersDbName);
    }

    public Health getHealth() {
        return getHealthOfDbs(DATABASES_TO_CHECK);
    }

    private Health getHealthOfDbs(@NotNull Set<String> dbsToCheck) {
        final Health health = new Health().setDetails(new HashMap<>());

        for (String database : dbsToCheck) {
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
            return health.getDetails().size() == dbsToCheck.size() ?
                    health.setStatus(Status.DOWN) :
                    health.setStatus(Status.ERROR);
        }
    }

    public Health getHealthOfSpecificDbs(Set<String> dbsToCheck) {
        return getHealthOfDbs(dbsToCheck);
    }
}
