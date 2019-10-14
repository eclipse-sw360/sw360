/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.sw360.portal.common.PortalConstants.MY_PROJECTS_PORTLET_NAME;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + MY_PROJECTS_PORTLET_NAME,

        "javax.portlet.display-name=My Projects",
        "javax.portlet.info.short-title=My Projects",
        "javax.portlet.info.title=My Projects",

        "javax.portlet.init-param.view-template=/html/homepage/myprojects/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MyProjectsPortlet extends Sw360Portlet {
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.LOAD_PROJECT_LIST.equals(action)) {
            serveProjectList(request, response);
        }
    }

    private void serveProjectList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        List<Project> myProjects = new ArrayList<>();
        User user = UserCacheHolder.getUserFromRequest(request);

        try {
            myProjects = thriftClients.makeProjectClient().getMyProjects(user.getEmail());
        } catch (TException e) {
            log.error("Could not fetch myProjects from backend for user, " + user.getEmail(), e);
        }
        myProjects = getWithFilledClearingStateSummary(myProjects, user);

        JSONArray jsonProjects = getProjectData(myProjects);
        JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
        jsonResult.put("aaData", jsonProjects);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem generating project list", e);
        }
    }

    private List<Project> getWithFilledClearingStateSummary(List<Project> projects, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();
        try {
            return projectClient.fillClearingStateSummary(projects, user);
        } catch (TException e) {
            log.error("Could not get summary of release clearing states for projects and their subprojects!", e);
            return projects;
        }
    }

    public JSONArray getProjectData(List<Project> projectList) {
        JSONArray projectData = JSONFactoryUtil.createJSONArray();
        for(Project project : projectList) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

            jsonObject.put("DT_RowId", project.getId());
            jsonObject.put("id", project.getId());
            jsonObject.put("name", SW360Utils.printName(project));
            jsonObject.put("description", Strings.nullToEmpty(project.getDescription()));
            jsonObject.put("releaseClearingState", acceptedReleases(project.getReleaseClearingStateSummary()));

            projectData.put(jsonObject);
        }

        return projectData;
    }

    private String acceptedReleases(ReleaseClearingStateSummary releaseClearingStateSummary) {
        String releaseCounts;

        if (releaseClearingStateSummary == null) {
            releaseCounts = "not available";
        } else {
            int total = releaseClearingStateSummary.newRelease + releaseClearingStateSummary.sentToClearingTool
                    + releaseClearingStateSummary.underClearing + releaseClearingStateSummary.reportAvailable
                    + releaseClearingStateSummary.approved;
            releaseCounts = releaseClearingStateSummary.approved + " / " + Integer.toString(total);
        }

        return releaseCounts;
    }
}
