/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStatusDataConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationListConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectDataConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectProjectRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.projects.UsedReleaseRelationsConverter;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

/**
 * Thrift {@link ProjectService.Iface} adapter that delegates to the projects REST backend
 * ({@code /projects/api/projects}). Keeps the Thrift contract intact for existing resource-server
 * callers while removing the Thrift transport.
 */
@org.springframework.stereotype.Component
public class ProjectServiceRestAdapter implements ProjectService.Iface {

    private static final String BASE = "/projects/api/projects";

    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.projects.Project>> PROJECT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<org.eclipse.sw360.datahandler.services.projects.Project>> PROJECT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.projects.ProjectLink>> PROJECT_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData>> RELEASE_CLEARING_STATUS_DATA_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.ReleaseLink>> RELEASE_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.ReleaseNode>> RELEASE_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations>> USED_RELEASE_RELATIONS_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, List<String>>> DUPLICATE_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, String>>> LIST_MAP_STRING_STRING =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project>> PROJECT_PAGE =
            new ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project>>() {};

    private final RestClient restClient;

    public ProjectServiceRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    // ---- Search / Query -------------------------------------------------------------------------

    @Override
    public List<Project> search(String text) throws TException {
        return call(() -> toThriftProjects(restClient.get()
                .uri(b -> {
                    var ub = b.path(BASE + "/search");
                    if (text != null) {
                        ub.queryParam("text", text);
                    }
                    return ub.build();
                })
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public List<Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, User user)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/search/refined")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public Map<PaginationData, List<Project>> refineSearchPageable(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData paginationData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        body.put("paginationData", PaginationDataConverter.fromThrift(paginationData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/refined/paginated")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
        return toPaginatedMap(result, paginationData);
    }

