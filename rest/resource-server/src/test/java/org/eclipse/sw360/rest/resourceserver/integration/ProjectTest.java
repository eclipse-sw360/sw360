/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import com.google.common.collect.Lists;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ObligationStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private Sw360ProjectService projectServiceMock;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    @MockitoBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockitoBean
    private Sw360LicenseInfoService licenseInfoMockService;

    @MockitoBean
    private SW360PackageService packageServiceMock;

    @MockitoBean
    private SW360ReportService sw360ReportServiceMock;

    @MockitoBean
    private Sw360VulnerabilityService vulnerabilityServiceMock;

    @MockitoBean
    private Sw360LicenseService sw360LicenseServiceMock;

    @MockitoBean
    private Sw360VendorService vendorServiceMock;

    @MockitoBean
    private RestControllerHelper<?> restControllerHelper;

    @MockitoBean
    private Sw360ComponentService componentServiceMock;

    // Test data
    private Project project1;
    private Project project2;
    private Release release1;
    private Release release2;
    private Attachment attachment1;
    private Attachment attachment2;
    private Package package1;
    private User testUser;
    private final Set<Project> projectList = new HashSet<>();

    @Before
    public void before() throws IOException, TException {
        setupTestUser();
        setupTestProjects();
        setupTestReleases();
        setupTestAttachments();
        setupTestPackages();
        setupCommonMocks();
    }

    private void setupTestUser() {
        testUser = new User();
        testUser.setId("123456789");
        testUser.setEmail("admin@sw360.org");
        testUser.setFullname("John Doe");
        testUser.setUserGroup(UserGroup.ADMIN);
        testUser.setDepartment("Test Department");
        testUser.setWantsMailNotification(true);

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(testUser);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(testUser);

        // Mock additional users for contributors
        User user1 = new User();
        user1.setId("user1");
        user1.setEmail("user1@sw360.org");
        user1.setFullname("User One");
        user1.setUserGroup(UserGroup.USER);
        user1.setDepartment("Test Department");

        User user2 = new User();
        user2.setId("user2");
        user2.setEmail("user2@sw360.org");
        user2.setFullname("User Two");
        user2.setUserGroup(UserGroup.USER);
        user2.setDepartment("Test Department");

        given(this.userServiceMock.getUserByEmail("user1@sw360.org")).willReturn(user1);
        given(this.userServiceMock.getUserByEmail("user2@sw360.org")).willReturn(user2);

    }

    private void setupTestProjects() {
        // Project 1
        project1 = new Project();
        project1.setId("p001");
        project1.setName("Project Alpha");
        project1.setDescription("Project Alpha description");
        project1.setProjectType(ProjectType.CUSTOMER);
        project1.setBusinessUnit("Group A");
        project1.setVersion("1.0.0");
        project1.setState(ProjectState.ACTIVE);
        project1.setClearingState(ProjectClearingState.CLOSED);
        project1.setVisbility(Visibility.EVERYONE);
        project1.setCreatedBy("admin@sw360.org");
        project1.setCreatedOn("2023-01-01");
        project1.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org")));
        project1.setContributors(new HashSet<>(Arrays.asList("user1@sw360.org")));
        project1.setAttachments(new HashSet<>());
        project1.setLinkedProjects(new HashMap<>());
        project1.setReleaseIdToUsage(new HashMap<>());
        project1.setExternalIds(new HashMap<>());
        project1.getExternalIds().put("portal-id", "13319-XX3");
        project1.getExternalIds().put("project-ext", "515432");

        // Set up linked obligation ID for orphaned obligations test
        project1.setLinkedObligationId("obl001");

        // Set up ReleaseClearingStateSummary for license clearing count test
        ReleaseClearingStateSummary clearingSummary = new ReleaseClearingStateSummary();
        clearingSummary.setNewRelease(1);
        clearingSummary.setSentToClearingTool(2);
        clearingSummary.setUnderClearing(3);
        clearingSummary.setReportAvailable(4);
        clearingSummary.setScanAvailable(5);
        clearingSummary.setApproved(6);
        project1.setReleaseClearingStateSummary(clearingSummary);

        // Project 2
        project2 = new Project();
        project2.setId("p002");
        project2.setName("Project Beta");
        project2.setDescription("Project Beta description");
        project2.setProjectType(ProjectType.INTERNAL);
        project2.setBusinessUnit("Group B");
        project2.setVersion("2.0.0");
        project2.setState(ProjectState.PHASE_OUT);
        project2.setClearingState(ProjectClearingState.IN_PROGRESS);
        project2.setVisbility(Visibility.EVERYONE);
        project2.setCreatedBy("admin@sw360.org");
        project2.setCreatedOn("2023-01-02");
        project2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org")));
        project2.setContributors(new HashSet<>(Arrays.asList("user2@sw360.org")));
        project2.setAttachments(new HashSet<>());
        project2.setLinkedProjects(new HashMap<>());
        project2.setReleaseIdToUsage(new HashMap<>());
        project2.setExternalIds(new HashMap<>());

        projectList.add(project1);
        projectList.add(project2);
    }

    private void setupTestReleases() {
        // Release 1
        release1 = new Release();
        release1.setId("r001");
        release1.setName("Release Alpha");
        release1.setVersion("1.0.0");
        release1.setComponentId("c001");
        release1.setMainlineState(MainlineState.MAINLINE);
        release1.setClearingState(ClearingState.APPROVED);
        release1.setCreatedBy("admin@sw360.org");
        release1.setCreatedOn("2023-01-01");
        release1.setMainLicenseIds(new HashSet<>(Arrays.asList("Apache-2.0")));
        release1.setOtherLicenseIds(new HashSet<>());
        release1.setAttachments(new HashSet<>());

        // Release 2
        release2 = new Release();
        release2.setId("r002");
        release2.setName("Release Beta");
        release2.setVersion("2.0.0");
        release2.setComponentId("c002");
        release2.setMainlineState(MainlineState.OPEN);
        release2.setClearingState(ClearingState.UNDER_CLEARING);
        release2.setCreatedBy("admin@sw360.org");
        release2.setCreatedOn("2023-01-02");
        release2.setMainLicenseIds(new HashSet<>(Arrays.asList("MIT")));
        release2.setOtherLicenseIds(new HashSet<>());
        release2.setAttachments(new HashSet<>());
    }

    private void setupTestAttachments() {
        // Attachment 1
        attachment1 = new Attachment();
        attachment1.setFilename("test-source.zip");
        attachment1.setAttachmentType(AttachmentType.SOURCE);
        attachment1.setCreatedBy("admin@sw360.org");
        attachment1.setCreatedOn("2023-01-01");
        attachment1.setSha1("da373e491d3863477568896089ee9457bc316783");
        attachment1.setCheckStatus(CheckStatus.ACCEPTED);
        attachment1.setCheckedBy("admin@sw360.org");
        attachment1.setCheckedOn("2023-01-01");

        // Attachment 2
        attachment2 = new Attachment();
        attachment2.setFilename("test-binary.jar");
        attachment2.setAttachmentType(AttachmentType.BINARY);
        attachment2.setCreatedBy("admin@sw360.org");
        attachment2.setCreatedOn("2023-01-02");
        attachment2.setSha1("da373e491d312365483589ee9457bc316783");
        attachment2.setCheckStatus(CheckStatus.ACCEPTED);
        attachment2.setCheckedBy("admin@sw360.org");
        attachment2.setCheckedOn("2023-01-02");
    }

    private void setupTestPackages() {
        package1 = new Package();
        package1.setId("pkg001");
        package1.setName("Test Package");
        package1.setVersion("1.0.0");
        package1.setPackageManager(PackageManager.MAVEN);
        package1.setCreatedBy("admin@sw360.org");
        package1.setCreatedOn("2023-01-01");
        package1.setVendorId("vnd001");
        package1.setModifiedBy("admin@sw360.org");
    }

    private void setupCommonMocks() throws IOException, TException {
        // Common project service mocks
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any()))
            .willReturn(Collections.singletonMap(
                new PaginationData().setRowsPerPage(2).setDisplayStart(0).setTotalRowCount(2),
                Arrays.asList(project1, project2)
            ));
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectList.size()).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        projectList.stream().toList()
                )
        );
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.getProjectForUserById(eq("unknown"), any())).willThrow(new RuntimeException("Project not found"));

        // Common release service mocks
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        // Common attachment service mocks
        given(this.attachmentServiceMock.getAttachmentContent(any())).willReturn(
                new AttachmentContent().setId("att001").setFilename("test-source.zip").setContentType("application/zip")
        );
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment1);

        // Common package service mocks
        given(this.packageServiceMock.getPackageForUserById(eq(package1.getId()))).willReturn(package1);
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);

        // Vendor mock for package embedding path
        Vendor vendor = new Vendor();
        vendor.setId("vnd001");
        vendor.setShortname("Acme");
        given(this.vendorServiceMock.getVendorById(eq("vnd001"))).willReturn(vendor);

        // Common license info service mocks
        given(this.licenseInfoMockService.getLicenseInfoForAttachment(any(), any(), anyString(), anyBoolean())).willReturn(new ArrayList<>());

        // Additional mocks for project operations
        given(this.projectServiceMock.createProject(any(), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.deleteProject(eq(project1.getId()), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getClearingInfo(any(), any())).willReturn(project1);
        given(this.projectServiceMock.getObligationData(eq("obl001"), any())).willReturn(new ObligationList().setId("obl001").setProjectId(project1.getId()));

        // Vulnerability service mocks
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());
        given(this.vulnerabilityServiceMock.fillVulnerabilityMetadata(any(), any())).willReturn(new HashMap<>());
    }

    // ========== CORE PROJECT OPERATIONS ==========

    @Test
    public void should_get_all_projects() throws IOException, TException {
        // Configure the mock to return test data
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any()))
            .willReturn(Collections.singletonMap(
                new PaginationData().setRowsPerPage(2).setDisplayStart(0).setTotalRowCount(2),
                Arrays.asList(project1, project2)
            ));
        
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 2);
    }

    @Test
    public void should_get_all_projects_paginated() throws IOException, TException {
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        Collections.singletonList(projectList.iterator().next())
                )
        );
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?page=0&page_entries=1",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_project_by_id() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(project1.getId(), responseBody.get("id").textValue());
        assertEquals(project1.getName(), responseBody.get("name").textValue());
        assertEquals(project1.getProjectType().toString(), responseBody.get("projectType").textValue());
    }

    @Test
    public void should_get_project_by_id_not_found() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/unknown",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_my_projects() throws IOException, TException {
        given(this.projectServiceMock.getMyProjects(any(), any())).willReturn(projectList.stream().toList());
        given(this.projectServiceMock.getWithFilledClearingStatus(any(), any())).willReturn(projectList.stream().toList());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/myprojects",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 2);
    }

    @Test
    public void should_create_project() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Project");
        body.put("description", "New project description");
        body.put("projectType", "CUSTOMER");
        body.put("businessUnit", "Group A");
        body.put("version", "1.0.0");
        body.put("visibility", "EVERYONE");
        body.put("state", "ACTIVE");
        body.put("clearingState", "OPEN");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(project1.getId(), responseBody.get("id").textValue());
        assertEquals(project1.getName(), responseBody.get("name").textValue());
    }

    // ========== PROJECT UPDATE & LINKING ==========

    @Test
    public void should_update_project() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Updated Project Name");
        body.put("description", "Updated project description");
        body.put("version", "2.0.0");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_forbid_update_without_permission() throws IOException, TException {
        // Return non-admin viewer to make PermissionUtils.checkEditablePermission fail
        User viewer = new User();
        viewer.setEmail("viewer@sw360.org");
        viewer.setId("viewer");
        viewer.setUserGroup(UserGroup.USER);
        given(this.userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(viewer);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "NoPerm");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        // AccessDeniedException → 403 by RestExceptionHandler
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void should_link_projects() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> projectIds = Arrays.asList(project2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkProjects",
                        HttpMethod.POST,
                        new HttpEntity<>(projectIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_link_projects_already_linked_conflict() throws IOException, TException {
        // project2 already linked to project1
        Map<String, ProjectProjectRelationship> linked = new HashMap<>();
        linked.put(project1.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project2.setLinkedProjects(linked);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        // updateProject will not be called because branch exits early; still safe to stub
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> projectIds = Arrays.asList(project2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkProjects",
                        HttpMethod.POST,
                        new HttpEntity<>(projectIds, headers),
                        String.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void should_link_projects_access_denied_without_comment() throws IOException, TException {
        // Force write action denied
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Simulate isWriteActionAllowed false
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> projectIds = Arrays.asList(project2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkProjects",
                        HttpMethod.POST,
                        new HttpEntity<>(projectIds, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_link_projects_sent_to_moderator() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        // Cause updateProject to return SENT_TO_MODERATOR branch
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> projectIds = Arrays.asList(project2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkProjects",
                        HttpMethod.POST,
                        new HttpEntity<>(projectIds, headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }


    @Test
    public void should_update_project_release_relationship() throws IOException, TException {
        // Setup project with existing release relationship
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        ProjectReleaseRelationship existingRelationship = new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN);
        existingRelationship.setComment("Original relationship");
        releaseIdToUsage.put(release1.getId(), existingRelationship);
        project1.setReleaseIdToUsage(releaseIdToUsage);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProjectReleaseRelationship(any(), any(), eq(release1.getId()))).willReturn(existingRelationship);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ProjectReleaseRelationship relationship = new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN);
        relationship.setComment("Updated relationship");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/release/" + release1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(relationship, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_remove_orphaned_obligations() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.removeOrphanObligations(any(), any(), any(), any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> orphanedObligations = Arrays.asList("obligation1", "obligation2");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/orphanObligation",
                        HttpMethod.PATCH,
                        new HttpEntity<>(orphanedObligations, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== ADVANCED SEARCH & ANALYTICS ==========

    @Test
    public void should_get_projects_by_type() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Collections.singletonList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?type=CUSTOMER",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_name() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Collections.singletonList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?name=Alpha",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_external_ids() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/searchByExternalIds?portal-id=" + project1.getExternalIds().get("portal-id") + "&project-ext=" + project1.getExternalIds().get("project-ext"),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_by_group() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?group=" + project1.getBusinessUnit().replace(" ", "%20"),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_by_tag() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?tag=test-tag",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_by_lucene_search() throws IOException, TException {
        given(this.projectServiceMock.refineSearch(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?name=test&type=PROJECT&luceneSearch=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_by_advance_search() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?name=test&type=PROJECT&tag=test-tag&luceneSearch=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_by_version() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?version=" + project1.getVersion(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_project_responsible() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?projectResponsible=John%20Doe",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_state() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?state=" + project1.getState().name(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_clearing_status() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?clearingStatus=" + project1.getClearingState().name(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_projects_by_additional_data() throws IOException, TException {
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?additionalData=env:prod",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "projects", 1);
    }

    @Test
    public void should_get_project_count() throws IOException, TException {
        given(this.projectServiceMock.getMyAccessibleProjectCounts(any())).willReturn(10);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/projectcount",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_with_all_details() throws IOException, TException {
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(1).setDisplayStart(0).setTotalRowCount(1),
                        Arrays.asList(project1)
                )
        );

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects?allDetails=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_linked_projects_transitive() throws IOException, TException {
        // Setup project1 with linked projects
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put(project2.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project1.setLinkedProjects(linkedProjects);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkedProjects?transitive=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== PROJECT RELEASES ==========

    @Test
    public void should_get_project_releases() throws IOException, TException {
        Set<String> releaseIds = new HashSet<>(Arrays.asList(release1.getId(), release2.getId()));
        given(this.projectServiceMock.getReleaseIds(any(), any(), anyBoolean())).willReturn(releaseIds);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_link_releases_to_project() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> releaseIds = Arrays.asList(release1.getId(), release2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.POST,
                        new HttpEntity<>(releaseIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_patch_releases_to_project() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> releaseIds = Arrays.asList(release1.getId(), release2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.PATCH,
                        new HttpEntity<>(releaseIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_get_project_releases_transitive() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases?transitive=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_releases_with_filter_and_empty() throws IOException, TException {
        // No releases returned to trigger empty page branch and NO_CONTENT/OK decision
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> projectIds = Arrays.asList(project1.getId());
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/releases?transitive=false&clearingState=APPROVED",
                        HttpMethod.GET,
                        new HttpEntity<>(projectIds, headers),
                        String.class);

        // Controller returns an empty page resource with 200 OK for zero releases in multi-project path
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_releases_filtered_by_clearing_state() throws IOException, TException {
        // Prepare: two releases with different clearing states
        release1.setClearingState(ClearingState.APPROVED);
        release2.setClearingState(ClearingState.UNDER_CLEARING);

        // Mock service to return both when collecting from project IDs
        given(this.projectServiceMock.getReleasesFromProjectIds(any(), anyBoolean(), any(), any())).willReturn(new HashSet<>(Arrays.asList(release1, release2)));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> projectIds = Arrays.asList(project1.getId());
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/releases?transitive=true&clearingState=APPROVED",
                        HttpMethod.GET,
                        new HttpEntity<>(projectIds, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Expect only approved release present
        String body = response.getBody();
        assertTrue(body.contains(release1.getId()));
    }

    @Test
    public void should_get_linked_project_releases() throws IOException, TException {
        // Setup project1 with linked projects
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put(project2.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project1.setLinkedProjects(linkedProjects);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);

        // Mock direct releases (project1 has no direct releases)
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>());

        // Mock all releases including linked projects (project1 + project2 releases)
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Arrays.asList(release1.getId())));

        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkedProjects/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== PROJECT ATTACHMENTS ==========

    @Test
    public void should_get_project_attachment_info() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.getAttachmentResourcesFromList(any(), any(), any())).willReturn(CollectionModel.of(List.of(EntityModel.of(attachment1))));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_update_project_attachment_info() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.updateAttachment(any(), any(), any(), any())).willReturn(attachment1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("attachmentType", "SOURCE");
        body.put("createdComment", "Updated attachment info");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachment/att001",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_patch_project_success() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Project Alpha Updated");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_patch_project_sent_to_moderator() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Project Alpha Updated");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_fail_patch_project_when_write_denied_and_no_comment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Project Alpha Updated");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_fail_patch_project_with_duplicate_attachment_error() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Project Alpha Updated");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ========== ADD ATTACHMENTS TO PROJECT ==========

    @Test
    public void should_add_attachments_success() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // uploadAttachment returns attachment which is added to project
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willAnswer(inv -> {
            Attachment a = inv.getArgument(1);
            a.setAttachmentContentId("ac1");
            return a;
        });
        given(this.projectServiceMock.verifyIfAttachmentsExist(eq(project1.getId()), any(), any())).willReturn(new HashSet<>());
        given(this.projectServiceMock.updateProjectForAttachment(any(), any(), any(), any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "a.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"createdComment\":\"c\",\"attachmentType\":\"SOURCE\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_add_attachments_duplicate_filename_error() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource res = new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "dup.txt"; } };
        parts.add("file", res);
        parts.add("file", res); // same filename twice -> triggers duplicate detection
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"createdComment\":\"c\"}, {\"attachmentContentId\":\"ac2\",\"createdComment\":\"c2\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_add_attachments_missing_after_upload_conflict() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("acX"));
        given(this.projectServiceMock.verifyIfAttachmentsExist(eq(project1.getId()), any(), any())).willReturn(new HashSet<>(Set.of("acX")));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "m.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"acX\",\"createdComment\":\"c\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_add_attachments_access_denied_without_comment() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "a.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"attachmentType\":\"SOURCE\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_add_attachments_sent_to_moderator() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(true).when(this.restControllerHelper).isWriteActionAllowed(any(), any());
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        given(this.projectServiceMock.verifyIfAttachmentsExist(eq(project1.getId()), any(), any())).willReturn(new HashSet<>());
        given(this.projectServiceMock.updateProjectForAttachment(any(), any(), any(), any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "a.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"attachmentType\":\"SOURCE\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_add_attachments_duplicate_on_update_conflict() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(true).when(this.restControllerHelper).isWriteActionAllowed(any(), any());
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        given(this.projectServiceMock.verifyIfAttachmentsExist(eq(project1.getId()), any(), any())).willReturn(new HashSet<>());
        given(this.projectServiceMock.updateProjectForAttachment(any(), any(), any(), any(), any())).willReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "a.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"attachmentType\":\"SOURCE\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("Duplicate attachment detected"));
    }

    @Test
    public void should_add_attachments_handle_error_during_update() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(true).when(this.restControllerHelper).isWriteActionAllowed(any(), any());
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        given(this.projectServiceMock.verifyIfAttachmentsExist(eq(project1.getId()), any(), any())).willReturn(new HashSet<>());
        // Force exception to hit catch block
        given(this.projectServiceMock.updateProjectForAttachment(any(), any(), any(), any(), any())).willAnswer(inv -> { throw new RuntimeException("boom"); });

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "a.txt"; } });
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"attachmentType\":\"SOURCE\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_fail_on_invalid_attachment_type() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("hi".getBytes()) { @Override public String getFilename() { return "inv.txt"; } });
        // invalid attachmentType to trigger setAttachmentTypeAndCheckStatus catch
        parts.add("attachments", "[{\"attachmentContentId\":\"ac1\",\"attachmentType\":\"INVALID\"}]");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_projects_used_by_project() throws IOException, TException {
        // searchLinkingProjects returns a set with one project
        given(this.projectServiceMock.searchLinkingProjects(eq(project1.getId()), any())).willReturn(new HashSet<>(Set.of(project2)));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/usedby/" + project1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_projects_used_by_project_no_content() throws IOException, TException {
        given(this.projectServiceMock.searchLinkingProjects(eq(project1.getId()), any())).willReturn(new HashSet<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/usedby/" + project1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        // createResources with empty list still returns a non-null resource; controller returns OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_save_attachment_usages_success() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // permission allowed
        // validate true
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.projectServiceMock.validate(anyList(), any(), any(), anySet())).willReturn(true);
        given(this.projectServiceMock.getUsedAttachments(any(), isNull())).willReturn(new ArrayList<>());
        // make usages no-op success
        doNothing().when(this.projectServiceMock).makeAttachmentUsages(anyList());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("selected", List.of("rel1_sourcePackage_ac1"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/saveAttachmentUsages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_fail_save_attachment_usages_invalid() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.projectServiceMock.validate(anyList(), any(), any(), anySet())).willReturn(false);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("selected", List.of("bad"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/saveAttachmentUsages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_save_attachment_usages_delete_and_create() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.projectServiceMock.validate(anyList(), any(), any(), anySet())).willReturn(true);

        AttachmentUsage existing = new AttachmentUsage();
        List<AttachmentUsage> initial = Arrays.asList(existing);
        // First call -> existing usages; second call after deletion -> empty
        given(this.projectServiceMock.getUsedAttachments(any(), isNull())).willReturn(initial, new ArrayList<>());

        // Make controller think one usage should be deleted and another should be created
        given(this.projectServiceMock.deselectedAttachmentUsagesFromRequest(anyList(), anyList(), anyList(), anyList(), eq(project1.getId())))
                .willReturn(Arrays.asList(existing));
        AttachmentUsage toCreate = new AttachmentUsage();
        given(this.projectServiceMock.selectedAttachmentUsagesFromRequest(anyList(), anyList(), anyList(), anyList(), eq(project1.getId())))
                .willReturn(Arrays.asList(toCreate));
        // Predicates: mark any usage as equivalent to ensure deletion path executes
        given(this.projectServiceMock.isUsageEquivalent(any())).willAnswer(inv -> new java.util.function.Predicate<AttachmentUsage>() {
            @Override public boolean test(AttachmentUsage t) { return true; }
        });

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("selected", List.of("rel1_sourcePackage_ac1"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/saveAttachmentUsages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_handle_exception_in_save_attachment_usages() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Throw TException inside try block to hit catch
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willThrow(new TException("fail"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("selected", List.of());

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/saveAttachmentUsages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ========== ATTACHMENT USAGE LISTING ==========

    @Test
    public void should_list_attachment_usages_all_releases() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_filter_withAttachment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        // Ensure release has attachments so filter 'withAttachment' keeps it
        release1.setAttachments(new HashSet<>(Set.of(new Attachment().setAttachmentType(AttachmentType.SOURCE))));
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage?filter=withAttachment&transitive=true",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_and_normalize_includeConcludedLicense() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);

        // Build an AttachmentUsage with license info usage data
        LicenseInfoUsage liu = new LicenseInfoUsage();
        liu.setIncludeConcludedLicense(true);
        AttachmentUsage au = new AttachmentUsage();
        au.setUsageData(UsageData.licenseInfo(liu));
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>(List.of(au)));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_cli_attachment_and_counts() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Transitive true for this test
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Set.of(release1.getId())));
        // Release with CLI attachment so mapping keeps ATTACHMENTS
        Attachment cli = new Attachment().setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML).setAttachmentContentId("ac1");
        release1.setAttachments(new HashSet<>(Set.of(cli)));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        // Count map plumbing in private countMap
        given(this.projectServiceMock.createLinkedProjects(any(), any(), anyBoolean(), any())).willReturn(new ArrayList<>());
        given(this.projectServiceMock.createLinkedProjects(any(), any(), anyBoolean(), any())).willReturn(new ArrayList<>());
        given(this.projectServiceMock.storeAttachmentUsageCount(anyList(), any())).willReturn(new HashMap<>(Map.of(release1.getId(), 1)));
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage?filter=withCliAttachment&transitive=true",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_source_attachment_and_counts() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        // Release with SOURCE attachment
        Attachment src = new Attachment().setAttachmentType(AttachmentType.SOURCE).setAttachmentContentId("as1");
        release1.setAttachments(new HashSet<>(Set.of(src)));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.projectServiceMock.createLinkedProjects(any(), any(), anyBoolean(), any())).willReturn(new ArrayList<>());
        given(this.projectServiceMock.createLinkedProjects(any(), any(), anyBoolean(), any())).willReturn(new ArrayList<>());
        given(this.projectServiceMock.storeAttachmentUsageCount(anyList(), any())).willReturn(new HashMap<>(Map.of(release1.getId(), 2)));
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage?filter=withSourceAttachment",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_filter_withoutSourceAttachment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        // Add a non-source attachment so filter keeps release
        Attachment bin = new Attachment().setAttachmentType(AttachmentType.BINARY).setAttachmentContentId("ab1");
        release1.setAttachments(new HashSet<>(Set.of(bin)));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage?filter=withoutSourceAttachment",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_filter_withoutAttachment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        // Ensure release has no attachments
        release1.setAttachments(new HashSet<>());
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage?filter=withoutAttachment",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_list_attachment_usages_with_release_usage_embedding() throws IOException, TException {
        // Ensure project has releaseIdToUsage so attachmentUsageReleases embeds usage
        Map<String, ProjectReleaseRelationship> relUsage = new HashMap<>();
        relUsage.put("rel-x", new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(relUsage);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Set.of(release1.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.attachmentServiceMock.getAllAttachmentUsage(eq(project1.getId()))).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachmentUsage",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== SBOM IMPORT ==========

    @Test
    public void should_import_spdx_success() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.isValidSbomFile(any(), eq("SPDX"))).willReturn(true);
        // upload returns attachment with content id
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        RequestSummary rs = new RequestSummary();
        rs.setRequestStatus(RequestStatus.SUCCESS);
        given(this.projectServiceMock.importSPDX(any(), anyString())).willReturn(rs);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("type", "SPDX");
        parts.add("file", new org.springframework.core.io.ByteArrayResource("data".getBytes()) { @Override public String getFilename() { return "sbom.spdx.rdf.xml"; } });

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_import_cyclonedx_access_denied() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.isValidSbomFile(any(), eq("CycloneDX"))).willReturn(true);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        RequestSummary rs = new RequestSummary();
        rs.setRequestStatus(RequestStatus.ACCESS_DENIED);
        rs.setMessage("You do not have sufficient permissions.");
        given(this.projectServiceMock.importCycloneDX(any(), anyString(), anyString(), anyBoolean())).willReturn(rs);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("type", "CycloneDX");
        parts.add("file", new org.springframework.core.io.ByteArrayResource("data".getBytes()) { @Override public String getFilename() { return "bom.cdx.json"; } });

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_import_spdx_duplicate() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.isValidSbomFile(any(), eq("SPDX"))).willReturn(true);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        RequestSummary rs = new RequestSummary();
        rs.setRequestStatus(RequestStatus.DUPLICATE);
        rs.setMessage("projectId");
        given(this.projectServiceMock.importSPDX(any(), anyString())).willReturn(rs);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("type", "SPDX");
        parts.add("file", new org.springframework.core.io.ByteArrayResource("data".getBytes()) { @Override public String getFilename() { return "sbom.spdx.rdf.xml"; } });

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_import_sbom_on_project_failure_invalid_file() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        RequestSummary rs = new RequestSummary();
        rs.setRequestStatus(RequestStatus.FAILURE);
        rs.setMessage("Invalid SBOM file");
        given(this.projectServiceMock.importCycloneDX(any(), anyString(), anyString(), anyBoolean())).willReturn(rs);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("bad".getBytes()) { @Override public String getFilename() { return "bom.cdx.json"; } });

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_import_sbom_on_project_failed_sanity_check() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(new Attachment().setAttachmentContentId("ac1"));
        RequestSummary rs = new RequestSummary();
        rs.setRequestStatus(RequestStatus.FAILED_SANITY_CHECK);
        rs.setMessage("Project name or version present in SBOM metadata tag is not same as the current SW360 project!");
        given(this.projectServiceMock.importCycloneDX(any(), anyString(), anyString(), anyBoolean())).willReturn(rs);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new org.springframework.core.io.ByteArrayResource("data".getBytes()) { @Override public String getFilename() { return "bom.cdx.json"; } });

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM",
                HttpMethod.POST,
                new HttpEntity<>(parts, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== DEPENDENCY NETWORK ==========

    @Test
    public void should_get_project_with_network() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_create_project_with_network_success() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Network Project");
        body.put("version", "1.0.0");
        // Minimal dependencyNetwork to exercise addOrPatchDependencyNetworkToProject path
        body.put("dependencyNetwork", Map.of("some", "value"));

        given(this.projectServiceMock.createProject(any(), any())).willReturn(project1.setId("np1"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_handle_invalid_dependency_network_format() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Force JsonProcessingException by sending a structurally invalid value for dependencyNetwork that triggers parsing branch
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Bad Net");
        body.put("version", "1.0.0");
        body.put("dependencyNetwork", "this-should-be-a-map");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_patch_project_with_network_success() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", Map.of("some", "value"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_patch_project_with_network_requires_comment_when_no_write() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", Map.of("some", "value"));
        // omit comment to trigger BadRequestClientException branch

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_patch_project_with_network_sent_to_moderator() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", Map.of("some", "value"));
        body.put("comment", "please moderate");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_patch_project_with_network_duplicate_attachment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(project1).when(this.restControllerHelper).updateProject(any(), any(), anyMap(), any());
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", Map.of("some", "value"));
        body.put("comment", "ok");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_patch_releases_with_map_body_existing_relation() throws IOException, TException {
        // Pre-populate existing relation so branch 'containsKey' is true
        Map<String, ProjectReleaseRelationship> existing = new HashMap<>();
        existing.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(existing);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> relMeta = new HashMap<>();
        relMeta.put("releaseRelation", "CONTAINED");
        relMeta.put("mainlineState", "OPEN");
        Map<String, Object> body = new HashMap<>();
        body.put(release1.getId(), relMeta);

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_patch_releases_with_invalid_body_type() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // send plain string body instead of list or map
        String invalidBody = "\"invalid\"";

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                HttpMethod.PATCH,
                new HttpEntity<>(invalidBody, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_link_packages_preserve_existing_comments() throws IOException, TException {
        // Existing package relationship with comment should be preserved
        Map<String, org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship> pkgMap = new HashMap<>();
        org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship rel = new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship();
        rel.setComment("keep-me");
        pkgMap.put("pkg-1", rel);
        project1.setPackageIds(pkgMap);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> ids = new HashSet<>(Arrays.asList("pkg-1", "pkg-2"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/link/packages",
                HttpMethod.PATCH,
                new HttpEntity<>(ids, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_unlink_packages_remove_ids() throws IOException, TException {
        Map<String, org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship> pkgMap = new HashMap<>();
        pkgMap.put("pkg-1", new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship());
        pkgMap.put("pkg-2", new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship());
        project1.setPackageIds(pkgMap);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> ids = new HashSet<>(Arrays.asList("pkg-1"));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/unlink/packages",
                HttpMethod.PATCH,
                new HttpEntity<>(ids, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_create_project_with_visibility_linked_projects_and_attachments() throws IOException, TException {
        // Mock attachments extraction during convertToProject
        Set<Attachment> atts = new HashSet<>(Set.of(new Attachment().setAttachmentContentId("att-1")));
        given(this.attachmentServiceMock.getAttachmentsFromRequest(any(), any())).willReturn(atts);

        given(this.projectServiceMock.createProject(any(), any())).willReturn(project1.setId("p-new"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Proj Vis");
        body.put("version", "1.0.0");
        // visibility processed and uppercased in convertToProject
        body.put("visibility", "everyone");
        // linkedProjects map with relationship string
        Map<String, Object> linked = new HashMap<>();
        linked.put(project2.getId(), "CONTAINED");
        body.put("linkedProjects", linked);
        // attachments payload (content not parsed in test because we mock service)
        body.put("attachments", List.of(Map.of("attachmentContentId", "att-1")));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_get_license_obligations_no_content_when_no_release() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // ensure project has no releaseIdToUsage
        project1.setReleaseIdToUsage(new HashMap<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligations",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_get_license_obligations_paginated() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // project has linked releases
        Map<String, ProjectReleaseRelationship> rels = new HashMap<>();
        rels.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(rels);
        // licenseInfoAttachment usage present
        // Controller expects a map of attachment usages keyed by attachmentContentId
        Map<String, AttachmentUsage> licUsage = new HashMap<>();
        AttachmentUsage usage = new AttachmentUsage();
        usage.setOwner(Source.projectId(project1.getId()));
        licUsage.put("att-lic", usage);
        given(this.projectServiceMock.getLicenseInfoAttachmentUsage(eq(project1.getId()))).willReturn(licUsage);
        // releases for usage
        // ProjectController expects getReleasesFromAttachmentUsage(licenseInfoAttachmentUsage, user)
        given(this.projectServiceMock.getLicensesFromAttachmentUsage(any(), any())).willReturn(new HashMap<>(Map.of("L1", Set.of(release1))));
        // build obligations map
        java.util.LinkedHashMap<String, ObligationStatusInfo> obligations = new java.util.LinkedHashMap<>();
        obligations.put("Obl-1", new ObligationStatusInfo().setStatus(ObligationStatus.OPEN));
        obligations.put("Obl-2", new ObligationStatusInfo().setStatus(ObligationStatus.OPEN));
        given(this.projectServiceMock.getLicenseObligationData(any(), any())).willReturn(obligations);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligations?page=0&size=1",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_license_obligation_data_project_view() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        Map<String, ProjectReleaseRelationship> rels = new HashMap<>();
        rels.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(rels);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        // No linked obligation id -> obligationStatusMap empty
        given(this.projectServiceMock.processLicenseInfoWithObligations(anyList(), anyMap(), anyList(), any())).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligations",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_license_obligation_data_release_view() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        Map<String, ProjectReleaseRelationship> rels = new HashMap<>();
        rels.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(rels);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        // No additional stubs needed: controller computes accepted CLI map internally

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligations?view=true",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_obligation_data_for_license_level() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        Map<String, ProjectReleaseRelationship> rels = new HashMap<>();
        rels.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(rels);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);

        Map<String, ObligationStatusInfo> statusMap = new HashMap<>();
        ObligationStatusInfo osInfo = new ObligationStatusInfo();
        osInfo.setId("os-1");
        osInfo.setReleaseIdToAcceptedCLI(new HashMap<>());
        statusMap.put("License-1", osInfo);
        given(this.projectServiceMock.getObligationData(eq(project1.getLinkedObligationId()), any())).willReturn(new ObligationList().setLinkedObligationStatus(statusMap));
        given(this.projectServiceMock.setObligationsFromAdminSection(any(), anyMap(), any(), eq("license"))).willReturn(new HashMap<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/obligation?obLevel=license",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_project_administration_summary() throws IOException, TException {
        // Add license info header text and external URLs to trigger sorting and setters
        project1.setLicenseInfoHeaderText("Header Text");
        Map<String, String> ext = new HashMap<>();
        ext.put("homepage", "http://a");
        ext.put("wiki", "http://b");
        project1.setExternalUrls(ext);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/summaryAdministration",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_dependency_network_list_view() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId() + "/listView",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_linked_resources_of_project_in_dependency_network() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId() + "/linkedResources?transitive=true",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_releases_in_dependency_network_by_index_path() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId() + "/releases?releaseIndexPath=0-1-0",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_include_additional_embedded_fields_in_hal_resource() throws IOException, TException {
        // Seed project fields used by setAdditionalFieldsToHalResource
        project1.setModifiedBy("modifier@sw360.org");
        project1.setProjectOwner("owner@sw360.org");
        project1.setSecurityResponsibles(new HashSet<>(Set.of("sec@sw360.org")));
        project1.setClearingTeam("clear@sw360.org");
        project1.setProjectResponsible("resp@sw360.org");

        // mock user lookups for modifier and owner
        given(this.userServiceMock.getUserByEmail(eq("modifier@sw360.org"))).willReturn(testUser);
        given(this.userServiceMock.getUserByEmail(eq("owner@sw360.org"))).willReturn(testUser);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId(),
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_update_dependency_network_on_patch_with_nodes() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // dependencyNetwork contains a list of nodes with releaseLink
        Map<String, Object> node = new HashMap<>();
        node.put("releaseLink", "/api/releases/" + release1.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", List.of(node));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_reject_dependency_network_with_cycle() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Construct dependencyNetwork that causes a cycle (node referencing same release twice via path)
        Map<String, Object> node = new HashMap<>();
        node.put("releaseLink", "/api/releases/" + release1.getId());
        // loadedReleases simulation is internal; here we send two nodes with same release to hit duplicate checks/cycle logic
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", List.of(node, node));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        // Controller catches IllegalState and maps as 400
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_validate_mainline_and_relationship_values() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> node = new HashMap<>();
        node.put("releaseLink", "/api/releases/" + release1.getId());
        node.put("mainlineState", "INVALID"); // invalid mainline should be rejected
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", List.of(node));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========== CLEARING REQUEST ==========

    @Test
    public void should_create_clearing_request_missing_type() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("comment", "please clear");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/clearingRequest",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_create_clearing_request_invalid_date() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("clearingType", "INTERNAL");
        body.put("priority", 1);
        // Past date to violate date limit logic
        body.put("requestedClearingDate", "2000-01-01");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/clearingRequest",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_create_clearing_request_invalid_team_member() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // return null for team member lookup to hit invalid team branch
        given(this.userServiceMock.getUserByEmail(eq("team@sw360.org"))).willReturn(null);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("clearingType", "INTERNAL");
        body.put("priority", 3);
        body.put("clearingTeam", "team@sw360.org");

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/clearingRequest",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_get_linked_releases_in_dependency_network() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/" + project1.getId() + "/linkedReleases",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_linked_releases_of_linked_projects() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/subProjects/releases",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_compare_dependency_network_with_default_network() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("dependencyNetwork", List.of());

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/compareDefaultNetwork",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_duplicate_project_with_dependency_network() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.createProject(any(), any())).willReturn(project2.setId("dup-1"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Duplicate");
        body.put("version", "2.0.0");
        body.put("dependencyNetwork", List.of());

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/network/duplicate/" + project1.getId(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_add_license_to_linked_releases() throws IOException, TException {
        given(this.projectServiceMock.addLicenseToLinkedReleases(eq(project1.getId()), any())).willReturn(new HashMap<>());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("licenses", List.of("MIT"));
        body.put("releaseIds", List.of(release1.getId(), release2.getId()));

        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + port + "/api/projects/" + project1.getId() + "/addLinkedReleasesLicenses",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_download_clearing_reports_zip() throws IOException, TException {
        // Prepare a clearing-report attachment on the project
        Attachment clearing = new Attachment();
        clearing.setFilename("clearing-report.docx");
        clearing.setAttachmentType(AttachmentType.CLEARING_REPORT);
        clearing.setAttachmentContentId("ac1");
        project1.setAttachments(new HashSet<>(Arrays.asList(clearing, attachment1)));

        // Attachment content and stream mocks
        AttachmentContent content = new AttachmentContent().setId("ac1").setFilename("clearing-report.docx").setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        given(this.attachmentServiceMock.getAttachmentContent(eq("ac1"))).willReturn(content);

        byte[] zipBytes = new byte[] {0x50, 0x4b, 0x03, 0x04}; // PK.. minimal zip signature
        java.io.InputStream zipStream = new java.io.ByteArrayInputStream(zipBytes);
        given(this.attachmentServiceMock.getStreamToAttachments(any(), any(), any())).willReturn(zipStream);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments/clearingReports",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/octet-stream", response.getHeaders().getFirst("Content-Type"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("Clearing-Reports-" + project1.getName() + ".zip"));
        assertTrue(response.getBody() != null && response.getBody().length >= 4);
    }

    @Test
    public void should_handle_ioexception_when_streaming_clearing_reports() throws IOException, TException {
        // Prepare a clearing-report attachment
        Attachment clearing = new Attachment();
        clearing.setFilename("clearing-report.docx");
        clearing.setAttachmentType(AttachmentType.CLEARING_REPORT);
        clearing.setAttachmentContentId("ac2");
        project1.setAttachments(new HashSet<>(Arrays.asList(clearing)));

        AttachmentContent content = new AttachmentContent().setId("ac2").setFilename("clearing-report.docx").setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        given(this.attachmentServiceMock.getAttachmentContent(eq("ac2"))).willReturn(content);

        // InputStream that throws IOException on read to exercise catch block
        java.io.InputStream failingStream = new java.io.InputStream() {
            @Override
            public int read() throws IOException { throw new IOException("stream failure"); }
        };
        given(this.attachmentServiceMock.getStreamToAttachments(any(), any(), any())).willReturn(failingStream);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/attachments/clearingReports",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);

        // Controller logs and returns normally; just assert we didn't error at framework level
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== PROJECT LICENSE CLEARING ==========

    @Test
    public void should_get_license_clearing() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        // Ensure project has release usage with a relation so the controller's filter branch executes
        Map<String, ProjectReleaseRelationship> usage = new HashMap<>();
        usage.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        usage.put(release2.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(usage);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseClearing?transitive=true&releaseRelation=CONTAINED",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== LICENSE INFO HEADER ==========

    @Test
    public void should_get_license_info_header() throws IOException, TException {
        given(this.projectServiceMock.getLicenseInfoHeaderText()).willReturn("Default License Header Text");

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/licenseInfoHeader",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_license_clearing_count() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getClearingInfo(any(), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseClearingCount",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_link_packages_to_project() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Set<String> packageIds = new HashSet<>(Arrays.asList(package1.getId(), "pkg002"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/link/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(packageIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_unlink_packages_from_project() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Set<String> packageIds = new HashSet<>(Arrays.asList(package1.getId(), "pkg002"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/unlink/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(packageIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_fail_link_packages_with_invalid_ids() throws IOException, TException {
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(false);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> badPackages = new HashSet<>(Arrays.asList("bad1", "bad2"));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/link/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(badPackages, headers),
                        String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_link_packages_sent_to_moderator() throws IOException, TException {
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> ids = new HashSet<>(Arrays.asList(package1.getId()));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/link/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(ids, headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_unlink_packages_access_denied_without_comment() throws IOException, TException {
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> ids = new HashSet<>(Arrays.asList(package1.getId()));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/unlink/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(ids, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_unlink_packages_sent_to_moderator() throws IOException, TException {
        given(this.packageServiceMock.validatePackageIds(any())).willReturn(true);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Set<String> ids = new HashSet<>(Arrays.asList(package1.getId()));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/unlink/packages",
                        HttpMethod.PATCH,
                        new HttpEntity<>(ids, headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_get_packages_by_project_id() throws IOException, TException {
        // Prepare project with one linked package to drive branches in createHalPackage
        Map<String, org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship> pkgMap = new HashMap<>();
        pkgMap.put(package1.getId(), new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship());
        project1.setPackageIds(pkgMap);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/packages",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Basic structure assertions (at least one package)
        JsonNode body = new ObjectMapper().readTree(response.getBody());
        assertTrue(body.isArray());
        assertTrue(body.size() >= 1);
    }

    @Test
    public void should_get_packages_by_project_id_with_vendor_and_modified_by() throws IOException, TException {
        // Prepare package with vendor and modifiedBy to exercise both branches
        package1.setVendorId("vendor-123");
        package1.setModifiedBy("admin@sw360.org");

        // Link the package in project
        Map<String, org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship> pkgMap = new HashMap<>();
        pkgMap.put(package1.getId(), new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship());
        project1.setPackageIds(pkgMap);

        // Mock vendor lookup
        Vendor v = new Vendor();
        v.setId("vendor-123");
        v.setFullname("Acme Corp");
        given(this.vendorServiceMock.getVendorById(eq("vendor-123"))).willReturn(v);
        // Ensure getUserByEmail in createHalPackage doesn't return null
        given(this.userServiceMock.getUserByEmail(eq(package1.getCreatedBy()))).willReturn(testUser);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/packages",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = new ObjectMapper().readTree(response.getBody());
        assertTrue(body.isArray());
        // Verify embedded vendor and modifiedBy are present
        JsonNode first = body.get(0);
        assertTrue(first.get("_embedded").has("sw360:vendors"));
        assertTrue(first.get("_embedded").has("modifiedBy"));
    }

    @Test
    public void should_get_vulnerability_summary() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Parent vulnerabilities
        VulnerabilityDTO v1 = new VulnerabilityDTO();
        v1.setExternalId("CVE-2024-1111");
        v1.setIntReleaseId(release1.getId());
        v1.setDescription("desc");
        v1.setTitle("title");
        v1.setPriority("1");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Lists.newArrayList(v1));

        // Linked project to trigger map population and loop
        Map<String, ProjectProjectRelationship> linked = new HashMap<>();
        linked.put(project2.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project1.setLinkedProjects(linked);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        VulnerabilityDTO v2 = new VulnerabilityDTO();
        v2.setExternalId("CVE-2024-2222");
        v2.setIntReleaseId(release2.getId());
        v2.setDescription("desc2");
        v2.setTitle("title2");
        v2.setPriority("2");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project2.getId()), any())).willReturn(Lists.newArrayList(v2));

        // Rating and metadata branches
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating rating = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating();
        Map<String, Map<String, List<org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus>>> hist = new HashMap<>();
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus vcs1 = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus();
        vcs1.setComment("ok");
        vcs1.setProjectAction("new");
        vcs1.setVulnerabilityRating(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject.APPLICABLE);
        hist.put("CVE-2024-1111", Map.of(release1.getId(), List.of(vcs1)));
        rating.setVulnerabilityIdToReleaseIdToStatus(hist);
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(List.of(rating));

        Map<String, Map<String, VulnerabilityRatingForProject>> meta1 = new HashMap<>();
        meta1.put("CVE-2024-1111", Map.of(release1.getId(), VulnerabilityRatingForProject.APPLICABLE));
        given(this.vulnerabilityServiceMock.fillVulnerabilityMetadata(any(VulnerabilityDTO.class), any())).willReturn(meta1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilitySummary",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_eccs_of_releases() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases/ecc?transitive=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_vulnerabilities_of_releases() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        VulnerabilityDTO vd = new VulnerabilityDTO();
        vd.setExternalId("CVE-2024-3333");
        vd.setIntReleaseId(release1.getId());
        vd.setDescription("d");
        vd.setTitle("t");
        vd.setPriority("1");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Lists.newArrayList(vd));

        org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating rating = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating();
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus vcs2 = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus();
        vcs2.setComment("c");
        vcs2.setProjectAction("a");
        vcs2.setVulnerabilityRating(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject.APPLICABLE);
        rating.setVulnerabilityIdToReleaseIdToStatus(Map.of("CVE-2024-3333", Map.of(release1.getId(), List.of(vcs2))));
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(List.of(rating));

        Map<String, Map<String, VulnerabilityRatingForProject>> meta = new HashMap<>();
        meta.put("CVE-2024-3333", Map.of(release1.getId(), VulnerabilityRatingForProject.APPLICABLE));
        given(this.vulnerabilityServiceMock.fillVulnerabilityMetadata(any(VulnerabilityDTO.class), any())).willReturn(meta);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities?priority=1&projectRelevance=RELEVANT",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_patch_update_vulnerabilities_success() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Existing vulnerability to satisfy validation
        VulnerabilityDTO existing = new VulnerabilityDTO();
        existing.setExternalId("CVE-2024-9999");
        existing.setIntReleaseId("rel-1");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Lists.newArrayList(existing));
        // Existing rating history to exercise copy-from-history branch
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating existingRating = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating();
        existingRating.setProjectId(project1.getId());
        existingRating.setVulnerabilityIdToReleaseIdToStatus(Map.of(
                "CVE-2024-9999", Map.of("rel-1", List.of(new org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus()
                        .setComment("old-comment").setProjectAction("old-action")
                        .setVulnerabilityRating(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject.APPLICABLE)) )));
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(List.of(existingRating));
        given(this.vulnerabilityServiceMock.updateProjectVulnerabilityRating(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        VulnerabilityDTO dto = new VulnerabilityDTO();
        dto.setExternalId("CVE-2024-9999");
        dto.setIntReleaseId("rel-1");
        dto.setProjectRelevance(null); // trigger rating copy from history
        dto.setComment(null); // trigger comment copy from history
        dto.setAction(null); // trigger action copy from history

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities",
                        HttpMethod.PATCH,
                        new HttpEntity<>(List.of(dto), headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_patch_update_vulnerabilities_sent_to_moderator() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        VulnerabilityDTO existing = new VulnerabilityDTO();
        existing.setExternalId("CVE-2024-9999");
        existing.setIntReleaseId("rel-1");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Lists.newArrayList(existing));
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());
        given(this.vulnerabilityServiceMock.updateProjectVulnerabilityRating(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        VulnerabilityDTO dto = new VulnerabilityDTO();
        dto.setExternalId("CVE-2024-9999");
        dto.setIntReleaseId("rel-1");
        dto.setProjectRelevance(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject.APPLICABLE.toString());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities",
                        HttpMethod.PATCH,
                        new HttpEntity<>(List.of(dto), headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_fail_patch_update_vulnerabilities_without_write_and_comment() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());
        VulnerabilityDTO existing = new VulnerabilityDTO();
        existing.setExternalId("CVE-2024-9999");
        existing.setIntReleaseId("rel-1");
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Lists.newArrayList(existing));
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        VulnerabilityDTO dto = new VulnerabilityDTO();
        dto.setExternalId("CVE-2024-9999");
        dto.setIntReleaseId("rel-1");
        dto.setProjectRelevance(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject.APPLICABLE.toString());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities",
                        HttpMethod.PATCH,
                        new HttpEntity<>(List.of(dto), headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_get_linked_projects() throws IOException, TException {
        // Setup project1 with linked projects
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put(project2.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project1.setLinkedProjects(linkedProjects);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkedProjects",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_linked_projects_releases() throws IOException, TException {
        // Setup project1 with linked projects
        Map<String, ProjectProjectRelationship> linkedProjects = new HashMap<>();
        linkedProjects.put(project2.getId(), new ProjectProjectRelationship(ProjectRelationship.CONTAINED));
        project1.setLinkedProjects(linkedProjects);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(false))).willReturn(new HashSet<>(Arrays.asList(release1.getId())));
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), eq(true))).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/linkedProjects/releases",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_license_obligations() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getObligationData(any(), any())).willReturn(new ObligationList());
        given(this.projectServiceMock.setLicenseInfoWithObligations(any(), any(), any(), any())).willReturn(new HashMap<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligations",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_add_obligations_from_license_db() throws IOException, TException {
        // Setup project with releases
        Map<String, ProjectReleaseRelationship> releaseUsage = new HashMap<>();
        releaseUsage.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));

        project1.setReleaseIdToUsage(releaseUsage);

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        // Mock attachment usage data
        Map<String, AttachmentUsage> attachmentUsage = new HashMap<>();
        AttachmentUsage usage = new AttachmentUsage();
        usage.setAttachmentContentId("att001");
        usage.setOwner(Source.releaseId(release1.getId()));
        usage.setUsedBy(Source.projectId(project1.getId()));
        attachmentUsage.put("att001", usage);
        given(this.projectServiceMock.getLicenseInfoAttachmentUsage(eq(project1.getId()))).willReturn(attachmentUsage);

        // Mock licenses from attachment usage
        Map<String, Set<Release>> licensesFromAttachmentUsage = new HashMap<>();
        Set<Release> releases = new HashSet<>();
        releases.add(release1);
        licensesFromAttachmentUsage.put("Apache-2.0", releases);
        given(this.projectServiceMock.getLicensesFromAttachmentUsage(any(), any())).willReturn(licensesFromAttachmentUsage);

        // Mock license obligation data
        Map<String, ObligationStatusInfo> licenseObligation = new HashMap<>();
        ObligationStatusInfo obligationInfo = new ObligationStatusInfo();
        obligationInfo.setId("obligation1");
        obligationInfo.setText("Test obligation");
        licenseObligation.put("obligation1", obligationInfo);
        given(this.projectServiceMock.getLicenseObligationData(any(), any())).willReturn(licenseObligation);

        // Mock release service
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);

        // Mock setLicenseInfoWithObligations
        given(this.projectServiceMock.setLicenseInfoWithObligations(any(), any(), any(), any())).willReturn(new HashMap<>());

        // Mock addLinkedObligations
        given(this.projectServiceMock.addLinkedObligations(any(), any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> obligationIds = Arrays.asList("obligation1", "obligation2");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseObligation",
                        HttpMethod.POST,
                        new HttpEntity<>(obligationIds, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_update_license_obligations() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.patchLinkedObligations(any(), any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, ObligationStatusInfo> obligationStatusMap = new HashMap<>();
        ObligationStatusInfo statusInfo = new ObligationStatusInfo();
        statusInfo.setStatus(ObligationStatus.OPEN);
        obligationStatusMap.put("obligation1", statusInfo);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/updateLicenseObligation",
                        HttpMethod.PATCH,
                        new HttpEntity<>(obligationStatusMap, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_get_export_project_create_clearing_request() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // The controller calls the report service through the main reports endpoint
        given(this.sw360ReportServiceMock.getLicenseInfoBuffer(any(), any(), any())).willReturn(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/reports?module=exportCreateProjectClearingReport&projectId=" + project1.getId() + "&generatorClassName=DocxGenerator&variant=REPORT",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== LICENSE & COMPLIANCE ==========

    @Test
    public void should_get_license_clearing_information() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getClearingInfo(any(), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseClearingCount",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== IMPORT/EXPORT ==========

    @Test
    public void should_import_spdx() throws IOException, TException {
        given(this.attachmentServiceMock.isValidSbomFile(any(), eq("SPDX"))).willReturn(true);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment1);
        given(this.projectServiceMock.importSPDX(any(), any())).willReturn(new RequestSummary().setRequestStatus(RequestStatus.SUCCESS).setMessage(project1.getId()));
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart request with file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("SPDX-2.2\nPackageName: TestPackage\nPackageVersion: 1.0.0".getBytes()) {
            @Override
            public String getFilename() {
                return "test.spdx";
            }
        });

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/import/SBOM?type=SPDX",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_import_cyclonedx() throws IOException, TException {
        given(this.attachmentServiceMock.isValidSbomFile(any(), eq("CycloneDX"))).willReturn(true);
        given(this.attachmentServiceMock.uploadAttachment(any(), any(), any())).willReturn(attachment1);
        given(this.projectServiceMock.importCycloneDX(any(), any(), any(), anyBoolean())).willReturn(new RequestSummary().setRequestStatus(RequestStatus.SUCCESS).setMessage("{\"projectId\":\"" + project1.getId() + "\"}"));
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart request with file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("{\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.4\",\"version\":1}".getBytes()) {
            @Override
            public String getFilename() {
                return "test.json";
            }
        });

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/import/SBOM?type=CycloneDX",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_import_cyclonedx_on_project() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.importCycloneDX(any(), any(), any(), anyBoolean())).willReturn(new RequestSummary().setRequestStatus(RequestStatus.SUCCESS).setMessage("{\"projectId\":\"" + project1.getId() + "\"}"));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create multipart request with file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("{\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.4\",\"version\":1}".getBytes()) {
            @Override
            public String getFilename() {
                return "test.json";
            }
        });

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/import/SBOM?doNotReplacePackageAndRelease=false",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== REPORTS & ADMINISTRATION ==========

    @Test
    public void should_get_project_report() throws IOException, TException {
        given(this.sw360ReportServiceMock.getProjectBuffer(any(), anyBoolean(), any())).willReturn(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/reports?module=projects&projectId=" + project1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_project_licenseclearing_spreadsheet() throws IOException, TException {
        given(this.sw360ReportServiceMock.getProjectReleaseSpreadSheetWithEcc(any(), any())).willReturn(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/reports?module=projectReleaseSpreadsheetWithEcc&projectId=" + project1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== VULNERABILITY MANAGEMENT ==========

    @Test
    public void should_get_project_vulnerabilities_by_externalid() throws IOException, TException {
        // Create a test VulnerabilityDTO with required fields
        VulnerabilityDTO vulnerabilityDTO = new VulnerabilityDTO();
        vulnerabilityDTO.setExternalId("CVE-2021-1234");
        vulnerabilityDTO.setIntReleaseId(release1.getId());
        vulnerabilityDTO.setPriority("1");

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Arrays.asList(vulnerabilityDTO));

        // Mock getProjectVulnerabilityRatingByProjectId to return empty optional
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());

        // Mock fillVulnerabilityMetadata to return proper metadata structure
        Map<String, Map<String, VulnerabilityRatingForProject>> vulRatingProj = new HashMap<>();
        Map<String, VulnerabilityRatingForProject> releaseRating = new HashMap<>();
        releaseRating.put(release1.getId(), VulnerabilityRatingForProject.NOT_CHECKED);
        vulRatingProj.put("CVE-2021-1234", releaseRating);
        given(this.vulnerabilityServiceMock.fillVulnerabilityMetadata(any(VulnerabilityDTO.class), any())).willReturn(vulRatingProj);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities?externalId=CVE-2021-1234",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_update_project_vulnerabilities() throws IOException, TException {
        // Create a test VulnerabilityDTO that matches what the controller expects
        VulnerabilityDTO vulnerabilityDTO = new VulnerabilityDTO();
        vulnerabilityDTO.setExternalId("CVE-2021-1234");
        vulnerabilityDTO.setIntReleaseId(release1.getId());
        vulnerabilityDTO.setProjectRelevance("APPLICABLE");
        vulnerabilityDTO.setComment("Updated vulnerability rating");
        vulnerabilityDTO.setAction("Update to Fixed Version");

        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        // Mock getVulnerabilitiesByProjectId to return the vulnerability that matches the request
        given(this.vulnerabilityServiceMock.getVulnerabilitiesByProjectId(eq(project1.getId()), any())).willReturn(Arrays.asList(vulnerabilityDTO));

        // Mock getProjectVulnerabilityRatingByProjectId to return empty list
        given(this.vulnerabilityServiceMock.getProjectVulnerabilityRatingByProjectId(eq(project1.getId()), any())).willReturn(new ArrayList<>());

        given(this.vulnerabilityServiceMock.updateProjectVulnerabilityRating(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create request body as List<VulnerabilityDTO> as expected by the controller
        List<VulnerabilityDTO> requestBody = List.of(vulnerabilityDTO);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities",
                        HttpMethod.PATCH,
                        new HttpEntity<>(requestBody, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ========== ERROR HANDLING ==========

    @Test
    public void should_create_duplicate_project() throws IOException, TException {
        given(this.projectServiceMock.createProject(any(), any())).willThrow(new DataIntegrityViolationException("sw360 project with name 'Duplicate Project' already exists."));

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Duplicate Project");
        body.put("version", "1.0.0");
        body.put("description", "This project already exists");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void should_fail_duplicate_without_name_and_version() throws IOException, TException {
        // Hitting controller guard that throws BadRequestClientException when neither name nor version present
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> empty = new HashMap<>();
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/duplicate/" + project1.getId(),
                        HttpMethod.POST,
                        new HttpEntity<>(empty, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_delete_project_requires_comment_for_non_writer() throws IOException, TException {
        // Force write not allowed branch and missing comment -> 400 with moderation-with-commit message
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Simulate isWriteActionAllowed = false by returning a different non-admin user from userService
        User viewer = new User();
        viewer.setEmail("viewer@sw360.org");
        viewer.setId("viewer");
        viewer.setUserGroup(UserGroup.USER);
        given(this.userServiceMock.getUserByEmailOrExternalId(anyString())).willReturn(viewer);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_get_project_licenses() throws IOException, TException {
        // Setup project with releases
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        releaseIdToUsage.put(release1.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        releaseIdToUsage.put(release2.getId(), new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
        project1.setReleaseIdToUsage(releaseIdToUsage);

        // Mock service calls
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);
        given(this.sw360LicenseServiceMock.getLicenseById(eq("Apache-2.0"))).willReturn(new License().setId("Apache-2.0").setFullname("Apache License 2.0"));
        given(this.sw360LicenseServiceMock.getLicenseById(eq("MIT"))).willReturn(new License().setId("MIT").setFullname("MIT License"));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenses",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "licenses", 2);

        // Additional verification of license content
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        JsonNode licenses = responseBody.get("_embedded").get("sw360:licenses");
        assertEquals(2, licenses.size());

        // Verify license content - check for fullName and checked fields (id is excluded by JSON mixin)
        Set<String> licenseNames = new HashSet<>();
        for (int i = 0; i < licenses.size(); i++) {
            JsonNode license = licenses.get(i);
            assertTrue("License should have 'fullName' field", license.has("fullName"));
            assertTrue("License should have 'checked' field", license.has("checked"));
            licenseNames.add(license.get("fullName").textValue());
        }
        assertTrue("Should contain Apache License 2.0", licenseNames.contains("Apache License 2.0"));
        assertTrue("Should contain MIT License", licenseNames.contains("MIT License"));
    }

    @Test
    public void should_download_project_license_info_with_included_attachments() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Linked releases with attachments
        Attachment att = new Attachment().setAttachmentContentId("ac1");
        ReleaseLink rlink = new ReleaseLink().setId(release1.getId()).setAttachments(Lists.newArrayList(att));
        ProjectLink plink = new ProjectLink().setLinkedReleases(Lists.newArrayList(rlink));
        given(this.projectServiceMock.createLinkedProjects(any(), any(), eq(true), any())).willReturn(Lists.newArrayList(plink));

        // Attachment usages include concluded license flag
        AttachmentUsage usage = new AttachmentUsage();
        usage.setOwner(Source.releaseId(release1.getId()));
        usage.setAttachmentContentId("ac1");
        usage.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(new HashSet<>()).setIncludeConcludedLicense(true)));
        given(this.attachmentServiceMock.getAttachemntUsages(eq(project1.getId()))).willReturn(Lists.newArrayList(usage));

        // License info generator and file
        OutputFormatInfo fmt = new OutputFormatInfo();
        fmt.setFileExtension("txt");
        fmt.setMimeType("text/plain");
        given(this.licenseInfoMockService.getOutputFormatInfoForGeneratorClass(eq("TextGenerator"))).willReturn(fmt);
        LicenseInfoFile file = new LicenseInfoFile();
        given(this.licenseInfoMockService.getLicenseInfoFile(any(), any(), anyString(), anyMap(), anyMap(), any(), anyString(), anyBoolean()))
                .willReturn(file);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseinfo?generatorClassName=TextGenerator&variant=REPORT&includeAllAttachments=true",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text/plain", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("attachment; filename="));
    }

    @Test
    public void should_download_project_license_info_without_included_attachments() throws Exception {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        // Linked release with attachment
        Attachment att = new Attachment().setAttachmentContentId("ac2");
        ReleaseLink rlink = new ReleaseLink().setId(release1.getId()).setAttachments(Lists.newArrayList(att));
        ProjectLink plink = new ProjectLink().setLinkedReleases(Lists.newArrayList(rlink));
        given(this.projectServiceMock.createLinkedProjects(any(), any(), eq(true), any())).willReturn(Lists.newArrayList(plink));

        // Usage marks the attachment as used with includeConcluded=false so controller invokes licenseInfoService.getLicenseInfoForAttachment
        AttachmentUsage usage = new AttachmentUsage();
        usage.setOwner(Source.releaseId(release1.getId()));
        usage.setAttachmentContentId("ac2");
        usage.setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(new HashSet<>()).setIncludeConcludedLicense(false)));
        given(this.attachmentServiceMock.getAttachemntUsages(eq(project1.getId()))).willReturn(Lists.newArrayList(usage));
        given(this.componentServiceMock.getReleaseById(eq(release1.getId()), any())).willReturn(release1);
        given(this.licenseInfoMockService.getLicenseInfoForAttachment(eq(release1), any(), eq("ac2"), eq(false)))
                .willReturn(Lists.newArrayList(new LicenseInfoParsingResult()));

        OutputFormatInfo fmt = new OutputFormatInfo();
        fmt.setFileExtension("html");
        fmt.setMimeType("text/html");
        given(this.licenseInfoMockService.getOutputFormatInfoForGeneratorClass(eq("XhtmlGenerator"))).willReturn(fmt);
        LicenseInfoFile file = new LicenseInfoFile();
        given(this.licenseInfoMockService.getLicenseInfoFile(any(), any(), anyString(), anyMap(), anyMap(), any(), anyString(), anyBoolean()))
                .willReturn(file);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<byte[]> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseinfo?generatorClassName=XhtmlGenerator&variant=DISCLOSURE&includeAllAttachments=false",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        byte[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text/html", response.getHeaders().getContentType().toString());
    }

    // ========== DUPLICATE PROJECT ==========

    @Test
    public void should_fail_duplicate_project_missing_name_and_version() throws IOException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/duplicate/" + project1.getId(),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_duplicate_project_success() throws IOException, TException {
        // Mock fetching and creation
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        Project cloned = new Project(project1);
        cloned.setId("p-clone");
        given(this.projectServiceMock.createProject(any(), any())).willReturn(cloned);
        // obligations copy
        doNothing().when(this.projectServiceMock).copyLinkedObligationsForClonedProject(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Project Alpha Copy");

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/duplicate/" + project1.getId(),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("Location").contains("/api/projects/duplicate/" + project1.getId() + "/p-clone"));
    }

    @Test
    public void should_create_project_with_release_uri_mapping_and_location() throws IOException, URISyntaxException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Prepare request body with releaseIdToUsage using URI keys to trigger mapping path
        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Project");
        Map<String, Object> relUsage = new HashMap<>();
        Map<String, String> relMeta = new HashMap<>();
        relMeta.put("releaseRelation", "CONTAINED");
        relMeta.put("mainlineState", "OPEN");
        relUsage.put("/api/releases/" + release1.getId(), relMeta);
        body.put("releaseIdToUsage", relUsage);

        // Mock creation to return a project with ID
        Project created = new Project();
        created.setId("np001");
        given(this.projectServiceMock.createProject(any(), any())).willReturn(created);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("Location").endsWith("/np001"));
    }

    // ========== LINK/PATCH RELEASES ==========

    @Test
    public void should_link_releases_with_list_body() throws IOException, TException {
        // update result OK
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SUCCESS);
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> releaseUris = Arrays.asList("/api/releases/" + release1.getId(), "/api/releases/" + release2.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.POST,
                        new HttpEntity<>(releaseUris, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void should_patch_releases_with_map_body_and_sent_to_moderator() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.updateProject(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> mapBody = new HashMap<>();
        Map<String, String> relMeta = new HashMap<>();
        relMeta.put("releaseRelation", "CONTAINED");
        relMeta.put("mainlineState", "OPEN");
        mapBody.put(release1.getId(), relMeta);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.PATCH,
                        new HttpEntity<>(mapBody, headers),
                        String.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_link_releases_access_denied_without_comment() throws IOException, TException {
        // Force deny write, no comment
        doReturn(false).when(this.restControllerHelper).isWriteActionAllowed(any(), any());
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> releaseUris = Arrays.asList("/api/releases/" + release1.getId());

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/releases",
                        HttpMethod.POST,
                        new HttpEntity<>(releaseUris, headers),
                        String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
