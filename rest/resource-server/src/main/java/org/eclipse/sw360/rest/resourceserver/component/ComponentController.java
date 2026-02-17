/*
 * Copyright Siemens AG, 2017-2019.
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

import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.thrift.RestrictedResource;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.VerificationStateInfo;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentDTO;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelationDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityState;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestExceptionHandler;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Strings.isNullOrEmpty;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ComponentController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String COMPONENTS_URL = "/components";
    private static final Logger log = LogManager.getLogger(ComponentController.class);
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT = ImmutableMap.<String, String>builder()
            .put("message", "Unauthorized user or empty commit message passed.").build();

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360VulnerabilityService vulnerabilityService;

    @Operation(
            summary = "List all of the service's components.",
            description = "List all of the service's components.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL)
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getComponents(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Name of the component to filter")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Categories of the component to filter, as a comma separated list.")
            @RequestParam(value = "categories", required = false) String categories,
            @Parameter(description = "Type of the component to filter",
                    schema = @Schema(implementation = ComponentType.class))
            @RequestParam(value = "type", required = false) String componentType,
            @Parameter(description = "Component languages to filter, as a comma separated list.")
            @RequestParam(value = "languages", required = false) String languages,
            @Parameter(description = "Software Platforms to filter, as a comma separated list.")
            @RequestParam(value = "softwarePlatforms", required = false) String softwarePlatforms,
            @Parameter(description = "Operating Systems to filter, as a comma separated list.")
            @RequestParam(value = "operatingSystems", required = false) String operatingSystems,
            @Parameter(description = "Vendors to filter, as a comma separated list.")
            @RequestParam(value = "vendors", required = false) String vendors,
            @Parameter(description = "Main Licenses to filter, as a comma separated list.")
            @RequestParam(value = "mainLicenses", required = false) String mainLicenses,
            @Parameter(description = "Created by user to filter (email).")
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @Parameter(description = "Date component was created on (YYYY-MM-DD).",
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(value = "createdOn", required = false) String createdOn,
            @Parameter(description = "Properties which should be present for each component in the result")
            @RequestParam(value = "fields", required = false) List<String> fields,
            @Parameter(description = "Flag to get components with all details.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            @Parameter(description = "Use lucenesearch to filter the components.")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        Map<PaginationData, List<Component>> paginatedComponents = null;

        Map<String, Set<String>> filterMap = getFilterMap(categories, componentType, languages, softwarePlatforms,
                operatingSystems, vendors, mainLicenses, createdBy, createdOn);
        if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            Set<String> values = Collections.singleton(name);
            filterMap.put(Component._Fields.NAME.getFieldName(), values);
        }
        if (luceneSearch) {
            if (filterMap.containsKey(Component._Fields.NAME.getFieldName())) {
                Set<String> values = filterMap.get(Component._Fields.NAME.getFieldName()).stream()
                        .map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(Component._Fields.NAME.getFieldName(), values);
            }
            paginatedComponents = componentService.refineSearch(filterMap, sw360User, pageable);
        } else {
            if (filterMap.isEmpty()) {
                paginatedComponents = componentService.getRecentComponentsSummaryWithPagination(sw360User, pageable);
            } else {
                paginatedComponents = componentService.searchComponentByExactValues(filterMap, sw360User, pageable);
            }
        }

        PaginationResult<Component> paginationResult;
        List<Component> allComponents = new ArrayList<>(paginatedComponents.values().iterator().next());
        int totalCount = Math.toIntExact(paginatedComponents.keySet().stream()
                .findFirst().map(PaginationData::getTotalRowCount).orElse(0L));
        paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                request, pageable, allComponents, SW360Constants.TYPE_COMPONENT, totalCount);

        CollectionModel<EntityModel<Component>> resources = getFilteredComponentResources(fields, allDetails, sw360User, paginationResult);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private CollectionModel getFilteredComponentResources(
            List<String> fields, boolean allDetails, User sw360User, PaginationResult<Component> paginationResult
    ) throws URISyntaxException {
        List<EntityModel<Component>> componentResources = new ArrayList<>();
        Consumer<Component> consumer = c -> {
            EntityModel<Component> embeddedComponentResource = null;
            if (!allDetails) {
                Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c, fields);
                embeddedComponentResource = EntityModel.of(embeddedComponent);
            } else {
                try {
                    embeddedComponentResource = createHalComponent(c, sw360User);
                } catch (TException e) {
                    throw new RuntimeException(e);
                }
                if (embeddedComponentResource == null) {
                    return;
                }
            }
            componentResources.add(embeddedComponentResource);
        };

        paginationResult.getResources().forEach(consumer);

        CollectionModel resources;
        if (componentResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(Component.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, componentResources);
        }
        return resources;
    }

    @Operation(
            summary = "Get all the resources where the component is used.",
            description = "Get all the resources where the component is used.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/usedBy" + "/{id}")
    public ResponseEntity<CollectionModel<EntityModel>> getUsedByResourceDetails(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication(); // Project
        Set<Project> sw360Projects = componentService.getProjectsByComponentId(id, user);
        Set<Component> sw360Components = componentService.getUsingComponentsForComponent(id, user);

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
        restrictedResource.setProjects(componentService.countProjectsByComponentId(id, user) - sw360Projects.size());
        resources.add(EntityModel.of(restrictedResource));

        CollectionModel<EntityModel> finalResources = CollectionModel.of(resources);
        return new ResponseEntity(finalResources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get a single component.",
            description = "Get a single component by its id.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{id}")
    public ResponseEntity<EntityModel<Component>> getComponent(
            @Parameter(description = "The id of the component to be retrieved.")
            @PathVariable("id") String id
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component sw360Component = componentService.getComponentForUserById(id, user);
        HalResource<Component> userHalResource = createHalComponent(sw360Component, user);
        restControllerHelper.addEmbeddedDataToComponent(userHalResource, sw360Component);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get recently created components.",
            description = "Return 5 of the service's most recently created components.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/recentComponents")
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getRecentComponent() throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<Component> sw360Components = componentService.getRecentComponents(user);

        List<EntityModel<Component>> resources = new ArrayList<>();
        sw360Components.forEach(c -> {
            Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c);
            resources.add(EntityModel.of(embeddedComponent));
        });

        CollectionModel<EntityModel<Component>> finalResources = CollectionModel.of(resources);
        return new ResponseEntity<>(finalResources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get subscribed components.",
            description = "List all of the service's mysubscriptions components.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/mySubscriptions")
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getMySubscriptions() throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<Component> sw360Components = componentService.getComponentSubscriptions(user);

        List<EntityModel<Component>> resources = new ArrayList<>();
        sw360Components.forEach(c -> {
            Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c);
            resources.add(EntityModel.of(embeddedComponent));
        });

        CollectionModel<EntityModel<Component>> finalResources = restControllerHelper.createResources(resources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @Operation(
            summary = "Toggle user subscription to a component",
            description = "Subscribes or unsubscribes the user to a specified component based on their current subscription status.",
            tags = {"Components"}
    )
    @PostMapping(value = COMPONENTS_URL + "/{id}/subscriptions")
    public ResponseEntity<String> toggleComponentSubscription(
            @Parameter(description = "The ID of the component.")
            @PathVariable("id") String componentId
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component componentById = componentService.getComponentForUserById(componentId, user);
        Set<String> subscribers = componentById.getSubscribers();

        boolean isSubscribed = subscribers.contains(user.getEmail());

        if (isSubscribed) {
            componentService.unsubscribeComponent(componentId, user);
            return new ResponseEntity<>("Successfully unsubscribed from the component.", HttpStatus.OK);
        } else {
            componentService.subscribeComponent(componentId, user);
            return new ResponseEntity<>("Successfully subscribed to the component.", HttpStatus.OK);
        }
    }

    @Operation(
            summary = "Get components by external ID.",
            description = "Get components by external ID.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/searchByExternalIds")
    public ResponseEntity<CollectionModel<EntityModel<Component>>> searchByExternalIds(
            @Parameter(
                    description = "The external IDs of the components to be retrieved.",
                    example = "component-id-key=1831A3&component-id-key=c77321"
            )
            HttpServletRequest request
    ) throws TException {
        String queryString = request.getQueryString();
        return restControllerHelper.searchByExternalIds(queryString, componentService, null);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update an existing component.",
            description = "Partially update an existing component. Only provided fields will be updated. " +
                         "If the user lacks direct write access, a moderation request will be created. " +
                         "Include a 'comment' field in the request body when submitting moderation requests.",
            tags = {"Components"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Component updated successfully"),
                    @ApiResponse(responseCode = "202", description = "Moderation request created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or missing required fields"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Write access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Component not found")
            }
    )
    @PatchMapping(value = COMPONENTS_URL + "/{id}")
    public ResponseEntity<EntityModel<Component>> patchComponent(
            @Parameter(description = "The id of the component to be updated.")
            @PathVariable("id") String id,
            @Parameter(description = "Updated component fields. Add 'comment' field in body for moderation request.")
            @RequestBody ComponentDTO updateComponentDto
    ) throws TException {
        final User user = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(user);
        Component sw360Component = validateAndGetComponent(id, updateComponentDto, user);
        String comment = extractModerationComment(updateComponentDto);
        if (!restControllerHelper.isWriteActionAllowed(sw360Component, user)
                && (comment == null || comment.isBlank())) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        user.setCommentMadeDuringModerationRequest(comment);

        if (updateComponentDto.getAttachments() != null && !updateComponentDto.getAttachments().isEmpty()) {
            updateComponentDto.getAttachments().forEach(attachment ->
                wrapSW360Exception(() -> attachmentService.fillCheckedAttachmentData(attachment, user))
            );
        }

        sw360Component = restControllerHelper.updateComponent(sw360Component, updateComponentDto);
        RequestStatus updateComponentStatus = componentService.updateComponent(sw360Component, user);

        if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }

        HalResource<Component> halResource = createHalComponent(sw360Component, user);
        return ResponseEntity.ok(halResource);
    }

    private String extractModerationComment(ComponentDTO updateComponentDto) {
        try {
            String comment = updateComponentDto.getComment();
            return (comment != null && !comment.isBlank()) ? comment.trim() : null;
        } catch (Exception e) {
            log.debug("Comment field not available in ComponentDTO: {}", e.getMessage());
            return null;
        }
    }


    private Component validateAndGetComponent(String id, ComponentDTO updateComponentDto, User user) {
        if (isNullOrEmpty(id)) {
            throw new BadRequestClientException("Component ID cannot be null or empty");
        }

        Component sw360Component;
        try {
            sw360Component = componentService.getComponentForUserById(id, user);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Component not found with ID: " + id);
        }

        if (sw360Component == null) {
            throw new ResourceNotFoundException("Component not found with ID: " + id);
        }

        if (updateComponentDto == null) {
            throw new BadRequestClientException("Component data cannot be null");
        }

        return sw360Component;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Delete existing components.",
            description = "Delete existing components by ids.",
            tags = {"Components"}
    )
    @DeleteMapping(value = COMPONENTS_URL + "/{ids}")
    public ResponseEntity<List<MultiStatus>> deleteComponents(
            @Parameter(description = "The ids of the components to be deleted.")
            @PathVariable("ids") List<String> idsToDelete,
            @Parameter(description = "Comment message")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        if (StringUtils.isNotEmpty(comment)) {
            user.setCommentMadeDuringModerationRequest(comment);
        }
        List<MultiStatus> results = new ArrayList<>();
        for(String id:idsToDelete) {
            RequestStatus requestStatus = componentService.deleteComponent(id, user);
            if(requestStatus == RequestStatus.SUCCESS) {
                results.add(new MultiStatus(id, HttpStatus.OK));
            } else if(requestStatus == RequestStatus.IN_USE) {
                results.add(new MultiStatus(id, HttpStatus.CONFLICT));
            } else if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
                results.add(new MultiStatus(id, HttpStatus.ACCEPTED));
            } else {
                results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
            }
        }
        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Create a new component.",
            description = "Create a new component.",
            tags = {"Components"}
    )
    @PostMapping(value = COMPONENTS_URL)
    public ResponseEntity<EntityModel<Component>> createComponent(
            @Parameter(description = "The component to be created.")
            @RequestBody Component component
    ) throws URISyntaxException, TException {

        User user = restControllerHelper.getSw360UserFromAuthentication();
        if(component.getComponentType() == null) {
            throw new BadRequestClientException("Required field componentType is not present");
        }

        if (component.getVendorNames() != null) {
            Set<String> vendors = new HashSet<>();
            for (String vendorUriString : component.getVendorNames()) {
                URI vendorURI = new URI(vendorUriString);
                String path = vendorURI.getPath();
                String vendorId = path.substring(path.lastIndexOf('/') + 1);
                Vendor vendor = vendorService.getVendorById(vendorId);
                String vendorFullName = vendor.getFullname();
                vendors.add(vendorFullName);
            }
            component.setVendorNames(vendors);
        }

        if (CommonUtils.isNullEmptyOrWhitespace(component.getBusinessUnit())) {
            component.setBusinessUnit(user.getDepartment());
        }

        Component sw360Component = componentService.createComponent(component, user);
        HalResource<Component> halResource = createHalComponent(sw360Component, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Component.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(
            summary = "Get all attachment information of a component.",
            description = "Get all attachment information of a component.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{id}/attachments")
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getComponentAttachments(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Component sw360Component = componentService.getComponentForUserById(id, sw360User);
        final CollectionModel<EntityModel<Attachment>> resources = attachmentService.getAttachmentResourcesFromList(sw360User, sw360Component.getAttachments(), Source.componentId(id));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get all releases of a component.",
            description = "Get all releases of a component.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{id}/releases")
    public ResponseEntity<CollectionModel<ReleaseLink>> getReleaseLinksByComponentId(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final List<ReleaseLink> releaseLinks = componentService.convertReleaseToReleaseLink(id, sw360User);
        CollectionModel<ReleaseLink> resources = CollectionModel.of(releaseLinks);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update an attachment info.",
            description = "Update attachment info a component.",
            tags = {"Components"}
    )
    @PatchMapping(value = COMPONENTS_URL + "/{id}/attachment/{attachmentId}")
    public ResponseEntity<EntityModel<Attachment>> patchComponentAttachmentInfo(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id,
            @Parameter(description = "The id of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            @Parameter(description = "The attachment info to be updated.")
            @RequestBody Attachment attachmentData,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment

    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component sw360Component = componentService.getComponentForUserById(id, sw360User);
        Set<Attachment> attachments = sw360Component.getAttachments();
        sw360User.setCommentMadeDuringModerationRequest(comment);
        Attachment updatedAttachment = attachmentService.updateAttachment(attachments, attachmentData, attachmentId,
                sw360User);
        RequestStatus updateComponentStatus = componentService.updateComponent(sw360Component, sw360User);
        if (!restControllerHelper.isWriteActionAllowed(sw360Component, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        } else {
            if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
                return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
            }
            EntityModel<Attachment> attachmentResource = EntityModel.of(updatedAttachment);
            return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
        }
    }

    @Operation(
            summary = "Create new attachment for a component.",
            description = "Create new attachment for a component.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Updated component.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = Component.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "202", description = "Request sent for moderation.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Moderation request is created\"}"
                                            ))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Failed to upload attachment."
                    )
            },
            tags = {"Components"}
    )
    @PostMapping(value = COMPONENTS_URL + "/{componentId}/attachments", consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("componentId") String componentId,
            @Parameter(description = "The file to be uploaded.")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "The attachment info to be created.")
            @RequestPart("attachment") Attachment newAttachment,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component component = componentService.getComponentForUserById(componentId, sw360User);
        sw360User.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(component, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        Attachment attachment = null;
        try {
            attachment = attachmentService.uploadAttachment(file, newAttachment, sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        component.addToAttachments(attachment);
        RequestStatus updateComponentStatus = componentService.updateComponent(component, sw360User);
        HalResource halComponent = createHalComponent(component, sw360User);
        if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halComponent, HttpStatus.OK);
    }

    @Operation(
            summary = "Download an attachment of a component.",
            description = "Download an attachment of a component.",
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true, example = "application/*"),
            },
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{componentId}/attachments/{attachmentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("componentId") String componentId,
            @Parameter(description = "The id of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Component component = componentService.getComponentForUserById(componentId, sw360User);
        attachmentService.downloadAttachmentWithContext(component, attachmentId, response, sw360User);
    }

    @Operation(
            summary = "Download the attachment bundle of a component.",
            description = "Download the attachment bundle of a component.",
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true, example = "application/zip"),
            },
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{componentId}/attachments/download", produces="application/zip")
    public void downloadAttachmentBundleFromComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("componentId") String componentId,
            HttpServletResponse response
    ) throws TException, IOException {
        final User user = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(user);
        final Component component = componentService.getComponentForUserById(componentId, user);
        Set<Attachment> attachments = component.getAttachments();
        attachmentService.downloadAttachmentBundleWithContext(component, attachments, user, response);
    }

    @Operation(
            summary = "Delete one or multiple attachments of a component.",
            description = "Delete one or multiple attachments from a component.\n\n" +
                    "Note that attachments can only be deleted if they are not used by a project.\n" +
                    "Requests that cannot delete any of the attachments specified fail with response\n" +
                    "status 500.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Attachments deleted successfully"),
                    @ApiResponse(responseCode = "202", description = "Deletion sent for moderation"),
                    @ApiResponse(responseCode = "500", description = "Attachment in use, can't delete")
            },
            tags = {"Components"}
    )
    @DeleteMapping(COMPONENTS_URL + "/{componentId}/attachments/{attachmentIds}")
    public ResponseEntity<HalResource<Component>> deleteAttachmentsFromComponent(
            @PathVariable("componentId") String componentId,
            @PathVariable("attachmentIds") List<String> attachmentIds,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component component = componentService.getComponentForUserById(componentId, user);
        user.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(component, user) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        Set<Attachment> attachmentsToDelete = attachmentService.filterAttachmentsToRemove(Source.componentId(componentId),
                component.getAttachments(), attachmentIds);
        if (attachmentsToDelete.isEmpty()) {
            // let the whole action fail if nothing can be deleted
            throw new RuntimeException("Could not delete attachments " + attachmentIds + " from component " + componentId);
        }
        log.debug("Deleting the following attachments from component " + componentId + ": " + attachmentsToDelete);
        component.getAttachments().removeAll(attachmentsToDelete);
        RequestStatus updateComponentStatus = componentService.updateComponent(component, user);
        HalResource<Component> halComponent = createHalComponent(component, user);
        if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halComponent, HttpStatus.OK);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ComponentController.class).slash("api/components").withRel("components"));
        return resource;
    }

    @Operation(
            summary = "Get vulnerabilities of a single component.",
            description = "Get vulnerabilities of a single component.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<VulnerabilityDTO>> getVulnerabilitiesOfComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<VulnerabilityDTO> allVulnerabilityDTOs = componentService.getVulnerabilitiesByComponent(id, user);
        CollectionModel<VulnerabilityDTO> resources = CollectionModel.of(allVulnerabilityDTOs);
        return new ResponseEntity<>(resources,HttpStatus.OK);
    }

    private HalResource<Component> createHalComponent(Component sw360Component, User user) throws TException {
        HalResource<Component> halComponent = new HalResource<>(sw360Component);
        User componentCreator = restControllerHelper.getUserByEmail(sw360Component.getCreatedBy());

        if (sw360Component.getReleaseIds() != null) {
            Set<String> releases = sw360Component.getReleaseIds();
            restControllerHelper.addEmbeddedReleases(halComponent, releases, releaseService, user);
        }

        if (sw360Component.getReleases() != null) {
            List<Release> releases = sw360Component.getReleases();
            restControllerHelper.addEmbeddedReleases(halComponent, releases);
        }

        if (sw360Component.getModerators() != null) {
            Set<String> moderators = sw360Component.getModerators();
            restControllerHelper.addEmbeddedModerators(halComponent, moderators);
        }

        if (!isNullOrEmpty(sw360Component.getDefaultVendorId())) {
            Vendor defaultVendor;
            if (sw360Component.getDefaultVendor() == null
                    || !sw360Component.getDefaultVendor().getId().equals(sw360Component.getDefaultVendorId())) {
                defaultVendor = vendorService.getVendorById(sw360Component.getDefaultVendorId());
            } else {
                defaultVendor = sw360Component.getDefaultVendor();
            }
            addEmbeddedDefaultVendor(halComponent, defaultVendor);
            // delete default vendor so that this object is not put directly in the
            // component object (it is available in the embedded resources path though)
            // but keep id so that this field is put directly into the component json
            sw360Component.setDefaultVendor(null);
        }

        if (sw360Component.getVendorNames() != null) {
            Set<String> vendors = sw360Component.getVendorNames();
            restControllerHelper.addEmbeddedVendors(halComponent, vendors);
            sw360Component.setVendorNames(null);
        }

        if (sw360Component.getAttachments() != null) {
            restControllerHelper.addEmbeddedAttachments(halComponent, sw360Component.getAttachments());
        }

        if(null!= componentCreator)
            restControllerHelper.addEmbeddedUser(halComponent, componentCreator, "createdBy");

        return halComponent;
    }

    private void addEmbeddedDefaultVendor(HalResource<Component> halComponent, Vendor defaultVendor) {
        HalResource<Vendor> halDefaultVendor = new HalResource<>(defaultVendor);
        Link vendorSelfLink = linkTo(UserController.class)
                .slash("api" + VendorController.VENDORS_URL + "/" + defaultVendor.getId()).withSelfRel();
        halDefaultVendor.add(vendorSelfLink);
        halComponent.addEmbeddedResource("defaultVendor", halDefaultVendor);
    }

    @Operation(
            summary = "Get all components associated to the user.",
            description = "Get all components associated to the user.",
            tags = {"Components"}
    )
    @GetMapping(value = COMPONENTS_URL + "/mycomponents")
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getMyComponents(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Component> sw360Components = componentService.getMyComponentsForUser(sw360User);
        PaginationResult<Component> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                sw360Components, SW360Constants.TYPE_COMPONENT);
        List<EntityModel<Component>> componentResources = new ArrayList<>();

        paginationResult.getResources().stream().forEach(c -> {
            Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c, null);
            EntityModel<Component> embeddedComponentResource = EntityModel.of(embeddedComponent);
            if (embeddedComponentResource == null) {
                return;
            }
            componentResources.add(embeddedComponentResource);
        });

        CollectionModel<EntityModel<Component>> finalResources = restControllerHelper.generatePagesResource(
                paginationResult, componentResources);
        HttpStatus status = finalResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(finalResources, status);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update the vulnerability of a component.",
            description = "Update the vulnerability of a component.",
            tags = {"Components"}
    )
    @PatchMapping(value = COMPONENTS_URL + "/{id}/vulnerabilities")
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> patchReleaseVulnerabilityRelation(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String componentId,
            @Parameter(description = "The vulnerability state to be updated.")
            @RequestBody VulnerabilityState vulnerabilityState
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();

        checkRequireReleaseVulnerabilityRelation(vulnerabilityState);
        Set<ReleaseVulnerabilityRelationDTO> releaseVulnerabilityRelationDTOsFromRequest = vulnerabilityState.getReleaseVulnerabilityRelationDTOs();
        Map<String,List<VulnerabilityDTO>> releaseIdsWithVulnerabilityDTOsActual = getReleaseIdsWithVulnerabilityDTOsActual(componentId, user);

        Map<String,Set<String>> releaseIdsWithExternalIdsFromRequest = new HashMap<>();
        Map<String, List<VulnerabilityDTO>> releaseVulnerabilityRelations = new HashMap<>();
        getReleaseIdsWithExternalIdsFromRequest(releaseIdsWithExternalIdsFromRequest, releaseVulnerabilityRelations, releaseIdsWithVulnerabilityDTOsActual, releaseVulnerabilityRelationDTOsFromRequest);
        if (validateReleaseVulnerabilityRelationDTO(releaseIdsWithExternalIdsFromRequest, vulnerabilityState)) {
            throw new BadRequestClientException("ReleaseVulnerabilityRelationDTO is not valid");
        }

        RequestStatus requestStatus = null;
        int countRequestStatus = 0;
        for (Map.Entry<String, List<VulnerabilityDTO>> entry : releaseVulnerabilityRelations.entrySet()) {
            for (VulnerabilityDTO vulnerabilityDTO: entry.getValue()) {
                requestStatus = updateReleaseVulnerabilityRelation(entry.getKey(), user, vulnerabilityState,vulnerabilityDTO.getExternalId());
                if (requestStatus != RequestStatus.SUCCESS) {
                    countRequestStatus ++;
                    break;
                }
            }
            if (countRequestStatus != 0){
                break;
            }
        }
        if (requestStatus == RequestStatus.ACCESS_DENIED){
            throw new AccessDeniedException("User not allowed!");
        }

        final List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = getVulnerabilityResources(releaseIdsWithExternalIdsFromRequest);
        CollectionModel<EntityModel<VulnerabilityDTO>> resources = restControllerHelper.createResources(vulnerabilityResources);
        HttpStatus status = resources == null ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    private void checkRequireReleaseVulnerabilityRelation(VulnerabilityState vulnerabilityState) {
        if(CommonUtils.isNullOrEmptyCollection(vulnerabilityState.getReleaseVulnerabilityRelationDTOs())) {
            throw new BadRequestClientException("Required field ReleaseVulnerabilityRelation is not present");
        }
        if(vulnerabilityState.getVerificationState() == null) {
            throw new BadRequestClientException("Required field verificationState is not present");
        }
    }

    private Map<String,List<VulnerabilityDTO>> getReleaseIdsWithVulnerabilityDTOsActual(String componentId, User user) throws TException {
        Map<String,List<VulnerabilityDTO>> releaseIdsWithVulnerabilityDTOsActual = new HashMap<>();
        List<String> releaseIds = componentService.getReleaseIdsFromComponentId(componentId, user);

        for (String releaseId: releaseIds) {
            List<VulnerabilityDTO> vulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, user);
            releaseIdsWithVulnerabilityDTOsActual.put(releaseId,vulnerabilityDTOs);
        }
        return releaseIdsWithVulnerabilityDTOsActual;
    }

    private void getReleaseIdsWithExternalIdsFromRequest(Map<String,Set<String>> releaseIdsWithExternalIdsFromRequest, Map<String, List<VulnerabilityDTO>> releaseVulnerabilityRelations,
                 Map<String,List<VulnerabilityDTO>> releaseIdsWithVulnerabilityDTOsActual, Set<ReleaseVulnerabilityRelationDTO> releaseVulnerabilityRelationDTOsFromRequest) {
        for (Map.Entry<String, List<VulnerabilityDTO>> entry : releaseIdsWithVulnerabilityDTOsActual.entrySet()) {
            List<VulnerabilityDTO> vulnerabilityDTOs = new ArrayList<>();
            Set<String> externalIds = new HashSet<>();
            entry.getValue().forEach(vulnerabilityDTO -> {
                releaseVulnerabilityRelationDTOsFromRequest.forEach(releaseVulnerabilityRelationDTO -> {
                    if (vulnerabilityDTO.getExternalId().equals(releaseVulnerabilityRelationDTO.getExternalId())
                            && vulnerabilityDTO.getIntReleaseName().equals(releaseVulnerabilityRelationDTO.getReleaseName())) {
                        externalIds.add(releaseVulnerabilityRelationDTO.getExternalId());
                        vulnerabilityDTOs.add(vulnerabilityDTO);
                    }
                });
            });
            if (CommonUtils.isNullOrEmptyCollection(externalIds) || CommonUtils.isNullOrEmptyCollection(vulnerabilityDTOs)){
                continue;
            }
            releaseIdsWithExternalIdsFromRequest.put(entry.getKey(),externalIds);
            releaseVulnerabilityRelations.put(entry.getKey(),vulnerabilityDTOs);
        }
    }

    private List<EntityModel<VulnerabilityDTO>> getVulnerabilityResources(Map<String,Set<String>> releaseIdsWithExternalIdsFromRequest) {
        List<VulnerabilityDTO> vulnerabilityDTOList = new ArrayList<>();
        for (Map.Entry<String, Set<String>> releaseIdsWithExternalIds: releaseIdsWithExternalIdsFromRequest.entrySet()) {
            vulnerabilityDTOList.addAll(vulnerabilityService.getVulnerabilityDTOByExternalId(releaseIdsWithExternalIds.getValue(), releaseIdsWithExternalIds.getKey()));
        }
        List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = new ArrayList<>();
        vulnerabilityDTOList.forEach(dto->{
            EntityModel<VulnerabilityDTO> vulnerabilityDTOEntityModel = EntityModel.of(dto);
            vulnerabilityResources.add(vulnerabilityDTOEntityModel);
        });
        return vulnerabilityResources ;
    }

    private RequestStatus updateReleaseVulnerabilityRelation(String releaseId, User user, VulnerabilityState vulnerabilityState, String externalIdRequest) throws TException {
        List<VulnerabilityDTO> vulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId, user);
        ReleaseVulnerabilityRelation releaseVulnerabilityRelation = new ReleaseVulnerabilityRelation();
        for (VulnerabilityDTO vulnerabilityDTO: vulnerabilityDTOs) {
            if (vulnerabilityDTO.getExternalId().equals(externalIdRequest)) {
                releaseVulnerabilityRelation = vulnerabilityDTO.getReleaseVulnerabilityRelation();
            }
        }
        ReleaseVulnerabilityRelation relation = updateReleaseVulnerabilityRelationFromRequest(releaseVulnerabilityRelation, vulnerabilityState, user);
        return vulnerabilityService.updateReleaseVulnerabilityRelation(relation,user);
    }

    private static ReleaseVulnerabilityRelation updateReleaseVulnerabilityRelationFromRequest(ReleaseVulnerabilityRelation dbRelation, VulnerabilityState vulnerabilityState, User user) {
        if (!dbRelation.isSetVerificationStateInfo()) {
            dbRelation.setVerificationStateInfo(new ArrayList<>());
        }
        VerificationStateInfo verificationStateInfo = new VerificationStateInfo();
        List<VerificationStateInfo> verificationStateHistory = dbRelation.getVerificationStateInfo();

        verificationStateInfo.setCheckedBy(user.getEmail());
        verificationStateInfo.setCheckedOn(SW360Utils.getCreatedOn());
        verificationStateInfo.setVerificationState(vulnerabilityState.getVerificationState());
        verificationStateInfo.setComment(vulnerabilityState.getComment());

        verificationStateHistory.add(verificationStateInfo);
        dbRelation.setVerificationStateInfo(verificationStateHistory);
        return dbRelation;
    }

    private boolean validateReleaseVulnerabilityRelationDTO(Map<String,Set<String>> releaseIdsWithExternalIdsFromRequest, VulnerabilityState vulnerabilityState ) {
        long countExternalIdsActual = 0;
        for (Map.Entry<String, Set<String>> releaseIdWithVulnerabilityId: releaseIdsWithExternalIdsFromRequest.entrySet()){
            countExternalIdsActual += releaseIdWithVulnerabilityId.getValue().stream().count();
        }
        long countExternalIdsFromRequest = vulnerabilityState.getReleaseVulnerabilityRelationDTOs().stream().count();
        return countExternalIdsActual != countExternalIdsFromRequest;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Import SBOM in SPDX format.",
            description = "Import SBOM in SPDX format.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Import successful.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ImportBomRequestPreparation.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Failed to upload attachment."
                    )
            },
            tags = {"Components"}
    )
    @PostMapping(value = COMPONENTS_URL + "/import/SBOM", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> importSBOM(
            @Parameter(description = "Type of SBOM being uploaded.",
                    schema = @Schema(type = "string", allowableValues = {"SPDX"})
            )
            @RequestParam(value = "type", required = true) String type,
            @Parameter(description = "The file to be uploaded.")
            @RequestBody MultipartFile file
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Attachment attachment;
        final RequestSummary requestSummary;
        if(!type.equalsIgnoreCase("SPDX") || !attachmentService.isValidSbomFile(file, type)) {
            throw new IllegalArgumentException("SBOM file is not valid. It currently only supports SPDX(.rdf/.spdx) files.");
        }
        try {
            attachment = attachmentService.uploadAttachment(file, new Attachment(), sw360User);
            try {
                requestSummary = componentService.importSBOM(sw360User, attachment.getAttachmentContentId());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }
        String releaseId = requestSummary.getMessage();
        if (!(requestSummary.getRequestStatus() == RequestStatus.SUCCESS && CommonUtils.isNotNullEmptyOrWhitespace(releaseId))) {
            throw new BadRequestClientException("Invalid SBOM file");
        }
        Release release = componentService.getReleaseById(requestSummary.getMessage(),sw360User);
        Component component = componentService.getComponentForUserById(release.getComponentId(),sw360User);
        HttpStatus status = HttpStatus.OK;
        HalResource<Component> halResource = createHalComponent(component, sw360User);
        return new ResponseEntity<>(halResource, status);
    }

    @Operation(
            summary = "Import SBOM in SPDX format.",
            description = "Import SBOM in SPDX format.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Import successful.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ImportBomRequestPreparation.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Failed to upload attachment."
                    )
            },
            tags = {"Components"}
    )
    @PostMapping(value = COMPONENTS_URL + "/prepareImport/SBOM", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> prepareImportSBOM(
            @Parameter(description = "Type of SBOM being uploaded.",
                    schema = @Schema(type = "string", allowableValues = {"SPDX"})
            )
            @RequestParam(value = "type", required = true) String type,
            @Parameter(description = "The file to be uploaded.")
            @RequestBody MultipartFile file
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Attachment attachment;
        final ImportBomRequestPreparation importBomRequestPreparation;
        if(!type.equalsIgnoreCase("SPDX") || !attachmentService.isValidSbomFile(file, type)) {
            throw new IllegalArgumentException("SBOM file is not valid. It currently only supports SPDX(.rdf/.spdx) files.");
        }
        try {
            attachment = attachmentService.uploadAttachment(file, new Attachment(), sw360User);
            try {
                importBomRequestPreparation = componentService.prepareImportSBOM(sw360User, attachment.getAttachmentContentId());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }
        ImportBomRequestPreparation importBomRequestPreparationResponse = handleImportBomRequestPreparation(importBomRequestPreparation);
        return new ResponseEntity<>(importBomRequestPreparationResponse, HttpStatus.OK);
    }

    private @NotNull ImportBomRequestPreparation handleImportBomRequestPreparation(ImportBomRequestPreparation importBomRequestPreparation) {
        ImportBomRequestPreparation importBomRequestPreparationResponse = new ImportBomRequestPreparation();
        if (importBomRequestPreparation.isComponentDuplicate && importBomRequestPreparation.isReleaseDuplicate) {
            importBomRequestPreparationResponse.setMessage("The Component and Release existed !");
        } else {
            importBomRequestPreparationResponse.setComponentsName(importBomRequestPreparation.getComponentsName());
            importBomRequestPreparationResponse.setReleasesName(importBomRequestPreparation.getReleasesName());
        }
        return importBomRequestPreparationResponse;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Merge two components.",
            description = "Merge source component into target component.",
            tags = {"Components"}
    )
    @PatchMapping(value = COMPONENTS_URL + "/mergecomponents")
    public ResponseEntity<RequestStatus> mergeComponents(
            @Parameter(description = "The id of the merge target component.")
            @RequestParam(value = "mergeTargetId", required = true) String mergeTargetId,
            @Parameter(description = "The id of the merge source component.")
            @RequestParam(value = "mergeSourceId", required = true) String mergeSourceId,
            @Parameter(description = "The merge selection.",
                    schema = @Schema(
                            implementation = ComponentMergeSelector.class,
                            type = "object",
                            example = """
                                    {
                                      "name": "Final Component Name",
                                      "createdOn": "Final created date",
                                      "createdBy": "Final creator name",
                                      "attachments": [
                                        {
                                          "attachmentContentId": "att1",
                                          "filename": "saveme.txt"
                                        }
                                      ]
                                    }
                                    """,
                            requiredProperties = {"name", "createdOn", "createdBy"}
                    )
            )
            @RequestBody ComponentMergeSelector mergeSelection
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        // perform the real merge, update merge target and delete merge sources
        RequestStatus requestStatus = componentService.mergeComponents(mergeTargetId, mergeSourceId, mergeSelection, sw360User);

        return new ResponseEntity<>(requestStatus, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Split two components.",
            description = "Split source component into target component.",
            tags = {"Components"},
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Request completed successfully."
                    ),
                    @ApiResponse(
                            responseCode = "404", description = "Source or target component not found.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "403", description = "Don't have permission to perform the action.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409", description = "Source or target component has a Moderation Request open.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Internal server error.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))
                            }
                    )
            }
    )
    @PatchMapping(value = COMPONENTS_URL + "/splitComponents")
    public ResponseEntity<RequestStatus> splitComponents(
            @Parameter(description = "Source and target components.",
                    schema = @Schema(
                            type = "object",
                            properties = {
                                    @StringToClassMapItem(key = "srcComponent", value = ComponentDTO.class),
                                    @StringToClassMapItem(key = "targetComponent", value = ComponentDTO.class),
                            },
                            example = """
                                    {
                                      "srcComponent": {
                                        "id": "comp1",
                                        "name": "Component1",
                                        "releaseIds": [
                                          "rel1",
                                          "rel2"
                                        ],
                                        "attachments": [
                                          {
                                            "attachmentContentId": "att1",
                                            "filename": "f1"
                                          }
                                        ]
                                      },
                                      "targetComponent": {
                                        "id": "comp2",
                                        "name": "Component2",
                                        "releaseIds": [
                                          "rel3",
                                          "rel4"
                                        ],
                                        "attachments": [
                                          {
                                            "attachmentContentId": "att2",
                                            "filename": "f2"
                                          }
                                        ]
                                      }
                                    }
                                    """,
                            requiredProperties = {"srcComponent", "targetComponent"}
                    )
            )
            @RequestBody Map<String, ComponentDTO> componentMap
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ComponentDTO srcComponentDTO = componentMap.get("srcComponent");
        ComponentDTO targetComponentDTO = componentMap.get("targetComponent");

        Component srcComponent = restControllerHelper.convertToComponent(srcComponentDTO);
        Component targetComponent = restControllerHelper.convertToComponent(targetComponentDTO);

        srcComponent.setReleases(componentService.getReleasesFromDto(srcComponentDTO, sw360User));
        targetComponent.setReleases(componentService.getReleasesFromDto(targetComponentDTO, sw360User));

        // perform the real merge, update merge target and delete merge source
        RequestStatus requestStatus = componentService.splitComponents(srcComponent, targetComponent, sw360User);

        return new ResponseEntity<>(requestStatus, HttpStatus.OK);
    }

    /**
     * Create a map of filters with the field name in the key and expected value in the value (as set).
     * @return Filter map from the user's request.
     */
    private @NonNull Map<String, Set<String>> getFilterMap(
            String categories, String componentType, String languages, String softwarePlatforms,
            String operatingSystems, String vendors, String mainLicenses, String createdBy, String createdOn
    ) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(categories)) {
            filterMap.put(Component._Fields.CATEGORIES.getFieldName(), CommonUtils.splitToSet(categories));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(componentType)) {
            filterMap.put(Component._Fields.COMPONENT_TYPE.getFieldName(), CommonUtils.splitToSet(componentType));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(languages)) {
            filterMap.put(Component._Fields.LANGUAGES.getFieldName(), CommonUtils.splitToSet(languages));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(softwarePlatforms)) {
            filterMap.put(Component._Fields.SOFTWARE_PLATFORMS.getFieldName(), CommonUtils.splitToSet(softwarePlatforms));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(operatingSystems)) {
            filterMap.put(Component._Fields.OPERATING_SYSTEMS.getFieldName(), CommonUtils.splitToSet(operatingSystems));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(vendors)) {
            filterMap.put(Component._Fields.VENDOR_NAMES.getFieldName(), CommonUtils.splitToSet(vendors));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(mainLicenses)) {
            filterMap.put(Component._Fields.MAIN_LICENSE_IDS.getFieldName(), CommonUtils.splitToSet(mainLicenses));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(createdBy)) {
            filterMap.put(Component._Fields.CREATED_BY.getFieldName(), CommonUtils.splitToSet(createdBy));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(createdOn)) {
            filterMap.put(Component._Fields.CREATED_ON.getFieldName(), CommonUtils.splitToSet(createdOn));
        }
        return filterMap;
    }

    /**
     * Create a filter predicate to remove all components which do not satisfy the restriction set.
     * @param restrictions Restrictions set to filter components on
     * @return Filter predicate for stream.
     */
    private static @NonNull Predicate<Component> filterComponentMap(Map<String, Set<String>> restrictions) {
        return component -> {
            for (Map.Entry<String, Set<String>> restriction : restrictions.entrySet()) {
                final Set<String> filterSet = restriction.getValue();
                Component._Fields field = Component._Fields.findByName(restriction.getKey());
                Object fieldValue = component.getFieldValue(field);
                if (fieldValue == null) {
                    return false;
                }
                if (field == Component._Fields.COMPONENT_TYPE && !filterSet.contains(component.componentType.name())) {
                    return false;
                } else if ((field == Component._Fields.CREATED_BY || field == Component._Fields.CREATED_ON)
                        && !fieldValue.toString().equalsIgnoreCase(filterSet.iterator().next())) {
                    return false;
                } else if (fieldValue instanceof Set) {
                    if (Sets.intersection(filterSet, (Set<String>) fieldValue).isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        };
    }
}
