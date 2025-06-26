/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests VIEWER role controller-level access restrictions.
 * These tests verify throwIfViewerUser() and PermissionUtils.isViewer()
 * guards in controllers. EndpointsFilter-level guards (mutation blocking,
 * clearing/moderation GET blocking) are tested separately.
 *
 * <h2>What VIEWER Can Do ✅</h2>
 * <ul>
 *   <li>View projects (summary + attachments only) with visibility=EVERYONE</li>
 *   <li>View components</li>
 *   <li>View releases</li>
 *   <li>View licenses (basic info only, not obligations)</li>
 *   <li>View linked projects and their releases</li>
 *   <li>Search (limited types)</li>
 *   <li>Create READ-only API tokens</li>
 * </ul>
 *
 * <h2>What VIEWER Cannot Do ❌</h2>
 * <ul>
 *   <li>Own/moderate/contribute to projects</li>
 *   <li>View vulnerabilities</li>
 *   <li>View obligations (including license obligations)</li>
 *   <li>Access ECC information</li>
 *   <li>Generate reports</li>
 *   <li>Access clearing/moderation requests (including license clearing)</li>
 *   <li>Access Fossology operations</li>
 *   <li>View "My Projects" (user-specific projects)</li>
 *   <li>Perform any mutation operations (POST/PATCH/PUT/DELETE)</li>
 *   <li>View projects with restricted visibility (non-EVERYONE)</li>
 *   <li>View security responsibles</li>
 *   <li>View linked obligations</li>
 * </ul>
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ViewerRoleAccessTest extends TestRestDocsSpecBase {

    private static final String VIEWER_EMAIL = "viewer@sw360.org";
    private static final String VIEWER_PASSWORD = "12345";

    @MockitoBean
    private Sw360ProjectService projectServiceMock;
    @MockitoBean
    private Sw360ComponentService componentServiceMock;
    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;
    @MockitoBean
    private Sw360LicenseService licenseServiceMock;
    @MockitoBean
    private Sw360VulnerabilityService vulnerabilityServiceMock;

    private User viewerUser;
    private Project everyoneProject;
    private Component component;
    private Release release;
    private License license;

    @Before
    public void before() throws TException {
        viewerUser = new User(VIEWER_EMAIL, "sw360")
                .setId("viewer-001")
                .setUserGroup(UserGroup.VIEWER);

        // Register VIEWER in Spring Security with READ authority
        when(sw360CustomUserDetailsService.loadUserByUsername(VIEWER_EMAIL))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        VIEWER_EMAIL, encoder.encode(VIEWER_PASSWORD),
                        List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()))));

        given(this.userServiceMock.getUserByEmailOrExternalId(VIEWER_EMAIL)).willReturn(viewerUser);
        given(this.userServiceMock.getUserByEmail(VIEWER_EMAIL)).willReturn(viewerUser);

        everyoneProject = new Project()
                .setId("proj-001")
                .setName("Public Project")
                .setVersion("1.0")
                .setProjectType(ProjectType.PRODUCT)
                .setVisbility(Visibility.EVERYONE)
                .setCreatedBy("admin@sw360.org")
                .setDescription("A publicly visible project");

        component = new Component()
                .setId("comp-001")
                .setName("Test Component")
                .setComponentType(ComponentType.OSS);

        release = new Release()
                .setId("rel-001")
                .setName("Test Release")
                .setVersion("1.0")
                .setComponentId("comp-001");

        license = new License()
                .setId("MIT")
                .setShortname("MIT")
                .setFullname("MIT License");

        given(this.projectServiceMock.getProjectForUserById(eq("proj-001"), any())).willReturn(everyoneProject);
        given(this.componentServiceMock.getComponentForUserById(eq("comp-001"), any())).willReturn(component);
        given(this.releaseServiceMock.getReleaseForUserById(eq("rel-001"), any())).willReturn(release);
    }

    private String viewerAuth() {
        return TestHelper.generateAuthHeader(VIEWER_EMAIL, VIEWER_PASSWORD);
    }

    // ---- Moderation Requests: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_moderation_requests_list() throws Exception {
        this.mockMvc.perform(get("/api/moderationrequest")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_moderation_request_by_id() throws Exception {
        this.mockMvc.perform(get("/api/moderationrequest/mr-001")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_moderation_requests_by_state() throws Exception {
        this.mockMvc.perform(get("/api/moderationrequest/byState")
                        .header("Authorization", viewerAuth())
                        .param("state", "open")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_my_submissions() throws Exception {
        this.mockMvc.perform(get("/api/moderationrequest/mySubmissions")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Vulnerability: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_vulnerabilities_list() throws Exception {
        this.mockMvc.perform(get("/api/vulnerabilities")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_vulnerability_by_id() throws Exception {
        this.mockMvc.perform(get("/api/vulnerabilities/CVE-2024-0001")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Obligation: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_obligations_list() throws Exception {
        this.mockMvc.perform(get("/api/obligations")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_obligation_by_id() throws Exception {
        this.mockMvc.perform(get("/api/obligations/obl-001")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- ECC: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_ecc() throws Exception {
        this.mockMvc.perform(get("/api/ecc")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Project: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_myprojects() throws Exception {
        this.mockMvc.perform(get("/api/projects/myprojects")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_deny_viewer_access_to_license_clearing() throws Exception {
        this.mockMvc.perform(get("/api/projects/proj-001/licenseClearing")
                        .header("Authorization", viewerAuth())
                        .param("transitive", "false")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void should_allow_viewer_to_read_linked_projects() throws Exception {
        given(this.projectServiceMock.getReleaseIds(eq("proj-001"), any(), eq(false))).willReturn(Collections.emptySet());
        given(this.projectServiceMock.getReleaseIds(eq("proj-001"), any(), eq(true))).willReturn(Collections.emptySet());

        this.mockMvc.perform(get("/api/projects/proj-001/linkedProjects")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_allow_viewer_to_read_linked_projects_releases() throws Exception {
        given(this.projectServiceMock.getReleaseIds(eq("proj-001"), any(), eq(false))).willReturn(Collections.emptySet());
        given(this.projectServiceMock.getReleaseIds(eq("proj-001"), any(), eq(true))).willReturn(Collections.emptySet());

        this.mockMvc.perform(get("/api/projects/proj-001/linkedProjects/releases")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isNoContent()); // Returns 204 when no releases found
    }

    // ---- License obligations: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_license_obligations() throws Exception {
        this.mockMvc.perform(get("/api/licenses/MIT/obligations")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Report: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_reports() throws Exception {
        this.mockMvc.perform(get("/api/reports")
                        .header("Authorization", viewerAuth())
                        .param("module", "projects")
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Fossology: controller-level throwIfViewerUser ----

    @Test
    public void should_deny_viewer_access_to_fossology_status() throws Exception {
        this.mockMvc.perform(get("/api/releases/rel-001/checkFossologyProcessStatus")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isForbidden());
    }

    // ---- Positive: VIEWER CAN read components, releases, and licenses ----

    @Test
    public void should_allow_viewer_to_read_components() throws Exception {
        Map<PaginationData, List<Component>> paginatedResult = new HashMap<>();
        PaginationData pageData = new PaginationData()
                .setTotalRowCount(1)
                .setRowsPerPage(10)
                .setDisplayStart(0);
        paginatedResult.put(pageData, List.of(component));

        given(this.componentServiceMock.getRecentComponentsSummaryWithPagination(any(), any())).willReturn(paginatedResult);

        this.mockMvc.perform(get("/api/components")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_allow_viewer_to_read_component_by_id() throws Exception {
        this.mockMvc.perform(get("/api/components/comp-001")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_allow_viewer_to_read_licenses() throws Exception {
        given(this.licenseServiceMock.getLicenses()).willReturn(List.of(license));

        this.mockMvc.perform(get("/api/licenses")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_allow_viewer_to_read_license_by_id() throws Exception {
        given(this.licenseServiceMock.getLicenseById(eq("MIT"))).willReturn(license);

        this.mockMvc.perform(get("/api/licenses/MIT")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void should_allow_viewer_to_read_projects_with_visibility_everyone() throws Exception {
        Map<PaginationData, List<Project>> paginatedResult = new HashMap<>();
        PaginationData pageData = new PaginationData()
                .setTotalRowCount(1)
                .setRowsPerPage(10)
                .setDisplayStart(0);
        paginatedResult.put(pageData, List.of(everyoneProject));

        given(this.projectServiceMock.getProjectsForUser(any(), any())).willReturn(paginatedResult);

        this.mockMvc.perform(get("/api/projects")
                        .header("Authorization", viewerAuth())
                        .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk());
    }
}
