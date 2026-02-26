/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.projects;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertIdUnset;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectSearchHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.ImportBomDryRunReport;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.Cloudant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 * @author ksoranko@verifa.io
 */
public class ProjectHandler implements ProjectService.Iface {

    private static final Logger log = LogManager.getLogger(ProjectHandler.class);
    private final ProjectDatabaseHandler handler;
    private final ProjectSearchHandler searchHandler;

    ProjectHandler() throws IOException {
        handler = new ProjectDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE,
                DatabaseSettings.COUCH_DB_ATTACHMENTS);
        searchHandler = new ProjectSearchHandler(DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_DATABASE);
    }

    ProjectHandler(Cloudant client, String dbName, String attchmntDbName) throws IOException {
        handler = new ProjectDatabaseHandler(client, dbName, attchmntDbName);
        searchHandler = new ProjectSearchHandler(DatabaseSettings.getConfiguredClient(), dbName);
    }

    ProjectHandler(Cloudant client, String dbName, String changeLogsDbName, String attchmntDbName) throws IOException {
        handler = new ProjectDatabaseHandler(client, dbName, changeLogsDbName, attchmntDbName);
        searchHandler = new ProjectSearchHandler(client, dbName);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    @Override
    public List<Project> search(String text) throws TException {
        return searchHandler.search(text);
    }

    @Override
    public List<Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, User user)
            throws TException {
        return searchHandler.search(text, subQueryRestrictions, user);
    }

    @Override
    public Map<PaginationData, List<Project>> refineSearchPageable(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData paginationData) throws TException {
        return searchHandler.search(text, subQueryRestrictions, user, paginationData);
    }

    @Override
    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        assertNotNull(user);
        assertNotEmpty(user.getEmail());

        return handler.getMyProjectsFull(user, userRoles);
    }

    @Override
    public List<Project> getAccessibleProjectsSummary(User user) throws TException {
        assertUser(user);

        return handler.getAccessibleProjectsSummary(user);
    }

    @Override
    public Map<PaginationData, List<Project>> getAccessibleProjectsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        assertUser(user);
        return handler.getAccessibleProjectsSummary(user, pageData);
    }

    @Override
    public Set<Project> getAccessibleProjects(User user) throws TException {
        assertUser(user);

        return handler.getAccessibleProjects(user);
    }

    @Override
    public List<Project> searchByName(String name, User user) throws TException {
        assertNotEmpty(name);
        assertUser(user);

        return handler.searchByName(name, user);
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        assertNotEmpty(name);
        assertUser(user);

        return handler.searchProjectByNamePrefixPaginated(user, name, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        assertNotEmpty(name);
        assertUser(user);

        return handler.searchProjectByExactNamePaginated(user, name, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchAccessibleProjectByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        assertUser(user);

        return handler.searchAccessibleProjectByExactValues(subQueryRestrictions, user, pageData);
    }

    @Override
    public ProjectData searchByGroup(String group, User user) throws SW360Exception {
        assertNotEmpty(group);
        assertUser(user);

        return handler.searchByGroup(group, user);
    }

    @Override
    public ProjectData searchByTag(String tag, User user) throws SW360Exception {
        assertNotEmpty(tag);
        assertUser(user);

        return handler.searchByTag(tag, user);
    }

    @Override
    public ProjectData searchByType(String type, User user) throws SW360Exception {
        assertNotEmpty(type);
        assertUser(user);

        return handler.searchByType(type, user);
    }

    @Override
    public Set<Project> searchByReleaseId(String id, User user) throws TException {
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            return searchHandler.searchByReleaseId(id, user);
        }
        return handler.searchByReleaseId(id, user);
    }

    @Override
    public Set<Project> searchByReleaseIds(Set<String> ids, User user) throws TException {
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            return searchHandler.searchByReleaseIds(ids, user);
        }
        return handler.searchByReleaseId(ids, user);
    }

    @Override
    public Set<Project> searchProjectByPackageId(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return handler.searchByPackageId(id, user);
    }

    @Override
    public Set<Project> searchProjectByPackageIds(Set<String> ids, User user) throws TException {
        assertNotEmpty(ids);
        assertUser(user);
        return handler.searchByPackageIds(ids, user);
    }

    @Override
    public int getProjectCountByPackageId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getProjectCountByPackageId(id);
    }

    @Override
    public Set<Project> searchLinkingProjects(String id, User user) throws TException {
        assertId(id);
        return handler.searchLinkingProjects(id, user);
    }

    ////////////////////////////
    // CLEARING REQUEST EMAIL //
    ////////////////////////////
    @Override
    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User user,
            String projectUrl) throws TException {
        assertNotNull(clearingRequest);
        assertNotEmpty(projectUrl);
        assertUser(user);
        return handler.createClearingRequest(clearingRequest, user, projectUrl);
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    @Override
    public Project getProjectById(String id, User user) throws SW360Exception {
        assertUser(user);
        assertId(id);

        Project project = handler.getProjectById(id, user);
        handler.addSelectLogs(project, user);
        assertNotNull(project);

        return project;
    }

    @Override
    public Project getProjectByIdIgnoringVisibility(String id) throws SW360Exception {
        assertId(id);
        Project project = handler.getProjectByIdIgnoringVisibility(id);
        assertNotNull(project);
        return project;
    }

    @Override
    public List<Project> getProjectsById(List<String> id, User user) throws TException {
        assertUser(user);
        assertNotNull(id);
        return handler.getProjectsById(id, user);
    }

    @Override
    public Project getProjectByIdForEdit(String id, User user) throws SW360Exception {
        assertUser(user);
        assertId(id);

        Project project = handler.getProjectForEdit(id, user);
        assertNotNull(project);

        return project;
    }

    @Override
    public int getCountByReleaseIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getCountByReleaseIds(ids);
    }

    @Override
    public int getCountByProjectId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getCountByProjectId(id);
    }

    @Override
    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        assertNotNull(externalIds);
        assertUser(user);

        return handler.searchByExternalIds(externalIds, user);
    }

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.importBomFromAttachmentContent(user, attachmentContentId);
    }

    @Override
    public ImportBomDryRunReport dryRunImportBom(User user, String filename, ByteBuffer bomContent) throws SW360Exception {
        assertUser(user);
        assertNotNull(filename);
        assertNotNull(bomContent);
        return handler.dryRunImportBom(user, filename, bomContent);
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId, String projectId)
            throws SW360Exception {
        assertId(attachmentContentId);
        assertUser(user);
        return handler.importCycloneDxFromAttachmentContent(user, attachmentContentId, projectId);
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(User user,
            String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) throws SW360Exception {
        assertId(attachmentContentId);
        assertUser(user);
        return handler.importCycloneDxFromAttachmentContent(user, attachmentContentId, projectId,
                doNotReplacePackageAndRelease);
    }

    @Override
    public RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases,
            User user) throws SW360Exception {
        assertId(projectId);
        assertUser(user);
        return handler.exportCycloneDxSbom(projectId, bomType, includeSubProjReleases, user);
    }

    @Override
    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) throws SW360Exception {
        assertId(attachmentContentId);
        return handler.getSbomImportInfoFromAttachmentAsString(attachmentContentId);
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////

    @Override
    public AddDocumentRequestSummary addProject(Project project, User user) throws TException {
        assertNotNull(project);
        assertIdUnset(project.getId());
        assertUser(user);
        validateNoEmptyKeys(project);
        return handler.addProject(project, user);
    }

    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    @Override
    public RequestStatus updateProject(Project project, User user) throws TException {
        assertNotNull(project);
        assertId(project.getId());
        assertUser(user);
        validateNoEmptyKeys(project);
        return handler.updateProject(project, user);
    }

    /**
     * Validates that the given project's Additional Role, External Url, External
     * Ids, and Additional Data
     * do not contain empty keys. Empty values are allowed.
     * Throws TException if any empty key is found.
     */
    private void validateNoEmptyKeys(Project project) throws TException {
        // Additional Role (roles)
        if (project.isSetRoles() && project.getRoles() != null) {
            for (Map.Entry<String, java.util.Set<String>> entry : project.getRoles().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("Project roles contain empty key");
                }
            }
        }
        // External Urls
        if (project.isSetExternalUrls() && project.getExternalUrls() != null) {
            for (Map.Entry<String, String> entry : project.getExternalUrls().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("External Urls contain empty key");
                }
            }
        }
        // External Ids
        if (project.isSetExternalIds() && project.getExternalIds() != null) {
            for (Map.Entry<String, String> entry : project.getExternalIds().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("External Ids contain empty key");
                }
            }
        }
        // Additional Data
        if (project.isSetAdditionalData() && project.getAdditionalData() != null) {
            for (Map.Entry<String, String> entry : project.getAdditionalData().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("Additional Data contains empty key");
                }
            }
        }
    }

    @Override
    public RequestStatus updateProjectWithForceFlag(Project project, User user, boolean forceUpdate) throws TException {
        assertNotNull(project);
        assertId(project.getId());
        assertUser(user);

        return handler.updateProject(project, user, forceUpdate);
    }

    public RequestStatus updateProjectFromModerationRequest(Project projectAdditions, Project projectDeletions,
            User user) {
        return handler.updateProjectFromAdditionsAndDeletions(projectAdditions, projectDeletions, user);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    @Override
    public RequestStatus deleteProject(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deleteProject(id, user);
    }

    @Override
    public RequestStatus deleteProjectWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deleteProject(id, user, forceDelete);
    }

    //////////////////////
    // HELPER FUNCTIONS //
    //////////////////////

    @Override
    public List<ProjectLink> getLinkedProjectsOfProject(Project project, boolean deep, User user) throws TException {
        assertNotNull(project);
        return handler.getLinkedProjects(project, deep, user);
    }

    @Override
    public List<ProjectLink> getLinkedProjectsById(String id, boolean deep, User user) throws TException {
        assertId(id);

        Project project = getProjectById(id, user);
        return getLinkedProjectsOfProject(project, deep, user);
    }

    @Override
    public List<ProjectLink> getLinkedProjects(Map<String, ProjectProjectRelationship> relations, boolean depth,
            User user) throws TException {
        assertNotNull(relations);
        assertUser(user);

        return handler.getLinkedProjects(relations, depth, user);
    }

    @Override
    public Map<String, List<String>> getDuplicateProjects() throws TException {
        return handler.getDuplicateProjects();
    }

    @Override
    public List<Project> fillClearingStateSummary(List<Project> projects, User user) throws TException {
        assertUser(user);
        return handler.fillClearingStateSummary(projects, user);
    }

    @Override
    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user)
            throws TException {
        return handler.fillClearingStateSummaryIncludingSubprojects(projects, user);
    }

    @Override
    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(Project project, User user)
            throws TException {
        return handler.fillClearingStateSummaryIncludingSubprojectsForSingleProject(project, user);
    }

    @Override
    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user)
            throws SW360Exception {
        return handler.getReleaseClearingStatuses(projectId, user);
    }

    @Override
    public RequestStatus exportForMonitoringList() throws TException {
        RequestStatus status = RequestStatus.FAILURE;
        try {
            status = handler.exportForMonitoringList();
        } catch (TException exp) {
            log.error(exp);
            throw exp;
        }
        return status;
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, User user)
            throws SW360Exception {
        return handler.getReleaseClearingStatusesWithAccessibility(projectId, user);
    }

    @Override
    public RequestStatus removeAttachmentFromProject(String projectId, User user, String attachmentContentId)
            throws TException {
        Project projectByIdForEdit = getProjectByIdForEdit(projectId, user);

        Set<Attachment> attachments = projectByIdForEdit.getAttachments();

        Optional<Attachment> attachmentOptional = CommonUtils.getAttachmentOptional(attachmentContentId, attachments);
        if (attachmentOptional.isPresent()) {
            attachments.remove(attachmentOptional.get());
            return updateProject(projectByIdForEdit, user);
        } else {
            return RequestStatus.SUCCESS;
        }
    }

    @Override
    public boolean projectIsUsed(String projectId) throws TException {
        return handler.checkIfInUse(projectId);
    }

    @Override
    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        assertNotNull(project);
        assertUser(user);

        return handler.getCyclicLinkedProjectPath(project, user);
    }

    @Override
    public ObligationList getLinkedObligations(String obligationId, User user) throws TException {
        assertId(obligationId);
        assertUser(user);
        return handler.getLinkedObligations(obligationId, user);
    }

    @Override
    public RequestStatus addLinkedObligations(ObligationList obligation, User user) throws TException {
        assertUser(user);
        assertNotNull(obligation);
        assertIdUnset(obligation.getId());
        return handler.addLinkedObligations(obligation, user);
    }

    @Override
    public RequestStatus updateLinkedObligations(ObligationList obligation, User user) throws TException {
        assertNotNull(obligation);
        assertId(obligation.getId());
        assertId(obligation.getProjectId());
        assertUser(user);
        return handler.updateLinkedObligations(obligation, user);
    }

    public void deleteReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.deleteReleaseRelationsUsage(usedReleaseRelations);
    }

    @Override
    public void addReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.addReleaseRelationsUsage(usedReleaseRelations);

    }

    @Override
    public void updateReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.updateReleaseRelationsUsage(usedReleaseRelations);
    }

    @Override
    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) throws TException {
        assertNotNull(projectId);
        return handler.getUsedReleaseRelationsByProjectId(projectId);
    }

    @Override
    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateInformationForListView(projectId, user, false);
    }

    @Override
    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateInformationForListView(projectId, user, true);
    }

    @Override
    public Set<String> getGroups() throws TException {
        return handler.getGroups();
    }

    @Override
    public int getMyAccessibleProjectCounts(User user) throws TException {
        return handler.getMyAccessibleProjects(user);
    }

    @Override
    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        handler.sendExportSpreadsheetSuccessMail(url, recepient);
    }

    @Override
    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token)
            throws TException {
        return handler.downloadExcel(user, extendedByReleases, token);
    }

    @Override
    public ByteBuffer getReportDataStream(User user, boolean extendedByReleases, String projectId)
            throws TException {
        return handler.getReportDataStream(user, extendedByReleases, projectId);
    }

    @Override
    public String getReportInEmail(User user, boolean extendedByReleases, String projectId)
            throws TException {
        return handler.getReportInEmail(user, extendedByReleases, projectId);
    }

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, User user)
            throws TException {
        return handler.getReleaseLinksOfProjectNetWorkByTrace(trace, projectId, user);
    }

    @Override
    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateForDependencyNetworkListView(projectId, user, true);
    }

    @Override
    public List<Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions) {
        return searchHandler.search(text, subQueryRestrictions);
    }

    @Override
    public List<ProjectLink> getLinkedProjectsWithoutReleases(Map<String, ProjectProjectRelationship> relations,
            boolean depth, User user) throws TException {
        assertNotNull(relations);
        assertUser(user);

        return handler.getLinkedProjectsWithoutReleases(relations, depth, user);
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(Project project, boolean deep, User user)
            throws TException {
        assertNotNull(project);
        return handler.getLinkedProjectsWithoutReleases(project, deep, user);
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(Project project, boolean deep, User user)
            throws TException {
        assertNotNull(project);
        return handler.getLinkedProjectsWithAllReleases(project, deep, user);
    }

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath,
            User user) throws SW360Exception {
        return handler.getReleaseLinksOfProjectNetWorkByIndexPath(indexPath, projectId, user);
    }

    @Override
    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, User sw360User)
            throws SW360Exception {
        return handler.getLinkedReleasesInDependencyNetworkOfProject(projectId, sw360User);
    }
}
