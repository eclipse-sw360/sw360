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

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;

/**
 * Client API for the components backend service.
 *
 * Callers use this instead of the former Thrift {@code ComponentService.Iface}.
 * Types are service-api POJOs. See {@link ComponentServiceRestClient} and {@link ComponentClients}.
 */
public interface ComponentClient {

    List<org.eclipse.sw360.datahandler.services.components.Component> getComponentsShort(Set<String> ids);

    List<org.eclipse.sw360.datahandler.services.components.Component> getComponentSummary(org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Component> getRecentComponentsSummary(int limit, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Component> getAccessibleRecentComponentsSummary(int limit, org.eclipse.sw360.datahandler.services.users.User user);

    int getTotalComponentsCount(org.eclipse.sw360.datahandler.services.users.User user);

    int getAccessibleTotalComponentsCount(org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleaseSummary(org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleaseSummary(org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    List<org.eclipse.sw360.datahandler.services.components.Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> refineSearchAccessibleComponents(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    List<org.eclipse.sw360.datahandler.services.components.Component> refineSearchWithAccessibility(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Component> getMyComponents(org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> searchAccessibleReleases(String searchText, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    List<org.eclipse.sw360.datahandler.services.components.Release> searchReleaseByNamePrefix(String name);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> searchReleaseByNamePaginated(String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleNewReleasesWithSrc(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByNamePrefixPaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByExactNamePaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> searchComponentByExactValues(Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    List<org.eclipse.sw360.datahandler.services.components.Component> getSubscribedComponents(org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getSubscribedReleases(org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getRecentReleases();

    List<org.eclipse.sw360.datahandler.services.components.Release> getRecentReleasesWithAccessibility(org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Component getComponentById(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Component getAccessibleComponentById(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Component getComponentByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Component getAccessibleComponentByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Release getReleaseById(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Release getAccessibleReleaseById(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Release getReleaseByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.Release getAccessibleReleaseByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByIdsForExport(Set<String> ids);

    List<String> getReleaseIdsFromComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getFullReleasesById(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesWithPermissions(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromVendorId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromVendorIds(Set<String> ids);

    List<org.eclipse.sw360.datahandler.services.components.Release> getAccessibleReleasesFromVendorIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByVendorId(String vendorId);

    org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addComponent(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponent(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponentWithForceFlag(org.eclipse.sw360.datahandler.services.components.Component component, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate);

    org.eclipse.sw360.datahandler.services.common.RequestSummary updateComponents(Set<org.eclipse.sw360.datahandler.services.components.Component> components, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateComponentFromModerationRequest(org.eclipse.sw360.datahandler.services.components.Component componentAdditions, org.eclipse.sw360.datahandler.services.components.Component componentDeletions, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus mergeComponents(String componentTargetId, String componentSourceId, org.eclipse.sw360.datahandler.services.components.Component componentSelection, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseWithForceFlag(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseFossology(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestSummary updateReleases(Set<org.eclipse.sw360.datahandler.services.components.Release> releases, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestSummary updateReleasesDirectly(Set<org.eclipse.sw360.datahandler.services.components.Release> releases, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleaseFromModerationRequest(org.eclipse.sw360.datahandler.services.components.Release releaseAdditions, org.eclipse.sw360.datahandler.services.components.Release releaseDeletions, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, org.eclipse.sw360.datahandler.services.components.Release releaseSelection, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReferencingReleases(String releaseId);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteComponent(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteComponentWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteRelease(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteReleaseWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFullDocsFromComponentId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Release> getReleasesFromComponentIdWithPagination(String id, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsForRelease(String releaseId);

    Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsForComponent(Set<String> releaseIds);

    Set<org.eclipse.sw360.datahandler.services.components.Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.components.Component> getComponentsByDefaultVendorId(String defaultVendorId);

    boolean releaseIsUsed(String releaseId);

    boolean componentIsUsed(String componentId);

    org.eclipse.sw360.datahandler.services.components.Component recomputeReleaseDependentFields(String componentId, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.components.BulkOperationNode deleteBulkRelease(String releaseId, org.eclipse.sw360.datahandler.services.users.User user, boolean isPreview);

    org.eclipse.sw360.datahandler.services.common.RequestStatus subscribeComponent(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus subscribeRelease(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus unsubscribeComponent(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus unsubscribeRelease(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Component> getComponentSummaryForExport();

    List<org.eclipse.sw360.datahandler.services.components.Component> getComponentDetailedSummaryForExport();

    List<org.eclipse.sw360.datahandler.services.components.Component> searchComponentForExport(String name, boolean caseSensitive);

    org.eclipse.sw360.datahandler.services.components.Component getComponentForReportFromFossologyUploadId(String uploadId);

    Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> getSourceAttachments(String releaseId);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleases(Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> relations);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship> relations, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleaseRelations(Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> relations);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship> relations, org.eclipse.sw360.datahandler.services.users.User user);

    Set<String> getUsedAttachmentContentIds();

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateReleasesWithSvmTrackingFeedback();

    org.eclipse.sw360.datahandler.services.common.RequestStatus uploadSourceCodeAttachmentToReleases();

    Map<String, List<String>> getDuplicateComponents();

    Map<String, List<String>> getDuplicateReleases();

    Map<String, List<String>> getDuplicateReleaseSources();

    Set<org.eclipse.sw360.datahandler.services.components.Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds);

    Set<org.eclipse.sw360.datahandler.services.components.Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds);

    String getCyclicLinkedReleasePath(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation prepareImportBom(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId);

    org.eclipse.sw360.datahandler.services.common.RequestSummary importBomFromAttachmentContent(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId);

    org.eclipse.sw360.datahandler.services.common.RequestStatus splitComponent(org.eclipse.sw360.datahandler.services.components.Component srcComponent, org.eclipse.sw360.datahandler.services.components.Component targetComponent, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.Release> getAllReleasesForUser(org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.components.Component> getRecentComponentsSummaryWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    void sendExportSpreadsheetSuccessMail(String url, String recepient);

    byte[] downloadExcel(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String token);

    byte[] getComponentReportDataStream(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases);

    String getComponentReportInEmail(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases);

    boolean isReleaseActionAllowed(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user, RequestedAction action);

    List<org.eclipse.sw360.datahandler.services.components.Release> getReleasesByListIds(List<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> getReleaseRelationNetworkOfRelease(org.eclipse.sw360.datahandler.services.components.Release release, org.eclipse.sw360.datahandler.services.users.User user);
}
