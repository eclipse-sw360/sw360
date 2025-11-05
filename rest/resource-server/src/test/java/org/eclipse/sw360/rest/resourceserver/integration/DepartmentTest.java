/*
 * Copyright Rohit Borra, 2025. Part of the SW360 GSOC Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.department.Sw360DepartmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class DepartmentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360DepartmentService departmentServiceMock;

    private User adminUser;

    @Before
    public void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setUserGroup(UserGroup.ADMIN);
        given(userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(adminUser);
        given(userServiceMock.getUserByEmail(anyString())).willReturn(adminUser);
    }

    @Test
    public void should_import_department_manually() throws IOException, TException {
        RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(RequestStatus.SUCCESS);
        given(departmentServiceMock.importDepartmentManually(any())).willReturn(summary);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/manuallyactive",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_schedule_import_department_success() throws IOException, TException {
        RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(RequestStatus.SUCCESS);
        given(departmentServiceMock.isDepartmentScheduled(any())).willReturn(false);
        given(departmentServiceMock.scheduleImportDepartment(any())).willReturn(summary);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/scheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("scheduled successfully"));
        assertTrue(response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_return_conflict_if_already_scheduled() throws IOException, TException {
        given(departmentServiceMock.isDepartmentScheduled(any())).willReturn(true);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/scheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("already scheduled"));
    }

    @Test
    public void should_handle_schedule_import_texception() throws IOException, TException {
        given(departmentServiceMock.isDepartmentScheduled(any())).willReturn(false);
        given(departmentServiceMock.scheduleImportDepartment(any())).willThrow(new TException("thrift issue"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/scheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertTrue(json.has("status"));
        assertTrue(json.has("error"));
    }

    @Test
    public void should_unschedule_import_department_success() throws IOException, TException {
        given(departmentServiceMock.unScheduleImportDepartment(any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/unscheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("unscheduled successfully"));
        assertTrue(response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_unschedule_import_department_texception() throws IOException, TException {
        given(departmentServiceMock.unScheduleImportDepartment(any())).willThrow(new TException("thrift issue"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/unscheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_update_folder_path_success() throws IOException, TException {
        doNothing().when(departmentServiceMock).writePathFolderConfig(anyString(), any());

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/writePathFolder?pathFolder=/tmp/dept",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Path updated successfully"));
    }

    @Test
    public void should_update_folder_path_failure() throws IOException, TException {
        doThrow(new RuntimeException("boom")).when(departmentServiceMock).writePathFolderConfig(anyString(), any());

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/writePathFolder?pathFolder=/tmp/dept",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_import_information() throws IOException, TException {
        given(departmentServiceMock.getImportInformation(any())).willReturn(
                Map.of("isScheduled", true, "interval", "1h"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/importInformation",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("isScheduled"));
        assertTrue(response.getBody().contains("interval"));
    }

    @Test
    public void should_get_log_file_list() throws IOException, TException {
        given(departmentServiceMock.getLogFileList()).willReturn(Set.of("2025-01-01", "2025-01-02"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/logFiles",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("2025-01-01"));
    }

    @Test
    public void should_get_log_file_content_by_date_success() throws IOException, TException {
        given(departmentServiceMock.getLogFileContentByDate(anyString())).willReturn(List.of("line1", "line2"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/logFileContent?date=2025-01-01",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("line1"));
    }

    @Test
    public void should_fail_log_file_content_invalid_date() throws IOException, TException {
        given(departmentServiceMock.getLogFileContentByDate(anyString())).willThrow(new org.eclipse.sw360.datahandler.thrift.SW360Exception("Invalid date time format, must be: yyyy-MM-dd"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/logFileContent?date=bad-date",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_members_emails_without_department_param() throws IOException {
        given(departmentServiceMock.getSecondaryDepartmentMembers()).willReturn(Map.of("DEP1", List.of("a@b.com")));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/members",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("DEP1"));
    }

    @Test
    public void should_get_members_emails_with_department_param() throws IOException {
        given(departmentServiceMock.getMemberEmailsBySecondaryDepartmentName(anyString()))
                .willReturn(Map.of("DEV", List.of("x@y.com", "z@y.com")));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/members?departmentName=DEV",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("DEV"));
        assertTrue(response.getBody().contains("x@y.com"));
    }

    @Test
    public void should_update_members_of_secondary_department() throws IOException, TException {
        doNothing().when(departmentServiceMock).updateMembersInDepartment(anyString(), any());
        given(departmentServiceMock.getMemberEmailsBySecondaryDepartmentName(anyString()))
                .willReturn(Map.of("QA", List.of("qa1@y.com", "qa2@y.com")));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> emails = new ArrayList<>();
        emails.add("qa1@y.com");
        emails.add("qa2@y.com");
        HttpEntity<List<String>> request = new HttpEntity<>(emails, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/members?departmentName=QA",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("QA"));
        assertTrue(response.getBody().contains("qa1@y.com"));
    }

    @Test
    public void should_expose_department_link_in_root() throws IOException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("department"));
    }

    @Test
    public void should_handle_import_department_texception() throws IOException, TException {
        given(departmentServiceMock.importDepartmentManually(any())).willThrow(new TException("thrift failure"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/manuallyactive",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertTrue(json.has("status"));
        assertTrue(json.has("error"));
    }

    @Test
    public void should_handle_schedule_import_sw360exception_on_check() throws IOException, TException {
        given(departmentServiceMock.isDepartmentScheduled(any()))
                .willThrow(new org.eclipse.sw360.datahandler.thrift.SW360Exception("schedule check failed"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/departments/scheduleImport",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
