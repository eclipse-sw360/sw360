/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.projects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.ObligationList;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.projects.ProjectData;
import org.eclipse.sw360.datahandler.services.projects.ProjectLink;
import org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectHandler projectHandler;

    public ProjectController() throws IOException {
        this.projectHandler = new ProjectHandler();
    }

    // ========================
    // Project CRUD
    // ========================

    @GetMapping("/search")
    public List<Project> search(@RequestParam String text) throws TException {
        return ProjectRestMapper.fromThriftProjects(projectHandler.search(text));
    }

    @PostMapping("/search/refined")
    public List<Project> refineSearch(
            @RequestBody RefineSearchRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(projectHandler.refineSearch(
                request.getText(),
                request.getSubQueryRestrictions(),
                user));
    }

    @PostMapping("/search/refined/paginated")
    public PaginatedResult<Project> refineSearchPageable(
            @RequestBody RefineSearchPageableRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftPaginatedProjects(
                projectHandler.refineSearchPageable(
                        request.getText(),
                        request.getSubQueryRestrictions(),
                        user,
                        ProjectRestMapper.toThriftPagination(request.getPaginationData())));
    }

    @PostMapping("/search/refined/no-user")
    public List<Project> refineSearchWithoutUser(@RequestBody RefineSearchRequest request) {
        return ProjectRestMapper.fromThriftProjects(projectHandler.refineSearchWithoutUser(
                request.getText(),
                request.getSubQueryRestrictions()));
    }

    @PostMapping("/my")
    public List<Project> getMyProjects(
            @RequestBody Map<String, Boolean> userRoles,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(projectHandler.getMyProjects(user, userRoles));
    }

    @GetMapping("/accessible/summary")
    public List<Project> getAccessibleProjectsSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(projectHandler.getAccessibleProjectsSummary(user));
    }

    @PostMapping("/accessible/summary/paginated")
    public PaginatedResult<Project> getAccessibleProjectsSummaryWithPagination(
            @RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftPaginatedProjects(
                projectHandler.getAccessibleProjectsSummaryWithPagination(
                        user, ProjectRestMapper.toThriftPagination(pageData)));
    }

    @GetMapping("/accessible")
    public Set<Project> getAccessibleProjects(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.getAccessibleProjects(user));
    }

    @GetMapping("/by-name")
    public List<Project> searchByName(
            @RequestParam String name,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(projectHandler.searchByName(name, user));
    }

    @PostMapping("/search/by-name-prefix")
    public PaginatedResult<Project> searchProjectByNamePrefixPaginated(
            @RequestBody NameSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftPaginatedProjects(
                projectHandler.searchProjectByNamePrefixPaginated(
                        user, request.getName(), ProjectRestMapper.toThriftPagination(request.getPaginationData())));
    }

    @PostMapping("/search/by-exact-name")
    public PaginatedResult<Project> searchProjectByExactNamePaginated(
            @RequestBody NameSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftPaginatedProjects(
                projectHandler.searchProjectByExactNamePaginated(
                        user, request.getName(), ProjectRestMapper.toThriftPagination(request.getPaginationData())));
    }

    @PostMapping("/search/by-exact-values")
    public PaginatedResult<Project> searchAccessibleProjectByExactValues(
            @RequestBody ExactValuesSearchRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftPaginatedProjects(
                projectHandler.searchAccessibleProjectByExactValues(
                        request.getSubQueryRestrictions(),
                        user,
                        ProjectRestMapper.toThriftPagination(request.getPaginationData())));
    }

    @GetMapping("/by-group")
    public ProjectData searchByGroup(
            @RequestParam String group,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectData(projectHandler.searchByGroup(group, user));
    }

    @GetMapping("/by-tag")
    public ProjectData searchByTag(
            @RequestParam String tag,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectData(projectHandler.searchByTag(tag, user));
    }

    @GetMapping("/by-type")
    public ProjectData searchByType(
            @RequestParam String type,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectData(projectHandler.searchByType(type, user));
    }

    @GetMapping("/by-release/{releaseId}")
    public Set<Project> searchByReleaseId(
            @PathVariable String releaseId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchByReleaseId(releaseId, user));
    }

    @PostMapping("/by-release-ids")
    public Set<Project> searchByReleaseIds(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchByReleaseIds(ids, user));
    }

    @GetMapping("/by-package/{packageId}")
    public Set<Project> searchProjectByPackageId(
            @PathVariable String packageId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchProjectByPackageId(packageId, user));
    }

    @PostMapping("/by-package-ids")
    public Set<Project> searchProjectByPackageIds(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchProjectByPackageIds(ids, user));
    }

    @GetMapping("/count/by-package/{packageId}")
    public int getProjectCountByPackageId(@PathVariable String packageId) throws TException {
        return projectHandler.getProjectCountByPackageId(packageId);
    }

    @GetMapping("/{id}/linking")
    public Set<Project> searchLinkingProjects(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchLinkingProjects(id, user));
    }

    @GetMapping("/{id}")
    public Project getProjectById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProject(projectHandler.getProjectById(id, user));
    }

    @GetMapping("/{id}/ignore-visibility")
    public Project getProjectByIdIgnoringVisibility(@PathVariable String id) throws SW360Exception {
        return ProjectRestMapper.fromThriftProject(projectHandler.getProjectByIdIgnoringVisibility(id));
    }

    @PostMapping("/by-ids")
    public List<Project> getProjectsById(
            @RequestBody List<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(projectHandler.getProjectsById(ids, user));
    }

    @GetMapping("/{id}/for-edit")
    public Project getProjectByIdForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProject(projectHandler.getProjectByIdForEdit(id, user));
    }

    @PostMapping("/count/by-release-ids")
    public int getCountByReleaseIds(@RequestBody Set<String> ids) throws TException {
        return projectHandler.getCountByReleaseIds(ids);
    }

    @GetMapping("/count/{id}")
    public int getCountByProjectId(@PathVariable String id) throws TException {
        return projectHandler.getCountByProjectId(id);
    }

    @PostMapping("/search-by-external-ids")
    public Set<Project> searchByExternalIds(
            @RequestBody Map<String, Set<String>> externalIds,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectSet(projectHandler.searchByExternalIds(externalIds, user));
    }

    @PostMapping
    public AddDocumentRequestSummary addProject(
            @RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftAddDocumentRequestSummary(
                projectHandler.addProject(ProjectRestMapper.toThriftProject(project), user));
    }

    @PutMapping
    public RequestStatus updateProject(
            @RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.updateProject(ProjectRestMapper.toThriftProject(project), user));
    }

    @PutMapping("/force")
    public RequestStatus updateProjectWithForceFlag(
            @RequestBody Project project,
            @RequestParam boolean forceUpdate,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.updateProjectWithForceFlag(ProjectRestMapper.toThriftProject(project), user, forceUpdate));
    }

    @PutMapping("/moderation")
    public RequestStatus updateProjectFromModerationRequest(
            @RequestBody UpdateProjectFromModerationRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.updateProjectFromModerationRequest(
                        ProjectRestMapper.toThriftProject(request.getAdditions()),
                        ProjectRestMapper.toThriftProject(request.getDeletions()),
                        user));
    }

    @DeleteMapping("/{id}")
    public RequestStatus deleteProject(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(projectHandler.deleteProject(id, user));
    }

    @DeleteMapping("/{id}/force")
    public RequestStatus deleteProjectWithForceFlag(
            @PathVariable String id,
            @RequestParam boolean forceDelete,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.deleteProjectWithForceFlag(id, user, forceDelete));
    }

    @GetMapping("/{id}/in-use")
    public boolean projectIsUsed(@PathVariable String id) throws TException {
        return projectHandler.projectIsUsed(id);
    }

    @PostMapping("/cyclic-path")
    public String getCyclicLinkedProjectPath(
            @RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getCyclicLinkedProjectPath(ProjectRestMapper.toThriftProject(project), user);
    }

    @GetMapping("/groups")
    public Set<String> getGroups() throws TException {
        return projectHandler.getGroups();
    }

    @GetMapping("/my-count")
    public int getMyAccessibleProjectCounts(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getMyAccessibleProjectCounts(user);
    }

    // ========================
    // Linked projects
    // ========================

    @PostMapping("/linked")
    public List<ProjectLink> getLinkedProjectsOfProject(
            @RequestBody LinkedProjectsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(
                projectHandler.getLinkedProjectsOfProject(
                        ProjectRestMapper.toThriftProject(request.getProject()),
                        request.isDeep(),
                        user));
    }

    @GetMapping("/{id}/linked")
    public List<ProjectLink> getLinkedProjectsById(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean deep,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(projectHandler.getLinkedProjectsById(id, deep, user));
    }

    @PostMapping("/linked/by-relations")
    public List<ProjectLink> getLinkedProjects(
            @RequestBody LinkedProjectsByRelationsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(
                projectHandler.getLinkedProjects(
                        ProjectRestMapper.toThriftProjectRelationshipMap(request.getRelations()),
                        request.isDeep(),
                        user));
    }

    @PostMapping("/linked/by-relations/no-releases")
    public List<ProjectLink> getLinkedProjectsWithoutReleases(
            @RequestBody LinkedProjectsByRelationsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(
                projectHandler.getLinkedProjectsWithoutReleases(
                        ProjectRestMapper.toThriftProjectRelationshipMap(request.getRelations()),
                        request.isDeep(),
                        user));
    }

    @PostMapping("/linked/no-releases")
    public List<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(
            @RequestBody LinkedProjectsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(
                projectHandler.getLinkedProjectsOfProjectWithoutReleases(
                        ProjectRestMapper.toThriftProject(request.getProject()),
                        request.isDeep(),
                        user));
    }

    @PostMapping("/linked/all-releases")
    public List<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(
            @RequestBody LinkedProjectsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjectLinks(
                projectHandler.getLinkedProjectsOfProjectWithAllReleases(
                        ProjectRestMapper.toThriftProject(request.getProject()),
                        request.isDeep(),
                        user));
    }

    // ========================
    // Clearing
    // ========================

    @PostMapping("/clearing-request")
    public AddDocumentRequestSummary createClearingRequest(
            @RequestBody ClearingRequest clearingRequest,
            @RequestParam String projectUrl,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftAddDocumentRequestSummary(
                projectHandler.createClearingRequest(
                        ProjectRestMapper.toThriftClearingRequest(clearingRequest), user, projectUrl));
    }

    @PostMapping("/clearing-state/fill")
    public List<Project> fillClearingStateSummary(
            @RequestBody List<Project> projects,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(
                projectHandler.fillClearingStateSummary(ProjectRestMapper.toThriftProjects(projects), user));
    }

    @PostMapping("/clearing-state/fill-with-subprojects")
    public List<Project> fillClearingStateSummaryIncludingSubprojects(
            @RequestBody List<Project> projects,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProjects(
                projectHandler.fillClearingStateSummaryIncludingSubprojects(
                        ProjectRestMapper.toThriftProjects(projects), user));
    }

    @PostMapping("/clearing-state/fill-single")
    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(
            @RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftProject(
                projectHandler.fillClearingStateSummaryIncludingSubprojectsForSingleProject(
                        ProjectRestMapper.toThriftProject(project), user));
    }

    @GetMapping("/{id}/clearing-status")
    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftReleaseClearingStatusDataList(
                projectHandler.getReleaseClearingStatuses(id, user));
    }

    @GetMapping("/{id}/clearing-status/accessible")
    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftReleaseClearingStatusDataList(
                projectHandler.getReleaseClearingStatusesWithAccessibility(id, user));
    }

    @GetMapping("/{id}/clearing-state-list")
    public List<Map<String, String>> getClearingStateInformationForListView(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getClearingStateInformationForListView(id, user);
    }

    @GetMapping("/{id}/clearing-state-list/accessible")
    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getAccessibleClearingStateInformationForListView(id, user);
    }

    @GetMapping("/{id}/dependency-network")
    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getAccessibleDependencyNetworkForListView(id, user);
    }

    // ========================
    // Release links
    // ========================

    @PostMapping("/{id}/release-links/by-trace")
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(
            @PathVariable String id,
            @RequestBody List<String> trace,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftReleaseLinks(
                projectHandler.getReleaseLinksOfProjectNetWorkByTrace(id, trace, user));
    }

    @PostMapping("/{id}/release-links/by-index")
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(
            @PathVariable String id,
            @RequestBody List<String> indexPath,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftReleaseLinks(
                projectHandler.getReleaseLinksOfProjectNetWorkByIndexPath(id, indexPath, user));
    }

    @GetMapping("/{id}/dependency-releases")
    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftReleaseNodes(
                projectHandler.getLinkedReleasesInDependencyNetworkOfProject(id, user));
    }

    // ========================
    // Obligations
    // ========================

    @GetMapping("/obligations/{id}")
    public ObligationList getLinkedObligations(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftObligationList(projectHandler.getLinkedObligations(id, user));
    }

    @PostMapping("/obligations")
    public RequestStatus addLinkedObligations(
            @RequestBody ObligationList obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.addLinkedObligations(ProjectRestMapper.toThriftObligationList(obligation), user));
    }

    @PutMapping("/obligations")
    public RequestStatus updateLinkedObligations(
            @RequestBody ObligationList obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.updateLinkedObligations(ProjectRestMapper.toThriftObligationList(obligation), user));
    }

    // ========================
    // Release relations
    // ========================

    @GetMapping("/{id}/release-relations")
    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(
            @PathVariable String id) throws TException {
        return ProjectRestMapper.fromThriftUsedReleaseRelationsList(
                projectHandler.getUsedReleaseRelationsByProjectId(id));
    }

    @PostMapping("/release-relations")
    public void addReleaseRelationsUsage(@RequestBody UsedReleaseRelations usedReleaseRelations) throws TException {
        projectHandler.addReleaseRelationsUsage(ProjectRestMapper.toThriftUsedReleaseRelations(usedReleaseRelations));
    }

    @PutMapping("/release-relations")
    public void updateReleaseRelationsUsage(@RequestBody UsedReleaseRelations usedReleaseRelations) throws TException {
        projectHandler.updateReleaseRelationsUsage(ProjectRestMapper.toThriftUsedReleaseRelations(usedReleaseRelations));
    }

    @DeleteMapping("/release-relations")
    public void deleteReleaseRelationsUsage(@RequestBody UsedReleaseRelations usedReleaseRelations) throws TException {
        projectHandler.deleteReleaseRelationsUsage(ProjectRestMapper.toThriftUsedReleaseRelations(usedReleaseRelations));
    }

    // ========================
    // Export/Import
    // ========================

    @PostMapping("/export/monitoring")
    public RequestStatus exportForMonitoringList() throws TException {
        return ProjectRestMapper.fromThriftRequestStatus(projectHandler.exportForMonitoringList());
    }

    @GetMapping(value = "/download-excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadExcel(
            @RequestParam boolean extendedByReleases,
            @RequestParam String token,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return toByteArray(projectHandler.downloadExcel(user, extendedByReleases, token));
    }

    @GetMapping(value = "/report-stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getReportDataStream(
            @RequestParam boolean extendedByReleases,
            @RequestParam String projectId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return toByteArray(projectHandler.getReportDataStream(user, extendedByReleases, projectId));
    }

    @GetMapping("/report-email")
    public String getReportInEmail(
            @RequestParam boolean extendedByReleases,
            @RequestParam String projectId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return projectHandler.getReportInEmail(user, extendedByReleases, projectId);
    }

    @PostMapping("/export-mail")
    public void sendExportSpreadsheetSuccessMail(
            @RequestParam String url,
            @RequestParam String recepient) throws TException {
        projectHandler.sendExportSpreadsheetSuccessMail(url, recepient);
    }

    @PostMapping("/import-bom")
    public RequestSummary importBomFromAttachmentContent(
            @RequestParam String attachmentContentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestSummary(
                projectHandler.importBomFromAttachmentContent(user, attachmentContentId));
    }

    @PostMapping("/import-cyclonedx")
    public RequestSummary importCycloneDxFromAttachmentContent(
            @RequestParam String attachmentContentId,
            @RequestParam String projectId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestSummary(
                projectHandler.importCycloneDxFromAttachmentContent(user, attachmentContentId, projectId));
    }

    @PostMapping("/import-cyclonedx/no-replace")
    public RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(
            @RequestParam String attachmentContentId,
            @RequestParam String projectId,
            @RequestParam boolean doNotReplace,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestSummary(
                projectHandler.importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(
                        user, attachmentContentId, projectId, doNotReplace));
    }

    @PostMapping("/export-cyclonedx")
    public RequestSummary exportCycloneDxSbom(
            @RequestParam String projectId,
            @RequestParam String bomType,
            @RequestParam boolean includeSubProjReleases,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestSummary(
                projectHandler.exportCycloneDxSbom(projectId, bomType, includeSubProjReleases, user));
    }

    @GetMapping("/sbom-import-info")
    public String getSbomImportInfoFromAttachmentAsString(
            @RequestParam String attachmentContentId) throws SW360Exception {
        return projectHandler.getSbomImportInfoFromAttachmentAsString(attachmentContentId);
    }

    // ========================
    // Other
    // ========================

    @GetMapping("/duplicates")
    public Map<String, List<String>> getDuplicateProjects() throws TException {
        return projectHandler.getDuplicateProjects();
    }

    @DeleteMapping("/{id}/attachment/{attachmentContentId}")
    public RequestStatus removeAttachmentFromProject(
            @PathVariable String id,
            @PathVariable String attachmentContentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ProjectRestMapper.fromThriftRequestStatus(
                projectHandler.removeAttachmentFromProject(id, user, attachmentContentId));
    }

    // ========================
    // Helper
    // ========================

    private static byte[] toByteArray(ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    // ========================
    // Request DTOs
    // ========================

    public static class RefineSearchRequest {
        private String text;
        private Map<String, Set<String>> subQueryRestrictions;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Map<String, Set<String>> getSubQueryRestrictions() { return subQueryRestrictions; }
        public void setSubQueryRestrictions(Map<String, Set<String>> subQueryRestrictions) { this.subQueryRestrictions = subQueryRestrictions; }
    }

    public static class RefineSearchPageableRequest {
        private String text;
        private Map<String, Set<String>> subQueryRestrictions;
        private PaginationData paginationData;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Map<String, Set<String>> getSubQueryRestrictions() { return subQueryRestrictions; }
        public void setSubQueryRestrictions(Map<String, Set<String>> subQueryRestrictions) { this.subQueryRestrictions = subQueryRestrictions; }
        public PaginationData getPaginationData() { return paginationData; }
        public void setPaginationData(PaginationData paginationData) { this.paginationData = paginationData; }
    }

    public static class NameSearchPaginatedRequest {
        private String name;
        private PaginationData paginationData;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public PaginationData getPaginationData() { return paginationData; }
        public void setPaginationData(PaginationData paginationData) { this.paginationData = paginationData; }
    }

    public static class ExactValuesSearchRequest {
        private Map<String, Set<String>> subQueryRestrictions;
        private PaginationData paginationData;

        public Map<String, Set<String>> getSubQueryRestrictions() { return subQueryRestrictions; }
        public void setSubQueryRestrictions(Map<String, Set<String>> subQueryRestrictions) { this.subQueryRestrictions = subQueryRestrictions; }
        public PaginationData getPaginationData() { return paginationData; }
        public void setPaginationData(PaginationData paginationData) { this.paginationData = paginationData; }
    }

    public static class LinkedProjectsRequest {
        private Project project;
        private boolean deep;

        public Project getProject() { return project; }
        public void setProject(Project project) { this.project = project; }
        public boolean isDeep() { return deep; }
        public void setDeep(boolean deep) { this.deep = deep; }
    }

    public static class LinkedProjectsByRelationsRequest {
        private Map<String, ProjectProjectRelationship> relations;
        private boolean deep;

        public Map<String, ProjectProjectRelationship> getRelations() { return relations; }
        public void setRelations(Map<String, ProjectProjectRelationship> relations) { this.relations = relations; }
        public boolean isDeep() { return deep; }
        public void setDeep(boolean deep) { this.deep = deep; }
    }

    public static class UpdateProjectFromModerationRequest {
        private Project additions;
        private Project deletions;

        public Project getAdditions() { return additions; }
        public void setAdditions(Project additions) { this.additions = additions; }
        public Project getDeletions() { return deletions; }
        public void setDeletions(Project deletions) { this.deletions = deletions; }
    }
}
