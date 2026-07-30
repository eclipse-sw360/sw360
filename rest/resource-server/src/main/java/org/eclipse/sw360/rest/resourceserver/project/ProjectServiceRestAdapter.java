/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStatusDataConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationListConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectDataConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectProjectRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.projects.UsedReleaseRelationsConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.projects.ProjectClient;
import org.eclipse.sw360.datahandler.projects.ProjectClients;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Component;

/**
 * Thrift {@link ProjectService.Iface} adapter that delegates to the projects REST backend
 * ({@code /projects/api/projects}). Keeps the Thrift contract intact for existing resource-server
 * callers while removing the Thrift transport.
 */
@Component
public class ProjectServiceRestAdapter implements ProjectService.Iface {

    private ProjectClient client() {
        return ProjectClients.get();
    }

    @Override
    public List<Project> search(String text) throws TException {
        return call(() -> toThriftProjects(client().search(text)));
    }

    @Override
    public List<Project> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions, User user) throws TException {
        return call(() -> toThriftProjects(client().refineSearch(text, subQueryRestrictions, UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Project>> refineSearchPageable(String text,
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData paginationData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result = call(() -> client().refineSearchPageable(text, subQueryRestrictions, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(paginationData)));
        return toPaginatedMap(result, paginationData);
    }

    @Override
    public List<Project> refineSearchWithoutUser(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return call(() -> toThriftProjects(client().refineSearchWithoutUser(text, subQueryRestrictions)));
    }

    @Override
    public List<Project> searchByName(String name, User user) throws TException {
        return call(() -> toThriftProjects(client().searchByName(name, UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByNamePrefixPaginated(User user, String name,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result = call(() -> client().searchProjectByNamePrefixPaginated(UserConverter.fromThrift(user), name, PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchProjectByExactNamePaginated(User user, String name,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result = call(() -> client().searchProjectByExactNamePaginated(UserConverter.fromThrift(user), name, PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Map<PaginationData, List<Project>> searchAccessibleProjectByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result = call(() -> client().searchAccessibleProjectByExactValues(subQueryRestrictions, UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public ProjectData searchByGroup(String group, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(client().searchByGroup(group, UserConverter.fromThrift(user))));
    }

    @Override
    public ProjectData searchByTag(String tag, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(client().searchByTag(tag, UserConverter.fromThrift(user))));
    }

    @Override
    public ProjectData searchByType(String type, User user) throws TException {
        return call(() -> ProjectDataConverter.toThrift(client().searchByType(type, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Project> searchByReleaseId(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchByReleaseId(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Project> searchByReleaseIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchByReleaseIds(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Project> searchProjectByPackageId(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchProjectByPackageId(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Project> searchProjectByPackageIds(Set<String> ids, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchProjectByPackageIds(ids, UserConverter.fromThrift(user))));
    }

    @Override
    public int getProjectCountByPackageId(String id) throws TException {
        return call(() -> client().getProjectCountByPackageId(id));
    }

    @Override
    public Set<Project> searchLinkingProjects(String id, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchLinkingProjects(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        return call(() -> toThriftProjectSet(client().searchByExternalIds(externalIds, UserConverter.fromThrift(user))));
    }

    @Override
    public Project getProjectById(String id, User user) throws TException {
        return call(() -> ProjectConverter.toThrift(client().getProjectById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Project getProjectByIdIgnoringVisibility(String id) throws TException {
        return call(() -> ProjectConverter.toThrift(client().getProjectByIdIgnoringVisibility(id)));
    }

    @Override
    public List<Project> getProjectsById(List<String> id, User user) throws TException {
        return call(() -> toThriftProjects(client().getProjectsById(id, UserConverter.fromThrift(user))));
    }

    @Override
    public Project getProjectByIdForEdit(String id, User user) throws TException {
        return call(() -> ProjectConverter.toThrift(client().getProjectByIdForEdit(id, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        return call(() -> toThriftProjects(client().getMyProjects(UserConverter.fromThrift(user), userRoles)));
    }

    @Override
    public List<Project> getAccessibleProjectsSummary(User user) throws TException {
        return call(() -> toThriftProjects(client().getAccessibleProjectsSummary(UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Project>> getAccessibleProjectsSummaryWithPagination(User user,
            PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result = call(() -> client().getAccessibleProjectsSummaryWithPagination(UserConverter.fromThrift(user), PaginationDataConverter.fromThrift(pageData)));
        return toPaginatedMap(result, pageData);
    }

    @Override
    public Set<Project> getAccessibleProjects(User user) throws TException {
        return call(() -> toThriftProjectSet(client().getAccessibleProjects(UserConverter.fromThrift(user))));
    }

    @Override
    public int getMyAccessibleProjectCounts(User user) throws TException {
        return call(() -> client().getMyAccessibleProjectCounts(UserConverter.fromThrift(user)));
    }

    @Override
    public int getCountByReleaseIds(Set<String> ids) throws TException {
        return call(() -> client().getCountByReleaseIds(ids));
    }

    @Override
    public int getCountByProjectId(String id) throws TException {
        return call(() -> client().getCountByProjectId(id));
    }

    @Override
    public AddDocumentRequestSummary addProject(Project project, User user) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(client().addProject(ProjectConverter.fromThrift(project), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateProject(Project project, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateProject(ProjectConverter.fromThrift(project), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateProjectWithForceFlag(Project project, User user, boolean forceUpdate) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateProjectWithForceFlag(
                ProjectConverter.fromThrift(project), UserConverter.fromThrift(user), forceUpdate)));
    }

    @Override
    public RequestStatus updateProjectFromModerationRequest(Project projectAdditions, Project projectDeletions,
            User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateProjectFromModerationRequest(
                ProjectConverter.fromThrift(projectAdditions), ProjectConverter.fromThrift(projectDeletions),
                UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus deleteProject(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteProject(id, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus deleteProjectWithForceFlag(String id, User user, boolean forceDelete) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().deleteProjectWithForceFlag(id, UserConverter.fromThrift(user), forceDelete)));
    }

    @Override
    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User user,
            String projectUrl) throws TException {
        return call(() -> AddDocumentRequestSummaryConverter.toThrift(client().createClearingRequest(
                ClearingRequestConverter.fromThrift(clearingRequest), UserConverter.fromThrift(user), projectUrl)));
    }

    @Override
    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user) throws TException {
        return call(() -> toThriftReleaseClearingStatusData(client().getReleaseClearingStatuses(projectId, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, User user)
            throws TException {
        return call(() -> toThriftReleaseClearingStatusData(
                client().getReleaseClearingStatusesWithAccessibility(projectId, UserConverter.fromThrift(user))));
    }

    @Override
    public List<Project> fillClearingStateSummary(List<Project> projects, User user) throws TException {
        return call(() -> toThriftProjects(client().fillClearingStateSummary(toPojoProjects(projects), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user) throws TException {
        return call(() -> toThriftProjects(client().fillClearingStateSummaryIncludingSubprojects(toPojoProjects(projects), UserConverter.fromThrift(user))));
    }

    @Override
    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(Project project, User user) throws TException {
        return call(() -> ProjectConverter.toThrift(client().fillClearingStateSummaryIncludingSubprojectsForSingleProject(ProjectConverter.fromThrift(project), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, User user) throws TException {
        return call(() -> client().getClearingStateInformationForListView(projectId, UserConverter.fromThrift(user)));
    }

    @Override
    public List<Map<String, String>> getAccessibleClearingStateInformationForListView(String projectId, User user) throws TException {
        return call(() -> client().getAccessibleClearingStateInformationForListView(projectId, UserConverter.fromThrift(user)));
    }

    @Override
    public RequestStatus exportForMonitoringList() throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().exportForMonitoringList()));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProject(Project project, boolean deep, User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjectsOfProject(ProjectConverter.fromThrift(project), deep, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsById(String id, boolean deep, User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjectsById(id, deep, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ProjectLink> getLinkedProjects(Map<String, ProjectProjectRelationship> relations, boolean depth,
            User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjects(
                toPojoRelationships(relations), depth, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsWithoutReleases(Map<String, ProjectProjectRelationship> relations,
            boolean depth, User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjectsWithoutReleases(
                toPojoRelationships(relations), depth, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithoutReleases(Project project, boolean deep, User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjectsOfProjectWithoutReleases(ProjectConverter.fromThrift(project), deep, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ProjectLink> getLinkedProjectsOfProjectWithAllReleases(Project project, boolean deep, User user) throws TException {
        return call(() -> toThriftProjectLinks(client().getLinkedProjectsOfProjectWithAllReleases(ProjectConverter.fromThrift(project), deep, UserConverter.fromThrift(user))));
    }

    @Override
    public ObligationList getLinkedObligations(String obligationId, User user) throws TException {
        return call(() -> ObligationListConverter.toThrift(client().getLinkedObligations(obligationId, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus addLinkedObligations(ObligationList obligation, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().addLinkedObligations(
                ObligationListConverter.fromThrift(obligation), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateLinkedObligations(ObligationList obligation, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateLinkedObligations(
                ObligationListConverter.fromThrift(obligation), UserConverter.fromThrift(user))));
    }

    @Override
    public void addReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        call(() -> {
            client().addReleaseRelationsUsage(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations));
            return null;
        });
    }

    @Override
    public void updateReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        call(() -> {
            client().updateReleaseRelationsUsage(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations));
            return null;
        });
    }

    @Override
    public void deleteReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        call(() -> {
            client().deleteReleaseRelationsUsage(UsedReleaseRelationsConverter.fromThrift(usedReleaseRelations));
            return null;
        });
    }

    @Override
    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) throws TException {
        return call(() -> toThriftUsedReleaseRelations(client().getUsedReleaseRelationsByProjectId(projectId)));
    }

    @Override
    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().importBomFromAttachmentContent(UserConverter.fromThrift(user), attachmentContentId)));
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId,
            String projectId) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().importCycloneDxFromAttachmentContent(UserConverter.fromThrift(user), attachmentContentId, projectId)));
    }

    @Override
    public RequestSummary importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(User user,
            String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().importCycloneDxFromAttachmentContentWithReplacePackageAndReleaseFlag(UserConverter.fromThrift(user), attachmentContentId, projectId, doNotReplacePackageAndRelease)));
    }

    @Override
    public RequestSummary exportCycloneDxSbom(String projectId, String bomType, boolean includeSubProjReleases,
            User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(client().exportCycloneDxSbom(projectId, bomType, includeSubProjReleases, UserConverter.fromThrift(user))));
    }

    @Override
    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) throws TException {
        return call(() -> client().getSbomImportInfoFromAttachmentAsString(attachmentContentId));
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
    public ByteBuffer getReportDataStream(User user, boolean extendedByReleases, String projectId) throws TException {
        return call(() -> ByteBuffer.wrap(client().getReportDataStream(UserConverter.fromThrift(user), extendedByReleases, projectId)));
    }

    @Override
    public String getReportInEmail(User user, boolean extendedByReleases, String projectId) throws TException {
        return call(() -> client().getReportInEmail(UserConverter.fromThrift(user), extendedByReleases, projectId));
    }

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(String projectId, List<String> trace, User user) throws TException {
        return call(() -> toThriftReleaseLinks(client().getReleaseLinksOfProjectNetWorkByTrace(projectId, trace, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(String projectId, List<String> indexPath,
            User user) throws TException {
        return call(() -> toThriftReleaseLinks(client().getReleaseLinksOfProjectNetWorkByIndexPath(projectId, indexPath, UserConverter.fromThrift(user))));
    }

    @Override
    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, User sw360User) throws TException {
        return call(() -> toThriftReleaseNodes(client().getLinkedReleasesInDependencyNetworkOfProject(projectId, UserConverter.fromThrift(sw360User))));
    }

    @Override
    public List<Map<String, String>> getAccessibleDependencyNetworkForListView(String projectId, User user) throws TException {
        return call(() -> client().getAccessibleDependencyNetworkForListView(projectId, UserConverter.fromThrift(user)));
    }

    @Override
    public Map<String, List<String>> getDuplicateProjects() throws TException {
        return call(() -> client().getDuplicateProjects());
    }

    @Override
    public boolean projectIsUsed(String projectId) throws TException {
        return call(() -> client().projectIsUsed(projectId));
    }

    @Override
    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        return call(() -> client().getCyclicLinkedProjectPath(ProjectConverter.fromThrift(project), UserConverter.fromThrift(user)));
    }

    @Override
    public RequestStatus removeAttachmentFromProject(String projectId, User user, String attachmentContentId) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().removeAttachmentFromProject(projectId, UserConverter.fromThrift(user), attachmentContentId)));
    }

    @Override
    public Set<String> getGroups() throws TException {
        return call(() -> client().getGroups());
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

    private Map<PaginationData, List<Project>> toPaginatedMap(
            org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.projects.Project> result,
            PaginationData fallback) {
        Map<PaginationData, List<Project>> map = new HashMap<>();
        if (result != null) {
            PaginationData thriftPage = result.getPaginationData() != null
                    ? PaginationDataConverter.toThrift(result.getPaginationData())
                    : (fallback != null ? fallback : new PaginationData());
            map.put(thriftPage, toThriftProjects(result.getData()));
        }
        return map;
    }

    private static List<Project> toThriftProjects(
            List<org.eclipse.sw360.datahandler.services.projects.Project> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ProjectConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.projects.Project> toPojoProjects(
            List<Project> thrifts) {
        if (thrifts == null) {
            return new ArrayList<>();
        }
        return thrifts.stream().map(ProjectConverter::fromThrift).collect(Collectors.toList());
    }

    private static Set<Project> toThriftProjectSet(
            Set<org.eclipse.sw360.datahandler.services.projects.Project> pojos) {
        if (pojos == null) {
            return new HashSet<>();
        }
        return pojos.stream().map(ProjectConverter::toThrift).collect(Collectors.toSet());
    }

    private static List<ProjectLink> toThriftProjectLinks(
            List<org.eclipse.sw360.datahandler.services.projects.ProjectLink> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ProjectLinkConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseClearingStatusData> toThriftReleaseClearingStatusData(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseClearingStatusDataConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseLink> toThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseLink> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseLinkConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ReleaseNode> toThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.services.components.ReleaseNode> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ReleaseNodeConverter::toThrift).collect(Collectors.toList());
    }

    private static List<UsedReleaseRelations> toThriftUsedReleaseRelations(
            List<org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(UsedReleaseRelationsConverter::toThrift).collect(Collectors.toList());
    }

    private static Map<String, org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship>
            toPojoRelationships(Map<String, ProjectProjectRelationship> thriftMap) {
        if (thriftMap == null) {
            return new HashMap<>();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectProjectRelationshipConverter.fromThrift(e.getValue())
        ));
    }
}
