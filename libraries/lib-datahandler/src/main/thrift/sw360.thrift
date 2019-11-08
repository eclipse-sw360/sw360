/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

namespace java org.eclipse.sw360.datahandler.thrift
namespace php sw360.thrift

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
    NAMINGERROR = 3
}

exception SW360Exception {
    1: required string why,
}

enum ModerationState {
    PENDING = 0,
    APPROVED = 1,
    REJECTED = 2,
    INPROGRESS =3,
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

struct ConfigContainer {
    1: optional string id,
    2: optional string revision,
    3: required ConfigFor configFor,
    4: required map<string, set<string>> configKeyToValues,
}

struct ProjectReleaseRelationship {
    1: required ReleaseRelationship releaseRelation,
    2: required MainlineState mainlineState,
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

/**
 * May be used to identify a source where the source can be of type project, component or release.
 * Using this type over a string allows the user to see which type of source the id is.
 */
union Source {
  1: string projectId
  2: string componentId
  3: string releaseId
}
