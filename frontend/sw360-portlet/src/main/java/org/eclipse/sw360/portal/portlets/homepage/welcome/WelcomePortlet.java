/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage.welcome;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;

import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.Portlet;

import static org.eclipse.sw360.portal.common.PortalConstants.WELCOME_PORTLET_NAME;

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

        "javax.portlet.init-param.view-template=/html/homepage/welcome/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class WelcomePortlet extends MVCPortlet {
}
