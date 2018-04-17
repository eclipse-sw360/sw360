/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.config.FossologySettings;
import org.eclipse.sw360.fossology.handler.FossologyFileHandler;
import org.eclipse.sw360.fossology.handler.FossologyHostKeyHandler;
import org.eclipse.sw360.fossology.handler.FossologyScriptsHandler;
import org.eclipse.sw360.fossology.ssh.FossologySshConnector;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author daniele.fognini@tngtech.com
 * @author johannes.najjar@tngtech.com
 */
@Component
public class FossologyHandler implements FossologyService.Iface {
    private final FossologyFileHandler fossologyFileHandler;
    private final FossologyHostKeyHandler fossologyHostKeyHandler;
    private final FossologySshConnector fossologySshConnector;
    private final FossologyScriptsHandler fossologyScriptsHandler;
    private final byte[] fossologyPubKey;

    @Autowired
    public FossologyHandler(FossologyFileHandler fossologyFileHandler, FossologyHostKeyHandler fossologyHostKeyHandler, FossologySshConnector fossologySshConnector, FossologyScriptsHandler fossologyScriptsHandler, FossologySettings fossologySettings) {
        this.fossologyFileHandler = fossologyFileHandler;
        this.fossologyHostKeyHandler = fossologyHostKeyHandler;
        this.fossologySshConnector = fossologySshConnector;
        this.fossologyScriptsHandler = fossologyScriptsHandler;
        this.fossologyPubKey = fossologySettings.getFossologyPublicKey();
    }

    @Override
    public RequestStatus sendToFossology(String releaseId, User user, String clearingTeam) throws TException {
        return fossologyFileHandler.sendToFossology(releaseId, user, clearingTeam);
    }

    @Override
    public RequestStatus sendReleasesToFossology(List<String> releaseIds, User user, String clearingTeam) throws TException {
        for (String releaseId : releaseIds) {
            RequestStatus requestStatus = sendToFossology(releaseId, user, clearingTeam);
            if (requestStatus != RequestStatus.SUCCESS) return requestStatus;
        }
        return RequestStatus.SUCCESS;
    }

    @Override
    public Release getStatusInFossology(String releaseId, User user, String clearingTeam) throws TException {
        return fossologyFileHandler.getStatusInFossology(releaseId, user, clearingTeam);
    }

    @Override
    public List<FossologyHostFingerPrint> getFingerPrints() throws TException {
        return fossologyHostKeyHandler.getFingerPrints();
    }

    @Override
    public RequestStatus setFingerPrints(List<FossologyHostFingerPrint> fingerPrints) throws TException {
        return fossologyHostKeyHandler.setFingerPrints(fingerPrints);
    }

    @Override
    public RequestStatus deployScripts() throws TException {
        return fossologyScriptsHandler.deployScripts();
    }

    @Override
    public RequestStatus checkConnection() throws TException {
        return fossologySshConnector.runInFossologyViaSsh("exit 2") == 2 ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
    }

    @Override
    public String getPublicKey() throws TException {
        return new String(fossologyPubKey);
    }
}
