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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationRequestTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ModerationRequestService moderationServiceMock;

    @MockitoBean
    private Sw360ProjectService projectServiceMock;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    @MockitoBean
    private Sw360ComponentService componentServiceMock;

    private User adminUser;
    private ModerationRequest openMr;

    @Before
    public void setUp() throws TException {
        adminUser = new User();
        adminUser.setEmail("admin@sw360.org");
        adminUser.setId("123456789");
        adminUser.setUserGroup(UserGroup.ADMIN);
        given(userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(adminUser);
        given(userServiceMock.getUserByEmail(anyString())).willReturn(adminUser);

        openMr = new ModerationRequest();
        openMr.setId("MR-1");
        openMr.setDocumentId("R-1");
        openMr.setDocumentType(DocumentType.RELEASE);
        openMr.setRequestingUser("admin@sw360.org");
        Set<String> moderators = new HashSet<>();
        moderators.add("admin@sw360.org");
        openMr.setModerators(moderators);
        openMr.setModerationState(ModerationState.INPROGRESS);

        // Stubs for embedding calls in controller when fetching by ID
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.projects.Project());
        given(releaseServiceMock.getReleaseForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.components.Release());
        given(componentServiceMock.getComponentForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.components.Component());
    }

    @Test
    public void should_get_moderation_request_by_id() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("MR-1"));
        assertTrue(response.getBody().contains("requestingUser"));
    }

    @Test
    public void should_patch_accept_moderation_request() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.acceptRequest(any(), anyString(), any())).willReturn(ModerationState.APPROVED);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        patch.put("action", "ACCEPT");
        patch.put("comment", "ok");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("APPROVED"));
    }

    @Test
    public void should_patch_reject_moderation_request() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.rejectRequest(any(), anyString(), any())).willReturn(ModerationState.REJECTED);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        patch.put("action", "REJECT");
        patch.put("comment", "nope");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("REJECTED"));
    }

    @Test
    public void should_patch_unassign_moderation_request() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.removeMeFromModerators(any(), any())).willReturn(ModerationState.PENDING);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        patch.put("action", "UNASSIGN");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("PENDING"));
    }

    @Test
    public void should_patch_assign_moderation_request() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.assignRequest(any(), any())).willReturn(ModerationState.INPROGRESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        patch.put("action", "ASSIGN");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("INPROGRESS"));
    }

    @Test
    public void should_patch_postpone_moderation_request() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.postponeRequest(any(), anyString())).willReturn(ModerationState.INPROGRESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        patch.put("action", "POSTPONE");
        patch.put("comment", "later");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().contains("INPROGRESS"));
    }

    @Test
    public void should_error_when_action_invalid() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> patch = new HashMap<>();
        // leave empty to trigger invalid action
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(patch, headers);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/MR-1",
                HttpMethod.PATCH,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_delete_moderation_request_info() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.deleteModerationRequestInfo(any(), anyString(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = java.util.List.of("MR-1");
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("deleted"));
    }

    @Test
    public void should_conflict_when_permission_denied_in_delete() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.deleteModerationRequestInfo(any(), anyString(), any())).willReturn(RequestStatus.FAILURE);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = new java.util.ArrayList<>(java.util.List.of("MR-1"));
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("permission"));
    }

    @Test
    public void should_conflict_when_some_deleted_and_some_open() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        // First id returns SUCCESS, second returns null (open state)
        given(moderationServiceMock.deleteModerationRequestInfo(any(), anyString(), any()))
                .willReturn(RequestStatus.SUCCESS)
                .willReturn(null);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = new java.util.ArrayList<>(java.util.List.of("MR-1", "MR-2"));
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("Some requests were deleted"));
    }

    @Test
    public void should_conflict_when_open_or_no_permission_generic() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString())).willReturn(openMr);
        given(moderationServiceMock.deleteModerationRequestInfo(any(), anyString(), any())).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = new java.util.ArrayList<>(java.util.List.of("MR-1"));
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void should_conflict_when_invalid_ids_in_delete() throws IOException, TException {
        given(moderationServiceMock.getModerationRequestById(anyString()))
                .willThrow(new org.springframework.data.rest.webmvc.ResourceNotFoundException("not found"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = new java.util.ArrayList<>(java.util.List.of("MR-unknown"));
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("invalid"));
    }

    @Test
    public void should_bad_request_when_delete_no_ids() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        java.util.List<String> ids = new java.util.ArrayList<>();
        HttpEntity<java.util.List<String>> request = new HttpEntity<>(ids, headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/delete",
                HttpMethod.DELETE,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_validate_ok_when_write_allowed() throws IOException, TException {
        // Component path
        given(componentServiceMock.getComponentForUserById(anyString(), any())).willReturn(new org.eclipse.sw360.datahandler.thrift.components.Component());

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/validate?entityType=COMPONENT&entityId=C-1",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_validate_not_found_when_entity_missing() throws IOException, TException {
        given(projectServiceMock.getProjectForUserById(anyString(), any())).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/validate?entityType=PROJECT&entityId=P-404",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_validate_bad_request_for_invalid_entity_type() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/validate?entityType=UNKNOWN&entityId=ID",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_validate_internal_error_on_thrift_exception() throws IOException, TException {
        org.apache.thrift.TException tex = new org.apache.thrift.TException("err");
        given(releaseServiceMock.getReleaseForUserById(anyString(), any())).willThrow(tex);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/validate?entityType=RELEASE&entityId=R-1",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_expose_moderation_link_in_root() throws IOException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("moderationrequest"));
    }

    @Test
    public void should_list_moderation_requests_empty_page() throws IOException, TException {
        given(moderationServiceMock.getRequestsByModerator(any(), any())).willReturn(java.util.List.of());
        given(moderationServiceMock.getTotalCountOfRequests(any())).willReturn(0L);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("_embedded"));
    }

    @Test
    public void should_error_on_invalid_state() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/byState?state=invalid",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_get_my_submissions() throws IOException, TException {
        java.util.Map<PaginationData, java.util.List<ModerationRequest>> map = new java.util.HashMap<>();
        PaginationData pd = new PaginationData();
        pd.setTotalRowCount(0);
        map.put(pd, java.util.List.of());
        given(moderationServiceMock.getRequestsByRequestingUser(any(), any())).willReturn(map);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/mySubmissions",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null);
    }

    @Test
    public void should_list_moderation_requests_by_state_open_with_details() throws IOException, TException {
        ModerationRequest mrS = new ModerationRequest(openMr);
        mrS.setId("MR-10");
        java.util.Map<PaginationData, java.util.List<ModerationRequest>> map = new java.util.HashMap<>();
        PaginationData pd = new PaginationData();
        pd.setTotalRowCount(1);
        map.put(pd, new java.util.ArrayList<>(java.util.List.of(mrS)));
        given(moderationServiceMock.getRequestsByState(any(), any(), anyBoolean(), anyBoolean())).willReturn(map);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/byState?state=open&allDetails=true&page=0&page_entries=20",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("MR-10"));
    }

    @Test
    public void should_list_moderation_requests_with_details_and_pagination() throws IOException, TException {
        // prepare two MRs so pagination resources non-empty
        ModerationRequest mr1 = new ModerationRequest(openMr);
        mr1.setId("MR-2");
        ModerationRequest mr2 = new ModerationRequest(openMr);
        mr2.setId("MR-3");
        java.util.List<ModerationRequest> list = new java.util.ArrayList<>(java.util.List.of(mr1, mr2));

        given(moderationServiceMock.getRequestsByModerator(any(), any())).willReturn(list);
        given(moderationServiceMock.getTotalCountOfRequests(any())).willReturn(2L);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest?allDetails=true&page=0&page_entries=20",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body != null && body.contains("MR-2"));
        assertTrue(body.contains("MR-3"));
        // when allDetails=true, embedded requestingUser is added
        assertTrue(body.contains("requestingUser"));
    }

    @Test
    public void should_list_with_all_details_and_trigger_duplicate_filters() throws IOException, TException {
        ModerationRequest dupMr = new ModerationRequest(openMr);
        dupMr.setId("MR-DUP");
        // prepare additions and deletions for all supported entities
        dupMr.setProjectAdditions(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("P1").setName("P"));
        dupMr.setProjectDeletions(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("P1").setName("P"));
        dupMr.setReleaseAdditions(new org.eclipse.sw360.datahandler.thrift.components.Release().setId("R1").setName("R"));
        dupMr.setReleaseDeletions(new org.eclipse.sw360.datahandler.thrift.components.Release().setId("R1").setName("R"));
        dupMr.setComponentAdditions(new org.eclipse.sw360.datahandler.thrift.components.Component().setId("C1").setName("C"));
        dupMr.setComponentDeletions(new org.eclipse.sw360.datahandler.thrift.components.Component().setId("C1").setName("C"));
        dupMr.setLicenseAdditions(new org.eclipse.sw360.datahandler.thrift.licenses.License().setId("L1").setFullname("Lf"));
        dupMr.setLicenseDeletions(new org.eclipse.sw360.datahandler.thrift.licenses.License().setId("L1").setFullname("Lf"));
        dupMr.setPackageInfoAdditions(new org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation().setId("PK1").setName("PK"));
        dupMr.setPackageInfoDeletions(new org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation().setId("PK1").setName("PK"));
        dupMr.setSPDXDocumentAdditions(new org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument().setId("SD1"));
        dupMr.setSPDXDocumentDeletions(new org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument().setId("SD1"));
        dupMr.setDocumentCreationInfoAdditions(new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation().setId("DC1"));
        dupMr.setDocumentCreationInfoDeletions(new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation().setId("DC1"));

        given(moderationServiceMock.getRequestsByModerator(any(), any())).willReturn(new java.util.ArrayList<>(java.util.List.of(dupMr)));
        given(moderationServiceMock.getTotalCountOfRequests(any())).willReturn(1L);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest?allDetails=true&page=0&page_entries=20",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("MR-DUP"));
    }

    @Test
    public void should_list_by_state_with_all_details_and_trigger_duplicate_filters() throws IOException, TException {
        ModerationRequest dupMr = new ModerationRequest(openMr);
        dupMr.setId("MR-DUP2");
        dupMr.setProjectAdditions(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("P2"));
        dupMr.setProjectDeletions(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("P2"));
        java.util.Map<PaginationData, java.util.List<ModerationRequest>> map = new java.util.HashMap<>();
        PaginationData pd = new PaginationData();
        pd.setTotalRowCount(1);
        map.put(pd, new java.util.ArrayList<>(java.util.List.of(dupMr)));
        given(moderationServiceMock.getRequestsByState(any(), any(), anyBoolean(), anyBoolean())).willReturn(map);

        HttpHeaders headers = getHeaders(port);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/moderationrequest/byState?state=open&allDetails=true&page=0&page_entries=20",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("MR-DUP2"));
    }
}
