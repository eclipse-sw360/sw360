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
import org.eclipse.sw360.datahandler.thrift.users.UserProfile;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class UserControllerTest {

    private UserService.Iface userServiceMock;
    private UserController userController;
    private User mockUser;

    @Before
    public void setUp() {
        // Create a mock of the UserService
        userServiceMock = mock(UserService.Iface.class);

        // Create an instance of UserController using the mock service
        userController = new UserController(new Sw360UserService(userServiceMock));

        // Create a mock user for tests
        mockUser = new User();
        mockUser.setPassword("initialPassword");
    }

    @Test
    public void testUpdatePassword() {
        String newPassword = "newPassword";

        // Call updatePassword method of UserController
        userController.updatePassword(mockUser, newPassword);

        // Assert that the password was updated
        assertEquals("newPassword", mockUser.getPassword());
    }

    @Test
    public void testGetUserProfile() throws Exception {
        // Prepare a mock UserProfile
        UserProfile mockProfile = new UserProfile();
        mockProfile.setUserId("user123");
        mockProfile.setName("John Doe");
        mockProfile.setEmail("john.doe@example.com");

        // Mock the getUserProfile method of the Sw360UserService
        when(userServiceMock.getUserProfile("user123")).thenReturn(mockProfile);

        // Call the getUserProfile method of UserController
        UserProfile profile = userController.getUserProfile("user123");

        // Assert that the profile returned matches the mock profile
        assertNotNull(profile);
        assertEquals("user123", profile.getUserId());
        assertEquals("John Doe", profile.getName());
        assertEquals("john.doe@example.com", profile.getEmail());
    }

    @Test
    public void testUpdateUserProfile() throws Exception {
        // Create some mock data for the user profile update
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Jane Doe");
        profileData.put("email", "jane.doe@example.com");

        // Call the updateUserProfile method of UserController
        userController.updateUserProfile(profileData);

        // Verify that the updateUserProfile method of the Sw360UserService was called
        verify(userServiceMock).updateUserProfile(profileData);
    }

    @Test
    public void testGetUserPassword() {
        // Get the user's password using the UserController
        String password = userController.getUserPassword(mockUser);

        // Assert that the password is what was set earlier
        assertEquals("initialPassword", password);
    }

    @Test
    public void testCreateUserToken() throws Exception {
        String userId = "user123";
        String expectedToken = "tokenXYZ";

        // Mock the createUserToken method
        when(userServiceMock.createUserToken(userId)).thenReturn(expectedToken);

        // Call the createUserToken method of UserController
        String token = userController.createUserToken(userId);

        // Assert that the token returned matches the expected token
        assertEquals(expectedToken, token);
    }

    @Test
    public void testRevokeToken() throws Exception {
        String userId = "user123";
        String token = "tokenXYZ";

        // Call the revokeToken method of UserController
        userController.revokeToken(userId, token);

        // Verify that the revokeToken method of Sw360UserService was called with correct arguments
        verify(userServiceMock).revokeToken(userId, token);
    }
}
