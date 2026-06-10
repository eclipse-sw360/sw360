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
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.schedule.Sw360ScheduleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
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
public class ScheduleTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ScheduleService scheduleServiceMock;

    private RequestSummary testRequestSummary;
    private ObjectMapper objectMapper;

    @Before
    public void before() throws Exception {
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
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_handle_exception_in_cancel_all_services() throws IOException {
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
        assertTrue("Response should contain requestStatus", response.getBody().contains("requestStatus"));
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
        assertTrue("Response should contain SUCCESS", response.getBody().contains("SUCCESS"));
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
        assertTrue("Response should contain SUCCESS", response.getBody().contains("SUCCESS"));
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
        assertTrue("Response should contain service name key", response.getBody().contains("cvesearchService"));
        assertTrue("Response should contain isScheduled", response.getBody().contains("isScheduled"));
        assertTrue("Response should contain intervalSeconds", response.getBody().contains("intervalSeconds"));
        assertTrue("Response should contain firstOffsetSeconds", response.getBody().contains("firstOffsetSeconds"));
        assertTrue("Response should contain nextSynchronization", response.getBody().contains("nextSynchronization"));
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
        assertTrue("Response should contain cvesearchService", response.getBody().contains("cvesearchService"));
        assertTrue("Response should contain svmsyncService", response.getBody().contains("svmsyncService"));
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
        assertTrue("Response should be true", response.getBody().contains("true"));
    }

    @Test
    public void should_return_false_when_no_service_is_scheduled() throws IOException {
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
        assertTrue("Response should be false", response.getBody().contains("false"));
    }

    @Test
    public void should_handle_exception_in_schedule_service() throws IOException {
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
