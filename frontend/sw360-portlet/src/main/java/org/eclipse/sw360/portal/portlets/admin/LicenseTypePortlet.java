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

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + LICENSE_TYPE_PORTLET_NAME,

        "javax.portlet.display-name=License Types",
        "javax.portlet.info.short-title=License Types",
        "javax.portlet.info.title=License Types",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/licenseTypes/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class LicenseTypePortlet extends Sw360Portlet {
    private static final Logger log = LogManager.getLogger(LicenseTypePortlet.class);
    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) {
        String action = request.getParameter(PortalConstants.ACTION);

        if(REMOVE_LICENSE_TYPE.equals(action)) {
            serveDeleteLicenseType(request, response);
        } else if (CHECK_LICENSE_TYPE_IN_USE.equals(action)) {
            serveCheckLicenseTypeInUse(request, response);
        }
    }

    private void serveDeleteLicenseType(ResourceRequest request, ResourceResponse response) {
        final String id = request.getParameter("id");
        final User user = UserCacheHolder.getUserFromRequest(request);
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

        try {
            RequestStatus status = licenseClient.deleteLicenseType(id, user);
            renderRequestStatus(request, response, status);
        } catch (TException e) {
            log.error("Error deleting license type", e);
            renderRequestStatus(request, response, RequestStatus.FAILURE);
        }
    }

    private void serveCheckLicenseTypeInUse(ResourceRequest request, ResourceResponse response) {
        final String id = request.getParameter("id");
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

        try {
            int licenseCount = licenseClient.checkLicenseTypeInUse(id);
            renderRequest(request, response, licenseCount);
        } catch (TException e) {
            log.error("Error check license type in use", e);
            renderRequest(request, response, -1);
        }
    }

    private void renderRequest(PortletRequest request, MimeResponse response, int nLicenses) {
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        jsonObject.put(PortalConstants.RESULT, nLicenses);
        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering Request", e);
        }
    }

    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_ADD.equals(pageName)) {
            include("/html/admin/licenseTypes/add.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<LicenseType> licenseTypeList;
        try {
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

            licenseTypeList = licenseClient.getLicenseTypes();

        } catch (TException e) {
            log.error("Could not get License Type from backend ", e);
            licenseTypeList = Collections.emptyList();
        }

        request.setAttribute(LICENSE_TYPE_LIST, licenseTypeList);
    }

    @UsedAsLiferayAction
    public void addLicenseType(ActionRequest request, ActionResponse response) throws PortletException, IOException{
        final LicenseType licenseType = new LicenseType();
        ComponentPortletUtils.updateLicenseTypeFromRequest(request, licenseType);

        try {
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
            final User user = UserCacheHolder.getUserFromRequest(request);

            RequestStatus requestStatus = licenseClient.addLicenseType(licenseType, user);

            if(RequestStatus.DUPLICATE.equals(requestStatus)) {
                setSW360SessionError(request, ErrorMessages.LICENSE_TYPE_DUPLICATE);
                response.setRenderParameter(PAGENAME, PAGENAME_ADD);
            } else if(RequestStatus.ACCESS_DENIED.equals(requestStatus)) {
                setSW360SessionError(request, ErrorMessages.LICENSE_TYPE_ACCESS_DENIED);
            }
        } catch (TException e) {
            log.error("Error adding license type in backend!", e);
            setSW360SessionError(request, ErrorMessages.DEFAULT_ERROR_MESSAGE);
        }
    }
}
