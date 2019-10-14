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

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.common.FossologyUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    protected void serveFossologyOutdated(ResourceRequest request, ResourceResponse response) {
        FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();

        String releaseId = request.getParameter(RELEASE_ID);
        RequestStatus result;
        String errorMessage;
        try {
            result = fossologyClient.markFossologyProcessOutdated(releaseId,
                    UserCacheHolder.getUserFromRequest(request));
            errorMessage = "No exception thrown from backend, but setting the FOSSology process of release with id "
                    + releaseId + " to outdated did result in state FAILURE.";
        } catch (TException e) {
            result = RequestStatus.FAILURE;
            errorMessage = "Could not mark FOSSology process of release with id " + releaseId
                    + " as outdated because of: " + e;
        }

        serveRequestStatus(request, response, result, errorMessage, log);
    }

    protected void serveFossologyStatus(ResourceRequest request, ResourceResponse response) {
        Iface componentClient = thriftClients.makeComponentClient();

        String releaseId = request.getParameter(RELEASE_ID);
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        try {
            Release release = componentClient.getReleaseById(releaseId, UserCacheHolder.getUserFromRequest(request));
            Set<ExternalToolProcess> fossologyProcesses = SW360Utils.getNotOutdatedExternalToolProcessesForTool(release,
                    ExternalTool.FOSSOLOGY);
            fillJsonObjectFromFossologyProcess(jsonObject, fossologyProcesses);

            Set<Attachment> sourceAttachments = componentClient.getSourceAttachments(releaseId);
            jsonObject.put("sourceAttachments", sourceAttachments.size());
            if (sourceAttachments.size() == 1) {
                jsonObject.put("sourceAttachmentName", sourceAttachments.iterator().next().getFilename());
            }
        } catch (TException e) {
            jsonObject.put("error", "Could not determine FOSSology state for this release!");
        }

        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    protected void serveFossologyProcess(ResourceRequest request, ResourceResponse response) {
        FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();

        String releaseId = request.getParameter(RELEASE_ID);
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        try {
            ExternalToolProcess fossologyProcess = fossologyClient.process(releaseId,
                    UserCacheHolder.getUserFromRequest(request));
            fillJsonObjectFromFossologyProcess(jsonObject, Stream.of(fossologyProcess).collect(Collectors.toSet()));
        } catch (TException e) {
            jsonObject.put("error", "Could not determine FOSSology state for this release!");
        }

        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    private void fillJsonObjectFromFossologyProcess(JSONObject jsonObject,
            Set<ExternalToolProcess> fossologyProcesses) {
        if (fossologyProcesses.size() == 0) {
            jsonObject.put("stepName", FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD);
            jsonObject.put("stepStatus", ExternalToolProcessStatus.NEW.toString());
        } else if (fossologyProcesses.size() == 1) {
            ExternalToolProcess fossologyProcess = fossologyProcesses.iterator().next();
            FossologyUtils.ensureOrderOfProcessSteps(fossologyProcess);
            ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                    .get(fossologyProcess.getProcessStepsSize() - 1);
            jsonObject.put("stepName", furthestStep.getStepName());
            jsonObject.put("stepStatus", furthestStep.getStepStatus().toString());
        } else {
            jsonObject.put("error", "More than one FOSSology process found for this release!");
        }
    }


    // USED but not fossology related

    protected void putReleasesAndProjectIntoRequest(PortletRequest request, String projectId, User user)
            throws TException {
        ProjectService.Iface client = thriftClients.makeProjectClient();
        List<ReleaseClearingStatusData> releaseClearingStatuses = client.getReleaseClearingStatuses(projectId, user);
        request.setAttribute(PortalConstants.RELEASES_AND_PROJECTS, releaseClearingStatuses);
    }

}
