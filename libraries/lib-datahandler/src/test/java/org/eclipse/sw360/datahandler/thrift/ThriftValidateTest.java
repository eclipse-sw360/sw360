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
package org.eclipse.sw360.datahandler.thrift;

import org.junit.Test;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;
import static org.junit.Assert.assertEquals;
import org.eclipse.sw360.datahandler.thrift.users.User;

import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareUser;
import static org.junit.Assert.assertFalse;

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

        assertEquals(user.getEmail(), user.getId());
        assertEquals(TYPE_USER,user.getType());
        assertFalse(user.isSetCommentMadeDuringModerationRequest());
    }
}
