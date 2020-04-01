/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import static org.eclipse.sw360.portal.common.PortalConstants.ADMIN_PORTLET_NAME;

import java.io.IOException;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + ADMIN_PORTLET_NAME,

        "javax.portlet.display-name=Administration",
        "javax.portlet.info.short-title=Admin",
        "javax.portlet.info.title=Administration",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class AdminPortlet extends Sw360Portlet {
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        request.setAttribute("baseUrl", ((ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY)).getURLCurrent());

        // Proceed with page rendering
        super.doView(request, response);
    }
}
