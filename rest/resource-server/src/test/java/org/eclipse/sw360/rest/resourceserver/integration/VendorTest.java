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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
public class VendorTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private Sw360VendorService vendorServiceMock;

    private Vendor testVendor;
    private Vendor testVendor2;
    private Set<Release> testReleases;

    @Before
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
        given(this.vendorServiceMock.getVendors(any())).willReturn(paginatedVendors);
        given(this.vendorServiceMock.getVendorById(eq(testVendor.getId()))).willReturn(testVendor);
        given(this.vendorServiceMock.getVendorById(eq(testVendor2.getId()))).willReturn(testVendor2);
        given(this.vendorServiceMock.getAllReleaseList(eq(testVendor.getId()))).willReturn(testReleases);
        given(this.vendorServiceMock.deleteVendorByid(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.vendorUpdate(any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.mergeVendors(any(), any(), any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.vendorServiceMock.exportExcel()).willReturn(ByteBuffer.allocate(10000));

        // Setup create vendor mock
        Vendor createdVendor = new Vendor("Apache", "Apache Software Foundation", "https://www.apache.org/");
        createdVendor.setId("987567468");
        given(this.vendorServiceMock.createVendor(any())).willReturn(createdVendor);
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
        assertTrue("Response should contain vendors", responseBody.contains("_embedded"));
        assertTrue("Response should contain sw360:vendors", responseBody.contains("sw360:vendors"));
        assertTrue("Response should contain Google vendor", responseBody.contains("Google Inc."));
        assertTrue("Response should contain Pivotal vendor", responseBody.contains("Pivotal Software"));
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
        assertTrue("Response should contain page information", responseBody.contains("page"));
        assertTrue("Response should contain totalElements", responseBody.contains("totalElements"));
        assertTrue("Response should contain totalPages", responseBody.contains("totalPages"));
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
        given(this.vendorServiceMock.searchVendors(eq("Google"), any())).willReturn(paginatedVendors);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/vendors?searchText=Google",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue("Response should contain Google vendor", responseBody.contains("Google Inc."));
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
        assertTrue("Response should contain vendor fullname", responseBody.contains("Google Inc."));
        assertTrue("Response should contain vendor shortname", responseBody.contains("Google"));
        assertTrue("Response should contain vendor URL", responseBody.contains("https://google.com"));
        assertTrue("Response should contain self link", responseBody.contains("_links"));
    }

    @Test
    public void should_return_400_for_nonexistent_vendor() throws IOException {
        // Mock vendor not found - the controller doesn't check for null, it just returns the result
        given(this.vendorServiceMock.getVendorById("nonexistent")).willReturn(null);

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
        assertTrue("Response should contain releases", responseBody.contains("_embedded"));
        assertTrue("Response should contain sw360:releases", responseBody.contains("sw360:releases"));
        assertTrue("Response should contain Release_1", responseBody.contains("Release_1"));
        assertTrue("Response should contain Release_2", responseBody.contains("Release_2"));
    }

    @Test
    public void should_return_404_for_vendor_releases_when_vendor_not_found() throws IOException {
        // Mock vendor not found
        given(this.vendorServiceMock.getVendorById("nonexistent")).willReturn(null);

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
        assertTrue("Response should contain Apache vendor", responseBody.contains("Apache Software Foundation"));
        assertTrue("Response should contain self link", responseBody.contains("_links"));
        assertNotNull("Response should contain location header", response.getHeaders().getLocation());
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
        assertTrue("Response should contain success message", responseBody.contains("Vendor updated successfully"));
    }

    @Test
    public void should_fail_update_vendor_with_duplicate_name() throws IOException {
        // Mock duplicate vendor scenario
        given(this.vendorServiceMock.vendorUpdate(any(), any(), any())).willReturn(RequestStatus.DUPLICATE);

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
        assertTrue("Response should contain duplicate error message", responseBody.contains("already exists"));
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
        assertTrue("Response should contain error message", responseBody.contains("cannot be null"));
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
        assertTrue("Response should contain success message", responseBody.contains("deleted successfully"));
        assertTrue("Response should contain vendor name", responseBody.contains("Google Inc."));
    }

    @Test
    public void should_fail_delete_nonexistent_vendor() throws IOException {
        // Mock vendor not found
        given(this.vendorServiceMock.getVendorById("nonexistent")).willReturn(null);

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
        assertNotNull("Response headers should not be null", responseHeaders);
        assertNotNull("Should have Content-Disposition header", responseHeaders.getFirst("Content-Disposition"));
        assertTrue("Should indicate file download",
                Objects.requireNonNull(responseHeaders.getFirst("Content-Disposition")).contains("attachment"));
        assertTrue("Should specify Excel filename",
                Objects.requireNonNull(responseHeaders.getFirst("Content-Disposition")).contains(".xlsx"));
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
        assertTrue("Response should contain SUCCESS status", responseBody.contains("SUCCESS"));
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
        doThrow(new TException("Test TException")).when(vendorServiceMock)
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
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_exception_in_export_vendors() throws IOException, TException {
        // Mock exception in exportExcel
        doThrow(new TException("Test TException")).when(vendorServiceMock)
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
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }

    @Test
    public void should_handle_exception_in_merge_vendors() throws IOException, TException, ResourceClassNotFoundException {
        // Mock TException in mergeVendors
        doThrow(new TException("Test TException")).when(vendorServiceMock)
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
        assertTrue("Response should contain error information",
                responseBody.contains("error") || responseBody.contains("message"));
    }
}
