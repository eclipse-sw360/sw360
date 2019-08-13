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

import com.google.common.collect.Sets;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.exporter.VendorExporter;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.portlet.*;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + VENDOR_PORTLET_NAME,

        "javax.portlet.display-name=Vendor Administration",
        "javax.portlet.info.short-title=Vendors",
        "javax.portlet.info.title=Vendor Administration",

        "javax.portlet.init-param.view-template=/html/admin/vendors/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class VendorPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(VendorPortlet.class);

    /**
     * Excel exporter
     */
    private final VendorExporter exporter = new VendorExporter();

    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(ACTION);

        if (EXPORT_TO_EXCEL.equals(action)) {
            exportExcel(request, response);
        } else if (REMOVE_VENDOR.equals(action)) {
            removeVendor(request, response);
        }
    }

    private void removeVendor(PortletRequest request, ResourceResponse response) throws IOException {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteVendor(request, log);
        serveRequestStatus(request, response, requestStatus, "Problem removing vendor", log);

    }

    private void exportExcel(ResourceRequest request, ResourceResponse response) {
        try {
            VendorService.Iface client = thriftClients.makeVendorClient();
            List<Vendor> vendors = client.getAllVendors();
            String filename = String.format("vendors-%s.xlsx", SW360Utils.getCreatedOn());
            PortletResponseUtil.sendFile(request, response, filename, exporter.makeExcelExport(vendors), CONTENT_TYPE_OPENXML_SPREADSHEET);
        } catch (IOException | TException e) {
            log.error("An error occurred while generating the Excel export", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_EDIT.equals(pageName)) {
            prepareVendorEdit(request);
            include("/html/admin/vendors/edit.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareVendorEdit(RenderRequest request) throws PortletException {
        String id = request.getParameter(VENDOR_ID);

        if (!isNullOrEmpty(id)) {
            try {
                VendorService.Iface vendorClient = thriftClients.makeVendorClient();
                Vendor vendor = vendorClient.getByID(id);
                request.setAttribute(VENDOR, vendor);


                final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                final List<Release> releasesFromVendorIds = componentClient.getReleasesFromVendorIds(Sets.newHashSet(id));

                request.setAttribute(RELEASE_LIST, releasesFromVendorIds);
            } catch (TException e) {
                log.error("Problem retrieving vendor");
            }
        }
        else{
            request.setAttribute(RELEASE_LIST, Collections.emptyList());
        }
    }

    private void prepareStandardView(RenderRequest request) throws IOException {
        List<Vendor> vendorList;
        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            VendorService.Iface vendorClient = thriftClients.makeVendorClient();

            vendorList = vendorClient.getAllVendors();

        } catch (TException e) {
            log.error("Could not get Vendors from backend ", e);
            vendorList = Collections.emptyList();
        }

        request.setAttribute(VENDOR_LIST, vendorList);
    }

    @UsedAsLiferayAction
    public void updateVendor(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String id = request.getParameter(VENDOR_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);

        if (id != null) {
            try {
                VendorService.Iface vendorClient = thriftClients.makeVendorClient();
                Vendor vendor = vendorClient.getByID(id);
                ComponentPortletUtils.updateVendorFromRequest(request, vendor);
                RequestStatus requestStatus = vendorClient.updateVendor(vendor, user);
                setSessionMessage(request, requestStatus, "Vendor", "update", vendor.getShortname());
            } catch (TException e) {
                log.error("Error fetching vendor from backend!", e);
            }
        }
        else{
            addVendor(request);
        }
    }

    @UsedAsLiferayAction
    public void removeVendor(ActionRequest request, ActionResponse response) throws IOException, PortletException {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteVendor(request, log);
        setSessionMessage(request, requestStatus, "Vendor", "delete");
        response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
    }

    private void addVendor(ActionRequest request)  {
        final Vendor vendor = new Vendor();
        ComponentPortletUtils.updateVendorFromRequest(request, vendor);

        try {
            VendorService.Iface vendorClient = thriftClients.makeVendorClient();
            String vendorId = vendorClient.addVendor(vendor);
        } catch (TException e) {
            log.error("Error adding vendor", e);
        }
    }
}
