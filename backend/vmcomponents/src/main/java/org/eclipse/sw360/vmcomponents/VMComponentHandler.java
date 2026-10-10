/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.common.SVMUtils;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.apache.log4j.Logger.getLogger;

/**
 * Implementation of the Thrift service
 *
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
public class VMComponentHandler implements VMComponentService.Iface {

    private static final Logger log = getLogger(VMComponentHandler.class);

    private final VMDatabaseHandler dbHandler;
    private final ComponentDatabaseHandler compHandler;


    public VMComponentHandler() throws IOException {
        dbHandler = new VMDatabaseHandler();
        compHandler = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    @Override
    public List<VMProcessReporting> getAllProcesses(User user) throws TException {
        if (PermissionUtils.isAdmin(user)){
            return dbHandler.getAll(VMProcessReporting.class);
        }
        return Collections.emptyList();
    }

    @Override
    public List<VMMatch> getAllMatches(User user) throws TException {
        if (!PermissionUtils.isAdmin(user)){
            return Collections.emptyList();
        }
        return dbHandler.getAll(VMMatch.class);
    }

    @Override
    public RequestSummary synchronizeComponents() throws TException {
        VMProcessHandler.cacheVendors(compHandler);

        // synchronize VMAction
        String actionStart = synchronizeElementType(VMAction.class, SVMConstants.ACTIONS_URL);
        log.info("Storing and getting master data of "+VMAction.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMPriority
        String prioStart = synchronizeElementType(VMPriority.class, SVMConstants.PRIORITIES_URL);
        log.info("Storing and getting master data of "+VMPriority.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMComponent
        String compStart = synchronizeElementType(VMComponent.class, SVMConstants.COMPONENTS_URL);
        log.info("Storing and getting master data of "+VMComponent.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize Vulnerability (bulk notifications)
        String vulnStart = synchronizeElementType(Vulnerability.class, SVMConstants.VULNERABILITIES_URL);
        log.info("Storing and getting master data of "+Vulnerability.class.getSimpleName()+" triggered. waiting for completion...");

        // triggerReporting
        VMProcessHandler.triggerReport(VMAction.class, actionStart);
        VMProcessHandler.triggerReport(VMPriority.class, prioStart);
        VMProcessHandler.triggerReport(VMComponent.class, compStart);
        VMProcessHandler.triggerReport(Vulnerability.class, vulnStart);

        return new RequestSummary(RequestStatus.SUCCESS);
    }

    /**
     * <p>Synchronize a single SVM element type. Decides between full sync (with
     * cleanup) and delta sync based on time elapsed since the last successful
     * complete sync.</p>
     * <p>Sync strategy:<ul>
     * <li>First run or no previous sync: full sync (no modified_after parameter).</li>
     * <li>{@code Elapsed since last complete sync >= CLEANUP_FREQUENCY_DAYS}: full sync to purge SVM-side deletions from local DB.</li>
     * <li>Otherwise: delta sync using modified_after = {@code lastEndDate - SVMSYNC_DELTA_OFFSET_DAYS}.</li>
     * </ul></p>
     */
    private <T extends TBase> String synchronizeElementType(Class<T> elementType, String url) {
        VMProcessReporting lastProcess = dbHandler.getLastSuccessfulProcessByElementType(elementType.getSimpleName());
        VMProcessReporting lastFullSync = dbHandler.getLastSuccessfulFullSyncByElementType(elementType.getSimpleName());
        VMProcessSyncType syncType = determineSyncType(
                lastProcess, lastFullSync, SVMConstants.CLEANUP_FREQUENCY_DAYS, new Date());
        String modifiedAfter = null;

        if (syncType == VMProcessSyncType.DELTA) {
            modifiedAfter = SVMUtils.calculateModifiedAfter(
                    lastProcess.getEndDate(), SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS);
        }

        String startDate = SW360Utils.getCreatedOnTime();
        VMProcessReporting reporting = new VMProcessReporting(elementType.getSimpleName(), startDate)
                .setSyncType(syncType);
        dbHandler.add(reporting);

        String syncDescription = syncType == VMProcessSyncType.COMPLETE
                ? "full (cleanup)"
                : "delta(" + SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS + "d)";
        log.info(String.format("SVM Sync [%s]: %s sync, last=%s, modified_after=%s",
            elementType.getSimpleName(), syncDescription,
            lastProcess != null ? lastProcess.getEndDate() : "none",
            modifiedAfter != null ? modifiedAfter : "none"));

        if (modifiedAfter != null) {
            VMProcessHandler.getElementIdsWithModifiedAfter(elementType, url, modifiedAfter, true);
        } else {
            VMProcessHandler.getElementIds(elementType, url, true);
        }
        return startDate;
    }

    /**
     * Select a full sync when there is no successful baseline, no known complete
     * sync, or the last complete sync is old enough to require cleanup.
     */
    static VMProcessSyncType determineSyncType(
            VMProcessReporting lastProcess, VMProcessReporting lastFullSync,
            int cleanupFrequencyDays, Date now
    ) {
        if (lastProcess == null || !lastProcess.isSetEndDate()
                || lastFullSync == null || !lastFullSync.isSetEndDate()) {
            return VMProcessSyncType.COMPLETE;
        }

        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lastDate = format.parse(lastFullSync.getEndDate());
            long diffMillis = now.getTime() - lastDate.getTime();
            long daysSinceLastFullSync = diffMillis / (1000 * 60 * 60 * 24);
            return daysSinceLastFullSync >= cleanupFrequencyDays
                    ? VMProcessSyncType.COMPLETE
                    : VMProcessSyncType.DELTA;
        } catch (ParseException e) {
            log.warn("Failed to parse last full sync end date: " + e.getMessage());
            return VMProcessSyncType.COMPLETE;
        }
    }

    @Override
    public RequestSummary triggerReverseMatch() throws TException {
        Set<String> releaseIds = compHandler.getAllReleaseIds();
        if (releaseIds != null && !releaseIds.isEmpty()){
            for (String releaseId: releaseIds) {
                VMProcessHandler.findReleaseMatch(releaseId, true);
            }
        }
        log.info("Reverse match triggered for "+(releaseIds==null?0:releaseIds.size())+" releases. waiting for completion...");
        return new RequestSummary(RequestStatus.SUCCESS);
    }

    @Override
    public RequestSummary acceptMatch(User user, String matchId) throws TException {
        return setMatchState(user, matchId, VMMatchState.ACCEPTED);
    }

    @Override
    public RequestSummary declineMatch(User user, String matchId) throws TException {
        return setMatchState(user, matchId, VMMatchState.DECLINED);
    }

    private RequestSummary setMatchState(User user, String matchId, VMMatchState state){
        if (!PermissionUtils.isAdmin(user) || StringUtils.isEmpty(matchId)){
            return new RequestSummary(RequestStatus.FAILURE);
        }

        VMMatch match = dbHandler.getById(VMMatch.class, matchId);
        if (match == null){
            return new RequestSummary(RequestStatus.FAILURE);
        }
        match.setState(state);
        return new RequestSummary(dbHandler.update(match));
    }
}
