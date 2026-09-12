/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thrift;

import org.junit.Test;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.users.User;

import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_OBLIGATION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareTodo;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThriftValidateTest {
    final String DUMMY_EMAIL_ADDRESS = "dummy.name@dummy.domain.tld";
    final String DUMMY_MODERATION_COMMENT = "Lorem ipsum";

    @Test(expected = SW360Exception.class)
    public void testPrepareUserExceptionThrownIfNoEmailAddress() throws Exception {
        // User without email address
        User blankUser = new User();
        prepareUser(blankUser);
    }

    @Test
    public void testPrepareUser() throws Exception {
        User user = new User();
        user.setEmail(DUMMY_EMAIL_ADDRESS);
        user.setCommentMadeDuringModerationRequest(DUMMY_MODERATION_COMMENT);
        prepareUser(user);

        assertNull(user.getId());
        assertEquals(TYPE_USER, user.getType());
        assertFalse(user.isSetCommentMadeDuringModerationRequest());
    }

    @Test
    public void testPrepareTodoNormalizesTabCharacters() throws Exception {
        Obligation obligation = new Obligation();
        obligation.setTitle("Test Obligation");
        obligation.setText("Line1\n\tIndented\tsegment");
        obligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);

        prepareTodo(obligation);

        assertEquals(TYPE_OBLIGATION, obligation.getType());
        assertFalse(obligation.getText().contains("\t"));
        assertEquals("Line1\n Indented segment", obligation.getText());
        assertTrue(obligation.getWhitelist().isEmpty());
    }
}
