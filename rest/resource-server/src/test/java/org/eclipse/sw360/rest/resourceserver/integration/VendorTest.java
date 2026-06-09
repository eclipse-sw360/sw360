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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

public class VendorTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    private Vendor testVendor;
    private Vendor testVendor2;
    private Set<Release> testReleases;

    @BeforeEach
    public void before() throws TException, ResourceClassNotFoundException {
        // Setup test vendor data
        testVendor = new Vendor();
        testVendor.setId("876876776");
        testVendor.setFullname("Google Inc.");
        testVendor.setShortname("Google");
        testVendor.setUrl("https://google.com");

        testVendor2 = new Vendor();
        testVendor2.setId("987567468");
        testVendor2.setFullname("Pivotal Software, Inc.");
        testVendor2.setShortname("Pivotal");
        testVendor2.setUrl("https://pivotal.io/");

        // Setup test releases
        testReleases = new HashSet<>();
        Release release1 = new Release();
        release1.setId("12345");
        release1.setName("Release_1");
        release1.setVersion("1.0.0");
        release1.setVendor(testVendor);

        Release release2 = new Release();
        release2.setId("123456");
        release2.setName("Release_2");
        release2.setVersion("2.0.0");
        release2.setVendor(testVendor);

        testReleases.add(release1);
        testReleases.add(release2);

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Setup vendor service mocks
        List<Vendor> vendorList = Arrays.asList(testVendor, testVendor2);
        PaginationData pageData = new PaginationData();
        pageData.setAscending(true);
        pageData.setRowsPerPage(10);
        pageData.setDisplayStart(0);
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors = Map.of(pageData, vendorList);
        given(this.sw360VendorService.getVendors(any())).willReturn(paginatedVendors);
        given(this.sw360VendorService.getVendorById(eq(testVendor.getId()))).willReturn(testVendor);
        given(this.sw360VendorService.getVendorById(eq(testVendor2.getId()))).willReturn(testVendor2);
        given(this.sw360VendorService.getAllReleaseList(eq(testVendor.getId()))).willReturn(testReleases);
        given(this.sw360VendorService.deleteVendorByid(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360VendorService.vendorUpdate(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360VendorService.mergeVendors(any(), any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.sw360VendorService.exportExcel()).willReturn(ByteBuffer.allocate(10000));

        // Setup create vendor mock
        Vendor createdVendor = new Vendor("Apache", "Apache Software Foundation", "https://www.apache.org/");
        createdVendor.setId("987567468");
        given(this.sw360VendorService.createVendor(any())).willReturn(createdVendor);
    }

    // ========== GET VENDORS TESTS ==========

    @Test
    public void should_get_all_vendors() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify response structure
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain vendors");
        assertTrue(responseBody.contains("sw360:vendors"), "Response should contain sw360:vendors");
        assertTrue(responseBody.contains("Google Inc."), "Response should contain Google vendor");
        assertTrue(responseBody.contains("Pivotal Software"), "Response should contain Pivotal vendor");
    }

    @Test
    public void should_get_vendors_with_pagination() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors?page=0&page_entries=5",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify pagination structure
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("page"), "Response should contain page information");
        assertTrue(responseBody.contains("totalElements"), "Response should contain totalElements");
        assertTrue(responseBody.contains("totalPages"), "Response should contain totalPages");
    }

    @Test
    public void should_get_vendors_with_search() throws IOException {
        // Mock search functionality
        PaginationData pageData = new PaginationData();
        pageData.setAscending(true);
        pageData.setRowsPerPage(10);
        pageData.setDisplayStart(0);
        pageData.setSortColumnNumber(0);
        Map<PaginationData, List<Vendor>> paginatedVendors = Map.of(pageData, Collections.singletonList(testVendor));
        given(this.sw360VendorService.searchVendors(eq("Google"), any())).willReturn(paginatedVendors);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors?searchText=Google",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Google Inc."), "Response should contain Google vendor");
    }

    // ========== GET SINGLE VENDOR TESTS ==========

    @Test
    public void should_get_vendor_by_id() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify vendor details
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Google Inc."), "Response should contain vendor fullname");
        assertTrue(responseBody.contains("Google"), "Response should contain vendor shortname");
        assertTrue(responseBody.contains("https://google.com"), "Response should contain vendor URL");
        assertTrue(responseBody.contains("_links"), "Response should contain self link");
    }

    @Test
    public void should_return_400_for_nonexistent_vendor() throws IOException {
        // Mock vendor not found - the controller doesn't check for null, it just returns the result
        given(this.sw360VendorService.getVendorById("nonexistent")).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/nonexistent",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // The controller doesn't throw ResourceNotFoundException for getVendor,
        // it just returns the result which could be null, leading to a 400 BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    // ========== GET VENDOR RELEASES TESTS ==========

    @Test
    public void should_get_vendor_releases() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId() + "/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify releases structure
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain releases");
        assertTrue(responseBody.contains("sw360:releases"), "Response should contain sw360:releases");
        assertTrue(responseBody.contains("Release_1"), "Response should contain Release_1");
        assertTrue(responseBody.contains("Release_2"), "Response should contain Release_2");
    }

    @Test
    public void should_return_404_for_vendor_releases_when_vendor_not_found() throws IOException {
        // Mock vendor not found
        given(this.sw360VendorService.getVendorById("nonexistent")).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/nonexistent/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ========== CREATE VENDOR TESTS ==========

    @Test
    public void should_create_vendor() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create vendor request body
        Map<String, Object> vendorData = new HashMap<>();
        vendorData.put("fullName", "Apache Software Foundation");
        vendorData.put("shortName", "Apache");
        vendorData.put("url", "https://www.apache.org/");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(vendorData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify created vendor
        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Apache Software Foundation"), "Response should contain Apache vendor");
        assertTrue(responseBody.contains("_links"), "Response should contain self link");
        assertNotNull(response.getHeaders().getLocation(), "Response should contain location header");
    }

    @Test
    public void should_fail_create_vendor_without_authority() throws IOException {
        // Test without proper authorization
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> vendorData = new HashMap<>();
        vendorData.put("fullName", "Test Vendor");
        vendorData.put("shortName", "Test");
        vendorData.put("url", "https://test.com");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(vendorData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ========== UPDATE VENDOR TESTS ==========

    @Test
    public void should_update_vendor() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Update vendor request body
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", "Updated Google Inc.");
        updateData.put("shortName", "Updated Google");
        updateData.put("url", "https://updated-google.com");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("Vendor updated successfully"), "Response should contain success message");
    }

    @Test
    public void should_fail_update_vendor_with_duplicate_name() throws IOException {
        // Mock duplicate vendor scenario
        given(this.sw360VendorService.vendorUpdate(any(), any(), any())).willReturn(RequestStatus.DUPLICATE);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", "Duplicate Vendor Name");
        updateData.put("shortName", "Duplicate");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("already exists"), "Response should contain duplicate error message");
    }

    @Test
    public void should_fail_update_vendor_with_empty_body() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Empty update data
        Map<String, Object> updateData = new HashMap<>();

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("cannot be null"), "Response should contain error message");
    }

    // ========== DELETE VENDOR TESTS ==========

    @Test
    public void should_delete_vendor() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("deleted successfully"), "Response should contain success message");
        assertTrue(responseBody.contains("Google Inc."), "Response should contain vendor name");
    }

    @Test
    public void should_fail_delete_nonexistent_vendor() throws IOException {
        // Mock vendor not found
        given(this.sw360VendorService.getVendorById("nonexistent")).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/nonexistent",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ========== EXPORT VENDOR TESTS ==========

    @Test
    public void should_export_vendors() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/exportVendorDetails",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response headers for file download
        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should not be null");
        assertNotNull(responseHeaders.getFirst("Content-Disposition"), "Should have Content-Disposition header");
        assertTrue(Objects.requireNonNull(responseHeaders.getFirst("Content-Disposition")).contains("attachment"), "Should indicate file download");
        assertTrue(Objects.requireNonNull(responseHeaders.getFirst("Content-Disposition")).contains(".xlsx"), "Should specify Excel filename");
    }

    // ========== MERGE VENDOR TESTS ==========

    @Test
    public void should_merge_vendors() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Merge request body
        Map<String, Object> mergeData = new HashMap<>();
        mergeData.put("fullName", "Merged Vendor");
        mergeData.put("shortName", "Merged");
        mergeData.put("url", "https://merged-vendor.com");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(mergeData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/mergeVendors?mergeTargetId=target123&mergeSourceId=source456",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("SUCCESS"), "Response should contain SUCCESS status");
    }

    @Test
    public void should_fail_merge_vendors_with_missing_parameters() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> mergeData = new HashMap<>();
        mergeData.put("fullName", "Merged Vendor");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(mergeData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/mergeVendors",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== EXCEPTION COVERAGE TESTS ==========

    @Test
    public void should_handle_exception_in_get_vendor_releases() throws IOException, TException {
        // Mock TException in getAllReleaseList
        doThrow(new TException("Test TException")).when(sw360VendorService)
                .getAllReleaseList(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/" + testVendor.getId() + "/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_exception_in_export_vendors() throws IOException, TException {
        // Mock exception in exportExcel
        doThrow(new TException("Test TException")).when(sw360VendorService)
                .exportExcel();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/exportVendorDetails",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }

    @Test
    public void should_handle_exception_in_merge_vendors() throws IOException, TException, ResourceClassNotFoundException {
        // Mock TException in mergeVendors
        doThrow(new TException("Test TException")).when(sw360VendorService)
                .mergeVendors(any(), any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> mergeData = new HashMap<>();
        mergeData.put("fullName", "Merged Vendor");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(mergeData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors/mergeVendors?mergeTargetId=target123&mergeSourceId=source456",
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("error") || responseBody.contains("message"), "Response should contain error information");
    }
}
