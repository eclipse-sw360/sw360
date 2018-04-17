/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.license.LicenseController;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RestControllerHelper {
    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final Sw360LicenseService licenseService;

    private static final Logger LOGGER = Logger.getLogger(RestControllerHelper.class);

    public User getSw360UserFromAuthentication(OAuth2Authentication oAuth2Authentication) {
        String userId = oAuth2Authentication.getName();
        return userService.getUserByEmail(userId);
    }

    public void addEmbeddedModerators(HalResource halResource, Set<String> moderators) {
        for (String moderatorEmail : moderators) {
            User user = new User();
            user.setEmail(moderatorEmail);
            addEmbeddedUser(halResource, user, "sw360:moderators");
        }
    }

    public void addEmbeddedReleases(
            HalResource halResource,
            Set<String> releases,
            Sw360ReleaseService sw360ReleaseService,
            User user) throws TException {
        for (String releaseId : releases) {
            final Release release = sw360ReleaseService.getReleaseForUserById(releaseId, user);
            addEmbeddedRelease(halResource, release);
        }
    }

    public void addEmbeddedReleases(
            HalResource halResource,
            List<Release> releases) {
        for (Release release : releases) {
            addEmbeddedRelease(halResource, release);
        }
    }

    public void addEmbeddedUser(HalResource halResource, User user, String relation) {
        User embeddedUser = convertToEmbeddedUser(user);
        Resource<User> embeddedUserResource = new Resource<>(embeddedUser);
        try {
            String userUUID = Base64.getEncoder().encodeToString(user.getEmail().getBytes("utf-8"));
            Link userLink = linkTo(UserController.class).slash("api/users/" + userUUID).withSelfRel();
            embeddedUserResource.add(userLink);
        } catch (Exception e) {
            LOGGER.error("cannot create embedded user with email: " + user.getEmail());
        }

        halResource.addEmbeddedResource(relation, embeddedUserResource);
    }

    public void addEmbeddedVendors(HalResource<Component> halComponent, Set<String> vendors) {
        for (String vendorFullName : vendors) {
            HalResource<Vendor> vendorHalResource = addEmbeddedVendor(vendorFullName);
            halComponent.addEmbeddedResource("sw360:vendors", vendorHalResource);
        }
    }

    private HalResource<Vendor> addEmbeddedVendor(String vendorFullName) {
        Vendor embeddedVendor = convertToEmbeddedVendor(vendorFullName);
        HalResource<Vendor> halVendor = new HalResource<>(embeddedVendor);
        try {
            Vendor vendorByFullName = vendorService.getVendorByFullName(vendorFullName);
            Link vendorSelfLink = linkTo(UserController.class)
                    .slash("api" + VendorController.VENDORS_URL + "/" + vendorByFullName.getId()).withSelfRel();
            halVendor.add(vendorSelfLink);
            return halVendor;
        } catch (Exception e) {
            LOGGER.error("cannot create self link for vendor with full name: " + vendorFullName);
        }
        return null;
    }

    public void addEmbeddedLicenses(HalResource<Release> halComponent, Set<String> licenseIds) {
        for (String licenseId : licenseIds) {
            HalResource<License> licenseHalResource = addEmbeddedLicense(licenseId);
            halComponent.addEmbeddedResource("sw360:licenses", licenseHalResource);
        }
    }

    private HalResource<License> addEmbeddedLicense(String licenseId) {
        License embeddedLicense = convertToEmbeddedLicense(licenseId);
        HalResource<License> halLicense = new HalResource<>(embeddedLicense);

        try {
            License licenseById = licenseService.getLicenseById(licenseId);
            embeddedLicense.setFullname(licenseById.getFullname());
            Link licenseSelfLink = linkTo(UserController.class)
                    .slash("api" + LicenseController.LICENSES_URL + "/" + licenseById.getId()).withSelfRel();
            halLicense.add(licenseSelfLink);
            return halLicense;
        } catch (Exception e) {
            LOGGER.error("cannot create self link for license with id: " + licenseId);
        }
        return null;
    }

    public HalResource<Release> createHalReleaseResource(Release release, boolean verbose) {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId()).withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);

        if (verbose) {
            if (release.getModerators() != null) {
                Set<String> moderators = release.getModerators();
                this.addEmbeddedModerators(halRelease, moderators);
                release.setModerators(null);
            }
            if (release.getAttachments() != null) {
                Set<Attachment> attachments = release.getAttachments();
                this.addEmbeddedAttachments(halRelease, attachments);
                release.setAttachments(null);
            }
            if (release.getVendor() != null) {
                Vendor vendor = release.getVendor();
                HalResource<Vendor> vendorHalResource = this.addEmbeddedVendor(vendor.getFullname());
                halRelease.addEmbeddedResource("sw360:vendors", vendorHalResource);
                release.setVendor(null);
            }
            if (release.getMainLicenseIds() != null) {
                this.addEmbeddedLicenses(halRelease, release.getMainLicenseIds());
                release.setMainLicenseIds(null);
            }
        }
        return halRelease;
    }

    public void addEmbeddedRelease(HalResource halResource, Release release) {
        Release embeddedRelease = convertToEmbeddedRelease(release);
        HalResource<Release> halRelease = new HalResource<>(embeddedRelease);
        Link releaseLink = linkTo(ReleaseController.class).
                slash("api/releases/" + release.getId()).withSelfRel();
        halRelease.add(releaseLink);
        halResource.addEmbeddedResource("sw360:releases", halRelease);
    }

    private void addEmbeddedAttachments(
            HalResource halResource,
            Set<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            Attachment embeddedAttachment = convertToEmbeddedAttachment(attachment);
            HalResource<Attachment> halAttachmentResource = new HalResource<>(embeddedAttachment);
            Link attachmentLink = linkTo(AttachmentController.class)
                    .slash("api/attachments/" + attachment.getAttachmentContentId()).withSelfRel();
            halAttachmentResource.add(attachmentLink);
            halResource.addEmbeddedResource("sw360:attachments", halAttachmentResource);
        }
    }

    public void addEmbeddedProject(HalResource<Project> halProject, Set<String> projectIds, Sw360ProjectService sw360ProjectService, User user) throws TException {
        for (String projectId : projectIds) {
            final Project project = sw360ProjectService.getProjectForUserById(projectId, user);
            addEmbeddedProject(halProject, project);
        }
    }

    private void addEmbeddedProject(HalResource halResource, Project project) {
        Project embeddedProject = convertToEmbeddedProject(project);
        HalResource<Project> halProject = new HalResource<>(embeddedProject);
        Link projectLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/" + project.getId()).withSelfRel();
        halProject.add(projectLink);
        halResource.addEmbeddedResource("sw360:projects", halProject);
    }

    public Project convertToEmbeddedProject(Project project) {
        Project embeddedProject = new Project(project.getName());
        embeddedProject.setId(project.getId());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setType(null);
        return embeddedProject;
    }

    public Component convertToEmbeddedComponent(Component component) {
        Component embeddedComponent = new Component();
        embeddedComponent.setId(component.getId());
        embeddedComponent.setName(component.getName());
        embeddedComponent.setComponentType(component.getComponentType());
        embeddedComponent.setType(null);
        return embeddedComponent;
    }

    public Release convertToEmbeddedRelease(Release release) {
        Release embeddedRelease = new Release();
        embeddedRelease.setId(release.getId());
        embeddedRelease.setName(release.getName());
        embeddedRelease.setVersion(release.getVersion());
        embeddedRelease.setType(null);
        return embeddedRelease;
    }

    public License convertToEmbeddedLicense(License license) {
        License embeddedLicense = new License();
        embeddedLicense.setId(license.getId());
        embeddedLicense.setFullname(license.getFullname());
        embeddedLicense.setType(null);
        return embeddedLicense;
    }

    public License convertToEmbeddedLicense(String licenseId) {
        License embeddedLicense = new License();
        embeddedLicense.setId(licenseId);
        embeddedLicense.setType(null);
        return embeddedLicense;
    }

    public User convertToEmbeddedUser(User user) {
        User embeddedUser = new User();
        embeddedUser.setId(user.getId());
        embeddedUser.setEmail(user.getEmail());
        embeddedUser.setType(null);
        return embeddedUser;
    }

    public Vendor convertToEmbeddedVendor(String fullName) {
        Vendor embeddedVendor = new Vendor();
        embeddedVendor.setFullname(fullName);
        embeddedVendor.setType(null);
        return embeddedVendor;
    }

    public Attachment convertToEmbeddedAttachment(Attachment attachment) {
        attachment.setCreatedTeam(null);
        attachment.setCreatedComment(null);
        attachment.setCreatedOn(null);
        attachment.setCreatedBy(null);
        attachment.setCheckedBy(null);
        attachment.setCheckedOn(null);
        attachment.setCheckedTeam(null);
        attachment.setCheckedComment(null);
        attachment.setCheckStatus(null);
        return attachment;
    }
}
