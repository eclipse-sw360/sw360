namespace java org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation
namespace php sw360.thrift.spdx.snippetinformation

struct SnippetInformation {
    1: optional string SPDXID,
    2: optional string snippetFromFile,
    3: optional set<SnippetRange> snippetRanges,
    4: optional string licenseConcluded,
    5: optional set<string> licenseInfoInSnippets,
    6: optional string licenseComments,
    7: optional string copyrightText,
    8: optional string comment,
    9: optional string name,
    10: optional string snippetAttributionText,
}

struct SnippetRange {
    1: optional string rangeType,
    2: optional string startPointer,
    3: optional string endPointer,
    4: optional string reference,
}