/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

include "sw360.thrift"
include "components.thrift"
include "projects.thrift"
include "users.thrift"
include "licenses.thrift"
include "spdx/spdxdocument.thrift"
include "spdx/documentcreationinformation.thrift"
include "spdx/packageinformation.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.moderation
namespace php sw360.thrift.moderation

typedef sw360.RequestStatus RequestStatus
typedef sw360.RemoveModeratorRequestStatus RemoveModeratorStatus
typedef sw360.ModerationState ModerationState
typedef sw360.Comment Comment
typedef sw360.PaginationData PaginationData
typedef components.Component Component
typedef components.Release Release
typedef projects.Project Project
typedef users.User User
typedef licenses.License License
typedef licenses.Obligation Obligation
typedef components.ComponentType ComponentType
typedef projects.ClearingRequest ClearingRequest
typedef spdxdocument.SPDXDocument SPDXDocument
typedef documentcreationinformation.DocumentCreationInformation DocumentCreationInformation
typedef packageinformation.PackageInformation PackageInformation

enum DocumentType {
    COMPONENT = 1,
    RELEASE = 2,
    PROJECT = 3,
    LICENSE = 4,
    USER = 5,
    SPDX_DOCUMENT = 6,
    SPDX_DOCUMENT_CREATION_INFO = 7,
    SPDX_PACKAGE_INFO = 8,
}

struct ModerationRequest {

    // Basic information
    1: optional string id,
    2: optional string revision,
    3: optional string type = "moderation",

    // Moderation request
    10: required i64 timestamp,
    37: optional i64 timestampOfDecision,
    11: required string documentId,
    12: required DocumentType documentType,
    13: optional string requestingUser,
    14: optional set<string> moderators,
    15: optional string documentName,
    16: required ModerationState moderationState,
    17: optional string reviewer,
    18: required bool requestDocumentDelete,
    19: optional string requestingUserDepartment,
    40: optional ComponentType componentType, // only relevant if the request is about components or releases
    34: optional string commentRequestingUser,
    35: optional string commentDecisionModerator,
    // Underlying objects
    20: optional Component componentAdditions,
    21: optional Release releaseAdditions,
    22: optional Project projectAdditions,
    23: optional License licenseAdditions,//only moderation of obligations is supported
    24: optional User user,

    30: optional Component componentDeletions,
    31: optional Release releaseDeletions,
    32: optional Project projectDeletions,
    33: optional License licenseDeletions,

    50: optional SPDXDocument SPDXDocumentAdditions,
    51: optional SPDXDocument SPDXDocumentDeletions,
    52: optional DocumentCreationInformation documentCreationInfoAdditions,
    53: optional DocumentCreationInformation documentCreationInfoDeletions,
    54: optional PackageInformation packageInfoAdditions,
    55: optional PackageInformation packageInfoDeletions,

}

