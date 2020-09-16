/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
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
        "javax.portlet.name=" + TODOS_PORTLET_NAME,

        "javax.portlet.display-name=Obligations",
        "javax.portlet.info.short-title=Obligations",
        "javax.portlet.info.title=Obligations",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/obligations/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TodoPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(TodoPortlet.class);


    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) {

        final String id = request.getParameter("id");
        final User user = UserCacheHolder.getUserFromRequest(request);

        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();


        try {
            RequestStatus status = licenseClient.deleteObligations(id, user);
            renderRequestStatus(request,response, status);
        } catch (TException e) {
            log.error("Error deleting oblig", e);
            renderRequestStatus(request,response, RequestStatus.FAILURE);
        }
    }


    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {


        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_ADD.equals(pageName)) {
            include("/html/admin/obligations/add.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<Obligation> obligList;
        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

            obligList = licenseClient.getObligations();

        } catch (TException e) {
            log.error("Could not get Obligation from backend ", e);
            obligList = Collections.emptyList();
        }

        request.setAttribute(TODO_LIST, obligList);
    }

    @UsedAsLiferayAction
    public void addObligations(ActionRequest request, ActionResponse response) {

        final Obligation oblig = new Obligation();
        ComponentPortletUtils.updateTodoFromRequest(request, oblig);

        try {
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
            final User user = UserCacheHolder.getUserFromRequest(request);

            licenseClient.addObligations(oblig, user);
        } catch (TException e) {
            log.error("Error adding oblig", e);
        }
    }
}
