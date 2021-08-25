namespace java org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected
namespace php sw360.thrift.spdx.otherlicensinginformationdetected

struct OtherLicensingInformationDetected {
    1: optional string licenseId,               // 6.1
    2: optional string extractedText,           // 6.2
    3: optional string licenseName,             // 6.3
    4: optional set<string> licenseCrossRefs,   // 6.4
    5: optional string licenseComment,          // 6.5
}