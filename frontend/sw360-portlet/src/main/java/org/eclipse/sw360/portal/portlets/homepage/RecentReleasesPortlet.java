/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.List;

import javax.portlet.*;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.portal.common.PortalConstants.RECENT_RELEASES_PORTLET_NAME;



@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + RECENT_RELEASES_PORTLET_NAME,

        "javax.portlet.display-name=Recent Releases",
        "javax.portlet.info.short-title=Recent Releases",
        "javax.portlet.info.title=Recent Releases",

        "javax.portlet.init-param.view-template=/html/homepage/recentreleases/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class RecentReleasesPortlet extends Sw360Portlet {

    private static final Logger log = getLogger(RecentReleasesPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<Release> releases=null;

        try {
            releases = thriftClients.makeComponentClient().getRecentReleases();
        } catch (TException e) {
            log.error("Could not fetch recent components from backend", e);
        }

        request.setAttribute("releases",  CommonUtils.nullToEmptyList(releases));

        super.doView(request, response);
    }
}
