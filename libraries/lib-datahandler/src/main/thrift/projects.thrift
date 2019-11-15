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
include "users.thrift"
include "attachments.thrift"
include "vendors.thrift"
include "components.thrift"
include "sw360.thrift"
include "commonobligations.thrift"
include "licenses.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.projects
namespace php sw360.thrift.projects

typedef sw360.RequestStatus RequestStatus
typedef sw360.DocumentState DocumentState
typedef sw360.Visibility Visibility
typedef sw360.ReleaseRelationship ReleaseRelationship
typedef sw360.MainlineState MainlineState
typedef sw360.ProjectReleaseRelationship ProjectReleaseRelationship
typedef components.Release Release
typedef components.ReleaseClearingStateSummary ReleaseClearingStateSummary
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef attachments.Attachment Attachment
typedef components.ReleaseLink ReleaseLink
typedef components.ReleaseClearingStatusData ReleaseClearingStatusData
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef licenses.Todo Todo

const string CLEARING_TEAM_UNKNOWN = "Unknown"

enum ProjectState {
    ACTIVE = 0,
    PHASE_OUT = 1,
    UNKNOWN = 2,
}

enum ProjectType {
    CUSTOMER = 0,
    INTERNAL = 1,
    PRODUCT = 2,
    SERVICE = 3,
    INNER_SOURCE = 4
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

struct Project {

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
    15: optional ProjectType projectType = ProjectType.CUSTOMER,
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
    30: optional map<string, ProjectRelationship> linkedProjects,
    31: optional map<string, ProjectReleaseRelationship> releaseIdToUsage,

    // Admin data
    40: optional string clearingTeam;
    41: optional string preevaluationDeadline,
    42: optional string systemTestStart,
    43: optional string systemTestEnd,
    44: optional string deliveryStart,
    45: optional string phaseOutSince,
    46: optional bool enableSvm, // flag for enabling Security Vulnerability Monitoring
    47: optional string licenseInfoHeaderText;
    48: optional bool enableVulnerabilitiesDisplay, // flag for enabling displaying vulnerabilities in project view
    134: optional string obligationsText,
    135: optional string clearingSummary,
    136: optional string specialRisksOSS,
    137: optional string generalRisks3rdParty,
    138: optional string specialRisks3rdParty,
    139: optional string deliveryChannels,
    140: optional string remarksAdditionalRequirements,
    141: optional set<ProjectTodo> todos,

    // Urls for the project
    50: optional string homepage,
    52: optional string wiki,

    // Information for ModerationRequests
    70: optional DocumentState documentState,

    // Optional fields for summaries!
//    100: optional set<string> releaseIds, //deleted
    101: optional ReleaseClearingStateSummary releaseClearingStateSummary,

    // obligation status
    102: optional map<string, ObligationStatusInfo> linkedObligations,
    200: optional map<RequestedAction, bool> permissions,
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
}

struct ProjectWithReleaseRelationTuple {
    1: required Project project,
    2: required ProjectReleaseRelationship relation,
}

struct ProjectTodo {
    1: required string todoId;
    2: required string userId;
    3: required string updated;
    4: required bool fulfilled;
}

enum ProjectObligationStatus {
    OPEN = 0,
    FULFILLED = 1,
    IN_PROGRESS = 2,
}

struct ObligationStatusInfo {
    1: optional string text, // need not be saved in database
    2: optional string action,
    3: optional ProjectObligationStatus status,
    4: optional string comment,
    5: optional string modifiedBy,
    6: optional string modifiedOn,
    7: optional set<Release> releases, // used to display in UI, no need to save this in database
    8: required set<string> licenseIds,
    9: optional map<string, string> releaseIdToAcceptedCLI,
}

service ProjectService {

    // Summary getters
    /**
     * get projects for user by emailadress
     */
    list<Project> getMyProjects(1: string user);

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
     * list of projects which are visible to the `user` and match the `name`
     */
    list<Project> searchByName(1: string name, 2: User user);

    /**
     * list of short project summaries which are visible to the `user` and have `id` in releaseIdToUsage
     */
    set<Project> searchByReleaseId(1: string id, 2: User user);

    /**
     * list of short project summaries which are visible to the `user` and have one of the `ids` in releaseIdToUsage
     */
    set<Project> searchByReleaseIds(1: set<string> ids, 2: User user);

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
    Project getProjectById(1: string id, 2: User user);

    /**
     * get multiple projects by id, if they are visible to the user
     * (part of project CRUD support)
     */
    list<Project> getProjectsById(1: list<string> id, 2: User user);

    /**
     * get project by id, with moderation requests of user applied
     */
    Project getProjectByIdForEdit(1: string id, 2: User user);

    /**
     * try to update a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     */
    RequestStatus updateProject(1: Project project, 2: User user);

    /**
     * try to delete a project as a user, if user has no permission, a moderation request is created
     * (part of project CRUD support)
     */
    RequestStatus deleteProject(1: string id, 2: User user);

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
     * this method is very inefficient as it loads whole DB repositories into memory; its use is dicouraged
     */
    list<ProjectLink> getLinkedProjects(1:  map<string, ProjectRelationship> relations, 2: User user);

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

    /**
     * get clearing status data for all releases linked by the given project and its subprojects
     */
    list<ReleaseClearingStatusData> getReleaseClearingStatuses(1: string projectId, 2: User user);

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
    string getCyclicLinkedProjectPath(1: Project project, 2: User user);
}
