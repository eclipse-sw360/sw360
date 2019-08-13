/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;

import javax.portlet.*;

import static org.eclipse.sw360.portal.common.PortalConstants.SCHEDULE_ADMIN_PORTLET_NAME;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + SCHEDULE_ADMIN_PORTLET_NAME,

        "javax.portlet.display-name=Schedule Administration",
        "javax.portlet.info.short-title=Schedule",
        "javax.portlet.info.title=Schedule Administration",

        "javax.portlet.init-param.view-template=/html/admin/scheduleAdmin/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ScheduleAdminPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(ScheduleAdminPortlet.class);


    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        prepareStandardView(request, response);
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request, RenderResponse response) {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            ScheduleService.Iface scheduleClient = new ThriftClients().makeScheduleClient();

            boolean isCveSearchScheduled = isCveSearchScheduled(scheduleClient, user);
            request.setAttribute(PortalConstants.CVESEARCH_IS_SCHEDULED, isCveSearchScheduled);
            boolean isAnyServiceScheduled = isAnyServiceScheduled(scheduleClient, user);
            request.setAttribute(PortalConstants.ANY_SERVICE_IS_SCHEDULED, isAnyServiceScheduled);
            int offsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_OFFSET, CommonUtils.formatTime(offsetInSeconds));
            int intervalInSeconds = scheduleClient.getInterval(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_INTERVAL, CommonUtils.formatTime(intervalInSeconds));
            String nextSync = scheduleClient.getNextSync(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_NEXT_SYNC, nextSync);
        } catch (TException te) {
            log.error(te.getMessage());
        }

    }

    private boolean isCveSearchScheduled(ScheduleService.Iface scheduleClient, User user) throws TException{
            RequestStatusWithBoolean requestStatus = scheduleClient.isServiceScheduled(ThriftClients.CVESEARCH_SERVICE, user);
            if(RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())){
                return requestStatus.isAnswerPositive();
            } else {
                throw new SW360Exception("Backend query for schedule status of cvesearch failed.");
            }
    }

    private boolean isAnyServiceScheduled(ScheduleService.Iface scheduleClient, User user) throws TException{
        RequestStatusWithBoolean requestStatus = scheduleClient.isAnyServiceScheduled(user);
        if(RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())){
            return requestStatus.isAnswerPositive();
        } else {
            throw new SW360Exception("Backend query for schedule status of services failed.");
        }
    }

    @UsedAsLiferayAction
    public void scheduleCveSearch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.CVESEARCH_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleCveSearch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.CVESEARCH_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleAllServices(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleAllServices(user);
            setSessionMessage(request, requestStatus, "Every task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }
}
