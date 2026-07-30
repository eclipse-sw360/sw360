/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.projects;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the projects backend service.
 *
 * Callers use this instead of the former Thrift {@code ProjectService.Iface}.
 * Types are service-api POJOs. See {@link ProjectServiceRestClient} and {@link ProjectClients}.
 */
public interface ProjectClient {

    List<org.eclipse.sw360.datahandler.services.projects.Project> search(String text);

    List<org.eclipse.sw360.datahandler.services.projects.Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> refineSearchPageable(String text, Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData paginationData);

    List<org.eclipse.sw360.datahandler.services.projects.Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions);

    List<org.eclipse.sw360.datahandler.services.projects.Project> searchByName(String name, org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByNamePrefixPaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByExactNamePaginated(org.eclipse.sw360.datahandler.services.users.User user, String name, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> searchAccessibleProjectByExactValues(Map<String, Set<String>> subQueryRestrictions, org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    org.eclipse.sw360.datahandler.services.projects.ProjectData searchByGroup(String group, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.ProjectData searchByTag(String tag, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.ProjectData searchByType(String type, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByReleaseId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByReleaseIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByPackageId(String id, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchProjectByPackageIds(Set<String> ids, org.eclipse.sw360.datahandler.services.users.User user);

    int getProjectCountByPackageId(String id);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchLinkingProjects(String id, org.eclipse.sw360.datahandler.services.users.User user);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> searchByExternalIds(Map<String, Set<String>> externalIds, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.Project getProjectById(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.Project getProjectByIdIgnoringVisibility(String id);

    List<org.eclipse.sw360.datahandler.services.projects.Project> getProjectsById(List<String> id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.Project getProjectByIdForEdit(String id, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.Project> getMyProjects(org.eclipse.sw360.datahandler.services.users.User user, Map<String, Boolean> userRoles);

    List<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjectsSummary(org.eclipse.sw360.datahandler.services.users.User user);

    PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjectsSummaryWithPagination(org.eclipse.sw360.datahandler.services.users.User user, org.eclipse.sw360.datahandler.services.common.PaginationData pageData);

    Set<org.eclipse.sw360.datahandler.services.projects.Project> getAccessibleProjects(org.eclipse.sw360.datahandler.services.users.User user);

    int getMyAccessibleProjectCounts(org.eclipse.sw360.datahandler.services.users.User user);

    int getCountByReleaseIds(Set<String> ids);

    int getCountByProjectId(String id);

    org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary addProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateProjectWithForceFlag(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user, boolean forceUpdate);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateProjectFromModerationRequest(org.eclipse.sw360.datahandler.services.projects.Project projectAdditions, org.eclipse.sw360.datahandler.services.projects.Project projectDeletions, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteProject(String id, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus deleteProjectWithForceFlag(String id, org.eclipse.sw360.datahandler.services.users.User user, boolean forceDelete);

    org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary createClearingRequest(org.eclipse.sw360.datahandler.services.projects.ClearingRequest clearingRequest, org.eclipse.sw360.datahandler.services.users.User user, String projectUrl);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.Project> fillClearingStateSummary(List<org.eclipse.sw360.datahandler.services.projects.Project> projects, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.Project> fillClearingStateSummaryIncludingSubprojects(List<org.eclipse.sw360.datahandler.services.projects.Project> projects, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user);

    List<Map<String, String>> getClearingStateInformationForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user);

    List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus exportForMonitoringList();

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProject(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsById(String id, boolean deep, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjects(Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship> relations, boolean depth, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsWithoutReleases(Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship> relations, boolean depth, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProjectWithoutReleases(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> getLinkedProjectsOfProjectWithAllReleases(org.eclipse.sw360.datahandler.services.projects.Project project, boolean deep, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.projects.ObligationList getLinkedObligations(String obligationId, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus addLinkedObligations(org.eclipse.sw360.datahandler.services.projects.ObligationList obligation, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus updateLinkedObligations(org.eclipse.sw360.datahandler.services.projects.ObligationList obligation, org.eclipse.sw360.datahandler.services.users.User user);

    void addReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations);

    void updateReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations);

    void deleteReleaseRelationsUsage(org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations usedReleaseRelations);

    List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId);

    org.eclipse.sw360.datahandler.services.common.RequestSummary importBomFromAttachmentContent(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId);

    org.eclipse.sw360.datahandler.services.common.RequestSummary importCycloneDxFromAttachmentContent(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId, String projectId);

    org.eclipse.sw360.datahandler.services.common.RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease);

    org.eclipse.sw360.datahandler.services.common.RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases, org.eclipse.sw360.datahandler.services.users.User user);

    String getSbomImportInfoFromAttachmentAsString(String attachmentContentId);

    void sendExportSpreadsheetSuccessMail(String url, String recepient);

    byte[] downloadExcel(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String token);

    byte[] getReportDataStream(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String projectId);

    String getReportInEmail(org.eclipse.sw360.datahandler.services.users.User user, boolean extendedByReleases, String projectId);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath, org.eclipse.sw360.datahandler.services.users.User user);

    List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, org.eclipse.sw360.datahandler.services.users.User sw360User);

    List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, org.eclipse.sw360.datahandler.services.users.User user);

    Map<String, List<String>> getDuplicateProjects();

    boolean projectIsUsed(String projectId);

    String getCyclicLinkedProjectPath(org.eclipse.sw360.datahandler.services.projects.Project project, org.eclipse.sw360.datahandler.services.users.User user);

    org.eclipse.sw360.datahandler.services.common.RequestStatus removeAttachmentFromProject(String projectId, org.eclipse.sw360.datahandler.services.users.User user, String attachmentContentId);

    Set<String> getGroups();
}
