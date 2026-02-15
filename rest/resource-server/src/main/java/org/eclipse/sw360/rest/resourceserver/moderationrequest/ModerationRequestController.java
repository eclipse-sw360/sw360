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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
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
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

import static org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService.isOpenModerationRequest;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ModerationRequestController implements RepresentationModelProcessor<RepositoryLinksResource> {

    private static final String REVIEWER = "reviewer";
    private static final String REQUESTING_USER = "requestingUser";
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

    @Operation(
            summary = "List all of the service's moderation requests.",
            description = "List all of the service's moderation requests.",
            tags = {"Moderation Requests"}
    )
    @GetMapping(value = MODERATION_REQUEST_URL)
    public ResponseEntity<CollectionModel<ModerationRequest>> getModerationRequests(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(description = "Fetch all details of the moderation request")
            @RequestParam(value = "allDetails", required = false) boolean allDetails
    ) throws TException, ResourceClassNotFoundException, URISyntaxException, PaginationParameterException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<ModerationRequest> moderationRequests = sw360ModerationRequestService.getRequestsByModerator(sw360User, pageable);

        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                new HashMap<>();
        PaginationData paginationData = new PaginationData();
        paginationData.setTotalRowCount(sw360ModerationRequestService.getTotalCountByModerationStateAndRequestingUser(sw360User,sw360User));
        modRequestsWithPageData.put(paginationData, moderationRequests);

        return getModerationResponseEntity(pageable, request, allDetails, modRequestsWithPageData);
    }

    @Operation(
            summary = "Get a single moderation request.",
            description = "Get a single moderation request by id.",
            tags = {"Moderation Requests"}
    )
    @GetMapping(value = MODERATION_REQUEST_URL + "/{id}")
    public ResponseEntity<HalResource<Map<String, Object>>> getModerationRequestById(
            @Parameter(description = "The id of the moderation request to be retrieved.")
            @PathVariable String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        ModerationRequest moderationRequest = filterModerationRequestNoDuplicates(
                sw360ModerationRequestService.getModerationRequestById(id));
        Map<String, Object> modObjectMapper = getModObjectMapper(moderationRequest);
        Link modLink = linkTo(ReleaseController.class).slash("api/moderationrequest/" + moderationRequest.getId())
                .withSelfRel();
        Map<String, Object> modLinkMap = new LinkedHashMap<>();
        modLinkMap.put("self", modLink);
        modObjectMapper.put("_links", modLinkMap);
        HalResource<Map<String, Object>> halModerationRequest = createHalModerationRequestWithAllDetails(
                modObjectMapper, sw360User);
        HttpStatus status = halModerationRequest.getContent() == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halModerationRequest, status);
    }

    private Map<String, Object> getModObjectMapper(ModerationRequest moderationRequest) {
        ObjectMapper oMapper = new ObjectMapper();
        oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        oMapper.setDefaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)
        );
        oMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE); // but only public getters
        oMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE); // and none of "is-setters"
        return oMapper.convertValue(moderationRequest, Map.class);
    }

    @Operation(
            summary = "Get moderation based on state.",
            description = "List all the ModerationRequest visible to the user based on the state and  respond with MR where user is a moderator",
            tags = {"Moderation Requests"}
    )
    @GetMapping(value = MODERATION_REQUEST_URL + "/byState")
    public ResponseEntity<CollectionModel<ModerationRequest>> getModerationRequestsByState(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(
                    description = "The moderation request state of the request.",
                    schema = @Schema(allowableValues = {"open", "closed"})
            )
            @RequestParam(value = "state", defaultValue = "open", required = true) String state,
            @Parameter(description = "Fetch all details of the moderation request.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails
    ) throws TException, URISyntaxException, ResourceClassNotFoundException, PaginationParameterException {
        List<String> stateOptions = new ArrayList<>();
        stateOptions.add("open");
        stateOptions.add("closed");
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        if (!stateOptions.contains(state)) {
            throw new BadRequestClientException(String.format(
                    "Invalid ModerationRequest state '%s', possible values are: %s", state, stateOptions));
        }

        boolean stateOpen = "open".equalsIgnoreCase(state);
        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                sw360ModerationRequestService.getRequestsByState(sw360User, pageable, stateOpen, allDetails);
        return getModerationResponseEntity(pageable, request, allDetails, modRequestsWithPageData);
    }

    private @NotNull HalResource<Map<String, Object>> createHalModerationRequestWithAllDetails(
            Map<String, Object> modObjectMapper, User sw360User
    ) throws TException {
        HalResource<Map<String, Object>> halModerationRequest = new HalResource<>(modObjectMapper);
        User requestingUser = restControllerHelper.getUserByEmail(modObjectMapper.get(REQUESTING_USER).toString());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, REQUESTING_USER);
        if (modObjectMapper.get(REVIEWER) != null
                && CommonUtils.isNotNullEmptyOrWhitespace(modObjectMapper.get(REVIEWER).toString())) {
            User reviewer = restControllerHelper.getUserByEmail(modObjectMapper.get(REVIEWER).toString());
            restControllerHelper.addEmbeddedUser(halModerationRequest, reviewer, REVIEWER);
        }
        String documentType = modObjectMapper.get("documentType").toString();
        String documentId = modObjectMapper.get("documentId").toString();
        if (documentType.equals((DocumentType.PROJECT).toString())) {
            Project project = projectService.getProjectForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedProject(halModerationRequest, project, true);
        } else if (documentType.equals((DocumentType.RELEASE).toString())) {
            Release release = releaseService.getReleaseForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedRelease(halModerationRequest, release);
        } else if (documentType.equals((DocumentType.COMPONENT).toString())) {
            Component component = componentService.getComponentForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedComponent(halModerationRequest, component);
        }
        return halModerationRequest;
    }

    private @NotNull HalResource<ModerationRequest> createHalModerationRequest(ModerationRequest moderationRequest) {
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(moderationRequest);
        User requestingUser = restControllerHelper.getUserByEmail(moderationRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, REQUESTING_USER);
        return halModerationRequest;
    }

    @Operation(
            summary = "Action on moderation request.",
            description = "Perform actions on the moderation request, save the comment by the reviewer and send email " +
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
    @PatchMapping(value = MODERATION_REQUEST_URL + "/{id}")
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
            case POSTPONE:
                moderationStatus = sw360ModerationRequestService.postponeRequest(moderationRequest, patch.getComment());
                break;
            default:
                throw new BadRequestClientException(
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
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request
    ) throws TException, URISyntaxException, ResourceClassNotFoundException, PaginationParameterException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
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
    ) throws ResourceClassNotFoundException, URISyntaxException, PaginationParameterException {
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

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Validate Moderation Request",
            description = "This endpoint validates a user for a moderation request.",
            tags = {"Moderation Requests"}
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", description = "Moderation request not required.",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(
                            value = "{\"message\": \"User can write to the entity. MR is not required.\"}"
                        ))
                }
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "Moderation request will be required.",
                    content = {
                        @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                value = "{\"message\": \"User allowed to perform write on entity. MR is required.\"}"
                            ))
                    }
                ),
            @ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid input or missing parameters.",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(
                            value = "{\"message\": \"Invalid input or missing required parameters.\"}"
                        ))
                }
            ),
            @ApiResponse(
                responseCode = "401", description = "Unauthorized - User does not have the required permissions.",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(
                            value = "{\"message\": \"User is not authorized to perform this action.\"}"
                        ))
                }
            ),
            @ApiResponse(
                responseCode = "403", description = "Forbidden - Access denied.",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(
                            value = "{\"message\": \"Access is denied due to insufficient permissions.\"}"
                        ))
                }
            ),
            @ApiResponse(
                responseCode = "500", description = "Internal Server Error.",
                content = {
                    @Content(mediaType = "application/json",
                        examples = @ExampleObject(
                            value = "{\"message\": \"An unexpected error occurred while validating the moderation request.\"}"
                        ))
                }
            )
        })
    @PostMapping(value = MODERATION_REQUEST_URL + "/validate")
    public ResponseEntity<String> validateModerationRequest(
            @Parameter(description = "Entity type", example = "PROJECT",
                    schema = @Schema(allowableValues = {"PROJECT", "COMPONENT", "RELEASE"}))
            @RequestParam String entityType,
            @Parameter(description = "Entity id.")
            @RequestParam String entityId
    ) throws SW360Exception {
        try {
            User user = restControllerHelper.getSw360UserFromAuthentication();
            Object entity = getEntityByTypeAndId(entityType, entityId, user);
            if (entity == null) {
                throw new ResourceNotFoundException("Entity not found for the given ID: " + entityId);
            }
            boolean isWriteActionAllowed = restControllerHelper.isWriteActionAllowed(entity, user);
            if (!isWriteActionAllowed) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                        "User allowed to perform write on entity. MR is required.");
            }
            return ResponseEntity.ok("User can write to the entity. MR is not required.");
        } catch (SW360Exception ex) {
            throw new ResourceNotFoundException("Entity not found for the given ID: " + entityId);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestClientException("Invalid entity type provided: " + ex.getMessage());
        } catch (TException ex) {
            throw new SW360Exception("An error occurred while processing the request: " + ex.getMessage());
        }
    }

    /**
     * Helper method to fetch entity by type and ID.
     */
    private Object getEntityByTypeAndId(String entityType, String entityId, User user) throws TException {
        try {
            switch (entityType.toLowerCase()) {
                case "project":
                    return projectService.getProjectForUserById(entityId, user);
                case "component":
                    return componentService.getComponentForUserById(entityId, user);
                case "release":
                    return releaseService.getReleaseForUserById(entityId, user);
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
        } catch (TTransportException e) {
            throw new RuntimeException("Unable to connect to the service. Please check the server status.", e);
        }
    }
    @Operation(
            summary = "Delete moderation request.",
            description = "Delete delete moderation request of the service.",
            tags = {"Moderation Requests"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @DeleteMapping(value = MODERATION_REQUEST_URL + "/delete")
    public ResponseEntity<?> deleteModerationRequest(
            @Parameter(description = "List of moderation request IDs to delete")
            @RequestBody List<String> ids
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<RequestStatus> requestStatusList = new ArrayList<>();
        List<String> incorrectIds = new ArrayList<>();
        List<String> correctIds = new ArrayList<>();
        List<String> deletedIds = new ArrayList<>();

        for (String id : ids) {
            try {
                ModerationRequest moderationRequest = sw360ModerationRequestService.getModerationRequestById(id);
                RequestStatus requestStatus = sw360ModerationRequestService.deleteModerationRequestInfo(sw360User, id,
                        moderationRequest);
                requestStatusList.add(requestStatus);
                if (requestStatus == RequestStatus.SUCCESS) {
                    deletedIds.add(id);
                } else {
                    correctIds.add(id);
                }
            } catch (ResourceNotFoundException ex) {
                incorrectIds.add(id);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", deletedIds);
        response.put("incorrect", incorrectIds);
        response.put("correct", correctIds);

        if (!requestStatusList.isEmpty()) {
            if (requestStatusList.contains(RequestStatus.FAILURE)) {
                response.put("message", "User doesn't have permission to delete.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else if (requestStatusList.contains(RequestStatus.SUCCESS) && requestStatusList.contains(null)) {
                response.put("message", "Some requests were deleted, but some are in an open state.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            } else if (requestStatusList.contains(RequestStatus.SUCCESS) && incorrectIds.isEmpty() && correctIds.isEmpty()) {
                response.put("message", "All specified moderation requests were successfully deleted.");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else if (requestStatusList.contains(null)) {
                response.put("message", "MR is in open state or user don't have permission.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        }

        if (!incorrectIds.isEmpty() && !correctIds.isEmpty()) {
            response.put("message", "Some moderation requests are invalid or open.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (incorrectIds.isEmpty() && correctIds.isEmpty() && !deletedIds.isEmpty()) {
            response.put("message", "All specified moderation requests were successfully deleted.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else if (!incorrectIds.isEmpty()) {
            response.put("message", "Some moderation requests are invalid.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if (!correctIds.isEmpty()) {
            response.put("message", "Some moderation requests are in an open state.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else {
            response.put("message", "No valid moderation requests found.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
