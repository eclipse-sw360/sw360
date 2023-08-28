/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.getSortedMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService implements AwareOfRestServices<Project> {

    private static final Logger log = LogManager.getLogger(Sw360ProjectService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private RestControllerHelper rch;

    public static final ExecutorService releaseExecutor = Executors.newFixedThreadPool(10);

    public Set<Project> getProjectsForUser(User sw360User, Pageable pageable) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        PaginationData pageData = new PaginationData()
                .setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize())
                .setSortColumnNumber(0);
        Map<PaginationData, List<Project>> pageDtToProjects = sw360ProjectClient.getAccessibleProjectsSummaryWithPagination(sw360User, pageData);
        return new HashSet<>(pageDtToProjects.entrySet().iterator().next().getValue());
    }

    public Project getProjectForUserById(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        try {
            Project project = sw360ProjectClient.getProjectById(projectId, sw360User);
            Map<String, String> sortedAdditionalData = getSortedMap(project.getAdditionalData(), true);
            project.setAdditionalData(sortedAdditionalData);
            return project;
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested Project Not Found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "Project or its Linked Projects are restricted and / or not accessible");
            } else {
                throw sw360Exp;
            }
        }
    }

    public Set<Project> searchLinkingProjects(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchLinkingProjects(projectId, sw360User);
    }

    public Set<Project> getProjectsByReleaseIds(Set<String> releaseids, User sw360User)
            throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByReleaseIds(releaseids, sw360User);
    }

    public Set<Project> getProjectsByRelease(String releaseid, User sw360User)
            throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByReleaseId(releaseid, sw360User);
    }

    public Project createProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        rch.checkForCyclicOrInvalidDependencies(sw360ProjectClient, project, sw360User);
        AddDocumentRequestSummary documentRequestSummary = sw360ProjectClient.addProject(project, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            project.setId(documentRequestSummary.getId());
            project.setCreatedBy(sw360User.getEmail());
            Map<String, String> sortedAdditionalData = getSortedMap(project.getAdditionalData(), true);
            project.setAdditionalData(sortedAdditionalData);
            return project;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 project with name '" + project.getName() + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Project name field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public RequestStatus updateProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        String cyclicLinkedProjectPath = null;
        rch.checkForCyclicOrInvalidDependencies(sw360ProjectClient, project, sw360User);

        // TODO: Move this logic to backend
        if (project.getReleaseIdToUsage() != null) {
            for (String releaseId : project.getReleaseIdToUsage().keySet()) {
                if (isNullEmptyOrWhitespace(releaseId)) {
                    throw new HttpMessageNotReadableException("Release Id can't be empty");
                }
            }
        }

        if (project.getVendor() != null && project.getVendorId() == null) {
            project.setVendorId(project.getVendor().getId());
        }

        RequestStatus requestStatus;
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            requestStatus = sw360ProjectClient.updateProjectWithForceFlag(project, sw360User, true);
        } else {
            requestStatus = sw360ProjectClient.updateProject(project, sw360User);
        }
        if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Project name field cannot be empty or contain only whitespace character");
        }

        if (requestStatus == RequestStatus.CLOSED_UPDATE_NOT_ALLOWED) {
            throw new RuntimeException("User cannot modify a closed project");
        } if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (requestStatus != RequestStatus.SENT_TO_MODERATOR && requestStatus != RequestStatus.SUCCESS) {
            throw new RuntimeException("sw360 project with name '" + project.getName() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deleteProject(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            return sw360ProjectClient.deleteProjectWithForceFlag(projectId, sw360User, true);
        } else {
            return sw360ProjectClient.deleteProject(projectId, sw360User);
        }
    }

    public void deleteAllProjects(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        List<Project> projects = sw360ProjectClient.getAccessibleProjectsSummary(sw360User);
        for (Project project : projects) {
            if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
                sw360ProjectClient.deleteProjectWithForceFlag(project.getId(), sw360User, true);
            } else {
                sw360ProjectClient.deleteProject(project.getId(), sw360User);
            }
        }
    }

    public List<Project> searchProjectByName(String name, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByName(name, sw360User);
    }

    public List<Project> searchProjectByGroup(String group, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByGroup(group, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public List<Project> searchProjectByTag(String tag, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByTag(tag, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public List<Project> searchProjectByType(String type, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByType(type, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public Set<String> getReleaseIds(String projectId, User sw360User, boolean transitive) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        if (transitive) {
            List<ReleaseClearingStatusData> releaseClearingStatusData = sw360ProjectClient.getReleaseClearingStatuses(projectId, sw360User);
            return releaseClearingStatusData.stream().map(r -> r.release.getId()).collect(Collectors.toSet());
        } else {
            final Project project = getProjectForUserById(projectId, sw360User);
            if (project.getReleaseIdToUsage() == null) {
                return new HashSet<String>();
            }
            return project.getReleaseIdToUsage().keySet();
        }
    }

    public void addEmbeddedLinkedProject(Project sw360Project, User sw360User, HalResource<Project> projectResource, Set<String> projectIdsInBranch) throws TException {
        projectIdsInBranch.add(sw360Project.getId());
        Map<String, ProjectProjectRelationship> linkedProjects = sw360Project.getLinkedProjects();
		List<String> keys = new ArrayList<>(linkedProjects.keySet());
		System.out.println("keys " + keys.size());
        if (keys != null) {
        	keys.forEach(linkedProjectId -> wrapTException(() -> {
                if (projectIdsInBranch.contains(linkedProjectId)) {
                    return;
                }
                Project linkedProject = getProjectForUserById(linkedProjectId, sw360User);
                System.out.println("project " + linkedProject);
                Project embeddedLinkedProject = rch.convertToEmbeddedLinkedProject(linkedProject);
                HalResource<Project> halLinkedProject = new HalResource<>(embeddedLinkedProject);
                Link projectLink = linkTo(ProjectController.class)
                        .slash("api/projects/" + embeddedLinkedProject.getId()).withSelfRel();
                halLinkedProject.add(projectLink);
                addEmbeddedLinkedProject(linkedProject, sw360User, halLinkedProject,
                        projectIdsInBranch);
                projectResource.addEmbeddedResource("sw360:linkedProjects", halLinkedProject);
            }));
        }
        projectIdsInBranch.remove(sw360Project.getId());
    }

    public void addEmbeddedlinkedRelease(Release sw360Release, User sw360User, HalResource<Release> releaseResource,
            Sw360ReleaseService releaseService, Set<String> releaseIdsInBranch) throws TException {
        releaseIdsInBranch.add(sw360Release.getId());
        Map<String, ReleaseRelationship> releaseIdToRelationship = sw360Release.getReleaseIdToRelationship();
        if (releaseIdToRelationship != null) {
            releaseIdToRelationship.keySet().forEach(linkedReleaseId -> wrapTException(() -> {
                if (releaseIdsInBranch.contains(linkedReleaseId)) {
                    return;
                }
                Release linkedRelease = releaseService.getReleaseForUserById(linkedReleaseId, sw360User);
                Release embeddedLinkedRelease = rch.convertToEmbeddedRelease(linkedRelease);
                HalResource<Release> halLinkedRelease = new HalResource<>(embeddedLinkedRelease);
                Link releaseLink = linkTo(ReleaseController.class)
                        .slash("api/releases/" + embeddedLinkedRelease.getId()).withSelfRel();
                halLinkedRelease.add(releaseLink);
                addEmbeddedlinkedRelease(linkedRelease, sw360User, halLinkedRelease, releaseService,
                        releaseIdsInBranch);
                releaseResource.addEmbeddedResource("sw360:releases", halLinkedRelease);
            }));
        }
        releaseIdsInBranch.remove(sw360Release.getId());
    }

    @Override
    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByExternalIds(externalIds, user);
    }

    @Override
    public Project convertToEmbeddedWithExternalIds(Project sw360Object) {
        return rch.convertToEmbeddedProject(sw360Object).setExternalIds(sw360Object.getExternalIds());
    }

    public ProjectService.Iface getThriftProjectClient() throws TTransportException {
        ProjectService.Iface projectClient = new ThriftClients().makeProjectClient();
        return projectClient;
    }

    public Function<ProjectLink, ProjectLink> filterAndSortAttachments(Collection<AttachmentType> attachmentTypes) {
        Predicate<Attachment> filter = att -> attachmentTypes.contains(att.getAttachmentType());
        return createProjectLinkMapper(rl -> rl.setAttachments(nullToEmptyList(rl.getAttachments())
                .stream()
                .filter(filter)
                .sorted(Comparator
                        .comparing((Attachment a) -> nullToEmpty(a.getCreatedTeam()))
                        .thenComparing(Comparator.comparing((Attachment a) -> nullToEmpty(a.getCreatedOn())).reversed()))
                .collect(Collectors.toList())));
    }

    public Function<ProjectLink, ProjectLink> createProjectLinkMapper(Function<ReleaseLink, ReleaseLink> releaseLinkMapper){
        return (projectLink) -> {
            List<ReleaseLink> mappedReleaseLinks = nullToEmptyList(projectLink
                    .getLinkedReleases())
                    .stream()
                    .map(releaseLinkMapper)
                    .collect(Collectors.toList());
            projectLink.setLinkedReleases(mappedReleaseLinks);
            return projectLink;
        };
    }

    protected List<ProjectLink> createLinkedProjects(Project project,
            Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep, User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils
                .flattenProjectLinkTree(SW360Utils.getLinkedProjects(project, deep, new ThriftClients(), log, user));
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    public Set<Release> getReleasesFromProjectIds(List<String> projectIds, boolean transitive, final User sw360User,
                                                  Sw360ReleaseService releaseService) {
        final List<Callable<List<Release>>> callableTasksToGetReleases = new ArrayList<Callable<List<Release>>>();

        projectIds.stream().forEach(id -> {
            Callable<List<Release>> getReleasesByProjectId = () -> {
                final Set<String> releaseIds = getReleaseIds(id, sw360User, transitive);

                List<Release> releases = releaseIds.stream().map(relId -> wrapTException(() -> {
                    final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                    return sw360Release;
                })).collect(Collectors.toList());
                return releases;
            };
            callableTasksToGetReleases.add(getReleasesByProjectId);
        });

        List<Future<List<Release>>> releasesFuture = new ArrayList<Future<List<Release>>>();
        try {
            releasesFuture = releaseExecutor.invokeAll(callableTasksToGetReleases);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error getting releases: " + e.getMessage());
        }

        List<List<Release>> listOfreleases = releasesFuture.stream().map(fut -> {
            List<Release> rels = new ArrayList<Release>();
            try {
                rels = fut.get();
            } catch (InterruptedException | ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ResourceNotFoundException) {
                    throw (ResourceNotFoundException) cause;
                }

                if (cause instanceof AccessDeniedException) {
                    throw (AccessDeniedException) cause;
                }
                throw new RuntimeException("Error getting releases: " + e.getMessage());
            }
            return rels;
        }).collect(Collectors.toList());

        final Set<Release> relList = new HashSet<Release>();
        listOfreleases.stream().forEach(listOfRel -> {
            for(Release rel : listOfRel) {
                relList.add(rel);
            }
        });
        return relList;
    }

    public ProjectReleaseRelationship updateProjectReleaseRelationship(
            Map<String, ProjectReleaseRelationship> releaseIdToUsage,
            ProjectReleaseRelationship requestBodyProjectReleaseRelationship, String releaseId) {
        if (!CommonUtils.isNullOrEmptyMap(releaseIdToUsage)) {
            Optional<Entry<String, ProjectReleaseRelationship>> actualProjectReleaseRelationshipEntry = releaseIdToUsage
                    .entrySet().stream().filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())
                            && entry.getKey().equals(releaseId))
                    .findFirst();
            if (actualProjectReleaseRelationshipEntry.isPresent()) {
                ProjectReleaseRelationship actualProjectReleaseRelationship = actualProjectReleaseRelationshipEntry
                        .get().getValue();
                rch.updateProjectReleaseRelationship(actualProjectReleaseRelationship,
                        requestBodyProjectReleaseRelationship);
                return actualProjectReleaseRelationship;
            }
        }
        throw new ResourceNotFoundException("Requested Release Not Found");
    }

    @PreDestroy
    public void shutDownThreadpool() {
        releaseExecutor.shutdown();
        try {
            if (!releaseExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                releaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            releaseExecutor.shutdownNow();
        }
    }

    public List<Project> refineSearch(Map<String, Set<String>> filterMap, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.refineSearch(null, filterMap, sw360User);
    }

    public void copyLinkedObligationsForClonedProject(Project createDuplicateProject, Project sw360Project, User user)
            throws TException {
        SW360Utils.copyLinkedObligationsForClonedProject(createDuplicateProject, sw360Project, getThriftProjectClient(),
                user);
    }

    private List<Project> getAllRequiredProjects(ProjectData projectData, User sw360User) throws TException {
        List<Project> listOfProjects = projectData.getFirst250Projects();
        List<String> projectIdsOfRemainingProject = projectData.getProjectIdsOfRemainingProject();
        if (CommonUtils.isNotEmpty(projectIdsOfRemainingProject)) {
            for (String id : projectIdsOfRemainingProject) {
                Project projectForUserById = getProjectForUserById(id, sw360User);
                listOfProjects.add(projectForUserById);
            }

        }
        return listOfProjects;
    }

    /**
     * From list of projects, filter projects based on their clearing state.
     * @param projects      List of projects to filter
     * @param clearingState Map of clearing states to filter projects for
     * @return List of filtered projects.
     */
    public List<Project> getWithFilledClearingStatus(List<Project> projects, Map<String, Boolean> clearingState) {
        if (!CommonUtils.isNullOrEmptyMap(clearingState)) {
            Boolean open = clearingState.getOrDefault(ProjectClearingState.OPEN.toString(), true);
            Boolean closed = clearingState.getOrDefault(ProjectClearingState.CLOSED.toString(), true);
            Boolean inProgress = clearingState.getOrDefault(ProjectClearingState.IN_PROGRESS.toString(), true);

            projects = projects.stream().filter(project -> {
                if (open != null && open && ProjectClearingState.OPEN.equals(project.getClearingState())) {
                    return true;
                } else if (closed != null && closed && ProjectClearingState.CLOSED.equals(project.getClearingState())) {
                    return true;
                } else if (inProgress != null && inProgress
                        && ProjectClearingState.IN_PROGRESS.equals(project.getClearingState())) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList());
        }
        return projects;
    }

    /**
     * Get my projects from the thrift client.
     * @param user      User to get projects for
     * @param userRoles User roles to filter projects
     * @return List of projects
     * @throws TException
     */
    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getMyProjects(user, userRoles);
    }

    /**
     * Get count of projects accessible by given user.
     * @param sw360User User to get the count for.
     * @return Total count of projects accessible by user.
     * @throws TException
     */
    public int getMyAccessibleProjectCounts(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getMyAccessibleProjectCounts(sw360User);
    }

    /**
     * Import SPDX SBOM using the method on the thrift client.
     * @param user                User uploading the SBOM
     * @param attachmentContentId Id of the attachment uploaded
     * @return RequestSummary
     * @throws TException
     */
    public RequestSummary importSPDX(User user, String attachmentContentId) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.importBomFromAttachmentContent(user, attachmentContentId);
    }

    /**
     * Import CycloneDX SBOM using the method on the thrift client.
     * @param user                User uploading the SBOM
     * @param attachmentContentId Id of the attachment uploaded
     * @return RequestSummary
     * @throws TException
     */
    public RequestSummary importCycloneDX(User user, String attachmentContentId, String projectId) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.importCycloneDxFromAttachmentContent(user, attachmentContentId, CommonUtils.nullToEmptyString(projectId));
    }

    /**
     * Get Projects are using release in dependencies (enable.flexible.project.release.relationship = true)
     * @param releaseId                Id of release
     * @return List<Project>
     */
    public List<Project> getProjectsUsedReleaseInDependencyNetwork(String releaseId) {
        return SW360Utils.getUsingProjectByReleaseIds(Collections.singleton(releaseId), null);
    }

    public void syncReleaseRelationNetworkAndReleaseIdToUsage(Project project, User user) throws TException {
        SW360Utils.syncReleaseRelationNetworkAndReleaseIdToUsage(project, user);
    }

    /**
     * Count the number of projects are using the releases that has releaseIds
     * @param releaseIds              Ids of Releases
     * @return int                    Number of projects
     * @throws TException
     */
    public int countProjectsByReleaseIds(Set<String> releaseIds) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            return sw360ProjectClient.getCountByReleaseIds(releaseIds);
        } catch (TException e) {
            log.error(e.getMessage());
            return 0;
        }
    }
}
