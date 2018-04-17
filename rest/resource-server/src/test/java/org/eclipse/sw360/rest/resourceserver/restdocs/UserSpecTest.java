/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void before() throws UnsupportedEncodingException {
        List<User> userList = new ArrayList<>();

        user = new User();
        user.setEmail("admin@sw360.org");
        user.setId(Base64.getEncoder().encodeToString(user.getEmail().getBytes("utf-8")));
        user.setUserGroup(UserGroup.ADMIN);
        user.setFullname("John Doe");
        user.setGivenname("John");
        user.setLastname("Doe");
        user.setDepartment("SW360 Administration");
        user.setWantsMailNotification(true);
        userList.add(user);

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        User user2 = new User();
        user2.setEmail("jane@sw360.org");
        user2.setId(Base64.getEncoder().encodeToString(user.getEmail().getBytes("utf-8")));
        user2.setUserGroup(UserGroup.USER);
        user2.setFullname("Jane Doe");
        user2.setGivenname("Jane");
        user2.setLastname("Doe");
        user2.setDepartment("SW360 BA");
        user2.setWantsMailNotification(false);
        userList.add(user2);

        given(this.userServiceMock.getAllUsers()).willReturn(userList);
    }

    @Test
    public void should_document_get_users() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:users[]email").description("The user's email"),
                                fieldWithPath("_embedded.sw360:users").description("An array of <<resources-users, User resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_user() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/users/" + user.getId())
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
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
