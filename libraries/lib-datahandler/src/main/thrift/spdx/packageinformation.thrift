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
include "sw360.thrift"
include "users.thrift"
include "documentcreationinformation.thrift"
include "annotations.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo
namespace php sw360.thrift.spdx.spdxpackageinfo

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.SW360Exception SW360Exception
typedef sw360.PaginationData PaginationData
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef documentcreationinformation.CheckSum CheckSum
typedef annotations.Annotations Annotation

struct PackageInformation {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "packageInformation",
    4: optional string spdxDocumentId,  // Id of the parent SPDX Document
    5: optional string name,                // 3.1
    6: optional string SPDXID,              // 3.2
    7: optional string versionInfo,         // 3.3
    8: optional string packageFileName,     // 3.4
    9: optional string supplier,            // 3.5
    10: optional string originator,         // 3.6
    11: optional string downloadLocation,   // 3.7
    12: optional bool filesAnalyzed,        // 3.8
    13: optional PackageVerificationCode packageVerificationCode,   // 3.9
    14: optional set<CheckSum> checksums,   // 3.10
    15: optional string homepage,           // 3.11
    16: optional string sourceInfo,         // 3.12
    17: optional string licenseConcluded,   // 3.13
    18: optional set<string> licenseInfoFromFiles,  // 3.14
    19: optional string licenseDeclared,    // 3.15
    20: optional string licenseComments,    // 3.16
    21: optional string copyrightText,      // 3.17
    22: optional string summary,            // 3.18
    23: optional string description,        // 3.19
    24: optional string packageComment,     // 3.20
    25: optional set<ExternalReference> externalRefs,   //3.21
    26: optional set<string> attributionText,   // 3.22
    27: optional set<Annotation> annotations,   // 3.23
    // Information for ModerationRequests
    30: optional DocumentState documentState,
    31: optional map<RequestedAction, bool> permissions,
    32: optional string createdBy,
}

struct PackageVerificationCode {
    1: optional set<string> excludedFiles,
    2: optional string value,
}

struct ExternalReference {
    1: optional string referenceCategory,
    2: optional string referenceLocator,
    3: optional string referenceType,
    4: optional string comment,
}

service PackageInformationService {
    list<PackageInformation> getPackageInformationSummary(1: User user);
    PackageInformation getPackageInformationById(1: string id, 2: User user);
    PackageInformation getPackageInformationForEdit(1: string id, 2: User user);
    AddDocumentRequestSummary addPackageInformation(1: PackageInformation packageInformation, 2: User user);
    AddDocumentRequestSummary addPackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus updatePackageInformation(1: PackageInformation packageInformation, 2: User user);
    RequestSummary updatePackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus updatePackageInfomationFromModerationRequest(1: PackageInformation packageInfoAdditions, 2: PackageInformation packageInfoDeletions, 3: User user);
    RequestStatus deletePackageInformation(1: string id, 2: User user);
}