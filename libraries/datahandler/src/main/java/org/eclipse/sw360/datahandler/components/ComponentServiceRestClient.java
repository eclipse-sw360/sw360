/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link ComponentClient}.
 *
 * Maps to {@code ComponentController} under {@code /components/api/components}.
 */
public class ComponentServiceRestClient implements ComponentClient {

    private static final String BASE = "/components/api/components";

    private static final ParameterizedTypeReference<List<Component>> COMPONENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Release>> RELEASE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<Component>> COMPONENT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<Release>> RELEASE_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ReleaseLink>> RELEASE_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ReleaseNode>> RELEASE_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, List<String>>> STRING_LIST_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<Attachment>> ATTACHMENT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<Component>> COMPONENT_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<Release>> RELEASE_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<String>> STRING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ComponentServiceRestClient(RestClient restClient) {
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

    private static Map<String, Object> nameSearchBody(String name, PaginationData pageData) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("pageData", pageData);
        return body;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getComponentsShort(Set<String> ids) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/short").queryParam("ids", ids).build())
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getComponentSummary(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(BASE + "/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getRecentComponentsSummary(int limit, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/recent").queryParam("limit", limit).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getAccessibleRecentComponentsSummary(int limit, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/accessible/recent").queryParam("limit", limit).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public int getTotalComponentsCount(org.eclipse.sw360.datahandler.services.users.User user) {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public int getAccessibleTotalComponentsCount(org.eclipse.sw360.datahandler.services.users.User user) {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/accessible/count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleaseSummary(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleaseSummary(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/accessible/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/accessible/paginated")
                 .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(RELEASE_PAGE));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.post()
                .uri(BASE + "/search")
                .body(body)
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> refineSearchAccessibleComponents(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        body.put("pageData", pageData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/accessible/paginated")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(COMPONENT_PAGE));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> refineSearchWithAccessibility(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.post()
                .uri(BASE + "/search/accessible")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getMyComponents(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(BASE + "/my")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> searchAccessibleReleases(String searchText, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/search/accessible")
                 .headers(h -> addUser(h, user))
                .body(nameSearchBody(searchText, pageData))
                .retrieve()
                .body(RELEASE_PAGE));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> searchReleaseByNamePrefix(String name) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/search").queryParam("name", name).build())
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> searchReleaseByNamePaginated(String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/search/paginated")
                
                .body(nameSearchBody(name != null ? name : "", pageData))
                .retrieve()
                .body(RELEASE_PAGE));
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleNewReleasesWithSrc(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/accessible/new-with-src")
                 .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(RELEASE_PAGE));
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByNamePrefixPaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/search/by-name-prefix")
                 .headers(h -> addUser(h, user))
                .body(nameSearchBody(name, pageData))
                .retrieve()
                .body(COMPONENT_PAGE));
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByExactNamePaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/search/by-exact-name")
                 .headers(h -> addUser(h, user))
                .body(nameSearchBody(name, pageData))
                .retrieve()
                .body(COMPONENT_PAGE));
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByExactValues(Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        Map<String, Object> body = new HashMap<>();
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        body.put("pageData", pageData);
        return call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-values")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(COMPONENT_PAGE));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getSubscribedComponents(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(BASE + "/subscribed")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getSubscribedReleases(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/subscribed")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getRecentReleases() {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/recent")
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getRecentReleasesWithAccessibility(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/recent/accessible")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component getComponentById(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component getAccessibleComponentById(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/accessible/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component getComponentByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component getAccessibleComponentByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/accessible/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Release getReleaseById(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Release getAccessibleReleaseById(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/accessible/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Release getReleaseByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Release getAccessibleReleaseByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/accessible/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByIdsForExport(Set<String> ids) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/by-ids/export")
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<String> getReleaseIdsFromComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> {
            List<String> result = restClient.get()
                    .uri(b -> b.path(BASE + "/releases/ids-by-component/{id}").build(id))
                    .headers(h -> addUser(h, user))
                    .retrieve()
                    .body(STRING_LIST);
            return result != null ? result : new ArrayList<>();
        });
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/by-ids/export/accessible")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/accessible/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getFullReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/full/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesWithPermissions(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/with-permissions")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromVendorId(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/by-vendor/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromVendorIds(Set<String> ids) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/by-vendor-ids")
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesFromVendorIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/accessible/by-vendor-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByVendorId(String vendorId) {
        Set<org.eclipse.sw360.datahandler.services.components.Release> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/set-by-vendor/{vendorId}").build(vendorId))
                .retrieve()
                .body(RELEASE_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addComponent(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(component)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases")
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponent(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(component)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponentWithForceFlag(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate) {
        return call(() -> restClient.put()
                .uri(b -> b.path(BASE + "/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(component)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary updateComponents(Set<org.eclipse.sw360.datahandler.services.components.Component> components, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/bulk")
                .headers(h -> addUser(h, user))
                .body(components)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponentFromModerationRequest(org.eclipse.sw360.datahandler.services.components.Component componentAdditions, org.eclipse.sw360.datahandler.services.components.Component componentDeletions, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", componentAdditions);
        body.put("deletions", componentDeletions);
        return call(() -> restClient.put()
                .uri(BASE + "/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus mergeComponents(String componentTargetId, String componentSourceId, org.eclipse.sw360.datahandler.services.components.Component componentSelection, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/merge")
                        .queryParam("targetId", componentTargetId)
                        .queryParam("sourceId", componentSourceId)
                        .build())
                .headers(h -> addUser(h, user))
                .body(componentSelection)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/releases")
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseWithForceFlag(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate) {
        return call(() -> restClient.put()
                .uri(b -> b.path(BASE + "/releases/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseFossology(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/releases/fossology")
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary updateReleases(Set<org.eclipse.sw360.datahandler.services.components.Release> releases, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/releases/bulk")
                .headers(h -> addUser(h, user))
                .body(releases)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestSummary updateReleasesDirectly(Set<org.eclipse.sw360.datahandler.services.components.Release> releases, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/releases/bulk/direct")
                .headers(h -> addUser(h, user))
                .body(releases)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseFromModerationRequest(org.eclipse.sw360.datahandler.services.components.Release releaseAdditions, org.eclipse.sw360.datahandler.services.components.Release releaseDeletions, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", releaseAdditions);
        body.put("deletions", releaseDeletions);
        return call(() -> restClient.put()
                .uri(BASE + "/releases/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, org.eclipse.sw360.datahandler.services.components.Release releaseSelection, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/releases/merge")
                        .queryParam("targetId", releaseTargetId)
                        .queryParam("sourceId", releaseSourceId)
                        .build())
                .headers(h -> addUser(h, user))
                .body(releaseSelection)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReferencingReleases(String releaseId) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/referencing").build(releaseId))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteComponent(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteComponentWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteRelease(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus deleteReleaseWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/by-component/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFullDocsFromComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/full-docs/by-component/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromComponentIdWithPagination(String id, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                        .uri(b -> b.path(BASE + "/releases/by-component/{id}/paginated").build(id))
                        .headers(h -> addUser(h, user))
                        .body(pageData)
                        .retrieve()
                        .body(RELEASE_PAGE));
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsForRelease(String releaseId) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/using-components").build(releaseId))
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, org.eclipse.sw360.datahandler.services.users.User user) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/using-components/accessible").build(releaseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsForComponent(Set<String> releaseIds) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.post()
                .uri(BASE + "/using-components")
                .body(releaseIds)
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, org.eclipse.sw360.datahandler.services.users.User user) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.post()
                .uri(BASE + "/using-components/accessible")
                .headers(h -> addUser(h, user))
                .body(releaseIds)
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> getComponentsByDefaultVendorId(String defaultVendorId) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-vendor/{id}").build(defaultVendorId))
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public boolean releaseIsUsed(String releaseId) {
        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/in-use").build(releaseId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public boolean componentIsUsed(String componentId) {
        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/in-use").build(componentId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component recomputeReleaseDependentFields(String componentId, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/{id}/recompute").build(componentId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.BulkOperationNode deleteBulkRelease(String releaseId, org.eclipse.sw360.datahandler.services.users.User user, boolean isPreview) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}/bulk")
                        .queryParam("preview", isPreview).build(releaseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.BulkOperationNode.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus subscribeComponent(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/{id}/subscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus subscribeRelease(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/releases/{id}/subscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus unsubscribeComponent(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/{id}/unsubscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus unsubscribeRelease(String id, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/releases/{id}/unsubscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getComponentSummaryForExport() {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(BASE + "/summary/export")
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> getComponentDetailedSummaryForExport() {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(BASE + "/detailed-summary/export")
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Component> searchComponentForExport(String name, boolean caseSensitive) {
        List<org.eclipse.sw360.datahandler.services.components.Component> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/search-export")
                        .queryParam("name", name)
                        .queryParam("caseSensitive", caseSensitive)
                        .build())
                .retrieve()
                .body(COMPONENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public org.eclipse.sw360.datahandler.services.components.Component getComponentForReportFromFossologyUploadId(String uploadId) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/by-fossology-upload/{uploadId}").build(uploadId))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class));
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> getSourceAttachments(String releaseId) {
        Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> set = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/source-attachments").build(releaseId))
                .retrieve()
                .body(ATTACHMENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleases(Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> relations) {
        Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> pojoRelations =
                relations;
        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(BASE + "/releases/linked")
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> relations, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> pojoRelations =
                relations;
        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(BASE + "/releases/linked/accessible")
                .headers(h -> addUser(h, user))
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleaseRelations(Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> relations) {
        Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> pojoRelations =
                relations;
        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(BASE + "/releases/linked-relations")
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> relations, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> pojoRelations =
                relations;
        List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> list = call(() -> restClient.post()
                .uri(BASE + "/releases/linked-relations/accessible")
                .headers(h -> addUser(h, user))
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public Set<String> getUsedAttachmentContentIds() {
        return call(() -> {
            Set<String> result = restClient.get()
                    .uri(BASE + "/used-attachment-ids")
                    .retrieve()
                    .body(STRING_SET);
            return result != null ? result : new HashSet<>();
        });
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleasesWithSvmTrackingFeedback() {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/svm-feedback")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.RequestStatus uploadSourceCodeAttachmentToReleases() {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/upload-source")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public Map<String, List<String>> getDuplicateComponents() {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/duplicates")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Map<String, List<String>> getDuplicateReleases() {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/releases/duplicates")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Map<String, List<String>> getDuplicateReleaseSources() {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/releases/duplicate-sources")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) {
        Set<org.eclipse.sw360.datahandler.services.components.Component> set = call(() -> restClient.post()
                .uri(BASE + "/search-by-external-ids")
                .body(externalIds)
                .retrieve()
                .body(COMPONENT_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public Set<org.eclipse.sw360.datahandler.services.components.Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) {
        Set<org.eclipse.sw360.datahandler.services.components.Release> set = call(() -> restClient.post()
                .uri(BASE + "/releases/search-by-external-ids")
                .body(externalIds)
                .retrieve()
                .body(RELEASE_SET));
        return set == null ? Set.of() : set;
    }

    @Override
    public String getCyclicLinkedReleasePath(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/cyclic-path")
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(String.class));
    }

    @Override
    public org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation prepareImportBom(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/import-bom/prepare")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation.class));
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
    public org.eclipse.sw360.datahandler.services.common.RequestStatus splitComponent(org.eclipse.sw360.datahandler.services.components.Component srcComponent, org.eclipse.sw360.datahandler.services.components.Component targetComponent, org.eclipse.sw360.datahandler.services.users.User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("srcComponent", srcComponent);
        body.put("targetComponent", targetComponent);
        return call(() -> restClient.post()
                .uri(BASE + "/split")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getAllReleasesForUser(org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.get()
                .uri(BASE + "/releases/all")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> getRecentComponentsSummaryWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData) {
        return call(() -> restClient.post()
                .uri(BASE + "/recent/paginated")
                 .headers(h -> addUser(h, user))
                .body(pageData)
                .retrieve()
                .body(COMPONENT_PAGE));
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) {
        Map<String, Object> body = new HashMap<>();
        body.put("url", url);
        body.put("recipient", recepient);
        callVoid(() -> {
            restClient.post()
                    .uri(BASE + "/export-mail")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            });
    }

    @Override
    public byte[] downloadExcel(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String token) {
        byte[] bytes = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/download-excel")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("token", token)
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public byte[] getComponentReportDataStream(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases) {
        byte[] bytes = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-stream")
                        .queryParam("extendedByReleases", extendedByReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public String getComponentReportInEmail(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-email")
                        .queryParam("extendedByReleases", extendedByReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(String.class));
    }

    @Override
    public boolean isReleaseActionAllowed(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user, RequestedAction action) {
        Boolean result = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/releases/action-allowed")
                        .queryParam("action", action.name()).build())
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByListIds(List<String> ids, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.Release> list = call(() -> restClient.post()
                .uri(BASE + "/releases/by-list-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> getReleaseRelationNetworkOfRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user) {
        List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> list = call(() -> restClient.post()
                .uri(BASE + "/releases/relation-network")
                .headers(h -> addUser(h, user))
                .body(release)
                .retrieve()
                .body(RELEASE_NODE_LIST));
        return list == null ? List.of() : list;
    }
}
