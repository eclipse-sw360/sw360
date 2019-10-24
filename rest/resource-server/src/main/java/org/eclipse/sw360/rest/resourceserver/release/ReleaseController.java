/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.release;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String RELEASES_URL = "/releases";
    private static final Logger log = Logger.getLogger(ReleaseController.class);

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private Sw360AttachmentService attachmentService;

    @NonNull
    private RestControllerHelper restControllerHelper;

    @RequestMapping(value = RELEASES_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getReleasesForUser(
            @RequestParam(value = "sha1", required = false) String sha1,
            @RequestParam(value = "fields", required = false) List<String> fields) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> sw360Releases = new ArrayList<>();

        if (sha1 != null && !sha1.isEmpty()) {
            sw360Releases.add(searchReleaseBySha1(sha1, sw360User));
        } else {
            sw360Releases.addAll(releaseService.getReleasesForUser(sw360User));
        }

        List<Resource> releaseResources = new ArrayList<>();
        for (Release sw360Release : sw360Releases) {
            Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(sw360Release, fields);
            Resource<Release> releaseResource = new Resource<>(embeddedRelease);
            releaseResources.add(releaseResource);
        }
        Resources<Resource> resources = new Resources<>(releaseResources);

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private Release searchReleaseBySha1(String sha1, User sw360User) throws TException {
        AttachmentInfo sw360AttachmentInfo = attachmentService.getAttachmentBySha1(sha1);
        return releaseService.getReleaseForUserById(sw360AttachmentInfo.getOwner().getReleaseId(), sw360User);
    }

    @RequestMapping(value = RELEASES_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getRelease(
            @PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        HalResource halRelease = createHalReleaseResource(sw360Release, true);
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/usedBy" + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource>> getUsedByResourceDetails(@PathVariable("id") String id)
            throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication(); // Project
        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> sw360Projects = releaseService.getProjectsByRelease(id, user);
        Set<org.eclipse.sw360.datahandler.thrift.components.Component> sw360Components = releaseService.getUsingComponentsForRelease(id, user);

        List<Resource> resources = new ArrayList<>();
        sw360Projects.stream().forEach(p -> {
            Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
            resources.add(new Resource<>(embeddedProject));
        });

        sw360Components.stream()
                .forEach(c -> {
                    Component embeddedComponent = restControllerHelper.convertToEmbeddedComponent(c);
                    resources.add(new Resource<>(embeddedComponent));
                });

        Resources<Resource<Object>> finalResources = new Resources(resources);
        return new ResponseEntity(finalResources, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/searchByExternalIds", method = RequestMethod.GET)
    public ResponseEntity searchByExternalIds(@RequestParam MultiValueMap<String, String> externalIdsMultiMap) throws TException {
        return restControllerHelper.searchByExternalIds(externalIdsMultiMap, releaseService, null);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL + "/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity<List<MultiStatus>> deleteReleases(
            @PathVariable("ids") List<String> idsToDelete) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<MultiStatus> results = new ArrayList<>();
        for(String id:idsToDelete) {
            RequestStatus requestStatus = releaseService.deleteRelease(id, user);
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
    @RequestMapping(value = RELEASES_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<Resource<Release>> patchComponent(
            @PathVariable("id") String id,
            @RequestBody Release updateRelease) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, user);
        sw360Release = this.restControllerHelper.updateRelease(sw360Release, updateRelease);
        releaseService.updateRelease(sw360Release, user);
        HalResource<Release> halRelease = createHalReleaseResource(sw360Release, true);
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = RELEASES_URL, method = RequestMethod.POST)
    public ResponseEntity<Resource<Release>> createRelease(
            @RequestBody Release release) throws URISyntaxException, TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

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

        Release sw360Release = releaseService.createRelease(release, sw360User);
        HalResource<Release> halResource = createHalReleaseResource(sw360Release, true);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Release.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @RequestMapping(value = RELEASES_URL + "/{id}/attachments", method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<Attachment>>> getReleaseAttachments(
            @PathVariable("id") String id) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Release sw360Release = releaseService.getReleaseForUserById(id, sw360User);
        final Resources<Resource<Attachment>> resources = attachmentService.getResourcesFromList(sw360Release.getAttachments());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/{releaseId}/attachments", method = RequestMethod.POST, consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToRelease(@PathVariable("releaseId") String releaseId,
                                                              @RequestPart("file") MultipartFile file,
                                                              @RequestPart("attachment") Attachment newAttachment) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        Attachment attachment;
        try {
            attachment = attachmentService.uploadAttachment(file, newAttachment, sw360User);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        final Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
        release.addToAttachments(attachment);
        releaseService.updateRelease(release, sw360User);

        final HalResource halRelease = createHalReleaseResource(release, true);

        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }

    @RequestMapping(value = RELEASES_URL + "/{releaseId}/attachments/{attachmentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromRelease(
            @PathVariable("releaseId") String releaseId,
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
        attachmentService.downloadAttachmentWithContext(release, attachmentId, response, sw360User);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ReleaseController.class).slash("api" + RELEASES_URL).withRel("releases"));
        return resource;
    }

    private HalResource<Release> createHalReleaseResource(Release release, boolean verbose) {
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
                HalResource<Vendor> vendorHalResource = restControllerHelper.addEmbeddedVendor(vendor.getFullname());
                halRelease.addEmbeddedResource("sw360:vendors", vendorHalResource);
                release.setVendor(null);
            }
            if (release.getMainLicenseIds() != null) {
                restControllerHelper.addEmbeddedLicenses(halRelease, release.getMainLicenseIds());
                release.setMainLicenseIds(null);
            }
        }
        return halRelease;
    }
}
