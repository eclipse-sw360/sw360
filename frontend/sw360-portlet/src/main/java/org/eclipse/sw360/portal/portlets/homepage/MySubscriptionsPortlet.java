/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.List;

import javax.portlet.*;

import static org.apache.log4j.Logger.getLogger;


@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + PortalConstants.MY_SUBSCRIPTIONS_PORTLET_NAME,

        "javax.portlet.display-name=My Subscriptions",
        "javax.portlet.info.short-title=My Subscriptions",
        "javax.portlet.info.title=My Subscriptions",

        "javax.portlet.init-param.view-template=/html/homepage/mysubscriptions/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MySubscriptionsPortlet extends Sw360Portlet {

    private static final Logger log = getLogger(MySubscriptionsPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<Component> components=null;
        List<Release> releases=null;

        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            ComponentService.Iface componentClient = thriftClients.makeComponentClient();
            components = componentClient.getSubscribedComponents(user);
            releases  = componentClient.getSubscribedReleases(user);
        } catch (TException e) {
            log.error("Could not fetch your subscriptions from backend", e);
        }

        request.setAttribute(PortalConstants.COMPONENT_LIST, CommonUtils.nullToEmptyList(components));
        request.setAttribute(PortalConstants.RELEASE_LIST, CommonUtils.nullToEmptyList( releases));

        super.doView(request, response);
    }
}
