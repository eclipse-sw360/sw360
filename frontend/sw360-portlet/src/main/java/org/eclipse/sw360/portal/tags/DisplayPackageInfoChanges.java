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
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
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
 * Display the fields that have changed in the SPDX Package Info
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
                    case PACKAGE_VERIFICATION_CODE:
                    case ANNOTATIONS:
                    case CHECKSUMS:
                    case EXTERNAL_REFS:
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
            String packageVerificationCodeRenderString = renderPackageVerificationCode();
            String annotaionsRenderString = renderAnnotaions();
            String checkSumRenderString = renderCheckSum();
            String externalReferenceRenderString = renderExternalReference();
            jspWriter.print(renderString + packageVerificationCodeRenderString.toString() + checkSumRenderString.toString() + externalReferenceRenderString.toString() + annotaionsRenderString.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private boolean ensureSomethingTodoAndNoNull(PackageInformation._Fields field) {
        if (!deletions.isSet(field) && !additions.isSet(field)) {
            return false;
        }

        if (field == PackageInformation._Fields.CHECKSUMS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == PackageInformation._Fields.EXTERNAL_REFS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == PackageInformation._Fields.ANNOTATIONS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        } else if (field == PackageInformation._Fields.PACKAGE_VERIFICATION_CODE){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashSet<>());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new HashSet<>());
            }
        }

        return true;
    }

    private String renderCheckSum() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(PackageInformation._Fields.CHECKSUMS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(PackageInformation._Fields.CHECKSUMS)){
            actual.checksums = new HashSet<>();
        }
        Iterator<CheckSum> checkSumDeletionsIterator = deletions.getChecksumsIterator();
        Iterator<CheckSum> checkSumAdditionsIterator = additions.getChecksumsIterator();
        Set<CheckSum> additionsCheckSums = additions.getChecksums();
        Set<CheckSum> deletionsCheckSums = deletions.getChecksums();

        int changeSize = deletionsCheckSums.size() + additionsCheckSums.size();

        for (int i = 0; i < changeSize; i++) {

            CheckSum checkSumDeletions = new CheckSum();
            CheckSum checkSumAdditions = new CheckSum();
            CheckSum checkSum = new CheckSum();
            Iterator<CheckSum> checkSumsIterator = actual.getChecksumsIterator();
            if (checkSumAdditionsIterator.hasNext()) {
                checkSumAdditions = checkSumAdditionsIterator.next();
                while (checkSumsIterator.hasNext()) {
                    checkSum = checkSumsIterator.next();
                    if (checkSumAdditions.getIndex() == checkSum.getIndex()) {
                        break;
                    } else {
                        checkSum = new CheckSum();
                    }
                }
                checkSum.setIndex(checkSumAdditions.getIndex());
                checkSumDeletions.setIndex(checkSumAdditions.getIndex());

            } else if (checkSumDeletionsIterator.hasNext()) {
                checkSumDeletions = checkSumDeletionsIterator.next();
                while (checkSumsIterator.hasNext()) {
                    checkSum = checkSumsIterator.next();
                    if (checkSumDeletions.getIndex() == checkSum.getIndex()) {
                        break;
                    }
                }
                checkSumAdditions.setIndex(checkSum.getIndex());
            }
            for (CheckSum._Fields field : CheckSum._Fields.values()) {
                FieldMetaData fieldMetaData = CheckSum.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        checkSum,
                        checkSumAdditions,
                        checkSumDeletions,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.checksum")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private String renderAnnotaions() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(PackageInformation._Fields.ANNOTATIONS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(PackageInformation._Fields.ANNOTATIONS)){
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

    private String renderExternalReference() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(PackageInformation._Fields.EXTERNAL_REFS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(PackageInformation._Fields.EXTERNAL_REFS)){
            actual.externalRefs = new HashSet<>();
        }
        Iterator<ExternalReference> externalRefDeletionsIterator = deletions.getExternalRefsIterator();
        Iterator<ExternalReference> externalRefAdditionsIterator = additions.getExternalRefsIterator();
        Set<ExternalReference> additionsExternalReferences = additions.getExternalRefs();
        Set<ExternalReference> deletionsExternalReferences = deletions.getExternalRefs();

        int changeSize = deletionsExternalReferences.size() + additionsExternalReferences.size();

        for (int i = 0; i < changeSize; i++) {

            ExternalReference externalRefDeletions = new ExternalReference();
            ExternalReference externalRefAdditions = new ExternalReference();
            ExternalReference externalRef = new ExternalReference();
            Iterator<ExternalReference> externalRefsIterator = actual.getExternalRefsIterator();
            if (externalRefAdditionsIterator.hasNext()) {
                externalRefAdditions = externalRefAdditionsIterator.next();
                while (externalRefsIterator.hasNext()) {
                    externalRef = externalRefsIterator.next();
                    if (externalRefAdditions.getIndex() == externalRef.getIndex()) {
                        break;
                    } else {
                        externalRef = new ExternalReference();
                    }
                }
                externalRef.setIndex(externalRefAdditions.getIndex());
                externalRefDeletions.setIndex(externalRefAdditions.getIndex());

            } else if (externalRefDeletionsIterator.hasNext()) {
                externalRefDeletions = externalRefDeletionsIterator.next();
                while (externalRefsIterator.hasNext()) {
                    externalRef = externalRefsIterator.next();
                    if (externalRefDeletions.getIndex() == externalRef.getIndex()) {
                        break;
                    }
                }
                externalRefAdditions.setIndex(externalRef.getIndex());
            }
            for (ExternalReference._Fields field : ExternalReference._Fields.values()) {
                FieldMetaData fieldMetaData = ExternalReference.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        externalRef,
                        externalRefAdditions,
                        externalRefDeletions,
                        field, fieldMetaData, "");
            }
        }
        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.external.references")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }

    private String renderPackageVerificationCode() {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        if (!ensureSomethingTodoAndNoNull(PackageInformation._Fields.PACKAGE_VERIFICATION_CODE)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(PackageInformation._Fields.PACKAGE_VERIFICATION_CODE)){
            actual.packageVerificationCode = new PackageVerificationCode();
        }

        for (PackageVerificationCode._Fields field : PackageVerificationCode._Fields.values()) {
            FieldMetaData fieldMetaData = PackageVerificationCode.metaDataMap.get(field);
            displaySimpleFieldOrSet(
                    display,
                    actual.getPackageVerificationCode(),
                    additions.getPackageVerificationCode(),
                    deletions.getPackageVerificationCode(),
                    field, fieldMetaData, "");
        }

        return "<h3>"+LanguageUtil.get(resourceBundle,"changes.in.package.verification.code")+ "</h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                LanguageUtil.get(resourceBundle,"field.name"), LanguageUtil.get(resourceBundle,"current.value"),
                LanguageUtil.get(resourceBundle,"former.value"), LanguageUtil.get(resourceBundle,"suggested.value"))
                + display.toString() + "</tbody></table>";
    }
}