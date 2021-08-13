include "sw360.thrift"
include "users.thrift"
include "snippetinformation.thrift"
include "relationshipsbetweenspdxelements.thrift"
include "annotations.thrift"

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

struct SPDXDocument {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "SPDXDocument",
    4: optional string releaseId,   // Id of the parent release
    5: optional string spdxDocumentCreationInfoId,
    6: optional set<string> spdxPackageInfoIds,
    7: optional set<string> spdxFileInfoIds,
    8: optional set<SnippetInformation> snippets,
    9: optional set<RelationshipsBetweenSPDXElements> relationships,
    10: optional set<Annotations> annotations,
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