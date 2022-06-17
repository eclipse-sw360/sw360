/*
 * Copyright (c) Verifa Oy, 2018-2019. Part of the SW360 Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.rest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.eclipse.sw360.wsimport.domain.*;
import org.eclipse.sw360.wsimport.utility.WsTokenType;

import java.io.IOException;

import static org.eclipse.sw360.wsimport.utility.TranslationConstants.GET_PROJECT_VITALS;
import static org.eclipse.sw360.wsimport.utility.TranslationConstants.GET_PROJECT_LICENSES;
import static org.eclipse.sw360.wsimport.utility.TranslationConstants.GET_ORGANIZATION_PROJECT_VITALS;

/**
 * @author ksoranko@verifa.io
 */
public class WsImportService {

    private static final Logger LOGGER = LogManager.getLogger(WsImportService.class);
    private static final WsRestClient restClient = new WsRestClient();
    private static final Gson gson = new Gson();

    public WsProject getWsProject(String projectToken, TokenCredentials tokenCredentials) throws JsonSyntaxException {
        String projectVitalString;
        WsProjectVitalInformation projectVitalInformation = null;
        try {
            projectVitalString = restClient.getData(GET_PROJECT_VITALS, projectToken, WsTokenType.PROJECT, tokenCredentials);
        } catch (IOException | HttpException e) {
            LOGGER.error("Exception with " + GET_PROJECT_VITALS + " request to " + tokenCredentials.getServerUrl() + ", with exception: " + e);
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
            LOGGER.error("WsProjectVitalInformation is empty...");
            return null;
        }
    }

    public WsLibrary[] getProjectLicenses(String projectToken, TokenCredentials tokenCredentials) throws JsonSyntaxException {
        String projectLibsString = null;
        try {
            projectLibsString = restClient.getData(GET_PROJECT_LICENSES, projectToken, WsTokenType.PROJECT, tokenCredentials);
        } catch (IOException | HttpException e) {
            LOGGER.error("Exception with " + GET_PROJECT_VITALS + " request to " + tokenCredentials.getServerUrl() + ", with exception: " + e);
            return null;
        }
        WsProjectLibs wsProjectLibs = gson.fromJson(projectLibsString, WsProjectLibs.class);
        if (wsProjectLibs != null) {
            return wsProjectLibs.getLibraries();
        } else {
            LOGGER.error("WsProjectLibs is empty...");
            return null;
        }

    }

}
