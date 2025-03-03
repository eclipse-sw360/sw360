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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
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
            throw new TException("Failed to create UserService client");
        }
        userClient.writePathFolderConfig(pathFolder);
    }
}

