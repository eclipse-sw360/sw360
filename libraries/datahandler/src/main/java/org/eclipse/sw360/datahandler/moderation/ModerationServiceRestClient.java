/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.moderation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.Comment;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.moderation.AcceptModerationRequest;
import org.eclipse.sw360.datahandler.services.moderation.ClearingSearchRequest;
import org.eclipse.sw360.datahandler.services.moderation.ExactValuesSearchRequest;
import org.eclipse.sw360.datahandler.services.moderation.ModeratorPageRequest;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.moderation.RefineSearchRequest;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link ModerationClient}.
 * 
 * Each public method maps one-to-one to an endpoint on the moderation Spring Boot WAR
 * ({@code ModerationController} under {@code /moderation/api/moderation}). User identity
 * is sent with {@code X-User-Email} / {@code X-User-Department} / {@code X-User-Group}
 * headers. Transport and HTTP errors are turned into {@link SW360Exception} so callers
 * can handle them the same way they used to handle Thrift failures.
 */
public class ModerationServiceRestClient implements ModerationClient {

    private static final String BASE = "/moderation/api/moderation";

    private static final ParameterizedTypeReference<List<ModerationRequest>> MR_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<ClearingRequest>> CR_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<ModerationRequest>> MR_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<ClearingRequest>> CR_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Long>> STRING_LONG_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<RequestStatus> REQUEST_STATUS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<RemoveModeratorRequestStatus> REMOVE_STATUS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ModerationServiceRestClient(RestClient restClient) {
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
            // Connection refused / timeouts (e.g. unit tests without a live moderation WAR)
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void callVoid(Runnable runnable) {
        call(() -> {
            runnable.run();
            return null;
        });
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

    private RequestStatus postEntityStatus(String path, Object body, User user) {
        return call(() -> restClient.post()
                .uri(BASE + path)
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(REQUEST_STATUS));
    }

    private void postEntityVoid(String path, Object body, User user) {
        callVoid(() -> restClient.post()
                .uri(BASE + path)
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RequestStatus createComponentRequest(Component component, User user) {
        return postEntityStatus("/requests/component", component, user);
    }

    @Override
    public RequestStatus createReleaseRequest(Release release, User user) {
        return postEntityStatus("/requests/release", release, user);
    }

    @Override
    public RequestStatus createReleaseRequestForEcc(Release release, User user) {
        return postEntityStatus("/requests/release/ecc", release, user);
    }

    @Override
    public RequestStatus createProjectRequest(Project project, User user) {
        return postEntityStatus("/requests/project", project, user);
    }

    @Override
    public RequestStatus createLicenseRequest(License license, User user) {
        return postEntityStatus("/requests/license", license, user);
    }

    @Override
    public RequestStatus createSPDXDocumentRequest(SPDXDocument spdx, User user) {
        return postEntityStatus("/requests/spdx-document", spdx, user);
    }

    @Override
    public RequestStatus createSpdxDocumentCreationInfoRequest(DocumentCreationInformation info, User user) {
        return postEntityStatus("/requests/spdx-document-creation-info", info, user);
    }

    @Override
    public RequestStatus createSpdxPackageInfoRequest(PackageInformation info, User user) {
        return postEntityStatus("/requests/spdx-package-info", info, user);
    }

    @Override
    public void createUserRequest(User user) {
        postEntityVoid("/requests/user", user, null);
    }

    @Override
    public void createComponentDeleteRequest(Component component, User user) {
        postEntityVoid("/requests/component/delete", component, user);
    }

    @Override
    public void createReleaseDeleteRequest(Release release, User user) {
        postEntityVoid("/requests/release/delete", release, user);
    }

    @Override
    public void createProjectDeleteRequest(Project project, User user) {
        postEntityVoid("/requests/project/delete", project, user);
    }

    @Override
    public void createSPDXDocumentDeleteRequest(SPDXDocument spdx, User user) {
        postEntityVoid("/requests/spdx-document/delete", spdx, user);
    }

    @Override
    public void createSpdxDocumentCreationInfoDeleteRequest(DocumentCreationInformation info, User user) {
        postEntityVoid("/requests/spdx-document-creation-info/delete", info, user);
    }

    @Override
    public void createSpdxPackageInfoDeleteRequest(PackageInformation info, User user) {
        postEntityVoid("/requests/spdx-package-info/delete", info, user);
    }

    @Override
    public List<ModerationRequest> getModerationRequestByDocumentId(String documentId) {
        List<ModerationRequest> list = call(() -> restClient.get()
                .uri(BASE + "/requests/by-document/{id}", documentId)
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public ModerationRequest getModerationRequestById(String id) {
        return call(() -> restClient.get()
                .uri(BASE + "/requests/{id}", id)
                .retrieve()
                .body(ModerationRequest.class));
    }

    @Override
    public RequestStatus updateModerationRequest(ModerationRequest moderationRequest) {
        return call(() -> restClient.put()
                .uri(BASE + "/requests")
                .body(moderationRequest)
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public RequestStatus acceptRequest(ModerationRequest request, String moderationDecisionComment, String reviewer) {
        AcceptModerationRequest body = new AcceptModerationRequest()
                .setRequest(request)
                .setModerationDecisionComment(moderationDecisionComment)
                .setReviewer(reviewer);
        return call(() -> restClient.post()
                .uri(BASE + "/requests/accept")
                .body(body)
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public void refuseRequest(String requestId, String moderationDecisionComment, String reviewer) {
        callVoid(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/requests/{id}/refuse")
                        .queryParam("moderationDecisionComment", moderationDecisionComment)
                        .queryParam("reviewer", reviewer)
                        .build(requestId))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RemoveModeratorRequestStatus removeUserFromAssignees(String requestId, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/requests/{id}/remove-assignee", requestId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(REMOVE_STATUS));
    }

    @Override
    public void cancelInProgress(String requestId) {
        callVoid(() -> restClient.post()
                .uri(BASE + "/requests/{id}/cancel", requestId)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void setInProgress(String requestId, User user) {
        callVoid(() -> restClient.post()
                .uri(BASE + "/requests/{id}/in-progress", requestId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void deleteRequestsOnDocument(String documentId) {
        callVoid(() -> restClient.delete()
                .uri(BASE + "/requests/by-document/{id}", documentId)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RequestStatus deleteModerationRequest(String id, User user) {
        return call(() -> restClient.delete()
                .uri(BASE + "/requests/{id}", id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public List<ModerationRequest> getRequestsByModerator(User user) {
        List<ModerationRequest> list = call(() -> restClient.get()
                .uri(BASE + "/requests/by-moderator")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<ModerationRequest> getRequestsByModeratorWithPaginationNoFilter(User user, PaginationData pageData) {
        List<ModerationRequest> list = call(() -> restClient.post()
                .uri(BASE + "/requests/by-moderator/page-no-filter")
                .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<ModerationRequest> getRequestsByModeratorWithPagination(User user, PaginationData pageData,
            boolean open) {
        ModeratorPageRequest body = new ModeratorPageRequest().setPageData(pageData).setOpen(open);
        PaginatedResult<ModerationRequest> result = call(() -> restClient.post()
                .uri(BASE + "/requests/by-moderator/page")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(MR_PAGE));
        return result != null ? result : new PaginatedResult<>(pageData, List.of());
    }

    @Override
    public PaginatedResult<ModerationRequest> getRequestsByModeratorWithPaginationAllDetails(User user,
            PaginationData pageData, boolean open) {
        ModeratorPageRequest body = new ModeratorPageRequest().setPageData(pageData).setOpen(open);
        PaginatedResult<ModerationRequest> result = call(() -> restClient.post()
                .uri(BASE + "/requests/by-moderator/page-all-details")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(MR_PAGE));
        return result != null ? result : new PaginatedResult<>(pageData, List.of());
    }

    @Override
    public List<ModerationRequest> getRequestsByRequestingUser(User user) {
        List<ModerationRequest> list = call(() -> restClient.get()
                .uri(BASE + "/requests/by-requesting-user")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<ModerationRequest> getRequestsByRequestingUserWithPagination(User user, PaginationData pageData) {
        List<ModerationRequest> list = call(() -> restClient.post()
                .uri(BASE + "/requests/by-requesting-user/page")
                .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public String createClearingRequest(ClearingRequest clearingRequest, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/clearing-requests")
                .headers(h -> addUser(h, user))
                .body(clearingRequest)
                .retrieve()
                .body(String.class));
    }

    @Override
    public RequestStatus updateClearingRequest(ClearingRequest clearingRequest, User user, String projectUrl) {
        return call(() -> restClient.put()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/clearing-requests")
                        .queryParam("projectUrl", projectUrl)
                        .build())
                .headers(h -> addUser(h, user))
                .body(clearingRequest)
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public Set<ClearingRequest> getMyClearingRequests(User user) {
        Set<ClearingRequest> set = call(() -> restClient.get()
                .uri(BASE + "/clearing-requests/mine")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(CR_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<ClearingRequest> getClearingRequestsByBU(String businessUnit) {
        Set<ClearingRequest> set = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/clearing-requests/by-bu")
                        .queryParam("businessUnit", businessUnit)
                        .build())
                .retrieve()
                .body(CR_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public ClearingRequest getClearingRequestByProjectId(String projectId, User user) {
        return call(() -> restClient.get()
                .uri(BASE + "/clearing-requests/by-project/{id}", projectId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(ClearingRequest.class));
    }

    @Override
    public ClearingRequest getClearingRequestById(String id, User user) {
        return call(() -> restClient.get()
                .uri(BASE + "/clearing-requests/{id}", id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(ClearingRequest.class));
    }

    @Override
    public ClearingRequest getClearingRequestByIdForEdit(String id, User user) {
        return call(() -> restClient.get()
                .uri(BASE + "/clearing-requests/{id}/for-edit", id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(ClearingRequest.class));
    }

    @Override
    public RequestStatus deleteClearingRequest(String id, User user) {
        return call(() -> restClient.delete()
                .uri(BASE + "/clearing-requests/{id}", id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public void updateClearingRequestForProjectDeletion(Project project, User user) {
        postEntityVoid("/clearing-requests/project-deletion", project, user);
    }

    @Override
    public void updateClearingRequestForChangeInProjectBU(String crId, String businessUnit, User user) {
        callVoid(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/clearing-requests/{id}/business-unit")
                        .queryParam("businessUnit", businessUnit)
                        .build(crId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RequestStatus addCommentToClearingRequest(String id, Comment comment, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/clearing-requests/{id}/comments", id)
                .headers(h -> addUser(h, user))
                .body(comment)
                .retrieve()
                .body(REQUEST_STATUS));
    }

    @Override
    public PaginatedResult<ClearingRequest> getRecentClearingRequestsWithPagination(User user,
            PaginationData pageData) {
        PaginatedResult<ClearingRequest> result = call(() -> restClient.post()
                .uri(BASE + "/clearing-requests/recent/page")
                .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(CR_PAGE));
        return result != null ? result : new PaginatedResult<>(pageData, List.of());
    }

    @Override
    public PaginatedResult<ClearingRequest> searchClearingRequestsByFilters(User user,
            Map<String, Set<String>> filterMap, PaginationData pageData) {
        ClearingSearchRequest body = new ClearingSearchRequest()
                .setFilterMap(filterMap != null ? filterMap : Map.of())
                .setPageData(pageData);
        PaginatedResult<ClearingRequest> result = call(() -> restClient.post()
                .uri(BASE + "/clearing-requests/search")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(CR_PAGE));
        return result != null ? result : new PaginatedResult<>(pageData, List.of());
    }

    @Override
    public int getOpenCriticalCrCountByGroup(String group) {
        Integer count = call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path(BASE + "/clearing-requests/open-critical-count")
                        .queryParam("group", group)
                        .build())
                .retrieve()
                .body(Integer.class));
        return count == null ? 0 : count;
    }

    @Override
    public List<ModerationRequest> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData) {
        RefineSearchRequest body = new RefineSearchRequest()
                .setText(text)
                .setSubQueryRestrictions(subQueryRestrictions != null ? subQueryRestrictions : Map.of())
                .setPageData(pageData);
        List<ModerationRequest> list = call(() -> restClient.post()
                .uri(BASE + "/requests/refine-search")
                .body(body)
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<ModerationRequest> searchModerationRequestsByExactValues(
            Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        ExactValuesSearchRequest body = new ExactValuesSearchRequest()
                .setSubQueryRestrictions(subQueryRestrictions != null ? subQueryRestrictions : Map.of())
                .setPageData(pageData);
        List<ModerationRequest> list = call(() -> restClient.post()
                .uri(BASE + "/requests/search-by-exact-values")
                .body(body)
                .retrieve()
                .body(MR_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public Map<String, Long> getCountByModerationState(User user) {
        Map<String, Long> map = call(() -> restClient.get()
                .uri(BASE + "/requests/count/by-moderation-state")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(STRING_LONG_MAP));
        return map == null ? Map.of() : map;
    }

    @Override
    public Map<String, Long> getCountByRequester(User user) {
        Map<String, Long> map = call(() -> restClient.get()
                .uri(BASE + "/requests/count/by-requester")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(STRING_LONG_MAP));
        return map == null ? Map.of() : map;
    }

    @Override
    public Map<String, Long> getCountByModerationStateAndRequestingUser(User moderator, User requestingUser) {
        Map<String, Long> map = call(() -> restClient.post()
                .uri(BASE + "/requests/count/by-moderation-state-and-requesting-user")
                .headers(h -> addUser(h, moderator))
                .body(requestingUser)
                .retrieve()
                .body(STRING_LONG_MAP));
        return map == null ? Map.of() : map;
    }

    @Override
    public Set<String> getRequestingUserDepts() {
        Set<String> set = call(() -> restClient.get()
                .uri(BASE + "/requesting-user-depts")
                .retrieve()
                .body(STRING_SET));
        return set == null ? Set.of() : set;
    }
}
