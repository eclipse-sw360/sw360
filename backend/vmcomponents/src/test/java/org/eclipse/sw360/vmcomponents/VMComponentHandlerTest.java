/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VMComponentHandlerTest {

    @Test
    void getAllMatchesReturnsEmptyForNonAdmin() throws Exception {
        VMComponentHandler handler = new VMComponentHandler();
        User user = new User();
        user.setEmail("user@test.com");
        user.setUserGroup(UserGroup.USER);

        List<org.eclipse.sw360.datahandler.services.vmcomponents.VMMatch> matches = handler.getAllMatches(user);

        assertTrue(matches.isEmpty());
    }
}
