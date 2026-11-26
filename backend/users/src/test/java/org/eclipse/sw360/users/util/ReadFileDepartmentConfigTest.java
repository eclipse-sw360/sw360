/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.users.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReadFileDepartmentConfigTest {
    @TempDir
    Path directory;

    @Test
    void shouldPreserveConfigurationReadAndWrite() throws Exception {
        Path path = directory.resolve("department-config.json");
        String input = "{\"configDepartment\":{\"pathFolder\":\"base\",\"lastRunningTime\":\"yesterday\","
                + "\"showFileLogFrom\":3}}";
        Files.writeString(path, input);
        ReadFileDepartmentConfig config = configFor(path);

        DepartmentConfigDTO actual = config.readFileJson();
        assertEquals("base", actual.getPathFolder());
        assertEquals("base/logs/", actual.getPathFolderLog());
        assertEquals("yesterday", actual.getLastRunningTime());
        assertEquals(3, actual.getShowFileLogFrom());

        config.writePathFolderConfig("updated");
        config.writeLastRunningTimeConfig("today");
        JsonNode actualJson = new ObjectMapper().readTree(Files.readString(path));
        JsonNode expected = new ObjectMapper().readTree(
                "{\"configDepartment\":{\"pathFolder\":\"updated\",\"lastRunningTime\":\"today\","
                        + "\"showFileLogFrom\":3}}");
        assertEquals(expected, actualJson);
    }

    @Test
    void shouldPreserveDefaultsAndMalformedInputBehavior() throws Exception {
        Path path = directory.resolve("department-config.json");
        Files.writeString(path, "{\"configDepartment\":{}}");
        ReadFileDepartmentConfig config = configFor(path);

        DepartmentConfigDTO actual = config.readFileJson();
        assertEquals("", actual.getPathFolder());
        assertEquals("", actual.getLastRunningTime());
        assertEquals(0, actual.getShowFileLogFrom());

        String nullValues = "{\"configDepartment\":{\"pathFolder\":null,\"lastRunningTime\":null}}";
        Files.writeString(path, nullValues);
        actual = config.readFileJson();
        assertEquals("null", actual.getPathFolder());
        assertEquals("null", actual.getLastRunningTime());

        Files.writeString(path, "{");
        assertNull(config.readFileJson());
    }

    private static ReadFileDepartmentConfig configFor(Path path) {
        return new ReadFileDepartmentConfig() {
            @Override
            protected String getPathConfig() {
                return path.toString();
            }
        };
    }
}
