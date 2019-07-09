/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
include "sw360.thrift"
include "attachments.thrift"
include "users.thrift"
include "vendors.thrift"
include "licenses.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.components
namespace php sw360.thrift.components

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.ReleaseRelationship ReleaseRelationship
typedef sw360.MainlineState MainlineState
typedef sw360.ProjectReleaseRelationship ProjectReleaseRelationship
typedef attachments.Attachment Attachment
typedef attachments.FilledAttachment FilledAttachment
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef vendors.Vendor Vendor
typedef licenses.License License

enum RepositoryType {
    UNKNOWN = 0,
    GIT = 14,
    CLEARCASE = 7,
    SVN = 1,
    CVS = 2,
    MERCURIAL = 16,
    PERFORCE = 4,
    VISUAL_SOURCESAFE = 6,
    BAZAAR = 11,
    ALIENBRAIN = 3,
    TEAM_FOUNDATION_SERVER = 5,
    RATIONAL_SYNERGY = 8,
    PTC_INTEGRITY = 9,
    DTR = 10,
    DARCS = 12,
    FOSSIL = 13,
    GNU_ARCH = 15,
    MONOTONE = 17,
    BIT_KEEPER = 18,
    RATIONAL_TEAM_CONCERT = 19
    RCS = 20,
 }


struct Repository {
    1: required string url,
    2: optional RepositoryType repositorytype
}

enum FossologyStatus {
    CONNECTION_FAILED = 0,

    ERROR = 1,

    NON_EXISTENT = 2,
    NOT_SENT = 3,
    INACCESSIBLE = 4,

    SENT = 10,
    SCANNING = 11,

    OPEN = 20,
    IN_PROGRESS = 21,
    CLOSED = 22,
    REJECTED = 23,

    REPORT_AVAILABLE = 30
}

enum ClearingState {
    NEW_CLEARING = 0,
    SENT_TO_FOSSOLOGY = 1,
    UNDER_CLEARING = 2,
    REPORT_AVAILABLE = 3,
    APPROVED = 4,
}

enum ECCStatus {
    OPEN = 0,
    IN_PROGRESS = 1,
    APPROVED = 2,
    REJECTED = 3,
}

struct COTSDetails{
    1: optional string usedLicense,
    2: optional string licenseClearingReportURL,
    3: optional bool containsOSS,
    4: optional bool ossContractSigned,
    5: optional string ossInformationURL,
    6: optional bool usageRightAvailable,
    7: optional string cotsResponsible,
    8: optional string clearingDeadline,
    9: optional bool sourceCodeAvailable,
}
struct EccInformation{
    1: optional ECCStatus eccStatus, // Status of ECC assessment
    2: optional string AL, // German Ausfuhrliste
    3: optional string ECCN, // European control classification number
    4: optional string assessorContactPerson, // email of ECC person
    5: optional string assessorDepartment, // department of ECC person
    6: optional string eccComment, // comments for ecc information
    7: optional string materialIndexNumber, // six digit material index number, string for convenience
    8: optional string assessmentDate, // Date - YYYY-MM-dd, date of the last editing of ECC information
}
struct ClearingInformation {
    // supplier / ec info
//    1: optional string AL, // moved to EccInformation
//    2: optional string ECCN, // moved to EccInformation
    3: optional string externalSupplierID, // foreign key fur SCM software TODO mcj move to component
//    4: optional string assessorContactPerson, // moved to EccInformation
//    5: optional string assessorDepartment, // moved to EccInformation
//    6: optional string eccComment, // moved to EccInformation
//    7: optional string materialIndexNumber, // moved to EccInformation
//    8: optional string assessmentDate, // moved to EccInformation
//    9: optional ECCStatus eccStatus, // moved to EccInformation

    // clearing related metadata part 1: strings,
    12: optional string additionalRequestInfo, //
    13: optional string evaluated, // Date - YYYY-MM-dd
    14: optional string procStart, // Date - YYYY-MM-dd
    15: optional string requestID, // foreign key
    16: optional string clearingTeam, // who did the clearing in org
    17: optional string requestorPerson, // again email who requested the clearing TODO mcj should be set automtically

    // clearing related data: release level - just boolean (Yes / no)
    31: optional bool binariesOriginalFromCommunity,
    32: optional bool binariesSelfMade,
    33: optional bool componentLicenseInformation,
    34: optional bool sourceCodeDelivery,
    35: optional bool sourceCodeOriginalFromCommunity,
    36: optional bool sourceCodeToolMade,
    37: optional bool sourceCodeSelfMade,
    38: optional bool sourceCodeCotsAvailable,
    39: optional bool screenshotOfWebSite,

