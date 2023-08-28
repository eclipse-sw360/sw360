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

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Predicate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.VerificationState;
import org.eclipse.sw360.datahandler.thrift.VerificationStateInfo;
import org.eclipse.sw360.datahandler.thrift.RestrictedResource;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentDTO;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelationDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityState;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.packages.PackageController;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String RELEASES_URL = "/releases";
    private static final Logger log = LogManager.getLogger(ReleaseController.class);
    private static final Map<String, ReentrantLock> mapOfLocks = new HashMap<String, ReentrantLock>();
    private static final ImmutableMap<Release._Fields,String> mapOfFieldsTobeEmbedded = ImmutableMap.of(
            Release._Fields.MODERATORS, "sw360:moderators",
            Release._Fields.ATTACHMENTS, "sw360:attachments",
            Release._Fields.COTS_DETAILS, "sw360:cotsDetails",
            Release._Fields.RELEASE_ID_TO_RELATIONSHIP,"sw360:releaseIdToRelationship",
            Release._Fields.CLEARING_INFORMATION, "sw360:clearingInformation");
    private static final ImmutableMap<Release._Fields, String[]> mapOfBackwardCompatible_Field_OldFieldNames_NewFieldNames = ImmutableMap.<Release._Fields, String[]>builder()
            .put(Release._Fields.SOURCE_CODE_DOWNLOADURL, new String[] { "downloadurl", "sourceCodeDownloadurl" })
            .build();
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private SW360PackageService packageService;

    @NonNull
    private final Sw360VulnerabilityService vulnerabilityService;

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private Sw360AttachmentService attachmentService;

    @NonNull
    private RestControllerHelper restControllerHelper;

    @NonNull
    private Sw360LicenseInfoService sw360LicenseInfoService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @GetMapping(value = RELEASES_URL)
    public ResponseEntity<CollectionModel<EntityModel>> getReleasesForUser(
            Pageable pageable,
            @RequestParam(value = "sha1", required = false) String sha1,
            @RequestParam(value = "fields", required = false) List<String> fields,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "allDetails", required = false) boolean allDetails, HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = new ArrayList<>();

        if (sha1 != null && !sha1.isEmpty()) {
            sw360Releases.addAll(searchReleasesBySha1(sha1, sw360User));
        } else {
            sw360Releases.addAll(releaseService.getReleasesForUser(sw360User));
        }

        for(Release release: sw360Releases) {
            releaseService.setComponentDependentFieldsInRelease(release, sw360User);
        }

        sw360Releases = sw360Releases.stream()
                .filter(release -> name == null || name.isEmpty() || release.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
        if (allDetails) {
            for (Release release: sw360Releases) {
                if(!CommonUtils.isNullEmptyOrWhitespace(release.getVendorId())) {
                    release.setVendor(vendorService.getVendorById(release.getVendorId()));
                }
            }
        }
        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable, sw360Releases, SW360Constants.TYPE_RELEASE);

        List<EntityModel> releaseResources = new ArrayList<>();
        for (Release sw360Release : paginationResult.getResources()) {
            EntityModel<Release> releaseResource = null;
            if (!allDetails) {
                Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(sw360Release, fields);
                releaseResource = EntityModel.of(embeddedRelease);
            } else {
                releaseResource = createHalReleaseResourceWithAllDetails(sw360Release);
            }

            releaseResources.add(releaseResource);
        }

        CollectionModel resources = null;
        if (CommonUtils.isNotEmpty(releaseResources)) {
            resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    private List<Release> searchReleasesBySha1(String sha1, User sw360User) throws TException {
        List<AttachmentInfo> attachmentInfos = attachmentService.getAttachmentsBySha1(sha1);
        List<Release> releases = new ArrayList<>();
        for (AttachmentInfo attachmentInfo : attachmentInfos) {
            if (attachmentInfo.getOwner().isSetReleaseId()) {
                releases.add(releaseService.getReleaseForUserById(attachmentInfo.getOwner().getReleaseId(), sw360User));
            }
        }
        return releases;
    }

    @GetMapping(value = RELEASES_URL + "/{id}")
    public ResponseEntity<EntityModel> getRelease(
            @PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        HalResource halRelease = createHalReleaseResource(sw360Release, true);
        restControllerHelper.addEmbeddedDataToHalResourceRelease(halRelease, sw360Release);
        List<ReleaseLink> linkedReleaseRelations = releaseService.getLinkedReleaseRelations(sw360Release, sw360User);
        if (linkedReleaseRelations != null) {
            restControllerHelper.addEmbeddedReleaseLinks(halRelease, linkedReleaseRelations);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @GetMapping(value = RELEASES_URL + "/recentReleases")
    public ResponseEntity<CollectionModel<EntityModel>> getRecentRelease() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = releaseService.getRecentReleases(sw360User);

        List<EntityModel> resources = new ArrayList<>();
        sw360Releases.forEach(r -> {
            Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(r);
            resources.add(EntityModel.of(embeddedRelease));
        });

        CollectionModel<EntityModel> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @GetMapping(value = RELEASES_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<VulnerabilityDTO>> getVulnerabilitiesOfReleases(
            @PathVariable("id") String id) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        final List<VulnerabilityDTO> allVulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByReleaseId(id, user);
        CollectionModel<VulnerabilityDTO> resources = CollectionModel.of(allVulnerabilityDTOs);
        return new ResponseEntity<>(resources,HttpStatus.OK);
    }

    @GetMapping(value = RELEASES_URL + "/mySubscriptions")
    public ResponseEntity<CollectionModel<EntityModel>> getReleaseSubscription() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = releaseService.getReleaseSubscriptions(sw360User);

        List<EntityModel> resources = new ArrayList<>();
        sw360Releases.forEach(c -> {
            Release embeddedComponent = restControllerHelper.convertToEmbeddedRelease(c);
            resources.add(EntityModel.of(embeddedComponent));
        });

        CollectionModel<EntityModel> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @RequestMapping(value = RELEASES_URL + "/usedBy" + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel>> getUsedByResourceDetails(@PathVariable("id") String id)
            throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication(); // Project
        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> sw360Projects = releaseService.getProjectsByRelease(id, user);
        Set<org.eclipse.sw360.datahandler.thrift.components.Component> sw360Components = releaseService.getUsingComponentsForRelease(id, user);

        List<EntityModel> resources = new ArrayList<>();
        sw360Projects.forEach(p -> {
            Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
            resources.add(EntityModel.of(embeddedProject));
        });

        sw360Components.forEach(c -> {
                    Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c);
                    resources.add(EntityModel.of(embeddedComponent));
                });

        RestrictedResource restrictedResource = new RestrictedResource();
        restrictedResource.setProjects(releaseService.countProjectsByReleaseId(id) - sw360Projects.size());
        resources.add(EntityModel.of(restrictedResource));

        CollectionModel<EntityModel> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @GetMapping(value = RELEASES_URL + "/searchByExternalIds")
    public ResponseEntity searchByExternalIds(HttpServletRequest request) throws TException {
        String queryString = request.getQueryString();
        MultiValueMap<String, String> externalIdsMultiMap = parseQueryString(queryString);
        return restControllerHelper.searchByExternalIds(new LinkedMultiValueMap<String, String>(externalIdsMultiMap), releaseService, null);
    }

    private MultiValueMap<String, String> parseQueryString(String queryString) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        if (queryString != null && !queryString.isEmpty()) {
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    parameters.add(key, value);
                }
            }
        }

        return parameters;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @DeleteMapping(value = RELEASES_URL + "/{ids}")
    public ResponseEntity<List<MultiStatus>> deleteReleases(
            @PathVariable("ids") List<String> idsToDelete) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<MultiStatus> results = new ArrayList<>();
        for(String id:idsToDelete) {
            RequestStatus requestStatus = releaseService.deleteRelease(id, user);
            if(requestStatus == RequestStatus.SUCCESS) {
                results.add(new MultiStatus(id, HttpStatus.OK));
            } else if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
                results.add(new MultiStatus(id, HttpStatus.ACCEPTED));
            } else if (requestStatus == RequestStatus.IN_USE) {
                results.add(new MultiStatus(id, HttpStatus.CONFLICT));
            } else {
                results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
            }
        }
        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = RELEASES_URL + "/{id}")
    public ResponseEntity<EntityModel<Release>> patchRelease(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> reqBodyMap) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, user);
        Release updateRelease = setBackwardCompatibleFieldsInRelease(reqBodyMap);
        updateRelease.setClearingState(sw360Release.getClearingState());
        sw360Release = this.restControllerHelper.updateRelease(sw360Release, updateRelease);
        releaseService.setComponentNameAsReleaseName(sw360Release, user);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, user);
        HalResource<Release> halRelease = createHalReleaseResource(sw360Release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = RELEASES_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> patchReleaseVulnerabilityRelation(@PathVariable("id") String releaseId,
                                                      @RequestBody VulnerabilityState vulnerabilityState) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        if(CommonUtils.isNullOrEmptyCollection(vulnerabilityState.getReleaseVulnerabilityRelationDTOs())) {
            throw new HttpMessageNotReadableException("Required field ReleaseVulnerabilityRelation is not present");
        }
        if(vulnerabilityState.getVerificationState() == null) {
            throw new HttpMessageNotReadableException("Required field verificationState is not present");
        }
        List<VulnerabilityDTO> actualVDto = vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, user);
        Set<String> externalIdsFromRequestDto = vulnerabilityState.getReleaseVulnerabilityRelationDTOs().stream().map(ReleaseVulnerabilityRelationDTO::getExternalId).collect(Collectors.toSet());
        List<VulnerabilityDTO> actualVDtoFromRequest = vulnerabilityService.getVulnerabilityDTOByExternalId(externalIdsFromRequestDto, releaseId);
        Set<String> actualExternalId = actualVDto.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> commonExtIds = Sets.intersection(actualExternalId, externalIdsFromRequestDto);
        if(CommonUtils.isNullOrEmptyCollection(commonExtIds) || commonExtIds.size() != externalIdsFromRequestDto.size()) {
            throw new HttpMessageNotReadableException("External ID is not valid");
        }

        Map<String, ReleaseVulnerabilityRelation> releasemap = new HashMap<>();
        actualVDtoFromRequest.forEach(vulnerabilityDTO -> {
            releasemap.put(vulnerabilityDTO.getExternalId(),vulnerabilityDTO.getReleaseVulnerabilityRelation());
        });
        RequestStatus requestStatus = null;
        for (Map.Entry<String, ReleaseVulnerabilityRelation> entry : releasemap.entrySet()) {
            requestStatus = updateReleaseVulnerabilityRelation(releaseId, user, vulnerabilityState.getComment(), vulnerabilityState.getVerificationState(), entry.getKey());
            if (requestStatus != RequestStatus.SUCCESS) {
                break;
            }
        }
        if (requestStatus == RequestStatus.ACCESS_DENIED){
            throw new HttpMessageNotReadableException("User not allowed!");
        }
        List<VulnerabilityDTO> vulnerabilityDTOList = getVulnerabilityUpdated(externalIdsFromRequestDto, releaseId);
        final List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = new ArrayList<>();
        vulnerabilityDTOList.forEach(dto->{
            final EntityModel<VulnerabilityDTO> vulnerabilityDTOEntityModel = EntityModel.of(dto);
            vulnerabilityResources.add(vulnerabilityDTOEntityModel);
        });
        CollectionModel<EntityModel<VulnerabilityDTO>> resources = null;
        resources = restControllerHelper.createResources(vulnerabilityResources);
        HttpStatus status = resources == null ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    public RequestStatus updateReleaseVulnerabilityRelation(String releaseId, User user, String comment, VerificationState verificationState, String externalIdRequest) throws TException {
        List<VulnerabilityDTO> vulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, user);
        ReleaseVulnerabilityRelation releaseVulnerabilityRelation = new ReleaseVulnerabilityRelation();
        for (VulnerabilityDTO vulnerabilityDTO: vulnerabilityDTOs) {
            if (vulnerabilityDTO.getExternalId().equals(externalIdRequest)) {
                releaseVulnerabilityRelation = vulnerabilityDTO.getReleaseVulnerabilityRelation();
            }
        }
        ReleaseVulnerabilityRelation relation = updateReleaseVulnerabilityRelationFromRequest(releaseVulnerabilityRelation, comment, verificationState, user);
        return vulnerabilityService.updateReleaseVulnerabilityRelation(relation,user);
    }

    public static ReleaseVulnerabilityRelation updateReleaseVulnerabilityRelationFromRequest(ReleaseVulnerabilityRelation dbRelation, String comment, VerificationState verificationState, User user) {
        if (!dbRelation.isSetVerificationStateInfo()) {
            dbRelation.setVerificationStateInfo(new ArrayList<>());
        }
        VerificationStateInfo verificationStateInfo = new VerificationStateInfo();
        List<VerificationStateInfo> verificationStateHistory = dbRelation.getVerificationStateInfo();

        verificationStateInfo.setCheckedBy(user.getEmail());
        verificationStateInfo.setCheckedOn(SW360Utils.getCreatedOn());
        verificationStateInfo.setVerificationState(verificationState);
        verificationStateInfo.setComment(comment);

        verificationStateHistory.add(verificationStateInfo);
        dbRelation.setVerificationStateInfo(verificationStateHistory);
        return dbRelation;
    }

    public List<VulnerabilityDTO> getVulnerabilityUpdated(Set<String> externalIds, String releaseIds) {
        return vulnerabilityService.getVulnerabilityDTOByExternalId(externalIds, releaseIds);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = RELEASES_URL)
    public ResponseEntity<EntityModel<Release>> createRelease(
            @RequestBody Map<String, Object> reqBodyMap) throws URISyntaxException, TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = setBackwardCompatibleFieldsInRelease(reqBodyMap);
        if (release.isSetComponentId()) {
            URI componentURI = new URI(release.getComponentId());
            String path = componentURI.getPath();
            String componentId = path.substring(path.lastIndexOf('/') + 1);
            release.setComponentId(componentId);
        }
        if (release.isSetVendorId()) {
            URI vendorURI = new URI(release.getVendorId());
            String path = vendorURI.getPath();
            String vendorId = path.substring(path.lastIndexOf('/') + 1);
            release.setVendorId(vendorId);
        }

        if (release.getMainLicenseIds() != null) {
            Set<String> mainLicenseIds = new HashSet<>();
            Set<String> mainLicenseUris = release.getMainLicenseIds();
            for (String licenseURIString : mainLicenseUris.toArray(new String[mainLicenseUris.size()])) {
                URI licenseURI = new URI(licenseURIString);
                String path = licenseURI.getPath();
                String licenseId = path.substring(path.lastIndexOf('/') + 1);
                mainLicenseIds.add(licenseId);
            }
            release.setMainLicenseIds(mainLicenseIds);
        }

        release.unsetClearingState();
        Release sw360Release = releaseService.createRelease(release, sw360User);
        HalResource<Release> halResource = createHalReleaseResource(sw360Release, true);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Release.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @GetMapping(value = RELEASES_URL + "/{id}/attachments")
    public ResponseEntity<CollectionModel<EntityModel<AttachmentDTO>>> getReleaseAttachment1s(
            @PathVariable("id") String id) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        final CollectionModel<EntityModel<AttachmentDTO>> resources = attachmentService.getAttachmentDTOResourcesFromList(sw360User, sw360Release.getAttachments(), Source.releaseId(sw360Release.getId()));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @GetMapping(value = RELEASES_URL + "/{releaseId}/attachments/download", produces="application/zip")
    public void downloadAttachmentBundleFromRelease(
            @PathVariable("releaseId") String releaseId,
            HttpServletResponse response) throws TException, IOException {
        final User user = restControllerHelper.getSw360UserFromAuthentication();
        final Release release = releaseService.getReleaseForUserById(releaseId, user);
        final Set<Attachment> attachments = release.getAttachments();
        attachmentService.downloadAttachmentBundleWithContext(release, attachments, user, response);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = RELEASES_URL + "/{id}/attachment/{attachmentId}")
    public ResponseEntity<EntityModel<Attachment>> patchReleaseAttachmentInfo(@PathVariable("id") String id,
            @PathVariable("attachmentId") String attachmentId, @RequestBody Attachment attachmentData)
            throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        Set<Attachment> attachments = sw360Release.getAttachments();
        Attachment updatedAttachment = attachmentService.updateAttachment(attachments, attachmentData, attachmentId, sw360User);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, sw360User);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<Attachment> attachmentResource = EntityModel.of(updatedAttachment);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @PostMapping(value = RELEASES_URL + "/{releaseId}/attachments", consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToRelease(@PathVariable("releaseId") String releaseId,
                                                              @RequestPart("file") MultipartFile file,
                                                              @RequestPart("attachment") Attachment newAttachment) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
        Attachment attachment = null;
        try {
            attachment = attachmentService.uploadAttachment(file, newAttachment, sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        release.addToAttachments(attachment);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(release, sw360User);
        HalResource halRelease = createHalReleaseResource(release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @GetMapping(value = RELEASES_URL + "/{releaseId}/attachments/{attachmentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromRelease(
            @PathVariable("releaseId") String releaseId,
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
        attachmentService.downloadAttachmentWithContext(release, attachmentId, response, sw360User);
    }

    @DeleteMapping(RELEASES_URL + "/{releaseId}/attachments/{attachmentIds}")
    public ResponseEntity<HalResource<Release>> deleteAttachmentsFromRelease(
            @PathVariable("releaseId") String releaseId,
            @PathVariable("attachmentIds") List<String> attachmentIds) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, user);

        Set<Attachment> attachmentsToDelete = attachmentService.filterAttachmentsToRemove(Source.releaseId(releaseId),
                release.getAttachments(), attachmentIds);
        if (attachmentsToDelete.isEmpty()) {
            // let the whole action fail if nothing can be deleted
            throw new RuntimeException("Could not delete attachments " + attachmentIds + " from release " + releaseId);
        }
        log.debug("Deleting the following attachments from release " + releaseId + ": " + attachmentsToDelete);
        release.getAttachments().removeAll(attachmentsToDelete);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(release, user);
        HalResource<Release> halRelease = createHalReleaseResource(release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/{id}/checkFossologyProcessStatus", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> checkFossologyProcessStatus(@PathVariable("id") String releaseId)
            throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, user);
        Map<String, Object> responseMap = new HashMap<>();
        ExternalToolProcess fossologyProcess = releaseService.getExternalToolProcess(release);
        ReentrantLock lock = mapOfLocks.get(releaseId);
        if (lock != null && lock.isLocked()) {
            responseMap.put("status", RequestStatus.PROCESSING);
        } else if (fossologyProcess != null && releaseService.isFOSSologyProcessCompleted(fossologyProcess)) {
            log.info("FOSSology process for Release : " + releaseId + " is complete.");
            responseMap.put("status", RequestStatus.SUCCESS);
        } else {
            responseMap.put("status", RequestStatus.FAILURE);
        }
        responseMap.put("fossologyProcessInfo", fossologyProcess);
        return new ResponseEntity<>(responseMap, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/{id}/triggerFossologyProcess", method = RequestMethod.GET)
    public ResponseEntity<HalResource> triggerFossologyProcess(@PathVariable("id") String releaseId,
            @RequestParam(value = "markFossologyProcessOutdated", required = false) boolean markFossologyProcessOutdated,
            @RequestParam(value = "uploadDescription", required = false) String uploadDescription,
            HttpServletResponse response) throws TException, IOException {
        releaseService.checkFossologyConnection();

        ReentrantLock lock = mapOfLocks.get(releaseId);
        Map<String, String> responseMap = new HashMap<>();
        HttpStatus status = null;
        if (lock == null || !lock.isLocked()) {
            if (mapOfLocks.size() > 10) {
                responseMap.put("message",
                        "Max 10 FOSSology Process can be triggered simultaneously. Please try after sometime.");
                status = HttpStatus.TOO_MANY_REQUESTS;
            } else {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                releaseService.executeFossologyProcess(user, attachmentService, mapOfLocks, releaseId,
                        markFossologyProcessOutdated, uploadDescription);
                responseMap.put("message", "FOSSology Process for Release Id : " + releaseId + " has been triggered.");
                status = HttpStatus.OK;
            }

        } else {
            status = HttpStatus.NOT_ACCEPTABLE;
            responseMap.put("message", "FOSSology Process for Release Id : " + releaseId
                    + " is already running. Please wait till it is completed.");
        }
        HalResource responseResource = new HalResource(responseMap);
        Link checkStatusLink = linkTo(ReleaseController.class).slash("api" + RELEASES_URL).slash(releaseId)
                .slash("checkFossologyProcessStatus").withSelfRel();
        responseResource.add(checkStatusLink);

        return new ResponseEntity<HalResource>(responseResource, status);
    }

    @RequestMapping(value = RELEASES_URL + "/{id}/reloadFossologyReport", method = RequestMethod.GET)
    public ResponseEntity<HalResource> triggerReloadFossologyReport(@PathVariable("id") String releaseId) throws TException {
        releaseService.checkFossologyConnection();
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, String> responseMap = new HashMap<>();
        String errorMsg = "Could not trigger report generation for this release";
        HttpStatus status = null;
        try {
            Release release = releaseService.getReleaseForUserById(releaseId, user);
            RequestStatus requestResult = releaseService.triggerReportGenerationFossology(releaseId, user);

            if (requestResult == RequestStatus.FAILURE) {
                responseMap.put("message", errorMsg);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            } else {
                ExternalToolProcess externalToolProcess = releaseService.getExternalToolProcess(release);
                if (externalToolProcess != null) {
                    ReentrantLock lock = mapOfLocks.get(releaseId);

                    if (lock == null || !lock.isLocked()) {
                        if (mapOfLocks.size() > 10) {
                            responseMap.put("message",
                                    "Max 10 FOSSology Process can be triggered simultaneously. Please try after sometime.");
                            status = HttpStatus.TOO_MANY_REQUESTS;
                        } else {
                            releaseService.executeFossologyProcess(user, attachmentService, mapOfLocks, releaseId,
                                    false, "");
                            responseMap.put("message", "Re-generate FOSSology's report process for Release Id : " + releaseId
                                    + " has been triggered.");
                            status = HttpStatus.OK;
                        }
                    } else {
                        responseMap.put("message", "Another FOSSology Process for Release Id : " + releaseId
                                + " is already running. Please wait till it is completed.");
                        status = HttpStatus.NOT_ACCEPTABLE;
                    }
                } else {
                    responseMap.put("message", "The source file is either not yet uploaded or scanning is not done.");
                    status = HttpStatus.INTERNAL_SERVER_ERROR;
                }
            }
        } catch (TException | IOException e) {
            responseMap.put("message", errorMsg);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Error pulling report from fossology", e);
        }

        HalResource responseResource = new HalResource(responseMap);
        if (status == HttpStatus.OK || status == HttpStatus.NOT_ACCEPTABLE) {
            Link checkStatusLink = linkTo(ReleaseController.class).slash("api" + RELEASES_URL).slash(releaseId)
                    .slash("checkFossologyProcessStatus").withSelfRel();
            responseResource.add(checkStatusLink);
        }
        return new ResponseEntity<>(responseResource, status);
    }

    // Link release to release
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL + "/{id}/releases", method = RequestMethod.POST)
    public ResponseEntity linkReleases(
            @PathVariable("id") String id,
            @RequestBody Map<String, ReleaseRelationship> releaseIdToRelationship) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        if (releaseIdToRelationship.isEmpty()) {
            throw new HttpMessageNotReadableException("Input data can not empty!");
        }
        sw360Release.setReleaseIdToRelationship(releaseIdToRelationship);

        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, sw360User);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL + "/{id}/link/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> linkPackages(
            @PathVariable("id") String id,
            @RequestBody Set<String> packagesInRequestBody) throws URISyntaxException, TException {
        RequestStatus linkPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, true);
        if (linkPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL + "/{id}/unlink/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> unlinkPackages(
            @PathVariable("id") String id,
            @RequestBody Set<String> packagesInRequestBody) throws URISyntaxException, TException {
        RequestStatus unlinkPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, false);
        if (unlinkPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(value = RELEASES_URL + "/{id}/spdxLicenses")
    public ResponseEntity writeSpdxLicenseInfoIntoRelease(
            @PathVariable("id") String releaseId,
            @RequestBody Map<String, Set<String>> licensesInfoInRequestBody
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
        Set<String> licenseIds = licensesInfoInRequestBody.get("mainLicenseIds");
        Set<String> otherLicenseIds = licensesInfoInRequestBody.get("otherLicenseIds");

        if (!CommonUtils.isNullOrEmptyCollection(licenseIds)) {
            for (String licenseId : licenseIds) {
                sw360Release.addToMainLicenseIds(licenseId);
            }
        }

        if (!CommonUtils.isNullOrEmptyCollection(otherLicenseIds)) {
            for (String licenseId : otherLicenseIds) {
                sw360Release.addToOtherLicenseIds(licenseId);
            }
        }

        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, sw360User);
        HalResource<Release> halRelease = createHalReleaseResource(sw360Release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @GetMapping(value = RELEASES_URL + "/{id}/spdxLicensesInfo")
    public ResponseEntity<?> loadSpdxLicensesInfo(
            @PathVariable("id") String releaseId,
            @RequestParam("attachmentId") String attachmentId,
            @RequestParam(value = "includeConcludedLicense", required = false, defaultValue = "false") boolean includeConcludedLicense) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, Set<String>> licenseToSrcFilesMap = new LinkedHashMap<>();
        Set<String> mainLicenseNames = new TreeSet<>();
        Set<String> otherLicenseNames = new TreeSet<>();
        final Set<String> concludedLicenseIds = new TreeSet<>();

        AttachmentType attachmentType;
        String attachmentName;
        long totalFileCount = 0;
        Map<String, Object> responseBody = new LinkedHashMap<>();
        Predicate<LicenseInfoParsingResult> filterLicenseResult = result -> (null != result.getLicenseInfo() &&
                null != result.getLicenseInfo().getLicenseNamesWithTexts());

        try {
            Release release = releaseService.getReleaseForUserById(releaseId, user);
            attachmentType = release.getAttachments().stream()
                    .filter(att -> attachmentId.equals(att.getAttachmentContentId())).map(Attachment::getAttachmentType).findFirst().orElse(null);
            if (null == attachmentType) {
                return new ResponseEntity<>("Cannot retrieve license information for attachment id " + attachmentId + " in release "
                        + releaseId + ".", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (!attachmentType.equals(AttachmentType.COMPONENT_LICENSE_INFO_XML) &&
                    !attachmentType.equals(AttachmentType.COMPONENT_LICENSE_INFO_COMBINED) &&
                    !attachmentType.equals(AttachmentType.INITIAL_SCAN_REPORT)) {
                return new ResponseEntity<>("Cannot retrieve license information for attachment type " + attachmentType + ".", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            attachmentName = release.getAttachments().stream()
                    .filter(att -> attachmentId.equals(att.getAttachmentContentId())).map(Attachment::getFilename).findFirst().orElse("");
            final boolean isISR = AttachmentType.INITIAL_SCAN_REPORT.equals(attachmentType);
            if (isISR) {
                includeConcludedLicense = true;
            }
            List<LicenseInfoParsingResult> licenseInfoResult = sw360LicenseInfoService.getLicenseInfoForAttachment(release, user, attachmentId, includeConcludedLicense);
            List<LicenseNameWithText> licenseWithTexts = licenseInfoResult.stream()
                    .filter(filterLicenseResult)
                    .map(LicenseInfoParsingResult::getLicenseInfo).map(LicenseInfo::getLicenseNamesWithTexts).flatMap(Set::stream)
                    .filter(license -> !license.getLicenseName().equalsIgnoreCase(SW360Constants.LICENSE_NAME_UNKNOWN)
                            && !license.getLicenseName().equalsIgnoreCase(SW360Constants.NA)
                            && !license.getLicenseName().equalsIgnoreCase(SW360Constants.NO_ASSERTION)) // exclude unknown, n/a and noassertion
                    .collect(Collectors.toList());

            if (attachmentName.endsWith(SW360Constants.RDF_FILE_EXTENSION)) {
                if (isISR) {
                    totalFileCount = licenseInfoResult.stream().map(LicenseInfoParsingResult::getLicenseInfo).map(LicenseInfo::getLicenseNamesWithTexts).flatMap(Set::stream)
                            .map(LicenseNameWithText::getSourceFiles).filter(Objects::nonNull).flatMap(Set::stream).distinct().count();
                    licenseToSrcFilesMap = CommonUtils.nullToEmptyList(licenseWithTexts).stream().collect(Collectors.toMap(LicenseNameWithText::getLicenseName,
                            LicenseNameWithText::getSourceFiles, (oldValue, newValue) -> oldValue));
                    licenseWithTexts.forEach(lwt -> {
                        lwt.getSourceFiles().forEach(sf -> {
                            if (sf.replaceAll(".*/", "").matches(SW360Constants.MAIN_LICENSE_FILES)) {
                                concludedLicenseIds.add(lwt.getLicenseName());
                            }
                        });
                    });
                } else {
                    concludedLicenseIds.addAll(licenseInfoResult.stream().flatMap(singleResult -> singleResult.getLicenseInfo().getConcludedLicenseIds().stream())
                            .collect(Collectors.toCollection(() -> new TreeSet<String>(String.CASE_INSENSITIVE_ORDER))));
                }
                otherLicenseNames = licenseWithTexts.stream().map(LicenseNameWithText::getLicenseName).collect(Collectors.toCollection(() -> new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)));
                otherLicenseNames.removeAll(concludedLicenseIds);
            } else if (attachmentName.endsWith(SW360Constants.XML_FILE_EXTENSION)) {
                mainLicenseNames = licenseWithTexts.stream()
                        .filter(license -> license.getType().equalsIgnoreCase(SW360Constants.LICENSE_TYPE_GLOBAL))
                        .map(LicenseNameWithText::getLicenseName).collect(Collectors.toCollection(() -> new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)));
                otherLicenseNames = licenseWithTexts.stream()
                        .filter(license -> !license.getType().equalsIgnoreCase(SW360Constants.LICENSE_TYPE_GLOBAL))
                        .map(LicenseNameWithText::getLicenseName).collect(Collectors.toCollection(() -> new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)));
            }
        } catch (TException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>("Cannot retrieve license information for attachment id " + attachmentId + " in release "
                    + releaseId + ".", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (CommonUtils.isNotEmpty(concludedLicenseIds)) {
            responseBody.put(SW360Constants.LICENSE_PREFIX, SW360Constants.CONCLUDED_LICENSE_IDS);
            responseBody.put(SW360Constants.LICENSE_IDS, concludedLicenseIds);
        } else if (CommonUtils.isNotEmpty(mainLicenseNames)) {
            responseBody.put(SW360Constants.LICENSE_PREFIX, SW360Constants.MAIN_LICENSE_ID);
            responseBody.put(SW360Constants.LICENSE_IDS, mainLicenseNames);
        }
        responseBody.put(SW360Constants.OTHER_LICENSE, SW360Constants.OTHER_LICENSE_IDS);
        responseBody.put(SW360Constants.OTHER_LICENSE_IDS_KEY, otherLicenseNames);
        if (AttachmentType.INITIAL_SCAN_REPORT.equals(attachmentType)) {
            responseBody.put(SW360Constants.LICENSE_PREFIX, SW360Constants.POSSIBLE_MAIN_LICENSE_IDS);
            responseBody.put(SW360Constants.TOTAL_FILE_COUNT, totalFileCount);
        }
        responseBody.putAll(licenseToSrcFilesMap);

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    private RequestStatus linkOrUnlinkPackages(String id, Set<String> packagesInRequestBody, boolean link)
            throws URISyntaxException, TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(id, sw360User);
        Set<String> packageIds = new HashSet<>();
        packageIds = release.getPackageIds();

        if (link) {
            packageIds.addAll(packagesInRequestBody);
        } else {
            packageIds.removeAll(packagesInRequestBody);
        }

        release.setPackageIds(packageIds);
        return releaseService.updateRelease(release, sw360User);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ReleaseController.class).slash("api" + RELEASES_URL).withRel("releases"));
        return resource;
    }

    private HalResource<Release> createHalReleaseResource(Release release, boolean verbose) throws TException {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId()).withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        if (verbose) {
            if (release.getModerators() != null) {
                Set<String> moderators = release.getModerators();
                restControllerHelper.addEmbeddedModerators(halRelease, moderators);
                release.setModerators(null);
            }
            if (release.getAttachments() != null) {
                Set<Attachment> attachments = release.getAttachments();
                restControllerHelper.addEmbeddedAttachments(halRelease, attachments);
                release.setAttachments(null);
            }
            if (release.getVendor() != null) {
                Vendor vendor = release.getVendor();
                HalResource<Vendor> vendorHalResource = restControllerHelper.addEmbeddedVendor(vendor);
                halRelease.addEmbeddedResource("sw360:vendors", vendorHalResource);
                release.setVendor(null);
            }
            if (release.getMainLicenseIds() != null) {
                restControllerHelper.addEmbeddedLicenses(halRelease, release.getMainLicenseIds());
                release.setMainLicenseIds(null);
            }
            Set<String> packageIds = release.getPackageIds();

            if (packageIds != null) {
                restControllerHelper.addEmbeddedPackages(halRelease, packageIds, packageService);
                release.setPackageIds(null);
            }
        }
        return halRelease;
    }
    private HalResource<Release> createHalReleaseResourceWithAllDetails(Release release) {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId())
                .withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        Set<String> packageIds = release.getPackageIds();

        if (packageIds != null) {
            for (String id : release.getPackageIds()) {
                Link packageLink = linkTo(ReleaseController.class)
                        .slash("api" + PackageController.PACKAGES_URL + "/" + id).withRel("packages");
                halRelease.add(packageLink);
            }
        }
        release.setPackageIds(null);
        for (Entry<Release._Fields, String> field : mapOfFieldsTobeEmbedded.entrySet()) {
            restControllerHelper.addEmbeddedFields(field.getValue(), release.getFieldValue(field.getKey()), halRelease);
        }
        // Do not add attachment as it is an embedded field
        release.unsetAttachments();
        return halRelease;
    }

    private Release setBackwardCompatibleFieldsInRelease(Map<String, Object> reqBodyMap) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        Set<Attachment> attachments = getAttachmentsFromRequest(reqBodyMap.get("attachments"), mapper);
        if (null != reqBodyMap.get("attachments")) {
            reqBodyMap.remove("attachments");
        }
        Release release = mapper.convertValue(reqBodyMap, Release.class);
        if (null != attachments) {
            release.setAttachments(attachments);
        }

        mapOfBackwardCompatible_Field_OldFieldNames_NewFieldNames.entrySet().stream().forEach(entry -> {
            Release._Fields field = entry.getKey();
            String oldFieldName = entry.getValue()[0];
            String newFieldName = entry.getValue()[1];
            if (!reqBodyMap.containsKey(newFieldName) && reqBodyMap.containsKey(oldFieldName)) {
                release.setFieldValue(field, CommonUtils.nullToEmptyString(reqBodyMap.get(oldFieldName)));
            }
        });

        return release;
    }

    private Set<Attachment> getAttachmentsFromRequest(Object attachmentData, ObjectMapper mapper) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (null == attachmentData) {
            return null;
        }
        Set<AttachmentDTO> attachmentDTOs = mapper.convertValue(attachmentData,
                mapper.getTypeFactory().constructCollectionType(Set.class, AttachmentDTO.class));
        return attachmentDTOs.stream()
                .map(attachmentDTO -> {
                    boolean isAttachmentExist = attachmentService.isAttachmentExist(attachmentDTO.getAttachmentContentId());
                    if (!isAttachmentExist) {
                        throw new ResourceNotFoundException("Attachment " + attachmentDTO.getAttachmentContentId() + " not found.");
                    }
                    return restControllerHelper.convertToAttachment(attachmentDTO, sw360User);
                })
                .collect(Collectors.toSet());
    }
}


