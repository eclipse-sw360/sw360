/*
 * Copyright Siemens AG, 2017, 2019.
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;

/**
 * This tag renders a span-tag with a text node displaying the given value and
 * setting it as title attribute. In addition the span receives the css class
 * "ellipsis" which will take care of adding ellipses when necessary.
 */
public class DisplayEllipsisString extends SimpleTagSupport {

    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public void doTag() throws JspException, IOException {
        getJspContext().getOut().print("<span class='text-truncate' title='" + TagUtils.escapeAttributeValue(value) + "'>"
                + TagUtils.escapeAttributeValue(value) + "</span>");
    }
}
