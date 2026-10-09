/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SW360UtilsTest {

    @Test
    public void testGetBUFromOrganisation() throws Exception {
        assertEquals("CT BE OSS", SW360Utils.getBUFromOrganisation("CT BE OSS NE"));
        assertEquals("CT BE", SW360Utils.getBUFromOrganisation("CT BE"));
    }

    @Test
    public void testGetTotalReleaseCountIncludesAllStates() {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary();
        summary.newRelease = 1;
        summary.sentToClearingTool = 2;
        summary.underClearing = 3;
        summary.reportAvailable = 4;
        summary.approved = 5;
        summary.scanAvailable = 6;
        summary.internalUseScanAvailable = 7;

        assertEquals(28, SW360Utils.getTotalReleaseCount(summary));
    }

    @Test
    public void testGetOpenReleaseCountIncludesAllStates() {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary();
        summary.newRelease = 1;
        summary.sentToClearingTool = 2;
        summary.underClearing = 3;
        summary.reportAvailable = 4;
        summary.approved = 5;
        summary.scanAvailable = 6;
        summary.internalUseScanAvailable = 7;

        // open = total - (approved + reportAvailable) = 28 - 9
        assertEquals(19, SW360Utils.getOpenReleaseCount(summary));
    }

    @Test
    public void testGetTotalReleaseCountWithScanAvailableReleases() {
        // Regression test: scanned releases that are not cleared yet must be
        // part of the total count, otherwise a clearing request with only
        // scan-available and approved releases looks fully cleared.
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary();
        summary.scanAvailable = 3;
        summary.approved = 1;

        assertEquals(4, SW360Utils.getTotalReleaseCount(summary));
        assertEquals(3, SW360Utils.getOpenReleaseCount(summary));
    }

    @Test
    public void testGetTotalReleaseCountWithInternalUseScanAvailableReleases() {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary();
        summary.newRelease = 1;
        summary.internalUseScanAvailable = 2;

        assertEquals(3, SW360Utils.getTotalReleaseCount(summary));
        assertEquals(3, SW360Utils.getOpenReleaseCount(summary));
    }

    @Test
    public void testGetReleaseCountsWithEmptySummary() {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary();

        assertEquals(0, SW360Utils.getTotalReleaseCount(summary));
        assertEquals(0, SW360Utils.getOpenReleaseCount(summary));
    }
}
