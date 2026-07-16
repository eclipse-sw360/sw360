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
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.clients.users.UsersClient;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Sw360DepartmentService {
    private static final Logger log = LogManager.getLogger(Sw360DepartmentService.class);

    private final UsersClient usersClient;
    private final ScheduleRestClient scheduleRestClient;

    public RequestSummary importDepartmentManually(User sw360User) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        return RequestSummaryConverter.toThrift(usersClient.importFileToDB());
    }

    public boolean isDepartmentScheduled(User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new AccessDeniedException("User is not an admin");
        }
        RequestStatusWithBoolean requestStatus = scheduleRestClient
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
        RequestStatusWithBoolean requestStatus = scheduleRestClient
                .isServiceScheduled(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
        if (RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus()) && requestStatus.isAnswerPositive()) {
            throw new SW360Exception("Department import is already scheduled.");
        }
        return scheduleRestClient.scheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
    }

    public RequestStatus unScheduleImportDepartment(User user) throws TException {
        return scheduleRestClient.unscheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
    }

    public void writePathFolderConfig(String pathFolder, User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new AccessDeniedException("User is not an admin");
        }
        usersClient.writePathFolderConfig(pathFolder);
    }

    public Map<String, Object> getImportInformation(User user) throws TException {
        Map<String, Object> response = new HashMap<>();
        response.put(SW360Constants.IMPORT_DEPARTMENT_IS_SCHEDULED, isDepartmentScheduled(user));
        response.put(SW360Constants.IMPORT_DEPARTMENT_FOLDER_PATH, usersClient.getPathConfigDepartment());
        response.put(SW360Constants.IMPORT_DEPARTMENT_LAST_RUNNING_TIME, usersClient.getLastRunningTime());
        response.put(SW360Constants.IMPORT_DEPARTMENT_INTERVAL, CommonUtils.formatTime(
                scheduleRestClient.getInterval(ThriftClients.IMPORT_DEPARTMENT_SERVICE)));
        response.put(SW360Constants.IMPORT_DEPARTMENT_NEXT_RUNNING_TIME,
                scheduleRestClient.getNextSync(ThriftClients.IMPORT_DEPARTMENT_SERVICE));
        return response;
    }

    public Set<String> getLogFileList() throws TException {
        return usersClient.getListFileLog();
    }

    public List<String> getLogFileContentByDate(String date) throws TException {
        if (!isValidDate(date)) {
            throw new SW360Exception("Invalid date time format, must be: yyyy-MM-dd");
        }
        try {
            return usersClient.getLogFileContentByName(date);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Log file for the requested date can not be read");
        }
    }

    private static boolean isValidDate(String dateStr) {
        if (CommonUtils.isNullEmptyOrWhitespace(dateStr)) {
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
            return usersClient.getSecondaryDepartmentMemberEmails();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, List<String>> getMemberEmailsBySecondaryDepartmentName(String departmentName) {
        try {
            List<String> memberEmails = List.copyOf(
                    usersClient.getMemberEmailsBySecondaryDepartmentName(departmentName));
            return Map.of(departmentName, memberEmails);
        } catch (Exception e) {
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

    private void deleteMembersFromDepartment(String departmentName, List<String> removedMemberEmails) {
        List<org.eclipse.sw360.datahandler.services.users.User> members = getUsersByEmails(removedMemberEmails);
        usersClient.deleteSecondaryDepartmentFromListUser(members, departmentName);
    }

    private void addMembersToDepartment(String departmentName, List<String> addedMemberEmails) {
        List<org.eclipse.sw360.datahandler.services.users.User> members = getUsersByEmails(addedMemberEmails);
        usersClient.updateDepartmentToListUser(members, departmentName);
    }

    private List<org.eclipse.sw360.datahandler.services.users.User> getUsersByEmails(List<String> emails) {
        try {
            return usersClient.getAllUserByEmails(emails);
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }
}
