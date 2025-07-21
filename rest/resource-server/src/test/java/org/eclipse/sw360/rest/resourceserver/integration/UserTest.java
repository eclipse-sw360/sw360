/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    private List<User> userList;
    private User user;
    private List<RestApiToken> restApiTokens;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() throws TException {
        userList = new ArrayList<>();
        restApiTokens = new ArrayList<>();

        // Setup User 1
        user = new User();
        user.setId("4784587578e87989");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        user.setGivenname("John");
        user.setLastname("Doe");
        user.setUserGroup(UserGroup.ADMIN);
        user.setDepartment("SW360 Administration");
        user.setWantsMailNotification(true);
        user.setFormerEmailAddresses(Set.of("admin_bachelor@sw360.org"));
        user.setSecondaryDepartmentsAndRoles(getSecondaryDepartmentsAndRoles());
        user.setNotificationPreferences(getNotificationPreferences());
        user.setRestApiTokens(getRestApiTokens());
        userList.add(user);

        // Setup User 2
        User user2 = new User();
        user2.setId("frwey45786rwe");
        user2.setEmail("jane@sw360.org");
        user2.setFullname("Jane Doe");
        user2.setGivenname("Jane");
        user2.setLastname("Doe");
        user2.setUserGroup(UserGroup.USER);
        user2.setDepartment("SW360 BA");
        user2.setWantsMailNotification(false);
        user2.setSecondaryDepartmentsAndRoles(getSecondaryDepartmentsAndRoles());
        userList.add(user2);

        given(this.userServiceMock.getUsersWithPagination(any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(userList.size()).setDisplayStart(0).setTotalRowCount(userList.size()),
                        userList.stream().toList()
                )
        );

        given(this.userServiceMock.getUser(user.getId())).willReturn(user);
        given(this.userServiceMock.getUser("frwey45786rwe")).willReturn(user2);
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("jane@sw360.org")).willReturn(user2);
        given(this.userServiceMock.getAllUsers()).willReturn(userList);

        // For user creation
        given(this.userServiceMock.addUser(any())).willReturn(
                new User("test@sw360.org", "DEPARTMENT").setId("1234567890").setFullname("FTest lTest")
                        .setGivenname("FTest").setLastname("lTest").setUserGroup(UserGroup.USER)
        );

        // For update
        doNothing().when(this.userServiceMock).updateUser(any());


        // For tokens
        given(this.userServiceMock.convertToRestApiToken(any(), any())).willReturn(getRestApiTokens().getFirst());
        given(this.userServiceMock.isTokenNameExisted(any(), any())).willReturn(true);

        // For departments - use the correct method names
        given(this.userServiceMock.getAvailableDepartments()).willReturn(Set.of("SW360 Administration", "SW360 BA"));
        given(this.userServiceMock.getExistingPrimaryDepartments()).willReturn(Set.of("SW360 Administration"));
        given(this.userServiceMock.getExistingSecondaryDepartments()).willReturn(Set.of("SW360 BA"));

    }

    @Test
    public void should_get_all_users() throws IOException {
        ResponseEntity<String> response = sendGet("/api/users");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "users", 2);
    }

    @Test
    public void should_get_single_user_by_id() throws IOException {
        String url = "/api/users/byid/" + user.getId();
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());

        // Verify all user data fields are returned correctly
        assertEquals(user.getEmail(), body.get("email").textValue());
        assertEquals(user.getUserGroup().toString(), body.get("userGroup").textValue());
        assertEquals(user.getFullname(), body.get("fullName").textValue());
        assertEquals(user.getGivenname(), body.get("givenName").textValue());
        assertEquals(user.getLastname(), body.get("lastName").textValue());
        assertEquals(user.getDepartment(), body.get("department").textValue());
        assertEquals(user.isWantsMailNotification(), body.get("wantsMailNotification").booleanValue());
        assertFalse(body.get("deactivated").booleanValue());

        // Verify complex nested objects exist
        assertTrue("Should have secondaryDepartmentsAndRoles", body.has("secondaryDepartmentsAndRoles"));
        assertTrue("Should have formerEmailAddresses", body.has("formerEmailAddresses"));
        assertTrue("Should have notificationPreferences", body.has("notificationPreferences"));

        // Verify the self link contains the user ID
        JsonNode selfLink = body.get("_links").get("self").get("href");
        assertTrue("Self link should contain user ID", selfLink.textValue().contains(user.getId()));
    }


    @Test
    public void should_get_single_user_by_email() throws IOException {
        String url = "/api/users/" + user.getEmail();
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());

        // Verify all user data fields are returned correctly (same as by ID)
        assertEquals(user.getEmail(), body.get("email").textValue());
        assertEquals(user.getUserGroup().toString(), body.get("userGroup").textValue());
        assertEquals(user.getFullname(), body.get("fullName").textValue());
        assertEquals(user.getGivenname(), body.get("givenName").textValue());
        assertEquals(user.getLastname(), body.get("lastName").textValue());
        assertEquals(user.getDepartment(), body.get("department").textValue());
        assertEquals(user.isWantsMailNotification(), body.get("wantsMailNotification").booleanValue());
        assertFalse(body.get("deactivated").booleanValue());

        // Verify complex nested objects exist
        assertTrue("Should have secondaryDepartmentsAndRoles", body.has("secondaryDepartmentsAndRoles"));
        assertTrue("Should have formerEmailAddresses", body.has("formerEmailAddresses"));
        assertTrue("Should have notificationPreferences", body.has("notificationPreferences"));
    }

    @Test
    public void should_get_user_profile() throws IOException {
        String url = "/api/users/profile";
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());

        // Verify all user profile fields are returned correctly
        assertEquals(user.getEmail(), body.get("email").textValue());
        assertEquals(user.getUserGroup().toString(), body.get("userGroup").textValue());
        assertEquals(user.getFullname(), body.get("fullName").textValue());
        assertEquals(user.getGivenname(), body.get("givenName").textValue());
        assertEquals(user.getLastname(), body.get("lastName").textValue());
        assertEquals(user.getDepartment(), body.get("department").textValue());
        assertEquals(user.isWantsMailNotification(), body.get("wantsMailNotification").booleanValue());
        assertFalse(body.get("deactivated").booleanValue());

        // Verify complex nested objects exist
        assertTrue("Should have secondaryDepartmentsAndRoles", body.has("secondaryDepartmentsAndRoles"));
        assertTrue("Should have formerEmailAddresses", body.has("formerEmailAddresses"));
        assertTrue("Should have notificationPreferences", body.has("notificationPreferences"));
    }

    @Test
    public void should_update_user_profile() throws Exception {
        String url = "/api/users/profile";
        Map<String, Object> updatedProfile = new HashMap<>();
        updatedProfile.put("wantsMailNotification", false);
        updatedProfile.put("notificationPreferences", getNotificationPreferences());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + url,
                HttpMethod.PATCH,
                new HttpEntity<>(objectMapper.writeValueAsString(updatedProfile), headers),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(user.getEmail(), body.get("email").textValue());
    }

    @Test
    public void should_create_user() throws Exception {
        String url = "/api/users";
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("fullName", "FTest lTest");
        newUser.put("givenName", "FTest");
        newUser.put("lastName", "lTest");
        newUser.put("email", "test@sw360.org");
        newUser.put("department", "DEPARTMENT");
        newUser.put("password", "12345");

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + url,
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(newUser), headers),
                String.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());

        // Verify all created user fields are returned correctly
        assertEquals("test@sw360.org", body.get("email").textValue());
        assertEquals("USER", body.get("userGroup").textValue());
        assertEquals("DEPARTMENT", body.get("department").textValue());
        assertEquals("FTest lTest", body.get("fullName").textValue());
        assertEquals("FTest", body.get("givenName").textValue());
        assertEquals("lTest", body.get("lastName").textValue());
        assertFalse(body.get("deactivated").booleanValue());
        assertTrue("Should have wantsMailNotification field", body.has("wantsMailNotification"));
        assertTrue("Should have _links section", body.has("_links"));
    }

    @Test
    public void should_update_existing_user() throws Exception {
        String url = "/api/users/" + user.getId();
        Map<String, Object> updateInfo = new HashMap<>();
        updateInfo.put("department", "DEPARTMENT");

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + url,
                HttpMethod.PATCH,
                new HttpEntity<>(objectMapper.writeValueAsString(updateInfo), headers),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("DEPARTMENT", body.get("department").textValue());
    }

    @Test
    public void should_list_all_user_tokens() throws IOException {
        String url = "/api/users/tokens";
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertTrue(body.get("_embedded").has("sw360:restApiTokens"));
    }

    @Test
    public void should_create_user_token() throws Exception {
        String url = "/api/users/tokens";
        Map<String, Object> tokenRequest = new HashMap<>();
        tokenRequest.put("name", "Token3");
        tokenRequest.put("expirationDate", "2023-12-29");
        tokenRequest.put("authorities", List.of("READ", "WRITE"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + url,
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(tokenRequest), headers),
                String.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_revoke_user_token() throws Exception {
        String url = "/api/users/tokens?name=Token1";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + url,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                String.class
        );
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_get_grouplist() throws Exception {
        String url = "/api/users/groupList";
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_all_departments() throws Exception {
        String url = "/api/users/departments";
        ResponseEntity<String> response = sendGet(url);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Helper methods ---

    private ResponseEntity<String> sendGet(String url) throws IOException {
        HttpHeaders headers = getHeaders(port);
        return new TestRestTemplate().exchange("http://localhost:" + port + url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
    }

    private Map<String, Set<UserGroup>> getSecondaryDepartmentsAndRoles() {
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = new HashMap<>();
        secondaryDepartmentsAndRoles.put("DEPARTMENT1", Set.of(UserGroup.CLEARING_EXPERT, UserGroup.ECC_ADMIN));
        secondaryDepartmentsAndRoles.put("DEPARTMENT2", Set.of(UserGroup.SW360_ADMIN, UserGroup.SECURITY_ADMIN));
        return secondaryDepartmentsAndRoles;
    }

    private Map<String, Boolean> getNotificationPreferences() {
        Map<String, Boolean> notificationPreferences = new HashMap<>();
        notificationPreferences.put("releaseCONTRIBUTORS", true);
        notificationPreferences.put("componentCREATED_BY", true);
        notificationPreferences.put("releaseCREATED_BY", true);
        notificationPreferences.put("moderationREQUESTING_USER", true);
        notificationPreferences.put("projectPROJECT_OWNER", true);
        notificationPreferences.put("moderationMODERATORS", true);
        notificationPreferences.put("releaseSUBSCRIBERS", true);
        notificationPreferences.put("componentMODERATORS", true);
        notificationPreferences.put("projectMODERATORS", true);
        notificationPreferences.put("projectROLES", true);
        notificationPreferences.put("releaseROLES", true);
        notificationPreferences.put("componentROLES", true);
        notificationPreferences.put("projectLEAD_ARCHITECT", true);
        notificationPreferences.put("componentCOMPONENT_OWNER", true);
        notificationPreferences.put("projectSECURITY_RESPONSIBLES", true);
        notificationPreferences.put("clearingREQUESTING_USER", true);
        notificationPreferences.put("projectCONTRIBUTORS", true);
        notificationPreferences.put("componentSUBSCRIBERS", true);
        notificationPreferences.put("projectPROJECT_RESPONSIBLE", true);
        notificationPreferences.put("releaseMODERATORS", true);
        return notificationPreferences;
    }

    private List<RestApiToken> getRestApiTokens() {
        if (!restApiTokens.isEmpty()) return restApiTokens;
        RestApiToken token1 = new RestApiToken();
        token1.setName("Token1");
        token1.setNumberOfDaysValid(10);
        token1.setCreatedOn("2023-12-19 02:31:52");
        token1.setAuthorities(Set.of("READ", "WRITE"));

        RestApiToken token2 = new RestApiToken();
        token2.setName("Token2");
        token2.setNumberOfDaysValid(11);
        token2.setCreatedOn("2023-12-19 02:31:52");
        token2.setAuthorities(Set.of("READ"));

        RestApiToken token3 = new RestApiToken();
        token3.setName("Token3");
        token3.setNumberOfDaysValid(10);
        token3.setCreatedOn("2023-12-19 02:31:52");
        token3.setAuthorities(Set.of("READ", "WRITE"));
        token3.setToken("MockedToken");

        restApiTokens = new ArrayList<>(List.of(token1, token2, token3));
        return restApiTokens;
    }
}