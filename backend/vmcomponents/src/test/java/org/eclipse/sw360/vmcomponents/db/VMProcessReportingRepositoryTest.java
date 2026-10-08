/*
 * SPDX-FileCopyrightText: © 2026 Siemens AG
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessSyncType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VMProcessReportingRepositoryTest {
    private static final String DB_NAME = DatabaseSettingsTest.COUCH_DB_VM;

    private DatabaseConnectorCloudant connector;
    private VMProcessReportingRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
        connector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
        repository = new VMProcessReportingRepository(connector);
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
    }

    @Test
    void shouldReturnLatestCompletedFullSyncRatherThanLatestDeltaOrLegacyProcess() throws SW360Exception {
        addProcess("2026-10-01 00:00:00", "2026-10-01 01:00:00", VMProcessSyncType.COMPLETE);
        addProcess("2026-10-02 00:00:00", "2026-10-02 01:00:00", VMProcessSyncType.COMPLETE);
        addProcess("2026-10-03 00:00:00", "2026-10-03 01:00:00", VMProcessSyncType.DELTA);
        addProcess("2026-10-04 00:00:00", "2026-10-04 01:00:00", null);

        VMProcessReporting latestFullSync =
                repository.getLastSuccessfulFullSyncByElementType("VMComponent");
        VMProcessReporting latestProcess =
                repository.getLastSuccessfulProcessByElementType("VMComponent");

        assertNotNull(latestFullSync);
        assertEquals("2026-10-02 01:00:00", latestFullSync.getEndDate());
        assertNotNull(latestProcess);
        assertEquals("2026-10-04 01:00:00", latestProcess.getEndDate());
    }

    @Test
    void shouldIgnoreIncompleteFullSyncReports() throws SW360Exception {
        addProcess("2026-10-01 00:00:00", null, VMProcessSyncType.COMPLETE);

        assertNull(repository.getLastSuccessfulFullSyncByElementType("VMComponent"));
    }

    private void addProcess(String startDate, String endDate, VMProcessSyncType syncType) throws SW360Exception {
        VMProcessReporting process = new VMProcessReporting("VMComponent", startDate);
        if (endDate != null) {
            process.setEndDate(endDate);
        }
        if (syncType != null) {
            process.setSyncType(syncType);
        }
        assertTrue(connector.add(process));
    }
}
