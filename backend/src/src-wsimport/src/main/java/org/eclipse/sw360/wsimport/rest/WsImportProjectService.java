/*
 * Copyright (c) Verifa Oy, 2018. Part of the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.rest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.eclipse.sw360.wsimport.domain.*;
import org.eclipse.sw360.wsimport.utility.WsTokenType;

import java.io.IOException;

/**
 * @author ksoranko@verifa.io
 */
public class WsImportProjectService {

    private static final Logger LOGGER = Logger.getLogger(WsImportProjectService.class);
    private static final WsRestClient restClient = new WsRestClient();
    private static final Gson gson = new Gson();

    public WsProject getWsProject(String projectToken, TokenCredentials tokenCredentials) throws JsonSyntaxException {
        String projectVitalString;
        WsProjectVitalInformation projectVitalInformation = null;
        try {
            projectVitalString = restClient.getData("getProjectVitals", projectToken, WsTokenType.PROJECT, tokenCredentials);
        } catch (IOException ioe) {
            LOGGER.error(ioe);
            return null;
        }
        WsProjectVitals wsProjectVitals = gson.fromJson(projectVitalString, WsProjectVitals.class);
        if (wsProjectVitals != null) {
            if (wsProjectVitals.getProjectVitals() != null) {
                projectVitalInformation = wsProjectVitals.getProjectVitals()[0];
            }
        }
        if (projectVitalInformation != null) {
            return new WsProject(projectVitalInformation.getId(),
                    projectVitalInformation.getName(),
                    projectVitalInformation.getToken(),
                    projectVitalInformation.getCreationDate());
        } else {
            return null;
        }
    }

    public WsLibrary[] getProjectLicenses(String projectToken, TokenCredentials tokenCredentials) throws JsonSyntaxException {
        String projectLibsString = null;
        try {
            projectLibsString = restClient.getData("getProjectLicenses", projectToken, WsTokenType.PROJECT, tokenCredentials);
        } catch (IOException ioe) {
            LOGGER.error(ioe);
            return null;
        }
        WsProjectLibs wsProjectLibs = gson.fromJson(projectLibsString, WsProjectLibs.class);
        if (wsProjectLibs != null) {
            return wsProjectLibs.getLibraries();
        } else {
            return null;
        }

    }

}
