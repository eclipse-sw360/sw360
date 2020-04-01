/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
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
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-info\">"+LanguageUtil.get(resourceBundle,"no.changes.in.basic.fields")+"</div>";
            } else {
                renderString = String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                        + "<thead><tr><th colspan=\"4\">"+ LanguageUtil.get(resourceBundle, "changes.for.basic.fields")+"</th></tr>"
                        + String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                        LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"), LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                        + renderString + "</tbody></table>";
            }

            jspWriter.print(renderString);
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }
}
