/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.common.CommonUtils;
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

import java.util.Properties;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import java.io.IOException;
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

        // Use pre-loaded delta offsets from SVMConstants to avoid cross-module dependency
        int deltaOffsetDays = SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS;
        int vulnDeltaOffsetDays = SVMConstants.VULN_DELTA_OFFSET_DAYS;

        // synchronize VMAction with delta sync
        String actionStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMAction.class.getSimpleName(), actionStart));
        synchronizeElementType(VMAction.class, SVMConstants.ACTIONS_URL, deltaOffsetDays);
        log.info("Storing and getting master data of "+VMAction.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMPriority with delta sync
        String prioStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMPriority.class.getSimpleName(), prioStart));
        synchronizeElementType(VMPriority.class, SVMConstants.PRIORITIES_URL, deltaOffsetDays);
        log.info("Storing and getting master data of "+VMPriority.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMComponent with delta sync
        String compStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMComponent.class.getSimpleName(), compStart));
        synchronizeElementType(VMComponent.class, SVMConstants.COMPONENTS_URL, deltaOffsetDays);
        log.info("Storing and getting master data of "+VMComponent.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize Vulnerability (bulk notifications) with delta sync
        String vulnStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(Vulnerability.class.getSimpleName(), vulnStart));
        synchronizeElementType(Vulnerability.class, SVMConstants.VULNERABILITIES_URL, vulnDeltaOffsetDays);
        log.info("Storing and getting master data of "+Vulnerability.class.getSimpleName()+" triggered. waiting for completion...");

        // triggerReporting
        VMProcessHandler.triggerReport(VMAction.class, actionStart);
        VMProcessHandler.triggerReport(VMPriority.class, prioStart);
        VMProcessHandler.triggerReport(VMComponent.class, compStart);
        VMProcessHandler.triggerReport(Vulnerability.class, vulnStart);

        return new RequestSummary(RequestStatus.SUCCESS);
    }

    private <T extends TBase> void synchronizeElementType(Class<T> elementType, String url, int deltaOffsetDays) {
        VMProcessReporting lastProcess = dbHandler.getLastSuccessfulProcessByElementType(elementType.getSimpleName());
        String modifiedAfter = null;
        String syncType = "full";
        
        if (lastProcess != null && lastProcess.isSetEndDate()) {
            modifiedAfter = SVMUtils.calculateModifiedAfter(lastProcess.getEndDate(), deltaOffsetDays);
            syncType = "delta(" + deltaOffsetDays + "d)";
        }
        
        log.info(String.format("SVM Sync [%s]: %s sync, last=%s, modified_after=%s", 
            elementType.getSimpleName(), syncType, 
            lastProcess != null ? lastProcess.getEndDate() : "none", 
            modifiedAfter != null ? modifiedAfter : "none"));
        
        VMProcessHandler.getElementIdsWithModifiedAfter(elementType, url, modifiedAfter, true);
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
