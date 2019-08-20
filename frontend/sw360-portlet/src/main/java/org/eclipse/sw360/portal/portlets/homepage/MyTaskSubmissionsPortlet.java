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
package org.eclipse.sw360.portal.portlets.homepage;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.portal.common.PortalConstants.MY_TASK_SUBMISSIONS_PORTLET_NAME;

import java.io.IOException;
import java.util.List;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.google.common.collect.Lists;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.moderation.ModerationPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + MY_TASK_SUBMISSIONS_PORTLET_NAME,
        "javax.portlet.display-name=My Task Submissions",
        "javax.portlet.info.short-title=My Task Submissions",
        "javax.portlet.info.title=My Task Submissions",

        "javax.portlet.init-param.view-template=/html/homepage/mytasksubmissions/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MyTaskSubmissionsPortlet extends Sw360Portlet {
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.DELETE_MODERATION_REQUEST.equals(action)) {
            serveDeleteModerationRequest(request, response);
        } else if (PortalConstants.LOAD_TASK_SUBMISSION_LIST.equals(action)) {
            serveTaskList(request, response);
        }
    }

    private void serveDeleteModerationRequest(ResourceRequest request, ResourceResponse response) throws IOException {
        RequestStatus requestStatus = ModerationPortletUtils.deleteModerationRequest(request, log);
        serveRequestStatus(request, response, requestStatus, "Problem removing moderation request", log);
    }

    private void serveTaskList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        List<ModerationRequest> moderations = Lists.newArrayList();

        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            moderations = thriftClients.makeModerationClient().getRequestsByRequestingUser(user);
        } catch (TException e) {
            log.error("Could not fetch your moderations from backend", e);
        }

        JSONArray jsonModerations = getModerationData(moderations);
        JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
        jsonResult.put("aaData", jsonModerations);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem generating task submission list", e);
        }
    }

    public JSONArray getModerationData(List<ModerationRequest> moderationList) {
        JSONArray projectData = JSONFactoryUtil.createJSONArray();
        for(ModerationRequest moderationRequest : moderationList) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

            jsonObject.put("DT_RowId", moderationRequest.getId());
            jsonObject.put("id", moderationRequest.getId());
            jsonObject.put("name", moderationRequest.getDocumentName());
            jsonObject.put("state", moderationState(moderationRequest.getModerationState()));

            projectData.put(jsonObject);
        }

        return projectData;
    }

    private String moderationState(ModerationState moderationState) {
        return "<span class='" + PortalConstants.TOOLTIP_CLASS__CSS + " "
            + PortalConstants.TOOLTIP_CLASS__CSS + "-" + moderationState.getClass().getSimpleName() + "-" + moderationState.toString() + "'>"
            + ThriftEnumUtils.enumToString(moderationState) + "</span>";
    }
}
