/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.users.ClientMetadata;
import org.eclipse.sw360.datahandler.thrift.users.UserAccess;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

/**
 * This displays a map of secondary groups and roles
 *
 * @author jaideep.palit@siemens.com
 */
public class DisplayMapOfOidcClientAndAcess extends SimpleTagSupport {

    private Map<String, ClientMetadata> value;

    public void setValue(Map<String, ClientMetadata> value) {
        this.value = value;
    }

    public void doTag() throws JspException, IOException {
        Map<String, ClientMetadata> fullValue = (value == null) ? new HashMap<>() : value;

        if (!CommonUtils.isNullOrEmptyMap(fullValue)) {
            String result = getMapAsString(new TreeMap<String, ClientMetadata>(fullValue));
            getJspContext().getOut().print(result);
        }
    }

    public static String getMapAsString(Map<String, ClientMetadata> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul class=\"mapDisplayRootItem\">");
        map.forEach((entryKey, entryValue) -> sb.append(getclientIdAndMetadataString(entryKey, entryValue)));
        sb.append("</ul>");
        return sb.toString();
    }

    private static String getclientIdAndMetadataString(String clientId, ClientMetadata clientMetadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("<li><span>")
          .append(StringEscapeUtils.escapeXml(clientMetadata.getName()))
          .append("</span><span> -> </span><span> ")
          .append(StringEscapeUtils.escapeXml(clientId))
          .append("</span><span> -> </span><span> ")
          .append(ThriftEnumUtils.enumToString(clientMetadata.getAccess()))
          .append("</span></li>");
        return sb.toString();
    }
}
