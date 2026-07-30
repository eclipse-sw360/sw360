/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.projects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
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
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link ProjectClient}.
 *
 * Maps to {@code ProjectController} under {@code /projects/api/projects}.
 */
public class ProjectServiceRestClient implements ProjectClient {

    private static final String BASE = "/projects/api/projects";

    private static final ParameterizedTypeReference<List<Project>> PROJECT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<Project>> PROJECT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ProjectLink>> PROJECT_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ReleaseClearingStatusData>> RELEASE_CLEARING_STATUS_DATA_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ReleaseLink>> RELEASE_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ReleaseNode>> RELEASE_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<UsedReleaseRelations>> USED_RELEASE_RELATIONS_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, List<String>>> DUPLICATE_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, String>>> LIST_MAP_STRING_STRING =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<Project>> PROJECT_PAGE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ProjectServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void callVoid(Runnable runnable) {
        try {
            runnable.run();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> search(String text) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.get()
                .uri(b -> {
                    var ub = b.path(BASE + "/search");
                    if (text != null) {
                        ub.queryParam("text", text);
                    }
                    return ub.build();
                })
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/search/refined")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> refineSearchPageable(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData paginationData) {

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        body.put("paginationData", paginationData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/refined/paginated")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions) {

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions);
        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/search/refined/no-user")
                .body(body)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> searchByName(String name, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-name").queryParam("name", name).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByNamePrefixPaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("paginationData", pageData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/by-name-prefix")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
    
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByExactNamePaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("paginationData", pageData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-name")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
    
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchAccessibleProjectByExactValues(Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {

        Map<String, Object> body = new HashMap<>();
        body.put("subQueryRestrictions", subQueryRestrictions);
        body.put("paginationData", pageData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-values")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(PROJECT_PAGE));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.ProjectData searchByGroup(String group, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-group").queryParam("group", group).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.ProjectData searchByTag(String tag, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-tag").queryParam("tag", tag).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.ProjectData searchByType(String type, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-type").queryParam("type", type).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ProjectData.class));
    
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByReleaseId(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-release/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByReleaseIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.post()
                .uri(BASE + "/by-release-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByPackageId(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-package/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByPackageIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.post()
                .uri(BASE + "/by-package-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public int getProjectCountByPackageId(String id) {

        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/count/by-package/{id}").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchLinkingProjects(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/linking").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByExternalIds(Map<String, Set<String>> externalIds, org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.post()
                .uri(BASE + "/search-by-external-ids")
                .headers(h -> addUser(h, user))
                .body(externalIds)
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.Project getProjectById(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.Project getProjectByIdIgnoringVisibility(String id) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/ignore-visibility").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> getProjectsById(List<String> id, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/by-ids")
                .headers(h -> addUser(h, user))
                .body(id)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.Project getProjectByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> getMyProjects(org.eclipse.sw360.datahandler.services.users.User user, Map<String, Boolean> userRoles) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/my")
                .headers(h -> addUser(h, user))
                .body(userRoles)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjectsSummary(org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.get()
                .uri(BASE + "/accessible/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjectsSummaryWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {

        return call(() -> restClient.post()
                        .uri(BASE + "/accessible/summary/paginated")
                        .headers(h -> addUser(h, user))
                        .body(pageData)
                        .retrieve()
                        .body(PROJECT_PAGE));
    
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjects(org.eclipse.sw360.datahandler.services.users.User user) {

        Set<org.eclipse.sw360.datahandler.services.projects.Project> set = call(() -> restClient.get()
                .uri(BASE + "/accessible")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public int getMyAccessibleProjectCounts(org.eclipse.sw360.datahandler.services.users.User user) {

        Integer count = call(() -> restClient.get()
                .uri(BASE + "/my-count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    
    }

    @Override
    public int getCountByReleaseIds(Set<String> ids) {

        Integer count = call(() -> restClient.post()
                .uri(BASE + "/count/by-release-ids")
                .body(ids)
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    
    }

    @Override
    public int getCountByProjectId(String id) {

        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/count/{id}").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.post()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(project)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(project)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public RequestStatus updateProjectWithForceFlag(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate) {

        return call(() -> restClient.put()
                .uri(b -> b.path(BASE + "/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(project)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateProjectFromModerationRequest(
            org.eclipse.sw360.datahandler.services.projects.Project projectAdditions,
            org.eclipse.sw360.datahandler.services.projects.Project projectDeletions,
            org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", projectAdditions);
        body.put("deletions", projectDeletions);
        return call(() -> restClient.put()
                .uri(BASE + "/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteProject(String id, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteProjectWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete) {

        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary createClearingRequest(org.eclipse.sw360.datahandler.services.projects.ClearingRequest clearingRequest, org.eclipse.sw360.datahandler.services.users.User user, String projectUrl) {

        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/clearing-request").queryParam("projectUrl", projectUrl).build())
                .headers(h -> addUser(h, user))
                .body(clearingRequest)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-status").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_CLEARING_STATUS_DATA_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(
            String projectId, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-status/accessible").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_CLEARING_STATUS_DATA_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> fillClearingStateSummary(List<org.eclipse.sw360.datahandler.services.projects.Project> projects, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/clearing-state/fill")
                .headers(h -> addUser(h, user))
                .body(projects)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.Project> fillClearingStateSummaryIncludingSubprojects(List<org.eclipse.sw360.datahandler.services.projects.Project> projects, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.Project> list = call(() -> restClient.post()
                .uri(BASE + "/clearing-state/fill-with-subprojects")
                .headers(h -> addUser(h, user))
                .body(projects)
                .retrieve()
                .body(PROJECT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.post()
                .uri(BASE + "/clearing-state/fill-single")
                .headers(h -> addUser(h, user))
                .body(project)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.Project.class));
    
    }

    @Override
    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-state-list").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    
    }

    @Override
    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/clearing-state-list/accessible").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus exportForMonitoringList() {

        return call(() -> restClient.post()
                .uri(BASE + "/export/monitoring")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProject(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("project", project);
        body.put("deep", deep);
        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.post()
                .uri(BASE + "/linked")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsById(String id, boolean deep, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/linked").queryParam("deep", deep).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjects(Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship> relations, boolean depth, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("relations", relations);
        body.put("deep", depth);
        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.post()
                .uri(BASE + "/linked/by-relations")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsWithoutReleases(Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship> relations, boolean depth, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("relations", relations);
        body.put("deep", depth);
        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.post()
                .uri(BASE + "/linked/by-relations/no-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProjectWithoutReleases(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("project", project);
        body.put("deep", deep);
        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.post()
                .uri(BASE + "/linked/no-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProjectWithAllReleases(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user) {

        Map<String, Object> body = new HashMap<>();
        body.put("project", project);
        body.put("deep", deep);
        List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> list = call(() -> restClient.post()
                .uri(BASE + "/linked/all-releases")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(PROJECT_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.projects.ObligationList getLinkedObligations(String obligationId, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligations/{obligationId}").build(obligationId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.projects.ObligationList.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus addLinkedObligations(org.eclipse.sw360.datahandler.services.projects.ObligationList obligation, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.post()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(obligation)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateLinkedObligations(org.eclipse.sw360.datahandler.services.projects.ObligationList obligation, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.put()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(obligation)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public void addReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations) {

        callVoid(() -> restClient.post()
                .uri(BASE + "/release-relations")
                .body(usedReleaseRelations)
                .retrieve()
                .toBodilessEntity());
    
    }

    @Override
    public void updateReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations) {

        callVoid(() -> restClient.put()
                .uri(BASE + "/release-relations")
                .body(usedReleaseRelations)
                .retrieve()
                .toBodilessEntity());
    
    }

    @Override
    public void deleteReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations) {
        callVoid(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/release-relations")
                .body(usedReleaseRelations)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) {

        List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/release-relations").build(projectId))
                .retrieve()
                .body(USED_RELEASE_RELATIONS_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary importBomFromAttachmentContent(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId) {

        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/import-bom")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary importCycloneDxFromAttachmentContent(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId, String projectId) {

        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/import-cyclonedx")
                        .queryParam("attachmentContentId", attachmentContentId)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) {

        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/import-cyclonedx/no-replace")
                        .queryParam("attachmentContentId", attachmentContentId)
                        .queryParam("projectId", projectId)
                        .queryParam("doNotReplace", doNotReplacePackageAndRelease).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/export-cyclonedx")
                        .queryParam("projectId", projectId)
                        .queryParam("bomType", bomType)
                        .queryParam("includeSubProjReleases", includeSubProjReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    
    }

    @Override
    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/sbom-import-info")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .retrieve()
                .body(String.class));
    
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) {

        callVoid(() -> restClient.post()
                .uri(b -> b.path(BASE + "/export-mail")
                        .queryParam("url", url)
                        .queryParam("recepient", recepient).build())
                .retrieve()
                .toBodilessEntity());
    
    }

    @Override
    public byte[] downloadExcel(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String token) {

        byte[] bytes = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/download-excel")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("token", token).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public byte[] getReportDataStream(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String projectId) {

        byte[] bytes = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-stream")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public String getReportInEmail(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String projectId) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-email")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("projectId", projectId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(String.class));
    
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/{projectId}/release-links/by-trace").build(projectId))
                .headers(h -> addUser(h, user))
                .body(trace)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath, org.eclipse.sw360.datahandler.services.users.User user) {

        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/{projectId}/release-links/by-index").build(projectId))
                .headers(h -> addUser(h, user))
                .body(indexPath)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, org.eclipse.sw360.datahandler.services.users.User sw360User) {

        List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/dependency-releases").build(projectId))
                .headers(h -> addUser(h, sw360User))
                .retrieve()
                .body(RELEASE_NODE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{projectId}/dependency-network").build(projectId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(LIST_MAP_STRING_STRING));
    
    }

    @Override
    public Map<String, List<String>> getDuplicateProjects() {

        Map<String, List<String>> result = call(() -> restClient.get()
                .uri(BASE + "/duplicates")
                .retrieve()
                .body(DUPLICATE_MAP));
        return result != null ? result : new HashMap<>();
    
    }

    @Override
    public boolean projectIsUsed(String projectId) {

        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/in-use").build(projectId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    
    }

    @Override
    public String getCyclicLinkedProjectPath(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user) {

        return call(() -> restClient.post()
                .uri(BASE + "/cyclic-path")
                .headers(h -> addUser(h, user))
                .body(project)
                .retrieve()
                .body(String.class));
    
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus removeAttachmentFromProject(String projectId, org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId) {

        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{projectId}/attachment/{attachmentContentId}")
                        .build(projectId, attachmentContentId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    
    }

    @Override
    public Set<String> getGroups() {

        Set<String> result = call(() -> restClient.get()
                .uri(BASE + "/groups")
                .retrieve()
                .body(STRING_SET));
        return result != null ? result : Set.of();
    
    }
}
