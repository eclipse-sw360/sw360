/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link UserDatabaseHandler} that do not require live user data.
 */
public class UserDatabaseHandlerTest {

    private static final String DB_NAME = DatabaseSettingsTest.COUCH_DB_USERS;

    private UserDatabaseHandler handler;

    @Before
    public void setUp() throws Exception {
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
        handler = new UserDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), DB_NAME);
    }

    @Test
    public void testReadFileCsv_parsesGroupsAndEmails() {
        String path = getClass().getClassLoader()
                .getResource("test-department-users.csv").getPath();

        Map<String, List<String>> result = handler.readFileCsv(path);

        assertEquals("Expected two department entries", 2, result.size());
        assertTrue("Engineering entry should exist", result.containsKey("Engineering"));
        assertEquals("Engineering should have two members",
                List.of("alice@example.org", "bob@example.org"),
                result.get("Engineering"));
        assertTrue("Legal entry should exist", result.containsKey("Legal"));
        assertEquals("Legal should have one member",
                List.of("carol@example.org"),
                result.get("Legal"));
    }

    @Test
    public void testReadFileCsv_nonExistentFile_returnsEmptyMap() {
        Map<String, List<String>> result = handler.readFileCsv("/nonexistent/path/file.csv");
        assertTrue("Should return empty map for missing file", result.isEmpty());
    }
}
