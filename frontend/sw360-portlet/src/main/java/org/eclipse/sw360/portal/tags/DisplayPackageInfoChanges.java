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
        Iterator<CheckSum> checkSumAdditionsIterator = additions.getChecksumsIterator();
        Iterator<CheckSum> checkSumDeletionsIterator = deletions.getChecksumsIterator();
        for (CheckSum checkSum : actual.getChecksums()) {
            CheckSum checkSumAdditions = new CheckSum();
            if (checkSumAdditionsIterator.hasNext()) {
                checkSumAdditions = checkSumAdditionsIterator.next();
            }
            CheckSum checkSumDeletions = new CheckSum();
            if (checkSumDeletionsIterator.hasNext()) {
                checkSumDeletions = checkSumDeletionsIterator.next();
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
        Iterator<ExternalReference> externalRefsAdditionsIterator = additions.getExternalRefsIterator();
        Iterator<ExternalReference> externalRefsDeletionsIterator = deletions.getExternalRefsIterator();
        for (ExternalReference externalRefs : actual.getExternalRefs()) {
            ExternalReference externalRefsAdditions = new ExternalReference();
            if (externalRefsAdditionsIterator.hasNext()) {
                externalRefsAdditions = externalRefsAdditionsIterator.next();
            }
            ExternalReference externalRefsDeletions = new ExternalReference();
            if (externalRefsDeletionsIterator.hasNext()) {
                externalRefsDeletions = externalRefsDeletionsIterator.next();
            }
            for (ExternalReference._Fields field : ExternalReference._Fields.values()) {
                FieldMetaData fieldMetaData = ExternalReference.metaDataMap.get(field);
                displaySimpleFieldOrSet(
                        display,
                        externalRefs,
                        externalRefsAdditions,
                        externalRefsDeletions,
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