/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
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
namespace java org.eclipse.sw360.datahandler.thrift.users
namespace php sw360.thrift.users

typedef sw360.RequestStatus RequestStatus

enum UserGroup {
    USER = 0,
    ADMIN = 1,
    CLEARING_ADMIN = 2,
    ECC_ADMIN = 3,
    SECURITY_ADMIN = 4,
    SW360_ADMIN = 5
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
}

struct User {

    1: required string id,
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
}

service UserService {

    /**
     * returns SW360-user with id equal to email
     **/
    User getByEmail(1:string email);

    /**
     * get list of all SW360-users in database with name equal to parameter name
     **/
    list<User> searchUsers(1:string name);

    /**
     * get list of all SW360-users in database
     **/
    list<User> getAllUsers();

    /**
     * add SW360-user to database, user.email is used as id
     **/
    RequestStatus addUser(1: User user);

    /**
     * update SW360-user in database
     **/
    RequestStatus updateUser(1: User user);

    /**
     * delete user from database, only possible if adminUser has permissions
     **/
    RequestStatus deleteUser(1: User user, 2: User adminUser);

    /**
     * returns department of the SW360-user with id equal to email
     **/
    string getDepartmentByEmail(1:string email);

}
