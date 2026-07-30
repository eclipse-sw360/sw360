/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the users backend service.
 *
 * Callers use this instead of direct HTTP to {@code /users/api/users}.
 * Types are service-api POJOs. See {@link UsersServiceRestClient} and {@link UsersClients}.
 */
public interface UsersClient {

    User getUser(String id);

    User getByEmail(String email);

    User getByEmailOrExternalId(String email, String externalId);

    User getByApiToken(String token);

    User getByOidcClientId(String clientId);

    List<User> getAllUsers();

    AddDocumentRequestSummary addUser(User user);

    RequestStatus updateUser(User user);

    PaginatedResult<User> getUsersWithPagination(PaginationData pageData);

    PaginatedResult<User> refineSearch(String text, Map<String, Set<String>> filterMap, PaginationData pageData);

    PaginatedResult<User> searchUsersByExactValues(Map<String, Set<String>> filterMap, PaginationData pageData);

    Set<String> getUserDepartments();

    Set<String> getUserSecondaryDepartments();

    RequestSummary importFileToDB();

    RequestStatus importDepartmentSchedule();

    void writePathFolderConfig(String pathFolder);

    String getPathConfigDepartment();

    String getLastRunningTime();

    Set<String> getListFileLog();

    List<String> getLogFileContentByName(String fileName);

    Map<String, List<String>> getSecondaryDepartmentMemberEmails();

    Set<String> getMemberEmailsBySecondaryDepartmentName(String departmentName);

    void updateDepartmentToListUser(List<User> users, String department);

    void deleteSecondaryDepartmentFromListUser(List<User> users, String department);

    List<User> getAllUserByEmails(List<String> emails);

    String getDepartmentByEmail(String email);
}
