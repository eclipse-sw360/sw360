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

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.ClearingReport;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentDTO;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.getSortedMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_FORCE_UPDATE_ENABLED;

@Service
@RequiredArgsConstructor
public class Sw360ComponentService implements AwareOfRestServices<Component> {

    @NonNull
    private final RestControllerHelper<Component> rch;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360VulnerabilityService vulnerabilityService;

    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User sw360User,
                                                                                         Pageable pageable) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360ComponentClient.getRecentComponentsSummaryWithPagination(sw360User, pageData);
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
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            Component component = sw360ComponentClient.getComponentById(componentId, sw360User);
            Map<String, String> sortedAdditionalData = CommonUtils.getSortedMap(component.getAdditionalData(), true);
            component.setAdditionalData(sortedAdditionalData);
            return component;
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Component does not exist! id=" + componentId);
            } else {
                throw sw360Exp;
            }
        }
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

    public RequestStatus subscribeComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.subscribeComponent(componentId, sw360User);
    }

    public RequestStatus unsubscribeComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.unsubscribeComponent(componentId, sw360User);
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
            throw new BadRequestClientException("Dependent document Id/ids not valid.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new BadRequestClientException("Component name field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public RequestStatus updateComponent(Component component, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus;
        if (SW360Utils.readConfig(IS_FORCE_UPDATE_ENABLED, false)) {
            requestStatus = sw360ComponentClient.updateComponentWithForceFlag(component, sw360User, true);
        } else {
            requestStatus = sw360ComponentClient.updateComponent(component, sw360User);
        }
        if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new BadRequestClientException("Dependent document Id/ids not valid.");
        } else if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new BadRequestClientException("Component name field cannot be empty or contain only whitespace character");
        } else if (requestStatus == RequestStatus.DUPLICATE_ATTACHMENT) {
            throw new RuntimeException("Multiple attachments with same name or content cannot be present in attachment list.");
        } else if (requestStatus != RequestStatus.SUCCESS && requestStatus != RequestStatus.SENT_TO_MODERATOR) {
            throw new RuntimeException("sw360 component with name '" + component.getName() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deleteComponent(String componentId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        if (SW360Utils.readConfig(IS_FORCE_UPDATE_ENABLED, false)) {
            return sw360ComponentClient.deleteComponentWithForceFlag(componentId, sw360User, true);
        } else {
            return sw360ComponentClient.deleteComponent(componentId, sw360User);
        }
    }

    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(
            User sw360User, String name, Pageable pageable
    ) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchComponentByExactNamePaginated(sw360User, name, pageableToPaginationData(pageable));
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

            //  Added as part of https://github.com/eclipse-sw360/sw360/issues/3161
            releaseLink.setCreatedBy(release.getCreatedBy());

            ClearingReport clearingReport = new ClearingReport();
            Set<Attachment> attachments = getAttachmentForClearingReport(release);
            if (!attachments.equals(Collections.emptySet())) {
                Set<Attachment> attachmentsAccepted = getAttachmentsStatusAccept(attachments);
                if(!attachmentsAccepted.isEmpty()) {
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
        return attachments.stream().anyMatch(attachment ->
                CheckStatus.ACCEPTED.equals(attachment.getCheckStatus()));
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        return new ThriftClients().makeComponentClient();
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

    public RequestStatus mergeComponents(String componentTargetId, String componentSourceId,
                                         Component componentSelection, User user) throws TException {
        validateComponentMergeSelection(componentSelection);

        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus requestStatus = sw360ComponentClient.mergeComponents(
                componentTargetId, componentSourceId, componentSelection, user);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new BadRequestClientException("Component already in use.");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new BadRequestClientException("Cannot merge these components");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Access denied");
        }

        return requestStatus;
    }

    public RequestStatus splitComponents(Component srcComponent, Component targetComponent, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();

        boolean found = false;
        try {
            if (sw360ComponentClient.getComponentById(srcComponent.getId(), sw360User) != null
                    && sw360ComponentClient.getComponentById(targetComponent.getId(), sw360User) != null) {
                found = true;
            }
        } catch (TException ignored) {
        }

        if (!found) {
            throw new ResourceNotFoundException("Source or target component not found");
        }

        RequestStatus requestStatus = sw360ComponentClient.splitComponent(srcComponent, targetComponent, sw360User);

        if (requestStatus == RequestStatus.IN_USE) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "Component has Moderation Request Open");
        } else if (requestStatus == RequestStatus.FAILURE) {
            throw new SW360Exception("Cannot split these components");
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Access denied!");
        }

        return requestStatus;
    }

    /**
     * Count the number of projects are using the component that has componentId
     *
     * @param componentId Ids of Component
     * @return int                    Number of projects
     */
    public int countProjectsByComponentId(String componentId, User sw360user) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Component component = sw360ComponentClient.getComponentById(componentId, sw360user);
        Set<String> releaseIds = SW360Utils.getReleaseIds(component.getReleases());
        return projectService.countProjectsByReleaseIds(releaseIds);
    }

    public Map<PaginationData, List<Component>> refineSearch(Map<String, Set<String>> filterMap, User sw360User, Pageable pageable) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360ComponentClient.refineSearchAccessibleComponents(null, filterMap, sw360User, pageData);
    }

    public Map<PaginationData, List<Component>> searchComponentByExactValues(Map<String, Set<String>> filterMap, User sw360User, Pageable pageable) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360ComponentClient.searchComponentByExactValues(filterMap, sw360User, pageData);
    }

    /**
     * Validate if the `componentSelection` object is not null and contains the required fields.
     * @param componentSelection The component selection object to validate.
     * @throws BadRequestClientException if the object is null or missing required fields.
     */
    private void validateComponentMergeSelection(Component componentSelection) {
        if (componentSelection == null) {
            throw new BadRequestClientException("Body for merge cannot be null");
        }
        Set<Component._Fields> requiredFields = ImmutableSet.<Component._Fields>builder()
                .add(Component._Fields.NAME)
                .add(Component._Fields.CREATED_ON)
                .add(Component._Fields.CREATED_BY)
                .build();

        for (Component._Fields field : requiredFields) {
            if (!componentSelection.isSet(field) || isNullEmptyOrWhitespace((String) componentSelection.getFieldValue(field))) {
                throw new BadRequestClientException("Merge body is missing field " + field.getFieldName());
            }
        }
    }

    /**
     * Get the list of releases from ComponentDTO's releaseIds set of string.
     * @param componentDTO Object to get release IDs from
     * @return List of Release
     */
    public List<Release> getReleasesFromDto(@NotNull ComponentDTO componentDTO, User user) throws TTransportException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();

        List<Release> releases = new ArrayList<>();

        if (!componentDTO.isSetReleaseIds() || componentDTO.getReleaseIds().isEmpty()) {
            return releases;
        }

        for (String releaseId : componentDTO.getReleaseIds()) {
            Release release;
            try {
                release = sw360ComponentClient.getReleaseById(releaseId, user);
            } catch (TException e) {
                continue;
            }
            releases.add(release);
        }

        return releases;
    }

    /**
     * Converts a Pageable object to a PaginationData object.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        ComponentSortColumn column = ComponentSortColumn.BY_CREATEDON;
        boolean ascending = false;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "createdOn" -> ComponentSortColumn.BY_CREATEDON;
                case "name" -> ComponentSortColumn.BY_NAME;
                case "vendorNames" -> ComponentSortColumn.BY_VENDOR;
                case "mainLicenseIds" -> ComponentSortColumn.BY_MAINLICENSE;
                case "type" -> ComponentSortColumn.BY_TYPE;
                default -> column; // Default to BY_CREATEDON if no match
            };
            ascending = order.isAscending();
        }
        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }
}
