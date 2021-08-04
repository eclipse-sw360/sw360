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
    3: optional string type = "spdxDocumentCreationInformation",
    4: optional string spdxDocumentId,  // Id of the parent SPDX Document
    5: optional string DocumentCreationInformationVersion,
    6: optional string dataLicense,
    7: optional string DocumentCreationInformationID,
    8: optional string name,
    9: optional string documentNamespace,
    10: optional set<ExternalDocumentReferences> externalDocumentRefs,
    11: optional string licenseListVersion,
    12: optional set<Creator> creator,
    13: optional string created,
    14: optional string creatorComment,
    15: optional string documentComment,
}

struct ExternalDocumentReferences {
    1: optional string externalDocumentId,
    2: optional CheckSum checksum,
    3: optional string spdxDocument,
}

struct CheckSum {
    1: optional string algorithm,
    2: optional string checksumValue,
}

struct Creator {
    1: optional string type,
    2: optional string value,
}

service DocumentCreationInformationService {
    list<DocumentCreationInformation> getDocumentCreationInformationSummary(1: User user);
    DocumentCreationInformation getDocumentCreationInformationById(1: string id, 2: User user);
    AddDocumentRequestSummary addDocumentCreationInformation(1: DocumentCreationInformation documentCreationInformation, 2: User user);
    RequestStatus updateDocumentCreationInformation(1: DocumentCreationInformation documentCreationInformation, 2: User user);
    RequestStatus deleteDocumentCreationInformation(1: string id, 2: User user);
}
