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

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.portal.common.PortalConstants;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + BULK_RELEASE_EDIT_PORTLET_NAME,

        "javax.portlet.display-name=Bulk Release Edit",
        "javax.portlet.info.short-title=Bulk Release Edit",
        "javax.portlet.info.title=Bulk Release Edit",

        "javax.portlet.init-param.view-template=/html/admin/bulkReleaseEdit/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BulkReleaseEditPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(BulkReleaseEditPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        ComponentService.Iface client = thriftClients.makeComponentClient();

        try {
            final List<Release> releaseSummary = client.getReleaseSummary(user);

            request.setAttribute(RELEASE_LIST, releaseSummary);

        } catch (TException e) {
            log.error("Could not fetch releases from backend", e);
            request.setAttribute(RELEASE_LIST, Collections.emptyList());
        }

        // Proceed with page rendering
        super.doView(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.RELEASE.equals(action)) {
            updateRelease(request, response);
        } else if (VIEW_VENDOR.equals(action)) {
            serveViewVendor(request, response);
        } else if (ADD_VENDOR.equals(action)) {
            serveAddVendor(request, response);
        } else {
            renderRequestStatus(request, response, RequestStatus.FAILURE);
        }
    }

    private void updateRelease(ResourceRequest request, ResourceResponse response) {
        final User user = UserCacheHolder.getUserFromRequest(request);

        RequestStatus requestStatus = RequestStatus.FAILURE;

        String releaseId = request.getParameter(RELEASE_ID);
        if (releaseId != null) {
            try {
                ComponentService.Iface client = thriftClients.makeComponentClient();
                Release release = client.getReleaseById(releaseId, user);
                ComponentPortletUtils.updateReleaseFromRequest(request, release);
                requestStatus = client.updateRelease(release, user);

            } catch (TException e) {
                log.error("Release update failed", e);
                requestStatus = RequestStatus.FAILURE;
            }
        }
        renderRequestStatus(request, response, requestStatus);
    }

    //** Copy paste of private portlet methods of ComponentPortlet... inheritance is not straight forward as we do not have attachments ... maybe if there is more time it can be done **/
    // TODO reduce code doubling here and in ComponentPortlet
    private void serveViewVendor(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String what = request.getParameter(PortalConstants.WHAT);
        String where = request.getParameter(PortalConstants.WHERE);

        if ("vendorSearch".equals(what)) {
            renderVendorSearch(request, response, where);
        }
    }

    private void renderVendorSearch(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        List<Vendor> vendors = null;
        try {
            VendorService.Iface client = thriftClients.makeVendorClient();
            if (isNullOrEmpty(searchText)) {
                vendors = client.getAllVendors();
            } else {
                vendors = client.searchVendors(searchText);
            }
        } catch (TException e) {
            log.error("Error searching vendors", e);
        }

        request.setAttribute("vendorsSearch", CommonUtils.nullToEmptyList(vendors));
        include("/html/components/ajax/vendorSearch.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    private void serveAddVendor(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        final Vendor vendor = new Vendor();
        ComponentPortletUtils.updateVendorFromRequest(request, vendor);

        try {
            VendorService.Iface client = thriftClients.makeVendorClient();
            String vendorId = client.addVendor(vendor);
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            jsonObject.put("id", vendorId);
            try {
                writeJSON(request, response, jsonObject);
            } catch (IOException e) {
                log.error("Problem rendering VendorId", e);
            }
        } catch (TException e) {
            log.error("Error adding vendor", e);
        }
    }
}
