/*
 * Copyright Siemens AG, 2014-2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "attachments.thrift"
include "vendors.thrift"
include "components.thrift"
include "sw360.thrift"
include "licenses.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.projects
namespace php sw360.thrift.projects

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.Visibility Visibility
typedef sw360.ReleaseRelationship ReleaseRelationship
typedef sw360.MainlineState MainlineState
typedef sw360.ProjectReleaseRelationship ProjectReleaseRelationship
typedef sw360.ObligationStatus ObligationStatus
typedef sw360.SW360Exception SW360Exception
typedef sw360.ClearingRequestState ClearingState
typedef sw360.ClearingRequestPriority ClearingPriority
typedef sw360.ClearingRequestType ClearingType
typedef sw360.ClearingRequestSize ClearingSize
typedef sw360.Comment Comment
typedef sw360.PaginationData PaginationData
typedef components.Release Release
typedef components.ReleaseClearingStateSummary ReleaseClearingStateSummary
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef attachments.Attachment Attachment
typedef components.ReleaseLink ReleaseLink
typedef components.ReleaseClearingStatusData ReleaseClearingStatusData
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef licenses.Obligation Obligation
typedef licenses.ObligationType ObligationType
typedef licenses.ObligationLevel ObligationLevel
typedef vendors.Vendor Vendor
typedef components.ReleaseNode ReleaseNode
typedef sw360.ProjectPackageRelationship ProjectPackageRelationship

const string CLEARING_TEAM_UNKNOWN = "Unknown"

enum ProjectState {
    ACTIVE = 0,
    PHASE_OUT = 1,
    UNKNOWN = 2,
    SVM_ONLY = 3,
    PRIVATE = 4,
    UNDER_DEVELOPMENT = 5,
    RELEASED = 6
}

enum ProjectType {
    CUSTOMER = 0,
    INTERNAL = 1,
    PRODUCT = 2,
    SERVICE = 3,
    INNER_SOURCE = 4,
    CLOUD_BACKEND = 5
}

enum ProjectRelationship {
    UNKNOWN = 0,
    REFERRED = 1,
    CONTAINED = 2,
    DUPLICATE = 3,
}

enum ProjectClearingState {
    OPEN = 0,
    IN_PROGRESS = 1,
    CLOSED = 2,
}

enum ProjectSortColumn {
    BY_CREATEDON = -1,
    BY_VENDOR = 0,
    BY_NAME = 1,
    BY_MAINLICENSE = 2,
    BY_TYPE = 3,
    BY_DESCRIPTION = 4,
    BY_RESPONSIBLE = 5,
    BY_STATE = 6,
}

struct ProjectProjectRelationship {
    1: required ProjectRelationship projectRelationship,
    2: optional bool enableSvm = true;
}

struct ProjectData {
    1: required i32 totalNumberOfProjects,
    2: required list<Project> first250Projects,
    3: optional list<string> projectIdsOfRemainingProject
}
struct Project {

    // General information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "project",
    4: required string name,
    5: optional string description,
    6: optional string version,
    7: optional string domain,

    // Additional informations
    10: optional set<Attachment> attachments,
    11: optional string createdOn, // Creation date YYYY-MM-dd
    12: optional string businessUnit,
    13: optional ProjectState state = ProjectState.ACTIVE,
    15: optional ProjectType projectType = ProjectType.PRODUCT,
    16: optional string tag,// user defined tags
    17: optional ProjectClearingState clearingState,

    // User details
    21: optional string createdBy,
    22: optional string projectResponsible,
    23: optional string leadArchitect,
    25: optional set<string> moderators = [],
//    26: optional set<string> comoderators, //deleted
    27: optional set<string> contributors = [],
    28: optional Visibility visbility = sw360.Visibility.EVERYONE,
    29: optional map<string,set<string>> roles, //customized roles with set of mail addresses
    129: optional set<string> securityResponsibles = [],
    130: optional string projectOwner,
    131: optional string ownerAccountingUnit,
    132: optional string ownerGroup,
    133: optional string ownerCountry,

    // Linked objects
    30: optional map<string, ProjectProjectRelationship> linkedProjects,
    31: optional map<string, ProjectReleaseRelationship> releaseIdToUsage,
    32: optional map<string, ProjectPackageRelationship> packageIds,
    //32: optional set<string> packageIds,

    // Admin data
    40: optional string clearingTeam;
    41: optional string preevaluationDeadline,
    42: optional string systemTestStart,
    43: optional string systemTestEnd,
    44: optional string deliveryStart,
    45: optional string phaseOutSince,
    46: optional bool enableSvm, // flag for enabling Security Vulnerability Monitoring

    // information from external data sources
    9: optional map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    49: optional bool considerReleasesFromExternalList, // Consider list of releases from existing external list
    47: optional string licenseInfoHeaderText;
    48: optional bool enableVulnerabilitiesDisplay, // flag for enabling displaying vulnerabilities in project view
    134: optional string obligationsText,
    135: optional string clearingSummary,
    136: optional string specialRisksOSS,
    137: optional string generalRisks3rdParty,
    138: optional string specialRisks3rdParty,
    139: optional string deliveryChannels,
    140: optional string remarksAdditionalRequirements,

    // Information for ModerationRequests
    70: optional DocumentState documentState,
    80: optional string clearingRequestId,

    // Optional fields for summaries!
//    100: optional set<string> releaseIds, //deleted
    101: optional ReleaseClearingStateSummary releaseClearingStateSummary,

    // linked release obligations
    102: optional string linkedObligationId,
    200: optional map<RequestedAction, bool> permissions,

    // Urls for the project
    201: optional map<string, string> externalUrls,
    202: optional Vendor vendor,
    203: optional string vendorId,
    204: optional string modifiedBy, // Last Modified By User Email
    205: optional string modifiedOn, // Last Modified Date YYYY-MM-dd

    206: optional string releaseRelationNetwork, // For configuration enable.flexible.project.release.relationship = true
}

struct ProjectLink {
    1: required string id,
    2: required string name,
    3: optional ProjectRelationship relation,
    4: optional string version,
//    5: optional string parentId,
    6: optional string nodeId,
    7: optional string parentNodeId,
    8: optional ProjectType projectType,
    13:optional ProjectState state,
    9: optional ProjectClearingState clearingState,
    10: optional list<ReleaseLink> linkedReleases,
    11: optional list<ProjectLink> subprojects,
    12: optional i32 treeLevel, //zero-based level in the ProjectLink tree, i.e. root has level 0
    14: optional bool enableSvm = true,
}

struct ProjectWithReleaseRelationTuple {
    1: required Project project,
    2: required ProjectReleaseRelationship relation,
}

struct ObligationList {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "obligationList",
    4: required string projectId,
    5: optional map<string, ObligationStatusInfo> linkedObligationStatus
}

struct ObligationStatusInfo {
    1: optional string text, // need not be saved in database
    2: optional string action,
    3: optional ObligationStatus status,
    4: optional string comment,
    5: optional string modifiedBy,
    6: optional string modifiedOn,
    7: optional set<Release> releases, // used to display in UI, no need to save this in database
    8: optional set<string> licenseIds,
    9: optional map<string, string> releaseIdToAcceptedCLI,
    10: optional string id,
    11: optional ObligationLevel obligationLevel,
    12: optional ObligationType obligationType,
}

struct UsedReleaseRelations {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "usedReleaseRelation",
    4: required string projectId,
    5: optional set<ReleaseRelationship> usedReleaseRelations = [],
    6: optional set<ProjectRelationship> usedProjectRelations,
}

struct ClearingRequest {
    // Basic information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "clearingRequest",

    // Clearing Request
    5: required string requestedClearingDate, // date YYYY-MM-dd
    6: optional string projectId,
    7: required ClearingState clearingState,
    8: required string requestingUser,
    9: optional string projectBU,
    10: optional string requestingUserComment,
    11: required string clearingTeam,
    13: optional string agreedClearingDate,
    14: required i64 timestamp,
    15: optional i64 timestampOfDecision,
    16: optional list<Comment> comments,
    17: optional i64 modifiedOn,
    18: optional list<i64> reOpenOn,
    19: optional ClearingPriority priority,
    20: optional ClearingType clearingType,
    21: optional ClearingSize clearingSize
}

struct ProjectDTO{
    // For configuration enable.flexible.project.release.relationship = true
    // General information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "project",
    4: required string name,
    5: optional string description,
    6: optional string version,
    7: optional string domain,

    // information from external data sources
    9: optional map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    // Additional informations
    10: optional set<Attachment> attachments,
    11: optional string createdOn, // Creation date YYYY-MM-dd
    12: optional string businessUnit,
    13: optional ProjectState state = ProjectState.ACTIVE,
    15: optional ProjectType projectType = ProjectType.PRODUCT,
    16: optional string tag,// user defined tags
    17: optional ProjectClearingState clearingState,

    // User details
    21: optional string createdBy,
    22: optional string projectResponsible,
    23: optional string leadArchitect,
    25: optional set<string> moderators = [],
//    26: optional set<string> comoderators, //deleted
    27: optional set<string> contributors = [],
    28: optional Visibility visbility = sw360.Visibility.BUISNESSUNIT_AND_MODERATORS,
    29: optional map<string,set<string>> roles, //customized roles with set of mail addresses
    129: optional set<string> securityResponsibles = [],
    130: optional string projectOwner,
    131: optional string ownerAccountingUnit,
    132: optional string ownerGroup,
    133: optional string ownerCountry,

    // Linked objects
    30: optional map<string, ProjectProjectRelationship> linkedProjects,

    // Admin data
    40: optional string clearingTeam;
    41: optional string preevaluationDeadline,
    42: optional string systemTestStart,
    43: optional string systemTestEnd,
    44: optional string deliveryStart,
    45: optional string phaseOutSince,
    46: optional bool enableSvm, // flag for enabling Security Vulnerability Monitoring
    49: optional bool considerReleasesFromExternalList, // Consider list of releases from existing external list,
    47: optional string licenseInfoHeaderText;
    48: optional bool enableVulnerabilitiesDisplay, // flag for enabling displaying vulnerabilities in project view
    134: optional string obligationsText,
    135: optional string clearingSummary,
    136: optional string specialRisksOSS,
    137: optional string generalRisks3rdParty,
    138: optional string specialRisks3rdParty,
    139: optional string deliveryChannels,
    140: optional string remarksAdditionalRequirements,

    // Information for ModerationRequests
    70: optional DocumentState documentState,
    80: optional string clearingRequestId,

    // Optional fields for summaries!
//    100: optional set<string> releaseIds, //deleted
    101: optional ReleaseClearingStateSummary releaseClearingStateSummary,

    // linked release obligations
    102: optional string linkedObligationId,
    200: optional map<RequestedAction, bool> permissions,

    // Urls for the project
    201: optional map<string, string> externalUrls,
    202: optional Vendor vendor,
    203: optional string vendorId,

    204: optional list<ReleaseNode> dependencyNetwork
}

service ProjectService {

    // Summary getters
    /**
     * get projects for user according to roles
     */
    list<Project> getMyProjects(1: User user, 2:  map<string, bool> userRoles);

    /**
     * get all projects as project summaries which are visible to user with pagination
     */
    map<PaginationData, list<Project>> getAccessibleProjectsSummaryWithPagination(1: User user, 2: PaginationData pageData);

    /**
     * get all projects as project summaries which are visible to user
     */
    list<Project> getAccessibleProjectsSummary(1: User user);

    /**
     * get all projects visible to user
     */
    set<Project> getAccessibleProjects(1: User user);

    // Search functions

    /**
     * global search function to list projects which match the text argument
     */
    list<Project> search(1: string text);

    /**
     * returns a list of projects which match `text` and the
     * `subQueryRestrictions` and are visible to the `user`
     */
    list<Project> refineSearch(1: string text, 2: map<string,set<string>>  subQueryRestrictions, 3: User user);

    /**
     * returns a list of projects which match `text` and the
     * `subQueryRestrictions` and are visible to the `user`. The request is pageable
     */
    map<PaginationData, list<Project>> refineSearchPageable(1: string text, 2: map<string,set<string>>  subQueryRestrictions, 3: User user, 4: PaginationData pageData);

    /**
     * list of projects which are visible to the `user` and match the `name`
     */
    list<Project> searchByName(1: string name, 2: User user);

    /**
     * Get Projects with name prefix and paginated
     **/
    map<PaginationData, list<Project>> searchProjectByNamePrefixPaginated(1: User user, 2: string name, 3: PaginationData pageData);

    /**
     * Get Projects with exact name and paginated
     **/
    map<PaginationData, list<Project>> searchProjectByExactNamePaginated(1: User user, 2: string name, 3: PaginationData pageData);

    /**
     * search projects in database that match subQueryRestrictions
     * Gets the projects with mango query and pagination.
     **/
    map<PaginationData, list<Project>> searchAccessibleProjectByExactValues(1: map<string, set<string>> subQueryRestrictions, 2: User user, 3: PaginationData pageData) throws (1: SW360Exception exp);

    /**
     * project data which are visible to the `user` and match the `group`
     */
    ProjectData searchByGroup(1: string group, 2: User user) throws (1: SW360Exception exp);

    /**
     * project data which are visible to the `user` and match the `tag`
     */
    ProjectData searchByTag(1: string tag, 2: User user) throws (1: SW360Exception exp);

    /**
     * project data which are visible to the `user` and match the `type`
     */
    ProjectData searchByType(1: string type, 2: User user) throws (1: SW360Exception exp);

    /**
     * list of short project summaries which are visible to the `user` and have `id` in releaseIdToUsage
     */
    set<Project> searchByReleaseId(1: string id, 2: User user);

    /**
     * list of short project summaries which are visible to the `user` and have one of the `ids` in releaseIdToUsage
     */
    set<Project> searchByReleaseIds(1: set<string> ids, 2: User user);

    /**
     * list of full project summaries which are visible to the `user` and have `id` in packageIds
     */
    set<Project> searchProjectByPackageId(1: string id, 2: User user);

    /**
     * list of full project summaries which are visible to the `user` and have one of the `ids` in packageIds
     */
    set<Project> searchProjectByPackageIds(1: set<string> ids, 2: User user);

    /**
     * get the count value of projects which have `id` in packageIds
     */
    i32 getProjectCountByPackageId(1: string id);

    /**
     * get short summaries of projects linked to the project with the id `id` which are visible
     * to the user
     */
    set<Project> searchLinkingProjects(1: string id, 2: User user);

    /**
     * add a project as a user to the db and get the id back
     * (part of project CRUD support)
     */
    AddDocumentRequestSummary addProject(1: Project project, 2: User user);

    /**
     * get a project by id, if it is visible for the user
     * (part of project CRUD support)
     */
    Project getProjectById(1: string id, 2: User user) throws (1: SW360Exception exp);

    /**
      * get a project by id based on id irrespective of its visibility for the user
      * (part of project CRUD support)
      */
    Project getProjectByIdIgnoringVisibility(1: string id) throws (1: SW360Exception exp);

    /**
     * get multiple projects by id, if they are visible to the user
     * (part of project CRUD support)
     */
    list<Project> getProjectsById(1: list<string> id, 2: User user);

    /**
     * get project by id, with moderation requests of user applied
     */
    Project getProjectByIdForEdit(1: string id, 2: User user) throws (1: SW360Exception exp);

    /**
     * try to update a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     */
    RequestStatus updateProject(1: Project project, 2: User user);

    /**
     * try to update a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     * If forceUpdate is true, this function can update regardless of write permissions.
     */
    RequestStatus updateProjectWithForceFlag(1: Project project, 2: User user, 3: bool forceUpdate);

    /**
     * try to delete a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     */
    RequestStatus deleteProject(1: string id, 2: User user);

    /**
     * try to delete a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     * If forceDelete is true, this function can delete regardless of delete permissions.
     */
    RequestStatus deleteProjectWithForceFlag(1: string id, 2: User user, 3: bool forceDelete);

    /**
     * updateproject in database if user has permissions, additions and deletions are the parts of the moderation request
     * that specify which properties to add to and which to delete from project
     **/
    RequestStatus updateProjectFromModerationRequest(1: Project additions, 2: Project deletions, 3: User user);

    /**
     * try to remove an attachment with the id `attachmentContentId` as `user`
     * from the project with projectId `projectId`, if user does not have permissions a moderation request is created
     */
    RequestStatus removeAttachmentFromProject(1: string projectId, 2:User user, 3:string attachmentContentId);

    //Linked Projects

    /**
     * check if a the project specified by projectId is linked to some other project
     */
    bool projectIsUsed(1: string projectId);

    /**
     * get a list of project links of the project that matches the id `id`
     * is equivalent to `getLinkedProjectsOfProject(getProjectById(id, user))`
     */
    list<ProjectLink> getLinkedProjectsById(1: string id, 2: bool deep, 3: User user);

    /**
     * get a list of project links of the project
     * The returned list contains one element and its pointing to the original linking project.
     * This allows returning linked releases of the original project at the same time.
     * If parameter `deep` is false, then the links are loaded only one level deep.
     * That is, the project links referenced by the top project link
     * do not have any release links or their subprojects loaded.
     */
    list<ProjectLink> getLinkedProjectsOfProject(1: Project project, 2: bool deep, 3: User user);

    /**
     * get a list of project links from keys of map `relations`
     * IMPORTANT:
     * this method is very inefficient as it loads whole DB repositories into memory; its use is discouraged
     */
    list<ProjectLink> getLinkedProjects(1:  map<string, ProjectProjectRelationship> relations, 2: bool depth, 3: User user);

    /**
     * get a list of duplicated projects matched by `.printName()`
     * returned as map from pretty printed name to list of matching ids
     */
    map <string, list<string>> getDuplicateProjects();

    /**
     * get the same list of projects back, but with filled release clearing state summaries
     */
    list<Project> fillClearingStateSummary(1: list<Project> projects, 2: User user);

    /**
     * get the same list of projects back, but with filled release clearing state summaries where this
     * clearing state summary contains the clearing states of releases of the complete subproject tree.
     * Visibility of any of the projects in the tree for the given user is currently not considered.
     */
    list<Project> fillClearingStateSummaryIncludingSubprojects(1: list<Project> projects, 2: User user);

    Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(1: Project project, 2: User user);

    /**
    * export all projects to SVM to create/update monitoring lists
    **/
    RequestStatus exportForMonitoringList();

    /**
     * get clearing status data for all releases linked by the given project and its subprojects
     */
    list<ReleaseClearingStatusData> getReleaseClearingStatuses(1: string projectId, 2: User user) throws (1: SW360Exception exp);

    /**
     * get clearing status data with accessibility for all releases linked by the given project and its subprojects
     */
    list<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(1: string projectId, 2: User user) throws (1: SW360Exception exp);

    /**
     * get the count value of projects which have `id` in releaseIdToUsage
     */
    i32 getCountByReleaseIds(1: set<string> ids);

    /**
     * get the count value of projects which have `id` in linkedProjects
     */
    i32 getCountByProjectId(1: string id);

    /**
     * get a set of projects based on the external id
     * external ids can have multiple values to one key
     */
    set<Project> searchByExternalIds(1: map<string, set<string>> externalIds, 2: User user);

    /**
     * get the cyclic hierarchy of linkedProjects
     */
    string getCyclicLinkedProjectPath(1: Project project, 2: User user) throws (1: SW360Exception exp);

    /**
     * get linked obligation of a project
     */
    ObligationList getLinkedObligations(1: string obligationId, 2: User user);

    /**
     * add linked obligations to a project
     */
    RequestStatus addLinkedObligations(1: ObligationList obligation, 2: User user);

    /**
     * update linked obligations of a project
     */
    RequestStatus updateLinkedObligations(1: ObligationList obligation, 2: User user);

    /**
     * Deletes an UsedReleaseRelations object. The given usage object must exist in the database.
     */
    void deleteReleaseRelationsUsage(1: UsedReleaseRelations usedReleaseRelations);

    /**
     * Add an used release relations for a Project.
     */
    void addReleaseRelationsUsage(1: UsedReleaseRelations usedReleaseRelations);

    /**
     * Update an used release relations for a Project.
     */
    void updateReleaseRelationsUsage(1: UsedReleaseRelations usedReleaseRelations);

    /**
     * Get used release relations by project id
     */
    list<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(1: string projectId);

    /**
     * parse a bom file and write the information to SW360
     **/
    RequestSummary importBomFromAttachmentContent(1: User user, 2:string attachmentContentId);

    /**
     * Parse a CycloneDx SBoM file (XML or JSON) and write the information to SW360 as Project / Component / Release / Package
     */
    RequestSummary importCycloneDxFromAttachmentContent(1: User user, 2: string attachmentContentId, 3: string projectId) throws (1: SW360Exception exp);

    /**
    * Parse a CycloneDx SBoM file (XML or JSON) during re-import on a project and write the information to SW360 as Project / Component / Release / Package
    * with replaceReleaseAndPackageFlag
    */
    RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(1: User user, 2: string attachmentContentId, 3: string projectId, 4: bool doNotReplacePackageAndRelease) throws (1: SW360Exception exp);

    /**
     * Export a CycloneDx SBoM file (XML or JSON) for a Project
     */
    RequestSummary exportCycloneDxSbom(1: string projectId, 2: string bomType, 3: bool includeSubProjReleases, 4: User user) throws (1: SW360Exception exp);

    /**
     * Get the SBOM import statistics information from attachment as String (JSON formatted)
     */
    string getSbomImportInfoFromAttachmentAsString(string attachmentContentId) throws (1: SW360Exception exp);

    /**
     * create clearing request for project
     */
    AddDocumentRequestSummary createClearingRequest(1: ClearingRequest clearingRequest, 2: User user, 3: string projectUrl);

    /**
     * get clearing state information for list view
     */
    list<map<string,string>> getClearingStateInformationForListView(1:string projectId, 2: User user) throws (1: SW360Exception exp);

    /**
     * get accessible clearing state information for list view
     */
    list<map<string,string>> getAccessibleClearingStateInformationForListView(1:string projectId, 2: User user) throws (1: SW360Exception exp);

    /**
    * filter groups from the projects
    */
    set<string> getGroups();

    /**
    * get accessible projects count
    */
    i32 getMyAccessibleProjectCounts(1: User user);

    /**
    * Send email to the user once spreadsheet export completed
    */
    void sendExportSpreadsheetSuccessMail(1: string url, 2: string userEmail);
    /*
    * make excel export
    */
    binary getReportDataStream(1: User user,2: bool extendedByReleases,3: string projectId) throws (1: SW360Exception exp);
     /*
    * excel export - return the filepath
    */
    string getReportInEmail(1: User user, 2: bool extendedByReleases, string projectId) throws (1: SW360Exception exp);
    /*
    * download excel
    */
    binary downloadExcel(1:User user,2:bool extendedByReleases,3:string token) throws (1: SW360Exception exc);

    /**
    * get list ReleaseLink in release network of project by project id and trace
    */
    list<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(1: string projectId, 2: list<string> trace, 3: User user);

    /**
    * get dependency network for list view
    */
    list<map<string, string>> getAccessibleDependencyNetworkForListView(1: string projectId, 2: User user) throws (1: SW360Exception exp);


    /**
     * returns a list of projects which match `text` and the `subQueryRestrictions`
     */
    list<Project> refineSearchWithoutUser(1: string text, 2: map<string,set<string>>  subQueryRestrictions);

    /**
     * get a list of project links from keys of map `relations`
     * do not get linked releases
     */
    list<ProjectLink> getLinkedProjectsWithoutReleases(1:  map<string, ProjectProjectRelationship> relations, 2: bool depth, 3: User user);

    /**
     * get a list of project links of the project
     * The returned list contains one element and its pointing to the original linking project.
     * This not allows returning linked releases of the original project at the same time.
     * If parameter `deep` is false, then the links are loaded only one level deep.
     * That is, the project links referenced by the top project link
     * do not have any release links or their subprojects loaded.
     */
    list<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(1: Project project, 2: bool deep, 3: User user);

    /**
     * get a list of project links of the project that matches the id `id`
     * with each project get all release in dependency network
     * is equivalent to `getLinkedProjectsOfProject(getProjectById(id, user))`
     */
    list<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(1: Project project, 2: bool deep, 3: User user);

    /**
    * get list ReleaseLink in dependency network of project by project id and index path
    */
    list<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(1: string projectId, 2: list<string> indexPath, 3: User user) throws (1: SW360Exception exp);

    /**
    * Get linked releases information in dependency network of a project
    */
    list<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(1: string projectId, 2: User sw360User) throws (1: SW360Exception exp);
}
