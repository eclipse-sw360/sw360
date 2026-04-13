/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class UserHandlerTest {
    private static final String DUMMY_LASTNAME = "Dummy Lastname";

    private static final String DUMMY_GIVENNAME = "Dummy Givenname";

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_USERS;

    private static final String DUMMY_EMAIL_ADDRESS_1 = "dummy.user1@dummy.domain.tld";
    private static final String DUMMY_EMAIL_ADDRESS_2 = "dummy.user2@dummy.domain.tld";
    private static final String DUMMY_COMMENT = "Lorem ipsum";
    private static final String DUMMY_DEPARTMENT = "DummyDepartment";

    UserHandler handler;
    private UserDatabaseHandler dbHandler;

    @Before
    public void setUp() throws Exception {
        // Create the database
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);

        // Create the connectors
        handler = new UserHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
        dbHandler = new UserDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
    }

    @Test
    public void testAddUser() throws Exception {
        User userWithComment = new User().setEmail(DUMMY_EMAIL_ADDRESS_1).setCommentMadeDuringModerationRequest(DUMMY_COMMENT).setGivenname(DUMMY_GIVENNAME).setLastname(DUMMY_LASTNAME).setDepartment(DUMMY_DEPARTMENT);

        handler.addUser(userWithComment);

        User userFromDatabase = handler.getByEmail(DUMMY_EMAIL_ADDRESS_1);
        assertEquals(DUMMY_EMAIL_ADDRESS_1, userFromDatabase.getEmail());
        assertFalse(userFromDatabase.isSetCommentMadeDuringModerationRequest());
    }

    @Test
    public void testUpdateUser() throws Exception {
        User userWithoutComment = new User().setEmail(DUMMY_EMAIL_ADDRESS_2).setGivenname(DUMMY_GIVENNAME).setLastname(DUMMY_LASTNAME).setDepartment(DUMMY_DEPARTMENT);

        handler.addUser(userWithoutComment); // does not contain a comment

        // update `userWithoutComment` with some stuff
        userWithoutComment.setDepartment(DUMMY_DEPARTMENT);
        userWithoutComment.setCommentMadeDuringModerationRequest(DUMMY_COMMENT);
        handler.updateUser(userWithoutComment); // now contains a comment

        User userFromDatabase = handler.getByEmail(DUMMY_EMAIL_ADDRESS_2);
        assertEquals(DUMMY_EMAIL_ADDRESS_2, userFromDatabase.getEmail());
        assertEquals(DUMMY_DEPARTMENT, userFromDatabase.getDepartment());
        assertFalse(userFromDatabase.isSetCommentMadeDuringModerationRequest());
    }

    @Test
    public void testReadFileCsv_parsesGroupsAndEmails() throws Exception {
        File csv = File.createTempFile("dept-users", ".csv");
        csv.deleteOnExit();
        Files.write(csv.toPath(),
                "Department,Email\nEngineering,alice@example.org\n,bob@example.org\nLegal,carol@example.org\n"
                        .getBytes());

        Map<String, List<String>> result = dbHandler.readFileCsv(csv.getAbsolutePath());

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
        Map<String, List<String>> result = dbHandler.readFileCsv("/nonexistent/path/file.csv");
        assertTrue("Should return empty map for missing file", result.isEmpty());
    }
}
