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
package org.eclipse.sw360.portal.portlets.admin;

import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.portal.common.FossologyConnectionHelper;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.portal.common.PortalConstants.FOSSOLOGY_PORTLET_NAME;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + FOSSOLOGY_PORTLET_NAME,

        "javax.portlet.display-name=Fossology Administration",
        "javax.portlet.info.short-title=Fossology",
        "javax.portlet.info.title=Fossology Administration",

        "javax.portlet.init-param.view-template=/html/admin/fossyAdmin/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class FossologyAdminPortlet extends Sw360Portlet {

    private final Logger log = Logger.getLogger(this.getClass());

    // duplicated from org.eclipse.sw360.fossology.config.FossologyRestConfig
    // because only interfaces are visible from backend services
    public static final String CONFIG_KEY_URL = "url";
    public static final String CONFIG_KEY_TOKEN = "token";
    public static final String CONFIG_KEY_FOLDER_ID = "folderId";

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        try {
            FossologyService.Iface client = thriftClients.makeFossologyClient();
            ConfigContainer fossologyConfigContainer = client.getFossologyConfig();
            Map<String, String> fossologyConfig = fossologyConfigContainer.getConfigKeyToValues().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().iterator().next()));
            request.setAttribute(PortalConstants.FOSSOLOGY_CONFIG_BEAN, fossologyConfig);
        } catch (TException e) {
            request.setAttribute(PortalConstants.FOSSOLOGY_CONFIG_BEAN, null);
            log.error("Error retrieving fossology config from backend.", e);
        }
        super.doView(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.FOSSOLOGY_CHECK_CONNECTION.equals(action)) {
            serveCheckConnection(request, response);
        } else {
            log.error("Unknown action parameter <" + action + ">, so no action has been performed!");
            renderRequestStatus(request, response, RequestStatus.FAILURE);
        }
    }

    private void serveCheckConnection(ResourceRequest request, ResourceResponse response)
            throws PortletException, IOException {
        RequestStatus checkConnection = RequestStatus.FAILURE;
        checkConnection = FossologyConnectionHelper.getInstance().checkFossologyConnection();
        renderRequestStatus(request, response, checkConnection);
    }

    @UsedAsLiferayAction
    public void updateConfig(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            FossologyService.Iface client = thriftClients.makeFossologyClient();
            ConfigContainer fossologyConfig = client.getFossologyConfig();

            Map<String, Set<String>> configKeyToValues = new HashMap<>();
            configKeyToValues.put(CONFIG_KEY_URL, Stream
                    .of(request.getParameter(PortalConstants.FOSSOLOGY_CONFIG_KEY_URL)).collect(Collectors.toSet()));
            configKeyToValues.put(CONFIG_KEY_TOKEN, Stream
                    .of(request.getParameter(PortalConstants.FOSSOLOGY_CONFIG_KEY_TOKEN)).collect(Collectors.toSet()));
            configKeyToValues.put(CONFIG_KEY_FOLDER_ID,
                    Stream.of(request.getParameter(PortalConstants.FOSSOLOGY_CONFIG_KEY_FOLDER_ID))
                            .collect(Collectors.toSet()));

            fossologyConfig.setConfigKeyToValues(configKeyToValues);
            client.setFossologyConfig(fossologyConfig);

            setSessionMessage(request, RequestStatus.SUCCESS, "Fossology configuration", "update", null);
        } catch (TException e) {
            log.error("An error occurred while updating fossology configuration!", e);
            setSessionMessage(request, RequestStatus.FAILURE, "Fossology configuration", "update", null);
        }
    }

}
