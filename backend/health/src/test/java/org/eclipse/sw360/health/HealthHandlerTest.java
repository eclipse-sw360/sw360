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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.thrift.health.Health;
import org.eclipse.sw360.datahandler.thrift.health.Status;
import org.eclipse.sw360.health.db.HealthDatabaseHandler;
import org.junit.After;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class HealthHandlerTest {

    @Autowired
    private HealthHandler healthHandler;

    @Autowired
    private HealthDatabaseHandler healthDatabaseHandler;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testGetHealthFailsUponMissingDB() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
        final Health health = healthHandler.getHealthOfSpecificDbs(healthDatabaseHandler.DATABASES_TO_CHECK);
        assertEquals(Status.DOWN, health.status);
        assertEquals(healthDatabaseHandler.DATABASES_TO_CHECK.size(), health.getDetails().size());
    }

    @Test
    public void testGetHealth() {
        final Health health = healthHandler.getHealthOfSpecificDbs(healthDatabaseHandler.DATABASES_TO_CHECK);
        assertEquals(Status.UP, health.status);
        assertEquals(new HashMap<>(), health.getDetails());
    }

    @Test
    public void testGetHealthWithPartialDBMissing() {
        final Health health = healthHandler.getHealthOfSpecificDbs(healthDatabaseHandler.DATABASES_TO_CHECK);
        assertEquals(Status.ERROR, health.getStatus());
        assertEquals(healthDatabaseHandler.DATABASES_TO_CHECK.size() -1, health.getDetails().size());
    }
}
