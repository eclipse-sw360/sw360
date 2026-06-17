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
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class LicenseTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    private License license1, license2, license3;
    private Obligation obligation1, obligation2;
    private RequestSummary testRequestSummary;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() throws TException, IOException {
        // Setup test request summary
        testRequestSummary = new RequestSummary();
        testRequestSummary.setRequestStatus(RequestStatus.SUCCESS);
        testRequestSummary.setMessage("{\"licensesSuccess\":0,\"licensesMissing\":0}");
        testRequestSummary.setTotalAffectedElements(0);
        testRequestSummary.setTotalElements(0);

        // Setup object mapper
        objectMapper = new ObjectMapper();

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Setup test licenses
        license1 = new License();
        license1.setId("Apache-2.0");
        license1.setFullname("Apache License 2.0");
        license1.setShortname("Apache 2.0");
        license1.setText("placeholder for the Apache 2.0 license text");
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("SPDX", "Apache-2.0");
        externalIds.put("Trove", "License :: OSI Approved :: Apache Software License");
        license1.setExternalIds(externalIds);
        license1.setAdditionalData(Collections.singletonMap("Key", "Value"));
        license1.setNote("License's Note");
        license1.setExternalLicenseLink("https://spdx.org/licenses/Apache-2.0.html");

        license2 = new License();
        license2.setId("MIT");
        license2.setFullname("The MIT License (MIT)");
        license2.setShortname("MIT");
        license2.setText("placeholder for the MIT license text");
        license2.setNote("License2's Note");
        license2.setExternalLicenseLink("https://spdx.org/licenses/MIT.html");

        license3 = new License();
        license3.setId("Apache-3.0");
        license3.setShortname("Apache 3.0");
        license3.setFullname("Apache License 3.0");

        // Setup test obligations
        obligation1 = new Obligation();
        obligation1.setId("0001");
        obligation1.setTitle("Obligation 1");
        obligation1.setText("This is text of Obligation 1");
        obligation1.setWhitelist(Collections.singleton("Department"));
        obligation1.setObligationType(ObligationType.PERMISSION);
        obligation1.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);

        obligation2 = new Obligation();
        obligation2.setId("0002");
        obligation2.setTitle("Obligation 2");
        obligation2.setText("This is text of Obligation 2");
        obligation2.setWhitelist(Collections.singleton("Department2"));
        obligation2.setObligationType(ObligationType.OBLIGATION);
        obligation2.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);

        List<Obligation> obligations = Arrays.asList(obligation1, obligation2);
        Set<String> obligationIds = new HashSet<>(Arrays.asList(obligation1.getId(), obligation2.getId()));
        license2.setObligationDatabaseIds(obligationIds);
        license2.setObligations(obligations);

        // Setup license types
        LicenseType licenseType1 = new LicenseType(1, "Public domain");
        licenseType1.setId("0443dda0b9ef420fa1f200e497efc98f");
        LicenseType licenseType2 = new LicenseType(2, "Proprietary license");
        licenseType2.setId("9e86774d0769e77bdf5902f936cb55c3");
        List<LicenseType> licenseTypes = new ArrayList<>(Arrays.asList(licenseType1, licenseType2));

        // Setup license service mocks
        List<License> licenseList = new ArrayList<>();
        licenseList.add(license1);
        licenseList.add(license2);

        given(this.licenseServiceMock.getLicenses()).willReturn(licenseList);
        given(this.licenseServiceMock.getLicenseById(eq(license1.getId()))).willReturn(license1);
        given(this.licenseServiceMock.getLicenseById(eq(license2.getId()))).willReturn(license2);
        given(this.licenseServiceMock.createLicense(any(), any())).willReturn(license3);
        given(this.licenseServiceMock.updateLicense(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.licenseServiceMock.updateWhitelist(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.licenseServiceMock.getObligationsByLicenseId(any())).willReturn(obligations);
        given(this.licenseServiceMock.getLicenseTypes()).willReturn(licenseTypes);
        given(this.licenseServiceMock.deleteLicenseType(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.licenseServiceMock.importOsadlInformation(any())).willReturn(testRequestSummary);
        given(this.licenseServiceMock.addLicenseType(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360ReportServiceMock.getLicenseBuffer()).willReturn(ByteBuffer.allocate(10000));
        given(this.licenseServiceMock.importSpdxInformation(any())).willReturn(new RequestSummary().setRequestStatus(RequestStatus.SUCCESS));

        doNothing().when(licenseServiceMock).deleteLicenseById(any(), any());
        doNothing().when(licenseServiceMock).deleteAllLicenseInfo(any());
        doNothing().when(licenseServiceMock).getDownloadLicenseArchive(any(), any(), any());
        doNothing().when(licenseServiceMock).uploadLicense(any(), any(), anyBoolean(), anyBoolean());
    }

    // ========== LICENSE CRUD TESTS ==========

    @Test
    public void should_get_all_licenses() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses?page=0&page_entries=5",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded licenses");
        assertTrue(responseBody.contains("sw360:licenses"), "Response should contain sw360:licenses");
        assertTrue(responseBody.contains("page"), "Response should contain pagination info");
        assertTrue(responseBody.contains("totalElements"), "Response should contain totalElements");
        assertTrue(responseBody.contains("Apache License 2.0"), "Response should contain Apache license");
        assertTrue(responseBody.contains("The MIT License (MIT)"), "Response should contain MIT license");
    }

    @Test
    public void should_get_license_by_id() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("fullName"), "Response should contain license fullName");
        assertTrue(responseBody.contains("shortName"), "Response should contain license shortName");
        assertTrue(responseBody.contains("Apache License 2.0"), "Response should contain Apache License 2.0");
        assertTrue(responseBody.contains("Apache 2.0"), "Response should contain Apache 2.0");
        assertTrue(responseBody.contains("externalIds"), "Response should contain externalIds");
        assertTrue(responseBody.contains("SPDX"), "Response should contain SPDX identifier");
        assertTrue(responseBody.contains("externalLicenseLink"), "Response should contain external license link");
        assertTrue(responseBody.contains("note"), "Response should contain note");
        assertTrue(responseBody.contains("License's Note"), "Response should contain License's Note");
    }

    @Test
    public void should_create_license() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> licenseData = new HashMap<>();
        licenseData.put("fullName", "Apache 3.0");
        licenseData.put("shortName", "Apache License 3.0");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(licenseData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("fullName"), "Response should contain created license fullName");
        assertTrue(responseBody.contains("shortName"), "Response should contain created license shortName");
        assertTrue(responseBody.contains("Apache 3.0"), "Response should contain Apache 3.0");
        assertTrue(responseBody.contains("Apache License 3.0"), "Response should contain Apache License 3.0");
        assertTrue(responseBody.contains("checked"), "Response should contain checked field");
        assertTrue(responseBody.contains("OSIApproved"), "Response should contain OSIApproved field");
        assertTrue(responseBody.contains("FSFLibre"), "Response should contain FSFLibre field");
        assertTrue(responseBody.contains("_links"), "Response should contain _links");
    }

    @Test
    public void should_update_license() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> licenseData = new HashMap<>();
        licenseData.put("fullName", "Apache License 4.0");
        licenseData.put("note", "Apache License");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(licenseData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("fullName"), "Response should contain updated license data");
    }

    @Test
    public void should_delete_license() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_delete_all_licenses() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/deleteAll",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== LICENSE TYPE TESTS ==========

    @Test
    public void should_get_license_types() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenseTypes",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded license types");
        assertTrue(responseBody.contains("sw360:licenseTypes"), "Response should contain sw360:licenseTypes");
        assertTrue(responseBody.contains("Public domain"), "Response should contain Public domain license type");
        assertTrue(responseBody.contains("Proprietary license"), "Response should contain Proprietary license type");
        assertTrue(responseBody.contains("_links"), "Response should contain _links");
    }

    @Test
    public void should_create_license_type() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/addLicenseType?licenseType=wer",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("SUCCESS"), "Response should contain SUCCESS status");
        assertTrue("SUCCESS".equals(responseBody) || responseBody.contains("SUCCESS"), "Response should be a valid response");
    }

    @Test
    public void should_delete_license_type() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenseTypes/" + license1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("SUCCESS"), "Response should contain SUCCESS status");
    }

    // ========== OBLIGATION TESTS ==========

    @Test
    public void should_get_obligations_by_license() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license2.getId() + "/obligations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded obligations");
        assertTrue(responseBody.contains("sw360:obligations"), "Response should contain sw360:obligations");
        assertTrue(responseBody.contains("Obligation 1"), "Response should contain Obligation 1");
        assertTrue(responseBody.contains("Obligation 2"), "Response should contain Obligation 2");
        assertTrue(responseBody.contains("title"), "Response should contain obligation titles");
        assertTrue(responseBody.contains("obligationType"), "Response should contain obligation types");
        assertTrue(responseBody.contains("_links"), "Response should contain _links");
    }

    @Test
    public void should_link_obligations() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Set<String> obligationIds = new HashSet<>(Arrays.asList(obligation1.getId(), obligation2.getId()));

        HttpEntity<Set<String>> requestEntity = new HttpEntity<>(obligationIds, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId() + "/obligations",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ========== IMPORT/EXPORT TESTS ==========

    @Test
    public void should_import_spdx_info() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/import/SPDX",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_import_osadl_info() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/import/OSADL",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("requestStatus"), "Response should contain request status");
        assertTrue(responseBody.contains("SUCCESS"), "Response should contain SUCCESS status");
        assertTrue(responseBody.contains("licensesSuccess"), "Response should contain OSADL import message");
        assertTrue(responseBody.contains("totalElements"), "Response should contain total elements");
        assertTrue(responseBody.contains("totalAffectedElements"), "Response should contain total affected elements");
    }


    @Test
    public void should_upload_license() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("licenseFile", new ByteArrayResource("test license content".getBytes()) {
            @Override
            public String getFilename() {
                return "test-license.rdf";
            }
        });
        body.add("overwriteIfExternalIdMatches", false);
        body.add("overwriteIfIdMatchesEvenWithoutExternalIdMatch", false);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/upload",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("SUCCESS") || responseBody.contains("success") ||
                responseBody.contains("uploaded") || responseBody.isEmpty(), "Response should indicate successful upload");
    }

    @Test
    public void should_get_license_report() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/reports?module=licenses",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========== EXCEPTION HANDLING TESTS ==========

    @Test
    public void should_handle_exception_in_get_licenses() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get licenses"))
                .when(licenseServiceMock).getLicenses();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_get_license_by_id() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get license"))
                .when(licenseServiceMock).getLicenseById(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_create_license() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to create license"))
                .when(licenseServiceMock).createLicense(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> licenseData = new HashMap<>();
        licenseData.put("fullName", "Test License");
        licenseData.put("shortName", "Test");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(licenseData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_update_license() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to update license"))
                .when(licenseServiceMock).updateLicense(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> licenseData = new HashMap<>();
        licenseData.put("fullName", "Updated License");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(licenseData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_delete_license() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to delete license"))
                .when(licenseServiceMock).deleteLicenseById(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_get_obligations() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get obligations"))
                .when(licenseServiceMock).getObligationsByLicenseId(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license2.getId() + "/obligations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_link_obligations() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to link obligations"))
                .when(licenseServiceMock).updateLicenseToDB(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Set<String> obligationIds = new HashSet<>(Arrays.asList(obligation1.getId()));

        HttpEntity<Set<String>> requestEntity = new HttpEntity<>(obligationIds, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId() + "/obligations",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_unlink_obligations() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to unlink obligations"))
                .when(licenseServiceMock).updateLicenseToDB(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Set<String> obligationIds = new HashSet<>(Arrays.asList(obligation1.getId()));

        HttpEntity<Set<String>> requestEntity = new HttpEntity<>(obligationIds, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId() + "/obligations",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_update_whitelist() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to update whitelist"))
                .when(licenseServiceMock).updateWhitelist(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Boolean> whitelistData = new HashMap<>();
        whitelistData.put("0001", true);

        HttpEntity<Map<String, Boolean>> requestEntity = new HttpEntity<>(whitelistData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/" + license1.getId() + "/whitelist",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_import_spdx() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to import SPDX"))
                .when(licenseServiceMock).importSpdxInformation(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/import/SPDX",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_import_osadl() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to import OSADL"))
                .when(licenseServiceMock).importOsadlInformation(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/import/OSADL",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_create_license_type() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to create license type"))
                .when(licenseServiceMock).addLicenseType(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenses/addLicenseType?licenseType=test",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_delete_license_type() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to delete license type"))
                .when(licenseServiceMock).deleteLicenseType(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/licenseTypes/" + license1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
