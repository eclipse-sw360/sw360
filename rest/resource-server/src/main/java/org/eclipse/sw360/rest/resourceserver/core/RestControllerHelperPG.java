/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.eclipse.sw360.datahandler.postgres.VulnerabilityPG;
import org.eclipse.sw360.datahandler.resourcelists.ResourceComparatorGenerator;
import org.eclipse.sw360.datahandler.resourcelists.ResourceListController;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ProjectPG;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.clearingrequest.Sw360ClearingRequestService;
import org.eclipse.sw360.rest.resourceserver.componentopenapi.ComponentsOpenAPIController;
import org.eclipse.sw360.rest.resourceserver.releasesopenapi.ReleaseOpenAPIController;
import org.eclipse.sw360.rest.resourceserver.releasesopenapi.ReleaseServicePG;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendoropenapi.VendorOpenAPIController;
import org.eclipse.sw360.rest.resourceserver.vendoropenapi.VendorServicePG;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RestControllerHelperPG<T> {

        @NonNull
        private final Sw360UserService userService;

        @NonNull
        private final VendorServicePG vendorServicePG = new VendorServicePG();

        @NonNull
        private final Sw360ClearingRequestService clearingRequestService;

        @NonNull
        private final ResourceComparatorGenerator<T> resourceComparatorGenerator =
                        new ResourceComparatorGenerator<>();

        @NonNull
        private final ResourceListController<T> resourceListController =
                        new ResourceListController<>();

        private static final Logger LOGGER = LogManager.getLogger(RestControllerHelperPG.class);

        private static final String PAGINATION_KEY_FIRST = "first";
        private static final String PAGINATION_KEY_PREVIOUS = "previous";
        private static final String PAGINATION_KEY_NEXT = "next";
        private static final String PAGINATION_KEY_LAST = "last";
        private static final String PAGINATION_PARAM_PAGE = "page";
        private static final double MIN_CVSS = 0;
        private static final double MAX_CVSS = 10;
        public static final String PAGINATION_PARAM_PAGE_ENTRIES = "page_entries";
        private static final String JWT_SUBJECT = "sub";

        @NonNull
        private final com.fasterxml.jackson.databind.Module sw360Module;

        public org.eclipse.sw360.datahandler.postgres.VendorPG convertToEmbeddedVendorPG(
                        org.eclipse.sw360.datahandler.postgres.VendorPG vendor) {
                org.eclipse.sw360.datahandler.postgres.VendorPG embeddedVendor =
                                new org.eclipse.sw360.datahandler.postgres.VendorPG();
                embeddedVendor.setShortname(vendor.getShortname());
                embeddedVendor.setFullname(vendor.getFullname());
                embeddedVendor.setUrl(vendor.getUrl());
                return embeddedVendor;
        }

        public org.eclipse.sw360.datahandler.postgres.VendorPG convertToEmbeddedVendorPG(
                        String fullName) {
                org.eclipse.sw360.datahandler.postgres.VendorPG embeddedVendor =
                                new org.eclipse.sw360.datahandler.postgres.VendorPG();
                embeddedVendor.setFullname(fullName);
                return embeddedVendor;
        }

        public HalResource<org.eclipse.sw360.datahandler.postgres.VendorPG> addEmbeddedVendorPG(
                        String vendorFullName) {
                org.eclipse.sw360.datahandler.postgres.VendorPG embeddedVendor =
                                convertToEmbeddedVendorPG(vendorFullName);
                HalResource<org.eclipse.sw360.datahandler.postgres.VendorPG> halVendor =
                                new HalResource<>(embeddedVendor);
                org.eclipse.sw360.datahandler.postgres.VendorPG vendorByFullName =
                                vendorServicePG.getVendorByFullName(vendorFullName);
                if (vendorByFullName != null) {
                        Link vendorSelfLink = linkTo(UserController.class)
                                        .slash("api" + VendorOpenAPIController.VENDORS_URL + "/"
                                                        + vendorByFullName.getId())
                                        .withSelfRel();
                        halVendor.add(vendorSelfLink);
                }
                return halVendor;
        }

        public void addEmbeddedVendorsPG(
                        HalResource<org.eclipse.sw360.datahandler.postgres.ComponentPG> halComponent,
                        Set<String> vendorFullnames) {
                for (String vendorFullName : vendorFullnames) {
                        HalResource<org.eclipse.sw360.datahandler.postgres.VendorPG> vendorHalResource =
                                        addEmbeddedVendorPG(vendorFullName);
                        halComponent.addEmbeddedResource("vendors", vendorHalResource);
                }
        }

        public void addEmbeddedReleasesPG(HalResource halResource, List<String> releases,
                        ReleaseServicePG sw360ReleaseService, User user) throws TException {
                for (String releaseId : releases) {
                        final ReleasePG release =
                                        sw360ReleaseService.getReleaseForUserById(releaseId, user);
                        addEmbeddedReleasePG(halResource, release);
                }
        }

        public void addEmbeddedReleasesPG(HalResource halResource,
                        List<org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI> releases) {
                for (org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI release : releases) {
                        addEmbeddedReleasePG(halResource, release);
                }
        }

        public void addEmbeddedVulnerabilitiesPG(HalResource<VulnerabilityPG> halResource,
                        List<org.eclipse.sw360.datahandler.postgres.VulnerabilityPG> vulnerabilities) {
                for (org.eclipse.sw360.datahandler.postgres.VulnerabilityPG vulnerability : vulnerabilities) {
                        HalResource<org.eclipse.sw360.datahandler.postgres.VulnerabilityPG> halVulnerability =
                                        new HalResource<>(vulnerability);
                        Link vulnerabilityLink = linkTo(ReleaseOpenAPIController.class)
                                        .slash("api/vulnerabilities/" + vulnerability.getId())
                                        .withSelfRel();
                        halVulnerability.add(vulnerabilityLink);
                        halResource.addEmbeddedResource("vulnerabilities", halVulnerability);
                }
        }

        public void addEmbeddedComponentPG(HalResource halResource,
                        org.eclipse.sw360.datahandler.postgres.ComponentPG component) {
                ComponentPG embeddedComponent = convertToEmbeddedComponent(component);
                HalResource<ComponentPG> halComponent = new HalResource<>(embeddedComponent);
                Link componentLink = linkTo(ComponentsOpenAPIController.class)
                                .slash("api/componentsOpenAPI/" + component.getId()).withSelfRel();
                halComponent.add(componentLink);
                halResource.addEmbeddedResource("components", halComponent);
        }

        public void addEmbeddedReleasePG(HalResource halResource,
                        org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI release) {
                org.eclipse.sw360.datahandler.postgres.ReleasePG embeddedRelease =
                                convertToEmbeddedReleasePG(release);
                HalResource<org.eclipse.sw360.datahandler.postgres.ReleasePG> halRelease =
                                new HalResource<>(embeddedRelease);
                Link releaseLink = linkTo(ReleaseOpenAPIController.class)
                                .slash("api/releases/" + release.getId()).withSelfRel();
                halRelease.add(releaseLink);
                halResource.addEmbeddedResource("releases", halRelease);
        }

        public ComponentPG convertToEmbeddedComponent(ComponentPG component, List<String> fields) {
                return this.convertToEmbeddedComponent(component);
        }

        public ComponentPG convertToEmbeddedComponent(ComponentPG component) {
                ComponentPG embeddedComponent = new ComponentPG();
                embeddedComponent.setId(component.getId());
                embeddedComponent.setName(component.getName());
                if (CommonUtils.isNotNullEmptyOrWhitespace(component.getDefaultVendorId())) {
                        try {
                                VendorPG defaultVendor = vendorServicePG
                                                .getVendorById(component.getDefaultVendorId());
                                embeddedComponent.setDefaultVendor(defaultVendor);
                        } catch (RuntimeException e) {
                                LOGGER.error("Failed to retrieve default vendor '{}' from SW360 database.",
                                                component.getDefaultVendorId(), e);
                        }
                }
                return embeddedComponent;
        }

        public org.eclipse.sw360.datahandler.postgres.ReleasePG convertToEmbeddedReleasePG(
                        org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI release) {
                org.eclipse.sw360.datahandler.postgres.ReleasePG embeddedRelease =
                                new org.eclipse.sw360.datahandler.postgres.ReleasePG();
                embeddedRelease.setId(release.getId());
                embeddedRelease.setName(release.getName());
                embeddedRelease.setVersion(release.getVersion());
                return embeddedRelease;
        }

        public void addEmbeddedDataToComponentOAPI(HalResource halResource,
                        ComponentAPI sw360Component) {
                addEmbeddedModifiedByToComponent(halResource, sw360Component);
                addEmbeddedComponentOwnerToComponent(halResource, sw360Component);
                addEmbeddedSubcribeToHalResourceComponent(halResource, sw360Component);
        }

        public void addEmbeddedModifiedByToComponent(HalResource halResource,
                        ComponentAPI sw360Component) {
                if (sw360Component.getModifiedBy() != null) {
                        User componentModify = getUserByEmail(sw360Component.getModifiedBy());
                        if (null != componentModify)
                                addEmbeddedUser(halResource, componentModify, "modifiedBy");
                }
        }

        public void addEmbeddedComponentOwnerToComponent(HalResource halResource,
                        ComponentAPI sw360Component) {
                if (sw360Component.getComponentOwner() != null) {
                        User componentOwner = getUserByEmail(sw360Component.getComponentOwner());
                        if (null != componentOwner) {
                                addEmbeddedUser(halResource, componentOwner, "componentOwner");
                                sw360Component.setComponentOwner(null);
                        }
                }
        }

        public void addEmbeddedSubcribeToHalResourceComponent(HalResource halResource,
                        ComponentAPI sw360Component) {
                if (!CommonUtils.isNullOrEmptyCollection(sw360Component.getSubscribers())) {
                        List<String> subscribers = sw360Component.getSubscribers();
                        for (String subscribersEmail : subscribers) {
                                User sw360User = getUserByEmail(subscribersEmail);
                                if (null != sw360User) {
                                        addEmbeddedUser(halResource, sw360User, "subscribers");
                                        sw360Component.setSubscribers(null);
                                }
                        }
                }
        }

        public User getUserByEmail(String emailId) {
                User sw360User;
                try {
                        sw360User = userService.getUserByEmail(emailId);
                } catch (RuntimeException e) {
                        sw360User = new User();
                        sw360User.setId(emailId).setEmail(emailId);
                        LOGGER.debug("Could not get user object from backend with email: "
                                        + emailId);
                }
                return sw360User;
        }

        public void addEmbeddedUser(HalResource halResource, User user, String relation) {
                User embeddedUser = convertToEmbeddedUser(user);
                EntityModel<User> embeddedUserResource = EntityModel.of(embeddedUser);
                try {
                        Link userLink = linkTo(UserController.class)
                                        .slash("api/users/byid/"
                                                        + URLEncoder.encode(user.getId(), "UTF-8"))
                                        .withSelfRel();
                        embeddedUserResource.add(userLink);
                        halResource.addEmbeddedResource(relation, embeddedUserResource);
                } catch (UnsupportedEncodingException e) {
                        LOGGER.error("cannot create embedded user with email: " + user.getEmail(),
                                        e);
                }
        }

        public User convertToEmbeddedUser(User user) {
                User embeddedUser = new User();
                embeddedUser.setId(user.getId());
                embeddedUser.setFullname(user.getFullname());
                embeddedUser.setEmail(user.getEmail());
                embeddedUser.setType(null);
                return embeddedUser;
        }

        public void addEmbeddedComponents(HalResource<ProjectPG> halProject,
                        List<ComponentPG> components) {
                for (ComponentPG component : components) {
                        ComponentPG embeddedComponent = convertToEmbeddedComponent(component);
                        HalResource<ComponentPG> halComponent =
                                        new HalResource<>(embeddedComponent);
                        Link componentLink = linkTo(ComponentsOpenAPIController.class)
                                        .slash("api/componentsOpenAPI/" + component.getId())
                                        .withSelfRel();
                        halComponent.add(componentLink);
                        halProject.addEmbeddedResource("components", halComponent);
                }
        }
}
