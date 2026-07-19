/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.ReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.BulkOperationNodeConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thrift {@link ComponentService.Iface} adapter that delegates to the components REST backend
 * ({@code /components/api/components}). Keeps the Thrift contract intact for existing resource-server
 * callers while removing the Thrift transport.
 */
@org.springframework.stereotype.Component
public class ComponentServiceRestAdapter implements ComponentService.Iface {

    private static final String BASE = "/components/api/components";

    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.Component>> COMPONENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.Release>> RELEASE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<org.eclipse.sw360.datahandler.services.components.Component>> COMPONENT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<org.eclipse.sw360.datahandler.services.components.Release>> RELEASE_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.ReleaseLink>> RELEASE_LINK_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.components.ReleaseNode>> RELEASE_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, List<String>>> STRING_LIST_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<org.eclipse.sw360.datahandler.services.attachments.Attachment>> ATTACHMENT_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component>> COMPONENT_PAGE =
            new ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component>>() {};
    private static final ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release>> RELEASE_PAGE =
            new ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release>>() {};
    private static final ParameterizedTypeReference<List<String>> STRING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ComponentServiceRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    // ---- Summary Getters ------------------------------------------------------------------------

    @Override
    public List<Component> getComponentsShort(Set<String> ids) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(b -> b.path(BASE + "/short").queryParam("ids", ids).build())
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> getComponentSummary(User user) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(BASE + "/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> getRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(b -> b.path(BASE + "/recent").queryParam("limit", limit).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(b -> b.path(BASE + "/accessible/recent").queryParam("limit", limit).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public int getTotalComponentsCount(User user) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public int getAccessibleTotalComponentsCount(User user) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(BASE + "/accessible/count")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public List<Release> getReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/accessible/summary")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData)
            throws TException {
        return paginatedReleaseCall(BASE + "/releases/accessible/paginated", null, pageData, user);
    }

    @Override
    public List<Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        return call(() -> toThriftComponents(restClient.post()
                .uri(BASE + "/search")
                .body(body)
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public Map<PaginationData, List<Component>> refineSearchAccessibleComponents(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        body.put("pageData", PaginationDataConverter.fromThrift(pageData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/accessible/paginated")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(COMPONENT_PAGE));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public List<Component> refineSearchWithAccessibility(String text,
            Map<String, Set<String>> subQueryRestrictions, User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        return call(() -> toThriftComponents(restClient.post()
                .uri(BASE + "/search/accessible")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> getMyComponents(User user) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(BASE + "/my")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public Map<PaginationData, List<Release>> searchAccessibleReleases(String searchText, User user,
            PaginationData pageData) throws TException {
        return paginatedReleaseCall(BASE + "/releases/search/accessible", searchText, pageData, user);
    }

    @Override
    public List<Release> searchReleaseByNamePrefix(String name) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(b -> b.path(BASE + "/releases/search").queryParam("name", name).build())
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData)
            throws TException {
        return paginatedReleaseCall(BASE + "/releases/search/paginated",
                name != null ? name : "", pageData, null);
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(User user, PaginationData pageData)
            throws TException {
        return paginatedReleaseCall(BASE + "/releases/accessible/new-with-src", null, pageData, user);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        return paginatedComponentCall(BASE + "/search/by-name-prefix", name, pageData, user);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        return paginatedComponentCall(BASE + "/search/by-exact-name", name, pageData, user);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("subQueryRestrictions", subQueryRestrictions != null ? subQueryRestrictions : Map.of());
        body.put("pageData", PaginationDataConverter.fromThrift(pageData));
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> restClient.post()
                        .uri(BASE + "/search/by-exact-values")
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(COMPONENT_PAGE));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public List<Component> getSubscribedComponents(User user) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(BASE + "/subscribed")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Release> getSubscribedReleases(User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/subscribed")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getRecentReleases() throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/recent")
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getRecentReleasesWithAccessibility(User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/recent/accessible")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    // ---- Get Individual Objects -----------------------------------------------------------------

    @Override
    public Component getComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public Component getAccessibleComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/accessible/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public Component getComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public Component getAccessibleComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/accessible/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public Release getReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class)));
    }

    @Override
    public Release getAccessibleReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/releases/accessible/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class)));
    }

    @Override
    public Release getReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class)));
    }

    @Override
    public Release getAccessibleReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/releases/accessible/{id}/for-edit").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Release.class)));
    }

    @Override
    public List<Release> getReleasesByIdsForExport(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/by-ids/export")
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<String> getReleaseIdsFromComponentId(String id, User user) throws TException {
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
    public List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/by-ids/export/accessible")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getAccessibleReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/accessible/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getFullReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/full/by-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/with-permissions")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(b -> b.path(BASE + "/releases/by-vendor/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getReleasesFromVendorIds(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/by-vendor-ids")
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/accessible/by-vendor-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public Set<Release> getReleasesByVendorId(String vendorId) throws TException {
        return call(() -> toThriftReleaseSet(restClient.get()
                .uri(b -> b.path(BASE + "/releases/set-by-vendor/{vendorId}").build(vendorId))
                .retrieve()
                .body(RELEASE_SET)));
    }

    // ---- Add Individual Objects -----------------------------------------------------------------

    @Override
    public AddDocumentRequestSummary addComponent(Component component, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(restClient.post()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(ComponentConverter.fromThrift(component))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class)));
    }

    @Override
    public AddDocumentRequestSummary addRelease(Release release, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(restClient.post()
                .uri(BASE + "/releases")
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary.class)));
    }

    // ---- Update Individual Objects --------------------------------------------------------------

    @Override
    public RequestStatus updateComponent(Component component, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(ComponentConverter.fromThrift(component))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateComponentWithForceFlag(Component component, User user, boolean forceUpdate)
            throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(b -> b.path(BASE + "/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(ComponentConverter.fromThrift(component))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestSummary updateComponents(Set<Component> components, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.put()
                .uri(BASE + "/bulk")
                .headers(h -> addUser(h, user))
                .body(toPojoComponentSet(components))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestStatus updateComponentFromModerationRequest(Component componentAdditions,
            Component componentDeletions, User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", ComponentConverter.fromThrift(componentAdditions));
        body.put("deletions", ComponentConverter.fromThrift(componentDeletions));
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId,
            Component componentSelection, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/merge")
                        .queryParam("targetId", componentTargetId)
                        .queryParam("sourceId", componentSourceId)
                        .build())
                .headers(h -> addUser(h, user))
                .body(ComponentConverter.fromThrift(componentSelection))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateRelease(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/releases")
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateReleaseWithForceFlag(Release release, User user, boolean forceUpdate)
            throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(b -> b.path(BASE + "/releases/force").queryParam("forceUpdate", forceUpdate).build())
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateReleaseFossology(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/releases/fossology")
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestSummary updateReleases(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.put()
                .uri(BASE + "/releases/bulk")
                .headers(h -> addUser(h, user))
                .body(toPojoReleaseSet(releases))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.put()
                .uri(BASE + "/releases/bulk/direct")
                .headers(h -> addUser(h, user))
                .body(toPojoReleaseSet(releases))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestStatus updateReleaseFromModerationRequest(Release releaseAdditions, Release releaseDeletions,
            User user) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", ReleaseConverter.fromThrift(releaseAdditions));
        body.put("deletions", ReleaseConverter.fromThrift(releaseDeletions));
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/releases/moderation")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, Release releaseSelection,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/releases/merge")
                        .queryParam("targetId", releaseTargetId)
                        .queryParam("sourceId", releaseSourceId)
                        .build())
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(releaseSelection))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public List<Release> getReferencingReleases(String releaseId) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/referencing").build(releaseId))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    // ---- Delete Individual Objects --------------------------------------------------------------

    @Override
    public RequestStatus deleteComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteComponentWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteReleaseWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}/force").queryParam("forceDelete", forceDelete).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Component-Release Relationships --------------------------------------------------------

    @Override
    public List<Release> getReleasesByComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(b -> b.path(BASE + "/releases/by-component/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(b -> b.path(BASE + "/releases/full-docs/by-component/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public Map<PaginationData, List<Release>> getReleasesFromComponentIdWithPagination(String id, User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> restClient.post()
                        .uri(b -> b.path(BASE + "/releases/by-component/{id}/paginated").build(id))
                        .headers(h -> addUser(h, user))
                        .body(PaginationDataConverter.fromThrift(pageData))
                        .retrieve()
                        .body(RELEASE_PAGE));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public Set<Component> getUsingComponentsForRelease(String releaseId) throws TException {
        return call(() -> toThriftComponentSet(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/using-components").build(releaseId))
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, User user)
            throws TException {
        return call(() -> toThriftComponentSet(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/using-components/accessible").build(releaseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public Set<Component> getUsingComponentsForComponent(Set<String> releaseIds) throws TException {
        return call(() -> toThriftComponentSet(restClient.post()
                .uri(BASE + "/using-components")
                .body(releaseIds)
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, User user)
            throws TException {
        return call(() -> toThriftComponentSet(restClient.post()
                .uri(BASE + "/using-components/accessible")
                .headers(h -> addUser(h, user))
                .body(releaseIds)
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) throws TException {
        return call(() -> toThriftComponentSet(restClient.get()
                .uri(b -> b.path(BASE + "/by-vendor/{id}").build(defaultVendorId))
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public boolean releaseIsUsed(String releaseId) throws TException {
        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/in-use").build(releaseId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public boolean componentIsUsed(String componentId) throws TException {
        Boolean result = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/in-use").build(componentId))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public Component recomputeReleaseDependentFields(String componentId, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/{id}/recompute").build(componentId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws TException {
        return call(() -> BulkOperationNodeConverter.toThrift(restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/releases/{id}/bulk")
                        .queryParam("preview", isPreview).build(releaseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.BulkOperationNode.class)));
    }

    // ---- Subscribe / Unsubscribe ----------------------------------------------------------------

    @Override
    public RequestStatus subscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/{id}/subscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus subscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/releases/{id}/subscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus unsubscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/{id}/unsubscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus unsubscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(b -> b.path(BASE + "/releases/{id}/unsubscribe").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Export ----------------------------------------------------------------------------------

    @Override
    public List<Component> getComponentSummaryForExport() throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(BASE + "/summary/export")
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(BASE + "/detailed-summary/export")
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public List<Component> searchComponentForExport(String name, boolean caseSensitive) throws TException {
        return call(() -> toThriftComponents(restClient.get()
                .uri(b -> b.path(BASE + "/search-export")
                        .queryParam("name", name)
                        .queryParam("caseSensitive", caseSensitive)
                        .build())
                .retrieve()
                .body(COMPONENT_LIST)));
    }

    @Override
    public Component getComponentForReportFromFossologyUploadId(String uploadId) throws TException {
        return call(() -> ComponentConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/by-fossology-upload/{uploadId}").build(uploadId))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.Component.class)));
    }

    @Override
    public Set<Attachment> getSourceAttachments(String releaseId) throws TException {
        return call(() -> toThriftAttachmentSet(restClient.get()
                .uri(b -> b.path(BASE + "/releases/{id}/source-attachments").build(releaseId))
                .retrieve()
                .body(ATTACHMENT_SET)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleases(Map<String, ProjectReleaseRelationship> relations) throws TException {
        Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> pojoRelations =
                toPojoProjectReleaseRelationshipMap(relations);
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(BASE + "/releases/linked")
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ProjectReleaseRelationship> relations,
            User user) throws TException {
        Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> pojoRelations =
                toPojoProjectReleaseRelationshipMap(relations);
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(BASE + "/releases/linked/accessible")
                .headers(h -> addUser(h, user))
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelations(Map<String, ReleaseRelationship> relations) throws TException {
        Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> pojoRelations =
                toPojoReleaseRelationshipMap(relations);
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(BASE + "/releases/linked-relations")
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, ReleaseRelationship> relations,
            User user) throws TException {
        Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> pojoRelations =
                toPojoReleaseRelationshipMap(relations);
        return call(() -> toThriftReleaseLinks(restClient.post()
                .uri(BASE + "/releases/linked-relations/accessible")
                .headers(h -> addUser(h, user))
                .body(pojoRelations)
                .retrieve()
                .body(RELEASE_LINK_LIST)));
    }

    @Override
    public Set<String> getUsedAttachmentContentIds() throws TException {
        return call(() -> {
            Set<String> result = restClient.get()
                    .uri(BASE + "/used-attachment-ids")
                    .retrieve()
                    .body(STRING_SET);
            return result != null ? result : new HashSet<>();
        });
    }

    @Override
    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/releases/svm-feedback")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus uploadSourceCodeAttachmentToReleases() throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/releases/upload-source")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/duplicates")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/releases/duplicates")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return call(() -> {
            Map<String, List<String>> result = restClient.get()
                    .uri(BASE + "/releases/duplicate-sources")
                    .retrieve()
                    .body(STRING_LIST_MAP);
            return result != null ? result : new HashMap<>();
        });
    }

    @Override
    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftComponentSet(restClient.post()
                .uri(BASE + "/search-by-external-ids")
                .body(externalIds)
                .retrieve()
                .body(COMPONENT_SET)));
    }

    @Override
    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftReleaseSet(restClient.post()
                .uri(BASE + "/releases/search-by-external-ids")
                .body(externalIds)
                .retrieve()
                .body(RELEASE_SET)));
    }

    @Override
    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/releases/cyclic-path")
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(String.class));
    }

    @Override
    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws TException {
        return call(() -> toThriftImportBomPrep(restClient.post()
                .uri(b -> b.path(BASE + "/import-bom/prepare")
                        .queryParam("attachmentContentId", attachmentContentId).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation.class)));
    }

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
    public RequestStatus splitComponent(Component srcComponent, Component targetComponent, User user)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("srcComponent", ComponentConverter.fromThrift(srcComponent));
        body.put("targetComponent", ComponentConverter.fromThrift(targetComponent));
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/split")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public List<Release> getAllReleasesForUser(User user) throws TException {
        return call(() -> toThriftReleases(restClient.get()
                .uri(BASE + "/releases/all")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        return paginatedComponentCall(BASE + "/recent/paginated", null, pageData, user);
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("url", url);
        body.put("recipient", recepient);
        call(() -> {
            restClient.post()
                    .uri(BASE + "/export-mail")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(b -> b.path(BASE + "/download-excel")
                        .queryParam("extendedByReleases", extendedByReleases)
                        .queryParam("token", token)
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class)));
    }

    @Override
    public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(b -> b.path(BASE + "/report-stream")
                        .queryParam("extendedByReleases", extendedByReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(byte[].class)));
    }

    @Override
    public String getComponentReportInEmail(User user, boolean extendedByReleases) throws TException {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/report-email")
                        .queryParam("extendedByReleases", extendedByReleases).build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(String.class));
    }

    @Override
    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) throws TException {
        Boolean result = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/releases/action-allowed")
                        .queryParam("action", action.name()).build())
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(Boolean.class));
        return result != null && result;
    }

    @Override
    public List<Release> getReleasesByListIds(List<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(restClient.post()
                .uri(BASE + "/releases/by-list-ids")
                .headers(h -> addUser(h, user))
                .body(ids)
                .retrieve()
                .body(RELEASE_LIST)));
    }

    @Override
    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) throws TException {
        return call(() -> toThriftReleaseNodes(restClient.post()
                .uri(BASE + "/releases/relation-network")
                .headers(h -> addUser(h, user))
                .body(ReleaseConverter.fromThrift(release))
                .retrieve()
                .body(RELEASE_NODE_LIST)));
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

    private static ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes == null ? new byte[0] : bytes);
    }

    private static <P, T> Map<PaginationData, List<T>> toPaginatedMap(
            org.eclipse.sw360.datahandler.services.common.PaginatedResult<P> result,
            PaginationData fallbackPageData,
            Function<P, T> converter) {
        Map<PaginationData, List<T>> map = new HashMap<>();
        if (result != null) {
            PaginationData pd = result.getPaginationData() != null
                    ? PaginationDataConverter.toThrift(result.getPaginationData())
                    : (fallbackPageData != null ? fallbackPageData : new PaginationData());
            List<T> items = result.getData() == null ? new ArrayList<>()
                    : result.getData().stream().map(converter).collect(Collectors.toList());
            map.put(pd, items);
        }
        return map;
    }

    private Map<PaginationData, List<Component>> paginatedComponentCall(String path, String searchText,
            PaginationData pageData, User user) throws TException {
        Object body = searchText != null
                ? nameSearchBody(searchText, pageData)
                : PaginationDataConverter.fromThrift(pageData);
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> restClient.post()
                        .uri(path)
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(COMPONENT_PAGE));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    private Map<PaginationData, List<Release>> paginatedReleaseCall(String path, String searchText,
            PaginationData pageData, User user) throws TException {
        Object body = searchText != null
                ? nameSearchBody(searchText, pageData)
                : PaginationDataConverter.fromThrift(pageData);
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> restClient.post()
                        .uri(path)
                        .headers(h -> addUser(h, user))
                        .body(body)
                        .retrieve()
                        .body(RELEASE_PAGE));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    private static Map<String, Object> nameSearchBody(String name, PaginationData pageData) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("pageData", PaginationDataConverter.fromThrift(pageData));
        return body;
    }

    // ---- Collection converters ------------------------------------------------------------------

    private static List<Component> toThriftComponents(
            List<org.eclipse.sw360.datahandler.services.components.Component> pojos) {
        if (pojos == null) return new ArrayList<>();
        return pojos.stream().map(ComponentConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.components.Component> toPojoComponents(
            List<Component> thrifts) {
        if (thrifts == null) return new ArrayList<>();
        return thrifts.stream().map(ComponentConverter::fromThrift).collect(Collectors.toList());
    }

    private static Set<Component> toThriftComponentSet(
            Set<org.eclipse.sw360.datahandler.services.components.Component> pojos) {
        if (pojos == null) return new HashSet<>();
        return pojos.stream().map(ComponentConverter::toThrift).collect(Collectors.toSet());
    }

    private static Set<org.eclipse.sw360.datahandler.services.components.Component> toPojoComponentSet(
            Set<Component> thrifts) {
        if (thrifts == null) return new HashSet<>();
        return thrifts.stream().map(ComponentConverter::fromThrift).collect(Collectors.toSet());
    }

    private static List<Release> toThriftReleases(
            List<org.eclipse.sw360.datahandler.services.components.Release> pojos) {
        if (pojos == null) return new ArrayList<>();
        return pojos.stream().map(ReleaseConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.components.Release> toPojoReleases(
            List<Release> thrifts) {
        if (thrifts == null) return new ArrayList<>();
        return thrifts.stream().map(ReleaseConverter::fromThrift).collect(Collectors.toList());
    }

    private static Set<Release> toThriftReleaseSet(
            Set<org.eclipse.sw360.datahandler.services.components.Release> pojos) {
        if (pojos == null) return new HashSet<>();
        return pojos.stream().map(ReleaseConverter::toThrift).collect(Collectors.toSet());
    }

    private static Set<org.eclipse.sw360.datahandler.services.components.Release> toPojoReleaseSet(
            Set<Release> thrifts) {
        if (thrifts == null) return new HashSet<>();
        return thrifts.stream().map(ReleaseConverter::fromThrift).collect(Collectors.toSet());
    }

    private static List<ReleaseLink> toThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> pojos) {
        if (pojos == null) return new ArrayList<>();
        return pojos.stream().map(ReleaseLinkConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseNode> toThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> pojos) {
        if (pojos == null) return new ArrayList<>();
        return pojos.stream().map(ReleaseNodeConverter::toThrift).collect(Collectors.toList());
    }

    private static Set<Attachment> toThriftAttachmentSet(
            Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> pojos) {
        if (pojos == null) return new HashSet<>();
        return pojos.stream().map(AttachmentConverter::toThrift).collect(Collectors.toSet());
    }

    private static Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship>
            toPojoProjectReleaseRelationshipMap(Map<String, ProjectReleaseRelationship> thriftMap) {
        if (thriftMap == null) return new HashMap<>();
        return thriftMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ProjectReleaseRelationshipConverter.fromThrift(e.getValue())));
    }

    private static Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship>
            toPojoReleaseRelationshipMap(Map<String, ReleaseRelationship> thriftMap) {
        if (thriftMap == null) return new HashMap<>();
        return thriftMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ReleaseRelationshipConverter.fromThrift(e.getValue())));
    }

    private static ImportBomRequestPreparation toThriftImportBomPrep(
            org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation pojo) {
        if (pojo == null) {
            return new ImportBomRequestPreparation();
        }
        ImportBomRequestPreparation thrift = new ImportBomRequestPreparation();
        if (pojo.getRequestStatus() != null) {
            thrift.setRequestStatus(RequestStatusConverter.toThrift(pojo.getRequestStatus()));
        }
        if (pojo.getIsComponentDuplicate() != null) {
            thrift.setIsComponentDuplicate(pojo.getIsComponentDuplicate());
        }
        if (pojo.getIsReleaseDuplicate() != null) {
            thrift.setIsReleaseDuplicate(pojo.getIsReleaseDuplicate());
        }
        if (pojo.getComponentsName() != null) {
            thrift.setComponentsName(pojo.getComponentsName());
        }
        if (pojo.getReleasesName() != null) {
            thrift.setReleasesName(pojo.getReleasesName());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getMessage() != null) {
            thrift.setMessage(pojo.getMessage());
        }
        return thrift;
    }
}
