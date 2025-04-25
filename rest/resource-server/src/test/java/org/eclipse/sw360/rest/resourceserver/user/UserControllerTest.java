/*******************************************************************************
 * Copyright (C) 2025 Rajnish Kumar <rk2452003@gmail.com>
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.sw360.rest.resourceserver.user;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private EntityLinks entityLinks;

    @Mock
    private Sw360UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestControllerHelper restControllerHelper;

    @InjectMocks
    private UserController userController;

    @Test
    public void testGetUserByEmail() {
        String email = "test@example.com";
        User user = new User();
        when(userService.getUserByEmail(email)).thenReturn(user);

        ResponseEntity<?> response = userController.getUserByEmail(email);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(user, response.getBody());
    }

    @Test
    public void testGetUser() {
        String userId = "userId";
        User user = new User();
        when(userService.getUser(userId)).thenReturn(user);

        ResponseEntity<?> response = userController.getUser(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(user, response.getBody());
    }

    @Test
    public void testCreateUser() {
        User newUser = new User();
        when(userService.createUser(newUser)).thenReturn(newUser);

        ResponseEntity<?> response = userController.createUser(newUser);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(newUser, response.getBody());
    }

    @Test
    public void testGetUserProfile() {
        String userId = "userId";
        Map<String, Object> profile = new HashMap<>();
        when(userService.getUserProfile(userId)).thenReturn(profile);

        ResponseEntity<?> response = userController.getUserProfile(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(profile, response.getBody());
    }

    @Test
    public void testUpdateUserProfile() {
        Map<String, Object> profile = new HashMap<>();
        when(userService.updateUserProfile(profile)).thenReturn(profile);

        ResponseEntity<?> response = userController.updateUserProfile(profile);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(profile, response.getBody());
    }

    @Test
    public void testGetUserRestApiTokens() {
        String userId = "userId";
        List<Map<String, Object>> tokens = Collections.emptyList();
        when(userService.getUserTokens(userId)).thenReturn(tokens);

        ResponseEntity<?> response = userController.getUserRestApiTokens(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(tokens, response.getBody());
    }

    @Test
    public void testCreateUserRestApiToken() {
        String userId = "userId";
        Map<String, Object> token = new HashMap<>();
        when(userService.createUserToken(userId)).thenReturn(token);

        ResponseEntity<?> response = userController.createUserRestApiToken(userId);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(token, response.getBody());
    }

    @Test
    public void testRevokeUserRestApiToken() {
        String userId = "userId";
        String tokenId = "tokenId";
        doNothing().when(userService).revokeToken(userId, tokenId);

        ResponseEntity<?> response = userController.revokeUserRestApiToken(userId, tokenId);
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    public void testGetGroupList() {
        List<String> groups = Arrays.asList("Engineering", "Legal");
        when(userService.getGroups()).thenReturn(groups);

        ResponseEntity<?> response = userController.getGroupList();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(groups, response.getBody());
    }

    @Test
    public void testPatchUser() {
        String userId = "userId";
        Map<String, Object> patch = new HashMap<>();
        User patchedUser = new User();
        when(userService.patchUser(userId, patch)).thenReturn(patchedUser);

        ResponseEntity<?> response = userController.patchUser(userId, patch);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(patchedUser, response.getBody());
    }

    @Test
    public void testGetExistingDepartments() {
        List<String> departments = Arrays.asList("R&D", "Sales");
        when(userService.getDepartments()).thenReturn(departments);

        ResponseEntity<?> response = userController.getExistingDepartments();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(departments, response.getBody());
    }
}
