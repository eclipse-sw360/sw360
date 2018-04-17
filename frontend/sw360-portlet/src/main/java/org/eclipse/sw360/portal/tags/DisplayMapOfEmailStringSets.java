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

import org.eclipse.sw360.datahandler.common.CommonUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This displays a map map<string, set<string>>
 *
 */
public class DisplayMapOfEmailStringSets extends SimpleTagSupport {

    private Map<String, Set<String>> value;
    private Map<String, Set<String>> autoFillValue;

    public void setValue(Map<String,Set<String>> value) {
        this.value = value;
    }
    public void setAutoFillValue(Map<String, Set<String>> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Map<String, Set<String>> fullValue;

        if (value == null) {
            fullValue = autoFillValue;
        } else {
            fullValue = value;
        }

        if (null != fullValue && ! fullValue.isEmpty()) {
            getJspContext().getOut().print(getMapOfEmailSetsAsString(fullValue));
        }
    }

    private static String getMapOfEmailSetsAsString(Map<String, Set<String>> mapOfSets){
        if (mapOfSets == null || mapOfSets.size() == 0){
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<ul class=\"mapDisplayRootItem\">");
        mapOfSets.entrySet().stream()
                .filter(e -> ! CommonUtils.allAreEmptyOrNull(e.getValue()))
                .forEach(e -> sb.append(
                "<li><span class=\"mapDisplayChildItemLeft\">"
                        + e.getKey()
                        + ":"
                        + "</span> <span class=\"mapDisplayChildItemRight\">"
                        + getEmailSetAsString(e.getValue())
                        + "</span></li>"
        ));
        sb.append("</ul>");
        return sb.toString();
    }

    private static String getEmailSetAsString(Set<String> emailStrings){
        Set<String> mailtToLinks = emailStrings.stream()
                .map(s-> "<a href=\"mailto:"+s+"\">"+s+"</a>")
                .collect(Collectors.toSet());
        return CommonUtils.COMMA_JOINER.join(mailtToLinks);
    }
}
