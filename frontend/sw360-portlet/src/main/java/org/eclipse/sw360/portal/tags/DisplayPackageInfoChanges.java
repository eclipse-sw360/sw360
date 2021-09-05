/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.tags.urlutils.LinkedReleaseRenderer;

import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.protocol.TType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;
import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed in the release
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class DisplayPackageInfoChanges extends UserAwareTag {
    private PackageInformation actual;
    private PackageInformation additions;
    private PackageInformation deletions;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setActual(PackageInformation actual) {
        this.actual = actual;
    }

    public void setAdditions(PackageInformation additions) {
        this.additions = additions;
    }

    public void setDeletions(PackageInformation deletions) {
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

        if (additions == null || deletions == null) {
            return SKIP_BODY;
        }

        try {
            for (PackageInformation._Fields field : PackageInformation._Fields.values()) {
                switch (field) {
                    // ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                        break;
                    default:
                        FieldMetaData fieldMetaData = PackageInformation.metaDataMap.get(field);
                        displaySimpleFieldOrSet(display, actual, additions, deletions, field, fieldMetaData, "");
                }
            }

            String renderString = display.toString();
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(),
                    getClass());

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-danger\">"
                        + LanguageUtil.get(resourceBundle, "no.changes.in.basic.fields") + "</div>";
            } else {
                renderString = String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                        + "<thead><tr><th colspan=\"4\">" + LanguageUtil.get(resourceBundle, "changes.for.basic.fields")
                        + " </th></tr>"
                        + String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                                LanguageUtil.get(resourceBundle, "field.name"),
                                LanguageUtil.get(resourceBundle, "current.value"),
                                LanguageUtil.get(resourceBundle, "former.value"),
                                LanguageUtil.get(resourceBundle, "suggested.value"))
                        + renderString + "</tbody></table>";
            }

            jspWriter.print(renderString);
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }
}