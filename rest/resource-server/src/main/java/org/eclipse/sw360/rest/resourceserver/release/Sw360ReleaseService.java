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

package org.eclipse.sw360.rest.resourceserver.release;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.google.common.collect.Sets;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;


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
    private final Sw360LicenseService licenseService;

    private static FossologyService.Iface fossologyClient;
    private static final String RESPONSE_STATUS_VALUE_COMPLETED = "Completed";
    private static final String RESPONSE_STATUS_VALUE_FAILED = "Failed";
    private static final String RELEASE_ATTACHMENT_ERRORMSG = "There has to be exactly one source attachment, but there are %s at this release. Please come back once you corrected that.";

    public List<Release> getReleasesForUser(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getAllReleasesForUser(sw360User);
    }

    public Release getReleaseForUserById(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Release releaseById = null;
        try {
            releaseById = sw360ComponentClient.getReleaseById(releaseId, sw360User);
            setComponentDependentFieldsInRelease(releaseById, sw360User);
            Map<String, String> sortedAdditionalData = CommonUtils.getSortedMap(releaseById.getAdditionalData(), true);
            releaseById.setAdditionalData(sortedAdditionalData);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Release does not exists! id=" + releaseId);
            } else {
                throw sw360Exp;
            }
        }

        return releaseById;
    }

    public Set<Release> getReleasesForUserByIds(Set<String> releaseIds) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Set<Release> limitedSet = new HashSet<>();
        try {
            List<Release> releases = sw360ComponentClient.getReleasesByIdsForExport(releaseIds);
            for(Release rel: releases) {
                Release limitedRelease = new Release();
                limitedRelease.setId(rel.getId());
                limitedRelease.setName(rel.getName());
                limitedRelease.setVersion(rel.getVersion());
                limitedSet.add(limitedRelease);
            }
        } catch (SW360Exception sw360Exp) {
                if (sw360Exp.getErrorCode() == 404) {
                    throw new ResourceNotFoundException("Release does not exists!");
                } else {
                    throw sw360Exp;
                }
            }
        return limitedSet;
    }

    public List<ReleaseLink> getLinkedReleaseRelations(Release release, User user) throws TException {
        List<ReleaseLink> linkedReleaseRelations = getLinkedReleaseRelationsWithAccessibility(release, user);
        linkedReleaseRelations = linkedReleaseRelations.stream().filter(Objects::nonNull).sorted(Comparator.comparing(
                rl -> rl.isAccessible() ? SW360Utils.getVersionedName(nullToEmptyString(rl.getName()), rl.getVersion()) : "~", String.CASE_INSENSITIVE_ORDER)
        ).collect(Collectors.toList());
        return linkedReleaseRelations;
    }

    public List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Release release, User user) throws TException {
        if (release != null && release.getReleaseIdToRelationship() != null) {
            ComponentService.Iface componentClient = getThriftComponentClient();
            return componentClient.getLinkedReleaseRelationsWithAccessibility(release.getReleaseIdToRelationship(), user);
        }
        return Collections.emptyList();
    }

    public Release setComponentDependentFieldsInRelease(Release releaseById, User sw360User) {
        String componentId = releaseById.getComponentId();
        if (CommonUtils.isNullEmptyOrWhitespace(componentId)) {
            throw new HttpMessageNotReadableException("ComponentId must be present");
        }
        Component componentById = null;
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            componentById = sw360ComponentClient.getComponentById(componentId, sw360User);
        } catch (TException e) {
            throw new HttpMessageNotReadableException("No Component found with Id - " + componentId);
        }
        releaseById.setComponentType(componentById.getComponentType());
        return releaseById;
    }

    public List<Release> setComponentDependentFieldsInRelease(List<Release> releases, User sw360User) {
        Map<String, Component> componentIdMap;

        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            List<Component> components = sw360ComponentClient.getComponentSummary(sw360User);
            componentIdMap = components.stream().collect(Collectors.toMap(Component::getId, c -> c));
        } catch (TException e) {
            throw new HttpMessageNotReadableException("No Components found");
        }
        
        for (Release release : releases) {
            String componentId = release.getComponentId();
            if (CommonUtils.isNullEmptyOrWhitespace(componentId)) {
                throw new HttpMessageNotReadableException("ComponentId must be present");
            }
            if (!componentIdMap.containsKey(componentId)) {
            	throw new HttpMessageNotReadableException("No Component found with Id - " + componentId);
            }
            Component component = componentIdMap.get(componentId);
            release.setComponentType(component.getComponentType());
        }
        return releases;
    }
    
    public List<Release> getReleaseSubscriptions(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getSubscribedReleases(sw360User);
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

    public SPDXDocument getSPDXDocumentById(String id, User user) throws TException {
        SPDXDocumentService.Iface spdxDocumentService = getThriftSPDXDocumentClient();
        return spdxDocumentService.getSPDXDocumentById(id, user);
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) throws TException {
        DocumentCreationInformationService.Iface documentCreationInformationService = getThriftDocumentCreationInformation();
        return documentCreationInformationService.getDocumentCreationInformationById(id, user);
    }

    public PackageInformation getPackageInformationById(String id, User user) throws TException {
        PackageInformationService.Iface packageInformation = getThriftIPackageInformation();
        return packageInformation.getPackageInformationById(id, user);
    }

    public Release createRelease(Release release, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        setComponentNameAsReleaseName(release, sw360User);
        rch.checkForCyclicOrInvalidDependencies(sw360ComponentClient, release, sw360User);
        AddDocumentRequestSummary documentRequestSummary = sw360ComponentClient.addRelease(release, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            release.setId(documentRequestSummary.getId());
            Map<String, String> sortedAdditionalData = CommonUtils.getSortedMap(release.getAdditionalData(), true);
            release.setAdditionalData(sortedAdditionalData);
            return release;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 release with name '" + SW360Utils.printName(release) + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        }
        else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException(
                    "Release name and version field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public void setComponentNameAsReleaseName(Release release, User sw360User) {
        String componentId = release.getComponentId();
        if (CommonUtils.isNullEmptyOrWhitespace(componentId)) {
            throw new HttpMessageNotReadableException("ComponentId must be present");
        }
        Component componentById = null;
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            componentById = sw360ComponentClient.getComponentById(componentId, sw360User);
        } catch (TException e) {
            throw new HttpMessageNotReadableException("No Component found with Id - " + componentId);
        }
        release.setName(componentById.getName());
    }

    public RequestStatus updateRelease(Release release, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        rch.checkForCyclicOrInvalidDependencies(sw360ComponentClient, release, sw360User);

        RequestStatus requestStatus;
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            requestStatus = sw360ComponentClient.updateReleaseWithForceFlag(release, sw360User, true);
        } else {
            requestStatus = sw360ComponentClient.updateRelease(release, sw360User);
        }
        if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException(
                    "Release name and version field cannot be empty or contain only whitespace character");
        } else if (requestStatus == RequestStatus.DUPLICATE_ATTACHMENT) {
            throw new RuntimeException("Multiple attachments with same name or content cannot be present in attachment list.");
        } else if (requestStatus != RequestStatus.SUCCESS && requestStatus != RequestStatus.SENT_TO_MODERATOR) {
            throw new RuntimeException(
                    "sw360 release with name '" + SW360Utils.printName(release) + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdxDocumentRequest, String releaseId, User user) throws TException {
        SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
        if (null == spdxDocumentRequest) {
            return null;
        }
        if (isNullOrEmpty(spdxDocumentRequest.getReleaseId()) && !isNullOrEmpty(releaseId)) {
            spdxDocumentRequest.setReleaseId(releaseId);
        }
        SPDXDocument spdxDocumentUpdate = prepareUpdateSPDXDocument(getSPDXDocumentById(spdxDocumentRequest.getId(), user),spdxDocumentRequest);
        return spdxClient.updateSPDXDocument(spdxDocumentUpdate, user);
    }

    public SPDXDocument prepareUpdateSPDXDocument(SPDXDocument spdxDocumentActual, SPDXDocument spdxDocumentRequest) {
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            Object fieldValue = spdxDocumentRequest.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case SNIPPETS:
                        Set<SnippetInformation> snippetInformations = prepareUpdateSnippetInformations(spdxDocumentActual.getSnippets(), spdxDocumentRequest.getSnippets());
                        spdxDocumentActual.setFieldValue(field, snippetInformations);
                        break;
                    case RELATIONSHIPS:
                        Set<RelationshipsBetweenSPDXElements> relationships = prepareUpdateRelationShips(spdxDocumentActual.getRelationships(), spdxDocumentRequest.getRelationships());
                        spdxDocumentActual.setFieldValue(field, relationships);
                        break;
                    case ANNOTATIONS:
                        Set<Annotations> annotations = prepareUpdateAnnotations(spdxDocumentActual.getAnnotations(), spdxDocumentRequest.getAnnotations());
                        spdxDocumentActual.setFieldValue(field, annotations);
                        break;
                    case OTHER_LICENSING_INFORMATION_DETECTEDS:
                        Set<OtherLicensingInformationDetected> otherLicensingInformationDetecteds = prepareUpdateOtherLicensingInformationDetecteds(spdxDocumentActual.getOtherLicensingInformationDetecteds(), spdxDocumentRequest.getOtherLicensingInformationDetecteds());
                        spdxDocumentActual.setFieldValue(field, otherLicensingInformationDetecteds);
                        break;
                    default:
                        spdxDocumentActual.setFieldValue(field, fieldValue);
                }
            }
        }
        return spdxDocumentActual;
    }

    public Set<OtherLicensingInformationDetected> prepareUpdateOtherLicensingInformationDetecteds(Set<OtherLicensingInformationDetected> otherLicenseActual, Set<OtherLicensingInformationDetected> otherLicenseRequest) {
        if(CommonUtils.isNullOrEmptyCollection(otherLicenseRequest)) {
            return Collections.emptySet();
        }
        if(otherLicenseActual.size() > otherLicenseRequest.size()) {
            return otherLicenseRequest;
        }
        Set<Integer> indexOfOtherLicenseActual = otherLicenseActual.stream().map(OtherLicensingInformationDetected::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfOtherLicenserequest = otherLicenseRequest.stream().map(OtherLicensingInformationDetected::getIndex).collect(Collectors.toSet());

        Set<OtherLicensingInformationDetected> otherLicensesUpdate = new HashSet<>();
        otherLicensesUpdate.addAll(otherLicenseActual);
        if(otherLicenseActual.size() < otherLicenseRequest.size()) {
            Set<Integer> newIndexs = Sets.difference(indexOfOtherLicenserequest, indexOfOtherLicenseActual);
            for(OtherLicensingInformationDetected otherLicensingInformationDetected: otherLicenseRequest) {
                for(Integer index: newIndexs) {
                    if(otherLicensingInformationDetected.getIndex() == index) {
                        otherLicensesUpdate.add(otherLicensingInformationDetected);
                    }
                }
            }
        }

        for (OtherLicensingInformationDetected actual: otherLicensesUpdate){
            for (OtherLicensingInformationDetected request: otherLicenseRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateOtherLicensingInformationDetecteds(actual, request);
                }
            }
        }
        return otherLicensesUpdate;
    }

    public OtherLicensingInformationDetected updateOtherLicensingInformationDetecteds(OtherLicensingInformationDetected otherLicenseActual, OtherLicensingInformationDetected otherLicenseRequest) {
        for (OtherLicensingInformationDetected._Fields field : OtherLicensingInformationDetected._Fields.values()) {
            Object fieldValue = otherLicenseRequest.getFieldValue(field);
            if (fieldValue != null) {
                otherLicenseActual.setFieldValue(field, fieldValue);
            }
        }
        return otherLicenseActual;
    }

    public Set<Annotations> prepareUpdateAnnotations(Set<Annotations> annotationActual, Set<Annotations> annotationRequest) {
        if(CommonUtils.isNullOrEmptyCollection(annotationRequest)) {
            return Collections.emptySet();
        }
        if(annotationActual.size() > annotationRequest.size()) {
            return annotationRequest;
        }
        Set<Integer> indexOfAnnotationActual = annotationActual.stream().map(Annotations::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfAnnotationRequest = annotationRequest.stream().map(Annotations::getIndex).collect(Collectors.toSet());

        Set<Annotations> annotationsUpdate = new HashSet<>();
        annotationsUpdate.addAll(annotationActual);
        if(annotationActual.size() < annotationRequest.size()) {
            Set<Integer> newIndexs = Sets.difference(indexOfAnnotationRequest, indexOfAnnotationActual);
            for(Annotations annotation: annotationRequest) {
                for(Integer index: newIndexs) {
                    if(annotation.getIndex() == index) {
                        annotationsUpdate.add(annotation);
                    }
                }
            }
        }
        for (Annotations actual: annotationsUpdate){
            for (Annotations request: annotationRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateAnnotations(actual, request);
                }
            }
        }
        return annotationsUpdate;
    }

    public Annotations updateAnnotations(Annotations annotationActual, Annotations annotationRequest) {
        for (Annotations._Fields field : Annotations._Fields.values()) {
            Object fieldValue = annotationRequest.getFieldValue(field);
            if (fieldValue != null) {
                annotationActual.setFieldValue(field, fieldValue);
            }
        }
        return annotationActual;
    }

    public Set<RelationshipsBetweenSPDXElements> prepareUpdateRelationShips(Set<RelationshipsBetweenSPDXElements> relationsActual, Set<RelationshipsBetweenSPDXElements> relationsRequest) {
        if(CommonUtils.isNullOrEmptyCollection(relationsRequest)) {
            return Collections.emptySet();
        }
        if(relationsActual.size() > relationsRequest.size()) {
            return relationsRequest;
        }
        Set<Integer> indexOfRelationActual = relationsActual.stream().map(RelationshipsBetweenSPDXElements::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfRelationRequest = relationsRequest.stream().map(RelationshipsBetweenSPDXElements::getIndex).collect(Collectors.toSet());

        Set<RelationshipsBetweenSPDXElements> relationsUpdate = new HashSet<>();
        relationsUpdate.addAll(relationsActual);
        if(relationsActual.size() < relationsRequest.size()) {
            Set<Integer> newIndexs = Sets.difference(indexOfRelationRequest, indexOfRelationActual);
            for(RelationshipsBetweenSPDXElements relationshipsBetweenSPDXElements: relationsRequest) {
                for(Integer index: newIndexs) {
                    if(relationshipsBetweenSPDXElements.getIndex() == index) {
                        relationsUpdate.add(relationshipsBetweenSPDXElements);
                    }
                }
            }
        }
        for (RelationshipsBetweenSPDXElements actual: relationsUpdate){
            for (RelationshipsBetweenSPDXElements request: relationsRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateRelationships(actual, request);
                }
            }
        }
        return relationsUpdate;
    }

    public void updateRelationships(RelationshipsBetweenSPDXElements relationsActual, RelationshipsBetweenSPDXElements relationsRequest) {
        for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
            if (relationsRequest.getFieldValue(field) != null) {
                relationsActual.setFieldValue(field, relationsRequest.getFieldValue(field));
            }
        }
    }

    public Set<SnippetInformation> prepareUpdateSnippetInformations(Set<SnippetInformation> snippetActual, Set<SnippetInformation> snippetRequest) {
        if(CommonUtils.isNullOrEmptyCollection(snippetRequest)) {
            return Collections.emptySet();
        }

        if(snippetActual.size() > snippetRequest.size()) {
            return snippetRequest;
        }
        Set<Integer> indexOfSnippetActual = snippetActual.stream().map(SnippetInformation::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfSnippetRequest = snippetRequest.stream().map(SnippetInformation::getIndex).collect(Collectors.toSet());

        Set<SnippetInformation> snippetsUpdate = new HashSet<>();
        snippetsUpdate.addAll(snippetActual);
        if(snippetActual.size() < snippetRequest.size()) {

            Set<Integer> newIndexs = Sets.difference(indexOfSnippetRequest, indexOfSnippetActual);
            for(SnippetInformation snippetInformation: snippetRequest) {
                for(Integer index: newIndexs) {
                    if(snippetInformation.getIndex() == index) {
                        snippetsUpdate.add(snippetInformation);
                    }
                }
            }
        }

        for (SnippetInformation actual: snippetsUpdate){
            for (SnippetInformation request: snippetRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateSnippetInformation(actual, request);
                }
            }
        }
        return snippetsUpdate;
    }

    public void updateSnippetInformation(SnippetInformation snippetActual, SnippetInformation snippetRequest) {
        for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
            Object fieldValue = snippetRequest.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case SNIPPET_RANGES:
                        Set<SnippetRange> snippetRanges = prepareUpdateSnippetRanges(snippetActual.getSnippetRanges(), snippetRequest.getSnippetRanges());
                        snippetActual.setFieldValue(field, snippetRanges);
                        break;
                    default:
                        snippetActual.setFieldValue(field, fieldValue);
                }
            }
        }
    }

    public Set<SnippetRange> prepareUpdateSnippetRanges(Set<SnippetRange> snippetRangeActual, Set<SnippetRange> snippetRangeRequest) {
        if(CommonUtils.isNullOrEmptyCollection(snippetRangeRequest)) {
            return Collections.emptySet();
        }
        if(snippetRangeActual.size() > snippetRangeRequest.size()) {
            return snippetRangeRequest;
        }
        Set<Integer> indexOfSnippetActual = snippetRangeActual.stream().map(SnippetRange::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfSnippetRequest = snippetRangeRequest.stream().map(SnippetRange::getIndex).collect(Collectors.toSet());

        Set<SnippetRange> snippetsUpdate = new HashSet<>();

        snippetsUpdate.addAll(snippetRangeActual);
        if(snippetRangeActual.size() < snippetRangeRequest.size()) {
            Set<Integer> newIndexs= Sets.difference(indexOfSnippetRequest, indexOfSnippetActual);
            for(SnippetRange snippetRange: snippetRangeRequest) {
                for(Integer index: newIndexs) {
                    if(snippetRange.getIndex() == index) {
                        snippetsUpdate.add(snippetRange);
                    }
                }
            }
        }

        for (SnippetRange actual: snippetsUpdate){
            for (SnippetRange request: snippetRangeRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateSnippetRange(actual, request);
                }
            }
        }

        return snippetsUpdate;
    }

    public void updateSnippetRange(SnippetRange snippetRangeActual, SnippetRange snippetRangeRequest) {
        for (SnippetRange._Fields field : SnippetRange._Fields.values()) {
            Object fieldValue = snippetRangeRequest.getFieldValue(field);
            if (fieldValue != null) {
                snippetRangeActual.setFieldValue(field, fieldValue);
            }
        }
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation documentCreationInformationRequest, String spdxId, User user) throws TException {
        DocumentCreationInformationService.Iface documentClient = new ThriftClients().makeSPDXDocumentInfoClient();
        if (isNullOrEmpty(documentCreationInformationRequest.getSpdxDocumentId())) {
            documentCreationInformationRequest.setSpdxDocumentId(spdxId);
        }

        DocumentCreationInformation documentCreationInformationUpdate = prepareUpdateDocumentCreationInformation(getDocumentCreationInformationById(documentCreationInformationRequest.getId(), user), documentCreationInformationRequest);
        return documentClient.updateDocumentCreationInformation(documentCreationInformationUpdate, user);
    }

    public DocumentCreationInformation prepareUpdateDocumentCreationInformation(DocumentCreationInformation documentCreationInformationUpdate, DocumentCreationInformation documentCreationInformationRequest) {
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
            Object fieldValue = documentCreationInformationRequest.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case EXTERNAL_DOCUMENT_REFS:
                        Set<ExternalDocumentReferences> externalDocumentReferences = prepareUpdateExternalDocumentReferences(documentCreationInformationUpdate.getExternalDocumentRefs(), documentCreationInformationRequest.getExternalDocumentRefs());
                        documentCreationInformationUpdate.setFieldValue(field, externalDocumentReferences);
                        break;
                    case CREATOR:
                        Set<Creator> relationships = prepareUpdateCreators(documentCreationInformationUpdate.getCreator(), documentCreationInformationRequest.getCreator());
                        documentCreationInformationUpdate.setFieldValue(field, relationships);
                        break;
                    default:
                        documentCreationInformationUpdate.setFieldValue(field, fieldValue);
                }
            }
        }
        return documentCreationInformationUpdate;
    }

    public Set<Creator> prepareUpdateCreators(Set<Creator> creatorsActual, Set<Creator> creatorsRequest) {
        if(CommonUtils.isNullOrEmptyCollection(creatorsRequest)) {
            return Collections.emptySet();
        }
        if(creatorsActual.size() > creatorsRequest.size()) {
            return creatorsRequest;
        }
        Set<Integer> indexOfCreatorsActual = creatorsActual.stream().map(Creator::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfCreatorsRequest = creatorsRequest.stream().map(Creator::getIndex).collect(Collectors.toSet());

        Set<Creator> creatorsUpdate = new HashSet<>();
        creatorsUpdate.addAll(creatorsActual);
        if(creatorsActual.size() < creatorsRequest.size()) {

            Set<Integer> newIndexs = Sets.difference(indexOfCreatorsRequest, indexOfCreatorsActual);
            for(Creator creator: creatorsRequest) {
                for(Integer index: newIndexs) {
                    if(creator.getIndex() == index) {
                        creatorsUpdate.add(creator);
                    }
                }
            }
        }
        for (Creator actual: creatorsUpdate){
            for (Creator request: creatorsRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateCreators(actual, request);
                }
            }
        }
        return creatorsUpdate;
    }

    public Creator updateCreators(Creator creatorActual, Creator creatorRequest) {
        for (Creator._Fields field : Creator._Fields.values()) {
            Object fieldValue = creatorRequest.getFieldValue(field);
            if (fieldValue != null) {
                creatorActual.setFieldValue(field, fieldValue);
            }
        }
        return creatorActual;
    }

    public Set<ExternalDocumentReferences> prepareUpdateExternalDocumentReferences(Set<ExternalDocumentReferences> externalDocumentReferencesActual,
                                                                                   Set<ExternalDocumentReferences> externalDocumentReferencesRequest) {
        if(CommonUtils.isNullOrEmptyCollection(externalDocumentReferencesRequest)) {
            return Collections.emptySet();
        }

        if(externalDocumentReferencesActual.size() > externalDocumentReferencesRequest.size()) {
            return externalDocumentReferencesRequest;
        }
        Set<Integer> indexOfExternalDocumentReferencesActual = externalDocumentReferencesActual.stream().map(ExternalDocumentReferences::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfExternalDocumentReferencesRequest = externalDocumentReferencesRequest.stream().map(ExternalDocumentReferences::getIndex).collect(Collectors.toSet());

        Set<ExternalDocumentReferences> externalDocumentReferencesUpdate = new HashSet<>();
        externalDocumentReferencesUpdate.addAll(externalDocumentReferencesActual);
        if(externalDocumentReferencesActual.size() < externalDocumentReferencesRequest.size()) {
            Set<Integer> newIndexs = Sets.difference(indexOfExternalDocumentReferencesRequest, indexOfExternalDocumentReferencesActual);
            for(ExternalDocumentReferences externalDocumentReferences: externalDocumentReferencesRequest) {
                for(Integer index: newIndexs) {
                    if(externalDocumentReferences.getIndex() == index) {
                        externalDocumentReferencesUpdate.add(externalDocumentReferences);
                    }
                }
            }
        }

        for (ExternalDocumentReferences actual: externalDocumentReferencesUpdate){
            for (ExternalDocumentReferences request: externalDocumentReferencesRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateExternalDocumentReferences(actual, request);
                }
            }
        }
        return externalDocumentReferencesUpdate;
    }

    public ExternalDocumentReferences updateExternalDocumentReferences(ExternalDocumentReferences externalDocumentReferencesActual, ExternalDocumentReferences externalDocumentReferencesRequest) {
        for (ExternalDocumentReferences._Fields field : ExternalDocumentReferences._Fields.values()) {
            Object fieldValue = externalDocumentReferencesRequest.getFieldValue(field);
            if (fieldValue != null) {
                externalDocumentReferencesActual.setFieldValue(field, fieldValue);
            }
        }
        return externalDocumentReferencesActual;
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInformationRequest, String spdxId, User user) throws TException {
        PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
        if (isNullOrEmpty(packageInformationRequest.getSpdxDocumentId())) {
            packageInformationRequest.setSpdxDocumentId(spdxId);
        }
        PackageInformation packageInformationUpdate = prepareUpdatePackageInformation(getPackageInformationById(packageInformationRequest.getId(), user), packageInformationRequest);
        return packageClient.updatePackageInformation(packageInformationUpdate, user);
    }

    public PackageInformation prepareUpdatePackageInformation(PackageInformation packageInformationUpdate, PackageInformation packageInformationRequest) {
        for (PackageInformation._Fields field : PackageInformation._Fields.values()) {
            Object fieldValue = packageInformationRequest.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case EXTERNAL_REFS:
                        Set<ExternalReference> externalDocumentReferences = prepareExternalReferences(packageInformationUpdate.getExternalRefs(), packageInformationRequest.getExternalRefs());
                        packageInformationUpdate.setExternalRefs(externalDocumentReferences);
                        break;
                    case RELATIONSHIPS:
                        Set<RelationshipsBetweenSPDXElements> relationships = prepareUpdateRelationShips(packageInformationUpdate.getRelationships(), packageInformationRequest.getRelationships());
                        packageInformationUpdate.setRelationships(relationships);
                        break;
                    case ANNOTATIONS:
                        Set<Annotations> annotations = prepareUpdateAnnotations(packageInformationUpdate.getAnnotations(), packageInformationRequest.getAnnotations());
                        packageInformationUpdate.setAnnotations(annotations);
                        break;
                    case CHECKSUMS:
                        Set<CheckSum> checkSums = prepareUpdateCheckSums(packageInformationUpdate.getChecksums(), packageInformationRequest.getChecksums());
                        packageInformationUpdate.setChecksums(checkSums);
                        break;
                    default:
                        packageInformationUpdate.setFieldValue(field, fieldValue);
                }
            }
        }
        return packageInformationUpdate;
    }

    public Set<CheckSum> prepareUpdateCheckSums(Set<CheckSum> checkSumsActual, Set<CheckSum> checkSumsRequest) {
        if(CommonUtils.isNullOrEmptyCollection(checkSumsRequest)) {
            return Collections.emptySet();
        }
        if(checkSumsActual.size() > checkSumsRequest.size()) {
            return checkSumsRequest;
        }
        Set<Integer> indexOfCheckSumsActual = checkSumsActual.stream().map(CheckSum::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfCheckSumsRequest = checkSumsRequest.stream().map(CheckSum::getIndex).collect(Collectors.toSet());

        Set<CheckSum> checkSumsUpdate = new HashSet<>();
        checkSumsUpdate.addAll(checkSumsActual);
        if(checkSumsActual.size() < checkSumsRequest.size()) {

            Set<Integer> newIndexs = Sets.difference(indexOfCheckSumsRequest, indexOfCheckSumsActual);
            for(CheckSum checkSum: checkSumsRequest) {
                for(Integer index: newIndexs) {
                    if(checkSum.getIndex() == index) {
                        checkSumsUpdate.add(checkSum);
                    }
                }
            }
        }
        for (CheckSum actual: checkSumsUpdate){
            for (CheckSum request: checkSumsRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateChecksum(actual, request);
                }
            }
        }
        return checkSumsUpdate;
    }

    public CheckSum updateChecksum(CheckSum checkSumActual, CheckSum checkSumRequest) {
        for (CheckSum._Fields field : CheckSum._Fields.values()) {
            Object fieldValue = checkSumRequest.getFieldValue(field);
            if (fieldValue != null) {
                checkSumActual.setFieldValue(field, fieldValue);
            }
        }
        return checkSumActual;
    }


    public Set<ExternalReference> prepareExternalReferences(Set<ExternalReference> externalReferencesActual, Set<ExternalReference> externalReferencesRequest) {
        if(CommonUtils.isNullOrEmptyCollection(externalReferencesRequest)) {
            return Collections.emptySet();
        }
        if(externalReferencesActual.size() > externalReferencesRequest.size()) {
            return externalReferencesRequest;
        }

        Set<Integer> indexOfExternalReferenceActual = externalReferencesActual.stream().map(ExternalReference::getIndex).collect(Collectors.toSet());
        Set<Integer> indexOfExternalReferenceRequest = externalReferencesRequest.stream().map(ExternalReference::getIndex).collect(Collectors.toSet());

        Set<ExternalReference> externalReferencesUpdate = new HashSet<>();
        externalReferencesUpdate.addAll(externalReferencesActual);
        if(externalReferencesActual.size() < externalReferencesRequest.size()) {
            Set<Integer> newIndexs = Sets.difference(indexOfExternalReferenceRequest, indexOfExternalReferenceActual);
            for(ExternalReference externalReference: externalReferencesRequest) {
                for(Integer index: newIndexs) {
                    if(externalReference.getIndex() == index) {
                        externalReferencesUpdate.add(externalReference);
                    }
                }
            }
        }

        for (ExternalReference actual: externalReferencesUpdate){
            for (ExternalReference request: externalReferencesRequest) {
                if(request.getIndex() == actual.getIndex()) {
                    updateExternalReference(actual, request);
                }
            }
        }
        return externalReferencesUpdate;
    }

    public ExternalReference updateExternalReference(ExternalReference externalReferenceActual, ExternalReference externalReferenceRequest) {
        for (ExternalReference._Fields field : ExternalReference._Fields.values()) {
            Object fieldValue = externalReferenceRequest.getFieldValue(field);
            if (fieldValue != null) {
                externalReferenceActual.setFieldValue(field, fieldValue);
            }
        }
        return externalReferenceActual;
    }

    public RequestStatus deleteRelease(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        RequestStatus deleteStatus;

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            if (!projectService.getProjectsUsedReleaseInDependencyNetwork(releaseId).isEmpty()) {
                return RequestStatus.IN_USE;
            }
        }

        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            deleteStatus = sw360ComponentClient.deleteReleaseWithForceFlag(releaseId, sw360User, true);
        } else {
            deleteStatus = sw360ComponentClient.deleteRelease(releaseId, sw360User);
        }
        if (deleteStatus.equals(RequestStatus.SUCCESS)) {
            SW360Utils.removeReleaseVulnerabilityRelation(releaseId, sw360User);
        }
        return deleteStatus;
    }

    public Set<Project> getProjectsByRelease(String releaseId, User sw360User) throws TException {
        return projectService.getProjectsByRelease(releaseId, sw360User);
    }

    public Set<Component> getUsingComponentsForRelease(String releaseId, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getUsingComponentsForRelease(releaseId);
    }

    public List<Release> getRecentReleases(User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.getRecentReleasesWithAccessibility(sw360User);
    }

    public ExternalToolProcess fossologyProcess(String releaseId, User sw360User, String uploadDescription) throws TException {
        FossologyService.Iface sw360FossologyClient = getThriftFossologyClient();
        ExternalToolProcess fossologyProcess = null;
        try {
            fossologyProcess = sw360FossologyClient.process(releaseId, sw360User, uploadDescription);
        } catch (TException exp) {
            throw new ResourceNotFoundException("Could not determine FOSSology state for this release!");
        }
        return fossologyProcess;
    }

    private void markFossologyProcessOutdated(String releaseId, User sw360User) throws TException {
        FossologyService.Iface sw360FossologyClient = getThriftFossologyClient();
        RequestStatus markFossologyProcessOutdatedStatus = sw360FossologyClient.markFossologyProcessOutdated(releaseId,
                sw360User);
        if (markFossologyProcessOutdatedStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("Unable to mark Fossology Process Outdated. Release Id: " + releaseId);
        }
    }

    public void checkFossologyConnection() throws TException {
        FossologyService.Iface sw360FossologyClient = getThriftFossologyClient();
        RequestStatus checkConnection = null;
        try {
            checkConnection = sw360FossologyClient.checkConnection();
        } catch (TException exp) {
            throw new RuntimeException("Connection to Fossology server Failed.");
        }

        if (checkConnection == RequestStatus.FAILURE) {
            throw new RuntimeException("Connection to Fossology server Failed.");
        }
    }

    public ExternalToolProcess getExternalToolProcess(Release release) {
        Set<ExternalToolProcess> notOutdatedExternalToolProcesses = SW360Utils
                .getNotOutdatedExternalToolProcessesForTool(release, ExternalTool.FOSSOLOGY);
        ExternalToolProcess fossologyProcess = null;
        if (!notOutdatedExternalToolProcesses.isEmpty()) {
            fossologyProcess = notOutdatedExternalToolProcesses.iterator().next();
        }

        return fossologyProcess;
    }

    public boolean isFOSSologyProcessCompleted(ExternalToolProcess fossologyProcess) {
        List<ExternalToolProcessStep> processSteps = fossologyProcess.getProcessSteps();
        if (fossologyProcess.processStatus == ExternalToolProcessStatus.DONE && processSteps != null
                && processSteps.size() == 3) {
            long countOfIncompletedSteps = processSteps.stream().filter(step -> {
                String result = step.getResult();
                return step.getStepStatus() != ExternalToolProcessStatus.DONE || result == null || result.equals("-1");
            }).count();
            if (countOfIncompletedSteps == 0)
                return true;
        }

        return false;
    }

    public void executeFossologyProcess(User user, Sw360AttachmentService attachmentService,
            Map<String, ReentrantLock> mapOfLocks, String releaseId, boolean markFossologyProcessOutdated,
            String uploadDescription)
            throws TException, IOException {
        String attachmentId = validateNumberOfSrcAttachedAndGetAttachmentId(releaseId, user);

        if (markFossologyProcessOutdated) {
            log.info("Marking FOSSology process outdated for Release : " + releaseId);
            markFossologyProcessOutdated(releaseId, user);
        }

        Release release = getReleaseForUserById(releaseId, user);

        ExternalToolProcess fossologyProcess = getExternalToolProcess(release);
        if (fossologyProcess != null && isFOSSologyProcessCompleted(fossologyProcess)) {
            log.info("FOSSology process for Release : " + releaseId + " already completed.");
            return;
        }

        final ExternalToolProcess fossologyProcessFinal = fossologyProcess;
        final Function<String, ReentrantLock> locks = relId -> {
            mapOfLocks.putIfAbsent(relId, new ReentrantLock());
            return mapOfLocks.get(relId);
        };

        Runnable asyncRunnable = () -> wrapTException(() -> {
            ReentrantLock lockObj = locks.apply(releaseId);
            ScheduledExecutorService service = null;

            try {
                if (lockObj.tryLock()) {
                    service = Executors.newSingleThreadScheduledExecutor();
                    triggerUploadScanAndReportStep(attachmentService, service, fossologyProcessFinal, release, user,
                            attachmentId, uploadDescription);
                }
            } catch (Exception exp) {
                log.error(String.format("Release : %s .Error occured while triggering Fossology Process . %s",
                        new Object[] { releaseId, exp.getMessage() }));
            } finally {
                log.info("Release : " + releaseId + " .Fossology Process exited, removing lock.");
                if (service != null)
                    service.shutdownNow();
                if (lockObj.isLocked())
                    lockObj.unlock();
                mapOfLocks.remove(releaseId);
            }
        });

        Thread asyncThread = new Thread(asyncRunnable);
        asyncThread.start();
    }

    private String validateNumberOfSrcAttachedAndGetAttachmentId(String releaseId, User user) throws TException {
        Release release = getReleaseForUserById(releaseId, user);
        Set<Attachment> attachments = release.getAttachments();

        if (attachments == null || attachments.isEmpty()) {
            throw new HttpMessageNotReadableException(String.format(RELEASE_ATTACHMENT_ERRORMSG, 0));
        }

        List<Attachment> listOfSources = attachments.parallelStream()
                .filter(attachment -> attachment.getAttachmentType() == AttachmentType.SOURCE)
                .collect(Collectors.toList());
        int noOfSrcAttached = listOfSources.size();

        if (noOfSrcAttached != 1) {
            throw new HttpMessageNotReadableException(String.format(RELEASE_ATTACHMENT_ERRORMSG, noOfSrcAttached));
        }

        return listOfSources.get(0).getAttachmentContentId();
    }

    private int getAttachmentSizeInMB(Sw360AttachmentService attachmentService, String attachmentId, Release release,
            User user) throws TException {
        int attachmentSizeinBytes = 0;
        try (ByteArrayOutputStream attachmentOutputStream = new ByteArrayOutputStream();
                InputStream streamToAttachments = attachmentService.getStreamToAttachments(
                        Collections.singleton(attachmentService.getAttachmentContent(attachmentId)), user, release)) {
            attachmentSizeinBytes = FileCopyUtils.copy(streamToAttachments, attachmentOutputStream);
        } catch (IOException exp) {
            log.error("Release : " + release.getId()
                    + " .Error occured while calculation attachment size.Attachment ID : " + attachmentId);
        }

        return (attachmentSizeinBytes / 1024) / 1024;
    }

    private void triggerUploadScanAndReportStep(Sw360AttachmentService attachmentService,
            ScheduledExecutorService service, ExternalToolProcess fossologyProcess, Release release, User user,
            String attachmentId, String uploadDescription) throws TException {

        int scanTriggerRetriesCount = 0, reportGenerateTriggerRetries = 0, reportGeneratestatusCheckCount = 0,
                maxRetries = 15;
        ScheduledFuture<ExternalToolProcess> future = null;
        String releaseId = release.getId();
        ExternalToolProcess fossologyProcessLocal = fossologyProcess;

        int attachmentSizeinMB = getAttachmentSizeInMB(attachmentService, attachmentId, release, user);

        int timeIntervalToCheckUnpackScanStatus = attachmentSizeinMB <= 5 ? 10 : 2 * attachmentSizeinMB;

        log.info(String.format(
                "Release : %s .Size of source is %s MB, Time interval to check scan and unpack status %s sec",
                new Object[] { releaseId, attachmentSizeinMB, timeIntervalToCheckUnpackScanStatus }));

        Callable<ExternalToolProcess> processRunnable = new Callable<ExternalToolProcess>() {
            public ExternalToolProcess call() throws Exception {
                return fossologyProcess(releaseId, user, uploadDescription);
            }
        };

        if (fossologyProcessLocal == null || !isUploadStepCompletedSuccessfully(fossologyProcessLocal, releaseId)) {
            log.info("Release : " + releaseId + " .Triggering Upload Step.");
            fossologyProcessLocal = fossologyProcess(releaseId, user, uploadDescription);
        }

        if (isUploadStepCompletedSuccessfully(fossologyProcessLocal, releaseId)
                && isUnpackSuccessFull(service, fossologyProcessLocal.getProcessSteps().get(0).getResult(),
                        timeIntervalToCheckUnpackScanStatus, releaseId)) {

            if (!isScanStepInCompletedSuccessfully(fossologyProcessLocal, releaseId)) {
                while (++scanTriggerRetriesCount < maxRetries
                        && !isScanTriggerSuccessfull(fossologyProcessLocal, releaseId)) {
                    log.info("Release : " + releaseId + " .Triggering Scan Step.");
                    future = service.schedule(processRunnable, 5, TimeUnit.SECONDS);
                    fossologyProcessLocal = getFutureResult(future);
                }
            }

            if (isScanTriggerSuccessfull(fossologyProcessLocal, releaseId) && isScanSuccessFull(service,
                    fossologyProcessLocal.getProcessSteps().get(1).getProcessStepIdInTool(),
                    timeIntervalToCheckUnpackScanStatus, releaseId)) {

                while (++reportGenerateTriggerRetries < maxRetries
                        && !isReportTriggerSuccessfull(fossologyProcessLocal, releaseId)) {
                    log.info("Release : " + releaseId + " .Triggering Report Step.");
                    future = service.schedule(processRunnable, 5, TimeUnit.SECONDS);
                    fossologyProcessLocal = getFutureResult(future);
                }
            }

            if (isReportTriggerSuccessfull(fossologyProcessLocal, releaseId)) {
                do {
                    log.info("Release : " + releaseId + " .Triggering Report Generation and attach to Release.");
                    future = service.schedule(processRunnable, 10, TimeUnit.SECONDS);
                    fossologyProcessLocal = getFutureResult(future);
                } while (++reportGeneratestatusCheckCount < maxRetries
                        && isReportGenerationInProgress(fossologyProcessLocal, releaseId));
            }
        }
    }

    private boolean isScanTriggerSuccessfull(ExternalToolProcess fossologyProcessLocal, String releaseId) {
        List<ExternalToolProcessStep> processSteps = fossologyProcessLocal.getProcessSteps();
        if (processSteps == null || processSteps.size() < 2) {
            log.warn("Release : " + releaseId + " .Scan Trigger not started! Retry...");
            return false;
        }
        ExternalToolProcessStep externalToolProcessStep = processSteps.get(1);
        String scanProcessStepIdInTool = externalToolProcessStep.getProcessStepIdInTool();
        String result = externalToolProcessStep.getResult();
        boolean status = scanProcessStepIdInTool != null && !scanProcessStepIdInTool.equals("-1");
        if (status) {
            log.info("Release : " + releaseId + " .Scan Trigger successful.");
        } else {
            log.warn("Release : " + releaseId + " .Scan Trigger failed! Retry...");
        }

        return status;
    }

    private boolean isReportTriggerSuccessfull(ExternalToolProcess fossologyProcessLocal, String releaseId) {
        List<ExternalToolProcessStep> processSteps = fossologyProcessLocal.getProcessSteps();
        if (processSteps == null || processSteps.size() < 3) {
            log.warn("Release : " + releaseId + " .Report Trigger not started! Retry...");
            return false;
        }
        ExternalToolProcessStep externalToolProcessStep = processSteps.get(2);
        String scanProcessStepIdInTool = externalToolProcessStep.getProcessStepIdInTool();
        String result = externalToolProcessStep.getResult();
        boolean status = scanProcessStepIdInTool != null && !scanProcessStepIdInTool.equals("-1");

        if (status) {
            log.info("Release : " + releaseId + " .Report Trigger is successfull.");
        } else {
            log.warn("Release : " + releaseId + " .Report Trigger is failed.Retry..");
        }
        return status;
    }

    private boolean isUploadStepCompletedSuccessfully(ExternalToolProcess fossologyProcess, String releaseId) {
        List<ExternalToolProcessStep> processSteps = fossologyProcess.getProcessSteps();
        if (processSteps == null || processSteps.size() < 1) {
            log.warn("Release : " + releaseId + " .Upload Step is not started.");
            return false;
        }
        ExternalToolProcessStep externalToolProcessStep = processSteps.get(0);
        String result = externalToolProcessStep.getResult();
        boolean status = result != null && !result.equals("-1")
                && externalToolProcessStep.getStepStatus() == ExternalToolProcessStatus.DONE;

        if (status) {
            log.info("Release : " + releaseId + " .Upload Step is complete.");
        } else {
            log.warn("Release : " + releaseId + " .Upload Step not completed.");
        }
        return status;
    }

    private boolean isScanStepInCompletedSuccessfully(ExternalToolProcess fossologyProcess, String releaseId) {
        List<ExternalToolProcessStep> processSteps = fossologyProcess.getProcessSteps();
        if (processSteps == null || processSteps.size() < 2) {
            log.warn("Release : " + releaseId + " .Scan Step is not started.");
            return false;
        }
        ExternalToolProcessStep externalToolProcessStep = processSteps.get(1);
        String scanProcessStepIdInTool = externalToolProcessStep.getProcessStepIdInTool();
        String result = externalToolProcessStep.getResult();
        boolean status = result != null && !result.equals("-1") && scanProcessStepIdInTool != null
                && !scanProcessStepIdInTool.equals("-1")
                && externalToolProcessStep.getStepStatus() == ExternalToolProcessStatus.DONE;

        if (status) {
            log.info("Release : " + releaseId + " .Scan Step is complete.");
        } else {
            log.warn("Release : " + releaseId + " .Scan Step not completed.");
        }

        return status;
    }

    private boolean isReportGenerationInProgress(ExternalToolProcess fossologyProcess, String releaseId) {
        List<ExternalToolProcessStep> processSteps = fossologyProcess.getProcessSteps();
        if (processSteps == null || processSteps.size() < 3) {
            log.info("Release : " + releaseId + " .Report Generation is not started.");
            return false;
        }
        ExternalToolProcessStep externalToolProcessStep = processSteps.get(2);
        String reportProcessStepIdInTool = externalToolProcessStep.getProcessStepIdInTool();
        String scanProcessStepIdInTool = externalToolProcessStep.getProcessStepIdInTool();
        String result = externalToolProcessStep.getResult();
        boolean status = result == null && scanProcessStepIdInTool != null && !scanProcessStepIdInTool.equals("-1")
                && externalToolProcessStep.getStepStatus() != ExternalToolProcessStatus.DONE;

        if (status) {
            log.info("Release : " + releaseId + " .Report Generation is in progess.");
        } else {
            log.warn("Release : " + releaseId + " .Report Generation is over.");
        }
        return status;
    }

    private boolean isUnpackSuccessFull(ScheduledExecutorService service, String uploadId, int timeInterval,
            String releaseId) throws TException {
        int unpackStatusCheckCount = 0, maxRetries = 15;
        ScheduledFuture<RequestStatus> future = null;
        FossologyService.Iface sw360FossologyClient = getThriftFossologyClient();

        Callable<RequestStatus> unpackStatusRunnable = new Callable<RequestStatus>() {
            public RequestStatus call() throws Exception {
                return checkUnpackCompletedSuccessfully(sw360FossologyClient, uploadId, releaseId);
            }
        };

        RequestStatus unpackStatus = RequestStatus.FAILURE;
        do {
            future = service.schedule(unpackStatusRunnable, timeInterval, TimeUnit.SECONDS);
            unpackStatus = getFutureResult(future);
            log.info(String.format("Release : %s .Unpack Status : %s , timeinterval : %s sec",
                    new Object[] { releaseId, unpackStatus.name(), timeInterval }));
        } while (++unpackStatusCheckCount < maxRetries && unpackStatus == RequestStatus.PROCESSING);

        return unpackStatus == RequestStatus.SUCCESS;
    }

    private boolean isScanSuccessFull(ScheduledExecutorService service, String scanJobId, int timeInterval,
            String releaseId) throws TException {
        int scanStatusCheckCount = 0, maxRetries = 15;
        ScheduledFuture<Object[]> future = null;
        FossologyService.Iface sw360FossologyClient = getThriftFossologyClient();

        Callable<Object[]> scanStatusRunnable = new Callable<Object[]>() {
            public Object[] call() throws Exception {
                return checkScanCompletedSuccessfully(sw360FossologyClient, scanJobId, releaseId);
            }
        };

        RequestStatus scanStatus = RequestStatus.FAILURE;
        do {
            future = service.schedule(scanStatusRunnable, timeInterval, TimeUnit.SECONDS);
            Object[] scanStatusWithETA = getFutureResult(future);
            scanStatus = (RequestStatus) scanStatusWithETA[0];
            Object eta = scanStatusWithETA[1];

            timeInterval = (eta == null || eta.toString().isEmpty() || Integer.parseInt(eta.toString()) == 0)
                    ? timeInterval
                    : Integer.parseInt(eta.toString());
            log.info(String.format("Release : %s .Scan Status : %s , timeinterval : %s",
                    new Object[] { releaseId, scanStatus, timeInterval }));
        } while (++scanStatusCheckCount < maxRetries && scanStatus == RequestStatus.PROCESSING);

        return scanStatus == RequestStatus.SUCCESS;
    }

    private <T> T getFutureResult(ScheduledFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException exp) {
            throw new RuntimeException("Execution of Fossology Process failed:" + exp.getMessage());
        }
    }

    private RequestStatus checkUnpackCompletedSuccessfully(FossologyService.Iface sw360FossologyClient, String uploadId,
            String releaseId) throws TException {
        log.info("Release : " + releaseId + " .Checking unpack status. uploadId = " + uploadId);
        Map<String, String> checkUnpackStatus = sw360FossologyClient.checkUnpackStatus(Integer.parseInt(uploadId));
        String status = checkUnpackStatus.get("status");
        if (status == null || status.equalsIgnoreCase(RESPONSE_STATUS_VALUE_FAILED)) {
            return RequestStatus.FAILURE;
        } else if (status.equalsIgnoreCase(RESPONSE_STATUS_VALUE_COMPLETED)) {
            return RequestStatus.SUCCESS;
        }

        return RequestStatus.PROCESSING;
    }

    private Object[] checkScanCompletedSuccessfully(FossologyService.Iface sw360FossologyClient, String scanJobId,
            String releaseId) throws TException {
        log.info("Release : " + releaseId + " .Checking scan status.scanJobId =" + scanJobId);
        Map<String, String> checkUnpackStatus = sw360FossologyClient.checkScanStatus(Integer.parseInt(scanJobId));
        String status = checkUnpackStatus.get("status");
        String eta = checkUnpackStatus.get("eta");
        log.info(String.format("Release : %s .status: %s, eta from response= %s ",
                new Object[] { releaseId, status, eta }));

        if (status == null || status.equalsIgnoreCase(RESPONSE_STATUS_VALUE_FAILED)) {
            return new Object[] { RequestStatus.FAILURE, eta };
        } else if (status.equalsIgnoreCase(RESPONSE_STATUS_VALUE_COMPLETED)) {
            return new Object[] { RequestStatus.SUCCESS, eta };
        }

        return new Object[] { RequestStatus.PROCESSING, eta };
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();
        return componentClient;
    }

    private SPDXDocumentService.Iface getThriftSPDXDocumentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxdocument/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new SPDXDocumentService.Client(protocol);
    }

    private DocumentCreationInformationService.Iface getThriftDocumentCreationInformation() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxdocumentcreationinfo/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new DocumentCreationInformationService.Client(protocol);
    }

    private PackageInformationService.Iface getThriftIPackageInformation() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxpackageinfo/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new PackageInformationService.Client(protocol);
    }

    private FossologyService.Iface getThriftFossologyClient() throws TTransportException {
        if (fossologyClient == null) {
            THttpClient thriftClient = new THttpClient(thriftServerUrl + "/fossology/thrift");
            TProtocol protocol = new TCompactProtocol(thriftClient);
            fossologyClient = new FossologyService.Client(protocol);
        }

        return fossologyClient;
    }

    /**
     * Re-generate Fossology report for release
     * @param releaseId                Id of Release need to re-generate report
     * @param user                     Request User
     * @return RequestStatus
     * @throws TException
     */
    public RequestStatus triggerReportGenerationFossology(String releaseId, User user) throws TException {
        FossologyService.Iface fossologyClient = getThriftFossologyClient();
        return fossologyClient.triggerReportGenerationFossology(releaseId, user);
    }

    /**
     * Count the number of projects are using the release that has releaseId
     * @param releaseId              Id of release
     * @return int                    Number of project
     * @throws TException
     */
    public int countProjectsByReleaseId(String releaseId) {
        return projectService.countProjectsByReleaseIds(Collections.singleton(releaseId));
    }

    /*
     * Use lucene search for searching releases based on name
     */
    public List<Release> refineSearch(String searchText, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.searchAccessibleReleases(searchText, sw360User);
    }

    public void addEmbeddedLinkedRelease(Release sw360Release, User sw360User, HalResource<ReleaseLink> releaseResource, Set<String> releaseIdsInBranch) {
        releaseIdsInBranch.add(sw360Release.getId());
        Map<String, ReleaseRelationship> releaseIdToRelationship = sw360Release.getReleaseIdToRelationship();
        if (releaseIdToRelationship != null) {
            releaseIdToRelationship.forEach((key, value) -> wrapTException(() -> {
                if (releaseIdsInBranch.contains(key)) {
                    return;
                }

                Release linkedRelease = getReleaseForUserById(key, sw360User);
                ReleaseLink embeddedLinkedRelease = convertToEmbeddedLinkedRelease(linkedRelease, sw360User, value);
                HalResource<ReleaseLink> halLinkedRelease = new HalResource<>(embeddedLinkedRelease);
                addEmbeddedLinkedRelease(linkedRelease, sw360User, halLinkedRelease, releaseIdsInBranch);
                releaseResource.addEmbeddedResource("sw360:releaseLinks", halLinkedRelease);
            }));
        }
        releaseIdsInBranch.remove(sw360Release.getId());
    }

    public ReleaseLink convertToEmbeddedLinkedRelease(Release release, User sw360User, ReleaseRelationship relationship) throws TException {
        ReleaseLink releaseLink = rch.convertToReleaseLink(release, relationship);
        releaseLink.setAccessible(isReleaseActionAllowed(release, sw360User, RequestedAction.READ));
        return releaseLink;
    }

    public boolean isReleaseActionAllowed(Release release, User sw360User, RequestedAction action) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        return sw360ComponentClient.isReleaseActionAllowed(release, sw360User, action);
    }

    public String checkForCyclicLinkedReleases(Release parentRelease, Release linkedRelease, User sw360User) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        Map<String, ReleaseRelationship> releaseRelationshipMap = new HashMap<>();
        releaseRelationshipMap.put(linkedRelease.getId(), ReleaseRelationship.CONTAINED);
        parentRelease.setReleaseIdToRelationship(releaseRelationshipMap);
        return sw360ComponentClient.getCyclicLinkedReleasePath(parentRelease, sw360User);
    }

    /**
     * Subscribe release
     * @param releaseId              Release ID
     * @throws TException            TException
     */
    public void subscribeRelease(User user, String releaseId) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        sw360ComponentClient.subscribeRelease(releaseId, user);
    }

    /**
     * Unsubscribe release
     * @param releaseId              Release ID
     * @throws TException            TException
     */
    public void unsubscribeRelease(User user, String releaseId) throws TException {
        ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
        sw360ComponentClient.unsubscribeRelease(releaseId, user);
    }

    /**
     * Checks the status of a Fossology report generation process
     * 
     * @param reportId the ID of the report to check
     * @return a map containing status information about the report
     */
    public Map<String, String> checkFossologyReportStatus(int reportId) throws TException {
        FossologyService.Iface fossologyClient = getThriftFossologyClient();
        return fossologyClient.checkReportGenerationStatus(reportId);
    }
}
