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

    private Sw360KeycloakUserEventService userEventService;

    private static final String TEST_EMAIL = "test.user@example.com";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_DEPARTMENT = "Engineering";

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        userEventService = new Sw360KeycloakUserEventService(sw360UserService, objectMapper, keycloakSession);

        when(keycloakSession.realms()).thenReturn(realmProvider);
        when(keycloakSession.users()).thenReturn(userProvider);
        when(realmProvider.getRealmByName("sw360")).thenReturn(realmModel);
    }

    @Test
    public void testUserLoginEvent_WithDepartment() {
        Event loginEvent = createLoginEvent();
        setupUserModelMock(TEST_DEPARTMENT);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(TEST_EMAIL, capturedUser.getEmail());
        assertEquals(OrganizationMapper.mapOrganizationName(TEST_DEPARTMENT), capturedUser.getDepartment());
    }

    @Test
    public void testUserLoginEvent_WithSpaceSeparatedDepartment() {
        String departmentWithSpaces = "ORG UNIT DEPT TEAM SUBTEAM";
        Event loginEvent = createLoginEvent();
        setupUserModelMock(departmentWithSpaces);
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(OrganizationMapper.mapOrganizationName(departmentWithSpaces), capturedUser.getDepartment());
    }

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
        String expectedDepartment = OrganizationMapper.mapOrganizationName(originalDepartment);
        assertEquals(expectedDepartment, capturedUser.getDepartment());
    }

    @Test
    public void testUserLoginEvent_WithMissingDepartment_UsesDefault() {
        Event loginEvent = createLoginEvent();
        setupUserModelMockWithoutDepartment();
        when(userProvider.getUserByEmail(realmModel, TEST_EMAIL)).thenReturn(userModel);

        userEventService.userLoginEvent(loginEvent);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(sw360UserService).createOrUpdateUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(Sw360KeycloakUserEventService.DEFAULT_DEPARTMENT, capturedUser.getDepartment());
    }

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

    @Test
    public void testIsValidEmail() {
        assertTrue(userEventService.isValidEmail("user@example.com"));
        assertTrue(userEventService.isValidEmail("user@mail.example.com"));
        assertTrue(userEventService.isValidEmail("user+tag@example.com"));
        assertFalse(userEventService.isValidEmail("userexample.com"));
        assertFalse(userEventService.isValidEmail("user@"));
        assertFalse(userEventService.isValidEmail("username"));
    }

    @Test
    public void testUserLoginEvent_WithNullDepartmentAttribute() {
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
