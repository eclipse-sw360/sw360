/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.common.utils.converter.vmcomponents.VMMatchConverter;
import org.eclipse.sw360.common.utils.converter.vmcomponents.VMProcessReportingConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchState;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.common.SVMUtils;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.apache.log4j.Logger.getLogger;

/**
 * SVM vulnerability monitoring service handler.
 *
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
@Service
public class VMComponentHandler {

    private static final Logger log = getLogger(VMComponentHandler.class);

    private final VMDatabaseHandler dbHandler;
    private final ComponentDatabaseHandler compHandler;

    public VMComponentHandler() throws IOException {
        dbHandler = new VMDatabaseHandler();
        compHandler = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    public List<org.eclipse.sw360.datahandler.services.vmcomponents.VMProcessReporting> getAllProcesses(User user) {
        if (PermissionUtils.isAdmin(user)) {
            return dbHandler.getAll(VMProcessReporting.class).stream()
                    .map(VMProcessReportingConverter::fromThrift)
                    .toList();
        }
        return Collections.emptyList();
    }

    public List<org.eclipse.sw360.datahandler.services.vmcomponents.VMMatch> getAllMatches(User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return Collections.emptyList();
        }
        return dbHandler.getAll(VMMatch.class).stream()
                .map(VMMatchConverter::fromThrift)
                .toList();
    }

    public RequestSummary synchronizeComponents() {
        VMProcessHandler.cacheVendors(compHandler);

        // synchronize VMAction
        String actionStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMAction.class.getSimpleName(), actionStart));
        synchronizeElementType(VMAction.class, SVMConstants.ACTIONS_URL);
        log.info("Storing and getting master data of " + VMAction.class.getSimpleName() + " triggered. waiting for completion...");

        // synchronize VMPriority
        String prioStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMPriority.class.getSimpleName(), prioStart));
        synchronizeElementType(VMPriority.class, SVMConstants.PRIORITIES_URL);
        log.info("Storing and getting master data of " + VMPriority.class.getSimpleName() + " triggered. waiting for completion...");

        // synchronize VMComponent
        String compStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMComponent.class.getSimpleName(), compStart));
        synchronizeElementType(VMComponent.class, SVMConstants.COMPONENTS_URL);
        log.info("Storing and getting master data of " + VMComponent.class.getSimpleName() + " triggered. waiting for completion...");

        // synchronize Vulnerability (bulk notifications)
        String vulnStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(Vulnerability.class.getSimpleName(), vulnStart));
        synchronizeElementType(Vulnerability.class, SVMConstants.VULNERABILITIES_URL);
        log.info("Storing and getting master data of " + Vulnerability.class.getSimpleName() + " triggered. waiting for completion...");

        // triggerReporting
        VMProcessHandler.triggerReport(VMAction.class, actionStart);
        VMProcessHandler.triggerReport(VMPriority.class, prioStart);
        VMProcessHandler.triggerReport(VMComponent.class, compStart);
        VMProcessHandler.triggerReport(Vulnerability.class, vulnStart);

        return new RequestSummary().setRequestStatus(RequestStatus.SUCCESS);
    }

    /**
     * <p>Synchronize a single SVM element type. Decides between full sync (with
     * cleanup) and delta sync based on time elapsed since the last successful
     * sync.</p>
     * <p>Sync strategy:<ul>
     * <li>First run or no previous sync: full sync (no modified_after parameter).</li>
     * <li>{@code Elapsed >= CLEANUP_FREQUENCY_DAYS}: full sync to purge SVM-side deletions from local DB.</li>
     * <li>Otherwise: delta sync using modified_after = {@code lastEndDate - SVMSYNC_DELTA_OFFSET_DAYS}.</li>
     * </ul></p>
     */
    private <T extends TBase> void synchronizeElementType(Class<T> elementType, String url) {
        VMProcessReporting lastProcess = dbHandler.getLastSuccessfulProcessByElementType(elementType.getSimpleName());
        String modifiedAfter = null;
        String syncType = "full";

        if (lastProcess != null && lastProcess.isSetEndDate()) {
            long daysSinceLastSync = calculateDaysSinceLastSync(lastProcess.getEndDate());
            if (daysSinceLastSync >= SVMConstants.CLEANUP_FREQUENCY_DAYS) {
                // Time for periodic full sync (includes cleanup of items deleted on SVM)
                syncType = "full (cleanup)";
            } else {
                // Use delta sync with configured overlap window
                modifiedAfter = SVMUtils.calculateModifiedAfter(
                        lastProcess.getEndDate(), SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS);
                syncType = "delta(" + SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS + "d)";
            }
        }

        log.info(String.format("SVM Sync [%s]: %s sync, last=%s, modified_after=%s",
            elementType.getSimpleName(), syncType,
            lastProcess != null ? lastProcess.getEndDate() : "none",
            modifiedAfter != null ? modifiedAfter : "none"));

        if (modifiedAfter != null) {
            VMProcessHandler.getElementIdsWithModifiedAfter(elementType, url, modifiedAfter, true);
        } else {
            VMProcessHandler.getElementIds(elementType, url, true);
        }
    }

    /**
     * Calculate days elapsed since last sync end date.
     *
     * @param lastEndDate end date from last successful sync
     * @return number of days since last sync, or 0 if calculation fails
     */
    private long calculateDaysSinceLastSync(String lastEndDate) {
        if (CommonUtils.isNullEmptyOrWhitespace(lastEndDate)) {
            return 0;
        }
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date lastDate = format.parse(lastEndDate);
            java.util.Date now = new java.util.Date();
            long diffMillis = now.getTime() - lastDate.getTime();
            return diffMillis / (1000 * 60 * 60 * 24);  // convert to days
        } catch (Exception e) {
            log.warn("Failed to calculate days since last sync: " + e.getMessage());
            return 0;
        }
    }

    public RequestSummary triggerReverseMatch() {
        Set<String> releaseIds = compHandler.getAllReleaseIds();
        if (releaseIds != null && !releaseIds.isEmpty()) {
            for (String releaseId : releaseIds) {
                VMProcessHandler.findReleaseMatch(releaseId, true);
            }
        }
        log.info("Reverse match triggered for " + (releaseIds == null ? 0 : releaseIds.size()) + " releases. waiting for completion...");
        return new RequestSummary().setRequestStatus(RequestStatus.SUCCESS);
    }

    public RequestSummary acceptMatch(User user, String matchId) {
        return setMatchState(user, matchId, VMMatchState.ACCEPTED);
    }

    public RequestSummary declineMatch(User user, String matchId) {
        return setMatchState(user, matchId, VMMatchState.DECLINED);
    }

    private RequestSummary setMatchState(User user, String matchId, VMMatchState state) {
        if (!PermissionUtils.isAdmin(user) || StringUtils.isEmpty(matchId)) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }

        VMMatch match = dbHandler.getById(VMMatch.class, matchId);
        if (match == null) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        match.setState(state);
        return new RequestSummary().setRequestStatus(ThriftConverter.fromThriftRequestStatus(dbHandler.update(match)));
    }
}
