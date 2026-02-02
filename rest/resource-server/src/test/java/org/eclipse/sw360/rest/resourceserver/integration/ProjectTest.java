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
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ObligationStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.report.SW360ReportService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
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
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

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
    public void before() throws TException, IOException {
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
    }

    private void setupCommonMocks() throws TException, IOException {
        // Common project service mocks
        given(this.projectServiceMock.searchAccessibleProjectByExactValues(any(), any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectList.size()).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        projectList.stream().toList()
                )
        );
        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(
                Collections.singletonMap(
                        new PaginationData().setRowsPerPage(projectList.size()).setDisplayStart(0).setTotalRowCount(projectList.size()),
                        projectList.stream().toList()
                )
        );
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getProjectForUserById(eq(project2.getId()), any())).willReturn(project2);
        given(this.projectServiceMock.getProjectForUserById(eq("unknown"), any())).willThrow(new TException("Project not found"));

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
    public void should_get_all_projects() throws IOException {
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

    @Test
    public void should_check_xss_body() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);

        given(this.projectServiceMock.createProject(any(), any())).willAnswer(i -> {
            Project project = i.getArgument(0);
            project.setId("dont_care");
            project.setCreatedBy("admin@sw360.org");
            return project;
        });

        String maliciousDescription = "New project test2223 <img/src/onerror=alert('1')>";
        String goodSummary = "© &lt; some year with “org”";

        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Project XSS");
        body.put("description", maliciousDescription);
        body.put("projectType", "CUSTOMER");
        body.put("businessUnit", "Group A");
        body.put("version", "1.2.0");
        body.put("visibility", "EVERYONE");
        body.put("state", "ACTIVE");
        body.put("clearingState", "OPEN");
        body.put("clearingSummary", goodSummary);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(org.owasp.encoder.Encode.forHtmlContent(maliciousDescription), responseBody.get("description").textValue());
        assertEquals(goodSummary, responseBody.get("clearingSummary").textValue());
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

    // ========== PROJECT LICENSE CLEARING ==========

    @Test
    public void should_get_license_clearing() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/licenseClearing?transitive=true",
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
    public void should_get_packages_by_project_id() throws IOException, TException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/packages",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_vulnerability_summary() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilitySummary",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void should_get_vulnerabilities_of_releases() throws IOException, TException {
        given(this.projectServiceMock.getProjectForUserById(eq(project1.getId()), any())).willReturn(project1);
        given(this.projectServiceMock.getReleaseIds(eq(project1.getId()), any(), anyBoolean())).willReturn(new HashSet<>(Arrays.asList(release1.getId(), release2.getId())));
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1.getId()), any())).willReturn(release1);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release2.getId()), any())).willReturn(release2);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/projects/" + project1.getId() + "/vulnerabilities?priority=1&projectRelevance=RELEVANT",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
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
        given(this.sw360ReportServiceMock.getProjectBuffer(any(), anyBoolean(), any(), any())).willReturn(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));

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
}
