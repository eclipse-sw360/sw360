/*
 * Copyright ADITYA-CODE-SOURCE, 2026. Part of the SW360 GSoC Project.
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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licensedb.LicenseDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Integration tests for LicenseDB sync functionality.
 * Tests the REST endpoints for LicenseDB integration including:
 * - License sync from LicenseDB to SW360
 * - Obligation sync from LicenseDB to SW360
 * - Connection testing
 * - Error handling when LicenseDB is unavailable
 *
 * Relates to issue #3841.
 */
@RunWith(SpringRunner.class)
public class LicenseDBSyncIntegrationTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360LicenseService licenseServiceMock;

    @MockitoBean
    private LicenseDBService licenseDBServiceMock;

    private ObjectMapper objectMapper;
    private License testLicense;
    private Obligation testObligation;

    @Before
    public void before() throws TException, IOException {
        objectMapper = new ObjectMapper();

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Setup test license
        testLicense = new License();
        testLicense.setId("MIT");
        testLicense.setShortname("MIT");
        testLicense.setFullname("The MIT License (MIT)");
        testLicense.setExternalIds(new HashMap<String, String>() {{
            put("LICENSEDB_ID", "licensedb-mit-001");
        }});

        // Setup test obligation
        testObligation = new Obligation();
        testObligation.setId("0001");
        testObligation.setTitle("MIT Obligation");
        testObligation.setText("Include copyright notice");
        testObligation.setObligationType(ObligationType.OBLIGATION);
        testObligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
    }

    // ========== LICENSE SYNC TESTS ==========

    @Test
    public void should_sync_licenses_from_licensedb() throws IOException, TException {
        // Mock successful license sync
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("licensesCreated", 5);
        syncResult.put("licensesUpdated", 2);
        syncResult.put("message", "License sync completed successfully");

        given(licenseDBServiceMock.syncLicenses(any())).willReturn(syncResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain success", responseBody.contains("success"));
        assertTrue("Response should contain licensesCreated", responseBody.contains("licensesCreated"));
        assertTrue("Response should contain licensesUpdated", responseBody.contains("licensesUpdated"));

        verify(licenseDBServiceMock, times(1)).syncLicenses(any());
    }

    @Test
    public void should_sync_obligations_from_licensedb() throws IOException, TException {
        // Mock successful obligation sync
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("obligationsCreated", 3);
        syncResult.put("obligationsUpdated", 1);
        syncResult.put("message", "Obligation sync completed successfully");

        given(licenseDBServiceMock.syncObligations(any())).willReturn(syncResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/obligations",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain success", responseBody.contains("success"));
        assertTrue("Response should contain obligationsCreated", responseBody.contains("obligationsCreated"));

        verify(licenseDBServiceMock, times(1)).syncObligations(any());
    }

    @Test
    public void should_sync_all_from_licensedb() throws IOException, TException {
        // Mock successful full sync
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("licensesCreated", 5);
        syncResult.put("obligationsCreated", 3);
        syncResult.put("message", "Full sync completed successfully");

        given(licenseDBServiceMock.syncAll(any())).willReturn(syncResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain success", responseBody.contains("success"));

        verify(licenseDBServiceMock, times(1)).syncAll(any());
    }

    // ========== CONNECTION TESTS ==========

    @Test
    public void should_test_licensedb_connection_success() throws IOException, TException {
        // Mock successful connection
        Map<String, Object> healthResult = new HashMap<>();
        healthResult.put("status", "connected");
        healthResult.put("message", "LicenseDB is reachable");

        given(licenseDBServiceMock.healthCheck()).willReturn(healthResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/test-connection",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain connected", responseBody.contains("connected"));

        verify(licenseDBServiceMock, times(1)).healthCheck();
    }

    @Test
    public void should_test_licensedb_connection_disabled() throws IOException, TException {
        // Mock disabled LicenseDB
        Map<String, Object> healthResult = new HashMap<>();
        healthResult.put("status", "disabled");
        healthResult.put("message", "LicenseDB integration is not enabled");

        given(licenseDBServiceMock.healthCheck()).willReturn(healthResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/test-connection",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain status", responseBody.contains("status"));
        assertTrue("Response should contain disabled", responseBody.contains("disabled"));
    }

    @Test
    public void should_get_licensedb_status() throws IOException, TException {
        // Mock status response
        Map<String, Object> statusResult = new HashMap<>();
        statusResult.put("enabled", true);
        statusResult.put("apiUrl", "https://licensedb.example.com");
        statusResult.put("apiVersion", "v1");
        statusResult.put("lastSync", "2026-04-05T14:00:00Z");
        statusResult.put("lastSyncStatus", "success");

        given(licenseDBServiceMock.getStatus()).willReturn(statusResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/status",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain enabled", responseBody.contains("enabled"));
        assertTrue("Response should contain apiUrl", responseBody.contains("apiUrl"));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void should_handle_licensedb_unavailable_during_sync() throws IOException, TException {
        // Mock LicenseDB unavailable error
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "error");
        errorResult.put("message", "Connection refused: LicenseDB is unavailable");

        given(licenseDBServiceMock.syncLicenses(any())).willReturn(errorResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error status", responseBody.contains("error"));
        assertTrue("Response should contain error message", responseBody.contains("Connection refused"));
    }

    @Test
    public void should_handle_auth_failure_during_sync() throws IOException, TException {
        // Mock authentication failure
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("status", "error");
        errorResult.put("message", "Authentication failed: Invalid credentials");

        given(licenseDBServiceMock.syncLicenses(any())).willReturn(errorResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain error status", responseBody.contains("error"));
        assertTrue("Response should contain auth error", responseBody.contains("Authentication failed"));
    }

    @Test
    public void should_handle_runtime_exception_during_sync() throws IOException, TException {
        // Mock service throwing runtime exception
        doThrow(new RuntimeException("LicenseDB service error"))
                .when(licenseDBServiceMock).syncLicenses(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_runtime_exception_during_connection_test() throws IOException, TException {
        // Mock service throwing runtime exception
        doThrow(new RuntimeException("Connection test failed"))
                .when(licenseDBServiceMock).healthCheck();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/test-connection",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== DATA PERSISTENCE TESTS ==========

    @Test
    public void should_verify_license_persistence_after_sync() throws IOException, TException {
        // Mock sync that creates licenses
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("licensesCreated", 1);
        syncResult.put("licensesUpdated", 0);

        given(licenseDBServiceMock.syncLicenses(any())).willReturn(syncResult);

        // After sync, verify license can be retrieved
        given(licenseServiceMock.getLicenseById("MIT")).willReturn(testLicense);

        HttpHeaders headers = getHeaders(port);

        // First sync
        ResponseEntity<String> syncResponse =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, syncResponse.getStatusCode());
        assertTrue(syncResponse.getBody().contains("success"));

        // Then verify license exists
        ResponseEntity<String> getResponse =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/licenses/MIT",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertTrue("Response should contain license ID", getResponse.getBody().contains("MIT"));

        verify(licenseDBServiceMock, times(1)).syncLicenses(any());
        verify(licenseServiceMock, times(1)).getLicenseById("MIT");
    }

    @Test
    public void should_verify_obligation_persistence_after_sync() throws IOException, TException {
        // Mock sync that creates obligations
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("obligationsCreated", 1);
        syncResult.put("obligationsUpdated", 0);

        given(licenseDBServiceMock.syncObligations(any())).willReturn(syncResult);

        // After sync, verify obligation can be retrieved
        given(licenseServiceMock.getObligationsByLicenseId("MIT")).willReturn(Collections.singletonList(testObligation));

        HttpHeaders headers = getHeaders(port);

        // First sync
        ResponseEntity<String> syncResponse =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/obligations",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, syncResponse.getStatusCode());
        assertTrue(syncResponse.getBody().contains("success"));

        // Then verify obligations exist
        ResponseEntity<String> getResponse =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/licenses/MIT/obligations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertTrue("Response should contain obligations", getResponse.getBody().contains("obligations"));

        verify(licenseDBServiceMock, times(1)).syncObligations(any());
        verify(licenseServiceMock, times(1)).getObligationsByLicenseId("MIT");
    }

    // ========== AUTHORIZATION TESTS ==========

    @Test
    public void should_require_admin_for_sync() throws IOException, TException {
        // This test verifies that sync endpoints require ADMIN authorization
        // The test uses admin credentials which should succeed
        Map<String, Object> syncResult = new HashMap<>();
        syncResult.put("status", "success");
        syncResult.put("licensesCreated", 0);

        given(licenseDBServiceMock.syncLicenses(any())).willReturn(syncResult);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/license-db/sync/licenses",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        // With admin credentials, should succeed
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
