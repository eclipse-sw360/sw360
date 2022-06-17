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

import com.google.common.base.Joiner;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashSet;
import java.util.ResourceBundle;
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
    private boolean main = true;

    public void setId(String id) {
        this.id = id;
    }

    public void setMain(Boolean main) {
        this.main = main;
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
    	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        
        display.append("<div class=\"form-group\">");
        display.append(String.format("<label for=\"%sDisplay\">" + LanguageUtil.get(resourceBundle, main ? "main.licenses" : "other.licenses") + "</label>", id))
                .append(String.format("<input type=\"hidden\" readonly=\"\" value=\"%s\" id=\"%s\" name=\"%s%s\"/>", licenseIdsStr, id, namespace, id))
                .append(String.format("<input class=\"clickable licenseSearchDialogInteractive form-control\" data-id=\"%s\" type=\"text\" readonly=\"\" placeholder=\""+LanguageUtil.get(resourceBundle,"click.to.set.licenses")+"\" value=\"%s\" id=\"%sDisplay\" />", id, licenseIdsStr, id));
        display.append("</div>");
    }
}
