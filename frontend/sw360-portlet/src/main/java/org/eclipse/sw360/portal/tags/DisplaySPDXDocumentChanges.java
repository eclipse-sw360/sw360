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

import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.tags.urlutils.LinkedReleaseRenderer;

import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.protocol.TType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
public class DisplaySPDXDocumentChanges extends UserAwareTag {
    private SPDXDocument actual;
    private SPDXDocument additions;
    private SPDXDocument deletions;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setActual(SPDXDocument actual) {
        this.actual = actual;
    }

    public void setAdditions(SPDXDocument additions) {
        this.additions = additions;
    }

    public void setDeletions(SPDXDocument deletions) {
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
            for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
                switch (field) {
                    // ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case OTHER_LICENSING_INFORMATION_DETECTEDS:
                        break;
                    default:
                        FieldMetaData fieldMetaData = SPDXDocument.metaDataMap.get(field);
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

            String a = renderOtherLicensingInformationDetected();
            jspWriter.print(renderString + a.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private boolean ensureSomethingTodoAndNoNull(SPDXDocument._Fields field) {
        if (!deletions.isSet(field) && !additions.isSet(field)) {
            return false;
        }

        if (field == SPDXDocument._Fields.SNIPPETS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == SPDXDocument._Fields.RELATIONSHIPS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == SPDXDocument._Fields.ANNOTATIONS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == SPDXDocument._Fields.OTHER_LICENSING_INFORMATION_DETECTEDS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        }
        return true;
    }

    private String renderSnippetInformation() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(SPDXDocument._Fields.SNIPPETS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(SPDXDocument._Fields.SNIPPETS)){
            actual.snippets = new HashSet<>();
        }
        for (SnippetInformation snippet : actual.getSnippets()) {
            for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
                FieldMetaData fieldMetaData = SnippetInformation.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        snippet,
                        additions.getSnippets().iterator().next(),
                        deletions.getSnippets().iterator().next(),
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.clearing.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"), LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private String renderOtherLicensingInformationDetected() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(SPDXDocument._Fields.OTHER_LICENSING_INFORMATION_DETECTEDS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(SPDXDocument._Fields.OTHER_LICENSING_INFORMATION_DETECTEDS)){
            actual.otherLicensingInformationDetecteds = new HashSet<>();
        }
        Iterator<OtherLicensingInformationDetected> otherLicensingAdditions = additions.getOtherLicensingInformationDetecteds().iterator();
        Iterator<OtherLicensingInformationDetected> otherLicensingDeletions = deletions.getOtherLicensingInformationDetecteds().iterator();
        for (OtherLicensingInformationDetected otherLicensing : actual.getOtherLicensingInformationDetecteds()) {
            OtherLicensingInformationDetected otherLicensingAddition = new OtherLicensingInformationDetected();
            if (otherLicensingAdditions.hasNext()) {
                otherLicensingAddition = otherLicensingAdditions.next();
            }
            OtherLicensingInformationDetected otherLicensingDeletion = new OtherLicensingInformationDetected();
            if (otherLicensingDeletions.hasNext()) {
                otherLicensingDeletion = otherLicensingDeletions.next();
            }

            for (OtherLicensingInformationDetected._Fields field : OtherLicensingInformationDetected._Fields.values()) {
                FieldMetaData fieldMetaData = OtherLicensingInformationDetected.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        otherLicensing,
                        otherLicensingAddition,
                        otherLicensingDeletion,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.other.licensing.information.detecteds")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"), LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

}