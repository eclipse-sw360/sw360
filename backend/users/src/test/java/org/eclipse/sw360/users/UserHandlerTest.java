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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class UserHandlerTest {
    private static final String DUMMY_LASTNAME = "Dummy Lastname";

    private static final String DUMMY_GIVENNAME = "Dummy Givenname";

    private static final String DUMMY_EMAIL_ADDRESS_1 = "dummy.user1@dummy.domain.tld";
    private static final String DUMMY_EMAIL_ADDRESS_2 = "dummy.user2@dummy.domain.tld";
    private static final String DUMMY_COMMENT = "Lorem ipsum";
    private static final String DUMMY_DEPARTMENT = "DummyDepartment";

    @Autowired
    UserHandler handler;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
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
}
