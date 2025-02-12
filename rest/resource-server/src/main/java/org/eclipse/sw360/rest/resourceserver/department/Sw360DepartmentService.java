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
    private static final String CONFIG_FILE = "/home/nikesh/git/Dummy/sw360/liferay/department-config.json";
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    ThriftClients thriftClients = new ThriftClients();

    public RequestSummary importDepartmentManually(User sw360User) throws TException {
        try {
            if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                throw new AccessDeniedException("User is not admin");
            }

            UserService.Iface userClient = thriftClients.makeUserClient();

            DepartmentConfigDTO configDTO;
            try {
                configDTO = readFileJson();
            } catch (IOException e) {
                log.error("Error reading configuration file", e);
                throw new TException("Error reading configuration file", e);
            }
            if (configDTO == null) {
                throw new TException("Configuration file is invalid or missing required fields");
            }
            return userClient.importDepartmentData(configDTO);
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

    public void writePathFolderConfig(String pathFolder) throws IOException, TException {
        DepartmentConfigDTO configDTO = readFileJson();
        if (configDTO == null) {
            throw new TException("Failed to read JSON config. ConfigDTO is null.");
        }

        Map<String, Object> config = new HashMap<>();
        Map<String, Object> map = new HashMap<>();

        map.put("pathFolder", pathFolder);
        map.put("lastRunningTime", configDTO.getLastRunningTime());
        map.put("showFileLogFrom", configDTO.getShowFileLogFrom());
        config.put("configDepartment", map);

        ObjectMapper objectMapper = new ObjectMapper();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
            objectMapper.writeValue(writer, config);
        } catch (IOException e) {
            throw new TException("Error writing to configuration file: " + e.getMessage(), e);
        }
    }

    public DepartmentConfigDTO readFileJson() throws IOException {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            throw new FileNotFoundException("Configuration file not found.");
        }

        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reader);
            JsonNode config = jsonNode.path("configDepartment");

            DepartmentConfigDTO dto = new DepartmentConfigDTO();
            dto.setPathFolder(config.path("pathFolder").asText());
            dto.setLastRunningTime(config.path("lastRunningTime").asText());
            dto.setShowFileLogFrom(config.path("showFileLogFrom").asInt());

            return dto;
        }
    }
}

