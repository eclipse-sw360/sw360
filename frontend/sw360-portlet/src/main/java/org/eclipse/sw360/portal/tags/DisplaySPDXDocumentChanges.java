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
import org.apache.commons.lang.StringEscapeUtils;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import static org.eclipse.sw360.portal.tags.TagUtils.*;

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
                        displaySimpleFieldOrSet(display, actual, additions, deletions, field, fieldMetaData, "", false);
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

        Set<SnippetInformation> additionsSnippetInformations = additions.getSnippets();
        Set<SnippetInformation> deletionsSnippetInformations = deletions.getSnippets();
        Set<SnippetInformation> currentSnippetInformations = actual.getSnippets();
        int changeSize = 0;
        if (additionsSnippetInformations.size() > deletionsSnippetInformations.size()) {
            changeSize = additionsSnippetInformations.size() + currentSnippetInformations.size() - deletionsSnippetInformations.size();
        } else {
            changeSize = currentSnippetInformations.size();
        }

        for (int i = 0; i < changeSize; i++) {
            SnippetInformation snippetDeletions = getSnippetInformationByIndex(deletions, i);
            SnippetInformation snippetAdditions = getSnippetInformationByIndex(additions, i);
            SnippetInformation snippet = getSnippetInformationByIndex(actual, i);

            String snippetRangeRendeString = null;
            for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
                FieldMetaData fieldMetaData = SnippetInformation.metaDataMap.get(field);
                if (field == SnippetInformation._Fields.SNIPPET_RANGES) {
                    snippetRangeRendeString = renderSnippetRange(snippet, snippetAdditions, snippetDeletions);
                } else {
                        displaySimpleFieldOrSet(
                            display,
                            snippet,
                            snippetAdditions,
                            snippetDeletions,
                            field, fieldMetaData, "", false);
                }
            }
            if (snippetRangeRendeString != null) {
                display.append(snippetRangeRendeString);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.snippets.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private SnippetInformation getSnippetInformationByIndex(SPDXDocument spdx, int index) {
        SnippetInformation snippet;
        Iterator<SnippetInformation> snippetsIterator = spdx.getSnippetsIterator();
        while (snippetsIterator.hasNext()) {
            snippet = snippetsIterator.next();
            if (snippet.getIndex() == index) {
                snippet.setIndex(0);    // Set 0 to not show Index when add or delete
                return snippet;
            }
        }
        return new SnippetInformation();
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

        Set<RelationshipsBetweenSPDXElements> additionsRelationshipsBetweenSPDXElementss = additions.getRelationships();
        Set<RelationshipsBetweenSPDXElements> deletionsRelationshipsBetweenSPDXElementss = deletions.getRelationships();
        Set<RelationshipsBetweenSPDXElements> currentRelationshipsBetweenSPDXElementss = actual.getRelationships();
        int changeSize = 0;
        if (additionsRelationshipsBetweenSPDXElementss.size() > deletionsRelationshipsBetweenSPDXElementss.size()) {
            changeSize = additionsRelationshipsBetweenSPDXElementss.size() + currentRelationshipsBetweenSPDXElementss.size()
                        - deletionsRelationshipsBetweenSPDXElementss.size();
        } else {
            changeSize = currentRelationshipsBetweenSPDXElementss.size();
        }

        for (int i = 0; i < changeSize; i++) {

            RelationshipsBetweenSPDXElements relationshipDeletions = getRelationshipByIndex(deletions, i);
            RelationshipsBetweenSPDXElements relationshipAdditions = getRelationshipByIndex(additions, i);
            RelationshipsBetweenSPDXElements relationship = getRelationshipByIndex(actual, i);

            for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
                FieldMetaData fieldMetaData = RelationshipsBetweenSPDXElements.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        relationship,
                        relationshipAdditions,
                        relationshipDeletions,
                        field, fieldMetaData, "", false);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.relationship.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private RelationshipsBetweenSPDXElements getRelationshipByIndex(SPDXDocument spdx, int index) {
        RelationshipsBetweenSPDXElements relationship;
        Iterator<RelationshipsBetweenSPDXElements> relationshipIterator = spdx.getRelationshipsIterator();
        while (relationshipIterator.hasNext()) {
            relationship = relationshipIterator.next();
            if (relationship.getIndex() == index) {
                return relationship;    // Set 0 to not show Index when add or delete
            }
        }
        return new RelationshipsBetweenSPDXElements();
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

        Set<Annotations> additionsAnnotations = additions.getAnnotations();
        Set<Annotations> deletionsAnnotations = deletions.getAnnotations();
        Set<Annotations> currentAnnotations = actual.getAnnotations();
        int changeSize = 0;
        if (additionsAnnotations.size() > deletionsAnnotations.size()) {
            changeSize = additionsAnnotations.size() + currentAnnotations.size() - deletionsAnnotations.size();
        } else {
            changeSize = currentAnnotations.size();
        }

        for (int i = 0; i < changeSize; i++) {
            Annotations annotationDeletions = getAnnotationsByIndex(deletions, i);
            Annotations annotationAdditions = getAnnotationsByIndex(additions, i);
            Annotations annotation = getAnnotationsByIndex(actual, i);

            for (Annotations._Fields field : Annotations._Fields.values()) {
                FieldMetaData fieldMetaData = Annotations.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        annotation,
                        annotationAdditions,
                        annotationDeletions,
                        field, fieldMetaData, "", false);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.annotaions.information")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private Annotations getAnnotationsByIndex(SPDXDocument spdx, int index) {
        Annotations annotations;
        Iterator<Annotations> annotationsIterator = spdx.getAnnotationsIterator();
        while (annotationsIterator.hasNext()) {
            annotations = annotationsIterator.next();
            if (annotations.getIndex() == index) {
                annotations.setIndex(0);    // Set 0 to not show Index when add or delete
                return annotations;
            }
        }
        return new Annotations();
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

        Set<OtherLicensingInformationDetected> additionsOtherLicensingInformationDetecteds = additions.getOtherLicensingInformationDetecteds();
        Set<OtherLicensingInformationDetected> deletionsOtherLicensingInformationDetecteds = deletions.getOtherLicensingInformationDetecteds();
        Set<OtherLicensingInformationDetected> currentOtherLicensingInformationDetecteds = actual.getOtherLicensingInformationDetecteds();
        int changeSize = 0;
        if (additionsOtherLicensingInformationDetecteds.size() > deletionsOtherLicensingInformationDetecteds.size()) {
            changeSize = additionsOtherLicensingInformationDetecteds.size()
                        + currentOtherLicensingInformationDetecteds.size()
                        - deletionsOtherLicensingInformationDetecteds.size();
        } else {
            changeSize = currentOtherLicensingInformationDetecteds.size();
        }

        for (int i = 0; i < changeSize; i++) {
            OtherLicensingInformationDetected otherLicensingDeletions = getOtherLicensingByIndex(deletions, i);
            OtherLicensingInformationDetected otherLicensingAdditions = getOtherLicensingByIndex(additions, i);
            OtherLicensingInformationDetected otherLicensing = getOtherLicensingByIndex(actual, i);
            for (OtherLicensingInformationDetected._Fields field : OtherLicensingInformationDetected._Fields.values()) {
                FieldMetaData fieldMetaData = OtherLicensingInformationDetected.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        otherLicensing,
                        otherLicensingAdditions,
                        otherLicensingDeletions,
                        field, fieldMetaData, "", false);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.other.licensing.information.detecteds")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private OtherLicensingInformationDetected getOtherLicensingByIndex(SPDXDocument spdx, int index) {
        OtherLicensingInformationDetected otherLicensing;
        Iterator<OtherLicensingInformationDetected> otherLicensingIterator = spdx.getOtherLicensingInformationDetectedsIterator();
        while (otherLicensingIterator.hasNext()) {
            otherLicensing = otherLicensingIterator.next();
            if (otherLicensing.getIndex() == index) {
                otherLicensing.setIndex(0); // Set 0 to not show Index when add or delete
                return otherLicensing;
            }
        }
        return new OtherLicensingInformationDetected();
    }

    private String renderSnippetRange(SnippetInformation actual, SnippetInformation additions, SnippetInformation deletions) {
        StringBuilder display = new StringBuilder();
        display.append("<tr><td>snippetRange:</td></tr>");
        if (! actual.isSet(SnippetInformation._Fields.SNIPPET_RANGES)){
            actual.snippetRanges = new HashSet<>();
        }
        if (! additions.isSet(SnippetInformation._Fields.SNIPPET_RANGES)){
            additions.snippetRanges = new HashSet<>();
        }
        if (! deletions.isSet(SnippetInformation._Fields.SNIPPET_RANGES)){
            deletions.snippetRanges = new HashSet<>();
        }
        if (additions.snippetRanges.isEmpty() && deletions.snippetRanges.isEmpty()) {
            return "";
        }

        Set<SnippetRange> additionsSnippetRanges = additions.getSnippetRanges();
        Set<SnippetRange> deletionsSnippetRanges = deletions.getSnippetRanges();
        Set<SnippetRange> currentSnippetRanges = actual.getSnippetRanges();
        int changeSize = 0;
        if (additionsSnippetRanges.size() > deletionsSnippetRanges.size()) {
            changeSize = additionsSnippetRanges.size() + currentSnippetRanges.size() - deletionsSnippetRanges.size();
        } else {
            changeSize = currentSnippetRanges.size();
        }

        for (int i = 0; i < changeSize; i++) {
            SnippetRange snippetRangeDeletions = getSnippetRangeByIndex(deletions, i);
            SnippetRange snippetRangeAdditions = getSnippetRangeByIndex(additions, i);
            SnippetRange snippetRange = getSnippetRangeByIndex(actual, i);
            String renderActual = "";
            String renderDeletions = "";
            String renderAdditions = "";

            for (SnippetRange._Fields field : SnippetRange._Fields.values()) {
                if (snippetRange.getFieldValue(field) == null) {
                    snippetRange.setFieldValue(field, NOT_SET);
                }
                if (snippetRangeAdditions.getFieldValue(field) == null) {
                    snippetRangeAdditions.setFieldValue(field, NOT_SET);
                }
                if (snippetRangeDeletions.getFieldValue(field) == null) {
                    snippetRangeDeletions.setFieldValue(field, NOT_SET);
                }
                if (!snippetRange.equals(snippetRangeAdditions) && !SnippetRange._Fields.INDEX.equals(field)) {
                    renderActual = renderActual + "<li>" + field.getFieldName() + ": " + StringEscapeUtils.escapeXml(snippetRange.getFieldValue(field).toString()) + "</li>";
                    renderDeletions = renderDeletions + "<li>" + field.getFieldName() + ": " + StringEscapeUtils.escapeXml(snippetRangeDeletions.getFieldValue(field).toString()) + "</li>";
                    renderAdditions = renderAdditions + "<li>" + field.getFieldName() + ": " + StringEscapeUtils.escapeXml(snippetRangeAdditions.getFieldValue(field).toString()) + "</li>";
                }
            }
            String renderTotal = "<tr><td></td><td> <ul>" + renderActual + "</ul> </td> <td> <ul>"
                                + renderDeletions + "</ul> </td> <td> <ul>"
                                + renderAdditions + "</ul> </td> </tr>";
            if (renderActual != "") {
                display.append(renderTotal);
            }
        }
        return display.toString();
    }

    private SnippetRange getSnippetRangeByIndex(SnippetInformation snippet, int index) {
        SnippetRange snippetRange;
        Iterator<SnippetRange> snippetRangeIterator = snippet.getSnippetRangesIterator();
        while (snippetRangeIterator.hasNext()) {
            snippetRange = snippetRangeIterator.next();
            if (snippetRange.getIndex() == index) {
                snippet.setIndex(0);    // Set 0 to not show Index when add or delete
                return snippetRange;
            }
        }
        return new SnippetRange();
    }

}