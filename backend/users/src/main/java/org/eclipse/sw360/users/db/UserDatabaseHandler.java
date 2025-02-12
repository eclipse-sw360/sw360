/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.db.UserSearchHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.users.util.FileUtil;
import org.eclipse.sw360.users.util.ReadFileDepartmentConfig;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserDatabaseHandler {

    private static final String LAST_NAME_IS_MANDATORY = "Last Name is mandatory";
    private static final String GIVEN_NAME_IS_MANDATORY = "Given Name is mandatory";
    /**
     * Connection to the couchDB database
     */
    private DatabaseConnectorCloudant db;
    private UserRepository repository;
    private UserSearchHandler userSearchHandler;
    private static final Logger log = LogManager.getLogger(UserDatabaseHandler.class);
    private ReadFileDepartmentConfig readFileDepartmentConfig;
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String TITLE = "IMPORT";
    private static boolean IMPORT_DEPARTMENT_STATUS = false;
    private List<String> departmentDuplicate;
    private List<String> emailDoNotExist;
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    public UserDatabaseHandler(Cloudant client, String dbName) throws IOException {
        // Create the connector
        db = new DatabaseConnectorCloudant(client, dbName);
        repository = new UserRepository(db);
        readFileDepartmentConfig = new ReadFileDepartmentConfig();
        userSearchHandler = new UserSearchHandler(DatabaseSettings.getConfiguredClient(), dbName);
    }

    public User getByEmail(String email) {
        return repository.getByEmail(email);
    }

    public User getUser(String id) {
        return db.get(User.class, id);
    }

    private void prepareUser(User user) throws SW360Exception {
        // Prepare component for database
        ThriftValidate.prepareUser(user);
    }

    public AddDocumentRequestSummary addUser(User user) throws SW360Exception {
        prepareUser(user);
        AddDocumentRequestSummary addDocReqSummarry = new AddDocumentRequestSummary();
        if (CommonUtils.isNullEmptyOrWhitespace(user.getGivenname())) {
            return addDocReqSummarry.setMessage(GIVEN_NAME_IS_MANDATORY).setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
        } else if (CommonUtils.isNullEmptyOrWhitespace(user.getLastname())) {
            return addDocReqSummarry.setMessage(LAST_NAME_IS_MANDATORY).setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
        }

        User existingUserInDB = getByEmail(user.getEmail());
        if (null != existingUserInDB) {
            return addDocReqSummarry.setId(existingUserInDB.getId())
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
        }
        // Add to database
        db.add(user);

        return addDocReqSummarry.setId(user.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    public RequestStatus updateUser(User user) throws SW360Exception {
        prepareUser(user);
        db.update(user);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteUser(User user, User adminUser) {
        if (makePermission(user, adminUser).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(user);
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    public List<User> getAll() {
        return repository.getAll();
    }

    public List<User> searchUsers(String searchText) {
        return userSearchHandler.searchByNameAndEmail(searchText);
    }

    public User getByExternalId(String externalId) {
        return repository.getByExternalId(externalId);
    }

    public User getByApiToken(String token) {
        return repository.getByApiToken(token);
    }

    public Set<String> getUserDepartments() {
        return repository.getUserDepartments();
    }

    public Set<String> getUserEmails() {
        return repository.getUserEmails();
    }

    public List<User> search(String text, Map<String, Set<String>> subQueryRestrictions) {
        return userSearchHandler.search(text, subQueryRestrictions);
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(PaginationData pageData) {
        return repository.getUsersWithPagination(pageData);
    }

    public Set<String> getAllEmailsByDepartmentKey(String departmentKey) {
        return repository.getEmailsByDepartmentName(departmentKey);
    }

    public RequestSummary importFileToDB(String pathFolder)  {
        departmentDuplicate = new ArrayList<>();
        emailDoNotExist = new ArrayList<>();
        List<String> listFileSuccess = new ArrayList<>();
        List<String> listFileFail = new ArrayList<>();
        RequestSummary requestSummary = new RequestSummary().setTotalAffectedElements(0).setMessage("");
        DepartmentConfigDTO configDTO = readFileDepartmentConfig.readFileJson();
        String pathFolderLog = configDTO.getPathFolderLog();
        Map<String, List<String>> mapArrayList = new HashMap<>();
        if (IMPORT_DEPARTMENT_STATUS) {
            return requestSummary.setRequestStatus(RequestStatus.PROCESSING);
        }
        IMPORT_DEPARTMENT_STATUS = true;
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        String lastRunningTime = dateFormat.format(calendar.getTime());
        readFileDepartmentConfig.writeLastRunningTimeConfig(lastRunningTime);
        try {
            FileUtil.writeLogToFile(TITLE, "START IMPORT DEPARTMENT", "", pathFolderLog);
            Set<String> files = FileUtil.listPathFiles(pathFolder);
            for (String file : files) {
                String extension = FilenameUtils.getExtension(file);
                if (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls")) {
                    mapArrayList = readFileExcel(file);
                } else if (extension.equalsIgnoreCase("csv")) {
                    mapArrayList = readFileCsv(file);
                }
                Map<String, User> mapEmail = validateListEmailExistDB(mapArrayList);
                String fileName = FilenameUtils.getName(file);
                if (departmentDuplicate.isEmpty() && emailDoNotExist.isEmpty()) {
                    mapArrayList.forEach((k, v) -> v.forEach(email -> updateDepartmentToUser(mapEmail.get(email), k)));
                    String joined = mapArrayList.keySet().stream().sorted().collect(Collectors.joining(", "));
                    listFileSuccess.add(fileName);
                    FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] - FILE NAME: [" + fileName + "]", SUCCESS, pathFolderLog);
                } else {
                    if (!departmentDuplicate.isEmpty()) {
                        String joined = departmentDuplicate.stream().sorted().collect(Collectors.joining(", "));
                        FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] IS DUPLICATE - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        departmentDuplicate = new ArrayList<>();
                    }
                    if (!emailDoNotExist.isEmpty()) {
                        String joined = emailDoNotExist.stream().sorted().collect(Collectors.joining(", "));
                        FileUtil.writeLogToFile(TITLE, "USER [" + joined + "] DOES NOT EXIST - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        emailDoNotExist = new ArrayList<>();
                    }
                    listFileFail.add(fileName);
                }
            }
            IMPORT_DEPARTMENT_STATUS = false;
            requestSummary.setTotalAffectedElements(listFileSuccess.size());
            requestSummary.setTotalElements(listFileSuccess.size() + listFileFail.size());
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } catch (IOException e) {
            IMPORT_DEPARTMENT_STATUS = false;
            String msg = "Failed to import department";
            requestSummary.setMessage(msg);
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            FileUtil.writeLogToFile(TITLE, "FILE ERROR: " + e.getMessage(), "", pathFolderLog);
        }
        FileUtil.writeLogToFile(TITLE, "[ FILE SUCCESS: " + listFileSuccess.size() + " - " + "FILE FAIL: " + listFileFail.size() + " - " + "TOTAL FILE: " + (listFileSuccess.size() + listFileFail.size()) + " ]", "Complete The File Import", pathFolderLog);
        FileUtil.writeLogToFile(TITLE, "END IMPORT DEPARTMENT", "", pathFolderLog);

        return requestSummary;
    }

    public RequestSummary importDepartmentFileToDB(DepartmentConfigDTO configDTO)  {
        String pathFolder = configDTO.getPathFolder();
        departmentDuplicate = new ArrayList<>();
        emailDoNotExist = new ArrayList<>();
        List<String> listFileSuccess = new ArrayList<>();
        List<String> listFileFail = new ArrayList<>();
        RequestSummary requestSummary = new RequestSummary().setTotalAffectedElements(0).setMessage("");
        String pathFolderLog = configDTO.getPathFolderLog();
        Map<String, List<String>> mapArrayList = new HashMap<>();
        if (IMPORT_DEPARTMENT_STATUS) {
            return requestSummary.setRequestStatus(RequestStatus.PROCESSING);
        }
        IMPORT_DEPARTMENT_STATUS = true;
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        String lastRunningTime = dateFormat.format(calendar.getTime());
        readFileDepartmentConfig.writeLastRunningTimeConfig(lastRunningTime);
        try {
            FileUtil.writeLogToFile(TITLE, "START IMPORT DEPARTMENT", "", pathFolderLog);
            Set<String> files = FileUtil.listPathFiles(pathFolder);
            for (String file : files) {
                String extension = FilenameUtils.getExtension(file);
                if (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls")) {
                    mapArrayList = readFileExcel(file);
                } else if (extension.equalsIgnoreCase("csv")) {
                    mapArrayList = readFileCsv(file);
                }
                Map<String, User> mapEmail = validateListEmailExistDB(mapArrayList);
                String fileName = FilenameUtils.getName(file);
                if (departmentDuplicate.isEmpty() && emailDoNotExist.isEmpty()) {
                    mapArrayList.forEach((k, v) -> v.forEach(email -> updateDepartmentToUser(mapEmail.get(email), k)));
                    String joined = mapArrayList.keySet().stream().sorted().collect(Collectors.joining(", "));
                    listFileSuccess.add(fileName);
                    FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] - FILE NAME: [" + fileName + "]", SUCCESS, pathFolderLog);
                } else {
                    if (!departmentDuplicate.isEmpty()) {
                        String joined = departmentDuplicate.stream().sorted().collect(Collectors.joining(", "));
                        FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] IS DUPLICATE - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        departmentDuplicate = new ArrayList<>();
                    }
                    if (!emailDoNotExist.isEmpty()) {
                        String joined = emailDoNotExist.stream().sorted().collect(Collectors.joining(", "));
                        FileUtil.writeLogToFile(TITLE, "USER [" + joined + "] DOES NOT EXIST - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        emailDoNotExist = new ArrayList<>();
                    }
                    listFileFail.add(fileName);
                }
            }
            IMPORT_DEPARTMENT_STATUS = false;
            requestSummary.setTotalAffectedElements(listFileSuccess.size());
            requestSummary.setTotalElements(listFileSuccess.size() + listFileFail.size());
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } catch (IOException e) {
            IMPORT_DEPARTMENT_STATUS = false;
            String msg = "Failed to import department";
            requestSummary.setMessage(msg);
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            FileUtil.writeLogToFile(TITLE, "FILE ERROR: " + e.getMessage(), "", pathFolderLog);
        }
        FileUtil.writeLogToFile(TITLE, "[ FILE SUCCESS: " + listFileSuccess.size() + " - " + "FILE FAIL: " + listFileFail.size() + " - " + "TOTAL FILE: " + (listFileSuccess.size() + listFileFail.size()) + " ]", "Complete The File Import", pathFolderLog);
        FileUtil.writeLogToFile(TITLE, "END IMPORT DEPARTMENT", "", pathFolderLog);

        return requestSummary;
    }

    public Map<String, List<String>> readFileCsv(String filePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailCsv = new ArrayList<>();
        try {
            File file = new File(filePath);
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build();
            List<String[]> rows = reader.readAll();
            String mapTemp = "";
            for (String[] row : rows) {
                if (row.length > 1) {
                    if (!Objects.equals(row[0], "")) {
                        if (!mapTemp.isEmpty()) {
                            if (listMap.containsKey(mapTemp)) {
                                departmentDuplicate.add(mapTemp);
                            }
                            listMap.put(mapTemp, emailCsv);
                            emailCsv = new ArrayList<>();
                        }
                        mapTemp = row[0];
                    }
                    String email = row[1];
                    emailCsv.add(email);
                }
            }
            if (listMap.containsKey(mapTemp)) {
                departmentDuplicate.add(mapTemp);
            }
            listMap.put(mapTemp, emailCsv);
        } catch (IOException | CsvException e) {
            log.error("Can't read file csv: {}", e.getMessage());
        }
        return listMap;
    }

    public Map<String, List<String>> readFileExcel(String filePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailExcel = new ArrayList<>();

        Workbook wb = null;
        try (InputStream inp = new FileInputStream(filePath)) {
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            rows.next();
            String mapTemp = "";
            while (rows.hasNext()) {
                Row row = rows.next();
                if (!isRowEmpty(row)) {
                    if (row.getCell(0) != null) {
                        if (!mapTemp.isEmpty()) {
                            if (listMap.containsKey(mapTemp)) {
                                departmentDuplicate.add(mapTemp);
                            }
                            listMap.put(mapTemp, emailExcel);
                            emailExcel = new ArrayList<>();
                        }
                        mapTemp = row.getCell(0).getStringCellValue();
                    }
                    String email = row.getCell(1).getStringCellValue();
                    emailExcel.add(email);
                }
            }
            if (listMap.containsKey(mapTemp)) {
                departmentDuplicate.add(mapTemp);
            }
            listMap.put(mapTemp, emailExcel);
        } catch (IOException ex) {
            log.error("Can't read file excel: {}", ex.getMessage());
        } finally {
            try {
                if (wb != null) wb.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return listMap;
    }

    public static boolean isRowEmpty(Row row) {
        boolean isEmpty = true;
        DataFormatter dataFormatter = new DataFormatter();
        if (row != null) {
            for (Cell cell : row) {
                if (dataFormatter.formatCellValue(cell).trim().length() > 0) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    public Map<String, List<User>> getAllUserByDepartment() {
        List<User> users = repository.getAll();
        Map<String, List<User>> listMap = new HashMap<>();
        for (User user : users) {
            if (user.getSecondaryDepartmentsAndRoles() != null) {
                user.getSecondaryDepartmentsAndRoles().forEach((key, value) -> {
                    if (listMap.containsKey(key)) {
                        List<User> list = listMap.get(key);
                        list.add(user);
                    } else {
                        List<User> list = new ArrayList<>();
                        list.add(user);
                        listMap.put(key, list);
                    }
                });
            }
        }
        return listMap;
    }

    public String convertUsersByDepartmentToJson(String departmentKey) {
        Set<String> emails = repository.getEmailsByDepartmentName(departmentKey);
        JsonArray departmentJsonArray = new JsonArray();
        for (String email : emails) {
            JsonObject object = new JsonObject();
            object.addProperty("email", email);
            departmentJsonArray.add(object);
        }
        return departmentJsonArray.toString().replace("\\", "");
    }

    public List<String> getAllEmailOtherDepartment(String departmentKey) {
        Set<String> emailsbyDepartment = getAllEmailsByDepartmentKey(departmentKey);
        Set<String> emailByListUser = getUserEmails();

        List<User> users = repository.getAll();
        for (User user : users) {
            emailByListUser.add(user.getEmail());
        }
        List<String> emailOtherDepartment = new ArrayList<>(emailByListUser);
        emailOtherDepartment.removeAll(emailsbyDepartment);
        return emailOtherDepartment;
    }

    public String convertEmailsOtherDepartmentToJson(String departmentKey) {
        JsonArray emailJsonArray = new JsonArray();
        List<String> emailOtherDepartment = getAllEmailOtherDepartment(departmentKey);
        for (String email : emailOtherDepartment) {
            JsonObject object = new JsonObject();
            object.addProperty("email", email);
            emailJsonArray.add(object);
        }
        return emailJsonArray.toString().replace("\\", "");
    }

    public Map<String, User> validateListEmailExistDB(Map<String, List<String>> mapList) {
        Map<String, User> listUser = new HashMap<>();
        Set<String> setEmail = new HashSet<>();
        mapList.forEach((v, k) -> setEmail.addAll(k));
        for (String email : setEmail) {
            User user = repository.getByEmail(email);
            if (user == null) {
                emailDoNotExist.add(email);
            } else {
                listUser.put(email, user);
            }
        }
        return listUser;
    }

    public void updateDepartmentToUser(User user, String department) {
        Map<String, Set<UserGroup>> map;
        Set<UserGroup> userGroups = new HashSet<>();
        if (user.getSecondaryDepartmentsAndRoles() != null) {
            map = user.getSecondaryDepartmentsAndRoles();
            for (Map.Entry<String, Set<UserGroup>> entry : map.entrySet()) {
                if (entry.getKey().equals(department)) {
                    userGroups = entry.getValue();
                }
            }
        } else {
            map = new HashMap<>();
        }
        userGroups.add(UserGroup.USER);
        map.put(department, userGroups);
        user.setSecondaryDepartmentsAndRoles(map);
        repository.update(user);
    }

    public void updateDepartmentToUsers(List<User> users, String department) {
        users.forEach(u -> {
            User user = repository.getByEmail(u.getEmail());
            updateDepartmentToUser(user, department);
        });
    }

    public void deleteDepartmentByUser(User user, String departmentKey) {
        Map<String, Set<UserGroup>> map = user.getSecondaryDepartmentsAndRoles();
        Set<UserGroup> userGroups = new HashSet<>();
        for (Map.Entry<String, Set<UserGroup>> entry : map.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                userGroups = entry.getValue();
            }
        }
        map.remove(departmentKey, userGroups);
        user.setSecondaryDepartmentsAndRoles(map);
        repository.update(user);
    }


    public void deleteDepartmentByUsers(List<User> users, String departmentKey) {
        for (User user : users) {
            deleteDepartmentByUser(user, departmentKey);
        }
    }

    public List<User> getAllUserByEmails(List<String> emails) {
        List<User> users = new ArrayList<>();
        for (String email : emails) {
            if (getByEmail(email) != null) {
                users.add(getByEmail(email));
            }
        }
        return users;
    }
    
    public User getByOidcClientId(String clientId) {
        return repository.getByOidcClientId(clientId);
    }

    public List<User> getAllDepartmentUser(String department) {
        List<User> users = repository.getAll();
        List<User> departmentUsers = new ArrayList<>();
        for (User user : users) {
            if (user.getDepartment() != null) {
                if (user.getDepartment().contains(department)) {
                    departmentUsers.add(user);
                }
            }
        }
        return departmentUsers;
    }

    public List<User> getAllUsersGroup(UserGroup userGroup) {
        List<User> users = repository.getAll();
        List<User> userGroups = new ArrayList<>();
        for (User user : users) {
            if (user.getUserGroup() != null) {
                if (user.getUserGroup().equals(userGroup)) {
                    userGroups.add(user);
                }
            }
        }
        return userGroups;
    }
}
