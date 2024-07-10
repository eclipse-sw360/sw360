/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import com.google.common.collect.Sets;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.formParameters;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    private User user;

    @Before
    public void before() throws TException {
        List<User> userList = new ArrayList<>();

        Map<String,Set<UserGroup>> secondaryDepartmentsAndRoles = new HashMap<String, Set<UserGroup>>();
        Set<UserGroup> userGroups1 = new HashSet<>();
        userGroups1.add(UserGroup.CLEARING_EXPERT);
        userGroups1.add(UserGroup.ECC_ADMIN);
        Set<UserGroup> userGroups2 = new HashSet<>();
        userGroups2.add(UserGroup.SW360_ADMIN);
        userGroups2.add(UserGroup.SECURITY_ADMIN);
        secondaryDepartmentsAndRoles.put("DEPARTMENT1", userGroups1);
        secondaryDepartmentsAndRoles.put("DEPARTMENT2", userGroups2);

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

        List<RestApiToken> restApiTokens = new ArrayList<>();
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
        restApiTokens.add(token1);
        restApiTokens.add(token2);

        user = new User();
        user.setEmail("admin@sw360.org");
        user.setId("4784587578e87989");
        user.setUserGroup(UserGroup.ADMIN);
        user.setFullname("John Doe");
        user.setGivenname("John");
        user.setLastname("Doe");
        user.setDepartment("SW360 Administration");
        user.setWantsMailNotification(true);
        user.setFormerEmailAddresses(Sets.newHashSet("admin_bachelor@sw360.org"));
        user.setSecondaryDepartmentsAndRoles(secondaryDepartmentsAndRoles);
        user.setNotificationPreferences(notificationPreferences);
        user.setRestApiTokens(restApiTokens);
        userList.add(user);

        List<User> mockUserList = Collections.singletonList(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUser("4784587578e87989")).willReturn(user);
        given(this.userServiceMock.getUser("4784587578e87989")).willReturn(user);
        when(this.userServiceMock.addUser(any())).then(
                invocation -> new User("test@sw360.org", "DEPARTMENT").setId("1234567890").setFullname("FTest lTest")
                        .setGivenname("FTest").setLastname("lTest").setUserGroup(UserGroup.USER));
        given(this.userServiceMock.getUserByEmailOrExternalId(any())).willReturn(user);
        when(userServiceMock.refineSearch(any())).thenReturn(mockUserList);

        User user2 = new User();
        user2.setEmail("jane@sw360.org");
        user2.setId("frwey45786rwe");
        user2.setUserGroup(UserGroup.USER);
        user2.setFullname("Jane Doe");
        user2.setGivenname("Jane");
        user2.setLastname("Doe");
        user2.setDepartment("SW360 BA");
        user2.setWantsMailNotification(false);
        user.setSecondaryDepartmentsAndRoles(secondaryDepartmentsAndRoles);
        userList.add(user2);

        given(this.userServiceMock.getAllUsers()).willReturn(userList);

        RestApiToken token3 = new RestApiToken();
        token3.setName("Token3");
        token3.setNumberOfDaysValid(10);
        token3.setCreatedOn("2023-12-19 02:31:52");
        token3.setAuthorities(Set.of("READ", "WRITE"));
        token3.setToken("MockedToken");
        given(this.userServiceMock.convertToRestApiToken(any(), any())).willReturn(token3);
        given(this.userServiceMock.isTokenNameExisted(any(), any())).willReturn(true);
    }

    @Test
    public void should_document_get_users() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("page_entries", "5")
                .param("sort", "email,desc")
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        formParameters(
                                parameterWithName("page").description("Page of users"),
                                parameterWithName("page_entries").description("Amount of users per page"),
                                parameterWithName("sort").description("Defines order of the users")
                        ),
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation"),
                                linkWithRel("first").description("Link to first page"),
                                linkWithRel("last").description("Link to last page")
                        ),
                        responseFields(
                                subsectionWithPath("_embedded.sw360:users[]email").description("The user's email"),
                                subsectionWithPath("_embedded.sw360:users[]department").description("The user's department"),
                                subsectionWithPath("_embedded.sw360:users[]deactivated").description("The user's deactivated"),
                                subsectionWithPath("_embedded.sw360:users[]fullName").description("The user's full name"),
                                subsectionWithPath("_embedded.sw360:users[]givenName").description("The user's given name"),
                                subsectionWithPath("_embedded.sw360:users[]lastName").description("The user's last name"),
                                subsectionWithPath("_embedded.sw360:users[]userGroup").description("The user's user Group"),
                                subsectionWithPath("_embedded.sw360:users[]secondaryDepartmentsAndRoles").description("The user's secondaryDepartmentsAndRoles").optional(),
                                subsectionWithPath("_embedded.sw360:users").description("An array of <<resources-users, User resources>>"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("page").description("Additional paging information"),
                                fieldWithPath("page.size").description("Number of users per page"),
                                fieldWithPath("page.totalElements").description("Total number of all existing users"),
                                fieldWithPath("page.totalPages").description("Total number of pages"),
                                fieldWithPath("page.number").description("Number of the current page")
                        )));
    }

    @Test
    public void should_document_create_user() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        Map<String, String> user = new HashMap<>();
        user.put("fullName", "FTest lTest");
        user.put("givenName", "FTest");
        user.put("lastName", "lTest");
        user.put("email", "test@sw360.org");
        user.put("department", "DEPARTMENT");
        user.put("password", "12345");
        mockMvc.perform(post("/api/users")
                .contentType(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(user))
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("email").description("The email of the user"),
                                fieldWithPath("fullName").description("The full name of the user"),
                                fieldWithPath("givenName").description("The given name of the user"),
                                fieldWithPath("lastName").description("The last name of the user"),
                                fieldWithPath("department").description("The department of the user"),
                                fieldWithPath("password").description("The password of the user")
                                ),
                        responseFields(
                                subsectionWithPath("email").description("The email of the user"),
                                subsectionWithPath("userGroup").description("The user group of the user"),
                                subsectionWithPath("department").description("The department of the user"),
                                subsectionWithPath("fullName").description("The full name of the user"),
                                subsectionWithPath("givenName").description("The given name of the user"),
                                subsectionWithPath("lastName").description("The last name of the user"),
                                subsectionWithPath("deactivated").description("Is user deactivated"),
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("_links").description("<<resources-user-get,User>> to user resource")
                        )));
    }

    @Test
    public void should_document_get_user_by_id() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users/byid/" + user.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-users,User resource>>")
                        ),
                        responseFields(
                                fieldWithPath("email").description("The user's email"),
                                fieldWithPath("userGroup").description("The user group, possible values are: " + Arrays.asList(UserGroup.values())),
                                fieldWithPath("fullName").description("The users's full name"),
                                fieldWithPath("givenName").description("The user's given name"),
                                fieldWithPath("lastName").description("The user's last name"),
                                fieldWithPath("department").description("The user's company department"),
                                subsectionWithPath("secondaryDepartmentsAndRoles").description("The user's secondary departments and roles"),
                                fieldWithPath("formerEmailAddresses").description("The user's former email addresses"),
                                fieldWithPath("deactivated").description("Is user deactivated"),
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("notificationPreferences").description("User's notification preferences"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_user() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users/" + user.getEmail())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-users,User resource>>")
                        ),
                        responseFields(
                                fieldWithPath("email").description("The user's email"),
                                fieldWithPath("userGroup").description("The user group, possible values are: " + Arrays.asList(UserGroup.values())),
                                fieldWithPath("fullName").description("The users's full name"),
                                fieldWithPath("givenName").description("The user's given name"),
                                fieldWithPath("lastName").description("The user's last name"),
                                fieldWithPath("department").description("The user's company department"),
                                subsectionWithPath("secondaryDepartmentsAndRoles").description("The user's secondary departments and roles"),
                                fieldWithPath("formerEmailAddresses").description("The user's former email addresses"),
                                fieldWithPath("deactivated").description("Is user deactivated"),
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("notificationPreferences").description("User's notification preferences"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_user_profile() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("email").description("The user's email"),
                                fieldWithPath("userGroup").description("The user group, possible values are: " + Arrays.asList(UserGroup.values())),
                                fieldWithPath("fullName").description("The users's full name"),
                                fieldWithPath("givenName").description("The user's given name"),
                                fieldWithPath("lastName").description("The user's last name"),
                                fieldWithPath("department").description("The user's company department"),
                                subsectionWithPath("secondaryDepartmentsAndRoles").description("The user's secondary departments and roles"),
                                fieldWithPath("formerEmailAddresses").description("The user's former email addresses"),
                                fieldWithPath("deactivated").description("Is user deactivated"),
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("notificationPreferences").description("User's notification preferences"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_update_user_profile() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);

        Map<String, Boolean> notificationPreferences = new HashMap<>();
        notificationPreferences.put("releaseCONTRIBUTORS", true);
        notificationPreferences.put("componentCREATED_BY", false);
        notificationPreferences.put("releaseCREATED_BY", false);
        notificationPreferences.put("moderationREQUESTING_USER", false);
        notificationPreferences.put("projectPROJECT_OWNER", true);
        notificationPreferences.put("moderationMODERATORS", false);
        notificationPreferences.put("releaseSUBSCRIBERS", true);
        notificationPreferences.put("componentMODERATORS", true);
        notificationPreferences.put("projectMODERATORS", false);
        notificationPreferences.put("projectROLES", false);
        notificationPreferences.put("releaseROLES", true);
        notificationPreferences.put("componentROLES", true);
        notificationPreferences.put("projectLEAD_ARCHITECT", false);
        notificationPreferences.put("componentCOMPONENT_OWNER", true);
        notificationPreferences.put("projectSECURITY_RESPONSIBLES", true);
        notificationPreferences.put("clearingREQUESTING_USER", true);
        notificationPreferences.put("projectCONTRIBUTORS", true);
        notificationPreferences.put("componentSUBSCRIBERS", true);
        notificationPreferences.put("projectPROJECT_RESPONSIBLE", false);
        notificationPreferences.put("releaseMODERATORS", false);

        Map<String, Object> updatedProfile = new HashMap<>();
        updatedProfile.put("wantsMailNotification", true);
        updatedProfile.put("notificationPreferences", notificationPreferences);

        mockMvc.perform(patch("/api/users/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(updatedProfile))
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("notificationPreferences").description("User's notification preferences")
                        ),
                        responseFields(
                                fieldWithPath("email").description("The user's email"),
                                fieldWithPath("userGroup").description("The user group, possible values are: " + Arrays.asList(UserGroup.values())),
                                fieldWithPath("fullName").description("The users's full name"),
                                fieldWithPath("givenName").description("The user's given name"),
                                fieldWithPath("lastName").description("The user's last name"),
                                fieldWithPath("department").description("The user's company department"),
                                fieldWithPath("deactivated").description("Is user deactivated"),
                                subsectionWithPath("secondaryDepartmentsAndRoles").description("The user's secondary departments and roles"),
                                fieldWithPath("formerEmailAddresses").description("The user's former email addresses"),
                                fieldWithPath("wantsMailNotification").description("Does user want to be notified via mail?"),
                                subsectionWithPath("notificationPreferences").description("User's notification preferences"),
                                subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_list_all_user_tokens() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users/tokens")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:restApiTokens[]name").description("The token's name"),
                                fieldWithPath("_embedded.sw360:restApiTokens[]createdOn").description("The token's created date"),
                                fieldWithPath("_embedded.sw360:restApiTokens[]numberOfDaysValid").description("The token's number of valid day"),
                                fieldWithPath("_embedded.sw360:restApiTokens[]authorities").description("The token's authorities"),
                                subsectionWithPath("_links").description("<<resources-user get,User>> to user resource")
                        )));
    }

    @Test
    public void should_document_create_user_token() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        Map<String, Object> tokenRequest = new HashMap<>();
        tokenRequest.put("name", "Token3");
        tokenRequest.put("expirationDate", "2023-12-29");
        tokenRequest.put("authorities", List.of("READ", "WRITE"));
        mockMvc.perform(post("/api/users/tokens")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(tokenRequest))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());
    }

    @Test
    public void should_document_revoke_user_token() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(delete("/api/users/tokens")
                        .contentType(MediaTypes.HAL_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("name", "Token1")
                )
                .andExpect(status().isNoContent());
    }
}
