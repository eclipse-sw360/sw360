/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * This displays a map
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayMapOfExternalIdsString extends SimpleTagSupport {

    private static final String NULL_STRING = "null";
    private Map<String, String> value;
    private Map<String, String> autoFillValue;

    public void setValue(Map<String, String> value) {
        this.value = value;
    }

    public void setAutoFillValue(Map<String, String> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Map<String, String> fullValue;

        if (value == null) {
            fullValue = autoFillValue;
        } else {
            fullValue = value;
        }

        if (null != fullValue && !fullValue.isEmpty()) {
            String result = getMapAsString(fullValue);
            getJspContext().getOut().print(result);
        }
    }

    public static String getMapAsString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul class=\"mapDisplayRootItem\">");
        map.forEach((entryKey, entryValue) -> sb.append(getExternalIdsString(entryKey, entryValue)));
        sb.append("</ul>");
        return sb.toString();
    }

    private static String getExternalIdsString(String externalIdKey, String externalIdValues) {
        StringBuilder sb = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        Set<String> externalIdValueSet = new TreeSet<>();
        try {
            if(externalIdValues.equals(NULL_STRING)) {
                externalIdValueSet.add(NULL_STRING);
            } else {
                externalIdValueSet = mapper.readValue(externalIdValues, Set.class);
            }
        } catch (IOException e) {
            externalIdValueSet.add(externalIdValues);
        }
        nullToEmptySet(externalIdValueSet).forEach( e ->
                sb.append("<li><span class=\"mapDisplayChildItemLeft\">")
                  .append(StringEscapeUtils.escapeXml(externalIdKey))
                  .append("</span><span class=\"mapDisplayChildItemRight\"> ")
                  .append(StringEscapeUtils.escapeXml(e))
                  .append("</span></li>")
        );
        return sb.toString();
    }
}
