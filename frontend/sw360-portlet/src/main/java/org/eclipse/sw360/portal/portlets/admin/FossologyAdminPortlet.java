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
package org.eclipse.sw360.portal.portlets.admin;

import com.liferay.portal.kernel.portlet.PortletResponseUtil;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.portal.common.FossologyConnectionHelper;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.portlet.*;

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

    private static final Logger log = Logger.getLogger(FossologyAdminPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<FossologyHostFingerPrint> fingerPrints;

        try {
            FossologyService.Iface client = thriftClients.makeFossologyClient();
            fingerPrints = client.getFingerPrints();
            request.setAttribute(PortalConstants.FINGER_PRINTS, fingerPrints);
        } catch (TException e) {
            request.setAttribute(PortalConstants.FINGER_PRINTS, Collections.emptyList());
            log.error("Error retrieving fingerprints", e);
        }
        super.doView(request, response);
    }

    @UsedAsLiferayAction
    public void setFingerPrints(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        List<FossologyHostFingerPrint> fingerPrints;
        FossologyService.Iface client;

        try {
            client = thriftClients.makeFossologyClient();
            fingerPrints = client.getFingerPrints();
        } catch (TException e) {
            log.error("Error retrieving fingerprints when setting", e);
            return;
        }
        for (FossologyHostFingerPrint fingerPrint : fingerPrints) {
            String bool = request.getParameter(fingerPrint.fingerPrint);
            fingerPrint.trusted = "on".equals(bool);
        }

        try {
            client.setFingerPrints(fingerPrints);
        } catch (TException e) {
            log.error("Problems setting finger prints", e);
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.FOSSOLOGY_CHECK_CONNECTION.equals(action)) {
            serveCheckConnection(request, response);
        } else if (PortalConstants.FOSSOLOGY_DEPLOY_SCRIPTS.equals(action)) {
            serveDeployScripts(request, response);
        } else if (PortalConstants.FOSSOLOGY_GET_PUBKEY.equals(action)) {
            servePublicKeyFile(request, response);
        }
    }

    private void servePublicKeyFile(ResourceRequest request, ResourceResponse response) {
        try {
            String publicKey = thriftClients.makeFossologyClient().getPublicKey();

            final ByteArrayInputStream keyStream = new ByteArrayInputStream(publicKey.getBytes());
            PortletResponseUtil.sendFile(request, response, "sw360_id.pub", keyStream, "text/plain");
        } catch (IOException | TException e) {
            log.error("An error occurred while retrieving the public key", e);
        }
    }

    public void serveDeployScripts(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        RequestStatus deploy = RequestStatus.FAILURE;
        try {
            deploy = thriftClients.makeFossologyClient().deployScripts();
        } catch (TException e) {
            log.error("Error connecting to backend", e);
        }
        renderRequestStatus(request, response, deploy);
    }

    public void serveCheckConnection(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
		RequestStatus checkConnection = RequestStatus.FAILURE;
		checkConnection = FossologyConnectionHelper.getInstance().checkFossologyConnection();
		renderRequestStatus(request, response, checkConnection);
    }

}
