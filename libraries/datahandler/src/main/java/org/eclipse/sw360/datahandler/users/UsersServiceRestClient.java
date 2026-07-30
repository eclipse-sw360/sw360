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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.users.PathFolderConfigRequest;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserEmailExternalIdRequest;
import org.eclipse.sw360.datahandler.services.users.UserSearchFilterRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

/**
 * HTTP implementation of {@link UsersClient} for {@code /users/api/users}.
 */
public class UsersServiceRestClient implements UsersClient {

    public static final String USERS_URI = "/users/api/users";

    private final RestClient restClient;

    public UsersServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public User getUser(String id) {
        return restClient.get()
                .uri(USERS_URI + "/" + id)
                .retrieve()
                .body(User.class);
    }

    @Override
    public User getByEmail(String email) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + "/by-email")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .body(User.class);
    }

    @Override
    public User getByEmailOrExternalId(String email, String externalId) {
        UserEmailExternalIdRequest request = new UserEmailExternalIdRequest()
                .setEmail(email)
                .setExternalId(externalId);
        return restClient.post()
                .uri(USERS_URI + "/by-email-or-external-id")
                .body(request)
                .retrieve()
                .body(User.class);
    }

    @Override
    public User getByApiToken(String token) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + "/by-api-token")
                        .queryParam("token", token)
                        .build())
                .retrieve()
                .body(User.class);
    }

    @Override
    public User getByOidcClientId(String clientId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + "/by-oidc-client-id")
                        .queryParam("clientId", clientId)
                        .build())
                .retrieve()
                .body(User.class);
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = restClient.get()
                .uri(USERS_URI)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return users != null ? users : List.of();
    }

    @Override
    public AddDocumentRequestSummary addUser(User user) {
        return restClient.post()
                .uri(USERS_URI)
                .body(user)
                .retrieve()
                .body(AddDocumentRequestSummary.class);
    }

    @Override
    public RequestStatus updateUser(User user) {
        return restClient.put()
                .uri(USERS_URI)
                .body(user)
                .retrieve()
                .body(RequestStatus.class);
    }

    @Override
    public PaginatedResult<User> getUsersWithPagination(PaginationData pageData) {
        return fetchPaginated("/page", pageData);
    }

    @Override
    public PaginatedResult<User> refineSearch(String text, Map<String, Set<String>> filterMap,
            PaginationData pageData) {
        UserSearchFilterRequest filter = new UserSearchFilterRequest()
                .setText(text)
                .setSubQueryRestrictions(filterMap);
        return fetchPaginatedWithBody("/refine-search", pageData, filter);
    }

    @Override
    public PaginatedResult<User> searchUsersByExactValues(Map<String, Set<String>> filterMap,
            PaginationData pageData) {
        UserSearchFilterRequest filter = new UserSearchFilterRequest()
                .setSubQueryRestrictions(filterMap);
        return fetchPaginatedWithBody("/search/exact", pageData, filter);
    }

    @Override
    public Set<String> getUserDepartments() {
        Set<String> result = restClient.get()
                .uri(USERS_URI + "/departments")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public Set<String> getUserSecondaryDepartments() {
        Set<String> result = restClient.get()
                .uri(USERS_URI + "/secondary-departments")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public RequestSummary importFileToDB() {
        return restClient.post()
                .uri(USERS_URI + "/import")
                .retrieve()
                .body(RequestSummary.class);
    }

    @Override
    public RequestStatus importDepartmentSchedule() {
        return restClient.post()
                .uri(USERS_URI + "/import/schedule")
                .retrieve()
                .body(RequestStatus.class);
    }

    @Override
    public void writePathFolderConfig(String pathFolder) {
        restClient.put()
                .uri(USERS_URI + "/config/path")
                .body(new PathFolderConfigRequest().setPathFolder(pathFolder))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String getPathConfigDepartment() {
        return restClient.get()
                .uri(USERS_URI + "/config/path")
                .retrieve()
                .body(String.class);
    }

    @Override
    public String getLastRunningTime() {
        return restClient.get()
                .uri(USERS_URI + "/config/last-running-time")
                .retrieve()
                .body(String.class);
    }

    @Override
    public Set<String> getListFileLog() {
        Set<String> result = restClient.get()
                .uri(USERS_URI + "/log-files")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public List<String> getLogFileContentByName(String fileName) {
        return restClient.get()
                .uri(USERS_URI + "/log-files/" + fileName + "/content")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() {
        Map<String, List<String>> result = restClient.get()
                .uri(USERS_URI + "/secondary-departments/members")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public Set<String> getMemberEmailsBySecondaryDepartmentName(String departmentName) {
        Set<String> result = restClient.get()
                .uri(USERS_URI + "/secondary-departments/" + departmentName + "/emails")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public void updateDepartmentToListUser(List<User> users, String department) {
        restClient.put()
                .uri(USERS_URI + "/secondary-departments/" + department + "/members")
                .body(users)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deleteSecondaryDepartmentFromListUser(List<User> users, String department) {
        restClient.method(HttpMethod.DELETE)
                .uri(USERS_URI + "/secondary-departments/" + department + "/members")
                .body(users)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<User> getAllUserByEmails(List<String> emails) {
        List<User> users = restClient.post()
                .uri(USERS_URI + "/by-emails")
                .body(emails)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return users != null ? users : List.of();
    }

    @Override
    public String getDepartmentByEmail(String email) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + "/department-by-email")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .body(String.class);
    }

    private PaginatedResult<User> fetchPaginated(String path, PaginationData pageData) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + path)
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.getDisplayStart())
                        .queryParam("rowsPerPage", pageData.getRowsPerPage())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private PaginatedResult<User> fetchPaginatedWithBody(String path, PaginationData pageData,
            UserSearchFilterRequest filter) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path(USERS_URI + path)
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.getDisplayStart())
                        .queryParam("rowsPerPage", pageData.getRowsPerPage())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .body(filter)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
