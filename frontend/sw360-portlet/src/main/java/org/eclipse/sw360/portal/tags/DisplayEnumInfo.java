/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.portal.common.PortalConstants;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.apache.thrift.TEnum;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.all;

/**
 * @author thomas.maier@evosoft.com
 */
public class DisplayEnumInfo extends SimpleTagSupport {
    private Class type;

    private Iterable<? extends TEnum> options;

    public void setType(Class type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void setOptions(Iterable options) throws JspException {
        if (!all(options, instanceOf(TEnum.class))) {
            throw new JspException("given type options are not of class TEnum");
        }

        this.options = (Iterable<? extends TEnum>) options;
    }

    public void doTag() throws JspException, IOException {
        if (options != null) {
            printEnumValuesInfo(options.getClass());
        } else if (type != null) {
            printEnumValuesInfo(type);
        } else {
            throw new JspException("you must select either a TEnum type or a collection of values");
        }
    }

    private void printEnumValuesInfo(Class enumClass) throws IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        JspWriter jspWriter = getJspContext().getOut();

        String result = "<span class='" + PortalConstants.TOOLTIP_CLASS__CSS + " "
                + PortalConstants.TOOLTIP_CLASS__CSS + "-" + enumClass.getSimpleName() + "' data-content='"+LanguageUtil.get(resourceBundle,enumClass.getSimpleName())+"'>"
                + "<svg class='lexicon-icon'><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg></span>";

        jspWriter.print(result);

    }
}
