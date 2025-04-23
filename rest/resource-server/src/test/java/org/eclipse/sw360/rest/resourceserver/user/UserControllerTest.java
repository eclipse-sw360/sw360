/*
 * Copyright Rajnish Kumar<rk2452003@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.user;

import org.eclipse.sw360.rest.resourceserver.user.model.UserDTO;
import org.eclipse.sw360.rest.resourceserver.user.model.UserProfileDTO;
import org.eclipse.sw360.rest.resourceserver.user.model.ApiTokenDTO;
import org.eclipse.sw360.rest.resourceserver.user.model.UserPatchDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    @Mock
    private Sw360UserService userService;

    @InjectMocks
    private UserController userController;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetUsers() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = userController.getUsers();
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testGetUserByEmail() {
        String email = "test@example.com";
        UserDTO user = new UserDTO(email);
        when(userService.getUserByEmail(email)).thenReturn(user);

        ResponseEntity<?> response = userController.getUserByEmail(email);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(user, response.getBody());
    }

    @Test
    public void testGetUser() {
        String userId = "userId";
        UserDTO user = new UserDTO(userId);
        when(userService.getUser(userId)).thenReturn(user);

        ResponseEntity<?> response = userController.getUser(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(user, response.getBody());
    }

    @Test
    public void testCreateUser() {
        UserDTO newUser = new UserDTO("new@example.com");
        when(userService.createUser(newUser)).thenReturn(newUser);

        ResponseEntity<?> response = userController.createUser(newUser);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(newUser, response.getBody());
    }

    @Test
    public void testGetUserProfile() {
        String userId = "userId";
        UserProfileDTO profile = new UserProfileDTO();
        when(userService.getUserProfile(userId)).thenReturn(profile);

        ResponseEntity<?> response = userController.getUserProfile(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(profile, response.getBody());
    }

    @Test
    public void testUpdateUserProfile() {
        UserProfileDTO profile = new UserProfileDTO();
        when(userService.updateUserProfile(profile)).thenReturn(profile);

        ResponseEntity<?> response = userController.updateUserProfile(profile);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(profile, response.getBody());
    }

    @Test
    public void testGetUserRestApiTokens() {
        String userId = "userId";
        when(userService.getUserTokens(userId)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getUserRestApiTokens(userId);
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testCreateUserRestApiToken() {
        String userId = "userId";
        ApiTokenDTO token = new ApiTokenDTO();
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
    public void testProcess() {
        // You can add logic here if 'process' is a real method
    }

    @Test
    public void testGetGroupList() {
        when(userService.getGroups()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getGroupList();
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testPatchUser() {
        String userId = "userId";
        UserPatchDTO patch = new UserPatchDTO();
        UserDTO updatedUser = new UserDTO(userId);

        when(userService.patchUser(userId, patch)).thenReturn(updatedUser);

        ResponseEntity<?> response = userController.patchUser(userId, patch);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(updatedUser, response.getBody());
    }

    @Test
    public void testGetExistingDepartments() {
        when(userService.getDepartments()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getExistingDepartments();
        assertEquals(200, response.getStatusCodeValue());
    }
}
