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
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * This displays a vendor field that can be used as extension point for a vendor
 * search dialog via onclick listeners (see also
 * src/main/webapp/js/components/includes/vendors/searchVendor.js)
 */
public class DisplayVendorEdit extends NameSpaceAwareTag {

    private String namespace;
    private String id;
    private String vendorId = "";
    private Vendor vendor = null;
    private String releaseId = "";
    private String label = "";

    public int doStartTag() throws JspException {
        JspWriter jspWriter = pageContext.getOut();

        namespace = getNamespace();
        StringBuilder display = new StringBuilder();
        try {
            if (vendor == null && StringUtils.isNotEmpty(vendorId)) {
                VendorService.Iface client;
                try {
                    client = new ThriftClients().makeVendorClient();
                    vendor = client.getByID(vendorId);
                } catch (TException ignored) {
                }
            }

            if (vendor != null) {
                printFullVendor(display, vendor);
            } else {
                printEmptyVendor(display);
            }

            jspWriter.print(display.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void printEmptyVendor(StringBuilder display) {
        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" readonly=\"\" value=\"\"  id=\"%s\" name=\"%s%s\"/>", id, namespace, id))
                .append(String.format(
                        "<input type=\"text\" readonly=\"\" class=\"form-control edit-vendor clickable\" placeholder=\"Click to set vendor\" id=\"%sDisplay\" data-release-id=\"%s\"/>",
                        id, releaseId));
        display.append("</div>");
    }

    private void printFullVendor(StringBuilder display, Vendor vendor) {
        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" readonly=\"\" value=\"%s\"  id=\"%s\" name=\"%s%s\"/>", vendor.getId(), id, namespace, id))
                .append(String.format(
                        "<input type=\"text\" readonly=\"\" class=\"form-control edit-vendor clickable\" value=\"%s\" id=\"%sDisplay\" data-release-id=\"%s\"/>",
                        vendor.getFullname(), id, releaseId));
        display.append("</div>");
    }

    private void printLabel(StringBuilder display) {
        if (StringUtils.isNotEmpty(label)) {
            display.append(
                    String.format("<label for=\"%sDisplay\">%s</label>", id, label));
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
