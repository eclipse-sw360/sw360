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
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.eclipse.sw360.health.db.HealthDatabaseHandler;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.*;

public class HealthHandlerTest {
    public static final Set<String> DATABASES_TO_CHECK = ImmutableSet.of(
            DatabaseSettingsTest.COUCH_DB_ATTACHMENTS,
            DatabaseSettingsTest.COUCH_DB_DATABASE,
            DatabaseSettingsTest.COUCH_DB_USERS);
    @Test
    public void testGetHealthFailsUponMissingDB() throws MalformedURLException {
        TestUtils.deleteAllDatabases();
        HealthHandler healthHandler = new HealthHandler(DatabaseSettingsTest.getConfiguredHttpClient());
        final Health health = healthHandler.getHealthOfSpecificDbs(DATABASES_TO_CHECK);
        assertEquals(Status.DOWN, health.status);
        assertEquals(DATABASES_TO_CHECK.size(), health.getDetails().size());
    }

    @Test
    public void testGetHealth() throws MalformedURLException {
        for (String database : DATABASES_TO_CHECK) {
            TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), database);
        }

        HealthHandler healthHandler = new HealthHandler(DatabaseSettingsTest.getConfiguredHttpClient());
        final Health health = healthHandler.getHealth();
        assertEquals(Status.UP, health.status);
        assertEquals(new HashMap<>(), health.getDetails());

        for (String database : DATABASES_TO_CHECK) {
            TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), database);
        }
    }

    @Test
    public void testGetHealthWithPartialDBMissing() throws MalformedURLException {
        final String couchDbDatabase = DatabaseSettingsTest.COUCH_DB_DATABASE;
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), couchDbDatabase);

        HealthHandler healthHandler = new HealthHandler(DatabaseSettingsTest.getConfiguredHttpClient());
        final Health health = healthHandler.getHealthOfSpecificDbs(DATABASES_TO_CHECK);
        assertEquals(Status.ERROR, health.getStatus());
        assertEquals(DATABASES_TO_CHECK.size() -1, health.getDetails().size());

        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredHttpClient(), couchDbDatabase);
    }
}