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

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ReleaseController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String RELEASES_URL = "/releases";
    private static final String SPDX_DOCUMENT = "spdxDocument";
    private static final String DOCUMENT_CREATION_INFORMATION = "documentCreationInformation";
    private static final String PACKAGE_INFORMATION = "packageInformation";
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
    private SW360SPDXDocumentService sw360SPDXDocumentService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @Operation(
            summary = "List all of the service's releases.",
            description = "List all of the service's releases.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getReleasesForUser(
            Pageable pageable,
            @Parameter(description = "sha1 of the release attachment")
            @RequestParam(value = "sha1", required = false) String sha1,
            @Parameter(description = "Fields of the object to be embedded in the response")
            @RequestParam(value = "fields", required = false) List<String> fields,
            @Parameter(description = "name of the release")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "luceneSearch parameter to filter the releases.")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            @Parameter(description = "fetch releases that are in NEW state and have a SRC/SRS attachment")
            @RequestParam(value = "isNewClearingWithSourceAvailable", required = false) boolean isNewClearingWithSourceAvailable,
            @Parameter(description = "allDetails of the release")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = new ArrayList<>();
        String queryString = request.getQueryString();
        Map<String, String> params = restControllerHelper.parseQueryString(queryString);

        if (luceneSearch && CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            sw360Releases.addAll(releaseService.refineSearch(name, sw360User));
        } else {
            if (sha1 != null && !sha1.isEmpty()) {
                sw360Releases.addAll(searchReleasesBySha1(sha1, sw360User));
            } else if (isNewClearingWithSourceAvailable) {
                sw360Releases.addAll(releaseService.getReleasesForUser(sw360User));
                sw360Releases = sw360Releases.stream()
                        .filter(release -> release.getClearingState() == ClearingState.NEW_CLEARING && !CommonUtils.isNullOrEmptyCollection(release.getAttachments())
                                && release.getAttachments().stream().anyMatch(attachment -> attachment.getAttachmentType() == AttachmentType.SOURCE
                                        || attachment.getAttachmentType() == AttachmentType.SOURCE_SELF)).collect(Collectors.toList());
            } else {
                sw360Releases.addAll(releaseService.getReleasesForUser(sw360User));
                sw360Releases = sw360Releases.stream()
                        .filter(release -> name == null || name.isEmpty() || release.getName().equalsIgnoreCase(params.get("name")))
                        .collect(Collectors.toList());
            }
        }

        if (allDetails) {
            for (Release release : sw360Releases) {
                if (!CommonUtils.isNullEmptyOrWhitespace(release.getVendorId())) {
                    release.setVendor(vendorService.getVendorById(release.getVendorId()));
                }
            }
        }

        if (CommonUtils.isNotNullEmptyOrWhitespace(sha1) || CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            for (Release release : sw360Releases) {
                releaseService.setComponentDependentFieldsInRelease(release, sw360User);
            }
        } else {
            releaseService.setComponentDependentFieldsInRelease(sw360Releases, sw360User);
        }

        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                sw360Releases, SW360Constants.TYPE_RELEASE);

        List<EntityModel<Release>> releaseResources = new ArrayList<>();
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

        CollectionModel<EntityModel<Release>> resources = null;
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

    @Operation(
            summary = "Get a release by ID.",
            description = "Get a single release by ID.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{id}")
    public ResponseEntity<EntityModel<Release>> getRelease(
            @Parameter(description = "The ID of the release to be retrieved.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        HalResource<Release> halRelease = createHalReleaseResource(sw360Release, true);
        restControllerHelper.addEmbeddedDataToHalResourceRelease(halRelease, sw360Release);
        List<ReleaseLink> linkedReleaseRelations = releaseService.getLinkedReleaseRelations(sw360Release, sw360User);

        String spdxId = sw360Release.getSpdxId();
        if (CommonUtils.isNotNullEmptyOrWhitespace(spdxId) && SW360Constants.SPDX_DOCUMENT_ENABLED) {
            SPDXDocument spdxDocument = releaseService.getSPDXDocumentById(spdxId, sw360User);
            sw360SPDXDocumentService.sortSectionForSPDXDocument(spdxDocument);
            restControllerHelper.addEmbeddedSpdxDocument(halRelease, spdxDocument);
            String spdxDocumentCreationInfoId = spdxDocument.getSpdxDocumentCreationInfoId();
            if (CommonUtils.isNotNullEmptyOrWhitespace(spdxDocumentCreationInfoId)) {
                DocumentCreationInformation documentCreationInformation = releaseService.getDocumentCreationInformationById(spdxDocumentCreationInfoId, sw360User);
                sw360SPDXDocumentService.sortSectionForDocumentCreation(documentCreationInformation);
                restControllerHelper.addEmbeddedDocumentCreationInformation(halRelease, documentCreationInformation);
            }
            String spdxPackageInfoId = spdxDocument.getSpdxPackageInfoIds().stream().findFirst().get();
            if(CommonUtils.isNotNullEmptyOrWhitespace(spdxPackageInfoId)) {
                PackageInformation packageInformation = releaseService.getPackageInformationById(spdxPackageInfoId, sw360User);
                sw360SPDXDocumentService.sortSectionForPackageInformation(packageInformation);
                restControllerHelper.addEmbeddedPackageInformation(halRelease, packageInformation);
            }
        }
        if (linkedReleaseRelations != null) {
            restControllerHelper.addEmbeddedReleaseLinks(halRelease, linkedReleaseRelations);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @Operation(
            summary = "Get recently created releases.",
            description = "Get 5 of the service's most recently created releases.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/recentReleases")
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getRecentRelease() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = releaseService.getRecentReleases(sw360User);

        List<EntityModel<Release>> resources = new ArrayList<>();
        sw360Releases.forEach(r -> {
            Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(r);
            resources.add(EntityModel.of(embeddedRelease));
        });

        CollectionModel<EntityModel<Release>> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @Operation(
            summary = "Get vulnerabilities of a single release.",
            description = "Get vulnerabilities of a single release.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<VulnerabilityDTO>> getVulnerabilitiesOfReleases(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id
    ) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        final List<VulnerabilityDTO> allVulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByReleaseId(id, user);
        CollectionModel<VulnerabilityDTO> resources = CollectionModel.of(allVulnerabilityDTOs);
        return new ResponseEntity<>(resources,HttpStatus.OK);
    }

    @Operation(
            summary = "Get service's releases subscription.",
            description = "Get service's releases subscription.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/mySubscriptions")
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getReleaseSubscription() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = releaseService.getReleaseSubscriptions(sw360User);

        List<EntityModel<Release>> resources = new ArrayList<>();
        sw360Releases.forEach(c -> {
            Release embeddedComponent = restControllerHelper.convertToEmbeddedRelease(c);
            resources.add(EntityModel.of(embeddedComponent));
        });

        CollectionModel<EntityModel<Release>> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @Operation(
            summary = "Get all the resources where the release is used.",
            description = "Get all the resources where the release is used.",
            tags = {"Releases"}
    )
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

    @Operation(
            summary = "Get releases by external IDs.",
            description = "Get releases where provided external IDs match.",
            tags = {"Releases"},
            parameters = {
                    @Parameter(
                            description = "The external IDs of the releases to filter.",
                            example = "{\n" +
                                    "\"mainline-id-component\": \"1432\",\n" +
                                    "\"mainline-id-component\": \"4876\"\n" +
                                    "}",
                            in = ParameterIn.QUERY,
                            name = "externalIds",
                            explode = Explode.TRUE,
                            schema = @Schema(implementation = LinkedMultiValueMap.class)
                    )
            }

    )
    @GetMapping(value = RELEASES_URL + "/searchByExternalIds")
    public ResponseEntity<Release> searchByExternalIds(
            HttpServletRequest request
    ) throws TException {
        String queryString = request.getQueryString();
        return restControllerHelper.searchByExternalIds(queryString, releaseService, null);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Delete releases.",
            description = "Delete existing releases.",
            tags = {"Releases"}
    )
    @DeleteMapping(value = RELEASES_URL + "/{ids}")
    public ResponseEntity<List<MultiStatus>> deleteReleases(
            @Parameter(description = "The IDs of the releases to be deleted.")
            @PathVariable("ids") List<String> idsToDelete
    ) throws TException {
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
    @Operation(
            summary = "Update a release.",
            description = "Update an existing release.",
            tags = {"Releases"}
    )
    @PatchMapping(value = RELEASES_URL + "/{id}")
    public ResponseEntity<EntityModel<Release>> patchRelease(
            @Parameter(description = "The ID of the release to be updated.")
            @PathVariable("id") String id,
            @Parameter(description = "The release object to be updated.",
                    schema = @Schema(implementation = Release.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, user);
        Release updateRelease = setBackwardCompatibleFieldsInRelease(reqBodyMap);
        updateRelease.setClearingState(sw360Release.getClearingState());
        sw360Release = this.restControllerHelper.updateRelease(sw360Release, updateRelease);
        releaseService.setComponentNameAsReleaseName(sw360Release, user);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, user);
        sw360Release = releaseService.getReleaseForUserById(id, user);
        HalResource<Release> halRelease = createHalReleaseResource(sw360Release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update vulnerabilities of a release.",
            description = "Update vulnerabilities of an existing release.",
            tags = {"Releases"}
    )
    @PatchMapping(value = RELEASES_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> patchReleaseVulnerabilityRelation(
            @Parameter(description = "The ID of the release to be updated.")
            @PathVariable("id") String releaseId,
            @Parameter(description = "The vulnerability object to be updated.")
            @RequestBody VulnerabilityState vulnerabilityState
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        if(CommonUtils.isNullOrEmptyCollection(vulnerabilityState.getReleaseVulnerabilityRelationDTOs())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Required field ReleaseVulnerabilityRelation is not present");
        }
        if(vulnerabilityState.getVerificationState() == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Required field verificationState is not present");
        }
        List<VulnerabilityDTO> actualVDto = vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, user);
        Set<String> externalIdsFromRequestDto = vulnerabilityState.getReleaseVulnerabilityRelationDTOs().stream().map(ReleaseVulnerabilityRelationDTO::getExternalId).collect(Collectors.toSet());
        List<VulnerabilityDTO> actualVDtoFromRequest = vulnerabilityService.getVulnerabilityDTOByExternalId(externalIdsFromRequestDto, releaseId);
        Set<String> actualExternalId = actualVDto.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> commonExtIds = Sets.intersection(actualExternalId, externalIdsFromRequestDto);
        if(CommonUtils.isNullOrEmptyCollection(commonExtIds) || commonExtIds.size() != externalIdsFromRequestDto.size()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "External ID is not valid");
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
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "User not allowed!");
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
    @Operation(
            summary = "Create a release.",
            description = "Create a new release.",
            tags = {"Releases"}
    )
    @PostMapping(value = RELEASES_URL)
    public ResponseEntity<EntityModel<Release>> createRelease(
            @Parameter(description = "The release object to be created.",
                    schema = @Schema(implementation = Release.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws URISyntaxException, TException {
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

    @Operation(
            summary = "Update SPDX document.",
            description = "Update SPDX document of a release.",
            tags = {"Releases"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = RELEASES_URL + "/{id}/spdx")
    public ResponseEntity<?> updateSPDX(
            @Parameter(description = "Updated data of SPDX document")
            @RequestBody Map<String, Object> reqBodyMap,
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId
    ) throws TException {
        if (Boolean.FALSE.equals(SW360Constants.SPDX_DOCUMENT_ENABLED)) {
            return new ResponseEntity<>("Feature SPDXDocument disable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (CommonUtils.isNullEmptyOrWhitespace(releaseId)) {
            throw new HttpMessageNotReadableException("Release id not found");
        }
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, user);
        String spdxId = "";

        if (CommonUtils.isNullEmptyOrWhitespace(release.getSpdxId())) {
            spdxId = sw360SPDXDocumentService.addSPDX(release, user);
        }
        SPDXDocument spdxDocumentActual = CommonUtils.isNullEmptyOrWhitespace(spdxId)
                ? releaseService.getSPDXDocumentById(release.getSpdxId(), user)
                : releaseService.getSPDXDocumentById(spdxId, user);
        spdxId = spdxDocumentActual.getId();
        if (CommonUtils.isNullEmptyOrWhitespace(spdxId)) {
            throw new HttpMessageNotReadableException("Update SPDXDocument Failed!");
        }
        HalResource<Release> halRelease = createHalReleaseResource(release, false);

        if(reqBodyMap.isEmpty()) {
            return ResponseEntity.ok(halRelease);
        }

        if (null != reqBodyMap.get(SPDX_DOCUMENT)) {
            SPDXDocument spdxDocumentRequest = sw360SPDXDocumentService.convertToSPDXDocument(reqBodyMap.get(SPDX_DOCUMENT));
            if (null != spdxDocumentRequest) {
                spdxDocumentRequest = sw360SPDXDocumentService.updateSPDXDocumentFromRequest(spdxDocumentRequest, spdxDocumentActual, release.getModerators());
                RequestStatus requestStatus = releaseService.updateSPDXDocument(spdxDocumentRequest, release.getId(), user);
                if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
                    return ResponseEntity.accepted().body(RESPONSE_BODY_FOR_MODERATION_REQUEST);
                }
                restControllerHelper.addEmbeddedSpdxDocument(halRelease, spdxDocumentRequest);
            }
        } else {
            restControllerHelper.addEmbeddedSpdxDocument(halRelease, spdxDocumentActual);
        }

        if (null != reqBodyMap.get(DOCUMENT_CREATION_INFORMATION)) {
            DocumentCreationInformation documentCreationInformation = sw360SPDXDocumentService.convertToDocumentCreationInformation(reqBodyMap.get(DOCUMENT_CREATION_INFORMATION));
            if (null != documentCreationInformation) {
                documentCreationInformation = sw360SPDXDocumentService.updateDocumentCreationInformationFromRequest(documentCreationInformation, spdxDocumentActual, release.getModerators());
                releaseService.updateDocumentCreationInformation(documentCreationInformation, spdxId, user);
                restControllerHelper.addEmbeddedDocumentCreationInformation(halRelease, documentCreationInformation);
            }
        }

        if (null != reqBodyMap.get(PACKAGE_INFORMATION)) {
            PackageInformation packageInformation = sw360SPDXDocumentService.convertToPackageInformation(reqBodyMap.get(PACKAGE_INFORMATION));
            if( null != packageInformation) {
                packageInformation = sw360SPDXDocumentService.updatePackageInformationFromRequest(packageInformation, spdxDocumentActual, release.getModerators());
                releaseService.updatePackageInformation(packageInformation, spdxId, user);
                restControllerHelper.addEmbeddedPackageInformation(halRelease, packageInformation);
            }
        }

        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @Operation(
            summary = "Get attachment info of a release.",
            description = "Get all attachment information of a release.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{id}/attachments")
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getReleaseAttachments(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        final CollectionModel<EntityModel<Attachment>> resources = attachmentService.getAttachmentResourcesFromList(sw360User, sw360Release.getAttachments(), Source.releaseId(id));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Download the attachment bundle of a release.",
            description = "Download the attachment bundle of a release.",
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true, example = "application/zip"),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/zip")}
                    )
            },
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{releaseId}/attachments/download", produces="application/zip")
    public void downloadAttachmentBundleFromRelease(
            @Parameter(description = "The ID of the release.")
            @PathVariable("releaseId") String releaseId,
            HttpServletResponse response
    ) throws TException, IOException {
        final User user = restControllerHelper.getSw360UserFromAuthentication();
        final Release release = releaseService.getReleaseForUserById(releaseId, user);
        final Set<Attachment> attachments = release.getAttachments();
        attachmentService.downloadAttachmentBundleWithContext(release, attachments, user, response);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update an attachment info of a release.",
            description = "Update an attachment information of a release.",
            tags = {"Releases"}
    )
    @PatchMapping(value = RELEASES_URL + "/{id}/attachment/{attachmentId}")
    public ResponseEntity<EntityModel<Attachment>> patchReleaseAttachmentInfo(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id,
            @Parameter(description = "The ID of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            @Parameter(description = "The attachment information.")
            @RequestBody Attachment attachmentData
    ) throws TException {
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

    @Operation(
            summary = "Create a new attachment for the release.",
            description = "Create a new attachment for the release.",
            tags = {"Releases"}
    )
    @PostMapping(value = RELEASES_URL + "/{releaseId}/attachments", consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource<Release>> addAttachmentToRelease(
            @Parameter(description = "The ID of the release.")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "The attachment file.")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "The attachment information.")
            @RequestPart("attachment") Attachment newAttachment
    ) throws TException {
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
        HalResource<Release> halRelease = createHalReleaseResource(release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @Operation(
            summary = "Download an attachment of a release.",
            description = "Download an attachment of a release.",
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true, example = MediaType.APPLICATION_OCTET_STREAM_VALUE),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)}
                    )
            },
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{releaseId}/attachments/{attachmentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromRelease(
            @Parameter(description = "The ID of the release.")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "The ID of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
        attachmentService.downloadAttachmentWithContext(release, attachmentId, response, sw360User);
    }

    @Operation(
            summary = "Delete attachments from a release.",
            description = "Delete attachments from a release.",
            tags = {"Releases"}
    )
    @DeleteMapping(RELEASES_URL + "/{releaseId}/attachments/{attachmentIds}")
    public ResponseEntity<HalResource<Release>> deleteAttachmentsFromRelease(
            @Parameter(description = "The ID of the release.")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "The IDs of the attachments to be deleted.")
            @PathVariable("attachmentIds") List<String> attachmentIds
    ) throws TException {
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

    @Operation(
            summary = "Check status of triggered FOSSology process of a release.",
            description = "Check status of triggered FOSSology process of a release.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(
                                            type = "object",
                                            example = "{\"status\": \"SUCCESS\"}"
                                    ))}
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/checkFossologyProcessStatus", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> checkFossologyProcessStatus(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId
    ) throws TException {
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
        
        // Add detailed report status if report step exists
        if (fossologyProcess != null && fossologyProcess.getProcessSteps().size() >= 3) {
            ExternalToolProcessStep reportStep = fossologyProcess.getProcessSteps().get(2);
            if (reportStep.getProcessStepIdInTool() != null && !reportStep.getProcessStepIdInTool().isEmpty()) {
                try {
                    int reportId = Integer.parseInt(reportStep.getProcessStepIdInTool());
                    if (reportId > 0) {
                        Map<String, String> reportStatus = releaseService.checkFossologyReportStatus(reportId);
                        responseMap.put("reportStatus", reportStatus);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid report ID format in process step: {}", reportStep.getProcessStepIdInTool());
                }
            }
        }
        
        responseMap.put("fossologyProcessInfo", fossologyProcess);
        return new ResponseEntity<>(responseMap, HttpStatus.OK);
    }

    @Operation(
            summary = "Trigger FOSSology process of a release.",
            description = "Trigger FOSSology process of a release.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(type = "object", implementation = Map.class),
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"FOSSology Process for Release Id : " +
                                                                    "\\\"123\\\" has been triggered.\"}"
                                            )
                                    }
                            )}
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Max 10 FOSSology Process can be " +
                                                                    "triggered simultaneously. Please try after sometime.\"}"
                                            )
                                    }
                            )}
                    ),
                    @ApiResponse(
                            responseCode = "406",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"FOSSology Process for Release Id : " +
                                                                    "\\\"123\\\" is already running. Please wait till" +
                                                                    " it is completed.\"}"
                                            )
                                    }
                            )}
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/triggerFossologyProcess", method = RequestMethod.GET)
    public ResponseEntity<HalResource> triggerFossologyProcess(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId,
            @Parameter(description = "Mark previous FOSSology process outdated and generate new.")
            @RequestParam(value = "markFossologyProcessOutdated", required = false) boolean markFossologyProcessOutdated,
            @Parameter(description = "Upload description to FOSSology")
            @RequestParam(value = "uploadDescription", required = false) String uploadDescription
    ) throws TException, IOException {
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

    @Operation(
            summary = "Re-generate fossology report.",
            description = "Re-generate fossology report.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(type = "object", implementation = Map.class),
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"FOSSology Process for Release Id : " +
                                                                    "\\\"123\\\" has been triggered.\"}"
                                            )
                                    }
                            )}
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Max 10 FOSSology Process can be " +
                                                                    "triggered simultaneously. Please try after sometime.\"}"
                                            )
                                    }
                            )}
                    ),
                    @ApiResponse(
                            responseCode = "406",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"FOSSology Process for Release Id : " +
                                                                    "\\\"123\\\" is already running. Please wait till" +
                                                                    " it is completed.\"}"
                                            )
                                    }
                            )}
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(type = "object", implementation = Map.class))}
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/reloadFossologyReport", method = RequestMethod.GET)
    public ResponseEntity<HalResource> triggerReloadFossologyReport(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId
    ) throws TException {
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

    @Operation(
            summary = "Link release to a release.",
            description = "Link release to a release.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            content = {@Content(mediaType = "application/hal+json")},
                            description = "Update stored."
                    ),
                    @ApiResponse(
                            responseCode = "202",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Moderation request is created\"}"
                                            )
                                    })},
                            description = "Moderation request is created for the update."
                    )
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL + "/{id}/releases", method = RequestMethod.POST)
    public ResponseEntity linkReleases(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id,
            @Parameter(description = "Relationship links with release ID as the key.",
                    schema = @Schema(
                            type = "object",
                            example = "{\n" +
                                    "  \"releaseId1\": \"DYNAMICALLY_LINKED\",\n" +
                                    "  \"releaseId2\": \"CONTAINED\"\n" +
                                    "}"
                    )
            )
            @RequestBody Map<String, ReleaseRelationship> releaseIdToRelationship
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        if (releaseIdToRelationship.isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Input data can not empty!");
        }
        sw360Release.setReleaseIdToRelationship(releaseIdToRelationship);

        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, sw360User);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Add or link packages to the project.",
            description = "Add or link packages to the project.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            content = {@Content(mediaType = "application/hal+json")},
                            description = "Update stored."
                    ),
                    @ApiResponse(
                            responseCode = "202",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Moderation request is created\"}"
                                            )
                                    })},
                            description = "Moderation request is created for the update."
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/link/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> linkPackages(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id,
            @Parameter(description = "The package IDs to be linked.")
            @RequestBody Set<String> packagesInRequestBody
    ) throws URISyntaxException, TException {
        if(!packageService.validatePackageIds(packagesInRequestBody)){
            return new ResponseEntity<>("Package ID invalid! ", HttpStatus.NOT_FOUND);
        }
        RequestStatus linkPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, true);
        if (linkPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Unlink packages to the project.",
            description = "Unlink packages to the project.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            content = {@Content(mediaType = "application/hal+json")},
                            description = "Update stored."
                    ),
                    @ApiResponse(
                            responseCode = "202",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Moderation request is created\"}"
                                            )
                                    })},
                            description = "Moderation request is created for the update."
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/unlink/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> unlinkPackages(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id,
            @Parameter(description = "The package IDs to be linked.")
            @RequestBody Set<String> packagesInRequestBody
    ) throws URISyntaxException, TException {
        if(!packageService.validatePackageIds(packagesInRequestBody)){
            return new ResponseEntity<>("Package ID invalid! ", HttpStatus.NOT_FOUND);
        }
        RequestStatus unlinkPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, false);
        if (unlinkPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(
            summary = "Write SPDX license info into release.",
            description = "Write SPDX license info into release.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(type = "object", implementation = Release.class))}
                    ),
                    @ApiResponse(
                            responseCode = "202",
                            content = {@Content(mediaType = "application/hal+json",
                                    examples = {
                                            @ExampleObject(
                                                    value =
                                                            "{\"message\": \"Moderation request is created\"}"
                                            )
                                    })},
                            description = "Moderation request is created for the update."
                    )
            }
    )
    @PostMapping(value = RELEASES_URL + "/{id}/spdxLicenses")
    public ResponseEntity writeSpdxLicenseInfoIntoRelease(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId,
            @Parameter(description = "The SPDX license info.",
                    schema = @Schema(
                            type = "object",
                            example = "{\n" +
                                    "  \"mainLicenseIds\": [\n" +
                                    "    \"LicenseRef-1\",\n" +
                                    "    \"LicenseRef-2\"\n" +
                                    "  ],\n" +
                                    "  \"otherLicenseIds\": [\n" +
                                    "    \"LicenseRef-3\",\n" +
                                    "    \"LicenseRef-4\"\n" +
                                    "  ]\n" +
                                    "}",
                            requiredProperties = {"mainLicenseIds", "otherLicenseIds"}
                    )
            )
            @RequestBody Map<String, Set<String>> licensesInfoInRequestBody
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
        Set<String> licenseIds = licensesInfoInRequestBody.get("mainLicenseIds");
        Set<String> otherLicenseIds = licensesInfoInRequestBody.get("otherLicenseIds");

        if (!CommonUtils.isNullOrEmptyCollection(licenseIds)) {
	    sw360Release.getMainLicenseIds().clear();
            for (String licenseId : licenseIds) {
                sw360Release.addToMainLicenseIds(licenseId);
            }
        }

        if (!CommonUtils.isNullOrEmptyCollection(otherLicenseIds)) {
	    sw360Release.getOtherLicenseIds().clear();
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

    @Operation(
            summary = "Load SPDX license info from release.",
            description = "Load SPDX License Information from the attachment of the release.",
            tags = {"Releases"}
    )
    @GetMapping(value = RELEASES_URL + "/{id}/spdxLicensesInfo")
    public ResponseEntity<?> loadSpdxLicensesInfo(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId,
            @Parameter(description = "The ID of the attachment.")
            @RequestParam("attachmentId") String attachmentId,
            @Parameter(description = "Include concluded license.")
            @RequestParam(value = "includeConcludedLicense", required = false, defaultValue = "false") boolean includeConcludedLicense
    ) {
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

    @Operation(
            summary = "Get linked releases information for a release by id.",
            description = "Get linked releases information for a release by id.",
            tags = {"Releases"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        content = {@Content(mediaType = "application/hal+json",
                                schema = @Schema(
                                        type = "object",
                                        example = """
                                                {
                                                    "_embedded": {
                                                        "sw360:releaseLinks": [
                                                            {
                                                                "id": "123211321",
                                                                "name": "Release 1",
                                                                "version": "1.0",
                                                                "releaseRelationship": "CONTAINED",
                                                                "clearingState": "NEW_CLEARING",
                                                                "licenseIds": [],
                                                                "accessible": true,
                                                                "componentId": "4566612"
                                                            }
                                                        ]
                                                    },
                                                    "_links": {
                                                        "curies": [
                                                            {
                                                                "href": "http://localhost:8080/resource/docs/{rel}.html",
                                                                "name": "sw360",
                                                                "templated": true
                                                            }
                                                        ]
                                                    }
                                                }
                                        """
                                ))}
                    )
            }
    )
    @GetMapping(value = RELEASES_URL + "/{id}/releases")
    public ResponseEntity<CollectionModel<HalResource<ReleaseLink>>> getLinkedReleases(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id,
            @Parameter(description = "Get direct (false) or transitive (true) linked releases.")
            @RequestParam(value = "transitive", required = false, defaultValue = "false") boolean transitive
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        Map<String, ReleaseRelationship> releaseRelationshipMap = !CommonUtils.isNullOrEmptyMap(sw360Release.getReleaseIdToRelationship())
                ? sw360Release.getReleaseIdToRelationship()
                : new HashMap<>();
        Set<String> releaseIdsInBranch = new HashSet<>();

        final List<HalResource<ReleaseLink>> linkedReleaseResources = releaseRelationshipMap.entrySet().stream()
                .map(item -> wrapTException(() -> {
                    final Release releaseById = releaseService.getReleaseForUserById(item.getKey(), sw360User);
                    final ReleaseLink embeddedReleaseLink = restControllerHelper.convertToReleaseLink(releaseById, item.getValue());
                    embeddedReleaseLink.setAccessible(releaseService.isReleaseActionAllowed(releaseById, sw360User, RequestedAction.READ));
                    final HalResource<ReleaseLink> releaseResource = new HalResource<>(embeddedReleaseLink);
                    if (transitive) {
                        releaseService.addEmbeddedLinkedRelease(releaseById, sw360User, releaseResource, releaseIdsInBranch);
                    }
                    return releaseResource;
                })).collect(Collectors.toList());

        CollectionModel<HalResource<ReleaseLink>> collectionModel = CollectionModel.of(linkedReleaseResources);
        return new ResponseEntity<>(collectionModel, HttpStatus.OK);
    }

    private RequestStatus linkOrUnlinkPackages(String id, Set<String> packagesInRequestBody, boolean link)
            throws URISyntaxException, TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(id, sw360User);
        Set<String> packageIds;
        packageIds = release.getPackageIds();
        if (CommonUtils.isNullOrEmptyCollection(packageIds)) {
            packageIds = new HashSet<>();
        }
        if (link) {
            packageIds.addAll(packagesInRequestBody);
        } else {
            packageIds.removeAll(packagesInRequestBody);
        }

        release.setPackageIds(packageIds);
        return releaseService.updateRelease(release, sw360User);
    }

    @Operation(
            summary = "Get assessment summary info of release.",
            description = "Get assessment summary info of release (Have one CLI file only).",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(
                                            type = "object",
                                            example = "{\n" +
                                                    "  \"GeneralAssessment\": \"General Assessment\",\n" +
                                                    "  \"CriticalFilesFound\": \"Critical Files Found\",\n" +
                                                    "  \"AdditionalNotes\": \"Additional Notes\",\n" +
                                                    "  \"UsageRestrictionsFound\": \"None\",\n" +
                                                    "  \"ExportRestrictionsFound\": \"Export Restrictions Found\",\n" +
                                                    "  \"DependencyNotes\": \"Dependency Notes\"\n" +
                                                    "}"
                                    ))}
                    )
            }
    )
    @GetMapping(value = RELEASES_URL + "/{id}/assessmentSummaryInfo")
    public ResponseEntity loadAssessmentSummaryInfo(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String id
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(id, user);
        final boolean INCLUDE_CONCLUDED_LICENSE = true;

        Map<String, String> assessmentSummaryMap = new HashMap<>();
        List<String> cliAttachmentIds = release.getAttachments().stream()
                .filter(att -> att.getAttachmentType().equals(AttachmentType.COMPONENT_LICENSE_INFO_XML))
                .map(Attachment::getAttachmentContentId).collect(Collectors.toList());
        if (cliAttachmentIds.size() != 1) {
            return new ResponseEntity<>("Number of CLI attachments must be 1", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<LicenseInfoParsingResult> licenseInfoResult = sw360LicenseInfoService.getLicenseInfoForAttachment(release,
                user, cliAttachmentIds.get(0), INCLUDE_CONCLUDED_LICENSE);

        if (CommonUtils.isNotEmpty(licenseInfoResult) && Objects.nonNull(licenseInfoResult.get(0).getLicenseInfo())) {
            assessmentSummaryMap = licenseInfoResult.get(0).getLicenseInfo().getAssessmentSummary();
        }

        if (CommonUtils.isNullOrEmptyMap(assessmentSummaryMap)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(assessmentSummaryMap, HttpStatus.OK);
    }

    @Operation(
            summary = "Check cyclic hierarchy of a release with other releases.",
            description = "Check cyclic hierarchy of a release with other releases.",
            tags = {"Releases"},
            responses = {
                    @ApiResponse(
                            responseCode = "207",
                            content = {@Content(mediaType = "application/hal+json",
                                    schema = @Schema(
                                            type = "object",
                                            example = """
                                                [
                                                    {
                                                        "message": "release1(1) -> release1(1)",
                                                        "status": 409
                                                    },
                                                    {
                                                        "message": "There are no cyclic link between 3765276512 and 12121212",
                                                        "status": 200
                                                    }
                                                ]
                                            """
                                    ))}
                    )
            }
    )
    @RequestMapping(value = RELEASES_URL + "/{id}/checkCyclicLink", method = RequestMethod.POST)
    public ResponseEntity<?> checkForCyclicReleaseLink(
            @Parameter(description = "The ID of the checking release.")
            @PathVariable("id") String releaseId,
            @Parameter(description = "Release ids to check",
                schema = @Schema(example = """
                        {
                          "linkedReleases": ["3765276512"],
                          "linkedToReleases": ["12121212"]
                        }
                    """
                )
            )
            @RequestBody Map<String, Set<String>> relationshipReleaseIds
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<ImmutableMap<String, Object>> results = new ArrayList<>();
        Release checkingRelease = releaseService.getReleaseForUserById(releaseId, user);
        if (!CommonUtils.isNullOrEmptyCollection(relationshipReleaseIds.get("linkedToReleases"))) {
            for (String parentReleaseId : relationshipReleaseIds.get("linkedToReleases")) {
                String cyclicPath;
                try {
                    Release parentRelease = releaseService.getReleaseForUserById(parentReleaseId, user);
                    cyclicPath = releaseService.checkForCyclicLinkedReleases(parentRelease, checkingRelease, user);
                } catch (ResourceNotFoundException notFoundException) {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", notFoundException.getMessage())
                            .put("status", 404)
                            .build());
                    continue;
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(cyclicPath.trim())) {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", cyclicPath)
                            .put("status", 409)
                            .build());
                } else {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", "There are no cyclic link between " + parentReleaseId + " and " + releaseId)
                            .put("status", 200)
                            .build());
                }
            }
        }

        if (!CommonUtils.isNullOrEmptyCollection(relationshipReleaseIds.get("linkedReleases"))) {
            for (String linkedReleaseId : relationshipReleaseIds.get("linkedReleases")) {
                String cyclicPath;
                try {
                    Release linkedRelease = releaseService.getReleaseForUserById(linkedReleaseId, user);
                    cyclicPath = releaseService.checkForCyclicLinkedReleases(checkingRelease, linkedRelease, user);
                } catch (ResourceNotFoundException notFoundException) {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", notFoundException.getMessage())
                            .put("status", 404)
                            .build());
                    continue;
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(cyclicPath.trim())) {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", cyclicPath)
                            .put("status", 409)
                            .build());
                } else {
                    results.add(ImmutableMap.<String, Object>builder()
                            .put("message", "There are no cyclic link between " + releaseId + " and " + linkedReleaseId)
                            .put("status", 200)
                            .build());
                }
            }
        }

        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    @Operation(
            summary = "Handle release subcription for requesting user.",
            description = "Handle release subcription for requesting user.",
            tags = {"Releases"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = RELEASES_URL + "/{id}/subscriptions")
    public ResponseEntity<String> handleReleaseSubscriptions(
            @Parameter(description = "The ID of the release.")
            @PathVariable("id") String releaseId
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release releaseById = releaseService.getReleaseForUserById(releaseId, user);
        Set<String> subscribers = releaseById.getSubscribers();
        if (subscribers.contains(user.getEmail())) {
            releaseService.unsubscribeRelease(user, releaseId);
            return new ResponseEntity<>("Release has been unsubscribed", HttpStatus.OK);
        } else {
            releaseService.subscribeRelease(user, releaseId);
            return new ResponseEntity<>("Release has been subscribed", HttpStatus.OK);
        }
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
            }
            if (release.getOtherLicenseIds() != null) {
                restControllerHelper.addEmbeddedOtherLicenses(halRelease, release.getOtherLicenseIds());
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

        Set<Attachment> attachments = attachmentService.getAttachmentsFromRequest(reqBodyMap.get("attachments"), mapper);
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
}
