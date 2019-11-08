/*
 * Copyright Siemens AG, 2017-2019.
 * Copyright Bosch Software Innovations GmbH, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.component;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import static com.google.common.base.Strings.isNullOrEmpty;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ComponentController implements ResourceProcessor<RepositoryLinksResource> {

    public static final String COMPONENTS_URL = "/components";
    private static final Logger log = Logger.getLogger(ComponentController.class);

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
    private final RestControllerHelper<Component> restControllerHelper;

    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources> getComponents(Pageable pageable,
                                                                        @RequestParam(value = "name", required = false) String name,
                                                                        @RequestParam(value = "type", required = false) String componentType,
                                                                        @RequestParam(value = "fields", required = false) List<String> fields,
                                                                        HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        List<Component> allComponents = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            allComponents.addAll(componentService.searchComponentByName(name));
        } else {
            allComponents.addAll(componentService.getComponentsForUser(sw360User));
        }

        PaginationResult<Component> paginationResult = restControllerHelper.createPaginationResult(request, pageable, allComponents, SW360Constants.TYPE_COMPONENT);

        List<Resource<Component>> componentResources = new ArrayList<>();
        paginationResult.getResources().stream()
                .filter(component -> componentType == null || (component.isSetComponentType() && componentType.equals(component.componentType.name())))
                .forEach(c -> {
                    Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c, fields);
                    componentResources.add(new Resource<>(embeddedComponent));
                });

        Resources resources;
        if (componentResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Component.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, componentResources);
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL + "/usedBy" + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getUsedByResourceDetails(@PathVariable("id") String id)
            throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication(); // Project
        Set<Project> sw360Projects = componentService.getProjectsByComponentId(id, user);
        Set<Component> sw360Components = componentService.getUsingComponentsForComponent(id, user);

        List<Resource<Object>> resources = new ArrayList<>();
        sw360Projects.stream().forEach(p -> {
            Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
            resources.add(new Resource<>(embeddedProject));
        });

        sw360Components.stream()
                .forEach(c -> {
                    Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c);
                    resources.add(new Resource<>(embeddedComponent));
                });

        Resources<Resource> finalResources = new Resources(resources);
        return new ResponseEntity(finalResources, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<Component>> getComponent(
            @PathVariable("id") String id) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component sw360Component = componentService.getComponentForUserById(id, user);
        HalResource<Component> userHalResource = createHalComponent(sw360Component, user);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL + "/searchByExternalIds", method = RequestMethod.GET)
    public ResponseEntity searchByExternalIds(@RequestParam MultiValueMap<String, String> externalIdsMultiMap) throws TException {
        return restControllerHelper.searchByExternalIds(externalIdsMultiMap, componentService, null);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = COMPONENTS_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<Resource<Component>> patchComponent(
            @PathVariable("id") String id,
            @RequestBody Component updateComponent) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Component sw360Component = componentService.getComponentForUserById(id, user);
        sw360Component = this.restControllerHelper.updateComponent(sw360Component, updateComponent);
        componentService.updateComponent(sw360Component, user);
        HalResource<Component> userHalResource = createHalComponent(sw360Component, user);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = COMPONENTS_URL + "/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity<List<MultiStatus>> deleteComponents(
            @PathVariable("ids") List<String> idsToDelete) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<MultiStatus> results = new ArrayList<>();
        for(String id:idsToDelete) {
            RequestStatus requestStatus = componentService.deleteComponent(id, user);
            if(requestStatus == RequestStatus.SUCCESS) {
                results.add(new MultiStatus(id, HttpStatus.OK));
            } else if(requestStatus == RequestStatus.IN_USE) {
                results.add(new MultiStatus(id, HttpStatus.CONFLICT));
            } else {
                results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
            }
        }
        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.POST)
    public ResponseEntity<Resource<Component>> createComponent(@RequestBody Component component) throws URISyntaxException, TException {

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

        Component sw360Component = componentService.createComponent(component, user);
        HalResource<Component> halResource = createHalComponent(sw360Component, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Component.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @RequestMapping(value = COMPONENTS_URL + "/{id}/attachments", method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<Attachment>>> getComponentAttachments(
            @PathVariable("id") String id) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component sw360Component = componentService.getComponentForUserById(id, sw360User);
        final Resources<Resource<Attachment>> resources = attachmentService.getResourcesFromList(sw360Component.getAttachments());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL + "/{componentId}/attachments", method = RequestMethod.POST, consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToComponent(@PathVariable("componentId") String componentId,
                                                                @RequestPart("file") MultipartFile file,
                                                                @RequestPart("attachment") Attachment newAttachment) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        Attachment attachment;
        try {
            attachment = attachmentService.uploadAttachment(file, newAttachment, sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        final Component component = componentService.getComponentForUserById(componentId, sw360User);
        component.addToAttachments(attachment);
        componentService.updateComponent(component, sw360User);

        final HalResource halRelease = createHalComponent(component, sw360User);

        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @RequestMapping(value = COMPONENTS_URL + "/{componentId}/attachments/{attachmentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromComponent(
            @PathVariable("componentId") String componentId,
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Component component = componentService.getComponentForUserById(componentId, sw360User);
        attachmentService.downloadAttachmentWithContext(component, attachmentId, response, sw360User);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ComponentController.class).slash("api/components").withRel("components"));
        return resource;
    }

    private HalResource<Component> createHalComponent(Component sw360Component, User user) throws TException {
        HalResource<Component> halComponent = new HalResource<>(sw360Component);

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
                    || sw360Component.getDefaultVendor().getId() != sw360Component.getDefaultVendorId()) {
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

        restControllerHelper.addEmbeddedUser(halComponent, userService.getUserByEmail(sw360Component.getCreatedBy()), "createdBy");

        return halComponent;
    }

    private void addEmbeddedDefaultVendor(HalResource<Component> halComponent, Vendor defaultVendor) {
        HalResource<Vendor> halDefaultVendor = new HalResource<>(defaultVendor);
        Link vendorSelfLink = linkTo(UserController.class)
                .slash("api" + VendorController.VENDORS_URL + "/" + defaultVendor.getId()).withSelfRel();
        halDefaultVendor.add(vendorSelfLink);
        halComponent.addEmbeddedResource("defaultVendor", halDefaultVendor);
    }
}
