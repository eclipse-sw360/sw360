/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.admin.fossology.Sw360FossologyAdminServices;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class FossologySpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
    Sw360FossologyAdminServices fossologyAdminServices;

    @Before
    public void before() throws TException, IOException {
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        doNothing().when(fossologyAdminServices).saveConfig(any(), any(), any(), any(), any(), any());
        doNothing().when(fossologyAdminServices).serverConnection(any());
    }

    @Test
    public void should_document_save_configuration() throws Exception {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("url", "http://localhost:8080");
        myMap.put("folderId", "1");
        myMap.put("token", "hdshj2341.@");
        mockMvc.perform(post("/api/fossology/saveConfig")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(myMap))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_check_server_configuration() throws Exception {
        mockMvc.perform(get("/api/fossology/reServerConnection")
                .contentType(MediaTypes.HAL_JSON)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword)))
                .andExpect(status().isOk());
    }
}
