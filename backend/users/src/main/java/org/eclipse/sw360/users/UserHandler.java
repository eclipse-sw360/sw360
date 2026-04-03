/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;


import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import com.ibm.cloud.cloudant.v1.Cloudant;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.eclipse.sw360.users.util.FileUtil;
import org.eclipse.sw360.users.util.ReadFileDepartmentConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Business-logic handler for the user service.
 *
 * <p>Previously implemented {@code UserService.Iface} (the Apache Thrift generated interface)
 * and was exposed via a Thrift servlet.  It is now a plain Spring {@code @Service} bean
 * exposed over HTTP/REST by {@link UserRestController}, removing the Thrift transport layer.
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserHandler {

    private static final Logger log = LogManager.getLogger(UserHandler.class);
    private static final String EXTENSION = ".log";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserDatabaseHandler db;
    private ReadFileDepartmentConfig readFileDepartmentConfig;

    public UserHandler() throws IOException {
        db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_USERS);
        readFileDepartmentConfig = new ReadFileDepartmentConfig();

        // Create admin user if not in database yet
        List<User> users = getAllUsers();
        if (users.isEmpty()) {
            Optional<String> COUCHDB_ADMIN_EMAIL =
                    Optional.ofNullable(System.getenv("COUCHDB_ADMIN_EMAIL") != null
                            ? System.getenv("COUCHDB_ADMIN_EMAIL")
                            : "setup@sw360.org");
            Optional<String> COUCHDB_ADMIN_PASSWORD =
                    Optional.ofNullable(System.getenv("COUCHDB_ADMIN_PASSWORD") != null
                            ? System.getenv("COUCHDB_ADMIN_PASSWORD")
                            : "sw360fossie");
            User admin = new User();
            admin.setEmail(COUCHDB_ADMIN_EMAIL.get());
            admin.setFullname("SW360 Admin");
            admin.setGivenname("SW360");
            admin.setLastname("Admin");
            admin.setDepartment("SW360");
            admin.setPassword(COUCHDB_ADMIN_PASSWORD.get());
            admin.setUserGroup(UserGroup.ADMIN);
            String encodedPassword = passwordEncoder.encode(admin.getPassword());
            admin.setPassword(encodedPassword);
            log.info("No users found. Creating default administrator user.");
            try {
                addUser(admin);
            } catch (SW360Exception e) {
                log.atError().withThrowable(e).log("Error creating admin user");
            }
        }
    }

    public UserHandler(Cloudant client, String userDbName) throws IOException {
        db = new UserDatabaseHandler(client, userDbName);
    }

    public User getUser(String id) throws SW360Exception {
        User user = db.getUser(id);
        assertNotNull(user);
        return user;
    }

    public User getByEmail(String email) throws SW360Exception {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        assertNotEmpty(email, "Invalid empty email " + stackTraceElement.getFileName() + ": "
                + stackTraceElement.getLineNumber());

        if (log.isTraceEnabled())
            log.trace("getByEmail: " + email);

        return db.getByEmail(email);
    }

    public User getByEmailOrExternalId(String email, String externalId) throws SW360Exception {
        User user = getByEmail(email);
        if (user == null) {
            user = db.getByExternalId(externalId);
        }
        if (user != null && user.isDeactivated()) {
            return null;
        }
        return user;
    }

    public User getByApiToken(String token) throws SW360Exception {
        assertNotEmpty(token);
        return db.getByApiToken(token);
    }

    public User getByOidcClientId(String clientId) throws SW360Exception {
        assertNotEmpty(clientId);
        return db.getByOidcClientId(clientId);
    }

    public List<User> searchUsers(String searchText) {
        return db.searchUsers(searchText);
    }

    public List<User> getAllUsers() {
        return db.getAll();
    }

    public AddDocumentRequestSummary addUser(User user) throws SW360Exception {
        assertUser(user);
        return db.addUser(user);
    }

    public RequestStatus updateUser(User user) throws SW360Exception {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.updateUser(user);
    }

    public RequestStatus deleteUser(User user, User adminUser) throws SW360Exception {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.deleteUser(user, adminUser);
    }

    public String getDepartmentByEmail(String email) throws SW360Exception {
        User user = getByEmail(email);
        return user != null ? user.getDepartment() : null;
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(User user,
            PaginationData pageData) {
        return db.getUsersWithPagination(pageData);
    }

    public Map<PaginationData, List<User>> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        return db.search(text, subQueryRestrictions, pageData);
    }

    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        return db.searchUsersByExactValues(subQueryRestrictions, pageData);
    }

    public Set<String> getUserDepartments() {
        return db.getUserDepartments();
    }

    public Set<String> getMemberEmailsBySecondaryDepartmentName(String departmentName) {
        return db.getAllEmailsBySecondaryDepartmentName(departmentName);
    }

    public Set<String> getUserEmails() {
        return db.getUserEmails();
    }

    public RequestSummary importFileToDB() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        RequestSummary requestSummary = new RequestSummary();
        if (!configDTO.getPathFolder().isEmpty()) {
            requestSummary = db.importFileToDB(configDTO.getPathFolder());
        }
        return requestSummary;
    }

    public RequestStatus importDepartmentSchedule() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        db.importFileToDB(configDTO.getPathFolder());
        return RequestStatus.SUCCESS;
    }

    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() {
        return db.getSecondaryDepartmentMemberEmails();
    }

    public Set<String> getListFileLog() {
        try {
            DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
            if (configDTO != null && !configDTO.getPathFolderLog().isEmpty()) {
                String path = configDTO.getPathFolderLog();
                File theDir = new File(path);
                if (!theDir.exists())
                    theDir.mkdirs();
                return FileUtil.getListFilesOlderThanNDays(configDTO.getShowFileLogFrom(), path);
            }
        } catch (IOException e) {
            log.error("Can't get file log: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    public List<String> getLogFileContentByName(String fileName) throws SW360Exception {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && configDTO.getPathFolderLog().length() > 0) {
            String logFolderPath = configDTO.getPathFolderLog();
            File theDir = new File(logFolderPath);
            if (!theDir.exists())
                theDir.mkdirs();
            String logFilePath = Paths.get(logFolderPath, fileName + EXTENSION).toString();
            return FileUtil.readFileLog(logFilePath);
        }
        return Collections.emptyList();
    }

    public String getLastModifiedFileName() {
        try {
            DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
            if (configDTO != null && !configDTO.getPathFolderLog().isEmpty()) {
                String path = configDTO.getPathFolderLog();
                File theDir = new File(path);
                if (!theDir.exists())
                    theDir.mkdirs();
                Set<String> strings = FileUtil.listFileNames(path);
                if (!strings.isEmpty()) {
                    File file = FileUtil.getFileLastModified(path);
                    return file.getName().replace(EXTENSION, "");
                }
            }
        } catch (IOException e) {
            log.error("Read file failed!", e.getMessage());
        }
        return "";
    }

    public String getPathConfigDepartment() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && !configDTO.getPathFolder().isEmpty()) {
            return configDTO.getPathFolder();
        }
        return "";
    }

    public void writePathFolderConfig(String pathFolder) {
        readFileDepartmentConfig.writePathFolderConfig(pathFolder);
    }

    public String getLastRunningTime() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && !configDTO.getLastRunningTime().isEmpty()) {
            return configDTO.getLastRunningTime();
        }
        return "";
    }

    public void updateDepartmentToListUser(List<User> users, String department) {
        db.updateDepartmentToUsers(users, department);
    }

    public void deleteSecondaryDepartmentFromListUser(List<User> users, String department) {
        db.deleteSecondaryDepartmentFromListUser(users, department);
    }

    public List<User> getAllUserByEmails(List<String> emails) {
        return db.getAllUserByEmails(emails);
    }

    public List<User> searchDepartmentUsers(String department) {
        return db.getAllDepartmentUser(department);
    }

    public List<User> searchUsersGroup(UserGroup userGroup) {
        return db.getAllUsersGroup(userGroup);
    }

    public Set<String> getUserSecondaryDepartments() {
        return db.getUserSecondaryDepartments();
    }
}
