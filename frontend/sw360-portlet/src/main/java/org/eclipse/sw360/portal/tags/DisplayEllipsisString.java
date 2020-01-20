/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
