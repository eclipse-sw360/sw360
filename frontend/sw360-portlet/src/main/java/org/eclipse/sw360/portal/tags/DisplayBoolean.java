/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

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
        JspWriter out = getJspContext().getOut();

        if (defined) {
            if (value)
                out.print("Yes");
            else
                out.print("No");
        } else {
            out.print("No");
        }
    }
}
