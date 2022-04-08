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
include "snippetinformation.thrift"
include "relationshipsbetweenspdxelements.thrift"
include "annotations.thrift"
include "otherlicensinginformationdetected.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument
namespace php sw360.thrift.spdx.spdxdocument

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.SW360Exception SW360Exception
typedef sw360.PaginationData PaginationData
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef snippetinformation.SnippetInformation SnippetInformation
typedef relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements RelationshipsBetweenSPDXElements
typedef annotations.Annotations Annotations
typedef otherlicensinginformationdetected.OtherLicensingInformationDetected OtherLicensingInformationDetected

struct SPDXDocument {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "SPDXDocument",
    4: optional string releaseId,   // Id of the parent release
    5: optional string spdxDocumentCreationInfoId,  // Id of Document Creation Info
    6: optional set<string> spdxPackageInfoIds,     // Ids of Package Info
    7: optional set<string> spdxFileInfoIds,        // Ids of File Info
    8: optional set<SnippetInformation> snippets,   // 9. Snippet Information
    9: optional set<RelationshipsBetweenSPDXElements> relationships,    // 11. Relationships
    10: optional set<Annotations> annotations,      // 12. Annotations
    11: optional set<OtherLicensingInformationDetected> otherLicensingInformationDetecteds, // 10. Other Licensing Information Detected
    // Information for ModerationRequests
    20: optional DocumentState documentState,
    21: optional map<RequestedAction, bool> permissions,
    22: optional string createdBy,
}

service SPDXDocumentService {
    list<SPDXDocument> getSPDXDocumentSummary(1: User user);
    SPDXDocument getSPDXDocumentById(1: string id, 2: User user);
    SPDXDocument getSPDXDocumentForEdit(1: string id, 2: User user);
    AddDocumentRequestSummary addSPDXDocument(1: SPDXDocument spdx, 2: User user);
    RequestStatus updateSPDXDocument(1: SPDXDocument spdx, 2: User user);
    RequestStatus updateSPDXDocumentFromModerationRequest(1: SPDXDocument spdxAdditions, 2: SPDXDocument spdxDeletions, 3: User user);
    RequestStatus deleteSPDXDocument(1: string id, 2: User user);
}
