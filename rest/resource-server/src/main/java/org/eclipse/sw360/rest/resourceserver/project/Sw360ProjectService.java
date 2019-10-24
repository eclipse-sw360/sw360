/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService implements AwareOfRestServices<Project> {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private RestControllerHelper rch;

    public List<Project> getProjectsForUser(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getAccessibleProjectsSummary(sw360User);
    }

    public Project getProjectForUserById(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getProjectById(projectId, sw360User);
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
        AddDocumentRequestSummary documentRequestSummary = sw360ProjectClient.addProject(project, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            project.setId(documentRequestSummary.getId());
            project.setCreatedBy(sw360User.getEmail());
            return project;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 project with name '" + project.getName() + "' already exists.");
        }
        return null;
    }

    public RequestStatus updateProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        RequestStatus requestStatus = sw360ProjectClient.updateProject(project, sw360User);
        if (requestStatus == RequestStatus.CLOSED_UPDATE_NOT_ALLOWED) {
            throw new RuntimeException("User cannot modify a closed project");
        } else if (requestStatus != RequestStatus.SUCCESS) {
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
}
