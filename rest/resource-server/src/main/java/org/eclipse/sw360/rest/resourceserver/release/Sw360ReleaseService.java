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

package org.eclipse.sw360.rest.resourceserver.release;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ReleaseService implements AwareOfRestServices<Release> {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private RestControllerHelper rch;

    @NonNull
    private final Sw360ProjectService projectService;

    public List<Release> getReleasesForUser(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getReleaseSummary(sw360User);
    }

    public Release getReleaseForUserById(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getReleaseById(releaseId, sw360User);
    }

    @Override
    public Set<Release> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchReleasesByExternalIds(externalIds);
    }

    @Override
    public Release convertToEmbeddedWithExternalIds(Release sw360Object) {
        return rch.convertToEmbeddedRelease(sw360Object).setExternalIds(sw360Object.getExternalIds());
    }

    public Release createRelease(Release release, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        AddDocumentRequestSummary documentRequestSummary = sw360ComponentClient.addRelease(release, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            release.setId(documentRequestSummary.getId());
            return release;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 release with name '" + release.getName() + "' already exists.");
        }
        return null;
    }

    public RequestStatus updateRelease(Release release, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus = sw360ComponentClient.updateRelease(release, sw360User);
        if (requestStatus != RequestStatus.SUCCESS) {
            throw new RuntimeException("sw360 release with name '" + release.getName() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deleteRelease(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.deleteRelease(releaseId, sw360User);
    }

    public Set<Project> getProjectsByRelease(String releaseId, User sw360User) throws TException {
        Set<Project> usedByProjects = projectService.getProjectsByRelease(releaseId, sw360User);
        return usedByProjects;
    }

    public Set<Component> getUsingComponentsForRelease(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Set<Component> usingComponentsForComponent = sw360ComponentClient.getUsingComponentsForRelease(releaseId);
        return usingComponentsForComponent;
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }
}
