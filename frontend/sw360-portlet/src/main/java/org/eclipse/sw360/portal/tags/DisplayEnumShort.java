/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;

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
        getJspContext().getOut().print("<span title='" + ThriftEnumUtils.enumToString(value) + "'>"
                + ThriftEnumUtils.enumToShortString(value) + "</span>");
    }
}
