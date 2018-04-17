/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.apache.thrift.TEnum;
import org.eclipse.sw360.portal.common.PortalConstants;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * This displays a list
 *
 * @author Cedric.Bodet@tngtech.com Johannes.Najjar@tngtech.com Thomas.Maier@evosoft.com
 */
public class DisplayEnum extends SimpleTagSupport {

    private TEnum value;
    private Boolean bare = false;

    public void setValue(TEnum value) {
        this.value = value;
    }

    public void setBare(Boolean bare) {
        this.bare = bare;
    }

    public void doTag() throws JspException, IOException {
        String enumValue;

        if (bare || value == null) {
            enumValue = ThriftEnumUtils.enumToString(value);
        } else {
            enumValue = printEnumValueWithTooltip();
        }

        getJspContext().getOut().print(enumValue);
    }

    private String printEnumValueWithTooltip() {
        return "<span class='" + PortalConstants.TOOLTIP_CLASS__CSS + " "
                + PortalConstants.TOOLTIP_CLASS__CSS + "-" + value.getClass().getSimpleName() + "-" + value.toString() + "'>"
                + ThriftEnumUtils.enumToString(value) + "</span>";
    }
}