    @Override
    public List<Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/search/refined/no-user")
                .body(body)
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public List<Project> searchByName(String name, User user) throws TException {
        return call(() -> toThriftProjects(restClient.get()
                .uri(b -> b.path(BASE + "/by-name").queryParam("name", name).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("paginationData", PaginationDataConverter.fromThrift(pageData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/by-name-prefix")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("paginationData", PaginationDataConverter.fromThrift(pageData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-name")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchAccessibleProjectByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("subQueryRestrictions", subQueryRestrictions);
        body.put("paginationData", PaginationDataConverter.fromThrift(pageData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-values")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public ProjectData searchByGroup(String group, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/by-group").queryParam("group", group).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class)));
    }

    @Override
    public ProjectData searchByTag(String tag, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/by-tag").queryParam("tag", tag).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class)));
    }

    @Override
    public ProjectData searchByType(String type, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/by-type").queryParam("type", type).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class)));
    }

    @Override
    public Set<Project> searchByReleaseId(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.get()
                .uri(b -> b.path(BASE + "/by-release/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public Set<Project> searchByReleaseIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.post()
                .uri(BASE + "/by-release-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public Set<Project> searchProjectByPackageId(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.get()
                .uri(b -> b.path(BASE + "/by-package/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public Set<Project> searchProjectByPackageIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.post()
                .uri(BASE + "/by-package-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public int getProjectCountByPackageId(String id) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/count/by-package/{id}").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public Set<Project> searchLinkingProjects(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/linking").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.post()
                .uri(BASE + "/search-by-external-ids")
                .headers(h -> addUser(h, user))
                .body(externalIds)
                .retrieve()
                .body(PROJECT_SET)));
    }

    // ---- Project getters ------------------------------------------------------------------------

    @Override
    public Project getProjectById(String id, User user) throws TException {
        return call(() -> ProjectConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class)));
    }

    @Override
    public Project getProjectByIdIgnoringVisibility(String id) throws TException {
        return call(() -> ProjectConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/ignore-visibility").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class)));
    }

    @Override
    public List<Project> getProjectsById(List<String> id, User user) throws TException {
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/by-ids")
                .headers(h -> addUser(h, user))
                .body(id)
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public Project getProjectByIdForEdit(String id, User user) throws TException {
        return call(() -> ProjectConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class)));
    }

    // ---- Summary / Accessibility ----------------------------------------------------------------

    @Override
    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/my")
                .headers(h -> addUser(h, user))
                .body(userRoles)
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public List<Project> getAccessibleProjectsSummary(User user) throws TException {
        return call(() -> toThriftProjects(restClient.get()
                .uri(BASE + "/accessible/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public Map<PaginationData, List<Project>> getAccessibleProjectsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result =
                call(() -> restClient.post()
                        .uri(BASE + "/accessible/summary/paginated")
                        .headers(h -> addUser(h, user))
                        .body(PaginationDataConverter.fromThrift(pageData))
                        .retrieve()
                        .body(PROJECT_PAGE));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Set<Project> getAccessibleProjects(User user) throws TException {
        return call(() -> toThriftProjectSet(restClient.get()
                .uri(BASE + "/accessible")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET)));
    }

    @Override
    public int getMyAccessibleProjectCounts(User user) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/my-count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    // ---- Counts ---------------------------------------------------------------------------------

    @Override
    public int getCountByReleaseIds(Set<String> ids) throws TException {
        Integer count = call(() -> restClient.post()
                .uri(BASE + "/count/by-release-ids")
                .body(ids)
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public int getCountByProjectId(String id) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/count/{id}").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    // ---- Add / Update / Delete ------------------------------------------------------------------

    @Override
    public AddDocumentRequestSummary addProject(Project project, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(restClient.post()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(ProjectConverter.fromThrift(project))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class)));
    }

    @Override
    public RequestStatus updateProject(Project project, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(ProjectConverter.fromThrift(project))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateProjectWithForceFlag(Project project, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(b -> b.path(BASE + "/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(ProjectConverter.fromThrift(project))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    public RequestStatus updateProjectFromModerationRequest(Project projectAdditions, Project projectDeletions,
            User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", ProjectConverter.fromThrift(projectAdditions));
        body.put("deletions", ProjectConverter.fromThrift(projectDeletions));
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteProject(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteProjectWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Clearing -------------------------------------------------------------------------------

    @Override
    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User user,
            String projectUrl) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/clearing-request").queryParam("projectUrl", projectUrl).build())
                .headers(h -> addUser(h, user))
                .body(ClearingRequestConverter.fromThrift(clearingRequest))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class)));
    }

    @Override
    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user) throws TException {
        return call(() -> toThriftReleaseClearingStatusData(restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-status").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_CLEARING_STATUS_DATA_LIST)));
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, User user)
            throws TException {
        return call(() -> toThriftReleaseClearingStatusData(restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-status/accessible").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_CLEARING_STATUS_DATA_LIST)));
    }

    @Override
    public List<Project> fillClearingStateSummary(List<Project> projects, User user) throws TException {
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/clearing-state/fill")
                .headers(h -> addUser(h, user))
                .body(toPojoProjects(projects))
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user)
            throws TException {
        return call(() -> toThriftProjects(restClient.post()
                .uri(BASE + "/clearing-state/fill-with-subprojects")
                .headers(h -> addUser(h, user))
                .body(toPojoProjects(projects))
                .retrieve()
                .body(PROJECT_LIST)));
    }

    @Override
    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(Project project, User user)
            throws TException {
        return call(() -> ProjectConverter.toThrift(restClient.post()
                .uri(BASE + "/clearing-state/fill-single")
                .headers(h -> addUser(h, user))
                .body(ProjectConverter.fromThrift(project))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class)));
    }

    @Override
    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, User user)
            throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-state-list").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    }

    @Override
    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, User user)
            throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-state-list/accessible").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    }

    @Override
    public RequestStatus exportForMonitoringList() throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/export/monitoring")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Linked projects ------------------------------------------------------------------------

    @Override
    public List<ProjectLink> getLinkedProjectsOfProject(Project project, boolean deep, User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("project", ProjectConverter.fromThrift(project));
        body.put("deep", deep);
        return call(() -> toThriftProjectLinks(restClient.post()
                .uri(BASE + "/linked")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsById(String id, boolean deep, User user) throws TException {
        return call(() -> toThriftProjectLinks(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/linked").queryParam("deep", deep).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    @Override
    public List<ProjectLink> getLinkedProjects(Map<String, ProjectProjectRelationship> relations, boolean depth,
            User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("relations", toPojoRelationships(relations));
        body.put("deep", depth);
        return call(() -> toThriftProjectLinks(restClient.post()
                .uri(BASE + "/linked/by-relations")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsWithoutReleases(Map<String, ProjectProjectRelationship> relations,
            boolean depth, User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("relations", toPojoRelationships(relations));
        body.put("deep", depth);
        return call(() -> toThriftProjectLinks(restClient.post()
                .uri(BASE + "/linked/by-relations/no-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(Project project, boolean deep, User user)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("project", ProjectConverter.fromThrift(project));
        body.put("deep", deep);
        return call(() -> toThriftProjectLinks(restClient.post()
                .uri(BASE + "/linked/no-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(Project project, boolean deep, User user)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("project", ProjectConverter.fromThrift(project));
        body.put("deep", deep);
        return call(() -> toThriftProjectLinks(restClient.post()
                .uri(BASE + "/linked/all-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST)));
    }

    // ---- Obligations ----------------------------------------------------------------------------

    @Override
    public ObligationList getLinkedObligations(String obligationId, User user) throws TException {
        return call(() -> ObligationListConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/obligations/{obligationId}").build(obligationId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ObligationList.class)));
    }

    @Override
    public RequestStatus addLinkedObligations(ObligationList obligation, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(ObligationListConverter.fromThrift(obligation))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateLinkedObligations(ObligationList obligation, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(ObligationListConverter.fromThrift(obligation))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Release relations usage ----------------------------------------------------------------

    @Override
    public void addReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        callVoid(() -> restClient.post()
                .uri(BASE + "/release-relations")
                .body(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void updateReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        callVoid(() -> restClient.put()
                .uri(BASE + "/release-relations")
                .body(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations))
                .retrieve()
                .toBodilessEntity());
    }

    public void deleteReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        callVoid(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/release-relations")
                .body(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) throws TException {
        return call(() -> toThriftUsedReleaseRelations(restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/release-relations").build(projectId))
                .retrieve()
                .body(USED_RELEASE_RELATIONS_LIST)));
    }

    // ---- Import / Export ------------------------------------------------------------------------

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/import-bom")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId,
            String projectId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/import-cyclonedx")
                        .queryParam("attachmentContentId", attachmentContentId)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(User user,
            String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/import-cyclonedx/no-replace")
                        .queryParam("attachmentContentId", attachmentContentId)
                        .queryParam("projectId", projectId)
                        .queryParam("doNotReplace", doNotReplacePackageAndRelease).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases,
            User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/export-cyclonedx")
                        .queryParam("projectId", projectId)
                        .queryParam("bomType", bomType)
                        .queryParam("includeSubProjReleases", includeSubProjReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/sbom-import-info")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .retrieve()
                .body(String.class));
    }

    // ---- Reports / Mail -------------------------------------------------------------------------

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        callVoid(() -> restClient.post()
                .uri(b -> b.path(BASE + "/export-mail")
                        .queryParam("url", url)
                        .queryParam("recepient", recepient).build())
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(b -> b.path(BASE + "/download-excel")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("token", token).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class)));
    }

    @Override
    public ByteBuffer getReportDataStream(User user, boolean extendedByReleases, String projectId) throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(b -> b.path(BASE + "/report-stream")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class)));
    }

    @Override
    public String getReportInEmail(User user, boolean extendedByReleases, String projectId) throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-email")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(String.class));
    }

    // ---- Release links --------------------------------------------------------------------------

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, User user)
            throws TException {
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(b -> b.path(BASE + "/{projectId}/release-links/by-trace").build(projectId))
                .headers(h -> addUser(h, user))
                .body(trace)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath,
            User user) throws TException {
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(b -> b.path(BASE + "/{projectId}/release-links/by-index").build(projectId))
                .headers(h -> addUser(h, user))
                .body(indexPath)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, User sw360User)
            throws TException {
        return call(() -> toThriftReleaseNodes(restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/dependency-releases").build(projectId))
                .headers(h -> addUser(h, sw360User))
                .retrieve()
                .body(RELEASE_NODE_LIST)));
    }

    // ---- Dependency network ---------------------------------------------------------------------

    @Override
    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, User user)
            throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/dependency-network").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    }

    // ---- Misc -----------------------------------------------------------------------------------

    @Override
    public Map<String, List<String>> getDuplicateProjects() throws TException {
        Map<String, List<String>> result = call(() -> restClient.get()
                .uri(BASE + "/duplicates")
                .retrieve()
                .body(DUPLICATE_MAP));
        return result != null ? result : new HashMap<>();
    }

    @Override
    public boolean projectIsUsed(String projectId) throws TException {
        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/in-use").build(projectId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/cyclic-path")
                .headers(h -> addUser(h, user))
                .body(ProjectConverter.fromThrift(project))
                .retrieve()
                .body(String.class));
    }

    @Override
    public RequestStatus removeAttachmentFromProject(String projectId, User user, String attachmentContentId)
            throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{projectId}/attachment/{attachmentContentId}")
                        .build(projectId, attachmentContentId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public Set<String> getGroups() throws TException {
        Set<String> result = call(() -> restClient.get()
                .uri(BASE + "/groups")
                .retrieve()
                .body(STRING_SET));
        return result != null ? result : new HashSet<>();
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private static <T> T call(Supplier<T> supplier) throws TException {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body)
                    .setErrorCode(e.getStatusCode().value());
        }
    }

    private static void callVoid(Runnable runnable) throws TException {
        try {
            runnable.run();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body)
                    .setErrorCode(e.getStatusCode().value());
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        headers.set("X-User-Email", user.getEmail());
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        headers.set("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    private static void addPaginationParams(UriBuilder ub, PaginationData pageData) {
        if (pageData != null) {
            ub.queryParam("displayStart", pageData.getDisplayStart());
            ub.queryParam("rowsPerPage", pageData.getRowsPerPage());
            ub.queryParam("ascending", pageData.isAscending());
            ub.queryParam("sortColumnNumber", pageData.getSortColumnNumber());
        }
    }

    private static ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes == null ? new byte[0] : bytes);
    }

    private Map<PaginationData, List<Project>> toPaginatedMap(
            org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result,
            PaginationData fallback) {
        Map<PaginationData, List<Project>> map = new HashMap<>();
        if (result != null) {
            PaginationData thriftPage = result.getPaginationData() != null
                    ? PaginationDataConverter.toThrift(result.getPaginationData())
                    : (fallback != null ? fallback : new PaginationData());
            map.put(thriftPage, toThriftProjects(result.getData()));
        }
        return map;
    }

    // ---- Project conversion helpers -------------------------------------------------------------

    private static List<Project> toThriftProjects(
            List<org.eclipse.sw360.datahandler.services.projects.Project> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ProjectConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.projects.Project> toPojoProjects(
            List<Project> thrifts) {
        if (thrifts == null) {
            return new ArrayList<>();
        }
        return thrifts.stream().map(ProjectConverter::fromThrift).collect(Collectors.toList());
    }

    private static Set<Project> toThriftProjectSet(
            Set<org.eclipse.sw360.datahandler.services.projects.Project> pojos) {
        if (pojos == null) {
            return new HashSet<>();
        }
        return pojos.stream().map(ProjectConverter::toThrift).collect(Collectors.toSet());
    }

    // ---- ProjectLink conversion helpers ---------------------------------------------------------

    private static List<ProjectLink> toThriftProjectLinks(
            List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ProjectLinkConverter::toThrift).collect(Collectors.toList());
    }

    // ---- Release conversion helpers -------------------------------------------------------------

    private static List<ReleaseClearingStatusData> toThriftReleaseClearingStatusData(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseClearingStatusDataConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseLink> toThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseLinkConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseNode> toThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseNodeConverter::toThrift).collect(Collectors.toList());
    }

    // ---- UsedReleaseRelations conversion helpers ------------------------------------------------

    private static List<UsedReleaseRelations> toThriftUsedReleaseRelations(
            List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(UsedReleaseRelationsConverter::toThrift).collect(Collectors.toList());
    }

    // ---- ProjectProjectRelationship conversion helpers ------------------------------------------

    private static Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship>
            toPojoRelationships(Map<String, ProjectProjectRelationship> thriftMap) {
        if (thriftMap == null) {
            return new HashMap<>();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectProjectRelationshipConverter.fromThrift(e.getValue())
        ));
    }
}
