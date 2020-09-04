/*
 * Copyright Bosch.IO 2020.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.health;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.eclipse.sw360.health.db.HealthDatabaseHandler;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class HealthHandlerTest {
    @Test
    public void testGetHealthFailsUponMissingDB() throws MalformedURLException {
        TestUtils.deleteAllDatabases();
        HealthHandler healthHandler = new HealthHandler();
        final Health health = healthHandler.getHealth();
        assertEquals(Status.DOWN, health.status);
        assertEquals(HealthDatabaseHandler.DATABASES_TO_CHECK.size(), health.getDetails().size());
    }

    @Test
    public void testGetHealth() throws MalformedURLException {
        for (String database : HealthDatabaseHandler.DATABASES_TO_CHECK) {
            TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), database);
        }

        HealthHandler healthHandler = new HealthHandler();
        final Health health = healthHandler.getHealth();
        assertEquals(Status.UP, health.status);
        assertEquals(new HashMap<>(), health.getDetails());

        for (String database : HealthDatabaseHandler.DATABASES_TO_CHECK) {
            TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), database);
        }
    }

    @Test
    public void testGetHealthWithPartialDBMissing() throws MalformedURLException {
        final String couchDbDatabase = DatabaseSettings.COUCH_DB_DATABASE;
        TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), couchDbDatabase);

        HealthHandler healthHandler = new HealthHandler();
        final Health health = healthHandler.getHealth();
        assertEquals(Status.ERROR, health.getStatus());
        assertEquals(HealthDatabaseHandler.DATABASES_TO_CHECK.size() -1, health.getDetails().size());

        TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), couchDbDatabase);
    }
}