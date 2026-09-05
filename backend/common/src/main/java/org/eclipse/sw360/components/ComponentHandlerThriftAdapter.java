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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
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

/**
 * Thrift {@link ComponentService.Iface} adapter that delegates to the components REST backend
 * that delegates to {@link ComponentHandler} (POJO core) with thrift conversion at the boundary.
 */
public class ComponentHandlerThriftAdapter implements ComponentService.Iface {

    private final ComponentHandler handler;

    public ComponentHandlerThriftAdapter(ComponentHandler handler) {
        this.handler = handler;
    }

    @Override
    public List<Component> getComponentsShort(Set<String> ids) throws TException {
        return call(() -> toThriftComponents(handler.getComponentsShort(ids)));
    }

    @Override
    public List<Component> getComponentSummary(User user) throws TException {
        return call(() -> toThriftComponents(handler.getComponentSummary(user)));
    }

    @Override
    public List<Component> getRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(handler.getRecentComponentsSummary(limit, user)));
    }

    @Override
    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(handler.getAccessibleRecentComponentsSummary(limit, user)));
    }

    @Override
    public int getTotalComponentsCount(User user) throws TException {
        return call(() -> handler.getTotalComponentsCount(user));
    }

    @Override
    public int getAccessibleTotalComponentsCount(User user) throws TException {
        return call(() -> handler.getAccessibleTotalComponentsCount(user));
    }

    @Override
    public List<Release> getReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleaseSummary(user)));
    }

    @Override
    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(handler.getAccessibleReleaseSummary(user)));
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedReleases(handler.getAccessibleReleasesWithPagination(user, pageData)));
    }

    @Override
    public List<Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return call(() -> toThriftComponents(handler.refineSearch(text, subQueryRestrictions)));
    }

    @Override
    public Map<PaginationData, List<Component>> refineSearchAccessibleComponents(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedComponents(handler.refineSearchAccessibleComponents(text, subQueryRestrictions, user, pageData)));
    }

    @Override
    public List<Component> refineSearchWithAccessibility(String text,
            Map<String, Set<String>> subQueryRestrictions, User user) throws TException {
        return call(() -> toThriftComponents(handler.refineSearchWithAccessibility(text, subQueryRestrictions, user)));
    }

    @Override
    public List<Component> getMyComponents(User user) throws TException {
        return call(() -> toThriftComponents(handler.getMyComponents(user)));
    }

    @Override
    public Map<PaginationData, List<Release>> searchAccessibleReleases(String searchText, User user,
            PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedReleases(handler.searchAccessibleReleases(searchText, user, pageData)));
    }

    @Override
    public List<Release> searchReleaseByNamePrefix(String name) throws TException {
        return call(() -> toThriftReleases(handler.searchReleaseByNamePrefix(name)));
    }

    @Override
    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedReleases(handler.searchReleaseByNamePaginated(name, pageData)));
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(User user, PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedReleases(handler.getAccessibleNewReleasesWithSrc(user, pageData)));
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedComponents(handler.searchComponentByNamePrefixPaginated(user, name, pageData)));
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedComponents(handler.searchComponentByExactNamePaginated(user, name, pageData)));
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedComponents(handler.searchComponentByExactValues(subQueryRestrictions, user, pageData)));
    }

    @Override
    public List<Component> getSubscribedComponents(User user) throws TException {
        return call(() -> toThriftComponents(handler.getSubscribedComponents(user)));
    }

    @Override
    public List<Release> getSubscribedReleases(User user) throws TException {
        return call(() -> toThriftReleases(handler.getSubscribedReleases(user)));
    }

    @Override
    public List<Release> getRecentReleases() throws TException {
        return call(() -> toThriftReleases(handler.getRecentReleases()));
    }

    @Override
    public List<Release> getRecentReleasesWithAccessibility(User user) throws TException {
        return call(() -> toThriftReleases(handler.getRecentReleasesWithAccessibility(user)));
    }

    @Override
    public Component getComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.getComponentById(id, user)));
    }

    @Override
    public Component getAccessibleComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.getAccessibleComponentById(id, user)));
    }

    @Override
    public Component getComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.getComponentByIdForEdit(id, user)));
    }

    @Override
    public Component getAccessibleComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.getAccessibleComponentByIdForEdit(id, user)));
    }

    @Override
    public Release getReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(handler.getReleaseById(id, user)));
    }

    @Override
    public Release getAccessibleReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(handler.getAccessibleReleaseById(id, user)));
    }

    @Override
    public Release getReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(handler.getReleaseByIdForEdit(id, user)));
    }

    @Override
    public Release getAccessibleReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(handler.getAccessibleReleaseByIdForEdit(id, user)));
    }

    @Override
    public List<Release> getReleasesByIdsForExport(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesByIdsForExport(ids)));
    }

    @Override
    public List<String> getReleaseIdsFromComponentId(String id, User user) throws TException {
        return call(() -> handler.getReleaseIdsFromComponentId(id, user));
    }

    @Override
    public List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesWithAccessibilityByIdsForExport(ids, user)));
    }

    @Override
    public List<Release> getReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesById(ids, user)));
    }

    @Override
    public List<Release> getAccessibleReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getAccessibleReleasesById(ids, user)));
    }

    @Override
    public List<Release> getFullReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getFullReleasesById(ids, user)));
    }

    @Override
    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesWithPermissions(ids, user)));
    }

    @Override
    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesFromVendorId(id, user)));
    }

    @Override
    public List<Release> getReleasesFromVendorIds(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesFromVendorIds(ids)));
    }

    @Override
    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getAccessibleReleasesFromVendorIds(ids, user)));
    }

    @Override
    public Set<Release> getReleasesByVendorId(String vendorId) throws TException {
        return call(() -> toThriftReleaseSet(handler.getReleasesByVendorId(vendorId)));
    }

    @Override
    public AddDocumentRequestSummary addComponent(Component component, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(handler.addComponent(ComponentConverter.fromThrift(component), user)));
    }

    @Override
    public AddDocumentRequestSummary addRelease(Release release, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(handler.addRelease(ReleaseConverter.fromThrift(release), user)));
    }

    @Override
    public RequestStatus updateComponent(Component component, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateComponent(ComponentConverter.fromThrift(component), user)));
    }

    @Override
    public RequestStatus updateComponentWithForceFlag(Component component, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateComponentWithForceFlag(ComponentConverter.fromThrift(component), user, forceUpdate)));
    }

    @Override
    public RequestSummary updateComponents(Set<Component> components, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(handler.updateComponents(toPojoComponentSet(components), user)));
    }

    @Override
    public RequestStatus updateComponentFromModerationRequest(Component componentAdditions,
            Component componentDeletions, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateComponentFromModerationRequest(ComponentConverter.fromThrift(componentAdditions), ComponentConverter.fromThrift(componentDeletions), user)));
    }

    @Override
    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId,
            Component componentSelection, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.mergeComponents(componentTargetId, componentSourceId, ComponentConverter.fromThrift(componentSelection), user)));
    }

    @Override
    public RequestStatus updateRelease(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateRelease(ReleaseConverter.fromThrift(release), user)));
    }

    @Override
    public RequestStatus updateReleaseWithForceFlag(Release release, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateReleaseWithForceFlag(ReleaseConverter.fromThrift(release), user, forceUpdate)));
    }

    @Override
    public RequestStatus updateReleaseFossology(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateReleaseFossology(ReleaseConverter.fromThrift(release), user)));
    }

    @Override
    public RequestSummary updateReleases(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(handler.updateReleases(toPojoReleaseSet(releases), user)));
    }

    @Override
    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(handler.updateReleasesDirectly(toPojoReleaseSet(releases), user)));
    }

    @Override
    public RequestStatus updateReleaseFromModerationRequest(Release releaseAdditions, Release releaseDeletions,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateReleaseFromModerationRequest(ReleaseConverter.fromThrift(releaseAdditions), ReleaseConverter.fromThrift(releaseDeletions), user)));
    }

    @Override
    public RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, Release releaseSelection,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.mergeReleases(releaseTargetId, releaseSourceId, ReleaseConverter.fromThrift(releaseSelection), user)));
    }

    @Override
    public List<Release> getReferencingReleases(String releaseId) throws TException {
        return call(() -> toThriftReleases(handler.getReferencingReleases(releaseId)));
    }

    @Override
    public RequestStatus deleteComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.deleteComponent(id, user)));
    }

    @Override
    public RequestStatus deleteComponentWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.deleteComponentWithForceFlag(id, user, forceDelete)));
    }

    @Override
    public RequestStatus deleteRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.deleteRelease(id, user)));
    }

    @Override
    public RequestStatus deleteReleaseWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.deleteReleaseWithForceFlag(id, user, forceDelete)));
    }

    @Override
    public List<Release> getReleasesByComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesByComponentId(id, user)));
    }

    @Override
    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesFullDocsFromComponentId(id, user)));
    }

    @Override
    public Map<PaginationData, List<Release>> getReleasesFromComponentIdWithPagination(String id, User user,
            PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedReleases(handler.getReleasesFromComponentIdWithPagination(id, user, pageData)));
    }

    @Override
    public Set<Component> getUsingComponentsForRelease(String releaseId) throws TException {
        return call(() -> toThriftComponentSet(handler.getUsingComponentsForRelease(releaseId)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, User user) throws TException {
        return call(() -> toThriftComponentSet(handler.getUsingComponentsWithAccessibilityForRelease(releaseId, user)));
    }

    @Override
    public Set<Component> getUsingComponentsForComponent(Set<String> releaseIds) throws TException {
        return call(() -> toThriftComponentSet(handler.getUsingComponentsForComponent(releaseIds)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, User user) throws TException {
        return call(() -> toThriftComponentSet(handler.getUsingComponentsWithAccessibilityForComponent(releaseIds, user)));
    }

    @Override
    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) throws TException {
        return call(() -> toThriftComponentSet(handler.getComponentsByDefaultVendorId(defaultVendorId)));
    }

    @Override
    public boolean releaseIsUsed(String releaseId) throws TException {
        return call(() -> handler.releaseIsUsed(releaseId));
    }

    @Override
    public boolean componentIsUsed(String componentId) throws TException {
        return call(() -> handler.componentIsUsed(componentId));
    }

    @Override
    public Component recomputeReleaseDependentFields(String componentId, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.recomputeReleaseDependentFields(componentId, user)));
    }

    @Override
    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws TException {
        return call(() -> handler.deleteBulkRelease(releaseId, user, isPreview));
    }

    @Override
    public RequestStatus subscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.subscribeComponent(id, user)));
    }

    @Override
    public RequestStatus subscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.subscribeRelease(id, user)));
    }

    @Override
    public RequestStatus unsubscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.unsubscribeComponent(id, user)));
    }

    @Override
    public RequestStatus unsubscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.unsubscribeRelease(id, user)));
    }

    @Override
    public List<Component> getComponentSummaryForExport() throws TException {
        return call(() -> toThriftComponents(handler.getComponentSummaryForExport()));
    }

    @Override
    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return call(() -> toThriftComponents(handler.getComponentDetailedSummaryForExport()));
    }

    @Override
    public List<Component> searchComponentForExport(String name, boolean caseSensitive) throws TException {
        return call(() -> toThriftComponents(handler.searchComponentForExport(name, caseSensitive)));
    }

    @Override
    public Component getComponentForReportFromFossologyUploadId(String uploadId) throws TException {
        return call(() -> ComponentConverter.toThrift(handler.getComponentForReportFromFossologyUploadId(uploadId)));
    }

    @Override
    public Set<Attachment> getSourceAttachments(String releaseId) throws TException {
        return call(() -> toThriftAttachmentSet(handler.getSourceAttachments(releaseId)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleases(Map<String, ProjectReleaseRelationship> relations) throws TException {
        return call(() -> handler.getLinkedReleases(relations));
    }

    @Override
    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ProjectReleaseRelationship> relations,
            User user) throws TException {
        return call(() -> handler.getLinkedReleasesWithAccessibility(relations, user));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelations(Map<String, ReleaseRelationship> relations) throws TException {
        return call(() -> handler.getLinkedReleaseRelations(relations));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, ReleaseRelationship> relations,
            User user) throws TException {
        return call(() -> handler.getLinkedReleaseRelationsWithAccessibility(relations, user));
    }

    @Override
    public Set<String> getUsedAttachmentContentIds() throws TException {
        return call(() -> handler.getUsedAttachmentContentIds());
    }

    @Override
    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.updateReleasesWithSvmTrackingFeedback()));
    }

    @Override
    public RequestStatus uploadSourceCodeAttachmentToReleases() throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.uploadSourceCodeAttachmentToReleases()));
    }

    @Override
    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return call(() -> handler.getDuplicateComponents());
    }

    @Override
    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return call(() -> handler.getDuplicateReleases());
    }

    @Override
    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return call(() -> handler.getDuplicateReleaseSources());
    }

    @Override
    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftComponentSet(handler.searchComponentsByExternalIds(externalIds)));
    }

    @Override
    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftReleaseSet(handler.searchReleasesByExternalIds(externalIds)));
    }

    @Override
    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        return call(() -> handler.getCyclicLinkedReleasePath(ReleaseConverter.fromThrift(release), user));
    }

    @Override
    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws TException {
        return call(() -> handler.prepareImportBom(user, attachmentContentId));
    }

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(handler.importBomFromAttachmentContent(user, attachmentContentId)));
    }

    @Override
    public RequestStatus splitComponent(Component srcComponent, Component targetComponent, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(handler.splitComponent(ComponentConverter.fromThrift(srcComponent), ComponentConverter.fromThrift(targetComponent), user)));
    }

    @Override
    public List<Release> getAllReleasesForUser(User user) throws TException {
        return call(() -> toThriftReleases(handler.getAllReleasesForUser(user)));
    }

    @Override
    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        return call(() -> toThriftPaginatedComponents(handler.getRecentComponentsSummaryWithPagination(user, pageData)));
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        call(() -> { handler.sendExportSpreadsheetSuccessMail(url, recepient); return null; });
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return call(() -> handler.downloadExcel(user, extendedByReleases, token));
    }

    @Override
    public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException {
        return call(() -> handler.getComponentReportDataStream(user, extendedByReleases));
    }

    @Override
    public String getComponentReportInEmail(User user, boolean extendedByReleases) throws TException {
        return call(() -> handler.getComponentReportInEmail(user, extendedByReleases));
    }

    @Override
    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) throws TException {
        return call(() -> handler.isReleaseActionAllowed(ReleaseConverter.fromThrift(release), user, action));
    }

    @Override
    public List<Release> getReleasesByListIds(List<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(handler.getReleasesByListIds(ids, user)));
    }

    @Override
    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) throws TException {
        return call(() -> handler.getReleaseRelationNetworkOfRelease(ReleaseConverter.fromThrift(release), user));
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws TException;
    }

    private static <T> T call(ThrowingSupplier<T> supplier) throws TException {
        try {
            return supplier.get();
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
            SW360Exception thriftEx = new SW360Exception(e.getMessage());
            if (e.getErrorCode() != null) {
                thriftEx.setErrorCode(e.getErrorCode());
            }
            thriftEx.initCause(e);
            throw thriftEx;
        }
    }


    private static Map<PaginationData, List<Release>> toThriftPaginatedReleases(
            Map<PaginationData, List<org.eclipse.sw360.datahandler.services.components.Release>> map) {
        Map<PaginationData, List<Release>> out = new HashMap<>();
        if (map != null) {
            for (Map.Entry<PaginationData, List<org.eclipse.sw360.datahandler.services.components.Release>> e : map.entrySet()) {
                out.put(e.getKey(), toThriftReleases(e.getValue()));
            }
        }
        return out;
    }

    private static Map<PaginationData, List<Component>> toThriftPaginatedComponents(
            Map<PaginationData, List<org.eclipse.sw360.datahandler.services.components.Component>> map) {
        Map<PaginationData, List<Component>> out = new HashMap<>();
        if (map != null) {
            for (Map.Entry<PaginationData, List<org.eclipse.sw360.datahandler.services.components.Component>> e : map.entrySet()) {
                out.put(e.getKey(), toThriftComponents(e.getValue()));
            }
        }
        return out;
    }

    private static List<Component> toThriftComponents(
            List<org.eclipse.sw360.datahandler.services.components.Component> pojos) {
        if (pojos == null) return new ArrayList<>();
        return pojos.stream().map(ComponentConverter::toThrift).collect(Collectors.toList());
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

    private static Set<Attachment> toThriftAttachmentSet(
            Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> pojos) {
        if (pojos == null) return new HashSet<>();
        return pojos.stream().map(AttachmentConverter::toThrift).collect(Collectors.toSet());
    }

}
