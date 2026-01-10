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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.obligation.Sw360ObligationService;
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
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
public class ObligationTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ObligationService obligationServiceMock;

    private Obligation obligation1, obligation2;
    private List<Obligation> obligationList;
    private ObjectMapper objectMapper;

    @Before
    public void before() throws TException {
        // Setup test user
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmailOrExternalId("user@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("user@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail(anyString())).willReturn(user);

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
        assertTrue("Response should contain embedded obligations", responseBody.contains("_embedded"));
        assertTrue("Response should contain sw360:obligations", responseBody.contains("sw360:obligations"));
        assertTrue("Response should contain pagination info", responseBody.contains("page"));
        assertTrue("Response should contain totalElements", responseBody.contains("totalElements"));
        assertTrue("Response should contain Obligation 1", responseBody.contains("Obligation 1"));
        assertTrue("Response should contain Obligation 2", responseBody.contains("Obligation 2"));
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
        assertTrue("Response should contain embedded obligations", responseBody.contains("_embedded"));
        assertTrue("Response should contain sw360:obligations", responseBody.contains("sw360:obligations"));
        assertTrue("Response should contain LICENSE_OBLIGATION", responseBody.contains("LICENSE_OBLIGATION"));
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
        assertTrue("Response should contain obligation title", responseBody.contains("title"));
        assertTrue("Response should contain obligation text", responseBody.contains("text"));
        assertTrue("Response should contain obligation level", responseBody.contains("obligationLevel"));
        assertTrue("Response should contain obligation type", responseBody.contains("obligationType"));
        assertTrue("Response should contain Obligation 1", responseBody.contains("Obligation 1"));
        assertTrue("Response should contain License Obligation", responseBody.contains("License Obligation"));
        assertTrue("Response should contain LICENSE_OBLIGATION", responseBody.contains("LICENSE_OBLIGATION"));
        assertTrue("Response should contain PERMISSION", responseBody.contains("PERMISSION"));
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
        assertTrue("Response should contain obligation title", responseBody.contains("title"));
        assertTrue("Response should contain obligation text", responseBody.contains("text"));
        assertTrue("Response should contain obligation level", responseBody.contains("obligationLevel"));
        assertTrue("Response should contain obligation type", responseBody.contains("obligationType"));
        assertTrue("Response should contain Test Obligation", responseBody.contains("Test Obligation"));
        assertTrue("Response should contain This is the text of my Test Obligation", responseBody.contains("This is the text of my Test Obligation"));
        assertTrue("Response should contain LICENSE_OBLIGATION", responseBody.contains("LICENSE_OBLIGATION"));
        assertTrue("Response should contain PERMISSION", responseBody.contains("PERMISSION"));
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
        assertTrue("Response should contain success message", responseBody.contains("has been updated successfully"));
        assertTrue("Response should contain obligation ID", responseBody.contains(obligation1.getId()));
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
        assertTrue("Response should contain resourceId", responseBody.contains("resourceId"));
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain obligation IDs", responseBody.contains(obligation1.getId()));
        assertTrue("Response should contain obligation IDs", responseBody.contains(obligation2.getId()));
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
        assertTrue("Response should contain error status", responseBody.contains("404"));
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
        assertTrue("Response should contain not found status", responseBody.contains("404"));
    }
}
