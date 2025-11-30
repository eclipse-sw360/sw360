/*
 * Copyright Siemens AG, 2025.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.department.Sw360DepartmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class DepartmentSpecTest extends TestRestDocsSpecBase {
    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockitoBean
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
        mockMvc.perform(post("/api/departments/manuallyactive")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_import_schedule_department() throws Exception {
        RequestSummary mockRequestSummary = new RequestSummary();
        mockRequestSummary.setRequestStatus(RequestStatus.SUCCESS);
        when(departmentServiceMock.scheduleImportDepartment(any())).thenReturn(mockRequestSummary);
        mockMvc.perform(post("/api/departments/scheduleImport")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_cancel_import_schedule_department() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("test@example.com");

        when(departmentServiceMock.unScheduleImportDepartment(any())).thenReturn(mockRequestStatus);
        mockMvc.perform(post("/api/departments/unscheduleImport")
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_update_folder_path() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        String folderPath = "/new/folder/path";
        doNothing().when(departmentServiceMock).writePathFolderConfig(anyString(), any());

        mockMvc.perform(post("/api/departments/writePathFolder")
                .param("pathFolder", folderPath)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_import_information() throws Exception {
        Map<String, Object> importInformation = ImmutableMap.<String, Object>builder()
                .put("folderPath", "/home/user/import")
                .put("lastRunningTime", "Mon Mar 03 17:55:37 ICT 2025")
                .put("isSchedulerStarted", true)
                .put("interval", "01:00:00")
                .put("nextRunningTime", "Wed Mar 05 17:00:00 ICT 2025").build();
        given(this.departmentServiceMock.getImportInformation(any())).willReturn(importInformation);
        mockMvc.perform(get("/api/departments/importInformation")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_log_file_list() throws Exception {
        Set<String> files = Set.of("2025-03-03.log", "2025-03-04.log");
        given(this.departmentServiceMock.getLogFileList()).willReturn(files);
        mockMvc.perform(get("/api/departments/logFiles")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_log_file_content_by_date() throws Exception {
        String requestDate = "2025-03-03";
        List<String> logContent = List.of(
                "2025-03-03 17:55:37 IMPORT START IMPORT DEPARTMENT ",
                "2025-03-03 17:55:37 IMPORT DEPARTMENT [] - FILE NAME: [department-config.json] SUCCESS",
                "2025-03-03 17:55:37 IMPORT [ FILE SUCCESS: 1 - FILE FAIL: 0 - TOTAL FILE: 1 ] Complete The File Import",
                "2025-03-03 17:55:37 IMPORT END IMPORT DEPARTMENT "
        );
        given(this.departmentServiceMock.getLogFileContentByDate(requestDate)).willReturn(logContent);
        mockMvc.perform(get("/api/departments/logFileContent?date=" + requestDate)
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_all_secondary_departments_and_their_members() throws Exception {
        Map<String, List<String>> departmentMembers = ImmutableMap.<String, List<String>>builder()
                .put("DEPARTMENT", List.of("user1@sw360.org", "user2@sw360.org"))
                .put("DEPARTMENT1", List.of("user1@sw360.org", "user3@sw360.org")).build();
        given(this.departmentServiceMock.getSecondaryDepartmentMembers()).willReturn(departmentMembers);
        mockMvc.perform(get("/api/departments/members")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_get_secondary_department_members_by_name() throws Exception {
        String departmentName = "DEPARTMENT";
        Map<String, List<String>> departmentMembers = ImmutableMap.<String, List<String>>builder()
                .put("DEPARTMENT", List.of("user1@sw360.org", "user2@sw360.org")).build();
        given(this.departmentServiceMock.getMemberEmailsBySecondaryDepartmentName(departmentName)).willReturn(departmentMembers);
        mockMvc.perform(get("/api/departments/members?departmentName=" + departmentName)
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_document_update_secondary_department_members_by_name() throws Exception {
        String departmentName = "DEPARTMENT1";
        List<String> emailsList = List.of("user4@sw360.org");
        Map<String, List<String>> departmentMembers = ImmutableMap.<String, List<String>>builder()
                .put("DEPARTMENT", List.of("user1@sw360.org", "user2@sw360.org"))
                .put("DEPARTMENT1", List.of("user1@sw360.org", "user3@sw360.org", "user4@sw360.org")).build();
        given(this.departmentServiceMock.getMemberEmailsBySecondaryDepartmentName(departmentName)).willReturn(departmentMembers);
        mockMvc.perform(patch("/api/departments/members?departmentName=" + departmentName)
                        .content(this.objectMapper.writeValueAsString(emailsList))
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
