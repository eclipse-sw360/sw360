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
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ComponentHandler implements ComponentService.Iface {

    private final ComponentDatabaseHandler handler;
    private final ComponentSearchHandler componentSearchHandler;
    private final ReleaseSearchHandler releaseSearchHandler;

    public ComponentHandler() throws IOException {
        this(DatabaseSettings.getConfiguredClient(), DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_CHANGE_LOGS, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    public ComponentHandler(Supplier<CloudantClient> cClient, Supplier<HttpClient> hclient, String dbName, String changeLogsDBName, String attachmentDbName) throws IOException {
        handler = new ComponentDatabaseHandler(cClient, dbName, changeLogsDBName, attachmentDbName);
        componentSearchHandler = new ComponentSearchHandler(hclient, cClient, dbName);
        releaseSearchHandler = new ReleaseSearchHandler(hclient, cClient, dbName);
    }

    // TODO use dependency injection instead of this constructors mess
    public ComponentHandler(ThriftClients thriftClients) throws IOException {
        this(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_CHANGE_LOGS, DatabaseSettings.COUCH_DB_ATTACHMENTS, thriftClients);
    }

    public ComponentHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> client, String dbName, String changeLogsDBName, String attachmentDbName, ThriftClients thriftClients) throws IOException {
        handler = new ComponentDatabaseHandler(client, dbName, changeLogsDBName, attachmentDbName, thriftClients);
        componentSearchHandler = new ComponentSearchHandler(httpClient, client, dbName);
        releaseSearchHandler = new ReleaseSearchHandler(httpClient, client, dbName);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////
    @Override
    public List<Component> getComponentsShort(Set<String> ids) {
        return handler.getComponentsShort(ids);
    }

    @Override
    public List<Component> getComponentSummary(User user) throws TException {
        assertUser(user);

        return handler.getComponentSummary(user);
    }

    @Override
    public List<Component> getRecentComponentsSummary(int limit, User user) throws TException {
        assertUser(user);

        return handler.getRecentComponentsSummary(limit, user);
    }

    @Override
    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) throws TException
    {
        return handler.getAccessibleRecentComponentsSummary(limit, user);
    }

    @Override
    public int getTotalComponentsCount(User user) throws TException {
        assertUser(user);
        return handler.getTotalComponentsCount();
    }

    @Override
    public int getAccessibleTotalComponentsCount(User user) throws TException {
        assertUser(user);
        return handler.getAccessibleTotalComponentsCount(user);
    }
    
    @Override
    public List<Release> getReleaseSummary(User user) throws TException {
        assertUser(user);

        return handler.getReleaseSummary();
    }

    @Override
    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        assertUser(user);

        return handler.getAccessibleReleaseSummary(user);
    }
    
    @Override
    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) throws TException {
        assertUser(user);
        return handler.getAccessibleReleasesWithPagination(user, pageData);
    }

    @Override
    public List<Component> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return componentSearchHandler.search(text, subQueryRestrictions);
    }

    @Override
    public List<Component> refineSearchAccessibleComponents(String text, Map<String,Set<String>> subQueryRestrictions, User user) throws TException {
        return componentSearchHandler.searchAccessibleComponents(text, subQueryRestrictions, user);
    }

    @Override
    public List<Component> refineSearchWithAccessibility(String text, Map<String,Set<String>> subQueryRestrictions, User user) throws TException {
        return componentSearchHandler.searchWithAccessibility(text, subQueryRestrictions, user);
    }

    @Override
    public List<Component> getMyComponents(User user) throws TException {
        assertUser(user);

        return handler.getMyComponents(user.getEmail());
    }

    @Override
    public List<Release> searchReleases(String searchText) throws TException {
        return releaseSearchHandler.search(searchText);
    }

    @Override
    public List<Release> searchAccessibleReleases(String searchText, User user) throws TException {
        return handler.searchAccessibleReleasesByText(releaseSearchHandler, searchText, user) ;
    }
    
    @Override
    public List<Release> searchReleaseByNamePrefix(String name) throws TException {
        return handler.searchReleaseByNamePrefix(name);
    }

    @Override
    public List<Component> getSubscribedComponents(User user) throws TException {
        assertUser(user);

        return handler.getSubscribedComponents(user.getEmail());
    }

    @Override
    public List<Release> getSubscribedReleases(User user) throws TException {
        assertUser(user);

        return handler.getSubscribedReleases(user.getEmail());
    }

    @Override
    public List<Release> getRecentReleases() throws TException {
        return handler.getRecentReleases();
    }

    @Override
    public List<Release> getRecentReleasesWithAccessibility(User user) throws TException {
        return handler.getRecentReleasesWithAccessibility(user);
    }
    
    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    @Override
    public Component getComponentById(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        Component component = handler.getComponent(id, user);
        handler.addSelectLogs(component, user);
        return component;
    }
    
    @Override
    public Component getAccessibleComponentById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleComponent(id, user);
    }

    @Override
    public Component getComponentByIdForEdit(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getComponentForEdit(id, user);
    }

    @Override
    public Component getAccessibleComponentByIdForEdit(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleComponentForEdit(id, user);
    }

    @Override
    public Release getReleaseById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        Release release = handler.getRelease(id, user);
        handler.addSelectLogs(release, user);
        return release;
    }

    @Override
    public Release getAccessibleReleaseById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleRelease(id, user);
    }

    @Override
    public Release getReleaseByIdForEdit(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getReleaseForEdit(id, user);
    }

    @Override
    public Release getAccessibleReleaseByIdForEdit(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);

        return handler.getAccessibleReleaseForEdit(id, user);
    }
    
    @Override
    public List<Release> getReleasesByIdsForExport(Set<String> ids) throws TException {
        assertNotNull(ids);
        return handler.getDetailedReleasesForExport(ids);
    }

    @Override
    public List<String> getReleaseIdsFromComponentId(String id, User user) throws TException {
        assertNotNull(id);
        return handler.getReleaseIdsFromComponentId(id,user);
    }

    @Override
    public List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException {
        assertNotNull(ids);
        assertUser(user);
        return handler.getDetailedReleasesWithAccessibilityForExport(ids, user);
    }
    
    @Override
    public List<Release> getReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleases(ids);
    }

    @Override
    public List<Release> getAccessibleReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getAccessibleReleases(ids, user);
    }
    
    @Override
    public List<Release> getFullReleasesById(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getFullReleases(ids);
    }

    @Override
    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleasesWithPermissions(ids, user);
    }

    @Override
    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        assertUser(user);
        assertNotNull(id);
        return handler.getReleasesFromVendorId(id, user);
    }

    @Override
    public List<Release> getReleasesFromVendorIds(Set<String> ids) throws TException {
        return handler.getReleasesFromVendorIds(ids);
    }

    @Override
    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) throws TException {
        return handler.getAccessibleReleasesFromVendorIds(ids, user);
    }
    
    @Override
    public Set<Release> getReleasesByVendorId(String vendorId) throws TException {
        return handler.getReleasesByVendorId(vendorId);
    }
    
    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////

    @Override
    public AddDocumentRequestSummary addComponent(Component component, User user) throws TException {
        assertNotNull(component);
        assertIdUnset(component.getId());
        assertUser(user);
        assertNotNull(component.getComponentType(), "ComponentType is not present on the request");

        return handler.addComponent(component, user.getEmail());
    }

    @Override
    public AddDocumentRequestSummary addRelease(Release release, User user) throws TException {
        assertNotNull(release);
        assertIdUnset(release.getId());
        assertUser(user);

        return handler.addRelease(release, user);
    }

    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    @Override
    public RequestStatus updateComponent(Component component, User user) throws TException {
        assertNotNull(component);
        assertId(component.getId());
        assertUser(user);

        return handler.updateComponent(component, user);
    }
    
    @Override
    public RequestStatus updateComponentWithForceFlag(Component component, User user, boolean forceUpdate) throws TException {
        assertNotNull(component);
        assertId(component.getId());
        assertUser(user);

        return handler.updateComponent(component, user, forceUpdate);
    }
    
    @Override
    public RequestSummary updateComponents(Set<Component> components, User user) throws TException {
        assertUser(user);

        return handler.updateComponents(components, user);
    }

    public RequestStatus updateComponentFromModerationRequest(Component componentAdditions, Component componentDeletions, User user) {
        return handler.updateComponentFromAdditionsAndDeletions(componentAdditions, componentDeletions, user);
    }

    @Override
    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId, Component componentSelection,
            User user) throws TException {
        return handler.mergeComponents(componentTargetId, componentSourceId, componentSelection, user);
    }

    @Override
    public RequestStatus updateRelease(Release release, User user) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);
        removeSelfLink(release);
        return handler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE);
    }

    @Override
    public RequestStatus updateReleaseWithForceFlag(Release release, User user, boolean forceUpdate) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);
        removeSelfLink(release);
        return handler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, forceUpdate);
    }
    
    private void removeSelfLink(Release release) {
        if(release.releaseIdToRelationship != null && !release.releaseIdToRelationship.isEmpty()) {
            release.releaseIdToRelationship.remove(release.id);
        }
    }

    @Override
    public RequestStatus updateReleaseFossology(Release release, User user) throws TException {
        assertNotNull(release);
        assertId(release.getId());
        assertUser(user);

        return handler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE_FOR_FOSSOLOGY);
    }

    @Override
    public RequestSummary updateReleases(Set<Release> releases, User user) throws TException {
        assertUser(user);
        return handler.updateReleases(releases, user, false);
    }

    @Override
    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws TException {
        assertUser(user);
        return handler.updateReleasesDirectly(releases, user);
    }

    public RequestStatus updateReleaseFromModerationRequest(Release releaseAdditions, Release releaseDeletions, User user) {
        return handler.updateReleaseFromAdditionsAndDeletions(releaseAdditions, releaseDeletions, user);
    }

    @Override
    public RequestStatus mergeReleases(String releaseTargetId, String releaseSourceId, Release releaseSelection,
            User user) throws TException {
        return handler.mergeReleases(releaseTargetId, releaseSourceId, releaseSelection, user);
    }

    @Override
    public List<Release> getReferencingReleases(String releaseId) throws TException {
        return handler.getReferencingReleases(releaseId);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    @Override
    public RequestStatus deleteComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteComponent(id, user);
    }

    @Override
    public RequestStatus deleteComponentWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteComponent(id, user, forceDelete);
    }
    
    @Override
    public RequestStatus deleteRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteRelease(id, user);
    }

    @Override
    public RequestStatus deleteReleaseWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertUser(user);
        assertId(id);

        return handler.deleteRelease(id, user, forceDelete);
    }
    
    @Override
    public List<Release> getReleasesByComponentId(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.getReleasesFromComponentId(id, user);

    }

    @Override
    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.getReleasesFullDocsFromComponentId(id, user);

    }

    @Override
    public Set<Component> getUsingComponentsForRelease(String releaseId) throws TException {
        return handler.getUsingComponents(releaseId);
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForRelease(String releaseId, User user) throws TException {
        return handler.getUsingComponentsWithAccessibility(releaseId, user);
    }
    
    @Override
    public Set<Component> getUsingComponentsForComponent(Set<String> releaseIds) throws TException {
        return handler.getUsingComponents(releaseIds);
    }

    @Override
    public Set<Component> getUsingComponentsWithAccessibilityForComponent(Set<String> releaseIds, User user) throws TException {
        return handler.getUsingComponentsWithAccessibility(releaseIds, user);
    }
    
    @Override
    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) throws TException {
        return handler.getComponentsByDefaultVendorId(defaultVendorId);
    }

    @Override
    public boolean releaseIsUsed(String releaseId) throws TException {
        return handler.checkIfInUse(releaseId);
    }

    @Override
    public boolean componentIsUsed(String componentId) throws TException {
        return handler.checkIfInUseComponent(componentId);
    }

    @Override
    public Component recomputeReleaseDependentFields(String componentId, User user) throws TException {
        assertUser(user);
        assertId(componentId);
        return handler.updateReleaseDependentFieldsForComponentId(componentId, user);
    }
    
    @Override
    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws SW360Exception {
        return handler.deleteBulkRelease(releaseId, user, isPreview);
    }

    //////////////////////////////////
    // SUBSCRIBE INDIVIDUAL OBJECTS //
    //////////////////////////////////
    @Override
    public RequestStatus subscribeComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.subscribeComponent(id, user);
    }

    @Override
    public RequestStatus subscribeRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.subscribeRelease(id, user);
    }

    @Override
    public RequestStatus unsubscribeComponent(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.unsubscribeComponent(id, user);
    }

    @Override
    public RequestStatus unsubscribeRelease(String id, User user) throws TException {
        assertUser(user);
        assertId(id);

        return handler.unsubscribeRelease(id, user);
    }

    /////////////////////
    // EXCEL EXPORT    //
    /////////////////////
    @Override
    public List<Component> getComponentSummaryForExport() throws TException {
        return handler.getComponentSummaryForExport();
    }

    @Override
    public List<Component> getComponentDetailedSummaryForExport() throws TException {
        return handler.getComponentDetailedSummaryForExport();
    }

    @Override
    public List<Component> searchComponentForExport(String name, boolean caseSensitive) throws TException {
        return handler.searchComponentByNameForExport(name, caseSensitive);
    }

    @Override
    public Component getComponentForReportFromFossologyUploadId(String uploadId) throws TException {
        return handler.getComponentForReportFromFossologyUploadId(uploadId);
    }

    @Override
    public Set<Attachment> getSourceAttachments(String releaseId) throws TException {
        return handler.getSourceAttachments(releaseId);
    }

    @Override
    public List<ReleaseLink> getLinkedReleases(Map<String, ProjectReleaseRelationship> relations) throws TException {
        assertNotNull(relations);

        return handler.getLinkedReleases(relations);
    }

    @Override
    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ProjectReleaseRelationship> relations, User user) throws TException {
        assertNotNull(relations);

        return handler.getLinkedReleasesWithAccessibility(relations, user);
    }
    
    @Override
    public List<ReleaseLink> getLinkedReleaseRelations(Map<String, ReleaseRelationship> relations) throws TException {
        return handler.getLinkedReleases(relations);
    }

    @Override
    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Map<String, ReleaseRelationship> relations, User user) throws TException {
        return handler.getLinkedReleasesWithAccessibility(relations, user);
    }

    @Override
    public Set<String> getUsedAttachmentContentIds() throws TException {
        return handler.getusedAttachmentContentIds();
    }

    @Override
    public RequestStatus updateReleasesWithSvmTrackingFeedback() throws TException {
        return handler.updateReleasesWithSvmTrackingFeedback();
    }

    @Override
    public Map<String, List<String>> getDuplicateComponents() throws TException {
        return handler.getDuplicateComponents();
    }

    @Override
    public Map<String, List<String>> getDuplicateReleases() throws TException {
        return handler.getDuplicateReleases();
    }

    @Override
    public Map<String, List<String>> getDuplicateReleaseSources() throws TException {
        return handler.getDuplicateReleaseSources();
    }

    @Override
    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        assertNotNull(externalIds);
        return handler.searchComponentsByExternalIds(externalIds);
    }

    @Override
    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) throws TException {
        assertNotNull(externalIds);
        return handler.searchReleasesByExternalIds(externalIds);
    }

    @Override
    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        assertNotNull(release);
        assertUser(user);

        return handler.getCyclicLinkedReleasePath(release, user);
    }

    @Override
    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.prepareImportBom(user, attachmentContentId);
    }

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.importBomFromAttachmentContent(user, attachmentContentId);
    }

    @Override
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

    @Override
    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        return handler.getRecentComponentsSummaryWithPagination(user, pageData);
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        handler.sendExportSpreadsheetSuccessMail(url, recepient);
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return handler.downloadExcel(user,extendedByReleases,token);
    }

	@Override
	public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException {
		return handler.getComponentReportDataStream(user,extendedByReleases);
	}

	@Override
	public String getComponentReportInEmail(User user, boolean extendedByReleases) throws TException {
		return handler.getComponentReportInEmail(user,extendedByReleases);
	}

    @Override
    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) {
        return handler.isReleaseActionAllowed(release, user, action);
    }

    @Override
    public List<Release> getReleasesByListIds(List<String> ids, User user) throws TException {
        assertUser(user);
        assertNotNull(ids);
        return handler.getReleaseByIds(ids);
    }

    @Override
    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) {
        return handler.getReleaseRelationNetworkOfRelease(release, user);
    }
}
