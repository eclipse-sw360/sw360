include "sw360.thrift"
include "users.thrift"
include "documentcreationinformation.thrift"
include "annotations.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.spdxpackageinfo
namespace php sw360.thrift.spdxpackageinfo

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
    5: optional string name,
    6: optional string SPDXID,
    7: optional string versionInfo,
    8: optional string packageFileName,
    9: optional string supplier,
    10: optional string originator,
    11: optional string downloadLocation,
    12: optional bool filesAnalyzed,
    13: optional PackageVerificationCode packageVerificationCode,
    14: optional set<CheckSum> checksums,
    15: optional string homepage,
    16: optional string sourceInfo,
    17: optional string licenseConcluded,
    18: optional set<string> licenseInfoFromFiles,
    19: optional string licenseDeclared,
    20: optional string licenseComments,
    21: optional string copyrightText,
    22: optional string summary,
    23: optional string description,
    24: optional string packageComment,
    25: optional set<ExternalReference> externalRefs,
    26: optional set<string> attributionText,
    27: optional set<Annotation> annotations,
    // Information for ModerationRequests
    30: optional DocumentState documentState,
    31: optional map<RequestedAction, bool> permissions,
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
    AddDocumentRequestSummary addPackageInformation(1: PackageInformation packageInformation, 2: User user);
    AddDocumentRequestSummary addPackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus updatePackageInformation(1: PackageInformation packageInformation, 2: User user);
    RequestSummary updatePackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus deletePackageInformation(1: string id, 2: User user);
}