/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/components")
public class ComponentController {

    private final ComponentHandler componentHandler;

    public ComponentController() throws IOException {
        this.componentHandler = new ComponentHandler();
    }

    // ──────────────────────────────────────────────
    // Component CRUD
    // ──────────────────────────────────────────────

    @GetMapping("/short")
    public List<Component> getComponentsShort(@RequestParam Set<String> ids) {
        return ComponentRestMapper.fromThriftComponents(componentHandler.getComponentsShort(ids));
    }

    @GetMapping("/summary")
    public List<Component> getComponentSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(componentHandler.getComponentSummary(user));
    }

    @GetMapping("/recent")
    public List<Component> getRecentComponentsSummary(
            @RequestParam int limit,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(componentHandler.getRecentComponentsSummary(limit, user));
    }

    @GetMapping("/accessible/recent")
    public List<Component> getAccessibleRecentComponentsSummary(
            @RequestParam int limit,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(componentHandler.getAccessibleRecentComponentsSummary(limit, user));
    }

    @GetMapping("/count")
    public int getTotalComponentsCount(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.getTotalComponentsCount(user);
    }

    @GetMapping("/accessible/count")
    public int getAccessibleTotalComponentsCount(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.getAccessibleTotalComponentsCount(user);
    }

    @GetMapping("/{id}")
    public Component getComponentById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponent(componentHandler.getComponentById(id, user));
    }

    @GetMapping("/accessible/{id}")
    public Component getAccessibleComponentById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponent(componentHandler.getAccessibleComponentById(id, user));
    }

    @GetMapping("/{id}/for-edit")
    public Component getComponentByIdForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponent(componentHandler.getComponentByIdForEdit(id, user));
    }

    @GetMapping("/accessible/{id}/for-edit")
    public Component getAccessibleComponentByIdForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponent(componentHandler.getAccessibleComponentByIdForEdit(id, user));
    }

    @PostMapping
    public AddDocumentRequestSummary addComponent(
            @RequestBody Component component,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftAddDocumentRequestSummary(
                componentHandler.addComponent(ComponentRestMapper.toThriftComponent(component), user));
    }

    @PutMapping
    public RequestStatus updateComponent(
            @RequestBody Component component,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateComponent(ComponentRestMapper.toThriftComponent(component), user));
    }

    @PutMapping("/force")
    public RequestStatus updateComponentWithForceFlag(
            @RequestBody Component component,
            @RequestParam boolean forceUpdate,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateComponentWithForceFlag(
                        ComponentRestMapper.toThriftComponent(component), user, forceUpdate));
    }

    @PutMapping("/bulk")
    public RequestSummary updateComponents(
            @RequestBody Set<Component> components,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestSummary(
                componentHandler.updateComponents(ComponentRestMapper.toThriftComponentSet(components), user));
    }

    @PutMapping("/moderation")
    public RequestStatus updateComponentFromModerationRequest(
            @RequestBody ComponentModerationRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateComponentFromModerationRequest(
                        ComponentRestMapper.toThriftComponent(request.getAdditions()),
                        ComponentRestMapper.toThriftComponent(request.getDeletions()),
                        user));
    }

    @PostMapping("/merge")
    public RequestStatus mergeComponents(
            @RequestParam String targetId,
            @RequestParam String sourceId,
            @RequestBody Component componentSelection,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.mergeComponents(targetId, sourceId,
                        ComponentRestMapper.toThriftComponent(componentSelection), user));
    }

    @DeleteMapping("/{id}")
    public RequestStatus deleteComponent(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.deleteComponent(id, user));
    }

    @DeleteMapping("/{id}/force")
    public RequestStatus deleteComponentWithForceFlag(
            @PathVariable String id,
            @RequestParam boolean forceDelete,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.deleteComponentWithForceFlag(id, user, forceDelete));
    }

    @GetMapping("/my")
    public List<Component> getMyComponents(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(componentHandler.getMyComponents(user));
    }

    @GetMapping("/subscribed")
    public List<Component> getSubscribedComponents(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(componentHandler.getSubscribedComponents(user));
    }

    @PostMapping("/{id}/subscribe")
    public RequestStatus subscribeComponent(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.subscribeComponent(id, user));
    }

    @PostMapping("/{id}/unsubscribe")
    public RequestStatus unsubscribeComponent(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.unsubscribeComponent(id, user));
    }

    @GetMapping("/{id}/in-use")
    public boolean componentIsUsed(@PathVariable String id) throws TException {
        return componentHandler.componentIsUsed(id);
    }

    @PostMapping("/{id}/recompute")
    public Component recomputeReleaseDependentFields(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponent(componentHandler.recomputeReleaseDependentFields(id, user));
    }

    @GetMapping("/summary/export")
    public List<Component> getComponentSummaryForExport() throws TException {
        return ComponentRestMapper.fromThriftComponents(componentHandler.getComponentSummaryForExport());
    }

    @GetMapping("/detailed-summary/export")
    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return ComponentRestMapper.fromThriftComponents(componentHandler.getComponentDetailedSummaryForExport());
    }

    @GetMapping("/search-export")
    public List<Component> searchComponentForExport(
            @RequestParam String name,
            @RequestParam boolean caseSensitive) throws TException {
        return ComponentRestMapper.fromThriftComponents(componentHandler.searchComponentForExport(name, caseSensitive));
    }

    @GetMapping("/by-fossology-upload/{uploadId}")
    public Component getComponentForReportFromFossologyUploadId(@PathVariable String uploadId) throws TException {
        return ComponentRestMapper.fromThriftComponent(
                componentHandler.getComponentForReportFromFossologyUploadId(uploadId));
    }

    @GetMapping("/by-vendor/{vendorId}")
    public Set<Component> getComponentsByDefaultVendorId(@PathVariable String vendorId) throws TException {
        return ComponentRestMapper.fromThriftComponentSet(componentHandler.getComponentsByDefaultVendorId(vendorId));
    }

    @PostMapping("/recent/paginated")
    public PaginatedResult<Component> getRecentComponentsSummaryWithPagination(
            @RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedComponents(
                componentHandler.getRecentComponentsSummaryWithPagination(
                        user, ComponentRestMapper.toThriftPagination(pageData)));
    }

    @GetMapping(value = "/download-excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadExcel(
            @RequestParam boolean extendedByReleases,
            @RequestParam String token,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return toByteArray(componentHandler.downloadExcel(user, extendedByReleases, token));
    }

    @GetMapping(value = "/report-stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getComponentReportDataStream(
            @RequestParam boolean extendedByReleases,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return toByteArray(componentHandler.getComponentReportDataStream(user, extendedByReleases));
    }

    @GetMapping("/report-email")
    public String getComponentReportInEmail(
            @RequestParam boolean extendedByReleases,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.getComponentReportInEmail(user, extendedByReleases);
    }

    @GetMapping("/duplicates")
    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return componentHandler.getDuplicateComponents();
    }

    @PostMapping("/search-by-external-ids")
    public Set<Component> searchComponentsByExternalIds(
            @RequestBody Map<String, Set<String>> externalIds) throws TException {
        return ComponentRestMapper.fromThriftComponentSet(
                componentHandler.searchComponentsByExternalIds(externalIds));
    }

    // ──────────────────────────────────────────────
    // Component Search
    // ──────────────────────────────────────────────

    @PostMapping("/search")
    public List<Component> refineSearch(@RequestBody ComponentSearchRequest request) throws TException {
        return ComponentRestMapper.fromThriftComponents(
                componentHandler.refineSearch(request.getText(), request.getSubQueryRestrictions()));
    }

    @PostMapping("/search/accessible/paginated")
    public PaginatedResult<Component> refineSearchAccessibleComponents(
            @RequestBody ComponentSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedComponents(
                componentHandler.refineSearchAccessibleComponents(
                        request.getText(), request.getSubQueryRestrictions(),
                        user, ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    @PostMapping("/search/accessible")
    public List<Component> refineSearchWithAccessibility(
            @RequestBody ComponentSearchRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponents(
                componentHandler.refineSearchWithAccessibility(
                        request.getText(), request.getSubQueryRestrictions(), user));
    }

    @PostMapping("/search/by-name-prefix")
    public PaginatedResult<Component> searchComponentByNamePrefixPaginated(
            @RequestBody NameSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedComponents(
                componentHandler.searchComponentByNamePrefixPaginated(
                        user, request.getName(),
                        ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    @PostMapping("/search/by-exact-name")
    public PaginatedResult<Component> searchComponentByExactNamePaginated(
            @RequestBody NameSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedComponents(
                componentHandler.searchComponentByExactNamePaginated(
                        user, request.getName(),
                        ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    @PostMapping("/search/by-exact-values")
    public PaginatedResult<Component> searchComponentByExactValues(
            @RequestBody ComponentSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedComponents(
                componentHandler.searchComponentByExactValues(
                        request.getSubQueryRestrictions(), user,
                        ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    // ──────────────────────────────────────────────
    // Release CRUD
    // ──────────────────────────────────────────────

    @GetMapping("/releases/summary")
    public List<Release> getReleaseSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleaseSummary(user));
    }

    @GetMapping("/releases/accessible/summary")
    public List<Release> getAccessibleReleaseSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getAccessibleReleaseSummary(user));
    }

    @PostMapping("/releases/accessible/paginated")
    public PaginatedResult<Release> getAccessibleReleasesWithPagination(
            @RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedReleases(
                componentHandler.getAccessibleReleasesWithPagination(
                        user, ComponentRestMapper.toThriftPagination(pageData)));
    }

    @GetMapping("/releases/{id}")
    public Release getReleaseById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRelease(componentHandler.getReleaseById(id, user));
    }

    @GetMapping("/releases/accessible/{id}")
    public Release getAccessibleReleaseById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRelease(componentHandler.getAccessibleReleaseById(id, user));
    }

    @GetMapping("/releases/{id}/for-edit")
    public Release getReleaseByIdForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRelease(componentHandler.getReleaseByIdForEdit(id, user));
    }

    @GetMapping("/releases/accessible/{id}/for-edit")
    public Release getAccessibleReleaseByIdForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRelease(componentHandler.getAccessibleReleaseByIdForEdit(id, user));
    }

    @PostMapping("/releases/by-ids/export")
    public List<Release> getReleasesByIdsForExport(@RequestBody Set<String> ids) throws TException {
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesByIdsForExport(ids));
    }

    @GetMapping("/releases/ids-by-component/{componentId}")
    public List<String> getReleaseIdsFromComponentId(
            @PathVariable String componentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.getReleaseIdsFromComponentId(componentId, user);
    }

    @PostMapping("/releases/by-ids/export/accessible")
    public List<Release> getReleasesWithAccessibilityByIdsForExport(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(
                componentHandler.getReleasesWithAccessibilityByIdsForExport(ids, user));
    }

    @PostMapping("/releases/by-ids")
    public List<Release> getReleasesById(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesById(ids, user));
    }

    @PostMapping("/releases/accessible/by-ids")
    public List<Release> getAccessibleReleasesById(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getAccessibleReleasesById(ids, user));
    }

    @PostMapping("/releases/full/by-ids")
    public List<Release> getFullReleasesById(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getFullReleasesById(ids, user));
    }

    @PostMapping("/releases/with-permissions")
    public List<Release> getReleasesWithPermissions(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesWithPermissions(ids, user));
    }

    @GetMapping("/releases/by-vendor/{vendorId}")
    public List<Release> getReleasesFromVendorId(
            @PathVariable String vendorId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesFromVendorId(vendorId, user));
    }

    @PostMapping("/releases/by-vendor-ids")
    public List<Release> getReleasesFromVendorIds(@RequestBody Set<String> ids) throws TException {
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesFromVendorIds(ids));
    }

    @PostMapping("/releases/accessible/by-vendor-ids")
    public List<Release> getAccessibleReleasesFromVendorIds(
            @RequestBody Set<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getAccessibleReleasesFromVendorIds(ids, user));
    }

    @GetMapping("/releases/set-by-vendor/{vendorId}")
    public Set<Release> getReleasesByVendorId(@PathVariable String vendorId) throws TException {
        return ComponentRestMapper.fromThriftReleaseSet(componentHandler.getReleasesByVendorId(vendorId));
    }

    @PostMapping("/releases")
    public AddDocumentRequestSummary addRelease(
            @RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftAddDocumentRequestSummary(
                componentHandler.addRelease(ComponentRestMapper.toThriftRelease(release), user));
    }

    @PutMapping("/releases")
    public RequestStatus updateRelease(
            @RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateRelease(ComponentRestMapper.toThriftRelease(release), user));
    }

    @PutMapping("/releases/force")
    public RequestStatus updateReleaseWithForceFlag(
            @RequestBody Release release,
            @RequestParam boolean forceUpdate,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateReleaseWithForceFlag(
                        ComponentRestMapper.toThriftRelease(release), user, forceUpdate));
    }

    @PutMapping("/releases/fossology")
    public RequestStatus updateReleaseFossology(
            @RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateReleaseFossology(ComponentRestMapper.toThriftRelease(release), user));
    }

    @PutMapping("/releases/bulk")
    public RequestSummary updateReleases(
            @RequestBody Set<Release> releases,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestSummary(
                componentHandler.updateReleases(ComponentRestMapper.toThriftReleaseSet(releases), user));
    }

    @PutMapping("/releases/bulk/direct")
    public RequestSummary updateReleasesDirectly(
            @RequestBody Set<Release> releases,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestSummary(
                componentHandler.updateReleasesDirectly(ComponentRestMapper.toThriftReleaseSet(releases), user));
    }

    @PutMapping("/releases/moderation")
    public RequestStatus updateReleaseFromModerationRequest(
            @RequestBody ReleaseModerationRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateReleaseFromModerationRequest(
                        ComponentRestMapper.toThriftRelease(request.getAdditions()),
                        ComponentRestMapper.toThriftRelease(request.getDeletions()),
                        user));
    }

    @PostMapping("/releases/merge")
    public RequestStatus mergeReleases(
            @RequestParam String targetId,
            @RequestParam String sourceId,
            @RequestBody Release releaseSelection,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.mergeReleases(targetId, sourceId,
                        ComponentRestMapper.toThriftRelease(releaseSelection), user));
    }

    @DeleteMapping("/releases/{id}")
    public RequestStatus deleteRelease(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.deleteRelease(id, user));
    }

    @DeleteMapping("/releases/{id}/force")
    public RequestStatus deleteReleaseWithForceFlag(
            @PathVariable String id,
            @RequestParam boolean forceDelete,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.deleteReleaseWithForceFlag(id, user, forceDelete));
    }

    @GetMapping("/releases/by-component/{componentId}")
    public List<Release> getReleasesByComponentId(
            @PathVariable String componentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesByComponentId(componentId, user));
    }

    @GetMapping("/releases/full-docs/by-component/{componentId}")
    public List<Release> getReleasesFullDocsFromComponentId(
            @PathVariable String componentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(
                componentHandler.getReleasesFullDocsFromComponentId(componentId, user));
    }

    @PostMapping("/releases/by-component/{componentId}/paginated")
    public PaginatedResult<Release> getReleasesFromComponentIdWithPagination(
            @PathVariable String componentId,
            @RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedReleases(
                componentHandler.getReleasesFromComponentIdWithPagination(
                        componentId, user, ComponentRestMapper.toThriftPagination(pageData)));
    }

    @GetMapping("/releases/{id}/referencing")
    public List<Release> getReferencingReleases(@PathVariable String id) throws TException {
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReferencingReleases(id));
    }

    @GetMapping("/releases/{releaseId}/using-components")
    public Set<Component> getUsingComponentsForRelease(@PathVariable String releaseId) throws TException {
        return ComponentRestMapper.fromThriftComponentSet(componentHandler.getUsingComponentsForRelease(releaseId));
    }

    @GetMapping("/releases/{releaseId}/using-components/accessible")
    public Set<Component> getUsingComponentsWithAccessibilityForRelease(
            @PathVariable String releaseId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponentSet(
                componentHandler.getUsingComponentsWithAccessibilityForRelease(releaseId, user));
    }

    @PostMapping("/using-components")
    public Set<Component> getUsingComponentsForComponent(@RequestBody Set<String> releaseIds) throws TException {
        return ComponentRestMapper.fromThriftComponentSet(
                componentHandler.getUsingComponentsForComponent(releaseIds));
    }

    @PostMapping("/using-components/accessible")
    public Set<Component> getUsingComponentsWithAccessibilityForComponent(
            @RequestBody Set<String> releaseIds,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftComponentSet(
                componentHandler.getUsingComponentsWithAccessibilityForComponent(releaseIds, user));
    }

    @GetMapping("/releases/{id}/in-use")
    public boolean releaseIsUsed(@PathVariable String id) throws TException {
        return componentHandler.releaseIsUsed(id);
    }

    @GetMapping("/releases/{id}/source-attachments")
    public Set<Attachment> getSourceAttachments(@PathVariable String id) throws TException {
        return ComponentRestMapper.fromThriftAttachments(componentHandler.getSourceAttachments(id));
    }

    @PostMapping("/releases/linked")
    public List<ReleaseLink> getLinkedReleases(
            @RequestBody Map<String, ProjectReleaseRelationship> relations) throws TException {
        return ComponentRestMapper.fromThriftReleaseLinks(
                componentHandler.getLinkedReleases(
                        ComponentRestMapper.toThriftProjectReleaseRelationshipMap(relations)));
    }

    @PostMapping("/releases/linked/accessible")
    public List<ReleaseLink> getLinkedReleasesWithAccessibility(
            @RequestBody Map<String, ProjectReleaseRelationship> relations,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleaseLinks(
                componentHandler.getLinkedReleasesWithAccessibility(
                        ComponentRestMapper.toThriftProjectReleaseRelationshipMap(relations), user));
    }

    @PostMapping("/releases/linked-relations")
    public List<ReleaseLink> getLinkedReleaseRelations(
            @RequestBody Map<String, ReleaseRelationship> relations) throws TException {
        return ComponentRestMapper.fromThriftReleaseLinks(
                componentHandler.getLinkedReleaseRelations(
                        ComponentRestMapper.toThriftReleaseRelationshipMap(relations)));
    }

    @PostMapping("/releases/linked-relations/accessible")
    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(
            @RequestBody Map<String, ReleaseRelationship> relations,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleaseLinks(
                componentHandler.getLinkedReleaseRelationsWithAccessibility(
                        ComponentRestMapper.toThriftReleaseRelationshipMap(relations), user));
    }

    @GetMapping("/releases/search")
    public List<Release> searchReleaseByNamePrefix(@RequestParam String name) throws TException {
        return ComponentRestMapper.fromThriftReleases(componentHandler.searchReleaseByNamePrefix(name));
    }

    @PostMapping("/releases/search/paginated")
    public PaginatedResult<Release> searchReleaseByNamePaginated(
            @RequestBody NameSearchPaginatedRequest request) throws TException {
        return ComponentRestMapper.toPaginatedReleases(
                componentHandler.searchReleaseByNamePaginated(
                        request.getName(),
                        ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    @PostMapping("/releases/search/accessible")
    public PaginatedResult<Release> searchAccessibleReleases(
            @RequestBody NameSearchPaginatedRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedReleases(
                componentHandler.searchAccessibleReleases(
                        request.getName(), user,
                        ComponentRestMapper.toThriftPagination(request.getPageData())));
    }

    @PostMapping("/releases/accessible/new-with-src")
    public PaginatedResult<Release> getAccessibleNewReleasesWithSrc(
            @RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.toPaginatedReleases(
                componentHandler.getAccessibleNewReleasesWithSrc(
                        user, ComponentRestMapper.toThriftPagination(pageData)));
    }

    @GetMapping("/releases/subscribed")
    public List<Release> getSubscribedReleases(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getSubscribedReleases(user));
    }

    @PostMapping("/releases/{id}/subscribe")
    public RequestStatus subscribeRelease(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.subscribeRelease(id, user));
    }

    @PostMapping("/releases/{id}/unsubscribe")
    public RequestStatus unsubscribeRelease(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(componentHandler.unsubscribeRelease(id, user));
    }

    @GetMapping("/releases/recent")
    public List<Release> getRecentReleases() throws TException {
        return ComponentRestMapper.fromThriftReleases(componentHandler.getRecentReleases());
    }

    @GetMapping("/releases/recent/accessible")
    public List<Release> getRecentReleasesWithAccessibility(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getRecentReleasesWithAccessibility(user));
    }

    @GetMapping("/releases/all")
    public List<Release> getAllReleasesForUser(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getAllReleasesForUser(user));
    }

    @PostMapping("/releases/by-list-ids")
    public List<Release> getReleasesByListIds(
            @RequestBody List<String> ids,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleases(componentHandler.getReleasesByListIds(ids, user));
    }

    @PostMapping("/releases/relation-network")
    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(
            @RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftReleaseNodes(
                componentHandler.getReleaseRelationNetworkOfRelease(
                        ComponentRestMapper.toThriftRelease(release), user));
    }

    @DeleteMapping("/releases/{id}/bulk")
    public BulkOperationNode deleteBulkRelease(
            @PathVariable String id,
            @RequestParam boolean preview,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftBulkOperationNode(componentHandler.deleteBulkRelease(id, user, preview));
    }

    @PostMapping("/releases/action-allowed")
    public boolean isReleaseActionAllowed(
            @RequestBody Release release,
            @RequestParam RequestedAction action,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.isReleaseActionAllowed(
                ComponentRestMapper.toThriftRelease(release), user,
                ComponentRestMapper.toThriftRequestedAction(action));
    }

    @PostMapping("/export-mail")
    public void sendExportSpreadsheetSuccessMail(
            @RequestBody ExportMailRequest request) throws TException {
        componentHandler.sendExportSpreadsheetSuccessMail(request.getUrl(), request.getRecipient());
    }

    // ──────────────────────────────────────────────
    // Other
    // ──────────────────────────────────────────────

    @GetMapping("/used-attachment-ids")
    public Set<String> getUsedAttachmentContentIds() throws TException {
        return componentHandler.getUsedAttachmentContentIds();
    }

    @PostMapping("/releases/svm-feedback")
    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.updateReleasesWithSvmTrackingFeedback());
    }

    @PostMapping("/releases/upload-source")
    public RequestStatus uploadSourceCodeAttachmentToReleases() throws TException {
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.uploadSourceCodeAttachmentToReleases());
    }

    @GetMapping("/releases/duplicates")
    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return componentHandler.getDuplicateReleases();
    }

    @GetMapping("/releases/duplicate-sources")
    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return componentHandler.getDuplicateReleaseSources();
    }

    @PostMapping("/releases/search-by-external-ids")
    public Set<Release> searchReleasesByExternalIds(
            @RequestBody Map<String, Set<String>> externalIds) throws TException {
        return ComponentRestMapper.fromThriftReleaseSet(
                componentHandler.searchReleasesByExternalIds(externalIds));
    }

    @PostMapping("/releases/cyclic-path")
    public String getCyclicLinkedReleasePath(
            @RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return componentHandler.getCyclicLinkedReleasePath(
                ComponentRestMapper.toThriftRelease(release), user);
    }

    @PostMapping("/import-bom/prepare")
    public ImportBomRequestPreparation prepareImportBom(
            @RequestParam String attachmentContentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftImportBomRequestPreparation(
                componentHandler.prepareImportBom(user, attachmentContentId));
    }

    @PostMapping("/import-bom")
    public RequestSummary importBomFromAttachmentContent(
            @RequestParam String attachmentContentId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestSummary(
                componentHandler.importBomFromAttachmentContent(user, attachmentContentId));
    }

    @PostMapping("/split")
    public RequestStatus splitComponent(
            @RequestBody SplitComponentRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return ComponentRestMapper.fromThriftRequestStatus(
                componentHandler.splitComponent(
                        ComponentRestMapper.toThriftComponent(request.getSrcComponent()),
                        ComponentRestMapper.toThriftComponent(request.getTargetComponent()),
                        user));
    }

    // ──────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────

    private static byte[] toByteArray(ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    // ──────────────────────────────────────────────
    // Request body DTOs
    // ──────────────────────────────────────────────

    public static class ComponentSearchRequest {
        private String text;
        private Map<String, Set<String>> subQueryRestrictions;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Map<String, Set<String>> getSubQueryRestrictions() { return subQueryRestrictions; }
        public void setSubQueryRestrictions(Map<String, Set<String>> subQueryRestrictions) {
            this.subQueryRestrictions = subQueryRestrictions;
        }
    }

    public static class ComponentSearchPaginatedRequest {
        private String text;
        private Map<String, Set<String>> subQueryRestrictions;
        private PaginationData pageData;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Map<String, Set<String>> getSubQueryRestrictions() { return subQueryRestrictions; }
        public void setSubQueryRestrictions(Map<String, Set<String>> subQueryRestrictions) {
            this.subQueryRestrictions = subQueryRestrictions;
        }
        public PaginationData getPageData() { return pageData; }
        public void setPageData(PaginationData pageData) { this.pageData = pageData; }
    }

    public static class NameSearchPaginatedRequest {
        private String name;
        private PaginationData pageData;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public PaginationData getPageData() { return pageData; }
        public void setPageData(PaginationData pageData) { this.pageData = pageData; }
    }

    public static class ComponentModerationRequest {
        private Component additions;
        private Component deletions;

        public Component getAdditions() { return additions; }
        public void setAdditions(Component additions) { this.additions = additions; }
        public Component getDeletions() { return deletions; }
        public void setDeletions(Component deletions) { this.deletions = deletions; }
    }

    public static class ReleaseModerationRequest {
        private Release additions;
        private Release deletions;

        public Release getAdditions() { return additions; }
        public void setAdditions(Release additions) { this.additions = additions; }
        public Release getDeletions() { return deletions; }
        public void setDeletions(Release deletions) { this.deletions = deletions; }
    }

    public static class SplitComponentRequest {
        private Component srcComponent;
        private Component targetComponent;

        public Component getSrcComponent() { return srcComponent; }
        public void setSrcComponent(Component srcComponent) { this.srcComponent = srcComponent; }
        public Component getTargetComponent() { return targetComponent; }
        public void setTargetComponent(Component targetComponent) { this.targetComponent = targetComponent; }
    }

    public static class ExportMailRequest {
        private String url;
        private String recipient;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
    }
}
