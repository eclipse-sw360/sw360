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

namespace java org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation
namespace php sw360.thrift.spdx.documentcreationinformation

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.SW360Exception SW360Exception
typedef sw360.PaginationData PaginationData
typedef users.User User
typedef users.RequestedAction RequestedAction

struct DocumentCreationInformation {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "documentCreationInformation",
    4: optional string spdxDocumentId,  // Id of the parent SPDX Document
    5: optional string spdxVersion,     // 6.1
    6: optional string dataLicense,     // 6.2
    7: optional string SPDXID,          // 6.3
    8: optional string name,            // 6.4
    9: optional string documentNamespace,   // 6.5
    10: optional set<ExternalDocumentReferences> externalDocumentRefs,  // 6.6
    11: optional string licenseListVersion, // 6.7
    12: optional set<Creator> creator,      // 6.8
    13: optional string created,            // 6.9
    14: optional string creatorComment,     // 6.10
    15: optional string documentComment,    // 6.11
    // Information for ModerationRequests
    20: optional DocumentState documentState,
    21: optional map<RequestedAction, bool> permissions,
    22: optional string createdBy,
}

struct ExternalDocumentReferences {
    1: optional string externalDocumentId,
    2: optional CheckSum checksum,
    3: optional string spdxDocument,
    4: optional i32 index,
}

struct CheckSum {
    1: optional string algorithm,
    2: optional string checksumValue,
    3: optional i32 index,
}

struct Creator {
    1: optional string type,
    2: optional string value,
    3: optional i32 index,
}

service DocumentCreationInformationService {
    list<DocumentCreationInformation> getDocumentCreationInformationSummary(1: User user);
    DocumentCreationInformation getDocumentCreationInformationById(1: string id, 2: User user);
    DocumentCreationInformation getDocumentCreationInfoForEdit(1: string id, 2: User user);
    AddDocumentRequestSummary addDocumentCreationInformation(1: DocumentCreationInformation documentCreationInformation, 2: User user);
    RequestStatus updateDocumentCreationInformation(1: DocumentCreationInformation documentCreationInformation, 2: User user);
    RequestStatus updateDocumentCreationInfomationFromModerationRequest(1: DocumentCreationInformation documentCreationInfoAdditions, 2: DocumentCreationInformation documentCreationInfoDeletions, 3: User user);
    RequestStatus deleteDocumentCreationInformation(1: string id, 2: User user);
}
