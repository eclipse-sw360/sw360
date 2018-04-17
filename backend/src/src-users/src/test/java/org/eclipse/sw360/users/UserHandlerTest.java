/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.users;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class UserHandlerTest {
    private static final String dbName = DatabaseSettings.COUCH_DB_USERS;

    private static final String DUMMY_EMAIL_ADDRESS_1 = "dummy.user1@dummy.domain.tld";
    private static final String DUMMY_EMAIL_ADDRESS_2 = "dummy.user2@dummy.domain.tld";
    private static final String DUMMY_COMMENT = "Lorem ipsum";
    private static final String DUMMY_DEPARTMENT = "DummyDepartment";

    UserHandler handler;

    @Before
    public void setUp() throws Exception {
        // Create the database
        TestUtils.createDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);

        // Create the connector
        handler = new UserHandler();
    }

    @After
    public void tearDown() throws Exception {
        // Delete the database
        TestUtils.deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), dbName);
    }

    @Test
    public void testAddUser() throws Exception {
        User userWithComment = new User().setEmail(DUMMY_EMAIL_ADDRESS_1).setCommentMadeDuringModerationRequest(DUMMY_COMMENT);

        handler.addUser(userWithComment);

        User userFromDatabase = handler.getByEmail(DUMMY_EMAIL_ADDRESS_1);
        assertEquals(DUMMY_EMAIL_ADDRESS_1, userFromDatabase.getEmail());
        assertFalse(userFromDatabase.isSetCommentMadeDuringModerationRequest());
    }

    @Test
    public void testUpdateUser() throws Exception {
        User userWithoutComment = new User().setEmail(DUMMY_EMAIL_ADDRESS_2);

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
}
