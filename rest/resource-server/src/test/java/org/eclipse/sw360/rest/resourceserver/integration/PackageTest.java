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
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
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
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class PackageTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    private Package package1, package2, package3;
    private Set<String> licenseIds;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void before() throws TException {
        // Setup object mapper
        objectMapper = new ObjectMapper();

        // Setup user mock
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmailOrExternalId("user@sw360.org")).willReturn(user);

        // Ensure user service returns the user for any email lookup
        given(this.userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(user);

        // Mock getUserByEmail for RestControllerHelper
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("user@sw360.org")).willReturn(user);

        // Setup license IDs
        licenseIds = new HashSet<>();
        licenseIds.add("MIT");
        licenseIds.add("GPL");

        // Setup test release
        Release testRelease = new Release()
                .setId("98745")
                .setName("Test Release")
                .setVersion("2")
                .setComponentId("17653524")
                .setCreatedOn("2021-04-27")
                .setCreatedBy("admin@sw360.org");

        given(this.releaseServiceMock.getReleaseForUserById(eq(testRelease.getId()), any())).willReturn(testRelease);

        // Setup test packages
        package1 = new Package()
                .setId("122357345")
                .setName("angular-sanitize")
                .setVersion("1.8.2")
                .setPackageType(org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType.LIBRARY)
                .setPurl("pkg:npm/angular-sanitize@1.8.2")
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2023-01-02")
                .setVcs("git+https://github.com/angular/angular.js.git")
                .setHomepageUrl("http://angularjs.org")
                .setLicenseIds(licenseIds)
                .setReleaseId(testRelease.getId())
                .setPackageManager(PackageManager.NPM)
                .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        package2 = new Package()
                .setId("875689754")
                .setName("applicationinsights-web")
                .setVersion("2.5.11")
                .setCreatedBy("user@sw360.org")
                .setCreatedOn("2023-02-02")
                .setPurl("pkg:npm/@microsoft/applicationinsights-web@2.5.11")
                .setPackageManager(PackageManager.NPM)
                .setPackageType(org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType.LIBRARY)
                .setVcs("git+https://github.com/microsoft/ApplicationInsights-JS.git")
                .setHomepageUrl("https://github.com/microsoft/ApplicationInsights-JS#readme")
                .setDescription("Application Insights is an extension of Azure Monitor and provides application performance monitoring (APM) features");

        package3 = new Package()
                .setId("1223573425")
                .setName("angular-sanitize")
                .setVersion("1.8.0")
                .setPackageType(org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType.LIBRARY)
                .setPurl("pkg:npm/angular-sanitize@1.8.0")
                .setCreatedBy("admin@sw360.org")
                .setCreatedOn("2023-01-02")
                .setVcs("git+https://github.com/angular/angular.js.git")
                .setHomepageUrl("http://angularjs.org")
                .setLicenseIds(licenseIds)
                .setReleaseId(testRelease.getId())
                .setPackageManager(PackageManager.NPM)
                .setDescription("Sanitizes an html string by stripping all potentially dangerous tokens.");

        // Setup package service mocks
        when(this.packageServiceMock.createPackage(any(), any())).thenReturn(package1);
        given(this.packageServiceMock.getPackageForUserById(eq(package1.getId()))).willReturn(package1);
        given(this.packageServiceMock.getPackageForUserById(eq(package2.getId()))).willReturn(package2);
        given(this.packageServiceMock.deletePackage(eq(package1.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.packageServiceMock.updatePackage(any(), any())).willReturn(RequestStatus.SUCCESS);

        List<Package> packageList = new ArrayList<>();
        packageList.add(package1);
        packageList.add(package2);

        List<Package> packageListByName = new ArrayList<>();
        packageListByName.add(package1);
        packageListByName.add(package3);

        given(this.packageServiceMock.getPackagesForUser()).willReturn(packageList);
        given(this.packageServiceMock.searchPackageByName(any())).willReturn(packageListByName);
        given(this.packageServiceMock.searchByPackageManager(any())).willReturn(List.of(package1));
        given(this.packageServiceMock.searchPackageByVersion(any())).willReturn(List.of(package1));
        given(this.packageServiceMock.searchPackageByPurl(any())).willReturn(List.of(package1));
        given(this.packageServiceMock.getTotalPackagesCounts()).willReturn(packageList.size());
    }

    // ========== PACKAGE CRUD TESTS ==========

    @Test
    public void should_get_all_packages() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded packages");
        assertTrue(responseBody.contains("sw360:packages"), "Response should contain sw360:packages");
        assertTrue(responseBody.contains("page"), "Response should contain pagination info");
        assertTrue(responseBody.contains("totalElements"), "Response should contain totalElements");
        assertTrue(responseBody.contains("angular-sanitize"), "Response should contain angular-sanitize package");
        assertTrue(responseBody.contains("applicationinsights-web"), "Response should contain applicationinsights-web package");
    }

    @Test
    public void should_get_packages_with_all_details() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?allDetails=true&page=0&page_entries=5&sort=name,desc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded packages");
        assertTrue(responseBody.contains("sw360:packages"), "Response should contain sw360:packages");
        assertTrue(responseBody.contains("id"), "Response should contain package IDs");
        assertTrue(responseBody.contains("name"), "Response should contain package names");
        assertTrue(responseBody.contains("version"), "Response should contain package versions");
        assertTrue(responseBody.contains("packageType"), "Response should contain package types");
        assertTrue(responseBody.contains("createdOn"), "Response should contain created dates");
        assertTrue(responseBody.contains("packageManager"), "Response should contain package managers");
        assertTrue(responseBody.contains("purl"), "Response should contain PURLs");
        assertTrue(responseBody.contains("vcs"), "Response should contain VCS URLs");
        assertTrue(responseBody.contains("homepageUrl"), "Response should contain homepage URLs");
        assertTrue(responseBody.contains("licenseIds"), "Response should contain license IDs");
        assertTrue(responseBody.contains("description"), "Response should contain descriptions");
    }

    @Test
    public void should_get_package_by_id() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("id"), "Response should contain package ID");
        assertTrue(responseBody.contains("name"), "Response should contain package name");
        assertTrue(responseBody.contains("version"), "Response should contain package version");
        assertTrue(responseBody.contains("packageType"), "Response should contain package type");
        assertTrue(responseBody.contains("createdOn"), "Response should contain created date");
        assertTrue(responseBody.contains("packageManager"), "Response should contain package manager");
        assertTrue(responseBody.contains("purl"), "Response should contain PURL");
        assertTrue(responseBody.contains("vcs"), "Response should contain VCS URL");
        assertTrue(responseBody.contains("homepageUrl"), "Response should contain homepage URL");
        assertTrue(responseBody.contains("licenseIds"), "Response should contain license IDs");
        assertTrue(responseBody.contains("releaseId"), "Response should contain release ID");
        assertTrue(responseBody.contains("description"), "Response should contain description");
        assertTrue(responseBody.contains("angular-sanitize"), "Response should contain angular-sanitize");
        assertTrue(responseBody.contains("1.8.2"), "Response should contain 1.8.2");
    }

    @Test
    public void should_create_package() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> packageData = new LinkedHashMap<>();
        packageData.put("name", "angular-sanitize");
        packageData.put("version", "1.8.2");
        packageData.put("packageType", "LIBRARY");
        packageData.put("purl", "pkg:npm/angular-sanitize@1.8.2");
        packageData.put("vcs", "git+https://github.com/angular/angular.js.git");
        packageData.put("homepageUrl", "https://github.com/angular/angular-sanitize");
        packageData.put("licenseIds", licenseIds);
        packageData.put("releaseId", "98745");
        packageData.put("description", "Sanitizes a html string by stripping all potentially dangerous tokens.");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(packageData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("id"), "Response should contain package ID");
        assertTrue(responseBody.contains("name"), "Response should contain package name");
        assertTrue(responseBody.contains("version"), "Response should contain package version");
        assertTrue(responseBody.contains("packageType"), "Response should contain package type");
        assertTrue(responseBody.contains("createdOn"), "Response should contain created date");
        assertTrue(responseBody.contains("packageManager"), "Response should contain package manager");
        assertTrue(responseBody.contains("purl"), "Response should contain PURL");
        assertTrue(responseBody.contains("vcs"), "Response should contain VCS URL");
        assertTrue(responseBody.contains("homepageUrl"), "Response should contain homepage URL");
        assertTrue(responseBody.contains("licenseIds"), "Response should contain license IDs");
        assertTrue(responseBody.contains("releaseId"), "Response should contain release ID");
        assertTrue(responseBody.contains("description"), "Response should contain description");
        assertTrue(responseBody.contains("_links"), "Response should contain _links");
        assertTrue(responseBody.contains("_embedded"), "Response should contain _embedded");
    }

    @Test
    public void should_update_package() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("homepageUrl", "https://angularJS.org");
        updateData.put("description", "Updated Description");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("id"), "Response should contain package ID");
        assertTrue(responseBody.contains("name"), "Response should contain package name");
        assertTrue(responseBody.contains("version"), "Response should contain package version");
        assertTrue(responseBody.contains("packageType"), "Response should contain package type");
        assertTrue(responseBody.contains("createdOn"), "Response should contain created date");
        assertTrue(responseBody.contains("packageManager"), "Response should contain package manager");
        assertTrue(responseBody.contains("purl"), "Response should contain PURL");
        assertTrue(responseBody.contains("vcs"), "Response should contain VCS URL");
        assertTrue(responseBody.contains("https://angularJS.org"), "Response should contain updated homepage URL");
        assertTrue(responseBody.contains("Updated Description"), "Response should contain updated description");
        assertTrue(responseBody.contains("licenseIds"), "Response should contain license IDs");
        assertTrue(responseBody.contains("releaseId"), "Response should contain release ID");
        assertTrue(responseBody.contains("_links"), "Response should contain _links");
        assertTrue(responseBody.contains("_embedded"), "Response should contain _embedded");
    }

    @Test
    public void should_delete_package() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== PACKAGE SEARCH TESTS ==========

    @Test
    public void should_search_packages() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?name=angular&version=1.8.2&purl=pkg:npm/angular-sanitize@1.8.2&packageManager=NPM&licenses=MIT,GPL&createdBy=admin@sw360.org&createdOn=2023-01-02&allDetails=true&orphanPackage=false&luceneSearch=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded packages");
        assertTrue(responseBody.contains("sw360:packages"), "Response should contain sw360:packages");
        assertTrue(responseBody.contains("page"), "Response should contain pagination info");
    }

    @Test
    public void should_search_packages_by_name() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?name=angular-sanitize",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("_embedded"), "Response should contain embedded packages");
        assertTrue(responseBody.contains("sw360:packages"), "Response should contain sw360:packages");
        assertTrue(responseBody.contains("angular-sanitize"), "Response should contain angular-sanitize");
    }

    @Test
    public void should_search_packages_by_purl_without_lucene_before_pagination() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?luceneSearch=false&purl=pkg:npm/angular-sanitize@1.8.2&page=0&page_entries=1&sort=name,desc&allDetails=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("angular-sanitize"), "Response should contain purl-matching package");
        assertFalse(responseBody.contains("applicationinsights-web"), "Response should not contain non-matching package");
    }

    @Test
    public void should_search_packages_by_double_encoded_purl_without_lucene() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages?luceneSearch=false&purl=pkg%253Anpm%252Fangular-sanitize%25401.8.2&page=0&page_entries=10&sort=score%252Casc&allDetails=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        String responseBody = response.getBody();
        assertTrue(responseBody.contains("angular-sanitize"), "Response should contain purl-matching package");
        assertFalse(responseBody.contains("applicationinsights-web"), "Response should not contain non-matching package");
    }

    // ========== EXCEPTION HANDLING TESTS ==========

    @Test
    public void should_handle_exception_in_get_packages() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get packages"))
                .when(packageServiceMock).getPackagesForUser();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_get_package_by_id() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to get package"))
                .when(packageServiceMock).getPackageForUserById(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_create_package() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to create package"))
                .when(packageServiceMock).createPackage(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> packageData = new LinkedHashMap<>();
        packageData.put("name", "test-package");
        packageData.put("version", "1.0.0");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(packageData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages",
                        HttpMethod.POST,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_update_package() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to update package"))
                .when(packageServiceMock).updatePackage(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("description", "Updated Description");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updateData, headers);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.PATCH,
                        requestEntity,
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_delete_package() throws IOException, TException {
        // Mock service to throw exception
        doThrow(new RuntimeException("Failed to delete package"))
                .when(packageServiceMock).deletePackage(any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/packages/" + package1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
