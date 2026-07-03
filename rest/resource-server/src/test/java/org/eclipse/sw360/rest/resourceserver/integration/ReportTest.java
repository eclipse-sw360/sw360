/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;

public class ReportTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void before() throws Exception {
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org"))
                .willReturn(UserConverter.fromThrift(TestHelper.getTestUser()));

        ByteBuffer dummyBuffer = ByteBuffer.wrap("dummy content".getBytes());

        given(sw360ReportServiceMock.getLicenseInfoBuffer(any(), eq("project123"), any()))
                .willReturn(dummyBuffer);

        // Return a filename with non-ASCII characters (™ symbol)
        given(sw360ReportServiceMock.getGenericLicInfoFileName(any(), eq("project123"), any(), any()))
                .willReturn("LicenseInfo-Gridscale X\u2122 Protection-1.2.1-2026-05-16_16_09_30.docx");
    }

    @Test
    public void should_return_report_with_valid_content_disposition_for_unicode_project_name() throws Exception {
        HttpHeaders headers = getHeaders(port);
        String url = "http://localhost:" + port + "/api/reports"
                + "?module=licenseInfo"
                + "&projectId=project123"
                + "&generatorClassName=DocxGenerator"
                + "&variant=REPORT";

        ResponseEntity<byte[]> response = new TestRestTemplate().exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull("Content-Disposition header should be present", contentDisposition);
        // ASCII fallback should have non-ASCII replaced with underscore
        assertTrue(contentDisposition.contains("filename=\"LicenseInfo-Gridscale X_ Protection-1.2.1-2026-05-16_16_09_30.docx\""), "Should contain ASCII fallback filename");
        // RFC 5987 encoded filename should be present
        assertTrue(contentDisposition.contains("filename*=UTF-8''"), "Should contain RFC 5987 filename*");
        // Should contain URL-encoded trademark symbol
        assertTrue(contentDisposition.contains("%E2%84%A2"), "Should contain encoded TM symbol");
    }

    @Test
    public void should_return_report_with_ascii_only_project_name() throws Exception {
        given(sw360ReportServiceMock.getGenericLicInfoFileName(any(), eq("projectAscii"), any(), any()))
                .willReturn("LicenseInfo-SimpleProject-1.0-2026-05-16.docx");

        ByteBuffer dummyBuffer = ByteBuffer.wrap("dummy content".getBytes());
        given(sw360ReportServiceMock.getLicenseInfoBuffer(any(), eq("projectAscii"), any()))
                .willReturn(dummyBuffer);

        HttpHeaders headers = getHeaders(port);
        String url = "http://localhost:" + port + "/api/reports"
                + "?module=licenseInfo"
                + "&projectId=projectAscii"
                + "&generatorClassName=DocxGenerator"
                + "&variant=REPORT";

        ResponseEntity<byte[]> response = new TestRestTemplate().exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull(contentDisposition, "Content-Disposition header should be present");
        assertTrue(contentDisposition.contains("filename=\"LicenseInfo-SimpleProject-1.0-2026-05-16.docx\""), "Should contain the original filename");
        assertTrue(contentDisposition.contains("filename*=UTF-8''"), "Should contain RFC 5987 filename*");
    }
}
