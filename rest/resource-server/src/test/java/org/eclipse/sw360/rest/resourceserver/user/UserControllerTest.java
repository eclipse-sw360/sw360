package org.eclipse.sw360.rest.resourceserver.user; /*******************************************************************************
 * Copyright (C) 2025 Rajnish Kumar <rk2452003@gmail.com>
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private Sw360UserService sw360UserService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setId("user123");
        testUser.setEmail("user@example.com");
        // Set other necessary fields
    }

    @Test
    public void testGetUserByEmail() {
        when(sw360UserService.getUserByEmail("user@example.com")).thenReturn(testUser);

        ResponseEntity<User> response = userController.getUserByEmail("user@example.com");

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("user123", response.getBody().getId());
    }

    // Add more test cases as needed
}
