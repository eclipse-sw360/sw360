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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * This displays undefined/true/false for booleans depending on if they are set
 *
 * @author Cedric.Bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class DisplayBoolean extends SimpleTagSupport {

    private boolean value;
    private boolean defined = true;

    public void setValue(boolean value) {
        this.value = value;
    }

    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    public void doTag() throws JspException, IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        JspWriter out = getJspContext().getOut();

        if (defined && value) {
            out.print("<span class=\"text-success\">");
            out.print("<svg class=\"lexicon-icon\"><title>"+LanguageUtil.get(resourceBundle,"yes")+"</title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#check-circle\"/></svg>");
            out.print("&nbsp;"+LanguageUtil.get(resourceBundle,"yes"));
            out.print("</span>");
        } else {
            out.print("<span class=\"text-danger\">");
            out.print("<svg class=\"lexicon-icon\"><title>"+LanguageUtil.get(resourceBundle,"no")+"</title><use href=\"/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#times-circle\"/></svg>");
            out.print("&nbsp;"+LanguageUtil.get(resourceBundle,"no"));
            out.print("</span>");
        }
    }
}
