/*
 * Copyright (c) Bosch Software Innovations GmbH 2015.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + PortalConstants.PROJECT_BDPIMPORT_PORTLET_NAME,

        "javax.portlet.display-name=BDP Import",
        "javax.portlet.info.short-title=BDP Import",
        "javax.portlet.info.title=BDP Import",

        "javax.portlet.init-param.view-template=/html/projects/import.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BdpImportPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(BdpImportPortlet.class);
    private static ProjectImportService.Iface projectImportClient = new ThriftClients().makeProjectImportClient();

    @interface BdpImportConfig {}

    static class LoginState {
        private Boolean loggedIn;
        private String loggedInServerUrl;

        public LoginState() {
            loggedIn = false;
            loggedInServerUrl = "";
        }

        public void login(String serverUrl) {
            loggedIn = true;
            loggedInServerUrl = serverUrl;
        }

        public void logout() {
            loggedIn = false;
            loggedInServerUrl = "";
        }

        public String getServerUrl() {
            return loggedInServerUrl;
        }

        boolean isLoggedIn() {
            return loggedIn;
        }

    }

    private RemoteCredentials getRemoteCredentialsFromSession(PortletSession session) {
        String dbUserName = (String) session.getAttribute(ProjectImportConstants.USERNAME);
        String dbUserPass = (String) session.getAttribute(ProjectImportConstants.PASSWORD);
        String dbUrl = (String) session.getAttribute(ProjectImportConstants.SERVER_URL);

        return new RemoteCredentials()
                .setPassword(dbUserPass)
                .setUsername(dbUserName)
                .setServerUrl(dbUrl);
    }

    private void putRemoteCredentialsIntoSession(PortletSession session, RemoteCredentials remoteCredentials) {
        session.setAttribute(ProjectImportConstants.USERNAME, nullToEmpty(remoteCredentials.getUsername()));
        session.setAttribute(ProjectImportConstants.PASSWORD, nullToEmpty(remoteCredentials.getPassword()));
        session.setAttribute(ProjectImportConstants.SERVER_URL, nullToEmpty(remoteCredentials.getServerUrl()));
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<Project> importables = new ArrayList<>();
        Boolean loggedIn = false;
        String loggedInServer = "";

        RemoteCredentials reCred = getRemoteCredentialsFromSession(request.getPortletSession());
        String projectName = request.getParameter(ProjectImportConstants.PROJECT_NAME);
        if (!nullToEmpty(reCred.getServerUrl()).isEmpty()) {
            importables = loadImportables(reCred, projectName);
            loggedIn = true;
            loggedInServer = reCred.getServerUrl();
        }
        String idName = getIdName();

        request.setAttribute("idName", idName);
        request.setAttribute("importables", importables);
        request.setAttribute("loggedIn", loggedIn);
        request.setAttribute("loggedInServer", loggedInServer);

        super.doView(request, response);
    }

    private List<String> getProjectIdsForImport(PortletRequest request) throws PortletException, IOException {
        String[] checked = request.getParameterValues("checked[]");
        List<String> checkedIds = new ArrayList<>();
        if (checked != null) {
            for (String s : checked) {
                checkedIds.add(s.substring(ProjectImportConstants.CHECKED_PROJECT.length()));
            }
        }
        return checkedIds;
    }

    private boolean isImportSuccessful(ImportStatus importStatus) {
        return (importStatus.isSetRequestStatus() && importStatus.getRequestStatus().equals(RequestStatus.SUCCESS) && importStatus.getFailedIds().isEmpty());
    }

    private ImportStatus importDatasources(List<String> toImport, User user, RemoteCredentials remoteCredentials) {
        ImportStatus importStatus = new ImportStatus();
        try {
            importStatus = projectImportClient.importDatasources(toImport, user, remoteCredentials);
            if (!isImportSuccessful(importStatus)) {
                if (importStatus.getRequestStatus().equals(RequestStatus.FAILURE)) {
                    log.error("Importing of data sources failed.");
                } else {
                    log.error("Importing has not succeeded for the following IDs: " + importStatus.getFailedIds().toString());
                }
            }
        } catch (TException e) {
            log.error("ImportDatasources: Exception ".concat(e.getMessage()));
        }
        return importStatus;
    }

    private List<Project> loadImportables(RemoteCredentials remoteCredentials, String projectName) {
        try {
            log.info("Looking for importables with prefix " + projectName);
            return projectImportClient.suggestImportables(remoteCredentials, Strings.nullToEmpty(projectName));
        } catch (TException e) {
            log.error("Thrift failed, (uncaught TException)", e);
            return ImmutableList.of();
        }
    }

    private String getIdName() {
        try {
            return projectImportClient.getIdName();
        } catch (TException e) {
            log.error("Thrift failed, (uncaught TException)", e);
            return "";
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        LoginState loginState = new LoginState();
        String requestedAction = request.getParameter(ProjectImportConstants.USER_ACTION__IMPORT);
        JSONObject responseData = handleRequestedAjaxAction(requestedAction, request, response, loginState);

        PrintWriter writer = response.getWriter();
        writer.write(responseData.toString());
    }

    public JSONObject handleRequestedAjaxAction(String requestedAction, ResourceRequest request, ResourceResponse response, LoginState loginState) throws IOException, PortletException {
        PortletSession session = request.getPortletSession();
        RemoteCredentials remoteCredentials = getRemoteCredentialsFromSession(session);
        JSONObject responseData = JSONFactoryUtil.createJSONObject();

        switch (requestedAction) {
            case ProjectImportConstants.USER_ACTION__IMPORT_DATA:
                User user = UserCacheHolder.getUserFromRequest(request);
                List<String> selectedIds = getProjectIdsForImport(request);
                importBdpProjects(user, selectedIds, responseData, remoteCredentials);
                break;
            case ProjectImportConstants.USER_ACTION__NEW_IMPORT_SOURCE:
                RemoteCredentials newCredentials = new RemoteCredentials()
                        .setUsername(request.getParameter(ProjectImportConstants.USERNAME))
                        .setPassword(request.getParameter(ProjectImportConstants.PASSWORD))
                        .setServerUrl(request.getParameter(ProjectImportConstants.SERVER_URL));
                if (!validateCredentials(newCredentials)) {
                    responseData.put(ProjectImportConstants.RESPONSE__STATUS,
                            ProjectImportConstants.RESPONSE__UNAUTHORIZED);
                } else {
                    setNewImportSource(newCredentials, session, responseData, loginState);
                }
                break;
            case ProjectImportConstants.USER_ACTION__UPDATE_IMPORTABLES:
                String projectName = request.getParameter(ProjectImportConstants.PROJECT_NAME);
                updateImportables(responseData, loginState, remoteCredentials, projectName);
                break;
            case ProjectImportConstants.USER_ACTION__DISCONNECT:
                putRemoteCredentialsIntoSession(session, new RemoteCredentials());
                loginState.logout();
                break;
            default:
                loginState.logout();
                break;
        }
        return responseData;
    }

    private boolean validateCredentials(RemoteCredentials credentials) {
        try {
            return projectImportClient.validateCredentials(credentials);
        } catch (TException e) {
            log.error("Thrift failed, (uncaught TException)", e);
            return false;
        }
    }

    private void importBdpProjects(User user, List<String> selectedIds, JSONObject responseData, RemoteCredentials remoteCredentials) throws PortletException, IOException {
        ImportStatus importStatus = importDatasources(selectedIds, user, remoteCredentials);
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

    void setNewImportSource(RemoteCredentials newCredentials, PortletSession session, JSONObject responseData, LoginState loginState) {

        String serverUrl = nullToEmpty(newCredentials.getServerUrl());

        if (serverUrl.isEmpty()) {
            loginState.logout();
            responseData.put(ProjectImportConstants.RESPONSE__STATUS,
                    ProjectImportConstants.RESPONSE__DB_URL_NOT_SET);
        } else {
            putRemoteCredentialsIntoSession(session, newCredentials);
            responseData.put(ProjectImportConstants.RESPONSE__STATUS,
                    ProjectImportConstants.RESPONSE__DB_CHANGED);
            responseData.put(ProjectImportConstants.RESPONSE__DB_URL, serverUrl);
            loginState.login(serverUrl);
        }
    }

    private void updateImportables(JSONObject responseData, LoginState loginState, RemoteCredentials remoteCredentials, String projectName) throws JsonProcessingException {
        if (!nullToEmpty(remoteCredentials.getServerUrl()).isEmpty()) {
            List<Project> importables = loadImportables(remoteCredentials, projectName);

            JSONArray serializedProjects = JSONFactoryUtil.createJSONArray();
            for (Project p : importables) {
                JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
                if (p.isSetExternalIds() && !isNullOrEmpty(p.getExternalIds().get(getIdName())))
                    jsonObject.put("externalId", p.getExternalIds().get(getIdName()));
                jsonObject.put("name", p.getName());
                serializedProjects.put(jsonObject.toString());
            }
            responseData.put(ProjectImportConstants.RESPONSE__NEW_IMPORTABLES, serializedProjects);
        }
        responseData.put(ProjectImportConstants.RESPONSE__DB_URL, remoteCredentials.getServerUrl());
        loginState.login(remoteCredentials.getServerUrl());
    }
}
