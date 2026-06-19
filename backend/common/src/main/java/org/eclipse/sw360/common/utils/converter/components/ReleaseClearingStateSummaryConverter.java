/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStateSummary;

public final class ReleaseClearingStateSummaryConverter {

    private ReleaseClearingStateSummaryConverter() {}

    public static ReleaseClearingStateSummary fromThrift(org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary thrift) {
        if (thrift == null) {
            return null;
        }
        ReleaseClearingStateSummary pojo = new ReleaseClearingStateSummary();
        if (thrift.isSetApproved()) {
            pojo.setApproved(thrift.getApproved());
        }
        if (thrift.isSetInternalUseScanAvailable()) {
            pojo.setInternalUseScanAvailable(thrift.getInternalUseScanAvailable());
        }
        if (thrift.isSetNewRelease()) {
            pojo.setNewRelease(thrift.getNewRelease());
        }
        if (thrift.isSetReportAvailable()) {
            pojo.setReportAvailable(thrift.getReportAvailable());
        }
        if (thrift.isSetScanAvailable()) {
            pojo.setScanAvailable(thrift.getScanAvailable());
        }
        if (thrift.isSetSentToClearingTool()) {
            pojo.setSentToClearingTool(thrift.getSentToClearingTool());
        }
        if (thrift.isSetUnderClearing()) {
            pojo.setUnderClearing(thrift.getUnderClearing());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary toThrift(ReleaseClearingStateSummary pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary thrift = new org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary();
        if (pojo.getApproved() != null) {
            thrift.setApproved(pojo.getApproved());
        }
        if (pojo.getInternalUseScanAvailable() != null) {
            thrift.setInternalUseScanAvailable(pojo.getInternalUseScanAvailable());
        }
        if (pojo.getNewRelease() != null) {
            thrift.setNewRelease(pojo.getNewRelease());
        }
        if (pojo.getReportAvailable() != null) {
            thrift.setReportAvailable(pojo.getReportAvailable());
        }
        if (pojo.getScanAvailable() != null) {
            thrift.setScanAvailable(pojo.getScanAvailable());
        }
        if (pojo.getSentToClearingTool() != null) {
            thrift.setSentToClearingTool(pojo.getSentToClearingTool());
        }
        if (pojo.getUnderClearing() != null) {
            thrift.setUnderClearing(pojo.getUnderClearing());
        }
        return thrift;
    }
}
