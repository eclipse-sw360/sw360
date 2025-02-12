/*
 * Copyright Siemens AG, 2024-2025.
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
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.department.Sw360DepartmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class DepartmentSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;
    
    @MockBean
    private Sw360DepartmentService departmentServiceMock;
    private RequestSummary requestSummary = new RequestSummary();
    RequestStatus mockRequestStatus = RequestStatus.SUCCESS;
    
    @Before
    public void before() throws TException {
        User sw360User = new User();
        sw360User.setId("123456789");
        sw360User.setEmail("admin@sw360.org");
        sw360User.setFullname("John Doe");
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(sw360User);
        given(this.departmentServiceMock.importDepartmentManually(any())).willReturn(requestSummary);
        given(this.departmentServiceMock.scheduleImportDepartment(any())).willReturn(requestSummary);
        when(departmentServiceMock.isDepartmentScheduled(sw360User)).thenReturn(false);
    }
    
    @Test
    public void should_document_import_department_manually() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("action", SW360Constants.IMPORT_DEPARTMENT_MANUALLY);

        mockMvc.perform(post("/api/department/manuallyactive")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(requestBody))
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_import_schedule_department() throws Exception {
        String userEmail = "test@example.com";
        RequestSummary mockRequestSummary = new RequestSummary();
        mockRequestSummary.setRequestStatus(RequestStatus.SUCCESS);
        when(departmentServiceMock.scheduleImportDepartment(any())).thenReturn(mockRequestSummary);
        mockMvc.perform(post("/api/department/scheduleImport")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .param("userEmail", userEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_cancel_import_schedule_department() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("test@example.com");

        when(departmentServiceMock.unScheduleImportDepartment(any())).thenReturn(mockRequestStatus);
        mockMvc.perform(post("/api/department/unscheduleImport")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
