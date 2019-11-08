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

import org.eclipse.sw360.datahandler.resourcelists.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.license.LicenseController;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProject;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;

import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RestControllerHelper<T> {

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final Sw360LicenseService licenseService;

    @NonNull
    private final ResourceComparatorGenerator<T> resourceComparatorGenerator = new ResourceComparatorGenerator<>();

    @NonNull
    private final ResourceListController<T> resourceListController = new ResourceListController<>();

    private static final Logger LOGGER = Logger.getLogger(RestControllerHelper.class);

    private static final String PAGINATION_KEY_FIRST = "first";
    private static final String PAGINATION_KEY_PREVIOUS = "previous";
    private static final String PAGINATION_KEY_NEXT = "next";
    private static final String PAGINATION_KEY_LAST = "last";
    private static final String PAGINATION_PARAM_PAGE = "page";
    public static final String PAGINATION_PARAM_PAGE_ENTRIES = "page_entries";

    public User getSw360UserFromAuthentication() {
        try {
            String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return userService.getUserByEmailOrExternalId(userId);
        } catch (RuntimeException e) {
            throw new AuthenticationServiceException("Could not load user from authentication.");
        }
    }

    public PaginationResult<T> createPaginationResult(HttpServletRequest request, Pageable pageable, List<T> resources, String resourceType) throws ResourceClassNotFoundException, PaginationParameterException {
        PaginationResult<T> paginationResult;
        if (requestContainsPaging(request)) {
            PaginationOptions<T> paginationOptions = paginationOptionsFromPageable(pageable, resourceType);
            paginationResult = resourceListController.applyPagingToList(resources, paginationOptions);
        } else {
            paginationResult = new PaginationResult<>(resources);
        }
        return paginationResult;
    }

    private boolean requestContainsPaging(HttpServletRequest request) {
        return request.getParameterMap().containsKey(PAGINATION_PARAM_PAGE) || request.getParameterMap().containsKey(PAGINATION_PARAM_PAGE_ENTRIES);
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> Resources<Resource<T>> generatePagesResource(PaginationResult paginationResult, List<Resource<T>> resources) throws URISyntaxException {
        if (paginationResult.isPagingActive()) {
            PagedResources.PageMetadata pageMetadata = createPageMetadata(paginationResult);
            List<Link> pagingLinks = this.getPaginationLinks(paginationResult, this.getAPIBaseUrl());
            return new PagedResources<>(resources, pageMetadata, pagingLinks);
        } else {
            return new Resources<>(resources);
        }
    }

    public PagedResources emptyPageResource(Class resourceClass, PaginationResult paginationResult) {
        EmbeddedWrappers embeddedWrappers = new EmbeddedWrappers(true);
        EmbeddedWrapper embeddedWrapper = embeddedWrappers.emptyCollectionOf(resourceClass);
        List<EmbeddedWrapper> list = Collections.singletonList(embeddedWrapper);
        PagedResources.PageMetadata pageMetadata = createPageMetadata(paginationResult);
        return new PagedResources<>(list, pageMetadata, new ArrayList<>());
    }

    private PagedResources.PageMetadata createPageMetadata(PaginationResult paginationResult) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        return new PagedResources.PageMetadata(
                paginationOptions.getPageSize(),
                paginationOptions.getPageNumber(),
                paginationResult.getTotalCount(),
                paginationResult.getTotalPageCount());
    }

    private List<Link> getPaginationLinks(PaginationResult paginationResult, String baseUrl) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        List<Link> paginationLinks = new ArrayList<>();

        paginationLinks.add(new Link(createPaginationLink(baseUrl, 0, paginationOptions.getPageSize()),PAGINATION_KEY_FIRST));
        if(paginationOptions.getPageNumber() > 0) {
            paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationOptions.getPageNumber() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_PREVIOUS));
        }
        if(paginationOptions.getOffset() + paginationOptions.getPageSize() < paginationResult.getTotalCount()) {
            paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationOptions.getPageNumber() + 1, paginationOptions.getPageSize()),PAGINATION_KEY_NEXT));
        }
        paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationResult.getTotalPageCount() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_LAST));

        return paginationLinks;
    }

    private String createPaginationLink(String baseUrl, int page, int pageSize) {
        return baseUrl + "?" + PAGINATION_PARAM_PAGE + "=" + page + "&" + PAGINATION_PARAM_PAGE_ENTRIES + "=" + pageSize;
    }

    private String getAPIBaseUrl() throws URISyntaxException {
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return new URI(uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                null,
                uri.getFragment()).toString();
    }

    private PaginationOptions<T> paginationOptionsFromPageable(Pageable pageable, String resourceClassName) throws ResourceClassNotFoundException {
        Comparator<T> comparator = this.comparatorFromPageable(pageable, resourceClassName);
        return new PaginationOptions<T>(pageable.getPageNumber(), pageable.getPageSize(), comparator);
    }

    private Comparator<T> comparatorFromPageable(Pageable pageable,  String resourceClassName) throws ResourceClassNotFoundException {
        Sort.Order order = firstOrderFromPageable(pageable);
        if(order == null) {
            return resourceComparatorGenerator.generateComparator(resourceClassName);
        }
        Comparator<T> comparator = resourceComparatorGenerator.generateComparator(resourceClassName, order.getProperty());
        if(order.isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private Sort.Order firstOrderFromPageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        if(sort == null) {
            return null;
        }
        Iterator<Sort.Order> orderIterator = sort.iterator();
        if(orderIterator.hasNext()) {
            return orderIterator.next();
        } else {
            return null;
        }
    }

    public void addEmbeddedModerators(HalResource halResource, Set<String> moderators) {
        User sw360User;
        for (String moderatorEmail : moderators) {
            try {
                sw360User = userService.getUserByEmail(moderatorEmail);
            } catch (RuntimeException e) {
                sw360User = new User();
                sw360User.setId(moderatorEmail).setEmail(moderatorEmail);
                LOGGER.debug("Could not get user object from backend with email: " + moderatorEmail);
            }
            addEmbeddedUser(halResource, sw360User, "sw360:moderators");
        }
    }

    public void addEmbeddedContributors(HalResource halResource, Set<String> contributors) {
        User sw360User;
        for (String contributorEmail : contributors) {
            try {
                sw360User = userService.getUserByEmail(contributorEmail);
            } catch (RuntimeException e) {
                sw360User = new User();
                sw360User.setId(contributorEmail).setEmail(contributorEmail);
                LOGGER.debug("Could not get user object from backend with email: " + contributorEmail);
            }
            addEmbeddedUser(halResource, sw360User, "sw360:contributors");
        }
    }

    public void addEmbeddedLeadArchitect(HalResource halResource, String leadArchitect) {
        User sw360User;
        try {
              sw360User = userService.getUserByEmail(leadArchitect);
            } catch (RuntimeException e) {
                sw360User = new User();
                sw360User.setId(leadArchitect).setEmail(leadArchitect);
                LOGGER.debug("Could not get user object from backend with email: " + leadArchitect);
            }
            addEmbeddedUser(halResource, sw360User, "leadArchitect");
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
            Link userLink = linkTo(UserController.class).slash("api/users/byid/" + URLEncoder.encode(user.getId(), "UTF-8")).withSelfRel();
            embeddedUserResource.add(userLink);
            halResource.addEmbeddedResource(relation, embeddedUserResource);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("cannot create embedded user with email: " + user.getEmail(), e);
        }
    }

    public void addEmbeddedVendors(HalResource<Component> halComponent, Set<String> vendorFullnames) {
        for (String vendorFullName : vendorFullnames) {
            HalResource<Vendor> vendorHalResource = addEmbeddedVendor(vendorFullName);
            halComponent.addEmbeddedResource("sw360:vendors", vendorHalResource);
        }
    }

    public HalResource<Vendor> addEmbeddedVendor(String vendorFullName) {
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

    public void addEmbeddedRelease(HalResource halResource, Release release) {
        Release embeddedRelease = convertToEmbeddedRelease(release);
        HalResource<Release> halRelease = new HalResource<>(embeddedRelease);
        Link releaseLink = linkTo(ReleaseController.class).
                slash("api/releases/" + release.getId()).withSelfRel();
        halRelease.add(releaseLink);
        halResource.addEmbeddedResource("sw360:releases", halRelease);
    }

    public void addEmbeddedAttachments(
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

    public void addEmbeddedProject(HalResource halResource, Project project) {
        Project embeddedProject = convertToEmbeddedProject(project);
        HalResource<Project> halProject = new HalResource<>(embeddedProject);
        Link projectLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/" + project.getId()).withSelfRel();
        halProject.add(projectLink);
        halResource.addEmbeddedResource("sw360:projects", halProject);
    }

    public Project updateProject(Project projectToUpdate, Project requestBodyProject) {
        for(Project._Fields field:Project._Fields.values()) {
            Object fieldValue = requestBodyProject.getFieldValue(field);
            if(fieldValue != null) {
                projectToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return projectToUpdate;
    }

    public Component updateComponent(Component componentToUpdate, Component requestBodyComponent) {
        for(Component._Fields field:Component._Fields.values()) {
            Object fieldValue = requestBodyComponent.getFieldValue(field);
            if(fieldValue != null) {
                componentToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return componentToUpdate;
    }

    public Release updateRelease(Release releaseToUpdate, Release requestBodyRelease) {
        for(Release._Fields field:Release._Fields.values()) {
            Object fieldValue = requestBodyRelease.getFieldValue(field);
            if(fieldValue != null) {
                releaseToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return releaseToUpdate;
    }

    public Project convertToEmbeddedProject(Project project) {
        Project embeddedProject = new EmbeddedProject();
        embeddedProject.setName(project.getName());
        embeddedProject.setId(project.getId());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setType(null);
        return embeddedProject;
    }

    public void addEmbeddedComponent(HalResource halResource, Component component) {
        Component embeddedComponent = convertToEmbeddedComponent(component);
        HalResource<Component> halComponent = new HalResource<>(embeddedComponent);
        Link componentLink = linkTo(ComponentController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + component.getId()).withSelfRel();
        halComponent.add(componentLink);
        halResource.addEmbeddedResource("sw360:components", halComponent);
    }

    public Component convertToEmbeddedComponent(Component component) {
        Component embeddedComponent = new Component();
        embeddedComponent.setId(component.getId());
        embeddedComponent.setName(component.getName());
        embeddedComponent.setComponentType(component.getComponentType());
        embeddedComponent.setType(null);
        return embeddedComponent;
    }

    public Component convertToEmbeddedComponent(Component component, List<String> fields) {
        Component embeddedComponent = this.convertToEmbeddedComponent(component);
        if (fields != null) {
            for(String fieldName:fields) {
                String thriftField = PropertyKeyMapping.componentThriftKeyFromJSONKey(fieldName);
                Component._Fields componentField = Component._Fields.findByName(thriftField);
                if(componentField != null) {
                    embeddedComponent.setFieldValue(componentField, component.getFieldValue(componentField));
                }
            }
        }
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

    public Release convertToEmbeddedRelease(Release release, List<String> fields) {
        Release embeddedRelease = this.convertToEmbeddedRelease(release);
        if (fields != null) {
            for(String fieldName:fields) {
                String thriftField = PropertyKeyMapping.releaseThriftKeyFromJSONKey(fieldName);
                Release._Fields releaseField = Release._Fields.findByName(thriftField);
                if(releaseField != null) {
                    embeddedRelease.setFieldValue(releaseField, release.getFieldValue(releaseField));
                }
            }
        }
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

    public Vendor convertToEmbeddedVendor(Vendor vendor) {
        Vendor embeddedVendor = convertToEmbeddedVendor(vendor.getFullname());
        embeddedVendor.setId(vendor.getId());
        return embeddedVendor;
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

    public Vulnerability convertToEmbeddedVulnerability(Vulnerability vulnerability) {
        Vulnerability embeddedVulnerability = new Vulnerability(vulnerability.getExternalId());
        embeddedVulnerability.setId(vulnerability.getId());
        embeddedVulnerability.setTitle(vulnerability.getTitle());
        return embeddedVulnerability;
    }

    /**
     * Generic Entity response method to get externalIds (projects, components, releases)
     */
    public <T> ResponseEntity searchByExternalIds(MultiValueMap<String, String> externalIdsMultiMap,
                                                  AwareOfRestServices<T> service,
                                                  User user) throws TException {

        Map<String, Set<String>> externalIds = getExternalIdsFromMultiMap(externalIdsMultiMap);
        Set<T> sw360Objects = service.searchByExternalIds(externalIds, user);
        List<Resource> resourceList = new ArrayList<>();

        sw360Objects.forEach(sw360Object -> {
            T embeddedResource = service.convertToEmbeddedWithExternalIds(sw360Object);
            Resource<T> releaseResource = new Resource<>(embeddedResource);
            resourceList.add(releaseResource);
        });

        Resources<Resource> resources = new Resources<>(resourceList);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private Map<String, Set<String>> getExternalIdsFromMultiMap(MultiValueMap<String, String> externalIdsMultiMap) {
        Map<String, Set<String>> externalIds = new HashMap<>();
        for (String externalIdKey : externalIdsMultiMap.keySet()) {
            externalIds.put(externalIdKey, new HashSet<>(externalIdsMultiMap.get(externalIdKey)));
        }

        return externalIds;
    }
}
