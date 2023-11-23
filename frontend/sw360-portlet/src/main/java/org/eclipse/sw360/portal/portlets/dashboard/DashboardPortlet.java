/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.dashboard;


import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.portlet.*;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.util.PortalUtil;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + DASHBOARD_PORTLET_NAME,

        "javax.portlet.display-name=Dashboard",
        "javax.portlet.info.short-title=Dashboard",
        "javax.portlet.info.title=Dashboard",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/dashboard/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class DashboardPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(DashboardPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);

        if (PAGENAME_SI_DASHBOARD.equals(pageName)) {
            include("/html/dashboard/viewSI.jsp", request, response);
        }
        else {
            // Proceed with page rendering
            super.doView(request, response);
        }
    }

}

