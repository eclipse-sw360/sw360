/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
//import org.eclipse.sw360.users.dto.DepartmentConfigDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ReadFileDepartmentConfig {

    private static final Logger log = LogManager.getLogger(ReadFileDepartmentConfig.class);
    private static final String FOLDER_LOG = "/logs/";

    protected String getPathConfig() throws IOException {
        StringBuilder path = new StringBuilder("/");
        File file = File.createTempFile("check", "text");
        String pathFile = file.getPath();
        String[] parts = pathFile.split("/");
        for (int i = 0; i < parts.length-1; i++) {
            if (!parts[i+1].contains("liferay"))
                path.append(parts[i+1]).append("/");
            else {
                path.append(parts[i+1]).append("/");
                break;
            }
        }
        return (path + "department-config.json");

    }

    public DepartmentConfigDTO readFileJson() {
        try {
            File file = new File(getPathConfig());
            if (file.exists()) {
                Reader reader = Files.newBufferedReader(Paths.get(getPathConfig()));
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(reader);
                JsonNode config = jsonNode.path("configDepartment");
                String pathFolder = config.path("pathFolder").asText();
                String lastRunningTime = config.path("lastRunningTime").asText();
                int showFileLogFrom = config.path("showFileLogFrom").asInt();
                String pathFolderLog = "";
                if (!pathFolder.isEmpty()) pathFolderLog = pathFolder + FOLDER_LOG;
                DepartmentConfigDTO departmentConfigDTO=new DepartmentConfigDTO();
                departmentConfigDTO.setPathFolder(pathFolder);
                departmentConfigDTO.setPathFolderLog(pathFolderLog);
                departmentConfigDTO.setLastRunningTime(lastRunningTime);
                departmentConfigDTO.setShowFileLogFrom(showFileLogFrom);
                return departmentConfigDTO;
            }
        } catch (FileNotFoundException e) {
            log.error("Error not find the file: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Unread file error: {}", e.getMessage());
        }
        return null;
    }

    public void writePathFolderConfig(String pathFolder) {
        BufferedWriter writer = null;
        try {
            DepartmentConfigDTO configDTO = readFileJson();
            writer = Files.newBufferedWriter(Paths.get(getPathConfig()));
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> map = new HashMap<>();
            map.put("pathFolder", pathFolder);
            map.put("lastRunningTime", configDTO.getLastRunningTime());
            map.put("showFileLogFrom", configDTO.getShowFileLogFrom());
            config.put("configDepartment", map);
            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writeValueAsString(config));
        } catch (FileNotFoundException e) {
            log.error("Error not find the file: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Unread file error: {}", e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                log.info("Error close the file!" , e.getMessage());
            }
        }
    }

    public void writeLastRunningTimeConfig(String lastRunningTime) {
        DepartmentConfigDTO configDTO = readFileJson();
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(Paths.get(getPathConfig()));
            Map<String, Object> config = new HashMap<>();
            Map<String, Object> map = new HashMap<>();
            map.put("lastRunningTime", lastRunningTime);
            map.put("pathFolder", configDTO.getPathFolder());
            map.put("showFileLogFrom", configDTO.getShowFileLogFrom());
            config.put("configDepartment", map);
            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writeValueAsString(config));
        } catch (FileNotFoundException e) {
            log.error("Error not find the file: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Unread file error: {}", e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                log.info("Error close the file!" ,e.getMessage());
            }
        }
    }
}