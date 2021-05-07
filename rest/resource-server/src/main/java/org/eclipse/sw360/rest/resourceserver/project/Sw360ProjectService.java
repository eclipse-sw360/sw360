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
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
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
import org.eclipse.sw360.datahandler.thrift.projects.Project;
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
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

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
        return sw360ProjectClient.getAccessibleProjects(sw360User);
    }

    public Project getProjectForUserById(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        try {
            return sw360ProjectClient.getProjectById(projectId, sw360User);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested Project Not Found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "Error fetching project. Either Project or its Linked Projects are not accessible");
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

    public RequestStatus deleteProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        RequestStatus requestStatus = sw360ProjectClient.deleteProject(project.getId(), sw360User);
        if (requestStatus != RequestStatus.SUCCESS) {
            throw new RuntimeException("sw360 project with name '" + project.getName() + " cannot be deleted.");
        }
        return requestStatus;
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

    public Set<String> getReleaseIds(String projectId, User sw360User, String transitive) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        if (Boolean.parseBoolean(transitive)) {
            List<ReleaseClearingStatusData> releaseClearingStatusData = sw360ProjectClient.getReleaseClearingStatuses(projectId, sw360User);
            return releaseClearingStatusData.stream().map(r -> r.release.getId()).collect(Collectors.toSet());
        } else {
            final Project project = getProjectForUserById(projectId, sw360User);
            return project.getReleaseIdToUsage().keySet();
        }
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

    private ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
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

    public Set<Release> getReleasesFromProjectIds(List<String> projectIds, String transitive, final User sw360User, Sw360ReleaseService releaseService) {
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
}
