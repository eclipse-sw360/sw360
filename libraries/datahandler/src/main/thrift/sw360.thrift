/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

namespace java org.eclipse.sw360.datahandler.thrift
namespace php sw360.thrift

enum Quadratic {
    NA = 0,
    YES = 1
}

enum Ternary {
    UNDEFINED = 0,
    NO = 1,
    YES = 2,
}

enum RequestStatus {
    SUCCESS = 0,
    SENT_TO_MODERATOR = 1,
    FAILURE = 2,
    IN_USE = 3,
    FAILED_SANITY_CHECK = 4,
    DUPLICATE = 5,
    DUPLICATE_ATTACHMENT = 6,
    ACCESS_DENIED = 7,
    CLOSED_UPDATE_NOT_ALLOWED = 8,
    INVALID_INPUT = 9,
    PROCESSING = 10,
    NAMINGERROR = 11
}

enum RemoveModeratorRequestStatus {
    SUCCESS = 0,
    LAST_MODERATOR = 1,
    FAILURE = 2,
}

enum AddDocumentRequestStatus {
    SUCCESS = 0,
    DUPLICATE = 1,
    FAILURE = 2,
    NAMINGERROR = 3,
    INVALID_INPUT = 4
}

exception SW360Exception {
    1: required string why,
    2: optional i32 errorCode,
}

enum ModerationState {
    PENDING = 0,
    APPROVED = 1,
    REJECTED = 2,
    INPROGRESS = 3,
}

enum ClearingRequestState {
    NEW = 0,
    ACCEPTED = 1,
    REJECTED = 2,
    IN_QUEUE = 3,
    IN_PROGRESS = 4,
    CLOSED = 5,
    AWAITING_RESPONSE = 6,
    ON_HOLD = 7,
    SANITY_CHECK = 8,
    PENDING_INPUT = 9
}

enum ClearingRequestPriority {
    LOW = 0,
    MEDIUM = 1,
    HIGH = 2,
    CRITICAL = 3,
}

enum ClearingRequestType {
    DEEP = 0,
    HIGH = 1
}

enum ClearingRequestSize{
    VERY_SMALL = 0,
    SMALL = 1,
    MEDIUM = 2,
    LARGE = 3,
    VERY_LARGE = 4
}

enum Visibility {
    PRIVATE = 0,
    ME_AND_MODERATORS = 1,
    BUISNESSUNIT_AND_MODERATORS = 2,
    EVERYONE = 3
}

enum VerificationState {
    NOT_CHECKED = 0,
    CHECKED = 1,
    INCORRECT = 2,
}

enum ReleaseRelationship {
    CONTAINED = 0,
    REFERRED = 1,
    UNKNOWN = 2,
    DYNAMICALLY_LINKED = 3,
    STATICALLY_LINKED = 4,
    SIDE_BY_SIDE = 5,
    STANDALONE = 6,
    INTERNAL_USE = 7,
    OPTIONAL = 8,
    TO_BE_REPLACED = 9,
    CODE_SNIPPET = 10,
}

enum MainlineState {
    OPEN = 0,
    MAINLINE = 1,
    SPECIFIC = 2,
    PHASEOUT = 3,
    DENIED = 4,
}

enum ConfigFor {
    FOSSOLOGY_REST = 0,
}

enum ObligationStatus {
    OPEN = 0,
    ACKNOWLEDGED_OR_FULFILLED = 1,
    WILL_BE_FULFILLED_BEFORE_RELEASE = 2,
    NOT_APPLICABLE = 3,
    DEFERRED_TO_PARENT_PROJECT = 4,
    FULFILLED_AND_PARENT_MUST_ALSO_FULFILL = 5,
    ESCALATED = 6
}

enum ClearingRequestEmailTemplate {
    NEW = 0,
    UPDATED = 1,
    PROJECT_UPDATED = 2,
    NEW_COMMENT = 3,
    CLOSED = 4,
    REJECTED = 5
}

enum DateRange {
    EQUAL = 0,
    LESS_THAN_OR_EQUAL_TO = 1,
    GREATER_THAN_OR_EQUAL_TO = 2,
    BETWEEN = 3
}

enum ClearingReportStatus {
    NO_STATUS = 0,
    NO_REPORT = 1,
    DOWNLOAD = 2
}

enum CycloneDxComponentType {
    APPLICATION = 0,
    CONTAINER = 1,
    DEVICE = 2,
    FILE = 3,
    FIRMWARE = 4,
    FRAMEWORK = 5,
    LIBRARY = 6,
    OPERATING_SYSTEM = 7,
}

struct ConfigContainer {
    1: optional string id,
    2: optional string revision,
    3: required ConfigFor configFor,
    4: required map<string, set<string>> configKeyToValues,
}

struct ProjectReleaseRelationship {
    1: required ReleaseRelationship releaseRelation,
    2: required MainlineState mainlineState,
    3: optional string comment,
    4: optional string createdOn,
    5: optional string createdBy,
}

struct VerificationStateInfo {
  1: required string checkedOn,
  2: required string checkedBy,
  3: optional string comment,
  4: required VerificationState verificationState,
}

struct DocumentState {
  1: required bool isOriginalDocument;
  2: optional ModerationState moderationState;
}

struct RequestSummary {
  1: required RequestStatus requestStatus;
  2: optional i32 totalAffectedElements;
  3: optional i32 totalElements;
  4: optional string message;
}

struct AddDocumentRequestSummary {
    1: optional AddDocumentRequestStatus requestStatus;
    2: optional string id;
    3: optional string message;
}

struct ImportBomRequestPreparation {
    1: required RequestStatus requestStatus;
    2: optional bool isComponentDuplicate;
    3: optional bool isReleaseDuplicate;
    4: optional string componentsName;
    5: optional string releasesName;
    6: optional string version;
    7: optional string message;
}

struct CustomProperties {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "customproperties",
    4: optional string documentType,
    5: map<string, set<string>> propertyToValues;
}

struct RequestStatusWithBoolean {
  1: required RequestStatus requestStatus;
  2: optional bool answerPositive;
}

struct Comment {
    1: required string text,
    2: required string commentedBy,
    3: optional i64 commentedOn, // timestamp of comment
    4: optional bool autoGenerated
}

struct PaginationData {
    1: optional i32 rowsPerPage,
    2: optional i32 displayStart,
    3: optional bool ascending,
    4: optional i32 sortColumnNumber,
    5: optional i64 totalRowCount
}

/**
 * May be used to identify a source where the source can be of type project, component or release.
 * Using this type over a string allows the user to see which type of source the id is.
 */
union Source {
  1: string projectId
  2: string componentId
  3: string releaseId
}

struct RestrictedResource {
    1: optional i32 projects,
}
