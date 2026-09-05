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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStatusDataConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationListConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter;
import org.eclipse.sw360.common.utils.converter.projects.UsedReleaseRelationsConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectSearchHandler;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.ObligationList;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.projects.ProjectData;
import org.eclipse.sw360.datahandler.services.projects.ProjectLink;
import org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.Cloudant;

/**
 * POJO façade for project services. Speaks service-api POJOs end-to-end with
 * {@link ProjectDatabaseHandler} for {@link Project}; converts remaining thrift
 * types (links, obligations, clearing requests, request statuses, release graphs)
 * at this boundary. Plain class (no {@code ProjectService.Iface}), following
 * {@code UserHandler} / {@code PackageHandler}.
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 * @author ksoranko@verifa.io
 */
public class ProjectHandler {

    private static final Logger log = LogManager.getLogger(ProjectHandler.class);
    private final ProjectDatabaseHandler handler;
    private final ProjectSearchHandler searchHandler;

    public ProjectHandler() throws IOException {
        handler = new ProjectDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE,
                DatabaseSettings.COUCH_DB_ATTACHMENTS);
        searchHandler = new ProjectSearchHandler(DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_DATABASE);
    }

    public ProjectHandler(Cloudant client, String dbName, String attchmntDbName) throws IOException {
        handler = new ProjectDatabaseHandler(client, dbName, attchmntDbName);
        searchHandler = new ProjectSearchHandler(DatabaseSettings.getConfiguredClient(), dbName);
    }

    public ProjectHandler(Cloudant client, String dbName, String changeLogsDbName, String attchmntDbName)
            throws IOException {
        handler = new ProjectDatabaseHandler(client, dbName, changeLogsDbName, attchmntDbName);
        searchHandler = new ProjectSearchHandler(client, dbName);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    public List<Project> search(String text) {
        return searchHandler.search(text);
    }

    public List<Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, User user) {
        return searchHandler.search(text, subQueryRestrictions, user);
    }

    public PaginatedResult<Project> refineSearchPageable(String text, Map<String, Set<String>> subQueryRestrictions,
            User user, PaginationData paginationData) {
        return fromPaginationMap(searchHandler.search(text, subQueryRestrictions, user, paginationData),
                paginationData);
    }

    public List<Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions) {
        return searchHandler.search(text, subQueryRestrictions);
    }

    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        assertNotNull(user);
        assertNotEmpty(user.getEmail());
        return nullToEmptyList(handler.getMyProjectsFull(user, userRoles));
    }

    public List<Project> getAccessibleProjectsSummary(User user) throws TException {
        assertUser(user);
        return nullToEmptyList(handler.getAccessibleProjectsSummary(user));
    }

    public PaginatedResult<Project> getAccessibleProjectsSummaryWithPagination(User user, PaginationData pageData)
            throws TException {
        assertUser(user);
        return fromPaginationMap(handler.getAccessibleProjectsSummary(user, pageData), pageData);
    }

    public Set<Project> getAccessibleProjects(User user) throws TException {
        assertUser(user);
        return nullToEmptySet(handler.getAccessibleProjects(user));
    }

    public List<Project> searchByName(String name, User user) throws TException {
        assertNotEmpty(name);
        assertUser(user);
        return nullToEmptyList(handler.searchByName(name, user));
    }

    public PaginatedResult<Project> searchProjectByNamePrefixPaginated(User user, String name, PaginationData pageData)
            throws TException {
        assertNotEmpty(name);
        assertUser(user);
        return fromPaginationMap(handler.searchProjectByNamePrefixPaginated(user, name, pageData), pageData);
    }

    public PaginatedResult<Project> searchProjectByExactNamePaginated(User user, String name, PaginationData pageData)
            throws TException {
        assertNotEmpty(name);
        assertUser(user);
        return fromPaginationMap(handler.searchProjectByExactNamePaginated(user, name, pageData), pageData);
    }

    public PaginatedResult<Project> searchAccessibleProjectByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        assertUser(user);
        return fromPaginationMap(
                handler.searchAccessibleProjectByExactValues(subQueryRestrictions, user, pageData), pageData);
    }

    public ProjectData searchByGroup(String group, User user) throws SW360Exception {
        assertNotEmpty(group);
        assertUser(user);
        return handler.searchByGroup(group, user);
    }

    public ProjectData searchByTag(String tag, User user) throws SW360Exception {
        assertNotEmpty(tag);
        assertUser(user);
        return handler.searchByTag(tag, user);
    }

    public ProjectData searchByType(String type, User user) throws SW360Exception {
        assertNotEmpty(type);
        assertUser(user);
        return handler.searchByType(type, user);
    }

    public Set<Project> searchByReleaseId(String id, User user) throws TException {
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            return searchHandler.searchByReleaseId(id, user);
        }
        return nullToEmptySet(handler.searchByReleaseId(id, user));
    }

    public Set<Project> searchByReleaseIds(Set<String> ids, User user) throws TException {
        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            return searchHandler.searchByReleaseIds(ids, user);
        }
        return nullToEmptySet(handler.searchByReleaseId(ids, user));
    }

    public Set<Project> searchProjectByPackageId(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return nullToEmptySet(handler.searchByPackageId(id, user));
    }

    public Set<Project> searchProjectByPackageIds(Set<String> ids, User user) throws TException {
        assertNotEmpty(ids);
        assertUser(user);
        return nullToEmptySet(handler.searchByPackageIds(ids, user));
    }

    public int getProjectCountByPackageId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getProjectCountByPackageId(id);
    }

    public Set<Project> searchLinkingProjects(String id, User user) throws TException {
        assertId(id);
        return nullToEmptySet(handler.searchLinkingProjects(id, user));
    }

    ////////////////////////////
    // CLEARING REQUEST EMAIL //
    ////////////////////////////
    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User user, String projectUrl)
            throws TException {
        assertNotNull(clearingRequest);
        assertNotEmpty(projectUrl);
        assertUser(user);
        return AddDocumentRequestSummaryConverter.fromThrift(handler.createClearingRequest(
                ClearingRequestConverter.toThrift(clearingRequest), user, projectUrl));
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    public Project getProjectById(String id, User user) throws SW360Exception {
        assertUser(user);
        assertId(id);

        Project project = handler.getProjectById(id, user);
        handler.addSelectLogs(project, user);
        assertNotNull(project);
        return project;
    }

    public Project getProjectByIdIgnoringVisibility(String id) throws SW360Exception {
        assertId(id);
        Project project = handler.getProjectByIdIgnoringVisibility(id);
        assertNotNull(project);
        return project;
    }

    public List<Project> getProjectsById(List<String> id, User user) throws TException {
        assertUser(user);
        assertNotNull(id);
        return nullToEmptyList(handler.getProjectsById(id, user));
    }

    public Project getProjectByIdForEdit(String id, User user) throws SW360Exception {
        assertUser(user);
        assertId(id);

        Project project = handler.getProjectForEdit(id, user);
        assertNotNull(project);
        return project;
    }

    public int getCountByReleaseIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getCountByReleaseIds(ids);
    }

    public int getCountByProjectId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getCountByProjectId(id);
    }

    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        assertNotNull(externalIds);
        assertUser(user);
        return nullToEmptySet(handler.searchByExternalIds(externalIds, user));
    }

    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return RequestSummaryConverter.fromThrift(handler.importBomFromAttachmentContent(user, attachmentContentId));
    }

    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId, String projectId)
            throws SW360Exception {
        assertId(attachmentContentId);
        assertUser(user);
        return RequestSummaryConverter.fromThrift(
                handler.importCycloneDxFromAttachmentContent(user, attachmentContentId, projectId));
    }

    public RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(User user,
            String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) throws SW360Exception {
        assertId(attachmentContentId);
        assertUser(user);
        return RequestSummaryConverter.fromThrift(handler.importCycloneDxFromAttachmentContent(
                user, attachmentContentId, projectId, doNotReplacePackageAndRelease));
    }

    public RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases,
            User user) throws SW360Exception {
        assertId(projectId);
        assertUser(user);
        return RequestSummaryConverter.fromThrift(
                handler.exportCycloneDxSbom(projectId, bomType, includeSubProjReleases, user));
    }

    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) throws SW360Exception {
        assertId(attachmentContentId);
        return handler.getSbomImportInfoFromAttachmentAsString(attachmentContentId);
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////

    public AddDocumentRequestSummary addProject(Project project, User user) throws TException {
        assertNotNull(project);
        assertIdUnset(project.getId());
        assertUser(user);
        validateNoEmptyKeys(project);
        return AddDocumentRequestSummaryConverter.fromThrift(handler.addProject(project, user));
    }

    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    public RequestStatus updateProject(Project project, User user) throws TException {
        assertNotNull(project);
        assertId(project.getId());
        assertUser(user);
        validateNoEmptyKeys(project);
        return RequestStatusConverter.fromThrift(handler.updateProject(project, user));
    }

    /**
     * Validates that the given project's Additional Role, External Url, External
     * Ids, and Additional Data do not contain empty keys. Empty values are allowed.
     * Throws TException if any empty key is found.
     */
    private void validateNoEmptyKeys(Project project) throws TException {
        if (project.getRoles() != null) {
            for (Map.Entry<String, Set<String>> entry : project.getRoles().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("Project roles contain empty key");
                }
            }
        }
        if (project.getExternalUrls() != null) {
            for (Map.Entry<String, String> entry : project.getExternalUrls().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("External Urls contain empty key");
                }
            }
        }
        if (project.getExternalIds() != null) {
            for (Map.Entry<String, String> entry : project.getExternalIds().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("External Ids contain empty key");
                }
            }
        }
        if (project.getAdditionalData() != null) {
            for (Map.Entry<String, String> entry : project.getAdditionalData().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    throw new TException("Additional Data contains empty key");
                }
            }
        }
    }

    public RequestStatus updateProjectWithForceFlag(Project project, User user, boolean forceUpdate) throws TException {
        assertNotNull(project);
        assertId(project.getId());
        assertUser(user);
        return RequestStatusConverter.fromThrift(handler.updateProject(project, user, forceUpdate));
    }

    public RequestStatus updateProjectFromModerationRequest(Project projectAdditions, Project projectDeletions,
            User user) {
        return RequestStatusConverter.fromThrift(
                handler.updateProjectFromAdditionsAndDeletions(projectAdditions, projectDeletions, user));
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    public RequestStatus deleteProject(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return RequestStatusConverter.fromThrift(handler.deleteProject(id, user));
    }

    public RequestStatus deleteProjectWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        assertId(id);
        assertUser(user);
        return RequestStatusConverter.fromThrift(handler.deleteProject(id, user, forceDelete));
    }

    //////////////////////
    // HELPER FUNCTIONS //
    //////////////////////

    public List<ProjectLink> getLinkedProjectsOfProject(Project project, boolean deep, User user) throws TException {
        assertNotNull(project);
        return fromThriftProjectLinks(handler.getLinkedProjects(project, deep, user));
    }

    public List<ProjectLink> getLinkedProjectsById(String id, boolean deep, User user) throws TException {
        assertId(id);
        Project project = getProjectById(id, user);
        return getLinkedProjectsOfProject(project, deep, user);
    }

    public List<ProjectLink> getLinkedProjects(Map<String, ProjectProjectRelationship> relations, boolean depth,
            User user) throws TException {
        assertNotNull(relations);
        assertUser(user);
        return fromThriftProjectLinks(handler.getLinkedProjects(relations, depth, user));
    }

    public Map<String, List<String>> getDuplicateProjects() throws TException {
        return handler.getDuplicateProjects();
    }

    public List<Project> fillClearingStateSummary(List<Project> projects, User user) throws TException {
        assertUser(user);
        return nullToEmptyList(handler.fillClearingStateSummary(projects, user));
    }

    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user)
            throws TException {
        return nullToEmptyList(handler.fillClearingStateSummaryIncludingSubprojects(projects, user));
    }

    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(Project project, User user)
            throws TException {
        return handler.fillClearingStateSummaryIncludingSubprojectsForSingleProject(project, user);
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user)
            throws SW360Exception {
        return handler.getReleaseClearingStatuses(projectId, user);
    }

    public RequestStatus exportForMonitoringList() throws TException {
        try {
            return RequestStatusConverter.fromThrift(handler.exportForMonitoringList());
        } catch (TException exp) {
            log.error(exp);
            throw exp;
        }
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, User user)
            throws SW360Exception {
        return handler.getReleaseClearingStatusesWithAccessibility(projectId, user);
    }

    public RequestStatus removeAttachmentFromProject(String projectId, User user, String attachmentContentId)
            throws TException {
        assertId(projectId);
        assertUser(user);
        Project project = handler.getProjectForEdit(projectId, user);
        assertNotNull(project);
        Set<Attachment> attachments = project.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return RequestStatus.SUCCESS;
        }
        Optional<Attachment> attachmentOptional = attachments.stream()
                .filter(a -> attachmentContentId.equals(a.getAttachmentContentId()))
                .findFirst();
        if (attachmentOptional.isPresent()) {
            attachments.remove(attachmentOptional.get());
            project.setAttachments(attachments);
            return RequestStatusConverter.fromThrift(handler.updateProject(project, user));
        }
        return RequestStatus.SUCCESS;
    }

    public boolean projectIsUsed(String projectId) throws TException {
        return handler.checkIfInUse(projectId);
    }

    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        assertNotNull(project);
        assertUser(user);
        return handler.getCyclicLinkedProjectPath(project, user);
    }

    public ObligationList getLinkedObligations(String obligationId, User user) throws TException {
        assertId(obligationId);
        assertUser(user);
        return ObligationListConverter.fromThrift(handler.getLinkedObligations(obligationId, user));
    }

    public RequestStatus addLinkedObligations(ObligationList obligation, User user) throws TException {
        assertUser(user);
        assertNotNull(obligation);
        assertIdUnset(obligation.getId());
        return RequestStatusConverter.fromThrift(
                handler.addLinkedObligations(ObligationListConverter.toThrift(obligation), user));
    }

    public RequestStatus updateLinkedObligations(ObligationList obligation, User user) throws TException {
        assertNotNull(obligation);
        assertId(obligation.getId());
        assertId(obligation.getProjectId());
        assertUser(user);
        return RequestStatusConverter.fromThrift(
                handler.updateLinkedObligations(ObligationListConverter.toThrift(obligation), user));
    }

    public void deleteReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.deleteReleaseRelationsUsage(UsedReleaseRelationsConverter.toThrift(usedReleaseRelations));
    }

    public void addReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.addReleaseRelationsUsage(UsedReleaseRelationsConverter.toThrift(usedReleaseRelations));
    }

    public void updateReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        assertNotNull(usedReleaseRelations);
        handler.updateReleaseRelationsUsage(UsedReleaseRelationsConverter.toThrift(usedReleaseRelations));
    }

    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) throws TException {
        assertNotNull(projectId);
        List<org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations> thriftList =
                handler.getUsedReleaseRelationsByProjectId(projectId);
        if (thriftList == null) {
            return new ArrayList<>();
        }
        return thriftList.stream().map(UsedReleaseRelationsConverter::fromThrift).collect(Collectors.toList());
    }

    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateInformationForListView(projectId, user, false);
    }

    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateInformationForListView(projectId, user, true);
    }

    public Set<String> getGroups() throws TException {
        return handler.getGroups();
    }

    public int getMyAccessibleProjectCounts(User user) throws TException {
        return handler.getMyAccessibleProjects(user);
    }

    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        handler.sendExportSpreadsheetSuccessMail(url, recepient);
    }

    public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws TException {
        return handler.downloadExcel(user, extendedByReleases, token);
    }

    public ByteBuffer getReportDataStream(User user, boolean extendedByReleases, String projectId) throws TException {
        return handler.getReportDataStream(user, extendedByReleases, projectId);
    }

    public String getReportInEmail(User user, boolean extendedByReleases, String projectId) throws TException {
        return handler.getReportInEmail(user, extendedByReleases, projectId);
    }

    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, User user)
            throws TException {
        return fromThriftReleaseLinks(handler.getReleaseLinksOfProjectNetWorkByTrace(trace, projectId, user));
    }

    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, User user)
            throws SW360Exception {
        assertNotNull(projectId);
        return handler.getClearingStateForDependencyNetworkListView(projectId, user, true);
    }

    public List<ProjectLink> getLinkedProjectsWithoutReleases(Map<String, ProjectProjectRelationship> relations,
            boolean depth, User user) throws TException {
        assertNotNull(relations);
        assertUser(user);
        return fromThriftProjectLinks(handler.getLinkedProjectsWithoutReleases(relations, depth, user));
    }

    public List<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(Project project, boolean deep, User user)
            throws TException {
        assertNotNull(project);
        return fromThriftProjectLinks(handler.getLinkedProjectsWithoutReleases(project, deep, user));
    }

    public List<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(Project project, boolean deep, User user)
            throws TException {
        assertNotNull(project);
        return fromThriftProjectLinks(handler.getLinkedProjectsWithAllReleases(project, deep, user));
    }

    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath,
            User user) throws SW360Exception {
        return fromThriftReleaseLinks(handler.getReleaseLinksOfProjectNetWorkByIndexPath(indexPath, projectId, user));
    }

    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, User sw360User)
            throws SW360Exception {
        List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> thriftList =
                handler.getLinkedReleasesInDependencyNetworkOfProject(projectId, sw360User);
        if (thriftList == null) {
            return new ArrayList<>();
        }
        return thriftList.stream().map(ReleaseNodeConverter::fromThrift).collect(Collectors.toList());
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private static PaginatedResult<Project> fromPaginationMap(
            Map<PaginationData, List<Project>> pojoMap, PaginationData originalPageData) {
        if (pojoMap == null || pojoMap.isEmpty()) {
            return new PaginatedResult<>(originalPageData, Collections.emptyList());
        }
        Map.Entry<PaginationData, List<Project>> entry = pojoMap.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(),
                entry.getValue() == null ? Collections.emptyList() : entry.getValue());
    }

    private static List<Project> nullToEmptyList(List<Project> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private static Set<Project> nullToEmptySet(Set<Project> set) {
        return set == null ? new HashSet<>() : set;
    }

    private static List<ProjectLink> fromThriftProjectLinks(
            List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink> thriftList) {
        if (thriftList == null || thriftList.isEmpty()) {
            return new ArrayList<>();
        }
        return thriftList.stream().map(ProjectLinkConverter::fromThrift).collect(Collectors.toList());
    }

    private static List<ReleaseClearingStatusData> fromThriftReleaseClearingStatusData(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData> thriftList) {
        if (thriftList == null || thriftList.isEmpty()) {
            return new ArrayList<>();
        }
        return thriftList.stream().map(ReleaseClearingStatusDataConverter::fromThrift).collect(Collectors.toList());
    }

    private static List<ReleaseLink> fromThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> thriftList) {
        if (thriftList == null || thriftList.isEmpty()) {
            return new ArrayList<>();
        }
        return thriftList.stream().map(ReleaseLinkConverter::fromThrift).collect(Collectors.toList());
    }

    ProjectDatabaseHandler databaseHandler() {
        return handler;
    }
}
