/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage;

import java.io.IOException;
import java.util.List;

import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;

public abstract class AbstractTasksPortlet extends Sw360Portlet {

    protected void sendModerations(ResourceRequest request, ResourceResponse response, List<ModerationRequest> moderations) throws IOException, PortletException {
        JSONArray jsonModerations = getModerationData(moderations);
        JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
        jsonResult.put("aaData", jsonModerations);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem generating task list", e);
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
