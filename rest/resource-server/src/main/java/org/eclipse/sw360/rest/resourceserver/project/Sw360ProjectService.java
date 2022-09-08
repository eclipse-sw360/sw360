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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLinkJSON;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

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

import javax.annotation.PreDestroy;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.getSortedMap;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService implements AwareOfRestServices<Project> {

    private static final Logger log = LogManager.getLogger(Sw360ProjectService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private RestControllerHelper rch;

    public static final ExecutorService releaseExecutor = Executors.newFixedThreadPool(10);

    public Set<Project> getProjectsForUser(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        int total = sw360ProjectClient.getMyAccessibleProjectCounts(sw360User);
        PaginationData pageData = new PaginationData();
        pageData.setAscending(true);
        Map<PaginationData, List<Project>> pageDtToProjects;
        Set<Project> projects = new HashSet<>();
        int displayStart = 0;
        int rowsPerPage = 500;
        while (0 < total) {
            pageData.setDisplayStart(displayStart);
            pageData.setRowsPerPage(rowsPerPage);
            displayStart = displayStart + rowsPerPage;
            pageDtToProjects = sw360ProjectClient.getAccessibleProjectsSummaryWithPagination(sw360User, pageData);
            projects.addAll(pageDtToProjects.entrySet().iterator().next().getValue());
            total = total - rowsPerPage;
        }
        return projects;
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

        if (project.getVendor() != null && project.getVendorId() == null) {
            project.setVendorId(project.getVendor().getId());
        }

        RequestStatus requestStatus = sw360ProjectClient.updateProject(project, sw360User);
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

    public void deleteProject(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        RequestStatus requestStatus = sw360ProjectClient.deleteProject(projectId, sw360User);
        if (requestStatus == RequestStatus.IN_USE) {
            throw new HttpMessageNotReadableException("Unable to delete project. Project is in Use");
        } else if (requestStatus != RequestStatus.SUCCESS) {
            throw new RuntimeException("sw360 project with id '" + projectId + " cannot be deleted.");
        }
    }

    public void deleteAllProjects(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        List<Project> projects = sw360ProjectClient.getAccessibleProjectsSummary(sw360User);
        for (Project project : projects) {
            sw360ProjectClient.deleteProject(project.getId(), sw360User);
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

    public void addEmbeddedlinkedRelease(ReleaseLinkJSON sw360Release, User sw360User, HalResource<Release> releaseResource,
                                         Sw360ReleaseService releaseService) throws TException {
        List<ReleaseLinkJSON> releaseInRelationShip = sw360Release.getReleaseLink();
        if (releaseInRelationShip != null) {
            releaseInRelationShip.forEach(release -> wrapTException(() -> {
                Release linkedRelease = releaseService.getReleaseForUserById(release.getReleaseId(), sw360User);
                Release embeddedLinkedRelease = rch.convertToEmbeddedRelease(linkedRelease);
                HalResource<Release> halLinkedRelease = new HalResource<>(embeddedLinkedRelease);
                Link releaseLink = linkTo(ReleaseController.class)
                        .slash("api/releases/" + embeddedLinkedRelease.getId()).withSelfRel();
                halLinkedRelease.add(releaseLink);
                addEmbeddedlinkedRelease(release, sw360User, halLinkedRelease, releaseService);
                releaseResource.addEmbeddedResource("sw360:releases", halLinkedRelease);
            }));
        }
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
                .flattenProjectLinkTree(SW360Utils.getLinkedProjectsWithAllReleases(project, deep, new ThriftClients(), log, user));
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
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

    public List<ReleaseLinkJSON> getReleasesLinkDirectlyByProjectId(String projectId, User sw360User, boolean transitive) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        Project sw360Project = sw360ProjectClient.getProjectById(projectId, sw360User);
        List<ReleaseLinkJSON> releaseLinkedDirectly = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (sw360Project.getReleaseRelationNetwork() != null) {
            try {
                List<ReleaseLinkJSON> releaseLinkJSONS = objectMapper.readValue(sw360Project.getReleaseRelationNetwork(), new TypeReference<>() {
                });
                releaseLinkedDirectly.addAll(releaseLinkJSONS);
                if(transitive) {
                    if(sw360Project.getLinkedProjects() != null) {
                        List<String> subProjectIds = sw360Project.getLinkedProjects().keySet().stream().collect(Collectors.toList());
                        subProjectIds.forEach((proId) -> {
                            try {
                                releaseLinkedDirectly.addAll(getReleasesLinkDirectlyByProjectId(proId, sw360User, transitive));
                            } catch (TException e) {
                                log.error("Error when fetch Project " + proId);
                            }
                        });
                    }
                }
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        return releaseLinkedDirectly;
    }

    public List<ReleaseLinkJSON> getReleasesInDependencyNetworkFromProjectIds(List<String> projectIds, final User sw360User, boolean transitive) {
        List<ReleaseLinkJSON> releasesLinked = new ArrayList<>();

        projectIds.forEach(id -> {
            try {
                releasesLinked.addAll(getReleasesLinkDirectlyByProjectId(id, sw360User, transitive));
            } catch (TException e) {
                log.error("Error when fetch Project " + id);
            }
        });

        return releasesLinked;
    }

    public Set<String> getReleasesIdByProjectId(String projectId, User sw360User, String transitive) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        Project sw360Project = sw360ProjectClient.getProjectById(projectId, sw360User);
        Set<String> releaseIds = new HashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (sw360Project.getReleaseRelationNetwork() != null) {
            try {
                List<ReleaseLinkJSON> releaseLinkJSONS = objectMapper.readValue(sw360Project.getReleaseRelationNetwork(), new TypeReference<>() {
                });
                if (!Boolean.parseBoolean(transitive)) {
                    releaseIds.addAll(releaseLinkJSONS.stream()
                            .map(ReleaseLinkJSON::getReleaseId)
                            .collect(Collectors.toSet()));
                } else {
                    for (ReleaseLinkJSON release : releaseLinkJSONS) {
                        getReleaseIdInDependency(release, releaseIds);
                    }
                    if(sw360Project.getLinkedProjects() != null) {
                        List<String> subProjectIds = sw360Project.getLinkedProjects().keySet().stream().collect(Collectors.toList());
                        subProjectIds.forEach((proId) -> {
                            try {
                                releaseIds.addAll(getReleasesIdByProjectId(proId, sw360User, transitive));
                            } catch (TException e) {
                                log.error("Error when fetch project: " + proId);
                            }
                        });
                    }
                }

            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        return releaseIds;
    }
    private Set<String> getReleaseIdInDependency(ReleaseLinkJSON node, Set<String> flatList) {

        if (node != null) {
            flatList.add(node.getReleaseId());
        }

        List<ReleaseLinkJSON> children = node.getReleaseLink();
        for (ReleaseLinkJSON child : children) {
            if(child.getReleaseLink() != null) {
                getReleaseIdInDependency(child, flatList);
            } else {
                flatList.add(node.getReleaseId());
            }
        }

        return flatList;
    }

    public List<Release> getDirectDependenciesOfReleaseInNetwork(Project project, String releaseId, User sw360User) throws TException {
        ObjectMapper objectMapper = new ObjectMapper();
        ComponentService.Iface releaseClient = getThriftComponentClient();
        Set<String> dependenciesId = new HashSet<>();
        try {
            List<ReleaseLinkJSON> dependencyNetwork = objectMapper.readValue(project.getReleaseRelationNetwork(), new TypeReference<List<ReleaseLinkJSON>>() {
            });
            for (ReleaseLinkJSON releaseNode : dependencyNetwork) {
                if (releaseNode.getReleaseId().equals(releaseId)) {
                    Set<String> subReleaseIds = releaseNode.getReleaseLink().stream().map(ReleaseLinkJSON::getReleaseId).collect(Collectors.toSet());
                    dependenciesId.addAll(subReleaseIds);
                } else {
                    dependenciesId.addAll(getDependenciesOfSubNodeByReleaseId(releaseNode.getReleaseLink(), releaseId));
                }
            }
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }

        List<Release> dependenceReleases = releaseClient.getReleasesById(dependenciesId, sw360User);
        return dependenceReleases;
    }

    public Set<String> getDependenciesOfSubNodeByReleaseId(List<ReleaseLinkJSON> subNode, String releaseId) {
        Set<String> dependenciesId = new HashSet<>();
        for (ReleaseLinkJSON releaseNode : subNode) {
            if (releaseNode.getReleaseId().equals(releaseId)) {
                Set<String> subReleaseIds = releaseNode.getReleaseLink().stream().map(ReleaseLinkJSON::getReleaseId).collect(Collectors.toSet());
                dependenciesId.addAll(subReleaseIds);
            } else {
                dependenciesId.addAll(getDependenciesOfSubNodeByReleaseId(releaseNode.getReleaseLink(), releaseId));
            }
        }
        return dependenciesId;
    }

    public ComponentService.Iface getThriftComponentClient() {
        ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();
        return componentClient;
    }
}