    40: optional bool finalizedLicenseScanReport,
    41: optional bool licenseScanReportResult,
    42: optional bool legalEvaluation,
    43: optional bool licenseAgreement,
    44: optional string scanned,
    45: optional bool componentClearingReport,
    46: optional string clearingStandard,   // which generation of tool used
    47: optional bool readmeOssAvailable,

    // more release base data from mainline
    50: optional string comment,
    52: optional i32 countOfSecurityVn, // count of security vulnerabilities
    53: optional string externalUrl, // URL pointing to another system, TODO should be map
}

struct Release {
    // Basic information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "release",
    4: optional string cpeid, // Unique CPE id for the release object
    5: required string name, // Release name (e.g. thrift), often identical to Component name
    6: required string version, // version or release name (e.g. 0.9.1)
    7: required string componentId, // Id of the parent component
    8: optional string releaseDate,

    // information from external data sources
    9: optional  map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    // Additional informations
    10: optional set<Attachment> attachments,
    11: optional string createdOn, // Creation date YYYY-MM-dd
    12: optional Repository repository, // Repository where the release is maintained
    16: optional MainlineState mainlineState, // enum: specific, open, mainline, phaseout
    17: optional ClearingState clearingState, // TODO we probably need to map by clearing team?

    // FOSSology Information
    20: optional string fossologyId,
    21: optional map<string, FossologyStatus> clearingTeamToFossologyStatus,
    22: optional string attachmentInFossology, // id of the attachment currently in fossology

    // string details
    30: optional string createdBy, // person who created the release
    131: optional string creatorDepartment, // department of user in `createdBy`. transient
    32: optional set<string> contributors, // contributors to the release
    34: optional set<string> moderators, // people who can modify the data
    36: optional set<string> subscribers, // List of subscribers
    37: optional map<string,set<string>> roles, //customized roles with set of mail addresses

    40: optional Vendor vendor,
    41: optional string vendorId,

    50: optional ClearingInformation clearingInformation,
    51: optional set<string> languages,
    53: optional set<string> operatingSystems,
    54: optional COTSDetails cotsDetails,
    55: optional EccInformation eccInformation,
    56: optional set<string> softwarePlatforms,

    65: optional set<string> mainLicenseIds,

    // Urls for the project
    70: optional string downloadurl, // URL for download page for this release

    80: optional map<string, ReleaseRelationship> releaseIdToRelationship,    //id, comment

    // Information for ModerationRequests
    90: optional DocumentState documentState,

    200: optional map<RequestedAction, bool> permissions,
}

enum ComponentType {
    INTERNAL = 0, //internal software closed source
    OSS = 1,      //open source software
    COTS = 2,     //commercial of the shelf
    FREESOFTWARE = 3, //freeware
    INNER_SOURCE = 4, //internal software with source open for customers within own company
    SERVICE = 5,
}

struct Component {

    // General information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "component",

    5: required string name, // Component name (e.g. thrift)
    6: optional string description, // Short description about the component

    // Additional informations
    10: optional set<Attachment> attachments,
    11: optional string createdOn, // Creation date YYYY-MM-dd
    12: optional ComponentType componentType,

    // string details
    20: optional string createdBy, // person who created the component in sw360
    24: optional set<string> subscribers, // List of subscriber information
    25: optional set<string> moderators, // people who can modify the data
    26: optional string componentOwner,
    27: optional string ownerAccountingUnit,
    28: optional string ownerGroup,
    29: optional string ownerCountry,
    30: optional map<string,set<string>> roles, //customized roles with set of mail addresses

    // information from external data sources
    31: optional  map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    // Linked objects
    32: optional list<Release> releases,
    33: optional set<string> releaseIds,

    35: optional set<string> mainLicenseIds,        //Aggregate of release main licenses

    36: optional Vendor defaultVendor,
    37: optional string defaultVendorId,

    // List of keywords
    40: optional set<string> categories,
    41: optional set<string> languages,             //Aggregate of release languages
    42: optional set<string> softwarePlatforms,
    43: optional set<string> operatingSystems,      //Aggregate of release operatingSystems
    44: optional set<string> vendorNames,           //Aggregate of release vendor Fullnames

    // Urls for the component, TODO should be map
    50: optional string homepage,
    51: optional string mailinglist,
    52: optional string wiki,
    53: optional string blog,
    54: optional string wikipedia,
    55: optional string openHub,

    // Information for ModerationRequests
    70: optional DocumentState documentState,

    200: optional map<RequestedAction, bool> permissions,
}

struct ReleaseClearingStateSummary {
    1: required i32 newRelease,
    2: required i32 underClearing,
    3: required i32 underClearingByProjectTeam,
    4: required i32 reportAvailable,
    5: required i32 approved,
}

