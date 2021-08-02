namespace java org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected
namespace php sw360.thrift.spdx.otherlicensinginformationdetected

struct OtherLicensingInformationDetected {
    1: optional string licenseId,
    2: optional string extractedText,
    3: optional string licenseName,
    4: optional set<string> licenseCrossRefs,
    5: optional string licenseComment,
}