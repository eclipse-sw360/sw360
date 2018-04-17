/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.apache.thrift.TEnum;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.portal.common.PortalConstants;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Iterator;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.all;

/**
 * @author thomas.maier@evosoft.com
 */
public class DisplayEnumInfo extends SimpleTagSupport {

    private static final String SW360_INFO_IMG_PATH = "/sw360-portlet/images/ic_info.png";
    private static final String SW360_INFO_IMG = "infopic";

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
        JspWriter jspWriter = getJspContext().getOut();

        String result = "<span class='" + PortalConstants.TOOLTIP_CLASS__CSS + " "
                + PortalConstants.TOOLTIP_CLASS__CSS + "-" + enumClass.getSimpleName() + "'>"
                + "<img class='" + SW360_INFO_IMG + "' src='" + SW360_INFO_IMG_PATH + "'></span>";

        jspWriter.print(result);

    }
}
