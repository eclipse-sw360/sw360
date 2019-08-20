/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projectimport;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.portlet.*;

import static com.google.common.base.Strings.nullToEmpty;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + PortalConstants.PROJECT_WSIMPORT_PORTLET_NAME,

        "javax.portlet.display-name=WhiteSource Import",
        "javax.portlet.info.short-title=WhiteSource Import",
        "javax.portlet.info.title=WhiteSource Import",

        "javax.portlet.init-param.view-template=/html/projects/wsimport.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class WsImportPortlet extends Sw360Portlet {
    private static final Logger LOGGER = Logger.getLogger(WsImportPortlet.class);
    private static ProjectImportService.Iface projectImportClient = new ThriftClients().makeWsImportClient();

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        super.doView(request, response);
    }

    private TokenCredentials getTokenCredentialsFromSession(PortletSession session) {
        String token = (String) session.getAttribute(ProjectImportConstants.TOKEN);
        String server = (String) session.getAttribute(ProjectImportConstants.SERVER_URL);
        String userKey = (String) session.getAttribute(ProjectImportConstants.USER_KEY);
        return new TokenCredentials()
                .setToken(token)
                .setServerUrl(server)
                .setUserKey(userKey);
    }

    private void putTokenCredentialsIntoSession(PortletSession session, TokenCredentials tokenCredentials) {
        session.setAttribute(ProjectImportConstants.TOKEN, nullToEmpty(tokenCredentials.getToken()));
        session.setAttribute(ProjectImportConstants.SERVER_URL, nullToEmpty(tokenCredentials.getServerUrl()));
        session.setAttribute(ProjectImportConstants.USER_KEY, nullToEmpty(tokenCredentials.getUserKey()));
    }

    private List<String> getProjectTokensForImport(PortletRequest request) throws IOException {
        String[] checked = request.getParameterValues("checked[]");
        List<String> checkedTokens = Arrays.asList(checked);

        return checkedTokens;
    }

    private boolean isImportSuccessful(ImportStatus importStatus) {
        return (importStatus.isSetRequestStatus() && importStatus.getRequestStatus().equals(RequestStatus.SUCCESS) && importStatus.getFailedIds().isEmpty());
    }

    private ImportStatus importDatasources(List<String> toImport, User user, TokenCredentials tokenCredentials) {
        ImportStatus importStatus = new ImportStatus();
        try {
            importStatus = projectImportClient.importData(toImport, user, tokenCredentials);
            if (!isImportSuccessful(importStatus)) {
                if (importStatus.getRequestStatus().equals(RequestStatus.FAILURE)) {
                    LOGGER.error("Importing of data sources failed.");
                } else {
                    LOGGER.error("Importing has not succeeded for the following IDs: " + importStatus.getFailedIds().toString());
                }
            }
        } catch (TException e) {
            LOGGER.error("ImportDatasources failed", e);
        }
        return importStatus;
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String requestedAction = request.getParameter(ProjectImportConstants.USER_ACTION__IMPORT);
        JSONObject responseData = handleRequestedAjaxAction(requestedAction, request);
        PrintWriter writer = response.getWriter();
        writer.write(responseData.toString());
    }

    public JSONObject handleRequestedAjaxAction(String requestedAction, ResourceRequest request) throws IOException, PortletException {
        PortletSession session = request.getPortletSession();
        TokenCredentials tokenCredentials = getTokenCredentialsFromSession(session);
        JSONObject responseData = JSONFactoryUtil.createJSONObject();

        switch (requestedAction) {
            case ProjectImportConstants.USER_ACTION__IMPORT_DATA:
                User user = UserCacheHolder.getUserFromRequest(request);
                List<String> selectedTokens = getProjectTokensForImport(request);
                importWsProjects(user, selectedTokens, responseData, tokenCredentials);
                break;
            case ProjectImportConstants.USER_ACTION__NEW_IMPORT_SOURCE:
                TokenCredentials newCredentials = new TokenCredentials()
                        .setToken(request.getParameter(ProjectImportConstants.TOKEN))
                        .setServerUrl(request.getParameter(ProjectImportConstants.SERVER_URL))
                        .setUserKey(request.getParameter(ProjectImportConstants.USER_KEY));
                setNewImportSource(newCredentials, session, responseData);
                break;
            default:
                break;
        }
        return responseData;
    }

    private void importWsProjects(User user, List<String> selectedIds, JSONObject responseData, TokenCredentials tokenCredentials) throws PortletException, IOException {
        ImportStatus importStatus = importDatasources(selectedIds, user, tokenCredentials);
        JSONObject jsonFailedIds = JSONFactoryUtil.createJSONObject();
        JSONArray jsonSuccessfulIds = JSONFactoryUtil.createJSONArray();

        if (importStatus.isSetRequestStatus() && importStatus.getRequestStatus().equals(RequestStatus.SUCCESS)) {
            importStatus.getFailedIds().forEach(jsonFailedIds::put);
            importStatus.getSuccessfulIds().forEach(jsonSuccessfulIds::put);

            responseData.put(ProjectImportConstants.RESPONSE__FAILED_IDS, jsonFailedIds);
            responseData.put(ProjectImportConstants.RESPONSE__SUCCESSFUL_IDS, jsonSuccessfulIds);
        }
        if (isImportSuccessful(importStatus)) {
            responseData.put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__SUCCESS);
        } else if (importStatus.isSetRequestStatus() && importStatus.getRequestStatus().equals(RequestStatus.SUCCESS)) {
            responseData.put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__FAILURE);
        } else {
            responseData.put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__GENERAL_FAILURE);
        }
    }

    void setNewImportSource(TokenCredentials newCredentials, PortletSession session, JSONObject responseData) {
        String serverUrl = nullToEmpty(newCredentials.getServerUrl());

        if (serverUrl.isEmpty()) {
            responseData.put(ProjectImportConstants.RESPONSE__STATUS,
                    ProjectImportConstants.RESPONSE__DB_URL_NOT_SET);
        } else {
            putTokenCredentialsIntoSession(session, newCredentials);
            responseData.put(ProjectImportConstants.RESPONSE__STATUS,
                    ProjectImportConstants.RESPONSE__DB_CHANGED);
            responseData.put(ProjectImportConstants.RESPONSE__DB_URL, serverUrl);
        }
    }

}
