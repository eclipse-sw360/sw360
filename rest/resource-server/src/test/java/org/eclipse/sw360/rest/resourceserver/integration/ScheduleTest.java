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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

public class ScheduleTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    private RequestSummary testRequestSummary;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() throws TException {
        testRequestSummary = new RequestSummary();
        testRequestSummary.setRequestStatus(RequestStatus.SUCCESS);
        testRequestSummary.setMessage("Service scheduled successfully");

        objectMapper = new ObjectMapper();

        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        given(this.scheduleServiceMock.cancelAllServices(any())).willReturn(RequestStatus.SUCCESS);

        // New unified endpoints
        given(this.scheduleServiceMock.scheduleService(any(), anyString())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.unscheduleService(any(), anyString())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.triggerManualService(any(), anyString())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.isServiceScheduled(anyString(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.isAnyServiceScheduled(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.getServiceDetails(anyString(), any()))
                .willReturn(Map.of("isScheduled", true, "firstOffsetSeconds", 0,
                        "intervalSeconds", 86400, "nextSynchronization", "2026-05-13T00:00:00"));
        given(this.scheduleServiceMock.getAllServicesDetails(any()))
                .willReturn(java.util.Map.of(
                        "cvesearchService", Map.of("isScheduled", true, "firstOffsetSeconds", 0,
                                "intervalSeconds", 86400, "nextSynchronization", "2026-05-13T00:00:00"),
                        "svmsyncService", Map.of("isScheduled", false, "firstOffsetSeconds", 3600,
                                "intervalSeconds", 86400, "nextSynchronization", "N/A")
                ));
    }

    // ========== UNSCHEDULE ALL SERVICES TEST ==========

    @Test
    public void should_cancel_all_schedule_services() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleAllServices",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("SUCCESS"), "Response should contain SUCCESS status");
    }

    @Test
    public void should_handle_exception_in_cancel_all_services() throws IOException, TException {
        doThrow(new RuntimeException("Service cancellation failed"))
                .when(scheduleServiceMock).cancelAllServices(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleAllServices",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== NEW UNIFIED ENDPOINT TESTS ==========

    @Test
    public void should_schedule_service_by_name() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/scheduleService?serviceName=cvesearchService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("requestStatus"), "Response should contain requestStatus");
    }

    @Test
    public void should_return_bad_request_when_schedule_service_name_missing() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/scheduleService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_unschedule_service_by_name() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/unscheduleService?serviceName=cvesearchService",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("SUCCESS"), "Response should contain SUCCESS");
    }

    @Test
    public void should_return_bad_request_when_unschedule_service_name_missing() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/unscheduleService",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_trigger_service_by_name() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/triggerService?serviceName=cvesearchService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("SUCCESS"), "Response should contain SUCCESS");
    }

    @Test
    public void should_return_bad_request_when_trigger_service_name_missing() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/triggerService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_return_service_details_for_specific_service() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/serviceDetails?serviceName=cvesearchService",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cvesearchService"), "Response should contain service name key");
        assertTrue(response.getBody().contains("isScheduled"), "Response should contain isScheduled");
        assertTrue(response.getBody().contains("intervalSeconds"), "Response should contain intervalSeconds");
        assertTrue(response.getBody().contains("firstOffsetSeconds"), "Response should contain firstOffsetSeconds");
        assertTrue(response.getBody().contains("nextSynchronization"), "Response should contain nextSynchronization");
    }

    @Test
    public void should_return_all_service_details_when_no_service_name() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/serviceDetails",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cvesearchService"), "Response should contain cvesearchService");
        assertTrue(response.getBody().contains("svmsyncService"), "Response should contain svmsyncService");
    }

    @Test
    public void should_check_service_status() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/status?serviceName=cveSearch",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void should_check_if_any_service_scheduled() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/isAnyServiceScheduled",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void should_return_true_when_any_service_is_scheduled() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/isAnyServiceScheduled",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("true"), "Response should be true");
    }

    @Test
    public void should_return_false_when_no_service_is_scheduled() throws IOException, TException {
        given(this.scheduleServiceMock.isAnyServiceScheduled(any())).willReturn(RequestStatus.FAILURE);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/isAnyServiceScheduled",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("false"), "Response should be false");
    }

    @Test
    public void should_handle_exception_in_schedule_service() throws IOException, TException {
        doThrow(new RuntimeException("Schedule service failed"))
                .when(scheduleServiceMock).scheduleService(any(), eq("cvesearchService"));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/schedule/scheduleService?serviceName=cvesearchService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