struct ReleaseLink{
    1: required string id,
    2: required string vendor,
    5: required string name,
    10: required string version,
    11: required string longName,
//    15: optional string comment,
    16: optional ReleaseRelationship releaseRelationship,
    17: optional MainlineState mainlineState,
//    20: optional string parentId,
//    21: optional list<ReleaseLink> subreleases,
    22: required bool hasSubreleases,
    25: optional string nodeId,
    26: optional string parentNodeId,

    31: optional ClearingState clearingState,
    32: optional list<Attachment> attachments,
    33: optional ComponentType componentType,
    100: optional set<string> licenseIds,
    101: optional set<string> licenseNames
}

struct ReleaseClearingStatusData {
    1: required Release release,
    2: optional ComponentType componentType,
    3: optional string projectNames, // comma separated list of project names for display; possibly abbreviated
    4: optional string mainlineStates, // comma separated list of mainline states for display; possibly abbreviated
}

service ComponentService {

    /**
     * short summary of all components identified by the given ids
     **/
    list<Component> getComponentsShort(1: set<string> ids);

    /**
     * short summary of all components visible to user
     **/
    list<Component> getComponentSummary(1: User user);

    /**
     * summary of up to `limit` components reverse ordered by `createdOn`. Negative `limit` will result in
     * all components being returned
     **/
    list<Component> getRecentComponentsSummary(1: i32 limit, 2: User user);

    /**
     * total number of components in the DB, irrespective of whether the user may see them or not
     **/
    i32 getTotalComponentsCount(1: User user);

    /**
     * short summary of all releases visible to user
     **/
    list<Release> getReleaseSummary(1: User user);

    /**
     * search components in database that match subQueryRestrictions
     **/
    list<Component> refineSearch(1: string text, 2: map<string, set<string>> subQueryRestrictions);

    /**
     * global search function to list releases which match the text argument
     */
    list<Release> searchReleases(1: string text);

    /**
     * get short summary of release by release name prefix
     **/
    list<Release> searchReleaseByNamePrefix(1: string name);

    /**
     * information for home portlet
     **/
    list<Component> getMyComponents(1: User user);

    /**
     * information for home portlet
     **/
    list<Component> getSubscribedComponents(1: User user);

    /**
     * information for home portlet
     **/
    list<Release> getSubscribedReleases(1: User user);

    /**
     * information for home portlet
     **/
    list<Release> getRecentReleases();

    // Component CRUD support
    /**
     * add component to database with user as creator,
     * return id
     **/
    AddDocumentRequestSummary addComponent(1: Component component, 2: User user);

    /**
     * get component from database filled with releases and permissions for user
     **/
    Component getComponentById(1: string id, 2: User user);

    /**
     * get component from database filled with releases and permissions for user
     * with moderation request of user applied if such request exists
     **/
    Component getComponentByIdForEdit(1: string id, 2: User user);

    /**
     * update component in database if user has permissions
     * otherwise create moderation request
     **/
    RequestStatus updateComponent(1: Component component, 2: User user);

    /**
    * update the bulk of components in database if user is admin
    **/
    RequestSummary updateComponents(1: set<Component> components, 2: User user);

    /**
     * delete component from database if user has permissions,
     * otherwise create moderation request
     **/
    RequestStatus deleteComponent(1: string id, 2: User user);

    /**
     * update component in database if user has permissions, additions and deletions are the parts of the moderation request
     * that specify which properties to add to and which to delete from component
     **/
    RequestStatus updateComponentFromModerationRequest(1: Component additions, 2: Component deletions, 3: User user);

    /**
     * merge component identified by componentSourceId into component identified by componentTargetId.
     * the componentSelection shows which data has to be set on the target. the source will be deleted afterwards.
     * if user does not have permissions, RequestStatus.FAILURE is returned
     * if any of the components has an active moderation request, it's a noop and RequestStatus.IN_USE is returned
     **/
    RequestStatus mergeComponents(1: string componentTargetId, 2: string componentSourceId, 3: Component componentSelection, 4: User user);

    // Release CRUD support
    /**
      * add release to database with user as creator,
      * return id
      **/
    AddDocumentRequestSummary addRelease(1: Release release, 2: User user);

    /**
      * get release from database filled with vendor and permissions for user
      **/
    Release getReleaseById(1: string id, 2: User user);

     /**
       * get release from database filled with vendor and permissions for user
       * with moderation request of user applied if such request exists
       **/
    Release getReleaseByIdForEdit(1: string id, 2: User user);

    /**
      * get short summary of all releases specified by ids
      **/
    list<Release> getReleasesByIdsForExport(1: set<string> ids);

    /**
      * get short summary of all releases specified by ids, user is not used
      **/
    list<Release> getReleasesById(1: set<string> ids, 2: User user);

    /**
      * get summary of all releases specified by ids, user is not used
      **/
    list<Release> getFullReleasesById(1: set<string> ids, 2: User user);

     /**
       * get summary of all releases specified by ids, filled with permissions for user
       **/
    list<Release> getReleasesWithPermissions(1: set<string> ids, 2: User user);

    /**
      * get summary of all releases with vendor specified by id, filled with permissions for user
      **/
    list<Release> getReleasesFromVendorId(1: string id, 2: User user);

    /**
      * get short summary of all releases with vendor specified by ids
      **/
    list<Release> getReleasesFromVendorIds(1: set<string> ids);

    /**
     * update release in database if user has permissions
     * otherwise create moderation request
     **/
    RequestStatus updateRelease(1: Release release, 2: User user);

    /**
     * update release called by fossology service
     * update release in database if user has permissions
     * otherwise create moderation request
     **/
    RequestStatus updateReleaseFossology(1: Release release, 2: User user);

    /**
     * update the bulk of releases in database if user is admin
     **/
    RequestSummary updateReleases(1: set<Release> releases, 2: User user);

    /**
     * delete release from database if user has permissions
     * otherwise create moderation request
     **/
    RequestStatus deleteRelease(1: string id, 2: User user);

    /**
     * update release in database if user has permissions, additions and deletions are the parts of the moderation request
     * that specify which properties to add to and which to delete from release
     **/
    RequestStatus updateReleaseFromModerationRequest(1: Release additions, 2: Release deletions, 3: User user);

    /**
     * get summaries of releases of component specified by id, filled with permissions for user
     **/
    list<Release> getReleasesByComponentId(1: string id, 2: User user);

    /**
     * get components belonging to linked releases of the release specified by releaseId
     **/
    set <Component> getUsingComponentsForRelease(1: string releaseId );

    /**
     * get components belonging to linked releases of the releases specified by releaseId
     **/
    set <Component> getUsingComponentsForComponent(1: set <string> releaseId );

    /**
     * check if release is used by other releases, components or projects
     **/
    bool releaseIsUsed(1: string releaseId);

     /**
       * check if one of the releases of the compnent is used by other releases, components or projects
       **/
    bool componentIsUsed(1: string componentId);

    // These two methods are needed because there is no rights management needed to subscribe
    /**
     *   subscribe user for component (no permission necessary)
     **/
    RequestStatus subscribeComponent(1: string id, 2: User user);

    /**
     *   subscribe user for release (no permission necessary)
     **/
    RequestStatus subscribeRelease(1: string id, 2: User user);

    /**
     *   unsubscribe user from component (no permission necessary)
     **/
    RequestStatus unsubscribeComponent(1: string id, 2: User user);

    /**
     *   unsubscribe user from release (no permission necessary)
     **/
    RequestStatus unsubscribeRelease(1: string id, 2: User user);

    /**
     * Make a list of components for Excel export and component importer
     **/
    list<Component> getComponentSummaryForExport();

    /**
     * Make a list of components for component importer
     **/
    list<Component> getComponentDetailedSummaryForExport();

    /**
     * get export summary for components whose name is matching parameter name
     **/
    list<Component> searchComponentForExport(1: string name);

    /**
     *  get component with fossologyId equal to uploadId, filled with releases and main licenses,
     *  releases are filled with vendor
     **/
    Component getComponentForReportFromFossologyUploadId(1: string uploadId );

    /**
     * get attachments with document type "source" of release with releaseId
     **/
    set<Attachment> getSourceAttachments(1:string releaseId);

    /**
     *  make releaseLinks from linked releases of a project in order to display project
     **/
    list<ReleaseLink> getLinkedReleases(1: map<string, ProjectReleaseRelationship> relations);

    /**
     *  make releaseLinks from linked releases of a release in order to display in release detail view
     **/
    list<ReleaseLink> getLinkedReleaseRelations(1: map<string, ReleaseRelationship> relations);

    /**
     * get all attachmentContentIds of attachments of projects, components and releases
     * used for attachment cleanup and component import
     **/
    set<string> getUsedAttachmentContentIds();

    /**
     * Method to ensure uniqueness of identifiers, used by database sanitation portlet,
     * return map of name to ids
     **/
    map <string, list<string>> getDuplicateComponents();

    /**
     * Method to ensure uniqueness of identifiers, used by database sanitation portlet,
     * return map of name to ids
     **/
    map <string, list<string>> getDuplicateReleases();

    /**
     * Method to ensure uniqueness of identifiers, used by database sanitation portlet,
     * return map of name to ids
     **/
    map <string, list<string>> getDuplicateReleaseSources();

   /**
     * get a set of components based on the external id external ids can have multiple values to one key
     */
    set<Component> searchComponentsByExternalIds(1: map<string, set<string>> externalIds);

   /**
     * get a set of releases based on the external id external ids can have multiple values to one key
     */
    set<Release> searchReleasesByExternalIds(1: map<string, set<string>> externalIds);
}
