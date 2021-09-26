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

        Iterator<SnippetInformation> snippetDeletionsIterator = deletions.getSnippetsIterator();
        Iterator<SnippetInformation> snippetAdditionsIterator = additions.getSnippetsIterator();
        Set<SnippetInformation> additionsSnippetInformations = additions.getSnippets();
        Set<SnippetInformation> deletionsSnippetInformations = deletions.getSnippets();

        int changeSize = deletionsSnippetInformations.size() + additionsSnippetInformations.size();

        for (int i = 0; i < changeSize; i++) {

            SnippetInformation snippetDeletions = new SnippetInformation();
            SnippetInformation snippetAdditions = new SnippetInformation();
            SnippetInformation snippet = new SnippetInformation();
            Iterator<SnippetInformation> snippetsIterator = actual.getSnippetsIterator();
            if (snippetAdditionsIterator.hasNext()) {
                snippetAdditions = snippetAdditionsIterator.next();
                while (snippetsIterator.hasNext()) {
                    snippet = snippetsIterator.next();
                    if (snippetAdditions.getIndex() == snippet.getIndex()) {
                        break;
                    } else {
                        snippet = new SnippetInformation();
                    }
                }
                snippet.setIndex(snippetAdditions.getIndex());
                snippetDeletions.setIndex(snippetAdditions.getIndex());

            } else if (snippetDeletionsIterator.hasNext()) {
                snippetDeletions = snippetDeletionsIterator.next();
                while (snippetsIterator.hasNext()) {
                    snippet = snippetsIterator.next();
                    if (snippetDeletions.getIndex() == snippet.getIndex()) {
                        break;
                    }
                }
                snippetAdditions.setIndex(snippet.getIndex());
            }
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
                            field, fieldMetaData, "");
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

        Iterator<RelationshipsBetweenSPDXElements> relationshipsDeletionsIterator = deletions.getRelationshipsIterator();
        Iterator<RelationshipsBetweenSPDXElements> relationshipsAdditionsIterator = additions.getRelationshipsIterator();
        Set<RelationshipsBetweenSPDXElements> additionsRelationshipsBetweenSPDXElementss = additions.getRelationships();
        Set<RelationshipsBetweenSPDXElements> deletionsRelationshipsBetweenSPDXElementss = deletions.getRelationships();

        int changeSize = deletionsRelationshipsBetweenSPDXElementss.size() + additionsRelationshipsBetweenSPDXElementss.size();

        for (int i = 0; i < changeSize; i++) {

            RelationshipsBetweenSPDXElements relationshipDeletions = new RelationshipsBetweenSPDXElements();
            RelationshipsBetweenSPDXElements relationshipAdditions = new RelationshipsBetweenSPDXElements();
            RelationshipsBetweenSPDXElements relationship = new RelationshipsBetweenSPDXElements();
            Iterator<RelationshipsBetweenSPDXElements> relationshipsIterator = actual.getRelationshipsIterator();
            if (relationshipsAdditionsIterator.hasNext()) {
                relationshipAdditions = relationshipsAdditionsIterator.next();
                while (relationshipsIterator.hasNext()) {
                    relationship = relationshipsIterator.next();
                    if (relationshipAdditions.getIndex() == relationship.getIndex()) {
                        break;
                    } else {
                        relationship = new RelationshipsBetweenSPDXElements();
                    }
                }
                relationship.setIndex(relationshipAdditions.getIndex());
                relationshipDeletions.setIndex(relationshipAdditions.getIndex());

            } else if (relationshipsDeletionsIterator.hasNext()) {
                relationshipDeletions = relationshipsDeletionsIterator.next();
                while (relationshipsIterator.hasNext()) {
                    relationship = relationshipsIterator.next();
                    if (relationshipDeletions.getIndex() == relationship.getIndex()) {
                        break;
                    }
                }
                relationshipAdditions.setIndex(relationship.getIndex());
            }

            for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
                FieldMetaData fieldMetaData = RelationshipsBetweenSPDXElements.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        relationship,
                        relationshipAdditions,
                        relationshipDeletions,
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

        Iterator<Annotations> annotationDeletionsIterator = deletions.getAnnotationsIterator();
        Iterator<Annotations> annotationAdditionsIterator = additions.getAnnotationsIterator();
        Set<Annotations> additionsAnnotationss = additions.getAnnotations();
        Set<Annotations> deletionsAnnotationss = deletions.getAnnotations();

        int changeSize = deletionsAnnotationss.size() + additionsAnnotationss.size();

        for (int i = 0; i < changeSize; i++) {

            Annotations annotationDeletions = new Annotations();
            Annotations annotationAdditions = new Annotations();
            Annotations annotation = new Annotations();
            Iterator<Annotations> annotationsIterator = actual.getAnnotationsIterator();
            if (annotationAdditionsIterator.hasNext()) {
                annotationAdditions = annotationAdditionsIterator.next();
                while (annotationsIterator.hasNext()) {
                    annotation = annotationsIterator.next();
                    if (annotationAdditions.getIndex() == annotation.getIndex()) {
                        break;
                    } else {
                        annotation = new Annotations();
                    }
                }
                annotation.setIndex(annotationAdditions.getIndex());
                annotationDeletions.setIndex(annotationAdditions.getIndex());

            } else if (annotationDeletionsIterator.hasNext()) {
                annotationDeletions = annotationDeletionsIterator.next();
                while (annotationsIterator.hasNext()) {
                    annotation = annotationsIterator.next();
                    if (annotationDeletions.getIndex() == annotation.getIndex()) {
                        break;
                    }
                }
                annotationAdditions.setIndex(annotation.getIndex());
            }

            for (Annotations._Fields field : Annotations._Fields.values()) {
                FieldMetaData fieldMetaData = Annotations.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        annotation,
                        annotationAdditions,
                        annotationDeletions,
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

        Iterator<OtherLicensingInformationDetected> otherLicensingDeletionsIterator = deletions.getOtherLicensingInformationDetectedsIterator();
        Iterator<OtherLicensingInformationDetected> otherLicensingAdditionsIterator = additions.getOtherLicensingInformationDetectedsIterator();
        Set<OtherLicensingInformationDetected> additionsOtherLicensingInformationDetecteds = additions.getOtherLicensingInformationDetecteds();
        Set<OtherLicensingInformationDetected> deletionsOtherLicensingInformationDetecteds = deletions.getOtherLicensingInformationDetecteds();

        int changeSize = deletionsOtherLicensingInformationDetecteds.size() + additionsOtherLicensingInformationDetecteds.size();

        for (int i = 0; i < changeSize; i++) {

            OtherLicensingInformationDetected otherLicensingDeletions = new OtherLicensingInformationDetected();
            OtherLicensingInformationDetected otherLicensingAdditions = new OtherLicensingInformationDetected();
            OtherLicensingInformationDetected otherLicensing = new OtherLicensingInformationDetected();
            Iterator<OtherLicensingInformationDetected> otherLicensingsIterator = actual.getOtherLicensingInformationDetectedsIterator();
            if (otherLicensingAdditionsIterator.hasNext()) {
                otherLicensingAdditions = otherLicensingAdditionsIterator.next();
                while (otherLicensingsIterator.hasNext()) {
                    otherLicensing = otherLicensingsIterator.next();
                    if (otherLicensingAdditions.getIndex() == otherLicensing.getIndex()) {
                        break;
                    } else {
                        otherLicensing = new OtherLicensingInformationDetected();
                    }
                }
                otherLicensing.setIndex(otherLicensingAdditions.getIndex());
                otherLicensingDeletions.setIndex(otherLicensingAdditions.getIndex());

            } else if (otherLicensingDeletionsIterator.hasNext()) {
                otherLicensingDeletions = otherLicensingDeletionsIterator.next();
                while (otherLicensingsIterator.hasNext()) {
                    otherLicensing = otherLicensingsIterator.next();
                    if (otherLicensingDeletions.getIndex() == otherLicensing.getIndex()) {
                        break;
                    }
                }
                otherLicensingAdditions.setIndex(otherLicensing.getIndex());
            }

            for (OtherLicensingInformationDetected._Fields field : OtherLicensingInformationDetected._Fields.values()) {
                FieldMetaData fieldMetaData = OtherLicensingInformationDetected.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        otherLicensing,
                        otherLicensingAdditions,
                        otherLicensingDeletions,
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

        Iterator<SnippetRange> creatorDeletionsIterator = deletions.getSnippetRangesIterator();
        Iterator<SnippetRange> creatorAdditionsIterator = additions.getSnippetRangesIterator();
        Set<SnippetRange> additionsSnippetRanges = additions.getSnippetRanges();
        Set<SnippetRange> deletionsSnippetRanges = deletions.getSnippetRanges();

        int changeSize = deletionsSnippetRanges.size() + additionsSnippetRanges.size();

        for (int i = 0; i < changeSize; i++) {

            SnippetRange creatorDeletions = new SnippetRange();
            SnippetRange creatorAdditions = new SnippetRange();
            SnippetRange creator = new SnippetRange();
            Iterator<SnippetRange> creatorsIterator = actual.getSnippetRangesIterator();
            if (creatorAdditionsIterator.hasNext()) {
                creatorAdditions = creatorAdditionsIterator.next();
                while (creatorsIterator.hasNext()) {
                    creator = creatorsIterator.next();
                    if (creatorAdditions.getIndex() == creator.getIndex()) {
                        break;
                    } else {
                        creator = new SnippetRange();
                    }
                }
                creator.setIndex(creatorAdditions.getIndex());
                creatorDeletions.setIndex(creatorAdditions.getIndex());

            } else if (creatorDeletionsIterator.hasNext()) {
                creatorDeletions = creatorDeletionsIterator.next();
                while (creatorsIterator.hasNext()) {
                    creator = creatorsIterator.next();
                    if (creatorDeletions.getIndex() == creator.getIndex()) {
                        break;
                    }
                }
                creatorAdditions.setIndex(creator.getIndex());
            }
            String render1 = "";
            String render2 = "";
            String render3 = "";
            for (SnippetRange._Fields field : SnippetRange._Fields.values()) {
                if (!SnippetRange._Fields.INDEX.equals(field)) {
                    render1 = render1 + "<li>" + field.getFieldName() + ": " + creator.getFieldValue(field) + "</li>";
                }
            }
            for (SnippetRange._Fields field : SnippetRange._Fields.values()) {
                if (!SnippetRange._Fields.INDEX.equals(field)) {
                    render2 = render2 + "<li>" + field.getFieldName() + ": " + creatorDeletions.getFieldValue(field) + "</li>";
                }
            }
            for (SnippetRange._Fields field : SnippetRange._Fields.values()) {
                if (!SnippetRange._Fields.INDEX.equals(field)) {
                    render3 = render3 + "<li>" + field.getFieldName() + ": " + creatorAdditions.getFieldValue(field) + "</li>";
                }
            }

            String renderTotal = "<tr><td></td><td> <ul>" + render1 + "</ul> </td> <td> <ul>"
                                + render2 + "</ul> </td> <td> <ul>"
                                + render3 + "</ul> </td> </tr>";
            display.append(renderTotal);
        }
        System.out.println(display.toString());
        return display.toString();
    }

}