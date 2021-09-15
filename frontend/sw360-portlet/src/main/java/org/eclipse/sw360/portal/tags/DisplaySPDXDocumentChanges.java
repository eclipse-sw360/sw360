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
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;

import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed in the SPDX Document
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
                    case SNIPPETS:
                    case ANNOTATIONS:
                    case RELATIONSHIPS:
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
            String snippetRenderString = renderSnippetInformation();
            String relationshipSRenderString = renderRelationshipInformation();
            String annotaionsRenderString = renderAnnotaionsInformation();
            String otherLicensingRenderString = renderOtherLicensingInformationDetected();
            jspWriter.print(renderString + snippetRenderString.toString()
                            + relationshipSRenderString.toString()
                            + annotaionsRenderString.toString()
                            + otherLicensingRenderString.toString());
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
        Iterator<SnippetInformation> snippetAdditionsIterator = additions.getSnippetsIterator();
        Iterator<SnippetInformation> snippetDeletionsIterator = deletions.getSnippetsIterator();
        for (SnippetInformation snippet : actual.getSnippets()) {
            SnippetInformation snippetAdditions = new SnippetInformation();
            if (snippetAdditionsIterator.hasNext()) {
                snippetAdditions = snippetAdditionsIterator.next();
            }
            SnippetInformation snippetDeletions = new SnippetInformation();
            if (snippetDeletionsIterator.hasNext()) {
                snippetDeletions = snippetDeletionsIterator.next();
            }
            for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
                FieldMetaData fieldMetaData = SnippetInformation.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        snippet,
                        snippetAdditions,
                        snippetDeletions,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.snippets.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private String renderRelationshipInformation() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(SPDXDocument._Fields.RELATIONSHIPS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(SPDXDocument._Fields.RELATIONSHIPS)){
            actual.relationships = new HashSet<>();
        }
        Iterator<RelationshipsBetweenSPDXElements> relationshipsAdditionsIterator = additions.getRelationshipsIterator();
        Iterator<RelationshipsBetweenSPDXElements> relationshipsDeletionsIterator = deletions.getRelationshipsIterator();
        for (RelationshipsBetweenSPDXElements relationships : actual.getRelationships()) {
            RelationshipsBetweenSPDXElements relationshipsAdditions = new RelationshipsBetweenSPDXElements();
            if (relationshipsAdditionsIterator.hasNext()) {
                relationshipsAdditions = relationshipsAdditionsIterator.next();
            }
            RelationshipsBetweenSPDXElements relationshipsDeletions = new RelationshipsBetweenSPDXElements();
            if (relationshipsDeletionsIterator.hasNext()) {
                relationshipsDeletions = relationshipsDeletionsIterator.next();
            }
            for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
                FieldMetaData fieldMetaData = RelationshipsBetweenSPDXElements.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        relationships,
                        relationshipsAdditions,
                        relationshipsDeletions,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.relationship.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private String renderAnnotaionsInformation() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(SPDXDocument._Fields.ANNOTATIONS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(SPDXDocument._Fields.ANNOTATIONS)){
            actual.annotations = new HashSet<>();
        }
        Iterator<Annotations> annotationsAdditionsIterator = additions.getAnnotationsIterator();
        Iterator<Annotations> annotationsDeletionsIterator = deletions.getAnnotationsIterator();
        for (Annotations annotations : actual.getAnnotations()) {
            Annotations annotationsAdditions = new Annotations();
            if (annotationsAdditionsIterator.hasNext()) {
                annotationsAdditions = annotationsAdditionsIterator.next();
            }
            Annotations annotationsDeletions = new Annotations();
            if (annotationsDeletionsIterator.hasNext()) {
                annotationsDeletions = annotationsDeletionsIterator.next();
            }
            for (Annotations._Fields field : Annotations._Fields.values()) {
                FieldMetaData fieldMetaData = Annotations.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        annotations,
                        annotationsAdditions,
                        annotationsDeletions,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.annotaions.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
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
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

}