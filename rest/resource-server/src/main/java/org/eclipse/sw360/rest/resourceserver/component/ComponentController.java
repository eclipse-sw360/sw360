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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.thrift.RestrictedResource;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.VerificationStateInfo;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentDTO;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentDTO;
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
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ComponentController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String COMPONENTS_URL = "/components";
    private static final Logger log = LogManager.getLogger(ComponentController.class);
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();

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
    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getComponents(
            Pageable pageable,
            @Parameter(description = "Name of the component to filter")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Type of the component to filter")
            @RequestParam(value = "type", required = false) String componentType,
            @Parameter(description = "Properties which should be present for each component in the result")
            @RequestParam(value = "fields", required = false) List<String> fields,
            @Parameter(description = "Flag to get components with all details.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            @Parameter(description = "lucenesearch parameter to filter the components.")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        List<Component> allComponents = new ArrayList<>();
        String queryString = request.getQueryString();
        Map<String, String> params = restControllerHelper.parseQueryString(queryString);

        Map<String, Set<String>> filterMap = new HashMap<>();
        if (luceneSearch) {
            if (CommonUtils.isNotNullEmptyOrWhitespace(componentType)) {
                Set<String> values = CommonUtils.splitToSet(componentType);
                filterMap.put(Component._Fields.COMPONENT_TYPE.getFieldName(), values);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
                Set<String> values = CommonUtils.splitToSet(name);
                values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(Component._Fields.NAME.getFieldName(), values);
            }
            allComponents.addAll(componentService.refineSearch(filterMap, sw360User));
        } else {
            if (name != null && !name.isEmpty()) {
                allComponents.addAll(componentService.searchComponentByName(params.get("name")));
            } else {
                allComponents.addAll(componentService.getComponentsForUser(sw360User));
            }
        }

        PaginationResult<Component> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                allComponents, SW360Constants.TYPE_COMPONENT);

        CollectionModel resources = getFilteredComponentResources(componentType, fields, allDetails, luceneSearch,
                sw360User, paginationResult);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private CollectionModel getFilteredComponentResources(String componentType, List<String> fields, boolean allDetails,
            boolean luceneSearch, User sw360User, PaginationResult<Component> paginationResult)
            throws URISyntaxException {
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

        if (luceneSearch) {
            paginationResult.getResources().stream().forEach(consumer);
        } else {
            paginationResult.getResources().stream()
                    .filter(component -> componentType == null
                            || (component.isSetComponentType() && componentType.equals(component.componentType.name())))
                    .forEach(consumer);
        }

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
    @RequestMapping(value = COMPONENTS_URL + "/usedBy" + "/{id}", method = RequestMethod.GET)
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
    @RequestMapping(value = COMPONENTS_URL + "/{id}", method = RequestMethod.GET)
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
    @RequestMapping(value = COMPONENTS_URL + "/recentComponents", method = RequestMethod.GET)
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
    @RequestMapping(value = COMPONENTS_URL + "/mySubscriptions", method = RequestMethod.GET)
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
            summary = "Get components by external ID.",
            description = "Get components by external ID.",
            tags = {"Components"}
    )
    @RequestMapping(value = COMPONENTS_URL + "/searchByExternalIds", method = RequestMethod.GET)
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
            description = "Update an existing component by its id.",
            tags = {"Components"}
    )
    @RequestMapping(value = COMPONENTS_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Component>> patchComponent(
            @Parameter(description = "The id of the component to be updated.")
            @PathVariable("id") String id,
            @Parameter(description = "The component with updated fields.")
            @RequestBody ComponentDTO updateComponentDto
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component sw360Component = componentService.getComponentForUserById(id, user);
        sw360Component = this.restControllerHelper.updateComponent(sw360Component, updateComponentDto);
        Set<AttachmentDTO> attachmentDTOS = updateComponentDto.getAttachmentDTOs();
        if (!CommonUtils.isNullOrEmptyCollection(attachmentDTOS)) {
            Set<Attachment> attachments = new HashSet<>();
            for (AttachmentDTO attachmentDTO: attachmentDTOS) {
                attachments.add(restControllerHelper.convertToAttachment(attachmentDTO, user));
            }
            sw360Component.setAttachments(attachments);
        } else {
            sw360Component.setAttachments(null);
        }
        RequestStatus updateComponentStatus = componentService.updateComponent(sw360Component, user);
        HalResource<Component> userHalResource = createHalComponent(sw360Component, user);
        if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Delete existing components.",
            description = "Delete existing components by ids.",
            tags = {"Components"}
    )
    @RequestMapping(value = COMPONENTS_URL + "/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity<List<MultiStatus>> deleteComponents(
            @Parameter(description = "The ids of the components to be deleted.")
            @PathVariable("ids") List<String> idsToDelete
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
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
    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Component>> createComponent(
            @Parameter(description = "The component to be created.")
            @RequestBody Component component
    ) throws URISyntaxException, TException {

        User user = restControllerHelper.getSw360UserFromAuthentication();
        if(component.getComponentType() == null) {
            throw new HttpMessageNotReadableException("Required field componentType is not present");
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
    @RequestMapping(value = COMPONENTS_URL + "/{id}/attachments", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<AttachmentDTO>>> getComponentAttachments(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component sw360Component = componentService.getComponentForUserById(id, sw360User);
        final CollectionModel<EntityModel<AttachmentDTO>> resources = attachmentService.getAttachmentDTOResourcesFromList(sw360User, sw360Component.getAttachments(), Source.releaseId(sw360Component.getId()));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get all releases of a component.",
            description = "Get all releases of a component.",
            tags = {"Components"}
    )
    @RequestMapping(value = COMPONENTS_URL + "/{id}/releases", method = RequestMethod.GET)
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
    @RequestMapping(value = COMPONENTS_URL + "/{id}/attachment/{attachmentId}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Attachment>> patchComponentAttachmentInfo(
            @Parameter(description = "The id of the component.")
            @PathVariable("id") String id,
            @Parameter(description = "The id of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            @Parameter(description = "The attachment info to be updated.")
            @RequestBody Attachment attachmentData
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component sw360Component = componentService.getComponentForUserById(id, sw360User);
        Set<Attachment> attachments = sw360Component.getAttachments();
        Attachment updatedAttachment = attachmentService.updateAttachment(attachments, attachmentData, attachmentId, sw360User);
        RequestStatus updateComponentStatus = componentService.updateComponent(sw360Component, sw360User);
        if (updateComponentStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<Attachment> attachmentResource = EntityModel.of(updatedAttachment);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
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
    @RequestMapping(value = COMPONENTS_URL + "/{componentId}/attachments", method = RequestMethod.POST, consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("componentId") String componentId,
            @Parameter(description = "The file to be uploaded.")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "The attachment info to be created.")
            @RequestPart("attachment") Attachment newAttachment
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component component = componentService.getComponentForUserById(componentId, sw360User);
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
    @RequestMapping(value = COMPONENTS_URL + "/{componentId}/attachments/{attachmentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromComponent(
            @Parameter(description = "The id of the component.")
            @PathVariable("componentId") String componentId,
            @Parameter(description = "The id of the attachment.")
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
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
            @PathVariable("attachmentIds") List<String> attachmentIds) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component component = componentService.getComponentForUserById(componentId, user);

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
    @RequestMapping(value = COMPONENTS_URL + "/mycomponents", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Component>>> getMyComponents(
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
            throw new HttpMessageNotReadableException("ReleaseVulnerabilityRelationDTO is not valid");
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
            throw new HttpMessageNotReadableException("User not allowed!");
        }

        final List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = getVulnerabilityResources(releaseIdsWithExternalIdsFromRequest);
        CollectionModel<EntityModel<VulnerabilityDTO>> resources = restControllerHelper.createResources(vulnerabilityResources);
        HttpStatus status = resources == null ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    private void checkRequireReleaseVulnerabilityRelation(VulnerabilityState vulnerabilityState) {
        if(CommonUtils.isNullOrEmptyCollection(vulnerabilityState.getReleaseVulnerabilityRelationDTOs())) {
            throw new HttpMessageNotReadableException("Required field ReleaseVulnerabilityRelation is not present");
        }
        if(vulnerabilityState.getVerificationState() == null) {
            throw new HttpMessageNotReadableException("Required field verificationState is not present");
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
    @RequestMapping(value = COMPONENTS_URL + "/import/SBOM", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
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
        if(!type.equalsIgnoreCase("SPDX")) {
            throw new IllegalArgumentException("SBOM file type is not valid. It currently only supports SPDX(.rdf/.spdx) files.");
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
            return new ResponseEntity<>("Invalid SBOM file", HttpStatus.BAD_REQUEST);
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
    @RequestMapping(value = COMPONENTS_URL + "/prepareImport/SBOM", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
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
        if(!type.equalsIgnoreCase("SPDX")) {
            throw new IllegalArgumentException("SBOM file type is not valid. It currently only supports SPDX(.rdf/.spdx) files.");
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
        HttpStatus status = importBomRequestPreparationResponse != null ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(importBomRequestPreparationResponse, status);
    }

    private ImportBomRequestPreparation handleImportBomRequestPreparation(ImportBomRequestPreparation importBomRequestPreparation) {
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
    @RequestMapping(value = COMPONENTS_URL + "/mergecomponents", method = RequestMethod.PATCH)
    public ResponseEntity<RequestStatus> mergeComponents(
            @Parameter(description = "The id of the merge target component.")
            @RequestParam(value = "mergeTargetId", required = true) String mergeTargetId,
            @Parameter(description = "The id of the merge source component.")
            @RequestParam(value = "mergeSourceId", required = true) String mergeSourceId,
            @Parameter(description = "The merge selection.")
            @RequestBody Component mergeSelection
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
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Source and target components.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {@ExampleObject(
                                    value = "{\n" +
                                            "  \"srcComponent\": {\n" +
                                            "    ...\n" +
                                            "  },\n" +
                                            "  \"targetComponent\": {\n" +
                                            "    ...\n" +
                                            "  }\n" +
                                            "}"
                            )}
                    )
            ),
            tags = {"Components"}
    )
    @RequestMapping(value = COMPONENTS_URL + "/splitComponents", method = RequestMethod.PATCH)
    public ResponseEntity<RequestStatus> splitComponents(
            @Parameter(
                    description = "Source and target components.",
                    example = "{\n" +
                            "  \"srcComponent\": {\n" +
                            "    ...\n" +
                            "  },\n" +
                            "  \"targetComponent\": {\n" +
                            "    ...\n" +
                            "  }\n" +
                            "}"
            )
            @RequestBody Map<String, Component> componentMap
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        Component srcComponent = componentMap.get("srcComponent");
        Component targetComponent = componentMap.get("targetComponent");

        // perform the real merge, update merge target and delete merge source
        RequestStatus requestStatus = componentService.splitComponents(srcComponent, targetComponent, sw360User);

        return new ResponseEntity<>(requestStatus, HttpStatus.OK);
    }
}
