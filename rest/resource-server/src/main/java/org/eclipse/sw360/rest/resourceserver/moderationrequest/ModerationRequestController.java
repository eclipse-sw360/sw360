/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 * SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
 */
package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

import static org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService.isOpenModerationRequest;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ModerationRequestController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String MODERATION_REQUEST_URL = "/moderationrequest";

    @Autowired
    private Sw360ModerationRequestService sw360ModerationRequestService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @Operation(
            summary = "List all of the service's moderation requests.",
            description = "List all of the service's moderation requests.",
            tags = {"Moderation Requests"}
    )
    @RequestMapping(value = MODERATION_REQUEST_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<ModerationRequest>> getModerationRequests(
            Pageable pageable, HttpServletRequest request,
            @Parameter(description = "Fetch all details of the moderation request")
            @RequestParam(value = "allDetails", required = false) boolean allDetails
    ) throws TException, ResourceClassNotFoundException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<ModerationRequest> moderationRequests = sw360ModerationRequestService.getRequestsByModerator(sw360User, pageable);

        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                new HashMap<>();
        PaginationData paginationData = new PaginationData();
        paginationData.setTotalRowCount(sw360ModerationRequestService.getTotalCountOfRequests(sw360User));
        modRequestsWithPageData.put(paginationData, moderationRequests);

        return getModerationResponseEntity(pageable, request, allDetails, modRequestsWithPageData);
    }

    @Operation(
            summary = "Get a single moderation request.",
            description = "Get a single moderation request by id.",
            tags = {"Moderation Requests"}
    )
    @RequestMapping(value = MODERATION_REQUEST_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<HalResource<ModerationRequest>> getModerationRequestById(
            @Parameter(description = "The id of the moderation request to be retrieved.")
            @PathVariable String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ModerationRequest moderationRequest = filterModerationRequestNoDuplicates(
                sw360ModerationRequestService.getModerationRequestById(id));
        HalResource<ModerationRequest> halModerationRequest = createHalModerationRequestWithAllDetails(moderationRequest,
                sw360User);
        HttpStatus status = halModerationRequest.getContent() == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halModerationRequest, status);
    }

    @Operation(
            summary = "Get moderation based on state.",
            description = "List all the ModerationRequest visible to the user based on the state.",
            tags = {"Moderation Requests"}
    )
    @RequestMapping(value = MODERATION_REQUEST_URL + "/byState", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<ModerationRequest>> getModerationRequestsByState(
            Pageable pageable, HttpServletRequest request,
            @Parameter(
                    description = "The moderation request state of the request.",
                    schema = @Schema(allowableValues = {"open", "closed"})
            )
            @RequestParam(value = "state", defaultValue = "open", required = true) String state,
            @Parameter(description = "Fetch all details of the moderation request.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails
    ) throws TException, URISyntaxException, ResourceClassNotFoundException {
        List<String> stateOptions = new ArrayList<>();
        stateOptions.add("open");
        stateOptions.add("closed");
        if (!stateOptions.contains(state)) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid ModerationRequest state '%s', possible values are: %s", state, stateOptions));
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean stateOpen = stateOptions.get(0).equalsIgnoreCase(state);
        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                sw360ModerationRequestService.getRequestsByState(sw360User, pageable, stateOpen, allDetails);
        return getModerationResponseEntity(pageable, request, allDetails, modRequestsWithPageData);
    }

    private @NotNull HalResource<ModerationRequest> createHalModerationRequestWithAllDetails(
            ModerationRequest moderationRequest, User sw360User) throws TException {
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(moderationRequest);
        User requestingUser = restControllerHelper.getUserByEmail(moderationRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, "requestingUser");

        if (CommonUtils.isNotNullEmptyOrWhitespace(moderationRequest.getReviewer())) {
            User reviewer = restControllerHelper.getUserByEmail(moderationRequest.getReviewer());
            restControllerHelper.addEmbeddedUser(halModerationRequest, reviewer, "reviewer");
        }

        DocumentType documentType = moderationRequest.getDocumentType();
        String documentId = moderationRequest.getDocumentId();
        if (documentType.equals(DocumentType.PROJECT)) {
            Project project = projectService.getProjectForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedProject(halModerationRequest, project, true);
        } else if (documentType.equals(DocumentType.RELEASE)) {
            Release release = releaseService.getReleaseForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedRelease(halModerationRequest, release);
        } else if (documentType.equals(DocumentType.COMPONENT)) {
            Component component = componentService.getComponentForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedComponent(halModerationRequest, component);
        }

        return halModerationRequest;
    }

    private @NotNull HalResource<ModerationRequest> createHalModerationRequest(ModerationRequest moderationRequest) {
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(moderationRequest);
        User requestingUser = restControllerHelper.getUserByEmail(moderationRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, "requestingUser");

        return halModerationRequest;
    }

    @Operation(
            summary = "Action on moderation request.",
            description = "Accept or reject the moderation request, save the comment by the reviewer and send email " +
                    "notifications.",
            tags = {"Moderation Requests"},
            responses = {@ApiResponse(
                    responseCode = "200",
                    content = {@Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schemaProperties = {@SchemaProperty(
                                    name = "status",
                                    schema = @Schema(implementation = ModerationState.class)
                            )}
                    )}
            )}
    )
    @RequestMapping(value = MODERATION_REQUEST_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<HalResource<Map<String, String>>> updateModerationRequestById(
            @Parameter(description = "The id of the moderation request to be updated.")
            @PathVariable String id,
            @Parameter(description = "Action to be applied.")
            @RequestBody ModerationPatch patch
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ModerationRequest moderationRequest = sw360ModerationRequestService.getModerationRequestById(id);

        if (!isOpenModerationRequest(moderationRequest)) {
            throw new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED,
                    "Moderation request is already closed. Cannot perform operation.");
        }

        if (!moderationRequest.getModerators().contains(sw360User.getEmail())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Unable to perform operation, user not a moderator.");
        }

        ModerationState moderationStatus;
        switch (patch.getAction()) {
            case ACCEPT:
                moderationStatus = sw360ModerationRequestService.acceptRequest(moderationRequest, patch.getComment(),
                        sw360User);
                break;
            case REJECT:
                moderationStatus = sw360ModerationRequestService.rejectRequest(moderationRequest, patch.getComment(),
                        sw360User);
                break;
            case UNASSIGN:
                moderationStatus = sw360ModerationRequestService.removeMeFromModerators(moderationRequest, sw360User);
                break;
            case ASSIGN:
                moderationStatus = sw360ModerationRequestService.assignRequest(moderationRequest, sw360User);
                break;
            default:
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Action should be `" +
                                Arrays.asList(ModerationPatch.ModerationAction.values()) +
                                "`, '" + patch.getAction() + "' received.");
        }
        String requestResponse = moderationStatus.toString();
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", requestResponse);
        HalResource<Map<String, String>> responseResource = new HalResource<>(responseMap);
        Link requestLink = linkTo(ReleaseController.class).slash("api" + MODERATION_REQUEST_URL).slash(id)
                .withSelfRel();
        responseResource.add(requestLink);

        return new ResponseEntity<>(responseResource, HttpStatus.ACCEPTED);
    }

    @Override
    public @NotNull RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ModerationRequestController.class).slash("api" + MODERATION_REQUEST_URL).withRel("moderationRequests"));
        return resource;
    }

    /**
     * Add moderation request entity models to resources.
     *
     * @param moderationRequest          Moderation request to add
     * @param allDetails                 Fetching all details?
     * @param moderationRequestResources Resources list to add MR to
     */
    private void addModerationRequest(ModerationRequest moderationRequest, boolean allDetails,
                                      List<EntityModel<ModerationRequest>> moderationRequestResources) {
        EntityModel<ModerationRequest> embeddedModerationRequestResource;
        if (!allDetails) {
            ModerationRequest embeddedModerationRequest = restControllerHelper.convertToEmbeddedModerationRequest(moderationRequest);
            embeddedModerationRequestResource = EntityModel.of(embeddedModerationRequest);
        } else {
            embeddedModerationRequestResource = createHalModerationRequest(moderationRequest);
        }
        moderationRequestResources.add(embeddedModerationRequestResource);
    }

    @Operation(
            summary = "Get my submissions.",
            description = "Get moderation requests submitted by the user. The responses are sortable by fields " +
                    "\"timestamp\", \"documentName\" and \"moderationState\".",
            tags = {"Moderation Requests"}
    )
    @GetMapping(value = MODERATION_REQUEST_URL + "/mySubmissions")
    public ResponseEntity<CollectionModel<ModerationRequest>> getSubmissions(
            Pageable pageable, HttpServletRequest request
    ) throws TException, URISyntaxException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                sw360ModerationRequestService.getRequestsByRequestingUser(sw360User, pageable);
        return getModerationResponseEntity(pageable, request, false, modRequestsWithPageData);
    }

    /**
     * Generate a Response Entity for paginated moderation request list.
     * @param pageable   Pageable request
     * @param request    HTTP Request
     * @param allDetails Request with allDetails?
     * @param modRequestsWithPageData Map of pagination data and moderation request list
     * @return Returns the Response Entity with pagination data.
     */
    @NotNull
    private ResponseEntity<CollectionModel<ModerationRequest>> getModerationResponseEntity(
            Pageable pageable, HttpServletRequest request, boolean allDetails,
            Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData
    ) throws ResourceClassNotFoundException, URISyntaxException {
        List<ModerationRequest> moderationRequests = new ArrayList<>();
        int totalCount = 0;
        if (!CommonUtils.isNullOrEmptyMap(modRequestsWithPageData)) {
            PaginationData paginationData = modRequestsWithPageData.keySet().iterator().next();
            moderationRequests = modRequestsWithPageData.get(paginationData);
            totalCount = (int) paginationData.getTotalRowCount();
        }

        PaginationResult<ModerationRequest> paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                request, pageable, moderationRequests, SW360Constants.TYPE_MODERATION, totalCount);

        List<EntityModel<ModerationRequest>> moderationRequestResources = new ArrayList<>();
        paginationResult.getResources().forEach(m -> {
            ModerationRequest filteredModerationRequest;
            if (allDetails) {
                filteredModerationRequest = filterModerationRequestNoDuplicates(m);
            } else {
                filteredModerationRequest = m;
            }
            addModerationRequest(filteredModerationRequest, allDetails, moderationRequestResources);
        });

        CollectionModel<ModerationRequest> resources;
        if (moderationRequestResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(ModerationRequest.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, moderationRequestResources);
        }
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    /**
     * Filter moderation request to remove duplicate additions and deletions data.
     * @param moderationRequest Moderation request to filter
     * @return Filtered moderation request
     */
    private @NotNull ModerationRequest filterModerationRequestNoDuplicates(@NotNull ModerationRequest moderationRequest) {
        ModerationRequest filteredModerationRequest = new ModerationRequest(moderationRequest);
        if (filteredModerationRequest.getProjectAdditions() != null &&
                filteredModerationRequest.getProjectDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getProjectAdditions,
                    ModerationRequest::getProjectDeletions, filteredModerationRequest::setProjectAdditions,
                    filteredModerationRequest::setProjectDeletions, Project._Fields.values(),
                    Project._Fields.ID
            );
        }
        if (filteredModerationRequest.getReleaseAdditions() != null &&
                filteredModerationRequest.getReleaseDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getReleaseAdditions,
                    ModerationRequest::getReleaseDeletions, filteredModerationRequest::setReleaseAdditions,
                    filteredModerationRequest::setReleaseDeletions, Release._Fields.values(),
                    Release._Fields.ID
            );
        }
        if (filteredModerationRequest.getComponentAdditions() != null &&
                filteredModerationRequest.getComponentDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getComponentAdditions,
                    ModerationRequest::getComponentDeletions, filteredModerationRequest::setComponentAdditions,
                    filteredModerationRequest::setComponentDeletions, Component._Fields.values(),
                    Component._Fields.ID
            );
        }
        if (filteredModerationRequest.getLicenseAdditions() != null &&
                filteredModerationRequest.getLicenseDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getLicenseAdditions,
                    ModerationRequest::getLicenseDeletions, filteredModerationRequest::setLicenseAdditions,
                    filteredModerationRequest::setLicenseDeletions, License._Fields.values(),
                    License._Fields.ID
            );
        }
        if (filteredModerationRequest.getPackageInfoAdditions() != null &&
                filteredModerationRequest.getPackageInfoDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getPackageInfoAdditions,
                    ModerationRequest::getPackageInfoDeletions, filteredModerationRequest::setPackageInfoAdditions,
                    filteredModerationRequest::setPackageInfoDeletions, PackageInformation._Fields.values(),
                    PackageInformation._Fields.ID
            );
        }
        if (filteredModerationRequest.getSPDXDocumentAdditions() != null &&
                filteredModerationRequest.getSPDXDocumentDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getSPDXDocumentAdditions,
                    ModerationRequest::getSPDXDocumentDeletions, filteredModerationRequest::setSPDXDocumentAdditions,
                    filteredModerationRequest::setSPDXDocumentDeletions, SPDXDocument._Fields.values(),
                    SPDXDocument._Fields.ID
            );
        }
        if (filteredModerationRequest.getDocumentCreationInfoAdditions() != null &&
                filteredModerationRequest.getDocumentCreationInfoDeletions() != null) {
            handleDuplicates(
                    filteredModerationRequest, ModerationRequest::getDocumentCreationInfoAdditions,
                    ModerationRequest::getDocumentCreationInfoDeletions,
                    filteredModerationRequest::setDocumentCreationInfoAdditions,
                    filteredModerationRequest::setDocumentCreationInfoDeletions,
                    DocumentCreationInformation._Fields.values(),
                    DocumentCreationInformation._Fields.ID
            );
        }
        return filteredModerationRequest;
    }

    /**
     * Go through all fields and set null if values are duplicate in additions and deletions. The function will ignore
     * the field with idField.
     * @param moderationRequest Moderation request to filter
     * @param getAddition       Function to get additions data in MR
     * @param getDeletion       Function to get deletions data in MR
     * @param setAddition       Function to set additions data in MR
     * @param setDeletion       Function to set deletions data in MR
     * @param fields            Fields of the object
     * @param idField           Field to ignore
     * @param <T>               Type of object
     * @param <F>               Type of field
     */
    private <T extends org.apache.thrift.TBase<T, F>, F extends org.apache.thrift.TFieldIdEnum> void handleDuplicates(
            @NotNull ModerationRequest moderationRequest,
            @NotNull Function<ModerationRequest, T> getAddition,
            @NotNull Function<ModerationRequest, T> getDeletion,
            @NotNull Function<T, ModerationRequest> setAddition,
            @NotNull Function<T, ModerationRequest> setDeletion,
            F @NotNull [] fields,
            F idField
    ) {
        T addition = getAddition.apply(moderationRequest);
        T deletion = getDeletion.apply(moderationRequest);

        for (F field : fields) {
            if (field == idField) {
                continue;
            }

            if ((addition.isSet(field) || deletion.isSet(field)) &&
                    (addition.getFieldValue(field) != null) &&
                    addition.getFieldValue(field).equals(deletion.getFieldValue(field))) {
                addition.setFieldValue(field, null);
                deletion.setFieldValue(field, null);
            }
        }

        setAddition.apply(addition);
        setDeletion.apply(deletion);
    }
}
