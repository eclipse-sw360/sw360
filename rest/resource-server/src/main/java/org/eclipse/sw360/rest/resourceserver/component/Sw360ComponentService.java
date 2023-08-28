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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.ClearingReport;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @NonNull
    private final Sw360VulnerabilityService vulnerabilityService;

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
        Map<String, String> sortedAdditionalData = CommonUtils.getSortedMap(component.getAdditionalData(), true);
        component.setAdditionalData(sortedAdditionalData);
        return component;
    }

    public Set<Project> getProjectsByComponentId(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360User);
        Set<String> releaseIds = SW360Utils.getReleaseIds(component.getReleases());

        return projectService.getProjectsByReleaseIds(releaseIds, sw360User);
    }
    public List<Component> getComponentSubscriptions(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getSubscribedComponents(sw360User);
    }

    public List<Component> getRecentComponents(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getRecentComponentsSummary(5, sw360User);
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
        RequestStatus requestStatus;
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            requestStatus = sw360ComponentClient.updateComponentWithForceFlag(component, sw360User, true);
        } else {
            requestStatus = sw360ComponentClient.updateComponent(component, sw360User);
        }
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
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            return sw360ComponentClient.deleteComponentWithForceFlag(componentId, sw360User, true);
        } else {
            return sw360ComponentClient.deleteComponent(componentId, sw360User);
        }
    }

    public List<Component> searchComponentByName(String name) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchComponentForExport(name.toLowerCase(), false);
    }

    public List<Release> getReleasesByComponentId(String id,User user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getReleasesFullDocsFromComponentId(id, user);
    }

    public List<ReleaseLink> convertReleaseToReleaseLink(String id,User user) throws TException {
        List<Release> releases = getReleasesByComponentId(id,user);
        List<ReleaseLink> releaseLinks = new ArrayList<>();
        releases.forEach(release -> {
            ReleaseLink releaseLink =new ReleaseLink();
            releaseLink.setId(release.getId());
            releaseLink.setName(release.getName());
            releaseLink.setVersion(release.getVersion());
            releaseLink.setClearingState(release.getClearingState());

            ClearingReport clearingReport = new ClearingReport();
            Set<Attachment> attachments = getAttachmentForClearingReport(release);
            if (!attachments.equals(Collections.emptySet())) {
                Set<Attachment> attachmentsAccepted = getAttachmentsStatusAccept(attachments);
                if(attachmentsAccepted.size() != 0) {
                    clearingReport.setClearingReportStatus(ClearingReportStatus.DOWNLOAD);
                    clearingReport.setAttachments(attachmentsAccepted);
                    releaseLink.setClearingReport(clearingReport);
                } else {
                    clearingReport.setClearingReportStatus(ClearingReportStatus.NO_STATUS);
                    releaseLink.setClearingReport(clearingReport);
                }
            } else {
                clearingReport.setClearingReportStatus(ClearingReportStatus.NO_REPORT);
                releaseLink.setClearingReport(clearingReport);
            }

            releaseLink.setMainlineState(release.getMainlineState());
            releaseLinks.add(releaseLink);
        });

        return releaseLinks;
    }

    private Set<Attachment> getAttachmentForClearingReport(Release release){
        final Set<Attachment> attachments = release.getAttachments();
        if (CommonUtils.isNullOrEmptyCollection(attachments))
            return Collections.emptySet();
        return attachments.stream().filter(attachment -> AttachmentType.COMPONENT_LICENSE_INFO_XML.equals(attachment.getAttachmentType()) ||
                                                         AttachmentType.CLEARING_REPORT.equals(attachment.getAttachmentType()))
                                   .collect(Collectors.toSet());
    }

    private Set<Attachment> getAttachmentsStatusAccept(Set<Attachment> attachments){
        return attachments.stream().filter(attachment -> CheckStatus.ACCEPTED.equals(attachment.getCheckStatus()))
                                   .collect(Collectors.toSet());
    }

    private Boolean checkStatusAttachment(Release release){
        final Set<Attachment> attachments = release.getAttachments();
        boolean checkStatusAttachment = attachments.stream().anyMatch(attachment ->
                CheckStatus.ACCEPTED.equals(attachment.getCheckStatus()));
        return checkStatusAttachment;
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

    public List<Component> getMyComponentsForUser(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getMyComponents(sw360User);
    }
    
    public List<VulnerabilityDTO> getVulnerabilitiesByComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        List<String> releaseIds = sw360ComponentClient.getReleaseIdsFromComponentId(componentId, sw360User);
        List<VulnerabilityDTO> vulnerabilityDTOByComponent = new ArrayList<>();
        for (String releaseId: releaseIds) {
            vulnerabilityDTOByComponent.addAll(vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, sw360User));
        }
        return vulnerabilityDTOByComponent;
    }

    public List<String> getReleaseIdsFromComponentId(String componentId, User user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getReleaseIdsFromComponentId(componentId, user);
    }

    public RequestSummary importSBOM(User user, String attachmentContentId) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.importBomFromAttachmentContent(user, attachmentContentId);
    }

    public ImportBomRequestPreparation prepareImportSBOM(User user, String attachmentContentId) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.prepareImportBom(user, attachmentContentId);
    }

  public RequestStatus mergeComponents(String componentTargetId, String componentSourceId, Component componentSelection, User user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus;
        requestStatus =  sw360ComponentClient.mergeComponents(componentTargetId, componentSourceId, componentSelection, user);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new HttpMessageNotReadableException("Component already in use.");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new HttpMessageNotReadableException("Cannot merge these components");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new RuntimeException("Access denied");
        }

        return requestStatus;
  }

    public RequestStatus splitComponents(Component srcComponent, Component targetComponent, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus;
        requestStatus = sw360ComponentClient.splitComponent(srcComponent, targetComponent, sw360User);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new HttpMessageNotReadableException("Component already in use.");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new HttpMessageNotReadableException("Cannot split these components");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new RuntimeException("Access denied...!");
        }

        return requestStatus;
    }

    /**
     * Count the number of projects are using the component that has componentId
     *
     * @param componentId Ids of Component
     * @return int                    Number of projects
     * @throws TException
     */
    public int countProjectsByComponentId(String componentId, User sw360user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360user);
        Set<String> releaseIds = SW360Utils.getReleaseIds(component.getReleases());
        return projectService.countProjectsByReleaseIds(releaseIds);
    }
}
