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
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

/**
 * This displays a map of secondary groups and roles
 *
 * @author jaideep.palit@siemens.com
 */
public class DisplayMapOfSecondaryGroupAndRoles extends SimpleTagSupport {

    private Map<String, Set<UserGroup>> value;

    public void setValue(Map<String, Set<UserGroup>> value) {
        this.value = value;
    }

    public void doTag() throws JspException, IOException {
        Map<String, Set<UserGroup>> fullValue = (value == null) ? new HashMap<>() : value;

        if (!CommonUtils.isNullOrEmptyMap(fullValue)) {
            String result = getMapAsString(new TreeMap<String, Set<UserGroup>>(fullValue));
            getJspContext().getOut().print(result);
        }
    }

    private static String getMapAsString(Map<String, Set<UserGroup>> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul class=\"mapDisplayRootItem\">");
        map.forEach((entryKey, entryValue) -> sb.append(getGroupAndRolesString(entryKey, entryValue)));
        sb.append("</ul>");
        return sb.toString();
    }

    private static String getGroupAndRolesString(String group, Set<UserGroup> roles) {
        StringBuilder sb = new StringBuilder();
        StringBuilder rolesSb = new StringBuilder();
        roles.forEach(role -> {
            if (rolesSb.length() > 0) {
                rolesSb.append(", ");
            }
            rolesSb.append(ThriftEnumUtils.enumToString(role));
        });
        sb.append("<li><span class=\"mapDisplayChildItemLeft\">")
          .append(StringEscapeUtils.escapeXml(group))
          .append("</span><span> -> </span><span class=\"mapDisplayChildItemRight\"> ")
          .append(rolesSb.toString())
          .append("</span></li>");
        return sb.toString();
    }
}
