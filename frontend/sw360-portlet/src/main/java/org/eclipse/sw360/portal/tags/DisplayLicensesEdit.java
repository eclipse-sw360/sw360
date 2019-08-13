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

import com.google.common.base.Joiner;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * This displays a user in Edit mode
 *
 * @author Johannes.Najjar@tngtech.com
 * @author daniele.fognini@tngtech.com
 */
public class DisplayLicensesEdit extends NameSpaceAwareTag {

    private String id;
    private Set<String> licenseIds = new HashSet<>();
    private String namespace;

    public void setId(String id) {
        this.id = id;
    }

    public void setLicenseIds(Set<String> licenseIds) {
        this.licenseIds = licenseIds;
    }

    public int doStartTag() throws JspException {
        JspWriter jspWriter = pageContext.getOut();

        namespace = getNamespace();
        StringBuilder display = new StringBuilder();
        try {
            String licenseIdsString = (licenseIds != null && !licenseIds.isEmpty()) ? Joiner.on(", ").join(licenseIds) : "";
            printHtmlElements(display, licenseIdsString);
            jspWriter.print(display.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void printHtmlElements(StringBuilder display, String licenseIdsStr) {
        display.append("<div class=\"form-group\">");
        display.append(String.format("<label for=\"%sDisplay\">Licenses</label>", id))
                .append(String.format("<input type=\"hidden\" readonly=\"\" value=\"%s\" id=\"%s\" name=\"%s%s\"/>", licenseIdsStr, id, namespace, id))
                .append(String.format("<input class=\"clickable licenseSearchDialogInteractive form-control\" data-id=\"%s\" type=\"text\" readonly=\"\" placeholder=\"Click to set Licenses\" value=\"%s\" id=\"%sDisplay\" />", id, licenseIdsStr, id));
        display.append("</div>");
    }
}
