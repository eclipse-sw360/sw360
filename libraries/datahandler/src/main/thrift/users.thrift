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
namespace java org.eclipse.sw360.datahandler.thrift.users
namespace php sw360.thrift.users

typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.PaginationData PaginationData
typedef sw360.SW360Exception SW360Exception

enum UserGroup {
    USER = 0,
    ADMIN = 1,
    CLEARING_ADMIN = 2,
    ECC_ADMIN = 3,
    SECURITY_ADMIN = 4,
    SW360_ADMIN = 5,
    CLEARING_EXPERT = 6,
    SECURITY_USER = 7
}

enum UserAccess {
    READ = 0,
    READ_WRITE =1
}

enum LocalGroup {
    BU = 0,
    CONTRIBUTOR = 1,
    MODERATOR = 2,
    OWNER = 3,
}

enum RequestedAction {
    READ = 1,
    WRITE = 2,
    DELETE = 3,
    USERS = 4,
    CLEARING = 5,
    ATTACHMENTS = 6,
    WRITE_ECC = 7,
    WRITE_VULNERABILITY = 8,
}

enum UserSortColumn {
    BY_SCORE = -2,
    BY_GIVENNAME = -1,
    BY_LASTNAME = 0,
    BY_EMAIL = 1,
    BY_STATUS = 2,
    BY_DEPARTMENT = 3,
    BY_ROLE = 4,
}

struct User {

    1: optional string id,
    2: optional string revision,
    3: optional string type = "user",
    4: required string email,
    5: optional UserGroup userGroup,
    6: optional string externalid,
    7: optional string fullname,
    8: optional string givenname, // firstname or given name of the person
    9: optional string lastname, // lastname or surname of the person
    10: required string department,
    11: optional bool wantsMailNotification,
    12: optional string commentMadeDuringModerationRequest,
    13: optional map<string, bool> notificationPreferences,
    14: optional set<string> formerEmailAddresses,
    20: optional list<RestApiToken> restApiTokens,
    21: optional map<string, bool> myProjectsPreferenceSelection,
    22: optional map<string, set<UserGroup>> secondaryDepartmentsAndRoles,
    23: optional list<string> primaryRoles,
    24: optional bool deactivated
    25: optional map<string, ClientMetadata> oidcClientInfos,
    26: optional string password
}

struct ClientMetadata {
    1: required string name,
    2: required UserAccess access
}

struct RestApiToken {
    1: optional string token,
    2: optional string name,
    3: optional string createdOn
    4: optional i32 numberOfDaysValid,
    5: optional set<string> authorities,
}

struct DepartmentConfigDTO {
    1: optional string pathFolder,
    2: optional string pathFolderLog,
    3: optional string lastRunningTime,
    4: optional i32 showFileLogFrom,
}
