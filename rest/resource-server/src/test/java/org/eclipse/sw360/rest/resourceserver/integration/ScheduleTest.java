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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.schedule.Sw360ScheduleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
    public void before() throws TException {
        // Setup test request summary
        testRequestSummary = new RequestSummary();
        testRequestSummary.setRequestStatus(RequestStatus.SUCCESS);
        testRequestSummary.setMessage("Service scheduled successfully");

        // Setup object mapper
        objectMapper = new ObjectMapper();

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Setup schedule service mocks
        given(this.scheduleServiceMock.cancelAllServices(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.scheduleCveSearch(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.cancelCveSearch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.deleteAttachmentService(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.cancelDeleteAttachment(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelAttachmentDeletionLocalFS(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.triggerCveSearch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelSvmSync(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.cancelSvmReverseMatch(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.scheduleSvmReverseMatch(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.svmReleaseTrackingFeedback(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.svmMonitoringListUpdate(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.triggerSrcUpload(any())).willReturn(testRequestSummary);
        given(this.scheduleServiceMock.cancelSvmMonitoringListUpdate(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.unscheduleSrcUpload(any())).willReturn(RequestStatus.SUCCESS);
        given(this.scheduleServiceMock.triggerSourceUploadForReleaseComponents(any())).willReturn(RequestStatus.SUCCESS);
    }

    // ========== SCHEDULE SERVICE TESTS ==========

    @Test
    public void should_cancel_all_schedule_services() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleAllServices",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_schedule_cve_service() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cveService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_cancel_schedule_svm_sync() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleSvmSync",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_schedule_reverse_svm_match() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/svmReverseMatch",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_cancel_reverse_match() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleSvmReverseMatch",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_track_feedback() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/trackingFeedback",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_svm_list_update() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/monitoringListUpdate",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_cancel_monitoring_svm_list() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelMonitoringListUpdate",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_src_upload() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/srcUpload",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_cancel_src_upload() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelSrcUpload",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_unschedule_cve_search() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleCve",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_schedule_service_from_local() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/deleteAttachment",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain request summary", response.getBody().contains("requestStatus"));
    }

    @Test
    public void should_cancel_schedule_attachment() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unScheduleDeleteAttachment",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_delete_old_attachment_from_local() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelAttachmentDeletion",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
    }

    @Test
    public void should_schedule_source_upload() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/scheduleSourceUploadForReleaseComponents",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain SUCCESS status", response.getBody().contains("SUCCESS"));
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

    // ========== EXCEPTION HANDLING TESTS ==========

    @Test
    public void should_handle_exception_in_cancel_all_services() throws IOException, TException {
        // Mock service to throw exception
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

    @Test
    public void should_handle_exception_in_schedule_cve_service() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("CVE service scheduling failed"))
                .when(scheduleServiceMock).scheduleCveSearch(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cveService",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_schedule_svm_sync() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("SVM sync scheduling failed"))
                .when(scheduleServiceMock).svmSync(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/scheduleSvmSync",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_cancel_svm_sync() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("SVM sync cancellation failed"))
                .when(scheduleServiceMock).cancelSvmSync(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleSvmSync",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_reverse_svm_match() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Reverse SVM match failed"))
                .when(scheduleServiceMock).scheduleSvmReverseMatch(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/svmReverseMatch",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_cancel_reverse_match() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Reverse match cancellation failed"))
                .when(scheduleServiceMock).cancelSvmReverseMatch(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleSvmReverseMatch",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_track_feedback() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Tracking feedback failed"))
                .when(scheduleServiceMock).svmReleaseTrackingFeedback(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/trackingFeedback",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_svm_list_update() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("SVM list update failed"))
                .when(scheduleServiceMock).svmMonitoringListUpdate(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/monitoringListUpdate",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_cancel_monitoring_svm_list() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Monitoring list cancellation failed"))
                .when(scheduleServiceMock).cancelSvmMonitoringListUpdate(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelMonitoringListUpdate",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_src_upload() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Source upload failed"))
                .when(scheduleServiceMock).triggerSrcUpload(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/srcUpload",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_cancel_src_upload() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Source upload cancellation failed"))
                .when(scheduleServiceMock).unscheduleSrcUpload(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelSrcUpload",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_unschedule_cve_search() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("CVE search unscheduling failed"))
                .when(scheduleServiceMock).cancelCveSearch(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unscheduleCve",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_schedule_service_from_local() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Attachment deletion scheduling failed"))
                .when(scheduleServiceMock).deleteAttachmentService(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/deleteAttachment",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_cancel_schedule_attachment() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Attachment scheduling cancellation failed"))
                .when(scheduleServiceMock).cancelDeleteAttachment(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/unScheduleDeleteAttachment",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_delete_old_attachment_from_local() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Attachment deletion from local FS failed"))
                .when(scheduleServiceMock).cancelAttachmentDeletionLocalFS(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/cancelAttachmentDeletion",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_schedule_source_upload() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Source upload scheduling failed"))
                .when(scheduleServiceMock).triggerSourceUploadForReleaseComponents(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/schedule/scheduleSourceUploadForReleaseComponents",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