service ModerationService {

    /**
     * write moderation request for component to database by comparing component with corresponding document in database
     * and writing difference as additions and deletions to moderation request,
     * set requestingUser of moderation request to user
     **/
    RequestStatus createComponentRequest(1: Component component, 2: User user);

    /**
      * write moderation request for release to database by comparing release with corresponding document in database
      * and writing difference as additions and deletions to moderation request,
      * set requestingUser of moderation request to use
      **/
    RequestStatus createReleaseRequest(1: Release release, 2: User user);

    /**
      * write moderation request for release to database by comparing release with corresponding document in database
      * and writing difference as additions and deletions to moderation request,
      * set requestingUser of moderation request to use
      * The moderation request is sent to ECC_ADMINs
      **/
    RequestStatus createReleaseRequestForEcc(1: Release release, 2: User user);

    /**
      * write moderation request for project to database by comparing project with corresponding document in database
      * and writing difference as additions and deletions to moderation request,
      * set requestingUser of moderation request to user
      **/
    RequestStatus createProjectRequest(1: Project project, 2: User user);

    /**
      * write moderation request for license to database,
      * only obligations and whitelists can be moderated, so license obligations are compared with corresponding obligations in database,
      * differences are written as additions and deletions to moderation request,
      * set requestingUser of moderation request to user
      **/
    RequestStatus createLicenseRequest(1: License license, 2: User user);

    /**
      * write moderation request for SPDXDocument to database,
      * differences are written as additions and deletions to moderation request,
      * set requestingUser of moderation request to user
      **/
    RequestStatus createSPDXDocumentRequest(1: SPDXDocument spdx, 2: User user);

    /**
    * write moderation request for spdx document creation info to database,
    * differences are written as additions and deletions to moderation request,
    * set requestingUser of moderation request to user
      **/
    RequestStatus createSpdxDocumentCreationInfoRequest(1: DocumentCreationInformation documentCreationInfo, 2: User user);

    /**
    * write moderation request for spdx document creation info to database,
    * differences are written as additions and deletions to moderation request,
    * set requestingUser of moderation request to user
      **/
    RequestStatus createSpdxPackageInfoRequest(1: PackageInformation packageInfo, 2: User user);

    /**
      * write moderation request for activating a user account to database
      **/
    oneway void createUserRequest(1: User user);

    /**
      * write moderation request for deleting component to database,
      * set requestingUser of moderation request to user
      **/
    oneway void createComponentDeleteRequest(1: Component component, 2: User user);

    /**
      * write moderation request for deleting release to database,
      * set requestingUser of moderation request to user
      **/
    oneway void createReleaseDeleteRequest(1: Release release, 2: User user);

    /**
      * write moderation request for deleting project to database,
      * set requestingUser of moderation request to user
      **/
    oneway void createProjectDeleteRequest(1: Project project, 2: User user);

    /**
      * write moderation request for deleting project to database,
      * set requestingUser of moderation request to user
      **/
    oneway void createSPDXDocumentDeleteRequest(1: SPDXDocument spdx, 2: User user);

    /**
      * write moderation request for deleting spdx document creation info to database,
      * set requestingUser of moderation request to user
      **/
    oneway void createSpdxDocumentCreationInfoDeleteRequest(1: DocumentCreationInformation documentCreationInfo, 2: User user);

    /**
    * write moderation request for deleting spdx package info to database,
    * set requestingUser of moderation request to user
      **/
    oneway void createSpdxPackageInfoDeleteRequest(1: PackageInformation packageInfo, 2: User user);

    /**
     * get list of moderation requests for document with documentId currently present in database
     **/
    list<ModerationRequest> getModerationRequestByDocumentId(1: string documentId);

    ModerationRequest getModerationRequestById(1: string id);

   /**
    * update moderationRequest in database
    **/
    RequestStatus updateModerationRequest(1: ModerationRequest moderationRequest);

   /**
     * set moderation state of moderation request to ACCEPTED
     * and send mail notifications
    **/
    RequestStatus acceptRequest(1: ModerationRequest request, 2: string moderationDecisionComment, 3: string reviewer);

    /**
     * set moderation state of moderation request specified by requestId to REJECTED,
     * save a comment by the moderator,
     * and send mail notification to requestingUser about decline
     **/
    oneway void refuseRequest(1: string requestId, 2: string moderationDecisionComment, 3: string reviewer);

    /**
     * remove user from moderators of moderation request specified by requestId,
     * set moderation state to PENDING,
     * unset reviewer
     * if user is last moderator for moderation request: do not remove, return RemoveModeratorStatus.LAST_MODERATOR
     **/
    RemoveModeratorStatus removeUserFromAssignees(1: string requestId, 2:User user);

    /**
     * set moderation state to PENDING of moderation request spedified by requestId,
     * unset reviewer
     **/
    oneway void cancelInProgress(1: string requestId);

    /**
     * set moderation state of moderation request specified by requestId to IN PROGRESS,
     * set reviewer to user
     **/
    oneway void setInProgress(1: string requestId, 2:User user);

    /**
     * delete moderation requests for document specified by documentId from database
     **/
    oneway void deleteRequestsOnDocument(1: string documentId);

    /**
     * get list of moderation requests where user is one of the moderators
     **/
    list<ModerationRequest> getRequestsByModerator(1: User user);

    /**
     * get list of moderation requests based on moderation state(open/closed) where user is one of the moderators, with pagination
     **/
    map<PaginationData, list<ModerationRequest>> getRequestsByModeratorWithPagination(1: User user, 2: PaginationData pageData, 3: bool open);

    /**
     * get list of moderation requests where user is requesting user
     **/
    list<ModerationRequest> getRequestsByRequestingUser(1: User user);

    /**
     * delete moderation request specified by id if user is requesting user of moderation request
     **/
    RequestStatus deleteModerationRequest(1: string id, 2: User user);

    /**
     * write clearing request for project to database
     **/
    string createClearingRequest(1: ClearingRequest clearingRequest, 2: User user);

    /**
     * update clearing request in database
     **/
    RequestStatus updateClearingRequest(1: ClearingRequest clearingRequest, 2: User user, 3: string projectUrl);

    /**
     * get list of clearing requests where user is requesting user or clearing team
     **/
    set<ClearingRequest> getMyClearingRequests(1: User user);

    /**
     * get list of clearing requests by business unit
     **/
    set<ClearingRequest> getClearingRequestsByBU(1: string businessUnit);

    /**
     * get clearing request by project Id
     **/
    ClearingRequest getClearingRequestByProjectId(1: string projectId, 2: User user);

    /**
     * update clearing request for associated project deletion
     **/
    oneway void updateClearingRequestForProjectDeletion(1: Project project, 2: User user);

    /**
     * update clearing request if project's BU is changed
     **/
    oneway void updateClearingRequestForChangeInProjectBU(1: string crId, 2: string businessUnit, 3: User user);

    /**
     * get clearing request by Id for view/read
     **/
    ClearingRequest getClearingRequestById(1: string id, 2: User user);

    /**
     * get clearing request by Id for edit
     **/
    ClearingRequest getClearingRequestByIdForEdit(1: string id, 2: User user);

    /**
     * add comment to clearing request
     **/
    RequestStatus addCommentToClearingRequest(1: string id, 2: Comment comment, 3: User user);

    /**
     * search moderation requests in database that match subQueryRestrictions
     **/
    list<ModerationRequest> refineSearch(1: string text, 2: map<string, set<string>> subQueryRestrictions);

    /**
     * get count of moderation requests by moderation state
     **/
    map<string, i64> getCountByModerationState(1: User user);

    /**
     * get requesting users departments
     **/
    set<string> getRequestingUserDepts();

    /**
     * get the count of open CR with priority 'critical' and user group
     **/
    i32 getOpenCriticalCrCountByGroup(1: string group);
}
