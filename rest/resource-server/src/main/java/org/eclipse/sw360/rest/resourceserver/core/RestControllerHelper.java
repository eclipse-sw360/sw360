/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.resourcelists.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityApiDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.license.LicenseController;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProject;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.rest.resourceserver.obligation.ObligationController;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

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

    private static final Logger LOGGER = LogManager.getLogger(RestControllerHelper.class);

    private static final String PAGINATION_KEY_FIRST = "first";
    private static final String PAGINATION_KEY_PREVIOUS = "previous";
    private static final String PAGINATION_KEY_NEXT = "next";
    private static final String PAGINATION_KEY_LAST = "last";
    private static final String PAGINATION_PARAM_PAGE = "page";
    private static final double MIN_CVSS = 0;
    private static final double MAX_CVSS = 10;
    public static final String PAGINATION_PARAM_PAGE_ENTRIES = "page_entries";
    public static final ImmutableSet<ProjectReleaseRelationship._Fields> SET_OF_PROJECTRELEASERELATION_FIELDS_TO_IGNORE = ImmutableSet
            .of(ProjectReleaseRelationship._Fields.CREATED_ON, ProjectReleaseRelationship._Fields.CREATED_BY);

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

    public <T extends TBase<?, ? extends TFieldIdEnum>> CollectionModel<EntityModel<T>> generatePagesResource(PaginationResult paginationResult, List<EntityModel<T>> resources) throws URISyntaxException {
        if (paginationResult.isPagingActive()) {
            PagedModel.PageMetadata pageMetadata = createPageMetadata(paginationResult);
            List<Link> pagingLinks = this.getPaginationLinks(paginationResult, this.getAPIBaseUrl());
            return PagedModel.of(resources, pageMetadata, pagingLinks);
        } else {
            return CollectionModel.of(resources);
        }
    }

    public PagedModel emptyPageResource(Class resourceClass, PaginationResult paginationResult) {
        EmbeddedWrappers embeddedWrappers = new EmbeddedWrappers(true);
        EmbeddedWrapper embeddedWrapper = embeddedWrappers.emptyCollectionOf(resourceClass);
        List<EmbeddedWrapper> list = Collections.singletonList(embeddedWrapper);
        PagedModel.PageMetadata pageMetadata = createPageMetadata(paginationResult);
        return PagedModel.of(list, pageMetadata, new ArrayList<>());
    }

    private PagedModel.PageMetadata createPageMetadata(PaginationResult paginationResult) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        return new PagedModel.PageMetadata(
                paginationOptions.getPageSize(),
                paginationOptions.getPageNumber(),
                paginationResult.getTotalCount(),
                paginationResult.getTotalPageCount());
    }

    private List<Link> getPaginationLinks(PaginationResult paginationResult, String baseUrl) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        List<Link> paginationLinks = new ArrayList<>();

        paginationLinks.add(Link.of(createPaginationLink(baseUrl, 0, paginationOptions.getPageSize()),PAGINATION_KEY_FIRST));
        if(paginationOptions.getPageNumber() > 0) {
            paginationLinks.add(Link.of(createPaginationLink(baseUrl, paginationOptions.getPageNumber() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_PREVIOUS));
        }
        if(paginationOptions.getOffset() + paginationOptions.getPageSize() < paginationResult.getTotalCount()) {
            paginationLinks.add(Link.of(createPaginationLink(baseUrl, paginationOptions.getPageNumber() + 1, paginationOptions.getPageSize()),PAGINATION_KEY_NEXT));
        }
        paginationLinks.add(Link.of(createPaginationLink(baseUrl, paginationResult.getTotalPageCount() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_LAST));

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
        return new PaginationOptions<>(pageable.getPageNumber(), pageable.getPageSize(), comparator);
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

        for (String moderatorEmail : moderators) {
            User sw360User = getUserByEmail(moderatorEmail);
            addEmbeddedUser(halResource, sw360User, "sw360:moderators");
        }
    }

    public User getUserByEmail(String emailId) {
        User sw360User;
        try {
            sw360User = userService.getUserByEmail(emailId);
        } catch (RuntimeException e) {
            sw360User = new User();
            sw360User.setId(emailId).setEmail(emailId);
            LOGGER.debug("Could not get user object from backend with email: " + emailId);
        }
        return sw360User;
    }

    public void addEmbeddedContributors(HalResource halResource, Set<String> contributors) {
        for (String contributorEmail : contributors) {
            User sw360User = getUserByEmail(contributorEmail);
            addEmbeddedUser(halResource, sw360User, "sw360:contributors");
        }
    }

    public void addEmbeddedLeadArchitect(HalResource halResource, String leadArchitect) {
        User sw360User = getUserByEmail(leadArchitect);
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
        EntityModel<User> embeddedUserResource = EntityModel.of(embeddedUser);
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
            embeddedLicense.setShortname(licenseId);
            Link licenseSelfLink = linkTo(UserController.class)
                    .slash("api" + LicenseController.LICENSES_URL + "/" + licenseById.getId()).withSelfRel();
            halLicense.add(licenseSelfLink);
            return halLicense;
        } catch (ResourceNotFoundException rne) {
            LOGGER.error("cannot create a self link for license with id" + licenseId);
            embeddedLicense.setShortname(licenseId);
            embeddedLicense.setOSIApproved(Quadratic.NA);
            embeddedLicense.setFSFLibre(Quadratic.NA);
            embeddedLicense.setChecked(false);
            embeddedLicense.setFullname(null);
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
            addEmbeddedProject(halProject, project, false);
        }
    }

    public void addEmbeddedProject(HalResource halResource, Project project, boolean isSingleProject) {
        Project embeddedProject = convertToEmbeddedProject(project);
        HalResource<Project> halProject = new HalResource<>(embeddedProject);
        Link projectLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/" + project.getId()).withSelfRel();
        halProject.add(projectLink);
        halResource.addEmbeddedResource(isSingleProject ? "sw360:project" : "sw360:projects", halProject);
    }

    public Project updateProject(Project projectToUpdate, Project requestBodyProject, Map<String, Object> reqBodyMap,
            ImmutableMap<Project._Fields, String> mapOfProjectFieldsToRequestBody) {
        for (Project._Fields field : Project._Fields.values()) {
            Object fieldValue = requestBodyProject.getFieldValue(field);
            if (fieldValue != null) {
                String reqBodyStr = field.getFieldName();
                if (mapOfProjectFieldsToRequestBody.containsKey(field)) {
                    reqBodyStr = mapOfProjectFieldsToRequestBody.get(field);
                }
                if (!reqBodyMap.containsKey(reqBodyStr)) {
                    continue;
                }
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
        for (Release._Fields field : Release._Fields.values()) {
            Object fieldValue = requestBodyRelease.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case MAIN_LICENSE_IDS:
                        isLicenseValid(requestBodyRelease.getMainLicenseIds());
                        break;
                    case OTHER_LICENSE_IDS:
                        isLicenseValid(requestBodyRelease.getOtherLicenseIds());
                        break;
                    default:
                }
                releaseToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return releaseToUpdate;
    }

    private void isLicenseValid(Set<String> licenses) {
        List <String> licenseIncorrect = new ArrayList<>();
        if (CommonUtils.isNotEmpty(licenses)) {
            for (String licenseId : licenses) {
                try {
                    licenseService.getLicenseById(licenseId);
                } catch (Exception e) {
                    licenseIncorrect.add(licenseId);
                }
            }
        }
        if (!licenseIncorrect.isEmpty()) {
            throw new HttpMessageNotReadableException("License with ids " + licenseIncorrect + " does not exist in SW360 database.");
        }
    }

    public ProjectReleaseRelationship updateProjectReleaseRelationship(
            ProjectReleaseRelationship actualProjectReleaseRelationship,
            ProjectReleaseRelationship requestBodyProjectReleaseRelationship) {
        for (ProjectReleaseRelationship._Fields field : ProjectReleaseRelationship._Fields.values()) {
            Object fieldValue = requestBodyProjectReleaseRelationship.getFieldValue(field);
            if (fieldValue != null && !SET_OF_PROJECTRELEASERELATION_FIELDS_TO_IGNORE.contains(field)) {
                actualProjectReleaseRelationship.setFieldValue(field, fieldValue);
            }
        }
        return actualProjectReleaseRelationship;
    }

    public Project convertToEmbeddedProject(Project project) {
        Project embeddedProject = new EmbeddedProject();
        embeddedProject.setName(project.getName());
        embeddedProject.setId(project.getId());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setVisbility(project.getVisbility());
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
        embeddedComponent.setVisbility(component.getVisbility());
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

    public Release convertToEmbeddedReleaseWithDet(Release release) {
        List<String> fields = List.of("id", "name", "version", "cpeid", "createdBy", "createdOn", "componentId","componentType",
                "additionalData", "clearingState", "mainLicenseIds", "binaryDownloadurl", "sourceCodeDownloadurl",
                "releaseDate", "externalIds", "languages", "operatingSystems", "softwarePlatforms", "vendor",
                "mainlineState");
        return convertToEmbeddedRelease(release, fields);
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
        embeddedLicense.unsetOSIApproved();
        embeddedLicense.unsetFSFLibre();
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
        embeddedUser.setFullname(user.getFullname());
        embeddedUser.setEmail(user.getEmail());
        embeddedUser.setType(null);
        return embeddedUser;
    }

    public void addEmbeddedObligations(HalResource<License> halLicense, List<Obligation> obligations) {
        for (Obligation obligation : obligations) {
            HalResource<Obligation> obligationHalResource = addEmbeddedObligation(obligation);
            halLicense.addEmbeddedResource("sw360:obligations", obligationHalResource);
        }
    }

    public HalResource<Obligation> addEmbeddedObligation(Obligation obligation) {
        Obligation embeddedObligation = convertToEmbeddedObligation(obligation);
        HalResource<Obligation> halObligation = new HalResource<>(embeddedObligation);
        try {
            Link obligationSelfLink = linkTo(UserController.class)
                    .slash("api" + ObligationController.OBLIGATION_URL + "/" + obligation.getId()).withSelfRel();
            halObligation.add(obligationSelfLink);
            return halObligation;
        } catch (Exception e) {
            LOGGER.error("cannot create self link for obligation with id: " +obligation.getId());
        }
        return null;
    }

    public Obligation convertToEmbeddedObligation(Obligation obligation) {
        Obligation embeddedObligation = new Obligation();
        embeddedObligation.setTitle(obligation.getTitle());
        embeddedObligation.setId(obligation.getId());
        embeddedObligation.setType(null);
        return embeddedObligation;
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
        embeddedVulnerability.setCvss(vulnerability.getCvss());
        embeddedVulnerability.setLastExternalUpdate(vulnerability.getLastExternalUpdate());

        embeddedVulnerability.setPublishDate(vulnerability.getPublishDate());
        return embeddedVulnerability;
    }


    public boolean setDataVulApiDTO(VulnerabilityApiDTO vulnerabilityApiDTO, Vulnerability vulnerability, Set<Release> releaseList) {
        for (Vulnerability._Fields field : Vulnerability._Fields.values()) {
            for (VulnerabilityApiDTO._Fields fieldDTO : VulnerabilityApiDTO._Fields.values()) {
                if (field.getThriftFieldId() == fieldDTO.getThriftFieldId()) {
                    if (vulnerability.getFieldValue(field) == null) {
                        break;
                    }
                    if (field.equals(Vulnerability._Fields.CVSS)) {
                        vulnerabilityApiDTO.setCvss(String.valueOf(vulnerability.getCvss()));
                    } else if (field.equals(Vulnerability._Fields.IS_SET_CVSS)) {
                        vulnerabilityApiDTO.setIsSetCvss(String.valueOf(vulnerability.isIsSetCvss()));
                    } else if (field.equals(Vulnerability._Fields.CVE_REFERENCES)) {
                        Set<CVEReference> cveReferences = vulnerability.getCveReferences();
                        if (cveReferences.size() > 0) {
                            vulnerabilityApiDTO.setCveReferences(convertCVEReferenceString(cveReferences));
                        }
                    } else {
                        vulnerabilityApiDTO.setFieldValue(fieldDTO, vulnerability.getFieldValue(field));
                    }
                }
            }
        }
        if (releaseList.size() > 0) {
            vulnerabilityApiDTO.setReleases(releaseList);
        }
        return true;
    }

    private Set<String> convertCVEReferenceString(Set<CVEReference> cveReferences) {
        Set<String> cveReferenceString = new HashSet<String>();
        for (CVEReference cveReference :cveReferences) {
            String cveInfo = cveReference.getYear() + "-" + cveReference.getNumber();
            cveReferenceString.add(cveInfo);
        }
        return cveReferenceString;
    }

    public boolean setDataForVulnerability(VulnerabilityApiDTO vulnerabilityApiDTO, Vulnerability vulnerability) {
        for (Vulnerability._Fields field : Vulnerability._Fields.values()) {
            if (field.equals(Vulnerability._Fields.REVISION) || field.equals(Vulnerability._Fields.ID) || field.equals(Vulnerability._Fields.TYPE)) {
                continue;
            }
            for (VulnerabilityApiDTO._Fields fieldDTO : VulnerabilityApiDTO._Fields.values()) {
                if (field.getThriftFieldId() == fieldDTO.getThriftFieldId()) {
                    if (vulnerabilityApiDTO.getFieldValue(fieldDTO) == null) {
                        break;
                    }
                    if (fieldDTO.equals(VulnerabilityApiDTO._Fields.CVSS)) {
                        if (!setDataCVSS(vulnerabilityApiDTO.getCvss(), vulnerability)) {
                            throw new RuntimeException(new SW360Exception("Invalid cvss: property 'cvss' should be a valid cvss.")
                                    .setErrorCode(org.apache.http.HttpStatus.SC_BAD_REQUEST));
                        }
                    } else if (fieldDTO.equals(VulnerabilityApiDTO._Fields.IS_SET_CVSS)) {
                        if(!setDataIsSetCvss(vulnerabilityApiDTO.getIsSetCvss(), vulnerability)) {
                            throw new RuntimeException(new SW360Exception("Invalid isSetCvss: property 'isSetCvss' should be a valid isSetCvss.")
                                    .setErrorCode(org.apache.http.HttpStatus.SC_BAD_REQUEST));
                        }
                    } else if (fieldDTO.equals(VulnerabilityApiDTO._Fields.CVE_REFERENCES)) {
                        setDataCveReferences(vulnerabilityApiDTO.getCveReferences(), vulnerability);
                    } else {
                        vulnerability.setFieldValue(field, vulnerabilityApiDTO.getFieldValue(fieldDTO));
                    }
                }
            }
        }
        vulnerability.setLastUpdateDate(SW360Utils.getCreatedOn());
        return true;
    }

    private boolean setDataCVSS(String cvss, Vulnerability vulnerability) {
        try {
            double m_cvss = Double.parseDouble(cvss);
            if (m_cvss < MIN_CVSS || m_cvss > MAX_CVSS) {
                return false;
            }
            vulnerability.setCvss(m_cvss);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean setDataIsSetCvss(String isSetCvss, Vulnerability vulnerability) {
        boolean m_isSetCvss;
        if (CommonUtils.isNullEmptyOrWhitespace(isSetCvss)) {
            return false;
        }
        if ("true".equals(isSetCvss)) {
            m_isSetCvss = true;
        } else if ("false".equals(isSetCvss)) {
            m_isSetCvss = false;
        } else {
            return false;
        }
        vulnerability.setIsSetCvss(m_isSetCvss);
        return true;
    }

    private void setDataCveReferences(Set<String> cveReferences, Vulnerability vulnerability) {
        if (cveReferences != null) {
            Set<CVEReference> cveReferenceList = new HashSet<CVEReference>();
            for (String cveReference : cveReferences) {
                if (CommonUtils.isNullEmptyOrWhitespace(cveReference)) {
                    throw new RuntimeException(new SW360Exception("Invalid yearNumber: property 'yearNumber' cannot be null, empty or whitespace.")
                            .setErrorCode(org.apache.http.HttpStatus.SC_BAD_REQUEST));
                }
                if (!Pattern.matches("^\\d{4}-\\d*", cveReference)) {
                    throw new RuntimeException(new SW360Exception("Invalid yearNumber: property 'yearNumber' is wrong format")
                            .setErrorCode(org.apache.http.HttpStatus.SC_BAD_REQUEST));
                }
                String[] yearAndNumber = cveReference.split("-");
                if (yearAndNumber.length != 2) {
                    throw new RuntimeException(new SW360Exception("Invalid yearNumber: property 'year-Number' is wrong format")
                            .setErrorCode(org.apache.http.HttpStatus.SC_BAD_REQUEST));
                }
                CVEReference m_cVEReference = new CVEReference();
                m_cVEReference.setYear(yearAndNumber[0]);
                m_cVEReference.setNumber(yearAndNumber[1]);
                cveReferenceList.add(m_cVEReference);
            }
            vulnerability.setCveReferences(cveReferenceList);
        }
    }

    /**
     * Generic Entity response method to get externalIds (projects, components, releases)
     */
    public <T> ResponseEntity searchByExternalIds(MultiValueMap<String, String> externalIdsMultiMap,
                                                  AwareOfRestServices<T> service,
                                                  User user) throws TException {

        Map<String, Set<String>> externalIds = getExternalIdsFromMultiMap(externalIdsMultiMap);
        Set<T> sw360Objects = service.searchByExternalIds(externalIds, user);
        List<EntityModel> resourceList = new ArrayList<>();

        sw360Objects.forEach(sw360Object -> {
            T embeddedResource = service.convertToEmbeddedWithExternalIds(sw360Object);
            EntityModel<T> releaseResource = EntityModel.of(embeddedResource);
            resourceList.add(releaseResource);
        });

        CollectionModel<EntityModel> resources = CollectionModel.of(resourceList);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    public CollectionModel<EntityModel<T>> createResources(List<EntityModel<T>> listOfResources) {
        CollectionModel<EntityModel<T>> resources = null;
        if (!listOfResources.isEmpty()) {
            resources = CollectionModel.of(listOfResources);
        }
        return resources;
    }

    private Map<String, Set<String>> getExternalIdsFromMultiMap(MultiValueMap<String, String> externalIdsMultiMap) {
        Map<String, Set<String>> externalIds = new HashMap<>();
        for (String externalIdKey : externalIdsMultiMap.keySet()) {
            externalIds.put(externalIdKey, new HashSet<>(externalIdsMultiMap.get(externalIdKey)));
        }

        return externalIds;
    }

    public <P, R> void checkForCyclicOrInvalidDependencies(P client, R element, User user) throws TException {
        String cyclicLinkedElementPath = null;
        try {
            if (client instanceof ProjectService.Iface) {
                ProjectService.Iface projectClient = (ProjectService.Iface) client;
                cyclicLinkedElementPath = projectClient.getCyclicLinkedProjectPath((Project) element, user);
            } else if (client instanceof ComponentService.Iface) {
                ComponentService.Iface componentClient = (ComponentService.Iface) client;
                cyclicLinkedElementPath = componentClient.getCyclicLinkedReleasePath((Release) element, user);
            }
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
            } else if (sw360Exp.getErrorCode() == 403) {
                if (element instanceof Project) {
                    throw new AccessDeniedException(
                            "Project or its Linked Projects are restricted and / or not accessible");
                }
            } else {
                throw sw360Exp;
            }
        }
        if (!isNullEmptyOrWhitespace(cyclicLinkedElementPath)) {
            if (element instanceof Project) {
                throw new HttpMessageNotReadableException("Cyclic linked Project : " + cyclicLinkedElementPath);
            } else if (element instanceof Release) {
                throw new HttpMessageNotReadableException("Cyclic linked Release : " + cyclicLinkedElementPath);
            }
        }
    }

    public void addEmbeddedFields(String relation,Object value,HalResource<? extends TBase> halResource) {
        if (value instanceof String) {
            if (CommonUtils.isNotNullEmptyOrWhitespace((String) value)) {
                halResource.addEmbeddedResource(relation, value);
            }
        } else if (value instanceof Set) {
            if (CommonUtils.isNotEmpty((Set) value)) {
                halResource.addEmbeddedResource(relation, value);
            }
        } else if (value instanceof Map) {
            if (!CommonUtils.isNullOrEmptyMap((Map) value)) {
                halResource.addEmbeddedResource(relation, value);
            }
        } else if (value != null) {
            halResource.addEmbeddedResource(relation, value);
        }
    }

    public ClearingRequest convertToEmbeddedClearingRequest(ClearingRequest clearingRequest) {
        ClearingRequest embeddedClearingRequest = new ClearingRequest();
        embeddedClearingRequest.setId(clearingRequest.getId());
        embeddedClearingRequest.setAgreedClearingDate(clearingRequest.getAgreedClearingDate());
        embeddedClearingRequest.setRequestedClearingDate(clearingRequest.getRequestedClearingDate());
        embeddedClearingRequest.setRequestingUser(clearingRequest.getRequestingUser());
        embeddedClearingRequest.setClearingState(clearingRequest.getClearingState());
        embeddedClearingRequest.setClearingTeam(clearingRequest.getClearingTeam());
        embeddedClearingRequest.setPriority(clearingRequest.getPriority());
        embeddedClearingRequest.setProjectBU(clearingRequest.getProjectBU());
        embeddedClearingRequest.setProjectId(clearingRequest.getProjectId());
        embeddedClearingRequest.setType(null);
        return embeddedClearingRequest;
    }

}
