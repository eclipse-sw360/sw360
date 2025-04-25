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

import org.eclipse.sw360.rest.resourceserver.user.dto.UserDTO;
import org.eclipse.sw360.rest.resourceserver.user.dto.UserProfileDTO;
import org.eclipse.sw360.rest.resourceserver.user.dto.ApiTokenDTO;
import org.eclipse.sw360.rest.resourceserver.user.dto.UserPatchDTO;

import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private final EntityLinks entityLinks = mock(EntityLinks.class);
    private final Sw360UserService userService = mock(Sw360UserService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RestControllerHelper restControllerHelper = mock(RestControllerHelper.class);

    private final UserController userController = new UserController(
            entityLinks,
            userService,
            passwordEncoder,
            restControllerHelper
    );

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
        UserProfileDTO profile = new UserProfileDTO(userId, "John Doe", "Engineering");
        when(userService.getUserProfile(userId)).thenReturn(profile);

        ResponseEntity<?> response = userController.getUserProfile(userId);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(profile, response.getBody());
    }

    @Test
    public void testUpdateUserProfile() {
        UserProfileDTO profile = new UserProfileDTO("userId", "John Doe", "Engineering");
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
        ApiTokenDTO token = new ApiTokenDTO("token123", "2025-04-23");
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
        when(userService.getGroups()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = userController.getGroupList();
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    public void testPatchUser() {
        String userId = "userId";
        UserPatchDTO patch = new UserPatchDTO("Updated Name");
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
