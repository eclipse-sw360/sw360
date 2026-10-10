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

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.sw360.datahandler.thrift.ReportFormat;
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

public class ReportTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void before() throws Exception {
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org"))
                .willReturn(TestHelper.getTestUser());

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

    @Test
    public void should_export_projects_components_and_licenses_as_readable_formats() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Data");
            sheet.createRow(0).createCell(0).setCellValue("name");
            sheet.createRow(1).createCell(0).setCellValue("Example");
            workbook.write(out);
        }
        ByteBuffer spreadsheet = ByteBuffer.wrap(out.toByteArray());
        given(sw360ReportServiceMock.getProjectReportBuffer(any(), eq("project123"), any()))
                .willReturn(ByteBuffer.wrap("<records/>".getBytes(StandardCharsets.UTF_8)));
        given(sw360ReportServiceMock.getComponentBuffer(any(), eq(false))).willReturn(spreadsheet);
        given(sw360ReportServiceMock.getLicenseBuffer()).willReturn(spreadsheet);
        given(sw360ReportServiceMock.getDocumentName(any(), any(), any())).willReturn("report.xlsx");
        given(sw360ReportServiceMock.getDocumentName(any(), any(), eq("components"), eq(ReportFormat.CSV)))
                .willReturn("components.csv");
        given(sw360ReportServiceMock.getDocumentName(any(), any(), eq("licenses"), eq(ReportFormat.JSON)))
                .willReturn("licenses.json");
        given(sw360ReportServiceMock.getDocumentName(any(), eq("project123"), eq("projects"), eq(ReportFormat.XML)))
                .willReturn("project.xml");

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> components = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/reports?module=components&format=csv",
                HttpMethod.GET, new HttpEntity<>(null, headers), byte[].class);
        ResponseEntity<byte[]> licenses = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/reports?module=licenses&format=json",
                HttpMethod.GET, new HttpEntity<>(null, headers), byte[].class);
        ResponseEntity<byte[]> projects = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/reports?module=projects&projectId=project123&format=xml",
                HttpMethod.GET, new HttpEntity<>(null, headers), byte[].class);

        assertEquals(HttpStatus.OK, components.getStatusCode());
        assertTrue(components.getHeaders().getContentType().toString().startsWith("text/csv"));
        assertTrue(components.getHeaders().getFirst("Content-Disposition").contains("components.csv"));
        assertTrue(new String(components.getBody(), StandardCharsets.UTF_8).contains("name"));
        assertEquals(HttpStatus.OK, licenses.getStatusCode());
        assertTrue(licenses.getHeaders().getContentType().toString().startsWith("application/json"));
        assertTrue(licenses.getHeaders().getFirst("Content-Disposition").contains("licenses.json"));
        assertTrue(new String(licenses.getBody(), StandardCharsets.UTF_8).contains("Example"));
        assertEquals(HttpStatus.OK, projects.getStatusCode());
        assertTrue(projects.getHeaders().getContentType().toString().startsWith("application/xml"));
        assertTrue(projects.getHeaders().getFirst("Content-Disposition").contains("project.xml"));
        assertEquals("<records/>", new String(projects.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    public void should_download_emailed_project_report_with_its_format() throws Exception {
        given(sw360ReportServiceMock.getDocumentName(any(), any(), any())).willReturn("report.xlsx");
        given(sw360ReportServiceMock.getDocumentName(any(), eq("project123"), eq("projects"), eq(ReportFormat.XML)))
                .willReturn("project.xml");
        ByteBuffer report = ByteBuffer.allocate(32);
        report.put("<records/>".getBytes(StandardCharsets.UTF_8));
        report.flip();
        given(sw360ReportServiceMock.getReportStreamFromURl(any(), eq(false), eq("report-token")))
                .willReturn(report);

        ResponseEntity<byte[]> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/reports/download?module=projects&projectId=project123&token=report-token&format=xml",
                HttpMethod.GET, new HttpEntity<>(null, getHeaders(port)), byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getContentType().toString().startsWith("application/xml"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("project.xml"));
        assertEquals("<records/>", new String(response.getBody(), StandardCharsets.UTF_8));
    }
}
