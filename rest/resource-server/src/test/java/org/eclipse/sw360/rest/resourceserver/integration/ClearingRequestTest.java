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
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.clearingrequest.Sw360ClearingRequestService;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class ClearingRequestTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ClearingRequestService clearingServiceMock;

    @MockitoBean
    private Sw360ProjectService projectServiceMock;

    @MockitoBean
    private Sw360ModerationRequestService moderationServiceMock;

    private User adminUser;
    private ClearingRequest cr;

    @Before
    public void setUp() throws TException {
        adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setId("123456789");
        adminUser.setUserGroup(UserGroup.ADMIN);
        given(userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(adminUser);
        given(userServiceMock.getUserByEmail(anyString())).willReturn(adminUser);

        cr = new ClearingRequest();
        cr.setId("CR-1");
        cr.setProjectId("P-1");
        cr.setClearingState(ClearingRequestState.NEW);
        cr.setRequestingUser("admin@sw360.org");
        cr.setClearingTeam("team@sw360.org");
    }

    @Test
    public void should_get_clearing_request_by_id() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);
        // Controller embeds project and may return null Hal when project is restricted; provide project with clearing summary
        org.eclipse.sw360.datahandler.thrift.projects.Project proj = new org.eclipse.sw360.datahandler.thrift.projects.Project();
        org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary summary = new org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary();
        summary.setNewRelease(1);
        summary.setUnderClearing(1);
        summary.setScanAvailable(0);
        summary.setReportAvailable(0);
        summary.setApproved(0);
        proj.setReleaseClearingStateSummary(summary);
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(proj);
        given(projectServiceMock.getClearingInfo(any(org.eclipse.sw360.datahandler.thrift.projects.Project.class), any())).willReturn(proj);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("CR-1"));
        assertTrue(body.contains("clearingTeam"));
    }

    @Test
    public void should_get_my_clearing_requests() throws IOException, TException {
        // Mock pagination methods
        org.eclipse.sw360.datahandler.thrift.PaginationData paginationData = new org.eclipse.sw360.datahandler.thrift.PaginationData();
        paginationData.setTotalRowCount(1);
        paginationData.setRowsPerPage(20);
        paginationData.setDisplayStart(0);
        given(clearingServiceMock.getRecentClearingRequestsWithPagination(any(), any())).willReturn(
            java.util.Collections.singletonMap(paginationData, List.of(cr))
        );
        // Ensure embedded details path executes without exception
        org.eclipse.sw360.datahandler.thrift.projects.Project proj = new org.eclipse.sw360.datahandler.thrift.projects.Project();
        org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary summary = new org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary();
        summary.setNewRelease(1);
        summary.setUnderClearing(1);
        summary.setReportAvailable(0);
        summary.setApproved(0);
        proj.setReleaseClearingStateSummary(summary);
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(proj);
        given(projectServiceMock.getClearingInfo(any(org.eclipse.sw360.datahandler.thrift.projects.Project.class), any())).willReturn(proj);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequests",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("sw360:clearingRequests"));
        // Embedded details
        assertTrue(body.contains("sw360:project"));
        assertTrue(body.contains("openRelease"));
    }

    @Test
    public void should_get_clearing_request_by_id_without_embedding() throws IOException, TException {
        // Skip embedding path to ensure 200 even if project embedding would fail in some environments
        ClearingRequest simpleCr = new ClearingRequest(cr);
        simpleCr.setProjectId("");
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(simpleCr);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("CR-1"));
    }

    @Test
    public void should_create_clearing_request_for_project() throws IOException, TException {
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(moderationServiceMock.getOpenCriticalCrCountByGroup(anyString())).willReturn(0);
        AddDocumentRequestSummary summary = new AddDocumentRequestSummary();
        summary.setRequestStatus(AddDocumentRequestStatus.SUCCESS);
        summary.setId("CR-1");
        given(projectServiceMock.createClearingRequest(any(), any(), anyString(), anyString())).willReturn(summary);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("projectId", "P-1");
        req.put("clearingTeam", "admin@sw360.org");
        req.put("clearingType", "DEEP");
        req.put("requestedClearingDate", "2030-01-01");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/P-1/clearingRequest",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("CR-1"));
    }

    @Test
    public void should_handle_texception_in_get_by_id() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willThrow(new TException("thrift"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertTrue(json.has("status"));
        assertTrue(json.has("error"));
        assertTrue(json.has("message"));
    }

    @Test
    public void should_expose_clearing_links_in_root() throws IOException {
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
        assertTrue(body != null && body.contains("clearingrequest"));
//        assertTrue(body.contains("clearingrequests"));
    }

    @Test
    public void should_get_comments_by_clearing_request_id_non_empty() throws IOException, TException {
        org.eclipse.sw360.datahandler.thrift.Comment c1 = new org.eclipse.sw360.datahandler.thrift.Comment();
        c1.setText("older");
        c1.setCommentedBy("admin@sw360.org");
        c1.setCommentedOn(1000L);
        org.eclipse.sw360.datahandler.thrift.Comment c2 = new org.eclipse.sw360.datahandler.thrift.Comment();
        c2.setText("newer");
        c2.setCommentedBy("admin@sw360.org");
        c2.setCommentedOn(2000L);

        ClearingRequest withComments = new ClearingRequest(cr);
        withComments.setComments(java.util.List.of(c1, c2));
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(withComments);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("commentingUser"));
        assertTrue(body.contains("older"));
        assertTrue(body.contains("newer"));
    }

    @Test
    public void should_get_comments_by_clearing_request_id_empty() throws IOException, TException {
        ClearingRequest withNoComments = new ClearingRequest(cr);
        withNoComments.setComments(new java.util.ArrayList<>());
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(withNoComments);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_get_comments() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willThrow(new RuntimeException("boom"));

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_reject_invalid_requested_date_by_requesting_user() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("requestedClearingDate", "not-a-date");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_reject_agreed_date_update_for_non_admin() throws IOException, TException {
        // Non-admin user
        User nonAdmin = new User();
        nonAdmin.setEmail("user@sw360.org");
        nonAdmin.setUserGroup(UserGroup.USER);
        given(userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(nonAdmin);
        given(userServiceMock.getUserByEmail(anyString())).willReturn(nonAdmin);
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("agreedClearingDate", "2031-01-01");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_reject_invalid_agreed_date_for_admin() throws IOException, TException {
        // Admin already set in setup; return CR
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("agreedClearingDate", "invalid-date");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_wrap_access_denied_from_service_as_bad_request() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(projectServiceMock.getClearingInfo(any(org.eclipse.sw360.datahandler.thrift.projects.Project.class), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(clearingServiceMock.updateClearingRequest(any(), any(), anyString(), anyString())).willReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.ACCESS_DENIED);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("clearingType", "DEEP");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_update_agreed_date_for_admin_with_valid_date() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(projectServiceMock.getClearingInfo(any(org.eclipse.sw360.datahandler.thrift.projects.Project.class), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(clearingServiceMock.updateClearingRequest(any(), any(), anyString(), anyString())).willReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("agreedClearingDate", "2031-01-01");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_reject_invalid_requesting_user_update() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);
        // Force helper.getUserByEmailOrNull to return null for this email only
        willThrow(new RuntimeException("not found"))
                .given(userServiceMock).getUserByEmail(org.mockito.ArgumentMatchers.eq("invalid@sw360.org"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("requestingUser", "invalid@sw360.org");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_allow_update_clearing_type_admin_success() throws IOException, TException {
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(cr);
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(projectServiceMock.getClearingInfo(any(org.eclipse.sw360.datahandler.thrift.projects.Project.class), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(clearingServiceMock.updateClearingRequest(any(), any(), anyString(), anyString())).willReturn(org.eclipse.sw360.datahandler.thrift.RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("clearingType", "DEEP");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_add_comment_to_clearing_request_success() throws IOException, TException {
        org.eclipse.sw360.datahandler.thrift.Comment c1 = new org.eclipse.sw360.datahandler.thrift.Comment();
        c1.setText("first");
        c1.setCommentedBy("admin@sw360.org");
        c1.setCommentedOn(1000L);
        org.eclipse.sw360.datahandler.thrift.Comment c2 = new org.eclipse.sw360.datahandler.thrift.Comment();
        c2.setText("second");
        c2.setCommentedBy("admin@sw360.org");
        c2.setCommentedOn(2000L);

        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest existing = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest(cr);
        existing.setComments(new java.util.ArrayList<>());
        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest updated = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest(cr);
        updated.setComments(java.util.List.of(c1, c2));

        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(existing);
        given(clearingServiceMock.addCommentToClearingRequest(anyString(), any(), any())).willReturn(updated);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("text", "hello");
        req.put("commentedBy", "admin@sw360.org");
        req.put("commentedOn", 3000L);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("commentingUser"));
        assertTrue(body.contains("first"));
        assertTrue(body.contains("second"));
    }

    @Test
    public void should_handle_illegal_argument_in_add_comment() throws IOException, TException {
        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest existing = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest(cr);
        existing.setComments(new java.util.ArrayList<>());
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(existing);
        given(clearingServiceMock.addCommentToClearingRequest(anyString(), any(), any())).willThrow(new IllegalArgumentException("invalid"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("text", "oops");
        req.put("commentedBy", "admin@sw360.org");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_resource_not_found_in_add_comment() throws IOException, TException {
        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest existing = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest(cr);
        existing.setComments(new java.util.ArrayList<>());
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(existing);
        given(clearingServiceMock.addCommentToClearingRequest(anyString(), any(), any())).willThrow(new org.springframework.data.rest.webmvc.ResourceNotFoundException("not found"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("text", "oops");
        req.put("commentedBy", "admin@sw360.org");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_handle_texception_in_add_comment() throws IOException, TException {
        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest existing = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest(cr);
        existing.setComments(new java.util.ArrayList<>());
        given(clearingServiceMock.getClearingRequestById(anyString(), any())).willReturn(existing);
        given(clearingServiceMock.addCommentToClearingRequest(anyString(), any(), any())).willThrow(new TException("t"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> req = new HashMap<>();
        req.put("text", "oops");
        req.put("commentedBy", "admin@sw360.org");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/clearingrequest/CR-1/comments",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
