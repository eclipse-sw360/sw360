/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.datatables.DataTablesParser;
import org.eclipse.sw360.portal.common.datatables.data.DataTablesParameters;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.thrift.projects.projectsConstants.CLEARING_TEAM_UNKNOWN;
import static org.eclipse.sw360.portal.common.PortalConstants.CLEARING_TEAM;
import static org.eclipse.sw360.portal.common.PortalConstants.PROJECT_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE_ID;

/**
 * Fossology aware portlet implementation
 *
 * @author Daniele.Fognini@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public abstract class FossologyAwarePortlet extends LinkedReleasesAndProjectsAwarePortlet {

    private static final Logger log = Logger.getLogger(FossologyAwarePortlet.class);

    public FossologyAwarePortlet() {

    }

    public FossologyAwarePortlet(ThriftClients clients) {
        super(clients);
    }

    @Override
    protected void dealWithGenericAction(ResourceRequest request, ResourceResponse response, String action)
            throws IOException, PortletException {

        if (super.isGenericAction(action)) {
            super.dealWithGenericAction(request, response, action);
        } else if (isFossologyAwareAction(action)) {
            dealWithFossologyAction(request, response, action);
        }
    }

    @Override
    protected boolean isGenericAction(String action) {
        return super.isGenericAction(action) || isFossologyAwareAction(action);
    }

    protected boolean isFossologyAwareAction(String action) {
        return action.startsWith(PortalConstants.FOSSOLOGY_PREFIX);
    }

    protected abstract void dealWithFossologyAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException;

    protected void serveSendToFossology(ResourceRequest request, ResourceResponse response) throws PortletException {
        final RequestStatus requestStatus = sendToFossology(request);
        renderRequestStatus(request, response, requestStatus);
    }

    private RequestStatus sendToFossology(ResourceRequest request) {
        final String releaseId = request.getParameter(RELEASE_ID);
        final String clearingTeam = request.getParameter(CLEARING_TEAM);

        try {
            FossologyService.Iface client = thriftClients.makeFossologyClient();
            return client.sendToFossology(releaseId, UserCacheHolder.getUserFromRequest(request), clearingTeam);
        } catch (TException e) {
            log.error("Could not send release to fossology", e);
        }
        return RequestStatus.FAILURE;
    }

    protected void serveFossologyStatus(ResourceRequest request, ResourceResponse response) throws IOException {
        DataTablesParameters parameters = DataTablesParser.parametersFrom(request);

        Release release = getReleaseForFossologyStatus(request);

        Map<String, String> fossologyStatus = getFossologyStatus(release);

        JSONObject jsonResponse = JSONFactoryUtil.createJSONObject();

        JSONArray data = JSONFactoryUtil.createJSONArray();

        for (Map.Entry<String, String> entry : fossologyStatus.entrySet()) {
            JSONObject row = JSONFactoryUtil.createJSONObject();
            row.put("0", entry.getKey());
            row.put("1", entry.getValue());
            data.put(row);
        }

        jsonResponse.put("attachment", getFossologyUploadableAttachment(release));

        jsonResponse.put("data", data);
        jsonResponse.put("draw", parameters.getDraw());
        jsonResponse.put("recordsTotal", fossologyStatus.size());
        jsonResponse.put("recordsFiltered", fossologyStatus.size());

        writeJSON(request, response, jsonResponse);
    }

    private Release getReleaseForFossologyStatus(ResourceRequest request) {
        String releaseId = request.getParameter(RELEASE_ID);
        String clearingTeam = request.getParameter(CLEARING_TEAM);

        boolean cached = Boolean.parseBoolean(request.getParameter("cached"));

        if (!isNullOrEmpty(releaseId) && !isNullOrEmpty(clearingTeam)) {
            try {
                final Release release;
                if (!cached) {
                    FossologyService.Iface client = thriftClients.makeFossologyClient();
                    release = client.getStatusInFossology(releaseId, UserCacheHolder.getUserFromRequest(request), clearingTeam);
                } else {
                    ComponentService.Iface client = thriftClients.makeComponentClient();
                    release = client.getReleaseById(releaseId, UserCacheHolder.getUserFromRequest(request));
                }
                if (release != null) {
                    return release;
                } else {
                    log.error("no response from backend!");
                }
            } catch (TException e) {
                log.error("Could not release status in fossology", e);
            }
        }

        log.error("Could not get release from request");
        return null;
    }

    private Map<String, String> getFossologyStatus(Release release) {
        if (release != null) {
            Set<ExternalToolRequest> fossologyRequests = SW360Utils.getExternalToolRequestsForTool(release, ExternalTool.FOSSOLOGY);
            return fossologyRequests.stream().collect(Collectors.toMap(etr -> {
                return etr.getToolUserGroup();
            }, etr -> {
                ExternalToolWorkflowStatus externalToolWorkflowStatus = etr.getExternalToolWorkflowStatus();
                switch (externalToolWorkflowStatus) {
                case NOT_SENT:
                case UPLOADING:
                case ACCESS_DENIED:
                case NOT_FOUND:
                case CONNECTION_TIMEOUT:
                case CONNECTION_FAILED:
                case SERVER_ERROR:
                    return ThriftEnumUtils.enumToString(externalToolWorkflowStatus);
                case SENT:
                    ExternalToolStatus externalToolStatus = etr.getExternalToolStatus();
                    return ThriftEnumUtils.enumToString(externalToolStatus);
                default:
                    return "";
                }
            }));
        } else {
            log.error("no response from backend!");
        }

        return Collections.emptyMap();
    }

    private String getFossologyUploadableAttachment(Release release) {
        String sourceAttachment = null;
        if (release != null) {
            try {
                ComponentService.Iface fossologyClient = thriftClients.makeComponentClient();
                final Set<Attachment> sourceAttachments = fossologyClient.getSourceAttachments(release.getId());
                if (sourceAttachments.size() == 1) {
                    sourceAttachment = CommonUtils.getFirst(sourceAttachments).getFilename();
                }
            } catch (TException e) {
                log.error("cannot get name of the attachment of release", e);
            }
        }

        if (isNullOrEmpty(sourceAttachment)) {
            return "no unique source attachment found!";
        } else {
            return sourceAttachment;
        }
    }

    protected void serveGetSendableReleases(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        String projectId = request.getParameter(PROJECT_ID);

        User user = UserCacheHolder.getUserFromRequest(request);
        try {
            putReleasesAndProjectIntoRequest(request, projectId, user);
            include("/html/projects/ajax/sendableTable.jsp", request, response, PortletRequest.RESOURCE_PHASE);

        } catch (TException e) {
            log.error("Problem with project client", e);
            throw new PortletException(e);
        }
    }

    protected void putReleasesAndProjectIntoRequest(PortletRequest request, String projectId, User user) throws TException {
        ProjectService.Iface client = thriftClients.makeProjectClient();
        List<ReleaseClearingStatusData> releaseClearingStatuses = client.getReleaseClearingStatuses(projectId, user);
        request.setAttribute(PortalConstants.RELEASES_AND_PROJECTS, releaseClearingStatuses);
    }


    protected void serveProjectSendToFossology(ResourceRequest request, ResourceResponse response) {

        String projectId = request.getParameter(PROJECT_ID);
        String[] releaseIdArray = request.getParameterValues(RELEASE_ID);
        if (projectId == null || releaseIdArray == null) {
            renderRequestStatus(request, response, RequestStatus.FAILURE);
            return;
        }

        String clearingTeam = request.getParameter(CLEARING_TEAM);

        if (isNullOrEmpty(clearingTeam)) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                ProjectService.Iface client = thriftClients.makeProjectClient();
                Project project = client.getProjectById(projectId, user);
                clearingTeam = project.getClearingTeam();
            } catch (TException e) {
                renderRequestStatus(request, response, RequestStatus.FAILURE);
                log.error("Problem with project client", e);
            }
        }

        if ( ! isNullOrEmpty(clearingTeam) && ! CLEARING_TEAM_UNKNOWN.equals(clearingTeam)) {
            List<String> releaseIds = Arrays.asList(releaseIdArray);
            try {
                FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();

                renderRequestStatus(request, response, fossologyClient.sendReleasesToFossology(releaseIds, UserCacheHolder.getUserFromRequest(request), clearingTeam));

            } catch (TException e) {
                renderRequestStatus(request, response, RequestStatus.FAILURE);
                log.error("Problem with fossology client", e);
            }
        } else {
            renderRequestStatus(request, response, RequestStatus.FAILURE);
            log.error("Cannot decide on a clearing team for project " + projectId);
        }
    }
}
