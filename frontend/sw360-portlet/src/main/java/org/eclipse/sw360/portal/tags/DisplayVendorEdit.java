/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;

import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
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
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        
        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" readonly=\"\" value=\"\"  id=\"%s\" name=\"%s%s\"/>", id, namespace, id))
                .append("<div class=\"form-group has-feedback\">")
                .append(String.format(
                        "<input type=\"text\" readonly=\"\" class=\"form-control edit-vendor clickable\" placeholder=\"" + LanguageUtil.get(resourceBundle, "click.to.set.vendor") + "\" id=\"%sDisplay\" data-release-id=\"%s\"/>",
                        id, releaseId))
                .append("<span class=\"glyphicon glyphicon-remove-circle form-control-feedback clearSelection\" id=\"clearVendor\"/>")
                .append("</div>");
        display.append("</div>");
    }

    private void printFullVendor(StringBuilder display, Vendor vendor) {
        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" readonly=\"\" value=\"%s\"  id=\"%s\" name=\"%s%s\"/>", vendor.getId(), id, namespace, id))
                .append("<div class=\"form-group has-feedback\">")
                .append(String.format(
                        "<input type=\"text\" readonly=\"\" class=\"form-control edit-vendor clickable\" value=\"%s\" id=\"%sDisplay\" data-release-id=\"%s\"/>",
                        vendor.getFullname(), id, releaseId))
                .append("<span class=\"glyphicon glyphicon-remove-circle form-control-feedback clearSelection\" id=\"clearVendor\"/>")
                .append("</div>");
        display.append("</div>");
    }

    private void printLabel(StringBuilder display) {
    	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        
        if (StringUtils.isNotEmpty(label)) {
            display.append(
                    String.format("<label for=\"%sDisplay\">%s</label>", id, LanguageUtil.get(resourceBundle,label)));
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
