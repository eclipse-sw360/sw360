/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.apache.thrift.TEnum;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * This tag renders a span-tag with a text node displaying the short string of
 * an enum according to {@link ThriftEnumUtils#enumToShortString(TEnum)}. The
 * title of the span will be the normal string value according to
 * {@link ThriftEnumUtils#enumToString(TEnum)}. So please make sure that your
 * enum is handled correctly there.
 */
public class DisplayEnumShort extends SimpleTagSupport {

    private TEnum value;

    public void setValue(TEnum value) {
        this.value = value;
    }

    public void doTag() throws JspException, IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        getJspContext().getOut().print("<span title='" + LanguageUtil.get(resourceBundle,ThriftEnumUtils.enumToShortString(value)) + "'>"
                + LanguageUtil.get(resourceBundle,ThriftEnumUtils.enumToShortString(value)) + "</span>");
    }
}
