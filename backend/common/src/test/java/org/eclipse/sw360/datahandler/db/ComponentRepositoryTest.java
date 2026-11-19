/*
 * Copyright Pratyay Patel, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.assumeCanConnectTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentRepositoryTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String PICOCLI_ID = "picocli-component-id";
    private static final String JACKSON_ID = "jackson-core-component-id";

    private static ComponentRepository repository;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        assumeCanConnectTo(DatabaseSettingsTest.getCouchDbUrl());
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        VendorRepository vendorRepository = new VendorRepository(db);
        repository = new ComponentRepository(db, new ReleaseRepository(db, vendorRepository), vendorRepository);

        db.add(new Component().setId(PICOCLI_ID).setName("picocli"));
        db.add(new Component().setId(JACKSON_ID).setName("Jackson-Core"));
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    void shouldReturnComponentId_whenNameMatchesCaseInsensitively() {
        assertEquals(Set.of(PICOCLI_ID), repository.getComponentIdsByName("picocli", true));
    }

    @Test
    void shouldReturnComponentId_whenNameDiffersOnlyInCase() {
        assertEquals(Set.of(JACKSON_ID), repository.getComponentIdsByName("JACKSON-core", true));
    }

    @Test
    void shouldReturnEmptySet_whenNoComponentHasTheName() {
        assertTrue(repository.getComponentIdsByName("does-not-exist", true).isEmpty());
    }

    @Test
    void shouldReturnComponentId_whenNameMatchesCaseSensitively() {
        assertEquals(Set.of(JACKSON_ID), repository.getComponentIdsByName("Jackson-Core", false));
        assertTrue(repository.getComponentIdsByName("jackson-core", false).isEmpty());
    }
}
