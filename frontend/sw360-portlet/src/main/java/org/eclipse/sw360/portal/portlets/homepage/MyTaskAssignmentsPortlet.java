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

import static org.eclipse.sw360.portal.common.PortalConstants.MY_TASK_ASSIGNMENTS_PORTLET_NAME;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.google.common.collect.Lists;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
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
        "javax.portlet.name=" + MY_TASK_ASSIGNMENTS_PORTLET_NAME,
        "javax.portlet.display-name=My Task Assignments",
        "javax.portlet.info.short-title=My Task Assignments",
        "javax.portlet.info.title=My Task Assignments",

        "javax.portlet.init-param.view-template=/html/homepage/mytaskassignments/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MyTaskAssignmentsPortlet extends AbstractTasksPortlet {
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.LOAD_TASK_ASSIGNMENT_LIST.equals(action)) {
            serveTaskList(request, response);
        }
    }

    private void serveTaskList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        List<ModerationRequest> openModerations = Lists.newArrayList();

        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            List<ModerationRequest> moderations = thriftClients.makeModerationClient().getRequestsByModerator(user);
            openModerations = moderations.stream().filter(ModerationPortletUtils::isOpenModerationRequest).collect(Collectors.toList());
        } catch (TException e) {
            log.error("Could not fetch your moderations from backend", e);
        }

        sendModerations(request, response, openModerations);
    }
}
