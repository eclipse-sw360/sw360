/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
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
import java.util.Locale;

public class DisplayCountryCodeName extends SimpleTagSupport {

    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public void doTag() throws JspException, IOException {
        String output = "";

        Locale locale = new Locale("", value);
        if (locale != null) {
            output = locale.getDisplayCountry();
        }

        getJspContext().getOut().print(output);
    }
}
