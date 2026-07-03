/*
 * Copyright Rohit Borra, 2025. Part of the SW360 GSOC Project.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;

public class ObligationTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    private Obligation obligation1, obligation2;
    private List<Obligation> obligationList;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() throws TException {
        // Setup test user
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(UserConverter.fromThrift(user));
        given(this.userServiceMock.getUserByEmailOrExternalId("user@sw360.org")).willReturn(UserConverter.fromThrift(user));
        given(this.userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(UserConverter.fromThrift(user));
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(UserConverter.fromThrift(user));
        given(this.userServiceMock.getUserByEmail("user@sw360.org")).willReturn(UserConverter.fromThrift(user));
        given(this.userServiceMock.getUserByEmail(anyString())).willReturn(UserConverter.fromThrift(user));

        // Setup test obligations
        obligation1 = new Obligation();
        obligation1.setId("888888888");
        obligation1.setText("License Obligation");
        obligation1.setTitle("Obligation 1");
        obligation1.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
        obligation1.setObligationType(ObligationType.PERMISSION);

        obligation2 = new Obligation();
        obligation2.setId("99999999");
        obligation2.setText("Organisation Obligation");
        obligation2.setTitle("Obligation 2");
        obligation2.setObligationLevel(ObligationLevel.ORGANISATION_OBLIGATION);
        obligation2.setObligationType(ObligationType.RISK);

        obligationList = Arrays.asList(obligation1, obligation2);

        PaginationData pageData = new PaginationData();
        pageData.setSortColumnNumber(0);
        pageData.setDisplayStart(0);
        pageData.setRowsPerPage(obligationList.size());
        pageData.setTotalRowCount(obligationList.size());
        pageData.setAscending(true);

        // Setup service mocks
        given(obligationServiceMock.getObligationsFiltered(any(), any(), any())).willReturn(Map.of(pageData, obligationList));
        given(obligationServiceMock.getObligationById(eq(obligation1.getId()), any())).willReturn(obligation1);
        given(obligationServiceMock.getObligationById(eq(obligation2.getId()), any())).willReturn(obligation2);
        given(obligationServiceMock.deleteObligation(eq(obligation1.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(obligationServiceMock.deleteObligation(eq(obligation2.getId()), any())).willReturn(RequestStatus.SUCCESS);

        // Setup create obligation mock
        Obligation createdObligation = new Obligation("This is the text of my Test Obligation");
        createdObligation.setId("1234567890");
        createdObligation.setTitle("Test Obligation");
        createdObligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
        createdObligation.setObligationType(ObligationType.PERMISSION);
        given(obligationServiceMock.createObligation(any(), any())).willReturn(createdObligation);

        // Setup update obligation mock
        given(obligationServiceMock.updateObligation(any(), any())).willReturn(obligation1);

        objectMapper = new ObjectMapper();
    }

    // ========== OBLIGATION CRUD TESTS ==========

    @Test
    public void should_get_all_obligations() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations?page=0&page_entries=5&sort=title,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded obligations");
        assertTrue(responseBody.contains("sw360:obligations"), "Response should contain sw360:obligations");
        assertTrue(responseBody.contains("page"), "Response should contain pagination info");
        assertTrue(responseBody.contains("totalElements"), "Response should contain totalElements");
        assertTrue(responseBody.contains("Obligation 1"), "Response should contain Obligation 1");
        assertTrue(responseBody.contains("Obligation 2"), "Response should contain Obligation 2");
    }

    @Test
    public void should_get_obligations_with_filter() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations?obligationLevel=LICENSE_OBLIGATION&page=0&page_entries=5",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded obligations");
        assertTrue(responseBody.contains("sw360:obligations"), "Response should contain sw360:obligations");
        assertTrue(responseBody.contains("LICENSE_OBLIGATION"), "Response should contain LICENSE_OBLIGATION");
    }

    @Test
    public void should_get_obligation_by_id() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("title"), "Response should contain obligation title");
        assertTrue(responseBody.contains("text"), "Response should contain obligation text");
        assertTrue(responseBody.contains("obligationLevel"), "Response should contain obligation level");
        assertTrue(responseBody.contains("obligationType"), "Response should contain obligation type");
        assertTrue(responseBody.contains("Obligation 1"), "Response should contain Obligation 1");
        assertTrue(responseBody.contains("License Obligation"), "Response should contain License Obligation");
        assertTrue(responseBody.contains("LICENSE_OBLIGATION"), "Response should contain LICENSE_OBLIGATION");
        assertTrue(responseBody.contains("PERMISSION"), "Response should contain PERMISSION");
    }

    @Test
    public void should_create_obligation() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> obligationData = new LinkedHashMap<>();
        obligationData.put("title", "Test Obligation");
        obligationData.put("text", "This is the text of my Test Obligation");
        obligationData.put("obligationLevel", ObligationLevel.LICENSE_OBLIGATION.toString());
        obligationData.put("obligationType", ObligationType.PERMISSION.toString());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(obligationData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("title"), "Response should contain obligation title");
        assertTrue(responseBody.contains("text"), "Response should contain obligation text");
        assertTrue(responseBody.contains("obligationLevel"), "Response should contain obligation level");
        assertTrue(responseBody.contains("obligationType"), "Response should contain obligation type");
        assertTrue(responseBody.contains("Test Obligation"), "Response should contain Test Obligation");
        assertTrue(responseBody.contains("This is the text of my Test Obligation"), "Response should contain This is the text of my Test Obligation");
        assertTrue(responseBody.contains("LICENSE_OBLIGATION"), "Response should contain LICENSE_OBLIGATION");
        assertTrue(responseBody.contains("PERMISSION"), "Response should contain PERMISSION");
    }

    @Test
    public void should_update_obligation() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("title", "Updated Obligation Title");
        updateData.put("text", "Updated obligation text");
        updateData.put("obligationLevel", ObligationLevel.PROJECT_OBLIGATION.toString());
        updateData.put("obligationType", ObligationType.RISK.toString());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("has been updated successfully"), "Response should contain success message");
        assertTrue(responseBody.contains(obligation1.getId()), "Response should contain obligation ID");
    }

    @Test
    public void should_delete_obligations() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId() + "," + obligation2.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("resourceId"), "Response should contain resourceId");
        assertTrue(responseBody.contains("status"), "Response should contain status");
        assertTrue(responseBody.contains(obligation1.getId()), "Response should contain obligation IDs");
        assertTrue(responseBody.contains(obligation2.getId()), "Response should contain obligation IDs");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void should_handle_obligation_not_found() throws IOException, TException {
        // Mock service to return null for non-existent obligation
        given(obligationServiceMock.getObligationById(eq("nonexistent-id"), any())).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/nonexistent-id",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_fail_update_obligation_with_empty_title() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("title", "");
        updateData.put("text", "Updated text");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_fail_update_obligation_with_empty_text() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("title", "Updated Title");
        updateData.put("text", "");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== EXCEPTION HANDLING TESTS ==========

    @Test
    public void should_handle_exception_in_get_obligations() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get obligations"))
                .when(obligationServiceMock).getObligationsFiltered(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_get_obligation_by_id() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get obligation"))
                .when(obligationServiceMock).getObligationById(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_create_obligation() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to create obligation"))
                .when(obligationServiceMock).createObligation(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> obligationData = new LinkedHashMap<>();
        obligationData.put("title", "Test Obligation");
        obligationData.put("text", "Test text");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(obligationData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_update_obligation() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to update obligation"))
                .when(obligationServiceMock).updateObligation(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("title", "Updated Title");
        updateData.put("text", "Updated text");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_delete_obligation() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to delete obligation"))
                .when(obligationServiceMock).deleteObligation(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/" + obligation1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("500"), "Response should contain server error status for unexpected failure");
    }

    @Test
    public void should_handle_exception_in_delete_nonexistent_obligation() throws IOException, TException {
        // Mock service to throw exception for non-existent obligation
        given(obligationServiceMock.getObligationById(eq("nonexistent-id"), any())).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/obligations/nonexistent-id",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("404"), "Response should contain not found status");
    }
}
