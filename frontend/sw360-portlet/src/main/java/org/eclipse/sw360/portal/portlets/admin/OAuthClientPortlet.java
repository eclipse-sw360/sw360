/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import static org.eclipse.sw360.portal.common.PortalConstants.OAUTH_CLIENT_PORTLET_NAME;

import javax.portlet.Portlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + OAUTH_CLIENT_PORTLET_NAME,

        "javax.portlet.display-name=OAuth Clients",
        "javax.portlet.info.short-title=OAuth Clients",
        "javax.portlet.info.title=OAuth Clients",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/oauthclient/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class OAuthClientPortlet extends Sw360Portlet {
    private static final Logger log = LogManager.getLogger(OAuthClientPortlet.class);
}
