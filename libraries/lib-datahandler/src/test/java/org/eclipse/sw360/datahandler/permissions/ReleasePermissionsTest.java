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
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


/**
 * @author: alex.borodin@evosoft.com
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleasePermissionsTest {

    @Mock
    private Release release;
    @Mock
    private User user;

    @Before
    public void setUp() throws Exception {
        when(user.isSetUserGroup()).thenReturn(true);
    }

    @Test
    public void testEccPermission() {
        ReleasePermissions permissions = new ReleasePermissions(release, user);

        when(user.getUserGroup()).thenReturn(UserGroup.ECC_ADMIN);
        assertThat(permissions.isActionAllowed(RequestedAction.WRITE_ECC), is(true));

        when(user.getUserGroup()).thenReturn(UserGroup.CLEARING_ADMIN);
        assertThat(permissions.isActionAllowed(RequestedAction.WRITE_ECC), is(false));
    }
}
