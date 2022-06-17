/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.homepage.welcome;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;

import org.eclipse.sw360.portal.common.PortletUtils;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import static org.eclipse.sw360.portal.common.PortalConstants.WELCOME_PORTLET_NAME;

import java.io.IOException;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/welcome.properties"
    },
    property = {
        "javax.portlet.name=" + WELCOME_PORTLET_NAME,

        "javax.portlet.display-name=Welcome",
        "javax.portlet.info.short-title=Welcome",
        "javax.portlet.info.title=Welcome",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/homepage/welcome/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class WelcomePortlet extends MVCPortlet {
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        PortletUtils.setWelcomePageGuideLine(request);
        super.doView(request, response);
    }
}
