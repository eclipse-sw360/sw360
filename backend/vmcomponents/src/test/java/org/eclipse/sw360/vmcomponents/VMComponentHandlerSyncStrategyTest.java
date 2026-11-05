/*
 * SPDX-FileCopyrightText: © 2026 Siemens AG
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessSyncType;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VMComponentHandlerSyncStrategyTest {
    private static final int CLEANUP_FREQUENCY_DAYS = 7;
    private static final String ELEMENT_TYPE = "VMComponent";

    @Test
    void shouldRunCompleteSyncWhenNoSuccessfulProcessExists() throws ParseException {
        VMProcessSyncType result = VMComponentHandler.determineSyncType(
                null, null, CLEANUP_FREQUENCY_DAYS, date("2026-10-08 00:00:00"));

        assertEquals(VMProcessSyncType.COMPLETE, result);
    }

    @Test
    void shouldRunCompleteSyncWhenNoRecordedCompleteSyncExists() throws ParseException {
        VMProcessReporting lastDelta = reporting("2026-10-07 00:00:00", VMProcessSyncType.DELTA);

        VMProcessSyncType result = VMComponentHandler.determineSyncType(
                lastDelta, null, CLEANUP_FREQUENCY_DAYS, date("2026-10-08 00:00:00"));

        assertEquals(VMProcessSyncType.COMPLETE, result);
    }

    @Test
    void shouldRunDeltaSyncBeforeCleanupFrequencyHasElapsed() throws ParseException {
        VMProcessReporting lastDelta = reporting("2026-10-07 00:00:00", VMProcessSyncType.DELTA);
        VMProcessReporting lastComplete = reporting("2026-10-02 00:00:00", VMProcessSyncType.COMPLETE);

        VMProcessSyncType result = VMComponentHandler.determineSyncType(
                lastDelta, lastComplete, CLEANUP_FREQUENCY_DAYS, date("2026-10-08 00:00:00"));

        assertEquals(VMProcessSyncType.DELTA, result);
    }

    @Test
    void shouldRunCompleteSyncWhenCleanupFrequencyHasElapsed() throws ParseException {
        VMProcessReporting lastDelta = reporting("2026-10-07 00:00:00", VMProcessSyncType.DELTA);
        VMProcessReporting lastComplete = reporting("2026-10-01 00:00:00", VMProcessSyncType.COMPLETE);

        VMProcessSyncType result = VMComponentHandler.determineSyncType(
                lastDelta, lastComplete, CLEANUP_FREQUENCY_DAYS, date("2026-10-08 00:00:00"));

        assertEquals(VMProcessSyncType.COMPLETE, result);
    }

    private static VMProcessReporting reporting(String endDate, VMProcessSyncType syncType) throws ParseException {
        return new VMProcessReporting(ELEMENT_TYPE, "2026-10-01 00:00:00")
                .setEndDate(endDate)
                .setSyncType(syncType);
    }

    private static Date date(String value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
    }
}
