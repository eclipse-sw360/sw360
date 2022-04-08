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

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;

import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import static org.eclipse.sw360.portal.tags.TagUtils.*;

public class DisplayDocumentCreationInfoChanges extends UserAwareTag {
    private DocumentCreationInformation actual;
    private DocumentCreationInformation additions;
    private DocumentCreationInformation deletions;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setActual(DocumentCreationInformation actual) {
        this.actual = actual;
    }

    public void setAdditions(DocumentCreationInformation additions) {
        this.additions = additions;
    }

    public void setDeletions(DocumentCreationInformation deletions) {
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
            for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
                switch (field) {
                    // ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case EXTERNAL_DOCUMENT_REFS:
                    case CREATOR:
                        break;
                    default:
                        FieldMetaData fieldMetaData = DocumentCreationInformation.metaDataMap.get(field);
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
            String externalDocumentRefsRenderString = renderExternalDocumentRefs();
            String creatorRenderString = renderCreator();
            jspWriter.print(renderString + externalDocumentRefsRenderString.toString() + creatorRenderString.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private boolean ensureSomethingTodoAndNoNull(DocumentCreationInformation._Fields field) {
        if (!deletions.isSet(field) && !additions.isSet(field)) {
            return false;
        }

        if (field == DocumentCreationInformation._Fields.EXTERNAL_DOCUMENT_REFS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == DocumentCreationInformation._Fields.CREATOR){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        }
        return true;
    }

    private String renderExternalDocumentRefs() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(DocumentCreationInformation._Fields.EXTERNAL_DOCUMENT_REFS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(DocumentCreationInformation._Fields.EXTERNAL_DOCUMENT_REFS)){
            actual.externalDocumentRefs = new HashSet<>();
        }

        Set<ExternalDocumentReferences> additionsExternalDocumentRefs = additions.getExternalDocumentRefs();
        Set<ExternalDocumentReferences> deletionsExternalDocumentRefs = deletions.getExternalDocumentRefs();
        Set<ExternalDocumentReferences> currentExternalDocumentRefs = actual.getExternalDocumentRefs();
        int changeSize = 0;
        if (additionsExternalDocumentRefs.size() > deletionsExternalDocumentRefs.size()) {
            changeSize = additionsExternalDocumentRefs.size() + currentExternalDocumentRefs.size() - deletionsExternalDocumentRefs.size();
        } else {
            changeSize = currentExternalDocumentRefs.size();
        }

        for (int i = 0; i < changeSize; i++) {
            ExternalDocumentReferences externalDocumentRefDeletions = getExternalDocumentRefsByIndex(deletions, i);
            ExternalDocumentReferences externalDocumentRefAdditions = getExternalDocumentRefsByIndex(additions, i);
            ExternalDocumentReferences externalDocumentRef = getExternalDocumentRefsByIndex(actual, i);
            String checkSumRendeString = null;

            for (ExternalDocumentReferences._Fields field : ExternalDocumentReferences._Fields.values()) {
                FieldMetaData fieldMetaData = ExternalDocumentReferences.metaDataMap.get(field);
                if (field == ExternalDocumentReferences._Fields.CHECKSUM) {
                    checkSumRendeString = renderCheckSum(externalDocumentRef, externalDocumentRefAdditions, externalDocumentRefDeletions);
                } else {
                    displaySimpleFieldOrSet(
                            display,
                            externalDocumentRef,
                            externalDocumentRefAdditions,
                            externalDocumentRefDeletions,
                            field, fieldMetaData, "", false);
                }
            }
            if (checkSumRendeString != null) {
                display.append(checkSumRendeString);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.external.document.references")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private ExternalDocumentReferences getExternalDocumentRefsByIndex(DocumentCreationInformation document, int index) {
        ExternalDocumentReferences externalDocumentRefs;
        Iterator<ExternalDocumentReferences> externalDocumentRefsIterator = document.getExternalDocumentRefsIterator();
        while (externalDocumentRefsIterator.hasNext()) {
            externalDocumentRefs = externalDocumentRefsIterator.next();
            if (externalDocumentRefs.getIndex() == index) {
                externalDocumentRefs.setIndex(0);   // Set 0 to not show Index when add or delete
                return externalDocumentRefs;
            }
        }
        return new ExternalDocumentReferences();
    }

    private String renderCreator() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(DocumentCreationInformation._Fields.CREATOR)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(DocumentCreationInformation._Fields.CREATOR)){
            actual.creator = new HashSet<>();
        }

        Set<Creator> additionsCreators = additions.getCreator();
        Set<Creator> deletionsCreators = deletions.getCreator();
        Set<Creator> currentCreators = actual.getCreator();
        int changeSize = 0;
        if (additionsCreators.size() > deletionsCreators.size()) {
            changeSize = additionsCreators.size() + currentCreators.size() - deletionsCreators.size();
        } else {
            changeSize = currentCreators.size();
        }

        for (int i = 0; i < changeSize; i++) {
            Creator creatorDeletions = getCreatorByIndex(deletions, i);
            Creator creatorAdditions = getCreatorByIndex(additions, i);
            Creator creator = getCreatorByIndex(actual, i);

            for (Creator._Fields field : Creator._Fields.values()) {
                FieldMetaData fieldMetaData = Creator.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        creator,
                        creatorAdditions,
                        creatorDeletions,
                        field, fieldMetaData, "", false);
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.creator")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private Creator getCreatorByIndex(DocumentCreationInformation document, int index) {
        Creator creator;
        Iterator<Creator> creatorIterator = document.getCreatorIterator();
        while (creatorIterator.hasNext()) {
            creator = creatorIterator.next();
            if (creator.getIndex() == index) {
                creator.setIndex(0);    // Set 0 to not show Index when add or delete
                return creator;
            }
        }
        return new Creator();
    }

    private String renderCheckSum(ExternalDocumentReferences actualChecsum, ExternalDocumentReferences additionsChecsum, ExternalDocumentReferences deletionsChecsum) {

        if (!deletionsChecsum.isSet(ExternalDocumentReferences._Fields.CHECKSUM)
            && !additionsChecsum.isSet(ExternalDocumentReferences._Fields.CHECKSUM)) {
            return "";
        }

        if (!actualChecsum.isSet(ExternalDocumentReferences._Fields.CHECKSUM)) {
            actualChecsum.checksum = new CheckSum();
            actualChecsum.checksum.algorithm = NOT_SET;
            actualChecsum.checksum.checksumValue = NOT_SET;
        }

        if (!deletionsChecsum.isSet(ExternalDocumentReferences._Fields.CHECKSUM)) {
            deletionsChecsum.checksum = new CheckSum();
            deletionsChecsum.checksum.algorithm = NOT_SET;
            deletionsChecsum.checksum.checksumValue = NOT_SET;
        }

        if (!additionsChecsum.isSet(ExternalDocumentReferences._Fields.CHECKSUM)) {
            additionsChecsum.checksum = new CheckSum();
            additionsChecsum.checksum.algorithm = NOT_SET;
            additionsChecsum.checksum.checksumValue = NOT_SET;
        }

        if (actualChecsum.checksum.algorithm.equals(additionsChecsum.checksum.algorithm)
            && actualChecsum.checksum.checksumValue.equals(additionsChecsum.checksum.checksumValue)) {
            return "";
        }

        String display = "<tr> <td>CheckSum:</td> <td> <ul> <li>algorithm: "
                    + StringEscapeUtils.escapeXml(actualChecsum.checksum.algorithm) + "</li> <li>checksumValue: "
                    + StringEscapeUtils.escapeXml(actualChecsum.checksum.checksumValue) +  "</li> </ul> </td> <td> <li>algorithm: "
                    + StringEscapeUtils.escapeXml(deletionsChecsum.checksum.algorithm) + "</li> <li>checksumValue: "
                    + StringEscapeUtils.escapeXml(deletionsChecsum.checksum.checksumValue) +  "</li> </ul> </td> <td> <li>algorithm: "
                    + StringEscapeUtils.escapeXml(additionsChecsum.checksum.algorithm) + " </li> <li>checksumValue: "
                    + StringEscapeUtils.escapeXml(additionsChecsum.checksum.checksumValue) + "</li> </ul> </td> </tr>";
        return display;
    }

}
