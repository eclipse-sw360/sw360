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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.*;

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

    private static final Logger LOGGER = Logger.getLogger(MyProjectsPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<Project> myProjects = new ArrayList<>();
        User user = UserCacheHolder.getUserFromRequest(request);
        try {
            myProjects = thriftClients.makeProjectClient().getMyProjects(user.getEmail());
        } catch (TException e) {
            LOGGER.error("Could not fetch myProjects from backend for user, " + user.getEmail(), e);
        }
        myProjects = getWithFilledClearingStateSummary(myProjects, user);
        request.setAttribute("projects",  CommonUtils.nullToEmptyList(myProjects));
        super.doView(request, response);
    }

    private List<Project> getWithFilledClearingStateSummary(List<Project> projects, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();
        try {
            return projectClient.fillClearingStateSummary(projects, user);
        } catch (TException e) {
            LOGGER.error("Could not get summary of release clearing states for projects and their subprojects!", e);
            return projects;
        }
    }
}
