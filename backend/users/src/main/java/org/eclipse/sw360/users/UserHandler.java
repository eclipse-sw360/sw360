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
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.eclipse.sw360.users.util.FileUtil;
import org.eclipse.sw360.users.util.ReadFileDepartmentConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
@Component
public class UserHandler implements UserService.Iface, InitializingBean {

    private static final Logger log = LogManager.getLogger(UserHandler.class);
    private static final String EXTENSION = ".log";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserDatabaseHandler db;
    private ReadFileDepartmentConfig readFileDepartmentConfig;

    @Override
    public void afterPropertiesSet() {
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
            } catch (TException e) {
                log.atError().withThrowable(e).log("Error creating admin user");
            }
        }
    }

    @Override
    public User getUser(String id) throws SW360Exception {
        User user = db.getUser(id);
        assertNotNull(user);
        return user;
    }

    @Override
    public User getByEmail(String email) throws TException {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        assertNotEmpty(email, "Invalid empty email " + stackTraceElement.getFileName() + ": "
                + stackTraceElement.getLineNumber());

        if (log.isTraceEnabled())
            log.trace("getByEmail: " + email);

        return db.getByEmail(email);
    }

    @Override
    public User getByEmailOrExternalId(String email, String externalId) throws TException {
        User user = getByEmail(email);
        if (user == null) {
            user = db.getByExternalId(externalId);
        }
        if (user != null && user.isDeactivated()) {
            return null;
        }
        return user;
    }

    @Override
    public User getByApiToken(String token) throws TException {
        assertNotEmpty(token);
        return db.getByApiToken(token);
    }

    @Override
    public User getByOidcClientId(String clientId) throws TException {
        assertNotEmpty(clientId);
        return db.getByOidcClientId(clientId);
    }

    @Override
    public List<User> searchUsers(String searchText) {
        return db.searchUsers(searchText);
    }

    @Override
    public List<User> getAllUsers() {
        return db.getAll();
    }

    @Override
    public AddDocumentRequestSummary addUser(User user) throws TException {
        assertUser(user);
        return db.addUser(user);
    }

    @Override
    public RequestStatus updateUser(User user) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.updateUser(user);
    }

    @Override
    public RequestStatus deleteUser(User user, User adminUser) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.deleteUser(user, adminUser);
    }

    @Override
    public String getDepartmentByEmail(String email) throws TException {
        User user = getByEmail(email);
        return user != null ? user.getDepartment() : null;
    }

    @Override
    public Map<PaginationData, List<User>> getUsersWithPagination(User user,
            PaginationData pageData) throws TException {
        return db.getUsersWithPagination(pageData);
    }

    @Override
    public Map<PaginationData, List<User>> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, PaginationData pageData)
            throws TException {
        return db.search(text, subQueryRestrictions, pageData);
    }

    @Override
    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String,Set<String>> subQueryRestrictions, PaginationData pageData) throws TException {
        return db.searchUsersByExactValues(subQueryRestrictions, pageData);
    }

    @Override
    public Set<String> getUserDepartments() throws TException {
        return db.getUserDepartments();
    }

    @Override
    public Set<String> getMemberEmailsBySecondaryDepartmentName(String departmentName) throws TException {
        return db.getAllEmailsBySecondaryDepartmentName(departmentName);
    }

    public Set<String> getUserEmails() throws TException {
        return db.getUserEmails();
    }

    @Override
    public RequestSummary importFileToDB() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        RequestSummary requestSummary = new RequestSummary();
        if (!configDTO.getPathFolder().isEmpty()) {
            requestSummary = db.importFileToDB(configDTO.getPathFolder());
        }
        return requestSummary;
    }

    @Override
    public RequestStatus importDepartmentSchedule() {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        db.importFileToDB(configDTO.getPathFolder());
        return RequestStatus.SUCCESS;
    }

    @Override
    public Map<String, List<String>> getSecondaryDepartmentMemberEmails() throws TException {
        return db.getSecondaryDepartmentMemberEmails();
    }

    @Override
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

    @Override
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

    @Override
    public String getLastModifiedFileName() throws TException {
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

    @Override
    public String getPathConfigDepartment() throws TException {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && !configDTO.getPathFolder().isEmpty()) {
            return configDTO.getPathFolder();
        }
        return "";
    }

    @Override
    public void writePathFolderConfig(String pathFolder) throws TException {
        readFileDepartmentConfig.writePathFolderConfig(pathFolder);
    }

    @Override
    public String getLastRunningTime() throws TException {
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        if (configDTO != null && !configDTO.getLastRunningTime().isEmpty()) {
            return configDTO.getLastRunningTime();
        }
        return "";
    }

    @Override
    public void updateDepartmentToListUser(List<User> users, String department) throws TException {
        db.updateDepartmentToUsers(users, department);
    }

    @Override
    public void deleteSecondaryDepartmentFromListUser(List<User> users, String department) throws TException {
        db.deleteSecondaryDepartmentFromListUser(users, department);
    }

    @Override
    public List<User> getAllUserByEmails(List<String> emails) throws TException {
        return db.getAllUserByEmails(emails);
    }

    @Override
    public List<User> searchDepartmentUsers(String department) throws TException {
        return db.getAllDepartmentUser(department);
    }

    @Override
    public List<User> searchUsersGroup(UserGroup userGroup) throws TException {
        return db.getAllUsersGroup(userGroup);
    }

    @Override
    public Set<String> getUserSecondaryDepartments() throws TException {
        return db.getUserSecondaryDepartments();
    }
}
