/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.businessrules;

import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;

import java.util.List;

/**
 * In earlier days, this computer was important and did a lot of computation.
 * Nowadays, the
 * {@link ComponentService.Iface#updateRelease(Release, org.eclipse.sw360.datahandler.thrift.users.User)}
 * and the
 * {@link FossologyService.Iface#process(String, org.eclipse.sw360.datahandler.thrift.users.User)}
 * methods take care of keeping the {@link Release#clearingState} up to date so
 * that this computer really only needs to aggregate the state of all releases.
 */
public class ReleaseClearingStateSummaryComputer {

    public static ReleaseClearingStateSummary computeReleaseClearingStateSummary(List<Release> releases, String clearingTeam) {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary(0, 0, 0, 0, 0);

        if (releases == null) {
            return summary;
        }

        for (Release release : releases) {
            switch (release.getClearingState()) {
            case NEW_CLEARING:
                summary.newRelease++;
                break;
            case SENT_TO_CLEARING_TOOL:
                summary.sentToClearingTool++;
                break;
            case UNDER_CLEARING:
                summary.underClearing++;
                break;
            case REPORT_AVAILABLE:
                summary.reportAvailable++;
                break;
            case APPROVED:
                summary.approved++;
                break;
            default:
                summary.newRelease++;
                break;
            }
        }

        return summary;
    }

}
