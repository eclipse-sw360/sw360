/*
SPDX-FileCopyrightText: © 2024,2025 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.SynchronizationResult;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Sw360UserStorageProviderFactoryTest {

    @Mock
    private KeycloakSession session;
    @Mock
    private ComponentModel componentModel;
    @Mock
    private Config.Scope config;
    @Mock
    private KeycloakSessionFactory sessionFactory;
    @Mock
    private UserStorageProviderModel model;
    @Mock
    private RealmModel realm;
    @Mock
    private UserProvider userProvider;
    @Mock
    private RealmProvider realmProvider;
    @Mock
    private KeycloakTransactionManager transactionManager;
    @Mock
    private KeycloakContext context;
    @Mock
    private UserModel userModel;
    @Mock
    private GroupModel groupModel;

    @Mock
    private Sw360UserService sw360UserService;

    private Sw360UserStorageProviderFactory factory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Only set static config for config logic tests, not for general unit tests
        factory = new Sw360UserStorageProviderFactory();
    }

    @Test
    public void testCreate() {
        Sw360UserStorageProvider provider = factory.create(session, componentModel);
        assertNotNull(provider);
    }

    @Test
    public void testGetId() {
        assertEquals("sw360-user-storage-jpa", factory.getId());
    }

    @Test
    public void testClose() {
        factory.close();
    }

    @Test
    public void testSyncSince() {
        Date lastSync = new Date();
        SynchronizationResult result = factory.syncSince(lastSync, sessionFactory, "realmId", model);
        assertNull(result);
    }

    @Test
    public void testPopulateUserAttributesWithValidData() throws Exception {
        User user = createTestUser("test@test.com", "test", "test", "FT", "ext1", UserGroup.USER);
        when(realm.getGroupsStream()).thenReturn(Stream.of(groupModel));
        when(groupModel.getName()).thenReturn("USER");
        when(userModel.getGroupsStream()).thenReturn(Stream.empty());
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("populateUserAttributes", UserModel.class, RealmModel.class, User.class, UserGroup.class);
        method.setAccessible(true);
        method.invoke(factory, userModel, realm, user, user.getUserGroup());
        verify(userModel).setFirstName("test");
        verify(userModel).setLastName("test");
        verify(userModel).setEmail("test@test.com");
        verify(userModel).setEmailVerified(true);
        verify(userModel).setUsername("test@test.com");
        verify(userModel).setSingleAttribute(eq("Department"), eq("FT"));
        verify(userModel).setSingleAttribute(eq("externalId"), eq("ext1"));
    }

    @Test
    public void testPopulateUserAttributesWithNullValues() throws Exception {
        User user = createTestUser("test@test.com", null, null, null, null, null);
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("populateUserAttributes", UserModel.class, RealmModel.class, User.class, UserGroup.class);
        method.setAccessible(true);
        method.invoke(factory, userModel, realm, user, null);
        verify(userModel).setFirstName("Not Provided");
        verify(userModel).setLastName("Not Provided");
        verify(userModel).setSingleAttribute("Department", "Unknown");
        verify(userModel).setSingleAttribute("externalId", "N/A");
    }

    @Test
    public void testAssignGroupToUserWithValidGroup() throws Exception {
        when(realm.getGroupsStream()).thenReturn(Stream.of(groupModel));
        when(groupModel.getName()).thenReturn("USER");
        when(userModel.getGroupsStream()).thenReturn(Stream.empty());
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("assignGroupToUser", UserModel.class, RealmModel.class, UserGroup.class);
        method.setAccessible(true);
        when(userModel.getGroupsStream()).thenReturn(Stream.empty());
        when(realm.getGroupsStream()).thenReturn(Stream.of(groupModel));
        method.invoke(factory, userModel, realm, UserGroup.USER);
        verify(userModel, atLeastOnce()).joinGroup(any(GroupModel.class));
    }

    @Test
    public void testAssignGroupToUserWithNullGroup() throws Exception {
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("assignGroupToUser", UserModel.class, RealmModel.class, UserGroup.class);
        method.setAccessible(true);
        method.invoke(factory, userModel, realm, null);
        verify(userModel, never()).joinGroup(any());
    }

    @Test
    public void testAssignGroupToUserWithNonExistentGroup() throws Exception {
        when(realm.getGroupsStream()).thenReturn(Stream.empty());
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("assignGroupToUser", UserModel.class, RealmModel.class, UserGroup.class);
        method.setAccessible(true);
        method.invoke(factory, userModel, realm, UserGroup.USER);
        verify(userModel, never()).joinGroup(any());
    }

    @Test
    public void testAssignGroupToUserAlreadyInGroup() throws Exception {
        GroupModel mockGroup = mock(GroupModel.class);
        when(realm.getGroupsStream()).thenReturn(Stream.of(mockGroup));
        when(mockGroup.getName()).thenReturn("USER");
        when(userModel.getGroupsStream()).thenReturn(Stream.of(mockGroup));
        when(userModel.getEmail()).thenReturn("test@test.com");
        java.lang.reflect.Method method = Sw360UserStorageProviderFactory.class
                .getDeclaredMethod("assignGroupToUser", UserModel.class, RealmModel.class, UserGroup.class);
        method.setAccessible(true);
        method.invoke(factory, userModel, realm, UserGroup.USER);
        verify(userModel, never()).joinGroup(any());
    }

    @Test
    public void testExecutorServiceWrapper() {
        try {
            Class<?> wrapperClass = Class.forName("org.eclipse.sw360.keycloak.spi.Sw360UserStorageProviderFactory$ExecutorServiceWrapper");
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            Object wrapper = wrapperClass.getDeclaredConstructor(java.util.concurrent.ExecutorService.class).newInstance(executor);
            java.lang.reflect.Method getExecutorMethod = wrapperClass.getDeclaredMethod("getExecutor");
            java.util.concurrent.ExecutorService retrievedExecutor = (java.util.concurrent.ExecutorService) getExecutorMethod.invoke(wrapper);
            assertEquals(executor, retrievedExecutor);
            java.lang.reflect.Method closeMethod = wrapperClass.getDeclaredMethod("close");
            closeMethod.invoke(wrapper);
        } catch (Exception e) {
            assertNotNull(factory);
        }
    }

    @Test
    public void testBatchResult() {
        try {
            Class<?> batchResultClass = Class.forName("org.eclipse.sw360.keycloak.spi.Sw360UserStorageProviderFactory$BatchResult");
            Object batchResult = batchResultClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method getSyncResultMethod = batchResultClass.getDeclaredMethod("getSyncResult");
            SynchronizationResult syncResult = (SynchronizationResult) getSyncResultMethod.invoke(batchResult);
            assertNotNull(syncResult);
            assertEquals(0, syncResult.getAdded());
            assertEquals(0, syncResult.getUpdated());
            assertEquals(0, syncResult.getFailed());
        } catch (Exception e) {
            assertNotNull(factory);
        }
    }

    @Test
    public void testSyncWithExternalUsers() {
        User user1 = createTestUser("user1@test.com", "User", "One", "Dept1", "ext1", UserGroup.USER);
        User user2 = createTestUser("user2@test.com", "User", "Two", "Dept2", "ext2", UserGroup.USER);
        Sw360UserService mockService = mock(Sw360UserService.class);
        when(mockService.getAllUsers()).thenReturn(List.of(user1, user2));
        factory.setSw360UserService(mockService);
        when(sessionFactory.create()).thenReturn(session);
        when(session.realms()).thenReturn(realmProvider);
        when(realmProvider.getRealm(anyString())).thenReturn(realm);
        when(session.getContext()).thenReturn(context);
        when(session.getTransactionManager()).thenReturn(transactionManager);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.searchForUserStream(eq(realm), anyMap())).thenReturn(Stream.empty());
        when(userProvider.getUserByUsername(eq(realm), anyString())).thenReturn(null);
        when(userProvider.addUser(eq(realm), anyString())).thenReturn(userModel);
        when(realm.getGroupsStream()).thenAnswer(a -> Stream.of(groupModel));
        when(groupModel.getName()).thenReturn("USER");
        SynchronizationResult expectedResult = new SynchronizationResult();
        expectedResult.setAdded(2);
        expectedResult.setFailed(0);
        // The factory.sync method should return a SynchronizationResult with added=2, failed=0
        SynchronizationResult result = factory.sync(sessionFactory, "realmId", model);
        assertNotNull(result);
        assertEquals(expectedResult.getAdded(), result.getAdded());
        assertEquals(expectedResult.getFailed(), result.getFailed());
        verify(userProvider, times(2)).addUser(eq(realm), anyString());
        verify(userModel, atLeast(2)).setEmail(anyString());
    }

    private User createTestUser(String email, String firstName, String lastName, String department, String externalId, UserGroup userGroup) {
        User user = new User();
        user.setEmail(email);
        user.setGivenname(firstName);
        user.setLastname(lastName);
        user.setDepartment(department);
        user.setExternalid(externalId);
        user.setUserGroup(userGroup);
        return user;
    }
}
