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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.importexport.Sw360ImportExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

public class ImportExportTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ImportExportService importExportServiceMock;

    @BeforeEach
    public void before() throws TException, ServletException, IOException {
        // Setup successful responses with detailed data
        RequestSummary successSummary = new RequestSummary();
        successSummary.setRequestStatus(RequestStatus.SUCCESS);
        successSummary.setMessage("Operation completed successfully");
        successSummary.setTotalElements(5);
        successSummary.setTotalAffectedElements(5);

        // Setup failure responses for error scenarios
        RequestSummary failureSummary = new RequestSummary();
        failureSummary.setRequestStatus(RequestStatus.FAILURE);
        failureSummary.setMessage("Operation failed due to invalid data");
        failureSummary.setTotalElements(3);
        failureSummary.setTotalAffectedElements(0);

        // Mock successful upload operations
        given(this.importExportServiceMock.uploadComponent(any(), any(), any(), any())).willReturn(successSummary);
        given(this.importExportServiceMock.uploadReleaseLink(any(), any(), any())).willReturn(successSummary);
        given(this.importExportServiceMock.uploadComponentAttachment(any(), any(), any())).willReturn(successSummary);

        // Setup void methods (download operations) with proper response writing
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"test.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("test,csv,data".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadCsvComponentTemplate(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"attachment_template.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("attachment,template,data".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadAttachmentTemplate(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"attachment_info.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("attachment,info,data".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadAttachmentInfo(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"release_sample.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("release,sample,data".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadReleaseSample(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"release_link.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("release,link,data".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadReleaseLink(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"component_details.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("component,details,data".getBytes());
            return null;
        }).when(importExportServiceMock).getComponentDetailedExport(any(), any());

        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(1);
            response.setHeader("Content-Disposition", "attachment; filename=\"users.csv\"");
            response.setContentType("text/csv");
            response.getOutputStream().write("users,data,export".getBytes());
            return null;
        }).when(importExportServiceMock).getDownloadUsers(any(), any());

        // Mock user service to return a proper User object
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
    }

    @Test
    public void should_download_component_template() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponentTemplate",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers indicate file download
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertTrue(Objects.requireNonNull(responseHeaders.getFirst("Content-Disposition")).contains("test.csv"), "Should be CSV content");

        // Verify response body contains CSV data
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("test,csv,data"), "Response should contain CSV data");
    }

    @Test
    public void should_download_attachment_sample() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadAttachmentSample",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("attachment,template,data"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("attachment_template.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_download_attachment_info() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadAttachmentInfo",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("attachment,info,data"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("attachment_info.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_download_release_sample() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseSample",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("release,sample,data"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("release_sample.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_download_release_link() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseLink",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("release,link,data"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("release_link.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_download_component_details() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponent",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("component,details,data"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("component_details.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_download_users() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadUsers",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("users,data,export"), "Response should contain CSV data");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("users.csv"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_upload_component_csv() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create a mock CSV file content
        String csvContent = "Component Name,Component Version,Component Description\nTest Component,1.0,Test Description";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-components.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("componentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestSummary> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        RequestSummary.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(RequestStatus.SUCCESS, response.getBody().getRequestStatus());
    }

    @Test
    public void should_upload_release_csv() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create a mock CSV file content
        String csvContent = "Release Name,Release Version,Component ID\nTest Release,1.0,comp123";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-releases.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("releaseFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestSummary> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadRelease",
                        HttpMethod.POST,
                        requestEntity,
                        RequestSummary.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(RequestStatus.SUCCESS, response.getBody().getRequestStatus());
    }

    @Test
    public void should_upload_component_attachment_csv() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create a mock CSV file content
        String csvContent = "Component ID,Attachment Type,File Name\ncomp123,SOURCE,test.zip";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-attachments.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("attachmentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestSummary> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/componentAttachment",
                        HttpMethod.POST,
                        requestEntity,
                        RequestSummary.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(RequestStatus.SUCCESS, response.getBody().getRequestStatus());
    }

    @Test
    public void should_fail_upload_component_without_file() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // No file added to body

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_fail_upload_release_without_file() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // No file added to body

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadRelease",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_fail_upload_attachment_without_file() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // No file added to body

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/componentAttachment",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_verify_comprehensive_upload_response() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create comprehensive test data
        String csvContent = "Component Name,Component Version,Component Description,Vendor Name\n" +
                "Test Component,1.0,Test Description,Test Vendor\n" +
                "Another Component,2.0,Another Description,Another Vendor";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "comprehensive-test.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("componentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestSummary> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        RequestSummary.class);

        // Comprehensive response verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        RequestSummary responseBody = response.getBody();
        assertEquals(RequestStatus.SUCCESS, responseBody.getRequestStatus());
        assertEquals(responseBody.getMessage(), "Operation completed successfully");
        assertEquals(5, responseBody.getTotalElements());
        assertEquals(5, responseBody.getTotalAffectedElements());

        // Verify the response structure is complete
        assertEquals(RequestStatus.SUCCESS, responseBody.getRequestStatus(), "Response should indicate success");
        assertTrue(responseBody.getMessage() != null && !responseBody.getMessage().isEmpty(), "Response should have a meaningful message");
        assertTrue(responseBody.getTotalElements() > 0, "Total elements should be positive");
        assertTrue(responseBody.getTotalAffectedElements() > 0, "Affected elements should be positive");
    }

    @Test
    public void should_verify_download_response_structure() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponentTemplate",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertNotNull(responseHeaders.getFirst("Content-Type"), "Should have Content-Type header");

        // Verify response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("test,csv,data"), "Response should contain CSV data");
        assertTrue(!responseBody.isEmpty(), "Response should not be empty");

        // Verify specific header values
        String contentDisposition = responseHeaders.getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
        assertTrue(contentDisposition.contains("filename"), "Content-Disposition should specify filename");
    }

    @Test
    public void should_verify_upload_response_with_detailed_assertions() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create test data with multiple records
        String csvContent = "Component Name,Component Version,Component Description,Vendor Name\n" +
                "Apache Commons,1.0,Utility library,Apache Foundation\n" +
                "Spring Framework,5.3.0,Application framework,Spring Source\n" +
                "JUnit,4.13,Testing framework,JUnit Team";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "detailed-test-components.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("componentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RequestSummary> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        RequestSummary.class);

        // Comprehensive response assertions
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");

        RequestSummary responseBody = response.getBody();

        // Verify RequestStatus
        assertEquals(RequestStatus.SUCCESS, responseBody.getRequestStatus(), "Request status should be SUCCESS");
        assertEquals(RequestStatus.SUCCESS, responseBody.getRequestStatus(), "Request status should indicate success");

        // Verify message content
        assertEquals(responseBody.getMessage(), "Operation completed successfully");
        assertNotNull(responseBody.getMessage(), "Message should not be null");
        assertFalse(responseBody.getMessage().isEmpty(), "Message should not be empty");
        assertTrue(responseBody.getMessage().contains("completed"), "Message should contain 'completed'");

        // Verify element counts
        assertEquals(5, responseBody.getTotalElements(), "Total elements should be 5");
        assertEquals(5, responseBody.getTotalAffectedElements(), "Affected elements should be 5");
        assertTrue(responseBody.getTotalElements() > 0, "Total elements should be positive");
        assertTrue(responseBody.getTotalAffectedElements() > 0, "Affected elements should be positive");
        assertTrue(responseBody.getTotalAffectedElements() <= responseBody.getTotalElements(), "Affected elements should not exceed total elements");

        // Verify response structure integrity
        assertTrue(responseBody.isSetRequestStatus() &&
                responseBody.isSetMessage() &&
                responseBody.isSetTotalElements() &&
                responseBody.isSetTotalAffectedElements(), "Response should be complete");
    }

    @Test
    public void should_verify_error_response_structure() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // No file added to body - this should cause an error

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        // Verify error response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "HTTP status should be BAD_REQUEST");

        // Verify error response body
        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertFalse(responseBody.isEmpty(), "Error response should not be empty");

        // Verify error response structure (JSON error response)
        assertTrue(responseBody.contains("error") || responseBody.contains("message") || responseBody.contains("status"), "Error response should contain error information");
    }


    // ========== EXCEPTION COVERAGE TESTS ==========

    @Test
    public void should_handle_io_exception_in_download_component_template() throws IOException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadCsvComponentTemplate(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponentTemplate",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading component template"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_io_exception_in_download_attachment_sample() throws IOException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadAttachmentTemplate(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadAttachmentSample",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading attachment sample"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_io_exception_in_download_attachment_info() throws IOException, TTransportException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadAttachmentInfo(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadAttachmentInfo",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading attachment info"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_t_exception_in_download_release_sample() throws IOException, TException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .getDownloadReleaseSample(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseSample",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading release sample"), "Error response should contain TException message");
    }

    @Test
    public void should_handle_io_exception_in_download_release_sample() throws IOException, TException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadReleaseSample(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseSample",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading release sample"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_t_exception_in_download_release_link() throws IOException, TException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .getDownloadReleaseLink(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseLink",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading release link"), "Error response should contain TException message");
    }

    @Test
    public void should_handle_io_exception_in_download_release_link() throws IOException, TException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadReleaseLink(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadReleaseLink",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading release link"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_t_exception_in_download_component() throws IOException, TException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .getComponentDetailedExport(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponent",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading component"), "Error response should contain TException message");
    }

    @Test
    public void should_handle_io_exception_in_download_component() throws IOException, TException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getComponentDetailedExport(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadComponent",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error downloading component"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_t_exception_in_download_users() throws IOException, TException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .getDownloadUsers(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadUsers",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error download users"), "Error response should contain TException message");
    }

    @Test
    public void should_handle_io_exception_in_download_users() throws IOException, TException {
        // Mock the service to throw IOException
        doThrow(new IOException("Test IO Exception")).when(importExportServiceMock)
                .getDownloadUsers(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/downloadUsers",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Should return 500 Internal Server Error due to SW360Exception
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("Error download users"), "Error response should contain IO exception message");
    }

    @Test
    public void should_handle_service_exception_in_upload_component() throws IOException, TException, ServletException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .uploadComponent(any(), any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String csvContent = "Component Name,Component Version\nTest Component,1.0";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-components.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("componentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadComponent",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        // Should return 500 Internal Server Error due to TException
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Error response should contain exception information");
    }

    @Test
    public void should_handle_service_exception_in_upload_release() throws IOException, TException, ServletException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .uploadReleaseLink(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String csvContent = "Release Name,Release Version\nTest Release,1.0";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-releases.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("releaseFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/uploadRelease",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        // Should return 500 Internal Server Error due to TException
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Error response should contain exception information");
    }

    @Test
    public void should_handle_service_exception_in_upload_component_attachment() throws IOException, TException, ServletException {
        // Mock the service to throw TException
        doThrow(new TException("Test TException")).when(importExportServiceMock)
                .uploadComponentAttachment(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String csvContent = "Attachment Name,Attachment Type\nTest Attachment,BINARY";
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-attachments.csv";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("attachmentFile", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/importExport/componentAttachment",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        // Should return 500 Internal Server Error due to TException
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody, "Error response body should not be null");
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Error response should contain exception information");
    }

}
