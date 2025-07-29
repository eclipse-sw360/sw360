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

import jakarta.servlet.ServletException;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.importexport.Sw360ImportExportService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
public class ImportExportTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ImportExportService importExportServiceMock;

    @Before
    public void before() throws TException, ServletException, IOException {
        // Setup default successful responses
        RequestSummary successSummary = new RequestSummary();
        successSummary.setRequestStatus(RequestStatus.SUCCESS);
        successSummary.setMessage("Operation completed successfully");

        given(this.importExportServiceMock.uploadComponent(any(), any(), any(), any())).willReturn(successSummary);
        given(this.importExportServiceMock.uploadReleaseLink(any(), any(), any())).willReturn(successSummary);
        given(this.importExportServiceMock.uploadComponentAttachment(any(), any(), any())).willReturn(successSummary);

        // Setup void methods (download operations)
        doNothing().when(importExportServiceMock).getDownloadCsvComponentTemplate(any(), any());
        doNothing().when(importExportServiceMock).getDownloadAttachmentTemplate(any(), any());
        doNothing().when(importExportServiceMock).getDownloadAttachmentInfo(any(), any());
        doNothing().when(importExportServiceMock).getDownloadReleaseSample(any(), any());
        doNothing().when(importExportServiceMock).getDownloadReleaseLink(any(), any());
        doNothing().when(importExportServiceMock).getComponentDetailedExport(any(), any());
        doNothing().when(importExportServiceMock).getDownloadUsers(any(), any());

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
        Assert.assertNotNull(response.getBody());
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
        Assert.assertNotNull(response.getBody());
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
        Assert.assertNotNull(response.getBody());
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

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
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

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
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

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
