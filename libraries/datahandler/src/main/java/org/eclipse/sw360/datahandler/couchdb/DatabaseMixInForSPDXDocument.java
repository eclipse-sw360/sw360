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
package org.eclipse.sw360.datahandler.couchdb;

import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Repository;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class DatabaseMixInForSPDXDocument {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setAnnotator",
        "setAnnotationDate",
        "setAnnotationType",
        "setSpdxIdRef",
        "setAnnotationComment",
        "setIndex",
        "index"
    })
    public static abstract class AnnotationsMixin extends Annotations {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setIndex",
        "index",
        "setAlgorithm",
        "setChecksumValue"
    })
    public static abstract class CheckSumMixin extends CheckSum {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setReferenceType",
        "setIndex",
        "setReferenceCategory",
        "index",
        "setComment",
        "setReferenceLocator"
    })
    public static abstract class ExternalReferenceMixin extends ExternalReference {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setExternalDocumentId",
        "setChecksum",
        "setSpdxDocument",
        "setIndex",
        "index"
    })
    public static abstract class ExternalDocumentReferencesMixin extends ExternalDocumentReferences {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setSPDXID",
        "setSnippetFromFile",
        "setSnippetRanges",
        "snippetRangesSize",
        "snippetRangesIterator",
        "setLicenseConcluded",
        "setLicenseInfoInSnippets",
        "licenseInfoInSnippetsSize",
        "licenseInfoInSnippetsIterator",
        "setLicenseComments",
        "setCopyrightText",
        "setComment",
        "setName",
        "setSnippetAttributionText",
        "setIndex",
        "spdxid",
        "index"
    })
    public static abstract class SnippetInformationMixin extends SnippetInformation {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setRangeType",
        "setStartPointer",
        "setEndPointer",
        "setReference",
        "setIndex",
        "index"
    })
    public static abstract class SnippetRangeMixin extends SnippetRange {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setSpdxElementId",
        "setRelationshipType",
        "setRelatedSpdxElement",
        "setRelationshipComment",
        "setIndex",
        "index"
    })
    public static abstract class RelationshipsBetweenSPDXElementsMixin extends RelationshipsBetweenSPDXElements {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setLicenseId",
        "setExtractedText",
        "setLicenseName",
        "setLicenseCrossRefs",
        "licenseCrossRefsSize",
        "licenseCrossRefsIterator",
        "setLicenseComment",
        "setIndex",
        "index"
    })
    public static abstract class OtherLicensingInformationDetectedMixin extends OtherLicensingInformationDetected {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setExcludedFiles",
        "excludedFilesSize",
        "excludedFilesIterator",
        "setIndex",
        "setValue"
    })
    public static abstract class PackageVerificationCodeMixin extends PackageVerificationCode {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({
        "setAnnotations",
        "type",
        "setPermissions",
        "permissions",
        "setAttributionText",
        "setId",
        "id",
        "attributionTextSize",
        "setLicenseDeclared",
        "setSupplier",
        "setLicenseComments",
        "checksumsIterator",
        "setFilesAnalyzed",
        "setPackageFileName",
        "setPackageComment",
        "setVersionInfo",
        "revision",
        "setExternalRefs",
        "setName",
        "permissionsSize",
        "setDescription",
        "setSourceInfo",
        "setHomepage",
        "setRevision",
        "setLicenseInfoFromFiles",
        "checksumsSize",
        "setChecksums",
        "annotationsSize",
        "setSummary",
        "setOriginator",
        "externalRefsSize",
        "setPackageVerificationCode",
        "setLicenseConcluded",
        "spdxDocumentId",
        "setType",
        "documentState",
        "setSpdxDocumentId",
        "setDownloadLocation",
        "licenseInfoFromFilesSize",
        "setCopyrightText",
        "setValue",
        "excludedFilesSize",
        "setExcludedFiles",
        "createdBy",
        "setCreatedBy",
        "setDocumentState",
        "setSPDXID",
        "annotationsIterator",
        "externalRefsIterator",
        "attributionTextIterator",
        "index",
        "setIndex",
        "relationshipsSize",
        "spdxid",
        "setRelationships",
        "relationshipsIterator",
        "licenseInfoFromFilesIterator"
    })
    public static abstract class PackageInformationMixin extends PackageInformation {
    }
}
