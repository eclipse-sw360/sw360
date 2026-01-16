/*
 * Copyright Siemens AG, 2024-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.keycloak.event.listener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.events.Event;
import org.keycloak.models.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Sw360KeycloakUserEventService}.
 * Tests cover user event handling and organization mapping integration.
 */
@RunWith(MockitoJUnitRunner.class)
public class Sw360KeycloakUserEventServiceTest {

    @Mock
    private Sw360UserService sw360UserService;

    @Mock
    private KeycloakSession keycloakSession;

    @Mock
    private RealmProvider realmProvider;

    @Mock
    private UserProvider userProvider;

    @Mock
    private RealmModel realmModel;

    @Mock
    private UserModel userModel;

    private ObjectMapper objectMapper;
    private Sw360KeycloakUserEventService userEventService;

    private static final String TEST_EMAIL = "test.user@example.com";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_DEPARTMENT = "Engineering";

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        userEventService = new Sw360KeycloakUserEventService(sw360UserService, objectMapper, keycloakSession);

        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.users()).thenReturn(userProvider);
        when(realmProvider.getRealmByName("sw360")).thenReturn(realmModel);
    }


    /**
     * Tests that user login event correctly processes user with department.
     * Verifies that OrganizationMapper is invoked for department mapping.
     */
    @Test
    public void testUserLoginEvent_WithDepartment_AppliesOrgMapping() {

        Event loginEvent = createLoginEvent();
        setupUserModelMock(TEST_DEPARTMENT);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertNotNull("User should not be null", capturedUser);
        assertEquals("Email should match", TEST_EMAIL, capturedUser.getEmail());
        // Department should be processed through OrganizationMapper
        // Since custom mapping is disabled by default, original department is returned
        assertEquals("Department should be set", TEST_DEPARTMENT, capturedUser.getDepartment());
    }

    /**
     * Tests that user login event handles department with spaces correctly.
     * Only the first part before space should be used.
     */
    @Test
    public void testUserLoginEvent_WithSpaceSeparatedDepartment_UsesFirstPart() {
        String departmentWithSpaces = "Engineering Sales Marketing";
        Event loginEvent = createLoginEvent();
        setupUserModelMock(departmentWithSpaces);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        // sanitizeDepartment splits on whitespace and takes first part
        assertEquals("Should use first department only", "Engineering", capturedUser.getDepartment());
    }

    /**
     * Tests that user login event applies OrganizationMapper to sanitized department.
     */
    @Test
    public void testUserLoginEvent_DepartmentPassedThroughOrganizationMapper() {
        String originalDepartment = "EXTERNAL_COMPANY";
        Event loginEvent = createLoginEvent();
        setupUserModelMock(originalDepartment);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        // With default config (custom mapping disabled), original name is returned
        String expectedDepartment = OrganizationMapper.mapOrganizationName(originalDepartment);
        assertEquals("Department should be mapped through OrganizationMapper",
                expectedDepartment, capturedUser.getDepartment());
    }

    /**
     * Tests that user login event handles default department when not provided.
     */
    @Test
    public void testUserLoginEvent_WithMissingDepartment_UsesDefault() {
        Event loginEvent = createLoginEvent();
        setupUserModelMockWithoutDepartment();
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("Should use default department",
                Sw360KeycloakUserEventService.DEFAULT_DEPARTMENT, capturedUser.getDepartment());
    }

    /**
     * Tests user login event with username lookup by user ID.
     */
    @Test
    public void testUserLoginEvent_WithNonEmailUsername_LooksUpById() {
        Event loginEvent = createLoginEventWithUsername(TEST_USERNAME);
        setupUserModelMock(TEST_DEPARTMENT);
        when(userProvider.getUserById(realmModel, TEST_USERNAME)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        verify(userProvider).getUserById(realmModel, TEST_USERNAME);
        verify(userProvider, never()).getUserByEmail(any(), any());
        verify(sw360UserService).createOrUpdateUser(any(User.class));
    }

    // ==================== Email Validation Tests ====================

    /**
     * Tests valid email address recognition.
     */
    @Test
    public void testIsValidEmail_WithValidEmail_ReturnsTrue() {
        assertTrue("Standard email should be valid",
                userEventService.isValidEmail("user@example.com"));
        assertTrue("Email with subdomain should be valid",
                userEventService.isValidEmail("user@mail.example.com"));
        assertTrue("Email with plus sign should be valid",
                userEventService.isValidEmail("user+tag@example.com"));
    }

    /**
     * Tests invalid email address recognition.
     */
    @Test
    public void testIsValidEmail_WithInvalidEmail_ReturnsFalse() {
        assertFalse("Missing @ should be invalid",
                userEventService.isValidEmail("userexample.com"));
        assertFalse("Missing domain should be invalid",
                userEventService.isValidEmail("user@"));
        assertFalse("Plain username should be invalid",
                userEventService.isValidEmail("username"));
    }

    // ==================== Organization Mapping Integration Tests ====================

    /**
     * Tests that OrganizationMapper configuration is accessible during user event processing.
     */
    @Test
    public void testOrgMappingConfiguration_IsAccessibleDuringProcessing() {
        // Verify OrganizationMapper is properly initialized and accessible
        // This tests the lazy loading mechanism
        assertFalse("Custom mapping should be disabled by default",
                OrganizationMapper.isCustomMappingEnabled());
        assertTrue("Mapping count should be non-negative",
                OrganizationMapper.getMappingCount() >= 0);
    }

    /**
     * Tests that department sanitization and mapping work together correctly.
     */
    @Test
    public void testDepartmentSanitizationAndMapping_Integration() {
        // Test various department formats
        String[] testDepartments = {
            "SingleWord",
            "Multiple Words Here",
            "  SpacePadded  ",
            "UPPERCASE",
            "lowercase",
            "Mixed_Special-Chars"
        };

        for (String dept : testDepartments) {
            Event loginEvent = createLoginEvent();
            setupUserModelMock(dept);
            when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

            userEventService.userLoginEvent(loginEvent);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(sw360UserService, atLeastOnce()).createOrUpdateUser(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertNotNull("Department should not be null for input: " + dept,
                    capturedUser.getDepartment());

            // Reset mock for next iteration
            reset(sw360UserService);
        }
    }

    /**
     * Tests that null department from UserModel is handled gracefully.
     */
    @Test
    public void testUserLoginEvent_WithNullDepartmentAttribute_HandlesGracefully() {
        Event loginEvent = createLoginEvent();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(Sw360KeycloakUserEventService.DEPARTMENT, Collections.singletonList(null));
        attributes.put(Sw360KeycloakUserEventService.EXTERNAL_ID, Collections.singletonList("ext123"));

        when(userModel.getEmail()).thenReturn(TEST_EMAIL);
        when(userModel.getFirstName()).thenReturn(TEST_FIRST_NAME);
        when(userModel.getLastName()).thenReturn(TEST_LAST_NAME);
        when(userModel.getUsername()).thenReturn(TEST_USERNAME);
        when(userModel.getAttributes()).thenReturn(attributes);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        verify(sw360UserService).createOrUpdateUser(any(User.class));
    }

    // ==================== Helper Methods ====================

    private Event createLoginEvent() {
        return createLoginEventWithUsername(TEST_EMAIL);
    }

    private Event createLoginEventWithUsername(String username) {
        Event event = mock(Event.class);
        Map<String, String> details = new HashMap<>();
        details.put(Sw360KeycloakUserEventService.USERNAME, username);
        when(event.getDetails()).thenReturn(details);
        return event;
    }

    private void setupUserModelMock(String department) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(Sw360KeycloakUserEventService.DEPARTMENT, Collections.singletonList(department));
        attributes.put(Sw360KeycloakUserEventService.EXTERNAL_ID, Collections.singletonList("ext123"));

        when(userModel.getEmail()).thenReturn(TEST_EMAIL);
        when(userModel.getFirstName()).thenReturn(TEST_FIRST_NAME);
        when(userModel.getLastName()).thenReturn(TEST_LAST_NAME);
        when(userModel.getUsername()).thenReturn(TEST_USERNAME);
        when(userModel.getAttributes()).thenReturn(attributes);
    }

    private void setupUserModelMockWithoutDepartment() {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(Sw360KeycloakUserEventService.EXTERNAL_ID, Collections.singletonList("ext123"));

        when(userModel.getEmail()).thenReturn(TEST_EMAIL);
        when(userModel.getFirstName()).thenReturn(TEST_FIRST_NAME);
        when(userModel.getLastName()).thenReturn(TEST_LAST_NAME);
        when(userModel.getUsername()).thenReturn(TEST_USERNAME);
        when(userModel.getAttributes()).thenReturn(attributes);
    }
}
