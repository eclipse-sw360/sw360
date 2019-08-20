/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.apache.thrift.TException;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed in the project
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayComponentChanges extends NameSpaceAwareTag {
    private Component actual;
    private Component additions;
    private Component deletions;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setActual(Component actual) {
        this.actual = actual;
    }

    public void setAdditions(Component additions) {
        this.additions = additions;
    }

    public void setDeletions(Component deletions) {
        this.deletions = deletions;
    }

    public void setTableClasses(String tableClasses) {
        this.tableClasses = tableClasses;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();

        StringBuilder display = new StringBuilder();
        String namespace = getNamespace();

        if (additions == null || deletions == null) {
            return SKIP_BODY;
        }

        try {
            for (Component._Fields field : Component._Fields.values()) {
                switch (field) {
                    //ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case CREATED_ON:
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                        //Releases and aggregates:
                    case RELEASES:
                    case RELEASE_IDS:
                    case MAIN_LICENSE_IDS:
                    case LANGUAGES:
                    case OPERATING_SYSTEMS:
                    case VENDOR_NAMES:
                        //Taken care of externally
                    case ATTACHMENTS:
                        break;

                    default:
                        FieldMetaData fieldMetaData = Component.metaDataMap.get(field);
                        displaySimpleFieldOrSet(display, actual, additions, deletions, field, fieldMetaData, "");
                }
            }

            String renderString = display.toString();

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-info\">No changes in basic fields.</div>";
            } else {
                renderString = String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                        + "<thead><tr><th colspan=\"4\"> Changes for Basic fields</th></tr>"
                        + String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                        FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                        + renderString + "</tbody></table>";
            }

            jspWriter.print(renderString);
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }
}
