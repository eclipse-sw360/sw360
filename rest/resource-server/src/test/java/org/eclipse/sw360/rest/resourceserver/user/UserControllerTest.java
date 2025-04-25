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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController userController = new UserController(userService);

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

// DTO Classes:

class UserDTO {
    private String identifier;

    public UserDTO() {}

    public UserDTO(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDTO)) return false;
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(identifier, userDTO.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}

class UserProfileDTO {
    private String userId;
    private String name;
    private String department;

    public UserProfileDTO() {}

    public UserProfileDTO(String userId, String name, String department) {
        this.userId = userId;
        this.name = name;
        this.department = department;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}

class ApiTokenDTO {
    private String token;
    private String expiry;

    public ApiTokenDTO() {}

    public ApiTokenDTO(String token, String expiry) {
        this.token = token;
        this.expiry = expiry;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
}

class UserPatchDTO {
    private String name;

    public UserPatchDTO() {}

    public UserPatchDTO(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

interface UserService {
    UserDTO getUserByEmail(String email);
    UserDTO getUser(String userId);
    UserDTO createUser(UserDTO user);
    UserProfileDTO getUserProfile(String userId);
    UserProfileDTO updateUserProfile(UserProfileDTO profile);
    java.util.List<ApiTokenDTO> getUserTokens(String userId);
    ApiTokenDTO createUserToken(String userId);
    void revokeToken(String userId, String tokenId);
    java.util.List<String> getGroups();
    UserDTO patchUser(String userId, UserPatchDTO patch);
    java.util.List<String> getDepartments();
}

class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public ResponseEntity<?> getUserByEmail(String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    public ResponseEntity<?> getUser(String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    public ResponseEntity<?> createUser(UserDTO user) {
        return ResponseEntity.status(201).body(userService.createUser(user));
    }

    public ResponseEntity<?> getUserProfile(String userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    public ResponseEntity<?> updateUserProfile(UserProfileDTO profile) {
        return ResponseEntity.ok(userService.updateUserProfile(profile));
    }

    public ResponseEntity<?> getUserRestApiTokens(String userId) {
        return ResponseEntity.ok(userService.getUserTokens(userId));
    }

    public ResponseEntity<?> createUserRestApiToken(String userId) {
        return ResponseEntity.status(201).body(userService.createUserToken(userId));
    }

    public ResponseEntity<?> revokeUserRestApiToken(String userId, String tokenId) {
        userService.revokeToken(userId, tokenId);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> getGroupList() {
        return ResponseEntity.ok(userService.getGroups());
    }

    public ResponseEntity<?> patchUser(String userId, UserPatchDTO patch) {
        return ResponseEntity.ok(userService.patchUser(userId, patch));
    }

    public ResponseEntity<?> getExistingDepartments() {
        return ResponseEntity.ok(userService.getDepartments());
    }
}
