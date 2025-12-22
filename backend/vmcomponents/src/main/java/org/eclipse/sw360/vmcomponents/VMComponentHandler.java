/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.log4j.Logger.getLogger;

/**
 * Implementation of the Thrift service
 *
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
@Component
public class VMComponentHandler implements VMComponentService.Iface {

    private static final Logger log = getLogger(VMComponentHandler.class);

    @Autowired
    private VMDatabaseHandler dbHandler;
    @Autowired
    private ComponentDatabaseHandler compHandler;

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
        String actionStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMAction.class.getSimpleName(), actionStart));
        VMProcessHandler.getElementIds(VMAction.class, SVMConstants.ACTIONS_URL, true);
        log.info("Storing and getting master data of "+VMAction.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMPriority
        String prioStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMPriority.class.getSimpleName(), prioStart));
        VMProcessHandler.getElementIds(VMPriority.class, SVMConstants.PRIORITIES_URL, true);
        log.info("Storing and getting master data of "+VMPriority.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMComponent
        String compStart = SW360Utils.getCreatedOnTime();
        dbHandler.add(new VMProcessReporting(VMComponent.class.getSimpleName(), compStart));
        VMProcessHandler.getElementIds(VMComponent.class, SVMConstants.COMPONENTS_URL, true);
        log.info("Storing and getting master data of "+VMComponent.class.getSimpleName()+" triggered. waiting for completion...");

        // triggerReporting
        VMProcessHandler.triggerReport(VMAction.class, actionStart);
        VMProcessHandler.triggerReport(VMPriority.class, prioStart);
        VMProcessHandler.triggerReport(VMComponent.class, compStart);

        return new RequestSummary(RequestStatus.SUCCESS);
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
