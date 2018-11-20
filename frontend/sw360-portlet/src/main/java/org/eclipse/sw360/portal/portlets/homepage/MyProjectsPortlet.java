/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
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
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Small homepage portlet
 *
 * @author cedric.bodet@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 * @author ksoranko@verifa.io
 */
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
