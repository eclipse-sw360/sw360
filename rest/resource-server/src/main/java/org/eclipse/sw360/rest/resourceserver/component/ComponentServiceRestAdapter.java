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
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.users.RequestedActionConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.components.ComponentClient;
import org.eclipse.sw360.datahandler.components.ComponentClients;
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
 * ({@code /components/api/components}). Keeps the Thrift contract intact for existing resource-server
 * callers while removing the Thrift transport.
 */
@org.springframework.stereotype.Component
public class ComponentServiceRestAdapter implements ComponentService.Iface {

    private ComponentClient client() {
        return ComponentClients.get();
    }

    @Override
    public List<Component> getComponentsShort(Set<String> ids) throws TException {
        return call(() -> toThriftComponents(client().getComponentsShort(ids)));
    }

    @Override
    public List<Component> getComponentSummary(User user) throws TException {
        return call(() -> toThriftComponents(client().getComponentSummary(UserConverter.fromThrift(user))));
    }

    @Override
    public List<Component> getRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(client().getRecentComponentsSummary(limit, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) throws TException {
        return call(() -> toThriftComponents(client().getAccessibleRecentComponentsSummary(limit, UserConverter.fromThrift(user))));
    }

    @Override
    public int getTotalComponentsCount(User user) throws TException {
        return call(() -> client().getTotalComponentsCount(UserConverter.fromThrift(user)));
    }

    @Override
    public int getAccessibleTotalComponentsCount(User user) throws TException {
        return call(() -> client().getAccessibleTotalComponentsCount(UserConverter.fromThrift(user)));
    }

    @Override
    public List<Release> getReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(client().getReleaseSummary(UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        return call(() -> toThriftReleases(client().getAccessibleReleaseSummary(UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> client().getAccessibleReleasesWithPagination(UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public List<Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return call(() -> toThriftComponents(client().refineSearch(text, subQueryRestrictions)));
    }

    @Override
    public Map<PaginationData, List<Component>> refineSearchAccessibleComponents(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> client().refineSearchAccessibleComponents(text, subQueryRestrictions, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public List<Component> refineSearchWithAccessibility(String text,
            Map<String, Set<String>> subQueryRestrictions, User user) throws TException {
        return call(() -> toThriftComponents(client().refineSearchWithAccessibility(text, subQueryRestrictions, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Component> getMyComponents(User user) throws TException {
        return call(() -> toThriftComponents(client().getMyComponents(UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Release>> searchAccessibleReleases(String searchText, User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> client().searchAccessibleReleases(searchText, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public List<Release> searchReleaseByNamePrefix(String name) throws TException {
        return call(() -> toThriftReleases(client().searchReleaseByNamePrefix(name)));
    }

    @Override
    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> client().searchReleaseByNamePaginated(name, PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(User user, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> client().getAccessibleNewReleasesWithSrc(UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> client().searchComponentByNamePrefixPaginated(UserConverter.fromThrift(user), name, PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> client().searchComponentByExactNamePaginated(UserConverter.fromThrift(user), name, PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public Map<PaginationData, List<Component>> searchComponentByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> client().searchComponentByExactValues(subQueryRestrictions, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public List<Component> getSubscribedComponents(User user) throws TException {
        return call(() -> toThriftComponents(client().getSubscribedComponents(UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getSubscribedReleases(User user) throws TException {
        return call(() -> toThriftReleases(client().getSubscribedReleases(UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getRecentReleases() throws TException {
        return call(() -> toThriftReleases(client().getRecentReleases()));
    }

    @Override
    public List<Release> getRecentReleasesWithAccessibility(User user) throws TException {
        return call(() -> toThriftReleases(client().getRecentReleasesWithAccessibility(UserConverter.fromThrift(user))));
    }

    @Override
    public Component getComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(client().getComponentById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Component getAccessibleComponentById(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(client().getAccessibleComponentById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Component getComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(client().getComponentByIdForEdit(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Component getAccessibleComponentByIdForEdit(String id, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(client().getAccessibleComponentByIdForEdit(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Release getReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(client().getReleaseById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Release getAccessibleReleaseById(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(client().getAccessibleReleaseById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Release getReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(client().getReleaseByIdForEdit(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Release getAccessibleReleaseByIdForEdit(String id, User user) throws TException {
        return call(() -> ReleaseConverter.toThrift(client().getAccessibleReleaseByIdForEdit(id, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesByIdsForExport(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(client().getReleasesByIdsForExport(ids)));
    }

    @Override
    public List<String> getReleaseIdsFromComponentId(String id, User user) throws TException {
        return call(() -> client().getReleaseIdsFromComponentId(id, UserConverter.fromThrift(user)));
    }

    @Override
    public List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesWithAccessibilityByIdsForExport(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesById(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getAccessibleReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getAccessibleReleasesById(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getFullReleasesById(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getFullReleasesById(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesWithPermissions(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesFromVendorId(id, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesFromVendorIds(Set<String> ids) throws TException {
        return call(() -> toThriftReleases(client().getReleasesFromVendorIds(ids)));
    }

    @Override
    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getAccessibleReleasesFromVendorIds(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Release> getReleasesByVendorId(String vendorId) throws TException {
        return call(() -> toThriftReleaseSet(client().getReleasesByVendorId(vendorId)));
    }

    @Override
    public AddDocumentRequestSummary addComponent(Component component, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(client().addComponent(ComponentConverter.fromThrift(component), UserConverter.fromThrift(user))));
    }

    @Override
    public AddDocumentRequestSummary addRelease(Release release, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(client().addRelease(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateComponent(Component component, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateComponent(ComponentConverter.fromThrift(component), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateComponentWithForceFlag(Component component, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateComponentWithForceFlag(ComponentConverter.fromThrift(component), UserConverter.fromThrift(user), forceUpdate)));
    }

    @Override
    public RequestSummary updateComponents(Set<Component> components, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().updateComponents(toPojoComponentSet(components), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateComponentFromModerationRequest(Component componentAdditions,
            Component componentDeletions, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateComponentFromModerationRequest(ComponentConverter.fromThrift(componentAdditions), ComponentConverter.fromThrift(componentDeletions), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId,
            Component componentSelection, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().mergeComponents(componentTargetId, componentSourceId, ComponentConverter.fromThrift(componentSelection), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateRelease(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateRelease(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateReleaseWithForceFlag(Release release, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateReleaseWithForceFlag(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user), forceUpdate)));
    }

    @Override
    public RequestStatus updateReleaseFossology(Release release, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateReleaseFossology(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestSummary updateReleases(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().updateReleases(toPojoReleaseSet(releases), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().updateReleasesDirectly(toPojoReleaseSet(releases), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateReleaseFromModerationRequest(Release releaseAdditions, Release releaseDeletions,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateReleaseFromModerationRequest(ReleaseConverter.fromThrift(releaseAdditions), ReleaseConverter.fromThrift(releaseDeletions), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, Release releaseSelection,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().mergeReleases(releaseTargetId, releaseSourceId, ReleaseConverter.fromThrift(releaseSelection), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReferencingReleases(String releaseId) throws TException {
        return call(() -> toThriftReleases(client().getReferencingReleases(releaseId)));
    }

    @Override
    public RequestStatus deleteComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteComponent(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus deleteComponentWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteComponentWithForceFlag(id, UserConverter.fromThrift(user), forceDelete)));
    }

    @Override
    public RequestStatus deleteRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteRelease(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus deleteReleaseWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteReleaseWithForceFlag(id, UserConverter.fromThrift(user), forceDelete)));
    }

    @Override
    public List<Release> getReleasesByComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesByComponentId(id, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesFullDocsFromComponentId(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Release>> getReleasesFromComponentIdWithPagination(String id, User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> result =
                call(() -> client().getReleasesFromComponentIdWithPagination(id, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ReleaseConverter::toThrift);
    }

    @Override
    public Set<Component> getUsingComponentsForRelease(String releaseId) throws TException {
        return call(() -> toThriftComponentSet(client().getUsingComponentsForRelease(releaseId)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, User user) throws TException {
        return call(() -> toThriftComponentSet(client().getUsingComponentsWithAccessibilityForRelease(releaseId, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Component> getUsingComponentsForComponent(Set<String> releaseIds) throws TException {
        return call(() -> toThriftComponentSet(client().getUsingComponentsForComponent(releaseIds)));
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, User user) throws TException {
        return call(() -> toThriftComponentSet(client().getUsingComponentsWithAccessibilityForComponent(releaseIds, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) throws TException {
        return call(() -> toThriftComponentSet(client().getComponentsByDefaultVendorId(defaultVendorId)));
    }

    @Override
    public boolean releaseIsUsed(String releaseId) throws TException {
        return call(() -> client().releaseIsUsed(releaseId));
    }

    @Override
    public boolean componentIsUsed(String componentId) throws TException {
        return call(() -> client().componentIsUsed(componentId));
    }

    @Override
    public Component recomputeReleaseDependentFields(String componentId, User user) throws TException {
        return call(() -> ComponentConverter.toThrift(client().recomputeReleaseDependentFields(componentId, UserConverter.fromThrift(user))));
    }

    @Override
    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws TException {
        return call(() -> BulkOperationNodeConverter.toThrift(client().deleteBulkRelease(releaseId, UserConverter.fromThrift(user), isPreview)));
    }

    @Override
    public RequestStatus subscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().subscribeComponent(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus subscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().subscribeRelease(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus unsubscribeComponent(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().unsubscribeComponent(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus unsubscribeRelease(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().unsubscribeRelease(id, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Component> getComponentSummaryForExport() throws TException {
        return call(() -> toThriftComponents(client().getComponentSummaryForExport()));
    }

    @Override
    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return call(() -> toThriftComponents(client().getComponentDetailedSummaryForExport()));
    }

    @Override
    public List<Component> searchComponentForExport(String name, boolean caseSensitive) throws TException {
        return call(() -> toThriftComponents(client().searchComponentForExport(name, caseSensitive)));
    }

    @Override
    public Component getComponentForReportFromFossologyUploadId(String uploadId) throws TException {
        return call(() -> ComponentConverter.toThrift(client().getComponentForReportFromFossologyUploadId(uploadId)));
    }

    @Override
    public Set<Attachment> getSourceAttachments(String releaseId) throws TException {
        return call(() -> toThriftAttachmentSet(client().getSourceAttachments(releaseId)));
    }

    @Override
    public List<ReleaseLink> getLinkedReleases(Map<String, ProjectReleaseRelationship> relations) throws TException {
        return call(() -> toThriftReleaseLinks(client().getLinkedReleases(toPojoProjectReleaseRelationshipMap(relations))));
    }

    @Override
    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ProjectReleaseRelationship> relations,
            User user) throws TException {
        return call(() -> toThriftReleaseLinks(client().getLinkedReleasesWithAccessibility(toPojoProjectReleaseRelationshipMap(relations), UserConverter.fromThrift(user))));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelations(Map<String, ReleaseRelationship> relations) throws TException {
        return call(() -> toThriftReleaseLinks(client().getLinkedReleaseRelations(toPojoReleaseRelationshipMap(relations))));
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, ReleaseRelationship> relations,
            User user) throws TException {
        return call(() -> toThriftReleaseLinks(client().getLinkedReleaseRelationsWithAccessibility(toPojoReleaseRelationshipMap(relations), UserConverter.fromThrift(user))));
    }

    @Override
    public Set<String> getUsedAttachmentContentIds() throws TException {
        return call(() -> client().getUsedAttachmentContentIds());
    }

    @Override
    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateReleasesWithSvmTrackingFeedback()));
    }

    @Override
    public RequestStatus uploadSourceCodeAttachmentToReleases() throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().uploadSourceCodeAttachmentToReleases()));
    }

    @Override
    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return call(() -> client().getDuplicateComponents());
    }

    @Override
    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return call(() -> client().getDuplicateReleases());
    }

    @Override
    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return call(() -> client().getDuplicateReleaseSources());
    }

    @Override
    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftComponentSet(client().searchComponentsByExternalIds(externalIds)));
    }

    @Override
    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        return call(() -> toThriftReleaseSet(client().searchReleasesByExternalIds(externalIds)));
    }

    @Override
    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        return call(() -> client().getCyclicLinkedReleasePath(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user)));
    }

    @Override
    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws TException {
        return call(() -> toThriftImportBomPrep(client().prepareImportBom(UserConverter.fromThrift(user), attachmentContentId)));
    }

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().importBomFromAttachmentContent(UserConverter.fromThrift(user), attachmentContentId)));
    }

    @Override
    public RequestStatus splitComponent(Component srcComponent, Component targetComponent, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().splitComponent(ComponentConverter.fromThrift(srcComponent), ComponentConverter.fromThrift(targetComponent), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Release> getAllReleasesForUser(User user) throws TException {
        return call(() -> toThriftReleases(client().getAllReleasesForUser(UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> result =
                call(() -> client().getRecentComponentsSummaryWithPagination(UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData, ComponentConverter::toThrift);
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        call(() -> { client().sendExportSpreadsheetSuccessMail(url, recepient); return null; });
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return call(() -> ByteBuffer.wrap(client().downloadExcel(UserConverter.fromThrift(user), extendedByReleases, token)));
    }

    @Override
    public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException {
        return call(() -> ByteBuffer.wrap(client().getComponentReportDataStream(UserConverter.fromThrift(user), extendedByReleases)));
    }

    @Override
    public String getComponentReportInEmail(User user, boolean extendedByReleases) throws TException {
        return call(() -> client().getComponentReportInEmail(UserConverter.fromThrift(user), extendedByReleases));
    }

    @Override
    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) throws TException {
        return call(() -> client().isReleaseActionAllowed(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user),
                RequestedActionConverter.fromThrift(action)));
    }

    @Override
    public List<Release> getReleasesByListIds(List<String> ids, User user) throws TException {
        return call(() -> toThriftReleases(client().getReleasesByListIds(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) throws TException {
        return call(() -> toThriftReleaseNodes(client().getReleaseRelationNetworkOfRelease(ReleaseConverter.fromThrift(release), UserConverter.fromThrift(user))));
    }

    private static <T> T call(Supplier<T> supplier) throws TException {
        try {
            return supplier.get();
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
            SW360Exception thriftEx = new SW360Exception(e.getMessage());
            if (e.getErrorCode() != null) {
                thriftEx.setErrorCode(e.getErrorCode());
            }
            throw thriftEx;
        }
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
