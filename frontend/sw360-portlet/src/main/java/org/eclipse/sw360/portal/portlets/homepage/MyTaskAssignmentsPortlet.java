/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.moderation.ModerationPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.log4j.Logger.getLogger;

/**
 * Small homepage portlet
 *
 * @author cedric.bodet@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class MyTaskAssignmentsPortlet extends Sw360Portlet {

    private static final Logger log = getLogger(MyTaskAssignmentsPortlet.class);


    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<ModerationRequest> openModerations=null;

        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            List<ModerationRequest> moderations = thriftClients.makeModerationClient().getRequestsByModerator(user);
            openModerations = moderations.stream().filter(ModerationPortletUtils::isOpenModerationRequest).collect(Collectors.toList());
        } catch (TException e) {
            log.error("Could not fetch your moderations from backend", e);
        }

        request.setAttribute(PortalConstants.MODERATION_REQUESTS,  CommonUtils.nullToEmptyList(openModerations));

        super.doView(request, response);
    }
}
