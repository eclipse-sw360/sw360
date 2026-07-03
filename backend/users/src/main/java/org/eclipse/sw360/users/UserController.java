/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.users.PathFolderConfigRequest;
import org.eclipse.sw360.datahandler.services.users.UpdateDepartmentUsersRequest;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserDeleteRequest;
import org.eclipse.sw360.datahandler.services.users.UserEmailExternalIdRequest;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.datahandler.services.users.UserSearchFilterRequest;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserHandler userHandler;

    public UserController(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) throws SW360Exception {
        return UserRestMapper.fromThriftUser(userHandler.getUser(id));
    }

    @GetMapping("/by-email")
    public User getByEmail(@RequestParam String email) throws TException {
        return UserRestMapper.fromThriftUser(userHandler.getByEmail(email));
    }

    @GetMapping("/by-api-token")
    public User getByApiToken(@RequestParam String token) throws TException {
        return UserRestMapper.fromThriftUser(userHandler.getByApiToken(token));
    }

    @GetMapping("/by-oidc-client-id")
    public User getByOidcClientId(@RequestParam String clientId) throws TException {
        return UserRestMapper.fromThriftUser(userHandler.getByOidcClientId(clientId));
    }

    @PostMapping("/by-email-or-external-id")
    public User getByEmailOrExternalId(@RequestBody UserEmailExternalIdRequest request) throws TException {
        return UserRestMapper.fromThriftUser(
                userHandler.getByEmailOrExternalId(request.getEmail(), request.getExternalId()));
    }

    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String searchText) {
        return UserRestMapper.fromThriftUsers(userHandler.searchUsers(searchText));
    }

    @GetMapping("/search/department")
    public List<User> searchDepartmentUsers(@RequestParam String department) throws TException {
        return UserRestMapper.fromThriftUsers(userHandler.searchDepartmentUsers(department));
    }

    @GetMapping("/search/group")
    public List<User> searchUsersGroup(@RequestParam UserGroup userGroup) throws TException {
        return UserRestMapper.fromThriftUsers(
                userHandler.searchUsersGroup(UserRestMapper.toThriftUserGroup(userGroup)));
    }

    @GetMapping
    public List<User> getAllUsers() {
        return UserRestMapper.fromThriftUsers(userHandler.getAllUsers());
    }

    @PostMapping
    public AddDocumentRequestSummary addUser(@RequestBody User user) throws TException {
        return UserRestMapper.fromThriftAddSummary(userHandler.addUser(UserRestMapper.toThriftUser(user)));
    }

    @PutMapping
    public RequestStatus updateUser(@RequestBody User user) throws TException {
        return UserRestMapper.fromThriftRequestStatus(userHandler.updateUser(UserRestMapper.toThriftUser(user)));
    }

    @DeleteMapping
    public RequestStatus deleteUser(@RequestBody UserDeleteRequest request) throws TException {
        return UserRestMapper.fromThriftRequestStatus(userHandler.deleteUser(
                UserRestMapper.toThriftUser(request.getUser()),
                UserRestMapper.toThriftUser(request.getAdminUser())));
    }

    @GetMapping("/department-by-email")
    public String getDepartmentByEmail(@RequestParam String email) throws TException {
        return userHandler.getDepartmentByEmail(email);
    }

    @GetMapping("/page")
    public PaginatedResult<User> getUsersWithPagination(@ModelAttribute PaginationData pageData) throws TException {
        Map<PaginationData, List<User>> result = UserRestMapper.fromThriftPaginatedUsers(
                userHandler.getUsersWithPagination(null, UserRestMapper.toThriftPagination(pageData)));
        Map.Entry<PaginationData, List<User>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @PostMapping("/refine-search")
    public PaginatedResult<User> refineSearch(
            @RequestBody UserSearchFilterRequest filter,
            @ModelAttribute PaginationData pageData) throws TException {
        Map<PaginationData, List<User>> result = UserRestMapper.fromThriftPaginatedUsers(
                userHandler.refineSearch(filter.getText(), filter.getSubQueryRestrictions(),
                        UserRestMapper.toThriftPagination(pageData)));
        Map.Entry<PaginationData, List<User>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @PostMapping("/search/exact")
    public PaginatedResult<User> searchUsersByExactValues(
            @RequestBody UserSearchFilterRequest filter,
            @ModelAttribute PaginationData pageData) throws TException {
        Map<PaginationData, List<User>> result = UserRestMapper.fromThriftPaginatedUsers(
                userHandler.searchUsersByExactValues(filter.getSubQueryRestrictions(),
                        UserRestMapper.toThriftPagination(pageData)));
        Map.Entry<PaginationData, List<User>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @GetMapping("/departments")
    public Set<String> getUserDepartments() throws TException {
        return userHandler.getUserDepartments();
    }

    @GetMapping("/emails")
    public Set<String> getUserEmails() throws TException {
        return userHandler.getUserEmails();
    }

    @PostMapping("/import")
    public RequestSummary importFileToDB() {
        return UserRestMapper.fromThriftRequestSummary(userHandler.importFileToDB());
    }

    @PostMapping("/import/schedule")
    public RequestStatus importDepartmentSchedule() {
        return UserRestMapper.fromThriftRequestStatus(userHandler.importDepartmentSchedule());
    }

    @GetMapping("/secondary-departments/members")
    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() throws TException {
        return userHandler.getSecondaryDepartmentMemberEmails();
    }

    @GetMapping("/log-files")
    public Set<String> getListFileLog() {
        return userHandler.getListFileLog();
    }

    @GetMapping("/log-files/last-modified")
    public String getLastModifiedFileName() throws TException {
        return userHandler.getLastModifiedFileName();
    }

    @GetMapping("/config/path")
    public String getPathConfigDepartment() throws TException {
        return userHandler.getPathConfigDepartment();
    }

    @PutMapping("/config/path")
    public void writePathFolderConfig(@RequestBody PathFolderConfigRequest request) throws TException {
        userHandler.writePathFolderConfig(request.getPathFolder());
    }

    @GetMapping("/config/last-running-time")
    public String getLastRunningTime() throws TException {
        return userHandler.getLastRunningTime();
    }

    @PostMapping("/by-emails")
    public List<User> getAllUserByEmails(@RequestBody List<String> emails) throws TException {
        return UserRestMapper.fromThriftUsers(userHandler.getAllUserByEmails(emails));
    }

    @PutMapping("/secondary-departments/{department}/members")
    public void updateDepartmentToListUser(
            @PathVariable String department,
            @RequestBody List<User> users) throws TException {
        userHandler.updateDepartmentToListUser(UserRestMapper.toThriftUsers(users), department);
    }

    @DeleteMapping("/secondary-departments/{department}/members")
    public void deleteSecondaryDepartmentFromListUser(
            @PathVariable String department,
            @RequestBody List<User> users) throws TException {
        userHandler.deleteSecondaryDepartmentFromListUser(UserRestMapper.toThriftUsers(users), department);
    }

    @GetMapping("/secondary-departments/{department}/emails")
    public Set<String> getMemberEmailsBySecondaryDepartmentName(@PathVariable String department) throws TException {
        return userHandler.getMemberEmailsBySecondaryDepartmentName(department);
    }

    @GetMapping("/log-files/{fileName}/content")
    public List<String> getLogFileContentByName(@PathVariable String fileName) throws SW360Exception {
        return userHandler.getLogFileContentByName(fileName);
    }

    @GetMapping("/secondary-departments")
    public Set<String> getUserSecondaryDepartments() throws TException {
        return userHandler.getUserSecondaryDepartments();
    }
}
