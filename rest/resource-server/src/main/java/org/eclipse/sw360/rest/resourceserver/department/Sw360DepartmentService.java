/*
 * Copyright Siemens AG,2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.department;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class Sw360DepartmentService {
    private static final Logger log = LogManager.getLogger(Sw360DepartmentService.class);
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    ThriftClients thriftClients = new ThriftClients();

    public RequestSummary importDepartmentManually(User sw360User) throws TException {
        try {
            if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                throw new AccessDeniedException("User is not admin");
            }
            UserService.Iface userClient = thriftClients.makeUserClient();
            return userClient.importFileToDB();
        } catch (TException e) {
            log.error("Error occurred while scheduling service", e);
            throw e;
        }
    }

    public boolean isDepartmentScheduled(User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new AccessDeniedException("User is not an admin");
        }
        ScheduleService.Iface scheduleClient = thriftClients.makeScheduleClient();
        RequestStatusWithBoolean requestStatus = scheduleClient
                .isServiceScheduled(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
        if (RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())) {
            return requestStatus.isAnswerPositive();
        } else {
            throw new SW360Exception("Backend query for schedule status of department failed.");
        }
    }

    public RequestSummary scheduleImportDepartment(User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new AccessDeniedException("User is not an admin");
        }
        ScheduleService.Iface scheduleClient = thriftClients.makeScheduleClient();
        RequestStatusWithBoolean requestStatus = scheduleClient
                .isServiceScheduled(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
        if (RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus()) && requestStatus.isAnswerPositive()) {
            throw new SW360Exception("Department import is already scheduled.");
        }
        return scheduleClient.scheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
    }

    public RequestStatus unScheduleImportDepartment(User user) throws TException {
        return thriftClients.makeScheduleClient().unscheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE,
                user);
    }

    public void writePathFolderConfig(String pathFolder, User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new AccessDeniedException("User is not an admin");
        }
        UserService.Iface userClient = thriftClients.makeUserClient();
        if (userClient == null) {
            throw new SW360Exception("Failed to create UserService client");
        }
        userClient.writePathFolderConfig(pathFolder);
    }

    public Map<String, Object> getImportInformation(User user) throws TException {
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        ScheduleService.Iface scheduleClient = thriftClients.makeScheduleClient();

        Map<String, Object> response = new HashMap<>();
        response.put(SW360Constants.IMPORT_DEPARTMENT_IS_SCHEDULED, isDepartmentScheduled(user));
        response.put(SW360Constants.IMPORT_DEPARTMENT_FOLDER_PATH, userClient.getPathConfigDepartment());
        response.put(SW360Constants.IMPORT_DEPARTMENT_LAST_RUNNING_TIME, userClient.getLastRunningTime());
        response.put(SW360Constants.IMPORT_DEPARTMENT_INTERVAL, CommonUtils.formatTime(scheduleClient.getInterval(ThriftClients.IMPORT_DEPARTMENT_SERVICE)));
        response.put(SW360Constants.IMPORT_DEPARTMENT_NEXT_RUNNING_TIME, scheduleClient.getNextSync(ThriftClients.IMPORT_DEPARTMENT_SERVICE));
        return response;
    }

    public Set<String> getLogFileList() throws TException {
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();

        return userClient.getListFileLog();
    }

    public List<String> getLogFileContentByDate(String date) throws TException {
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        if (isValidDate(date)) {
            throw new SW360Exception("Invalid date time format, must be: yyyy-MM-dd");
        }
        try {
            return userClient.getLogFileContentByName(date);
        } catch (SW360Exception sw360Exception) {
            throw new ResourceNotFoundException("Log file for the requested date can not be read");
        }
    }

    private static boolean isValidDate(String dateStr) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(dateStr)) {
            return false;
        }
        final String DATE_FORMAT = "yyyy-MM-dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        try {
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public Map<String, List<String>> getSecondaryDepartmentMembers() {
        try {
            ThriftClients thriftClients = new ThriftClients();
            UserService.Iface userClient = thriftClients.makeUserClient();
            return userClient.getSecondaryDepartmentMemberEmails();
        } catch (TException e) {
            log.error(e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, List<String>> getMemberEmailsBySecondaryDepartmentName(String departmentName) {
        try {
            ThriftClients thriftClients = new ThriftClients();
            UserService.Iface userClient = thriftClients.makeUserClient();
            List<String> memberEmails = List.copyOf(userClient.getMemberEmailsBySecondaryDepartmentName(departmentName));
            return Map.of(departmentName, memberEmails);
        } catch (TException e) {
            log.error(e.getMessage());
            return Map.of(departmentName, Collections.emptyList());
        }
    }

    public void updateMembersInDepartment(String departmentName, List<String> newMembersList) throws TException {
        List<String> currentMembersEmails = getMemberEmailsBySecondaryDepartmentName(departmentName).get(departmentName);
        List<String> deleteMembersEmails = new ArrayList<>(currentMembersEmails);
        deleteMembersEmails.removeAll(newMembersList);

        List<String> addedMembersEmails = new ArrayList<>(newMembersList);
        addedMembersEmails.removeAll(currentMembersEmails);

        deleteMembersFromDepartment(departmentName, deleteMembersEmails);
        addMembersToDepartment(departmentName, addedMembersEmails);
    }

    private void deleteMembersFromDepartment(String departmentName, List<String> removedMemberEmails) throws TException {
        List<User> addedMembers = getUsersByEmails(removedMemberEmails);
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        userClient.deleteSecondaryDepartmentFromListUser(addedMembers, departmentName);
    }

    private void addMembersToDepartment(String departmentName, List<String> addedMemberEmails) throws TException {
        List<User> addedMembers = getUsersByEmails(addedMemberEmails);
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        userClient.updateDepartmentToListUser(addedMembers, departmentName);
    }

    private List<User> getUsersByEmails(List<String> emails) {
        try {
            ThriftClients thriftClients = new ThriftClients();
            UserService.Iface userClient = thriftClients.makeUserClient();
            return userClient.getAllUserByEmails(emails);
        } catch (TException exception) {
            return Collections.emptyList();
        }
    }
}
