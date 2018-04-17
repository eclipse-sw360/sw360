/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
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
import java.util.Map;

/**
 * This displays a map of maps of <string,string>
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayMapOfMaps extends SimpleTagSupport {

    private Map<String, Map<String, String>> value;
    private Map<String, Map<String, String>> autoFillValue;

    public void setValue(Map<String,Map<String, String>> value) {
        this.value = value;
    }
    public void setAutoFillValue(Map<String, Map<String, String>> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Map<String, Map<String, String>> fullValue;

        if (value == null) {
            fullValue = autoFillValue;
        } else {
            fullValue = value;
        }

        if (null != fullValue && ! fullValue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");
            fullValue.entrySet().stream().forEach(e -> sb.append(
                    "<li><b>"+e.getKey()+"</b>:<div style=\"padding:0 0 0 1em;\">"+ DisplayMap.getMapAsString(e.getValue()) + "</div></li>"
            ));
            sb.append("</ul>");
            getJspContext().getOut().print(sb.toString());
        }
    }
}
