/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.PagedUsersResult;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.users.dto.DeleteUserRequest;
import org.eclipse.sw360.users.dto.UpdateDepartmentRequest;
import org.eclipse.sw360.users.dto.UserExactSearchRequest;
import org.eclipse.sw360.users.dto.UserSearchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring REST controller for the Users backend service.
 *
 * <p>Replaces the former Apache Thrift {@code UserServlet} + {@code TServlet} transport stack.
 * Every method delegates directly to the {@link UserHandler} Spring bean; HTTP status codes
 * are derived from {@link SW360Exception#getErrorCode()} where applicable.
 *
 * <p>This controller is intentionally a thin translation layer — all business logic
 * remains inside {@link UserHandler} and {@code UserDatabaseHandler}.
 */
@RestController
@RequestMapping("/users")
public class UserRestController {

    private static final Logger log = LogManager.getLogger(UserRestController.class);

    private final UserHandler userHandler;

    public UserRestController(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    // -------------------------------------------------------------------------
    // Simple lookups
    // -------------------------------------------------------------------------

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userHandler.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        try {
            return ResponseEntity.ok(userHandler.getUser(id));
        } catch (SW360Exception e) {
            return ResponseEntity.status(e.getErrorCode()).build();
        }
    }

    @GetMapping("/byEmail")
    public ResponseEntity<User> getByEmail(@RequestParam String email) {
        try {
            User user = userHandler.getByEmail(email);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    @GetMapping("/byEmailOrExternalId")
    public ResponseEntity<User> getByEmailOrExternalId(
            @RequestParam String email,
            @RequestParam String externalId) {
        try {
            User user = userHandler.getByEmailOrExternalId(email, externalId);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    @GetMapping("/byApiToken")
    public ResponseEntity<User> getByApiToken(@RequestParam String token) {
        try {
            User user = userHandler.getByApiToken(token);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    @GetMapping("/byOidcClientId")
    public ResponseEntity<User> getByOidcClientId(@RequestParam String clientId) {
        try {
            User user = userHandler.getByOidcClientId(clientId);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    @GetMapping("/searchUsers")
    public List<User> searchUsers(@RequestParam String searchText) {
        return userHandler.searchUsers(searchText);
    }

    @GetMapping("/byDepartment")
    public List<User> searchDepartmentUsers(@RequestParam String department) {
        return userHandler.searchDepartmentUsers(department);
    }

    @GetMapping("/byGroup")
    public List<User> searchUsersGroup(@RequestParam UserGroup userGroup) {
        return userHandler.searchUsersGroup(userGroup);
    }

    @PostMapping("/byEmails")
    public List<User> getAllUserByEmails(@RequestBody List<String> emails) {
        return userHandler.getAllUserByEmails(emails);
    }

    // -------------------------------------------------------------------------
    // CRUD mutations
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<AddDocumentRequestSummary> addUser(@RequestBody User user) {
        try {
            AddDocumentRequestSummary summary = userHandler.addUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(summary);
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<RequestStatus> updateUser(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userHandler.updateUser(user));
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    /**
     * Deletes a user.  Both the user to be deleted and the acting admin user
     * are required in the request body.
     */
    @DeleteMapping
    public ResponseEntity<RequestStatus> deleteUser(@RequestBody DeleteUserRequest request) {
        try {
            return ResponseEntity.ok(userHandler.deleteUser(request.getUser(), request.getAdminUser()));
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Paginated / search queries
    // -------------------------------------------------------------------------

    /**
     * Returns a page of users.  Replaces {@code UserService.Iface#getUsersWithPagination}.
     */
    @PostMapping("/page")
    public PagedUsersResult getUsersWithPagination(@RequestBody PaginationData pageData) {
        return PagedUsersResult.from(userHandler.getUsersWithPagination(null, pageData));
    }

    /**
     * Full-text / Lucene search.  Replaces {@code UserService.Iface#refineSearch}.
     */
    @PostMapping("/search")
    public PagedUsersResult refineSearch(@RequestBody UserSearchRequest request) {
        return PagedUsersResult.from(
                userHandler.refineSearch(request.getText(), request.getFilterMap(), request.getPageData()));
    }

    /**
     * Exact-value search with Mango queries.  Replaces {@code UserService.Iface#searchUsersByExactValues}.
     */
    @PostMapping("/searchExact")
    public PagedUsersResult searchUsersByExactValues(@RequestBody UserExactSearchRequest request) {
        return PagedUsersResult.from(
                userHandler.searchUsersByExactValues(request.getFilterMap(), request.getPageData()));
    }

    // -------------------------------------------------------------------------
    // Department / metadata queries
    // -------------------------------------------------------------------------

    @GetMapping("/departments")
    public Set<String> getUserDepartments() {
        return userHandler.getUserDepartments();
    }

    @GetMapping("/secondaryDepartments")
    public Set<String> getUserSecondaryDepartments() {
        return userHandler.getUserSecondaryDepartments();
    }

    @GetMapping("/emails")
    public Set<String> getUserEmails() {
        return userHandler.getUserEmails();
    }

    @GetMapping("/secondaryMemberEmails")
    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() {
        return userHandler.getSecondaryDepartmentMemberEmails();
    }

    @GetMapping("/secondaryDepartmentMemberEmails")
    public Set<String> getMemberEmailsBySecondaryDepartmentName(@RequestParam String departmentName) {
        return userHandler.getMemberEmailsBySecondaryDepartmentName(departmentName);
    }

    @PutMapping("/department")
    public ResponseEntity<Void> updateDepartmentToListUser(@RequestBody UpdateDepartmentRequest request) {
        userHandler.updateDepartmentToListUser(request.getUsers(), request.getDepartment());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/secondaryDepartment")
    public ResponseEntity<Void> deleteSecondaryDepartmentFromListUser(@RequestBody UpdateDepartmentRequest request) {
        userHandler.deleteSecondaryDepartmentFromListUser(request.getUsers(), request.getDepartment());
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Department file / schedule imports
    // -------------------------------------------------------------------------

    @PostMapping("/import")
    public RequestSummary importFileToDB() {
        return userHandler.importFileToDB();
    }

    @PostMapping("/importSchedule")
    public RequestStatus importDepartmentSchedule() {
        return userHandler.importDepartmentSchedule();
    }

    @GetMapping("/departmentPath")
    public String getPathConfigDepartment() {
        return userHandler.getPathConfigDepartment();
    }

    @PostMapping("/departmentPath")
    public ResponseEntity<Void> writePathFolderConfig(@RequestParam String pathFolder) {
        userHandler.writePathFolderConfig(pathFolder);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/departmentLastRunTime")
    public String getLastRunningTime() {
        return userHandler.getLastRunningTime();
    }

    // -------------------------------------------------------------------------
    // File log queries (department import audit)
    // -------------------------------------------------------------------------

    @GetMapping("/fileLog")
    public Set<String> getListFileLog() {
        return userHandler.getListFileLog();
    }

    @GetMapping("/fileLog/lastModified")
    public String getLastModifiedFileName() {
        return userHandler.getLastModifiedFileName();
    }

    @GetMapping("/fileLog/{fileName}")
    public ResponseEntity<List<String>> getLogFileContentByName(@PathVariable String fileName) {
        try {
            return ResponseEntity.ok(userHandler.getLogFileContentByName(fileName));
        } catch (SW360Exception e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getErrorCode()), e.getMessage());
        }
    }
}
