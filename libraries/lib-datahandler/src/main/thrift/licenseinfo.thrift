/*
 * Copyright Siemens AG, 2016-2018. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "components.thrift"
include "projects.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.licenseinfo
namespace php sw360.thrift.licenseinfo

typedef components.Release Release
typedef components.Attachment Attachment
typedef users.User User
typedef projects.Project Project
typedef projects.ObligationStatusInfo ObligationStatusInfo

enum LicenseInfoRequestStatus{
    SUCCESS = 0,
    NO_APPLICABLE_SOURCE = 1,
    FAILURE = 2,
}

enum OutputFormatVariant{
    REPORT = 0,
    DISCLOSURE = 1,
}

struct OutputFormatInfo {
    1: optional string fileExtension,
    2: optional string description,
    3: optional string generatorClassName,
    4: bool isOutputBinary,
    5: optional string mimeType,
    6: optional OutputFormatVariant variant,
}

struct LicenseNameWithText {
    1: optional string licenseName,
    2: optional string licenseText,
    /* 3: optional i32 id, removed since only used as counter in XhtmlGenerator */
    4: optional string acknowledgements,
    5: optional string licenseSpdxId,
    6: optional string type,
    7: optional set<ObligationAtProject> obligationsAtProject,
}

struct LicenseInfo {
    10: optional list<string> filenames, // actual sources used
//    11: required string filetype, // actual parser type used

    20: optional set<string> copyrights,
    21: optional set<LicenseNameWithText> licenseNamesWithTexts,
    22: optional string sha1Hash,
    23: optional string componentName,
    24: optional set<string> concludedLicenseIds,
    25: optional i32 totalObligations,
}

struct LicenseInfoParsingResult {
    1: required LicenseInfoRequestStatus status,
    2: optional string message,
    3: optional LicenseInfo licenseInfo,

    // identifying data of the scanned release, if applicable
    30: optional string vendor,
    31: optional string name,
    32: optional string version,
    33: optional string componentType,
    34: optional Release release,
    35: optional string attachmentContentId,
}

enum ObligationInfoRequestStatus {
    SUCCESS = 0,
    NO_APPLICABLE_SOURCE = 1,
    FAILURE = 2,
}

struct ObligationParsingResult {
    1: required ObligationInfoRequestStatus status,
    2: optional string message,
    3: optional list<ObligationAtProject> obligationsAtProject,
    4: optional Release release,
    5: optional string attachmentContentId,
}

struct ObligationAtProject {
    1: required string topic,
    2: required string text,
    3: required list<string> licenseIDs,
    4: optional ObligationStatusInfo obligationStatusInfo,
    5: optional string id,
    6: optional string type
}

struct LicenseInfoFile {
    1: required OutputFormatInfo outputFormatInfo,
    2: required binary              generatedOutput,
}

struct LicenseObligationsStatusInfo {
    1: list<LicenseInfoParsingResult> licenseInfoResults,
    2: map<string, ObligationStatusInfo> obligationStatusMap
}

service LicenseInfoService {

    /**
     * parses the attachment of one release for license information and returns the result.
     */
    list<LicenseInfoParsingResult> getLicenseInfoForAttachment(1: Release release, 2: string attachmentContentId, 3: bool includeConcludedLicense, 4: User user);

    /**
     * parses the attachment of one release for obligations information and returns the result.
     */
    list<ObligationParsingResult> getObligationsForAttachment(1: Release release, 2: string attachmentContentId, 3: User user);

    /**
     * mark the obligation associated with excluded release,
     * to map the parsed obligation and it's status.
     */
    LicenseObligationsStatusInfo getProjectObligationStatus(1: map<string, ObligationStatusInfo> obligationStatusMap, 2: list<LicenseInfoParsingResult> licenseInfoResults, 3: map<string, string> excludedReleaseIdToAcceptedCLI);

    /**
     * create the mapping between license and obligations
     */
    LicenseInfoParsingResult createLicenseToObligationMapping(1: LicenseInfoParsingResult licenseInfoResult, 2: ObligationParsingResult obligationInfoResult);

    /**
     * get a copyright and license information file on all linked releases and linked releases of linked projects (recursively)
     * output format as specified by outputType
     */
    LicenseInfoFile getLicenseInfoFile(1: Project project, 2: User user, 3: string outputGeneratorClassName, 4: map<string, map<string, bool>> releaseIdsToSelectedAttachmentIds, 5: map<string, set<LicenseNameWithText>> excludedLicensesPerAttachment, 6: string externalIds, 7: string fileName);

    /**
      * returns all available output types
      */
    list<OutputFormatInfo> getPossibleOutputFormats();

    /**
     * returns file extension that is applicable when licenseInfo is generated by the generator class
     */
    OutputFormatInfo getOutputFormatInfoForGeneratorClass(1: string generatorClassName);

    /**
     * returns the default license info header text
     */
    string getDefaultLicenseInfoHeaderText();

    /**
     * returns the default obligations text
     */
    string getDefaultObligationsText();

}
