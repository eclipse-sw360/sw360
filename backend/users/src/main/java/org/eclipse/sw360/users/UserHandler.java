/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.DepartmentConfigDTO;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.eclipse.sw360.users.util.FileUtil;
import org.eclipse.sw360.users.util.ReadFileDepartmentConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * User service business logic.
 *
 * @author cedric.bodet@tngtech.com
 */
@Service
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
            } catch (Exception e) {
                log.atError().withThrowable(e).log("Error creating admin user");
            }
        }
    }

    public UserHandler(Cloudant client, String userDbName) throws IOException {
        db = new UserDatabaseHandler(client, userDbName);
        readFileDepartmentConfig = new ReadFileDepartmentConfig();
    }

    public User getUser(String id) {
        User user = db.getUser(id);
        assertNotNull(user);
        return user;
    }

    public User getByEmail(String email) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        assertNotEmpty(email, "Invalid empty email " + stackTraceElement.getFileName() + ": "
                + stackTraceElement.getLineNumber());

        if (log.isTraceEnabled())
            log.trace("getByEmail: " + email);

        return db.getByEmail(email);
    }

    public User getByEmailOrExternalId(String email, String externalId) {
        User user = getByEmail(email);
        if (user == null) {
            user = db.getByExternalId(externalId);
        }
        if (user != null && Boolean.TRUE.equals(user.getDeactivated())) {
            return null;
        }
        if (user != null
                && externalId != null
                && !externalId.isEmpty()) {
            String currentExternalId = user.getExternalid();
            if (!externalId.equals(email)
                    && (CommonUtils.isNullEmptyOrWhitespace(currentExternalId)
                    || !currentExternalId.equals(externalId))) {
                log.info("Updating differing externalId for user: {} | was: {} | now: {}",
                        email, currentExternalId, externalId);
                user.setExternalid(externalId);
                db.updateUser(user);
            }
        }

        return user;
    }

    public User getByApiToken(String token) {
        assertNotEmpty(token);
        return db.getByApiToken(token);
    }

    public User getByOidcClientId(String clientId) {
        assertNotEmpty(clientId);
        return db.getByOidcClientId(clientId);
    }

    public List<User> searchUsers(String searchText) {
        return db.searchUsers(searchText);
    }

    public List<User> getAllUsers() {
        return db.getAll();
    }

    public AddDocumentRequestSummary addUser(User user) {
        assertNotNull(user);
        assertNotEmpty(user.getEmail(), "User Email was empty in " + user);
        assertNotEmpty(user.getDepartment(), "User Department was empty in " + user);
        return db.addUser(user);
    }

    public RequestStatus updateUser(User user) {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.updateUser(user);
    }

    public RequestStatus deleteUser(User user, User adminUser) {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        assertNotNull(adminUser);
        return db.deleteUser(user, adminUser);
    }

    public String getDepartmentByEmail(String email) {
        User user = getByEmail(email);
        return user != null ? user.getDepartment() : null;
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(PaginationData pageData) {
        return db.getUsersWithPagination(pageData);
    }

    public Map<PaginationData, List<User>> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        return db.search(text, subQueryRestrictions, pageData);
    }

    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String,Set<String>> subQueryRestrictions, PaginationData pageData) {
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
        if (configDTO != null && configDTO.getPathFolder() != null && !configDTO.getPathFolder().isEmpty()) {
            requestSummary = db.importFileToDB(configDTO.getPathFolder());
        }
        return requestSummary;
    }

    public RequestStatus importDepartmentSchedule() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && configDTO.getPathFolder() != null) {
            db.importFileToDB(configDTO.getPathFolder());
        }
        return RequestStatus.SUCCESS;
    }

    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() {
        return db.getSecondaryDepartmentMemberEmails();
    }

    public Set<String> getListFileLog() {
        try {
            DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
            if (configDTO != null && configDTO.getPathFolderLog() != null && !configDTO.getPathFolderLog().isEmpty()) {
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

    public List<String> getLogFileContentByName(String fileName) {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && configDTO.getPathFolderLog() != null && configDTO.getPathFolderLog().length() > 0) {
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
            if (configDTO != null && configDTO.getPathFolderLog() != null && !configDTO.getPathFolderLog().isEmpty()) {
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
        if (configDTO != null && configDTO.getPathFolder() != null && !configDTO.getPathFolder().isEmpty()) {
            return configDTO.getPathFolder();
        }
        return "";
    }

    public void writePathFolderConfig(String pathFolder) {
        readFileDepartmentConfig.writePathFolderConfig(pathFolder);
    }

    public String getLastRunningTime() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && configDTO.getLastRunningTime() != null && !configDTO.getLastRunningTime().isEmpty()) {
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

    private static void assertNotNull(Object value) {
        if (value == null) {
            throw new SW360Exception("Unexpected null value");
        }
    }

    private static void assertNotEmpty(String value) {
        assertNotEmpty(value, "Unexpected empty value");
    }

    private static void assertNotEmpty(String value, String message) {
        if (value == null || value.isEmpty()) {
            throw new SW360Exception(message);
        }
    }
}
