/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.getSortedMap;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ComponentService implements AwareOfRestServices<Component> {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private final RestControllerHelper<Component> rch;

    @NonNull
    private final Sw360ProjectService projectService;

    public List<Component> getComponentsForUser(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getComponentSummary(sw360User);
    }

    public Release getReleaseById(String id, User sw360User) {
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            Release release = sw360ComponentClient.getReleaseById(id, sw360User);
            Map<String, String> sortedAdditionalData = getSortedMap(release.getAdditionalData(), true);
            release.setAdditionalData(sortedAdditionalData);
            return release;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Component getComponentForUserById(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360User);
        Map<String, String> sortedAdditionalData = getSortedMap(component.getAdditionalData(), true);
        component.setAdditionalData(sortedAdditionalData);
        return component;
    }

    public Set<Project> getProjectsByComponentId(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360User);
        Set<String> releaseIds = SW360Utils.getReleaseIds(component.getReleases());

        return getUsingProjectAccesibleByReleaseIds(releaseIds, sw360User);
    }

    public Set<Component> getUsingComponentsForComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360User);
        Set<String> releaseIds = SW360Utils.getReleaseIds(component.getReleases());
        return sw360ComponentClient.getUsingComponentsForComponent(releaseIds);
    }

    @Override
    public Set<Component> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchComponentsByExternalIds(externalIds);
    }

    @Override
    public Component convertToEmbeddedWithExternalIds(Component sw360Object) {
        return rch.convertToEmbeddedComponent(sw360Object).setExternalIds(sw360Object.getExternalIds());
    }

    public Component createComponent(Component component, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        AddDocumentRequestSummary documentRequestSummary = sw360ComponentClient.addComponent(component, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            component.setId(documentRequestSummary.getId());
            component.setCreatedBy(sw360User.getEmail());
            Map<String, String> sortedAdditionalData = getSortedMap(component.getAdditionalData(), true);
            component.setAdditionalData(sortedAdditionalData);
            return component;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 component with name '" + component.getName() + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Component name field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public RequestStatus updateComponent(Component component, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus = sw360ComponentClient.updateComponent(component, sw360User);
        if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Component name field cannot be empty or contain only whitespace character");
        } else if (requestStatus != RequestStatus.SUCCESS && requestStatus != RequestStatus.SENT_TO_MODERATOR) {
            throw new RuntimeException("sw360 component with name '" + component.getName() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deleteComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.deleteComponent(componentId, sw360User);
    }

    public List<Component> searchComponentByName(String name) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchComponentForExport(name.toLowerCase(), false);
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }

    private ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
    }

    public Set<Project> getUsingProjectAccesibleByReleaseIds(Set<String> releaseIds, User user) {
        return SW360Utils.getUsingProjectByReleaseIds(releaseIds, user).stream().collect(Collectors.toSet());
    }
}
