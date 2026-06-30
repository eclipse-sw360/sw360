/*
 * Copyright Siemens AG, 2023-2024,2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FossologySpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @BeforeEach
    public void before() throws TException, IOException,TTransportException {
        fossologyClient = mock(FossologyService.Iface.class);
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(
                new User("admin@sw360.org", "sw360").setId("123456789"));
        when(fossologyAdminServices.getThriftFossologyClient()).thenReturn(fossologyClient);
        when(fossologyClient.getFossologyConfig()).thenReturn(fossologyConfig);
        Mockito.doNothing().when(fossologyAdminServices).saveConfig(any(), any(), any(), any(), any(), any());
        Mockito.doNothing().when(fossologyAdminServices).serverConnection(any());
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
