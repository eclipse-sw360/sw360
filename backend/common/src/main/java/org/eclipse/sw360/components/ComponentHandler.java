/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ComponentSearchHandler;
import org.eclipse.sw360.datahandler.db.ReleaseSearchHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseImmutableField;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;

import com.ibm.cloud.cloudant.v1.Cloudant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * POJO façade for component services. Speaks service-api POJOs with ComponentDatabaseHandler.
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ComponentHandler {

    private final ComponentDatabaseHandler handler;
    private final ComponentSearchHandler componentSearchHandler;
    private final ReleaseSearchHandler releaseSearchHandler;

    public ComponentHandler() throws IOException {
        this(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_CHANGE_LOGS, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    public ComponentHandler(Cloudant cClient, String dbName, String changeLogsDBName, String attachmentDbName) throws IOException {
        handler = new ComponentDatabaseHandler(cClient, dbName, changeLogsDBName, attachmentDbName);
        componentSearchHandler = new ComponentSearchHandler(cClient, dbName);
        releaseSearchHandler = new ReleaseSearchHandler(cClient, dbName);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////
    public List<Component> getComponentsShort(Set<String> ids) {
        return handler.getComponentsShort(ids);
    }

    public List<Component> getComponentSummary(User user) throws TException {
        assertUser(user);

        return handler.getComponentSummary(user);
    }

    public List<Component> getRecentComponentsSummary(int limit, User user) throws TException {
        assertUser(user);

        return handler.getRecentComponentsSummary(limit, user);
    }

    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) throws TException
    {
        return handler.getAccessibleRecentComponentsSummary(limit, user);
    }

    public int getTotalComponentsCount(User user) throws TException {
        assertUser(user);
        return handler.getTotalComponentsCount();
    }

    public int getAccessibleTotalComponentsCount(User user) throws TException {
        assertUser(user);
        return handler.getAccessibleTotalComponentsCount(user);
    }

    public List<Release> getReleaseSummary(User user) throws TException {
        assertUser(user);

        return handler.getReleaseSummary();
    }

    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        assertUser(user);

        return handler.getAccessibleReleaseSummary(user);
    }

    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) throws TException {
        assertUser(user);
        return withPojoPagination(pageData, p -> handler.getAccessibleReleasesWithPagination(user, p));
    }

    public List<Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return componentSearchHandler.search(text, subQueryRestrictions);
    }

    public Map<PaginationData, List<Component>> refineSearchAccessibleComponents(String text, Map<String,Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        return withPojoPagination(pageData, p -> componentSearchHandler.searchAccessibleComponents(text, subQueryRestrictions, user, p));
    }

    public List<Component> refineSearchWithAccessibility(String text, Map<String,Set<String>> subQueryRestrictions, User user) throws TException {
        return componentSearchHandler.searchWithAccessibility(text, subQueryRestrictions, user);
    }

    public List<Component> getMyComponents(User user) throws TException {
        assertUser(user);

        return handler.getMyComponents(user.getEmail());
    }

    public Map<PaginationData, List<Release>> searchAccessibleReleases(String searchText, User user, PaginationData pageData) throws TException {
        return withPojoPagination(pageData, p -> handler.searchAccessibleReleasesByText(releaseSearchHandler, searchText, user, p));
    }

    public List<Release> searchReleaseByNamePrefix(String name) throws TException {
        return handler.searchReleaseByNamePrefix(name);
    }

    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData) throws TException {
        return withPojoPagination(pageData, p -> handler.searchReleaseByNamePaginated(name, p));
    }

    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(User user, PaginationData pageData) throws TException {
        assertUser(user);
        return withPojoPagination(pageData, p -> handler.getAccessibleNewReleasesWithSrc(user, p));
    }

    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name, PaginationData pageData) {
        return withPojoPagination(pageData, p -> handler.searchComponentByNamePrefixPaginated(user, name, p));
    }

    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name, PaginationData pageData) {
        return withPojoPagination(pageData, p -> handler.searchComponentByExactNamePaginated(user, name, p));
    }

    public Map<PaginationData, List<Component>> searchComponentByExactValues(Map<String,Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        assertUser(user);

        return withPojoPagination(pageData, p -> handler.searchComponentByExactValues(subQueryRestrictions, user, p));
    }

    public List<Component> getSubscribedComponents(User user) throws TException {
        assertUser(user);

        return handler.getSubscribedComponents(user.getEmail());
    }

    public List<Release> getSubscribedReleases(User user) throws TException {
        assertUser(user);

        return handler.getSubscribedReleases(user.getEmail());
    }

    public List<Release> getRecentReleases() throws TException {
        return handler.getRecentReleases();
    }

    public List<Release> getRecentReleasesWithAccessibility(User user) throws TException {
        return handler.getRecentReleasesWithAccessibility(user);
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////
    public Component getComponentById(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        Component component = handler.getComponent(id, user);
        handler.addSelectLogs(component, user);
        return component;
    }

    public Component getAccessibleComponentById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleComponent(id, user);
    }

    public Component getComponentByIdForEdit(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getComponentForEdit(id, user);
    }

    public Component getAccessibleComponentByIdForEdit(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleComponentForEdit(id, user);
    }

    public Release getReleaseById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        Release release = handler.getRelease(id, user);
        handler.addSelectLogs(release, user);
        return release;
    }

    public Release getAccessibleReleaseById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleRelease(id, user);
    }

    public Release getReleaseByIdForEdit(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getReleaseForEdit(id, user);
    }

    public Release getAccessibleReleaseByIdForEdit(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleReleaseForEdit(id, user);
    }

    public List<Release> getReleasesByIdsForExport(Set<String> ids) throws TException {
        assertNotNull(ids);
        return handler.getDetailedReleasesForExport(ids);
    }

    public List<String> getReleaseIdsFromComponentId(String id, User user) throws TException {
        assertNotNull(id);
        return handler.getReleaseIdsFromComponentId(id,user);
    }

    public List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException {
        assertNotNull(ids);
        assertUser(user);
        return handler.getDetailedReleasesWithAccessibilityForExport(ids, user);
    }

    public List<Release> getReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleases(ids);
    }

    public List<Release> getAccessibleReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getAccessibleReleases(ids, user);
    }

    public List<Release> getFullReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getFullReleases(ids);
    }

    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleasesWithPermissions(ids, user);
    }

    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        assertUser(user);
        assertNotNull(id);
        return handler.getReleasesFromVendorId(id, user);
    }

    public List<Release> getReleasesFromVendorIds(Set<String> ids) throws TException {
        return handler.getReleasesFromVendorIds(ids);
    }

    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) throws TException {
        return handler.getAccessibleReleasesFromVendorIds(ids, user);
    }

    public Set<Release> getReleasesByVendorId(String vendorId) throws TException {
        return handler.getReleasesByVendorId(vendorId);
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////
    public AddDocumentRequestSummary addComponent(Component component, User user) throws TException {
        assertNotNull(component);
        assertIdUnset(component.getId());
        assertUser(user);
        assertNotNull(component.getComponentType(), "ComponentType is not present on the request");

        return handler.addComponent(component, user.getEmail());
    }

    public AddDocumentRequestSummary addRelease(Release release, User user) throws TException {
        assertNotNull(release);
        assertIdUnset(release.getId());
        assertUser(user);

        return handler.addRelease(release, user);
    }

    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus updateComponent(Component component, User user) throws TException {
        assertNotNull(component);
        assertId(component.getId());
        assertUser(user);

        return handler.updateComponent(component, user);
    }

    public RequestStatus updateComponentWithForceFlag(Component component, User user, boolean forceUpdate) throws TException {
        assertNotNull(component);
        assertId(component.getId());
        assertUser(user);

        return handler.updateComponent(component, user, forceUpdate);
    }

    public RequestSummary updateComponents(Set<Component> components, User user) throws TException {
        assertUser(user);

        return handler.updateComponents(components, user);
    }

    public RequestStatus updateComponentFromModerationRequest(Component componentAdditions, Component componentDeletions, User user) {
        return handler.updateComponentFromAdditionsAndDeletions(componentAdditions, componentDeletions, user);
    }

    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId, Component componentSelection,
            User user) throws TException {
        return handler.mergeComponents(componentTargetId, componentSourceId, componentSelection, user);
    }

    public RequestStatus updateRelease(Release release, User user) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);
        removeSelfLink(release);
        return handler.updateRelease(release, user, ReleaseImmutableField.DEFAULT);
    }

    public RequestStatus updateReleaseWithForceFlag(Release release, User user, boolean forceUpdate) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);
        removeSelfLink(release);
        return handler.updateRelease(release, user, ReleaseImmutableField.DEFAULT, forceUpdate);
    }

    private void removeSelfLink(Release release) {
        if(release.getReleaseIdToRelationship() != null && !release.getReleaseIdToRelationship().isEmpty()) {
            release.getReleaseIdToRelationship().remove(release.getId());
        }
    }

    public RequestStatus updateReleaseFossology(Release release, User user) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);

        return handler.updateRelease(release, user, ReleaseImmutableField.FOR_FOSSOLOGY);
    }

    public RequestSummary updateReleases(Set<Release> releases, User user) throws TException {
        assertUser(user);
        return handler.updateReleases(releases, user, false);
    }

    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws TException {
        assertUser(user);
        return handler.updateReleasesDirectly(releases, user);
    }

    public RequestStatus updateReleaseFromModerationRequest(Release releaseAdditions, Release releaseDeletions, User user) {
        return handler.updateReleaseFromAdditionsAndDeletions(releaseAdditions, releaseDeletions, user);
    }

    public RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, Release releaseSelection,
            User user) throws TException {
        return handler.mergeReleases(releaseTargetId, releaseSourceId, releaseSelection, user);
    }

    public List<Release> getReferencingReleases(String releaseId) throws TException {
        return handler.getReferencingReleases(releaseId);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus deleteComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteComponent(id, user);
    }

    public RequestStatus deleteComponentWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteComponent(id, user, forceDelete);
    }

    public RequestStatus deleteRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteRelease(id, user);
    }

    public RequestStatus deleteReleaseWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteRelease(id, user, forceDelete);
    }

    public List<Release> getReleasesByComponentId(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.getReleasesFromComponentId(id, user);

    }

    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.getReleasesFullDocsFromComponentId(id, user);

    }

    public Map<PaginationData, List<Release>> getReleasesFromComponentIdWithPagination(String id, User user, PaginationData pageData) throws TException {
        assertUser(user);
        assertId(id);

        return withPojoPagination(pageData, p -> handler.getReleasesFromComponentIdWithPagination(id, user, p));
    }

    public Set<Component> getUsingComponentsForRelease(String releaseId) throws TException {
        return handler.getUsingComponents(releaseId);
    }

    public Set<Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, User user) throws TException {
        return handler.getUsingComponentsWithAccessibility(releaseId, user);
    }

    public Set<Component> getUsingComponentsForComponent(Set<String> releaseIds) throws TException {
        return handler.getUsingComponents(releaseIds);
    }

    public Set<Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, User user) throws TException {
        return handler.getUsingComponentsWithAccessibility(releaseIds, user);
    }

    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) throws TException {
        return handler.getComponentsByDefaultVendorId(defaultVendorId);
    }

    public boolean releaseIsUsed(String releaseId) throws TException {
        return handler.checkIfInUse(releaseId);
    }

    public boolean componentIsUsed(String componentId) throws TException {
        return handler.checkIfInUseComponent(componentId);
    }

    public Component recomputeReleaseDependentFields(String componentId, User user) throws TException {
        assertUser(user);
        assertId(componentId);
        return handler.updateReleaseDependentFieldsForComponentId(componentId, user);
    }

    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws SW360Exception {
        return handler.deleteBulkRelease(releaseId, user, isPreview);
    }

    //////////////////////////////////
    // SUBSCRIBE INDIVIDUAL OBJECTS //
    //////////////////////////////////
    public RequestStatus subscribeComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.subscribeComponent(id, user);
    }

    public RequestStatus subscribeRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.subscribeRelease(id, user);
    }

    public RequestStatus unsubscribeComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.unsubscribeComponent(id, user);
    }

    public RequestStatus unsubscribeRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.unsubscribeRelease(id, user);
    }

    /////////////////////
    // EXCEL EXPORT    //
    /////////////////////
    public List<Component> getComponentSummaryForExport() throws TException {
        return handler.getComponentSummaryForExport();
    }

    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return handler.getComponentDetailedSummaryForExport();
    }

    public List<Component> searchComponentForExport(String name, boolean caseSensitive) throws TException {
        return handler.searchComponentByNameForExport(name, caseSensitive);
    }

    public Component getComponentForReportFromFossologyUploadId(String uploadId) throws TException {
        return handler.getComponentForReportFromFossologyUploadId(uploadId);
    }

    public Set<Attachment> getSourceAttachments(String releaseId) throws TException {
        return handler.getSourceAttachments(releaseId);
    }

    public List<ReleaseLink> getLinkedReleases(Map<String, ProjectReleaseRelationship> relations) throws TException {
        assertNotNull(relations);

        return handler.getLinkedReleases(relations);
    }

    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ProjectReleaseRelationship> relations, User user) throws TException {
        assertNotNull(relations);

        return handler.getLinkedReleasesWithAccessibility(relations, user);
    }

    public List<ReleaseLink> getLinkedReleaseRelations(Map<String, ReleaseRelationship> relations) throws TException {
        return handler.getLinkedReleases(relations);
    }

    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, ReleaseRelationship> relations, User user) throws TException {
        return handler.getLinkedReleasesWithAccessibility(relations, user);
    }

    public Set<String> getUsedAttachmentContentIds() throws TException {
        return handler.getusedAttachmentContentIds();
    }

    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return handler.updateReleasesWithSvmTrackingFeedback();
    }

    public RequestStatus uploadSourceCodeAttachmentToReleases() throws TException {
        return handler.uploadSourceCodeAttachmentToReleases();
    }

    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return handler.getDuplicateComponents();
    }

    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return handler.getDuplicateReleases();
    }

    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return handler.getDuplicateReleaseSources();
    }

    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        assertNotNull(externalIds);
        return handler.searchComponentsByExternalIds(externalIds);
    }

    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        assertNotNull(externalIds);
        return handler.searchReleasesByExternalIds(externalIds);
    }

    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        assertNotNull(release);
        assertUser(user);

        return handler.getCyclicLinkedReleasePath(release, user);
    }

    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.prepareImportBom(user, attachmentContentId);
    }

    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.importBomFromAttachmentContent(user, attachmentContentId);
    }

    public RequestStatus splitComponent(Component srcComponent, Component targetComponent, User user) throws TException {
        assertNotNull(srcComponent);
        assertId(srcComponent.getId());
        assertNotNull(targetComponent);
        assertId(targetComponent.getId());
        assertUser(user);
        return handler.splitComponent(srcComponent, targetComponent, user);
    }

    public List<Release> getAllReleasesForUser(User user) throws TException {
        assertUser(user);
        return handler.getAllReleases();
    }

    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        return withPojoPagination(pageData, p -> handler.getRecentComponentsSummaryWithPagination(user, p));
    }

    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        handler.sendExportSpreadsheetSuccessMail(url, recepient);
    }

    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return handler.downloadExcel(user,extendedByReleases,token);
    }
	public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException {
		return handler.getComponentReportDataStream(user,extendedByReleases);
	}
	public String getComponentReportInEmail(User user, boolean extendedByReleases) throws TException {
		return handler.getComponentReportInEmail(user,extendedByReleases);
	}

    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) {
        return handler.isReleaseActionAllowed(release, user, action);
    }

    public List<Release> getReleasesByListIds(List<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleaseByIds(ids);
    }

    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) {
        return handler.getReleaseRelationNetworkOfRelease(release, user);
    }

    @FunctionalInterface
    private interface PojoPaginationCall<T> {
        Map<org.eclipse.sw360.datahandler.services.common.PaginationData, List<T>> call(
                org.eclipse.sw360.datahandler.services.common.PaginationData pageData) throws TException;
    }

    private static <T> Map<PaginationData, List<T>> withPojoPagination(
            PaginationData thriftPage, PojoPaginationCall<T> call) {
        try {
            org.eclipse.sw360.datahandler.services.common.PaginationData pojo =
                    org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter.fromThrift(thriftPage);
            Map<org.eclipse.sw360.datahandler.services.common.PaginationData, List<T>> result = call.call(pojo);
            if (result == null || result.isEmpty()) {
                return java.util.Collections.singletonMap(thriftPage, java.util.List.of());
            }
            Map.Entry<org.eclipse.sw360.datahandler.services.common.PaginationData, List<T>> entry =
                    result.entrySet().iterator().next();
            org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter.copyTotalsToThrift(entry.getKey(), thriftPage);
            return java.util.Collections.singletonMap(thriftPage, entry.getValue());
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }
}
