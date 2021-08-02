include "sw360.thrift"
include "users.thrift"
include "documentcreationinformation.thrift"
include "annotations.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.spdx.packageinformation
namespace php sw360.thrift.spdx.packageinformation

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
    3: optional string type = "spdxPackageInformation",
    4: optional string name,
    5: optional string SPDXID,
    6: optional string versionInfo,
    7: optional string packageFileName,
    8: optional string supplier,
    9: optional string originator,
    10: optional string downloadLocation,
    11: optional bool filesAnalyzed,
    12: optional PackageVerificationCode packageVerificationCode,
    13: optional set<CheckSum> checksums,
    14: optional string homepage,
    15: optional string sourceInfo,
    16: optional string licenseConcluded,
    17: optional set<string> licenseInfoFromFiles,
    18: optional string licenseDeclared,
    19: optional string licenseComments,
    20: optional string copyrightText,
    21: optional string summary,
    22: optional string description,
    23: optional string packageComment,
    24: optional set<ExternalReference> externalRefs,
    25: optional set<string> attributionText,
    26: optional set<Annotation> annotations,
}

struct PackageVerificationCode {
    1: optional set<string> packageVerificationCodeExcludedFiles,
    2: optional string packageVerificationCodeValue,
}

struct ExternalReference {
    1: optional string referenceCategory,
    2: optional string referenceLocator,
    3: optional string referenceType,
    4: optional string comment,
}

service PackageInformationService {
    list<PackageInformation> getPackageInformationsShort(1: set<string> ids, 2: User user);
    list<PackageInformation> getPackageInformationSummary(1: User user);
    PackageInformation getPackageInformationById(1: string id, 2: User user);
    RequestStatus addPackageInformation(1: PackageInformation packageInformation, 2: User user);
    AddDocumentRequestSummary addPackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus updatePackageInformation(1: PackageInformation packageInformation, 2: User user);
    RequestSummary updatePackageInformations(1: set<PackageInformation> packageInformations, 2: User user);
    RequestStatus deletePackageInformation(1: string id, 2: User user);
}