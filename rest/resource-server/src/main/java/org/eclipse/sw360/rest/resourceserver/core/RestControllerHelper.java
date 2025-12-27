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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.resourcelists.PaginationOptions;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceComparatorGenerator;
import org.eclipse.sw360.datahandler.resourcelists.ResourceListController;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestSize;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.clearingrequest.Sw360ClearingRequestService;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.license.LicenseController;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.EmbeddedModerationRequest;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.ModerationRequestController;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.obligation.Sw360ObligationService;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProject;
import org.jetbrains.annotations.NotNull;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProjectDTO;
import org.springframework.security.access.AccessDeniedException;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.obligation.ObligationController;
import org.eclipse.sw360.rest.resourceserver.packages.PackageController;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

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
    private final Sw360ObligationService obligationService;

    @NonNull
    private final Sw360ClearingRequestService clearingRequestService;

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
    private static final String JWT_SUBJECT = "sub";

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;
    public static final ImmutableSet<ProjectReleaseRelationship._Fields> SET_OF_PROJECTRELEASERELATION_FIELDS_TO_IGNORE = ImmutableSet
            .of(ProjectReleaseRelationship._Fields.CREATED_ON, ProjectReleaseRelationship._Fields.CREATED_BY);
    private static final ImmutableMap<Release._Fields,String> mapOfReleaseFieldsTobeEmbedded = ImmutableMap.of(
            Release._Fields.MODERATORS, "sw360:moderators",
            Release._Fields.ATTACHMENTS, "sw360:attachments",
            Release._Fields.COTS_DETAILS, "sw360:cotsDetails",
            Release._Fields.RELEASE_ID_TO_RELATIONSHIP,"sw360:releaseIdToRelationship",
            Release._Fields.CLEARING_INFORMATION, "sw360:clearingInformation");

    public User getSw360UserFromAuthentication() {
        try {
            String userId = null;
            Object principle = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principle instanceof Jwt jwt) {
                if (jwt.getClaims().containsKey("resource_access") || !jwt.getClaims().containsKey("user_name")) {
                    userId = jwt.getClaim("email");
                    if (userId == null) {
                        userId = jwt.getClaim("mapped_user_email");
                    }
                } else {
                    String clientId = jwt.getClaim(JWT_SUBJECT);
                    if (clientId == null) {
                        userId = jwt.getClaim("user_name");
                        return userService.getUserByEmailOrExternalId(userId);
                    }
                }
            } else if (principle instanceof String) {
                userId = principle.toString();
            } else {
                org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) principle;
                userId = user.getUsername();
            }
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

    public PaginationResult<T> paginationResultFromPaginatedList(HttpServletRequest request, Pageable pageable,
                                                                 List<T> resources, String resourceType, int totalCount)
            throws ResourceClassNotFoundException, PaginationParameterException {
        if (!requestContainsPaging(request)) {
            request.setAttribute(PAGINATION_PARAM_PAGE, pageable.getPageNumber());
            request.setAttribute(PAGINATION_PARAM_PAGE_ENTRIES, pageable.getPageSize());
        }
        PaginationOptions<T> paginationOptions = paginationOptionsFromPageable(pageable, resourceType);
        return resourceListController.getPaginationResultFromPaginatedList(resources,
                paginationOptions, totalCount);
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
            if(sw360User!=null)
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

    public User getUserByEmailOrNull(String emailId) {
        User sw360User;
        try {
            sw360User = userService.getUserByEmail(emailId);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not get user object from backend with email: " + emailId);
            return null;
        }
        return sw360User;
    }

    public void addEmbeddedContributors(HalResource halResource, Set<String> contributors) {
        for (String contributorEmail : contributors) {
            User sw360User = getUserByEmail(contributorEmail);
            addEmbeddedUser(halResource, sw360User, "sw360:contributors");
        }
    }

    public void addEmbeddedDataToHalResourceRelease(HalResource halResource, Release sw360Release) {
        addEmbeddedContributorsToHalResourceRelease(halResource, sw360Release);
        addEmbeddedCreatedByToHalResourceRelease(halResource, sw360Release.getCreatedBy());
        addEmbeddedModifiedByToHalResourceRelease(halResource, sw360Release.getModifiedBy());
        addEmbeddedSubcribeToHalResourceRelease(halResource, sw360Release);
        addEmbeddedCotsDetails(halResource, sw360Release);
    }

    public void addEmbeddedContributorsToHalResourceRelease(HalResource halResource, Release sw360Release) {
        if (!CommonUtils.isNullOrEmptyCollection(sw360Release.getContributors())) {
            Set<String> contributors = sw360Release.getContributors();
            for (String contributorEmail : contributors) {
                User sw360User = getUserByEmail(contributorEmail);
                if (null != sw360User) {
                    addEmbeddedUser(halResource, sw360User, "sw360:contributors");
                    sw360Release.setContributors(null);
                }

            }
        }
    }

    public void addEmbeddedSubcribeToHalResourceRelease(HalResource halResource, Release sw360Release) {
        if (!CommonUtils.isNullOrEmptyCollection(sw360Release.getSubscribers())) {
            Set<String> subscribers = sw360Release.getSubscribers();
            for (String subscribersEmail : subscribers) {
                User sw360User = getUserByEmail(subscribersEmail);
                if (null != sw360User) {
                    addEmbeddedUser(halResource, sw360User, "sw360:subscribers");
                    sw360Release.setSubscribers(null);
                }
            }
        }
    }

    public void addEmbeddedCreatedByToHalResourceRelease(HalResource halRelease, String createdBy) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(createdBy)) {
            User releaseCreator = getUserByEmail(createdBy);
            if (null != releaseCreator )
                addEmbeddedUser(halRelease, releaseCreator, "sw360:createdBy");
        }
    }

    public void addEmbeddedModifiedByToHalResourceRelease(HalResource halRelease, String modifiedBy) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(modifiedBy)) {
            User releaseModify = getUserByEmail(modifiedBy);
            if (null != releaseModify)
                addEmbeddedUser(halRelease, releaseModify, "sw360:modifiedBy");
        }
    }

    public void addEmbeddedLeadArchitect(HalResource halResource, String leadArchitect) {
        User sw360User = getUserByEmail(leadArchitect);
        addEmbeddedUser(halResource, sw360User, "leadArchitect");
    }

    public void addEmbeddedModifiedBy(HalResource halResource, User sw360User, String emailId) {
        addEmbeddedUser(halResource, sw360User, "modifiedBy");
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

    public void addEmbeddedReleaseLinks(
            HalResource halResource,
            List<ReleaseLink> releaseLinks) {
        List<ReleaseLink> releaseLinkInogreAttachments = releaseLinks.stream().map(releaseLink -> releaseLink.setAttachments(null)).collect(Collectors.toList());
        for (ReleaseLink releaseLink : releaseLinkInogreAttachments) {
            addEmbeddedReleaseLink(halResource, releaseLink);
        }
    }

    public void addEmbeddedSpdxDocument(HalResource halResource, SPDXDocument spdxDocument) {
        HalResource<SPDXDocument> halRelease = new HalResource<>(spdxDocument);
        halResource.addEmbeddedResource("sw360:spdxDocument", halRelease);
    }

    public void addEmbeddedDocumentCreationInformation(HalResource halResource, DocumentCreationInformation documentCreationInformation) {
        HalResource<DocumentCreationInformation> halRelease = new HalResource<>(documentCreationInformation);
        halResource.addEmbeddedResource("sw360:documentCreationInformation", halRelease);
    }

    public void addEmbeddedPackageInformation(HalResource halResource, PackageInformation packageInformation) {
        HalResource<PackageInformation> halRelease = new HalResource<>(packageInformation);
        halResource.addEmbeddedResource("sw360:packageInformation", halRelease);
    }

    public void addEmbeddedPackages(
            HalResource<Package> halResource,
            Set<String> packages,
            SW360PackageService sw360PackageService) throws TException {
        for (String packageId : packages) {
            final Package pkg = sw360PackageService.getPackageForUserById(packageId);
            addEmbeddedPackage(halResource, pkg);
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

    public Release convertToEmbeddedReleaseAttachments(Release release) {
        Release embeddedRelease = new Release();
        embeddedRelease.setId(release.getId());
        embeddedRelease.setName(release.getName());
        embeddedRelease.setVersion(release.getVersion());
        embeddedRelease.setAttachments(release.getAttachments());
        embeddedRelease.setComponentType(release.getComponentType());
        embeddedRelease.setClearingState(release.getClearingState());
        embeddedRelease.setType(null);
        return embeddedRelease;
    }

    public void addEmbeddedProjectAttachmentUsage(HalResource halResource, List<Map<String, Object>> releases, List<Map<String, Object>> attachmentUsageMap) {
        halResource.addEmbeddedResource("sw360:release", releases);
        halResource.addEmbeddedResource("sw360:attachmentUsages", attachmentUsageMap);
    }

    public HalResource<Vendor> addEmbeddedVendor(String vendorFullName) {
        Vendor embeddedVendor = convertToEmbeddedVendor(vendorFullName);
        HalResource<Vendor> halVendor = new HalResource<>(embeddedVendor);
        try {
            Vendor vendorByFullName = vendorService.getVendorByFullName(vendorFullName);
            if(vendorByFullName != null) {
                Link vendorSelfLink = linkTo(UserController.class)
                        .slash("api" + VendorController.VENDORS_URL + "/" + vendorByFullName.getId()).withSelfRel();
                halVendor.add(vendorSelfLink);
            }
            return halVendor;
        } catch (Exception e) {
            LOGGER.error("cannot create self link for vendor with full name: " + vendorFullName);
        }
        return null;
    }

    public HalResource<Vendor> addEmbeddedVendor(Vendor vendor) {
        Vendor embeddedVendor = convertToEmbeddedVendor(vendor);
        HalResource<Vendor> halVendor = new HalResource<>(embeddedVendor);
        try {
            Vendor vendorByFullName = vendorService.getVendorByFullName(vendor.getFullname());
            if(vendorByFullName != null) {
                Link vendorSelfLink = linkTo(UserController.class)
                        .slash("api" + VendorController.VENDORS_URL + "/" + vendorByFullName.getId()).withSelfRel();
                halVendor.add(vendorSelfLink);
            }
            return halVendor;
        } catch (Exception e) {
            LOGGER.error("cannot create self link for vendor with full name: " + vendor.getFullname());
        }
        return null;
    }

    public void addEmbeddedLicenses(HalResource<Release> halComponent, Set<String> licenseIds) {
        for (String licenseId : licenseIds) {
            HalResource<License> licenseHalResource = addEmbeddedLicense(licenseId);
            halComponent.addEmbeddedResource("sw360:licenses", licenseHalResource);
        }
    }

    public Set<String> getObligationIdsFromRequestWithValueTrue(Map<String, Boolean> reqBodyMaps) {
        Map<String, Boolean> obligationIdsRequest = reqBodyMaps.entrySet().stream()
                .filter(reqBodyMap-> reqBodyMap.getValue().equals(true))
                .collect(Collectors.toMap(reqBodyMap-> reqBodyMap.getKey(),reqBodyMap -> reqBodyMap.getValue()));
        return obligationIdsRequest.keySet();
    }

    public boolean checkDuplicateLicense(List<License> licenses, String licenseId) {
        return licenses.stream().anyMatch(licenseCheck -> licenseCheck.getShortname().equalsIgnoreCase(licenseId));
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
            LOGGER.error("cannot create a self link for license with id " + licenseId);
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

    public LicenseType convertToEmbeddedLicenseType(LicenseType licenseType) {
        LicenseType embeddedLicenseType = new LicenseType();
        embeddedLicenseType.setId(licenseType.getId());
        embeddedLicenseType.setLicenseType(licenseType.getLicenseType());
        return embeddedLicenseType;
    }

    public void addEmbeddedRelease(HalResource halResource, Release release) {
        Release embeddedRelease = convertToEmbeddedRelease(release);
        HalResource<Release> halRelease = new HalResource<>(embeddedRelease);
        Link releaseLink = linkTo(ReleaseController.class).
                slash("api/releases/" + release.getId()).withSelfRel();
        halRelease.add(releaseLink);
        halResource.addEmbeddedResource("sw360:releases", halRelease);
    }

    public void addEmbeddedReleaseLink(HalResource halResource, ReleaseLink releaseLink) {
        HalResource<ReleaseLink> halRelease = new HalResource<>(releaseLink);
        halResource.addEmbeddedResource("sw360:releaseLinks", halRelease);
    }

    public void addEmbeddedSingleRelease(HalResource halResource, Release release) {
        Release embeddedRelease = convertToEmbeddedRelease(release);
        HalResource<Release> halRelease = new HalResource<>(embeddedRelease);
        Link releaseLink = linkTo(ReleaseController.class).
                slash("api/releases/" + release.getId()).withSelfRel();
        halRelease.add(releaseLink);
        halResource.addEmbeddedResource("sw360:release", halRelease);
    }

    public void addEmbeddedPackage(HalResource<Package> halResource, Package pkg) {
        Package embeddedPackage = convertToEmbeddedPackage(pkg);
        HalResource<Package> halPackage = new HalResource<>(embeddedPackage);
        Link packageLink = linkTo(PackageController.class).
                slash("api/packages/" + pkg.getId()).withSelfRel();
        halPackage.add(packageLink);
        halResource.addEmbeddedResource("sw360:packages", halPackage);
    }

    public HalResource<Release> addEmbeddedReleaseLinks(Release release) {
        final HalResource<Release> releaseResource = new HalResource<>(release);
        Link releaseLink = linkTo(ReleaseController.class)
                .slash("api/releases/" + release.getId()).withSelfRel();
        releaseResource.add(releaseLink);
        return releaseResource;
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

    public Component updateComponent(Component componentToUpdate, ComponentDTO requestBodyComponent) {
        Component component = convertToComponent(requestBodyComponent);
        for(Component._Fields field:Component._Fields.values()) {
            Object fieldValue = component.getFieldValue(field);
            if(fieldValue != null) {
                componentToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return componentToUpdate;
    }

    public Package updatePackage(Package packageToUpdate, Package requestBodyPackage) {
        for (Package._Fields field:Package._Fields.values()) {
            Object fieldValue = requestBodyPackage.getFieldValue(field);
            if (fieldValue != null) {
                packageToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return packageToUpdate;
    }

    public ClearingRequest updateClearingRequest(ClearingRequest crToUpdate, ClearingRequest requestBodyCR) {
        for(ClearingRequest._Fields field: ClearingRequest._Fields.values()) {
            Object fieldValue = requestBodyCR.getFieldValue(field);
            if (fieldValue != null) {
                crToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return crToUpdate;
    }

    public User updateUserProfile(User userToUpdate, Map<String, Object> requestBodyUser, ImmutableSet<User._Fields> setOfUserProfileFields) {
        for (User._Fields field : setOfUserProfileFields) {
            Object fieldValue = requestBodyUser.get(field.getFieldName());
            if (fieldValue != null) {
                switch (field) {
                    case NOTIFICATION_PREFERENCES:
                        Object wantNotification = requestBodyUser.get(User._Fields.WANTS_MAIL_NOTIFICATION.getFieldName());
                        if (wantNotification == null) {
                            if (userToUpdate.isWantsMailNotification()) {
                                userToUpdate.setFieldValue(field, fieldValue);
                            }
                        } else {
                            if (Boolean.TRUE.equals(wantNotification)) {
                                userToUpdate.setFieldValue(field, fieldValue);
                            }
                        }
                        break;
                    default:
                        userToUpdate.setFieldValue(field, fieldValue);
                        break;
                }
            }
        }
        return userToUpdate;
    }

    public User updateUser(User userToUpdate, User requestBodyUser) {
        for (User._Fields field:User._Fields.values()) {
            Object fieldValue = requestBodyUser.getFieldValue(field);
            if (fieldValue != null) {
                userToUpdate.setFieldValue(field, fieldValue);
            }
        }
        return userToUpdate;
    }

    public Component convertToComponent(ComponentDTO componentDTO) {
        Component component = new Component();

        component.setId(componentDTO.getId());
        component.setName(componentDTO.getName());
        component.setDescription(componentDTO.getDescription());
        component.setCreatedOn(componentDTO.getCreatedOn());
        component.setComponentType(componentDTO.getComponentType());
        component.setCreatedBy(componentDTO.getCreatedBy());
        component.setSubscribers(componentDTO.getSubscribers());
        component.setModerators(componentDTO.getModerators());
        component.setComponentOwner(componentDTO.getComponentOwner());
        component.setOwnerAccountingUnit(componentDTO.getOwnerAccountingUnit());
        component.setOwnerGroup(componentDTO.getOwnerGroup());
        component.setOwnerCountry(componentDTO.getOwnerCountry());
        component.setRoles(componentDTO.getRoles());
        component.setExternalIds(componentDTO.getExternalIds());
        component.setAdditionalData(componentDTO.getAdditionalData());
        component.setDefaultVendorId(componentDTO.getDefaultVendorId());
        component.setCategories(componentDTO.getCategories());
        component.setHomepage(componentDTO.getHomepage());
        component.setMailinglist(componentDTO.getMailinglist());
        component.setWiki(componentDTO.getWiki());
        component.setBlog(componentDTO.getBlog());
        component.setAttachments(componentDTO.getAttachments());

        return component;
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

    public License convertLicenseFromRequest(Map<String, Object> reqBodyMap, License licenseUpdate) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);
        License licenseRequestBody = mapper.convertValue(reqBodyMap, License.class);
        if (null == reqBodyMap.get("checked") && !licenseUpdate.isChecked()) {
            licenseRequestBody.setChecked(false);
        }
        return licenseRequestBody;
    }

    private void isLicenseValid(Set<String> licenses) {
        List<String> licenseIncorrect = new ArrayList<>();
        if (CommonUtils.isNotEmpty(licenses)) {
            for (String licenseId : licenses) {
                try {
                    licenseService.getLicenseById(licenseId);
                } catch (Exception e) {
                    try {
                        createMissingLicense(licenseId);
                    } catch (Exception createException) {
                        licenseIncorrect.add(licenseId);
                    }
                }
            }
        }
        if (!licenseIncorrect.isEmpty()) {
            throw new BadRequestClientException("License with ids " + licenseIncorrect + " does not exist in SW360 database and could not be created automatically.");
        }
    }

    private void createMissingLicense(String licenseId) throws Exception {
        License newLicense = new License();
        newLicense.setId(licenseId);
        newLicense.setShortname(licenseId);
        newLicense.setFullname(licenseId);
        User user = getSw360UserFromAuthentication();
        licenseService.createLicense(newLicense, user);
    }

    public License mapLicenseRequestToLicense(License licenseRequestBody, License licenseUpdate) {
        for (License._Fields field : License._Fields.values()) {
            Object fieldValue = licenseRequestBody.getFieldValue(field);
            if (fieldValue != null) {
                switch (field) {
                    case OBLIGATION_DATABASE_IDS:
                        isObligationValid(licenseRequestBody.getObligationDatabaseIds());
                        break;
                    default:
                }
                licenseUpdate.setFieldValue(field, fieldValue);
            }
        }
        return licenseUpdate;
    }

    private void isObligationValid(Set<String> obligationIds) {
        List <String> obligationIncorrect = new ArrayList<>();
        if (CommonUtils.isNotEmpty(obligationIds)) {
            for (String obligationId : obligationIds) {
                try {
                    obligationService.getObligationById(obligationId, null);
                } catch (Exception e) {
                    obligationIncorrect.add(obligationId);
                }
            }
        }
        if (!obligationIncorrect.isEmpty()) {
            throw new BadRequestClientException("Obligation with ids " + obligationIncorrect + " does not exist in SW360 database.");
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
        embeddedProject.setDescription(project.getDescription());
        embeddedProject.setProjectResponsible(project.getProjectResponsible());
        embeddedProject.setProjectOwner(project.getProjectOwner());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setState(project.getState());
        embeddedProject.setClearingState(project.getClearingState());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setVisbility(project.getVisbility());
        embeddedProject.setBusinessUnit(project.getBusinessUnit());
        embeddedProject.setEnableSvm(project.isEnableSvm());
        embeddedProject.setTag(project.getTag());
        embeddedProject.setType(null);
        embeddedProject.setClearingRequestId(project.getClearingRequestId());
        return embeddedProject;
    }

    public Project convertToEmbeddedLinkedProject(Project project) {
        Project embeddedProject = new EmbeddedProject();
        embeddedProject.setName(project.getName());
        embeddedProject.setId(project.getId());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setState(project.getState());
        embeddedProject.setClearingState(project.getClearingState());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setReleaseIdToUsage(project.getReleaseIdToUsage());
        embeddedProject.setLinkedProjects(project.getLinkedProjects());
        embeddedProject.setEnableSvm(project.isEnableSvm());
        embeddedProject.setEnableVulnerabilitiesDisplay(project.isEnableVulnerabilitiesDisplay());
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
        embeddedComponent.setDescription(component.getDescription());
        embeddedComponent.setComponentType(component.getComponentType());
        embeddedComponent.setVisbility(component.getVisbility());
        embeddedComponent.setMainLicenseIds(component.getMainLicenseIds());
        embeddedComponent.setVcs(component.getVcs());
        if (CommonUtils.isNotNullEmptyOrWhitespace(component.getDefaultVendorId())) {
            try {
                Vendor defaultVendor = vendorService.getVendorById(component.getDefaultVendorId());
                embeddedComponent.setDefaultVendor(defaultVendor);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to retrieve default vendor '{}' from SW360 database.",
                        component.getDefaultVendorId(), e);
            }
        }
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
        embeddedRelease.setClearingState(release.getClearingState());
        embeddedRelease.setType(null);
        return embeddedRelease;
    }

    public Release convertToEmbeddedLinkedProjectsReleases(Release release) {
        Release embeddedRelease = new Release();
        embeddedRelease.setId(release.getId());
        embeddedRelease.setName(release.getName());
        embeddedRelease.setVersion(release.getVersion());
        embeddedRelease.setComponentId(release.getComponentId());
        embeddedRelease.setMainlineState(release.getMainlineState());
        embeddedRelease.setClearingState(release.getClearingState());
        embeddedRelease.setVendor(release.getVendor());
        embeddedRelease.setType(null);
        return embeddedRelease;
    }

    public Package convertToEmbeddedPackage(Package pkg) {
        Package embeddedPackage = new Package();
        embeddedPackage.setId(pkg.getId());
        embeddedPackage.setName(pkg.getName());
        embeddedPackage.setVersion(pkg.getVersion());
        embeddedPackage.setPackageManager(pkg.getPackageManager());
        embeddedPackage.setLicenseIds(pkg.getLicenseIds());
        embeddedPackage.setPurl(pkg.getPurl());
        return embeddedPackage;
    }

    public Release convertToEmbeddedReleaseWithDet(Release release) {
        List<String> fields = List.of("id", "name", "version", "cpeid", "createdBy", "createdOn", "componentId","componentType",
                "additionalData", "clearingState", "mainLicenseIds", "binaryDownloadurl", "sourceCodeDownloadurl",
                "releaseDate", "externalIds", "languages", "operatingSystems", "softwarePlatforms", "vendor",
                "mainlineState", "packageIds");
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
        embeddedLicense.setShortname(license.getShortname());
        embeddedLicense.setChecked(license.isChecked());
        embeddedLicense.setLicenseType(license.getLicenseType());
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

    public Release convertToEmbeddedLinkedRelease(Release release) {
        Release embeddedRelease = new Release();
        embeddedRelease.setId(release.getId());
        embeddedRelease.setMainlineState(release.getMainlineState());
        embeddedRelease.setClearingState(release.getClearingState());
        embeddedRelease.setMainLicenseIds(release.getMainLicenseIds());
        embeddedRelease.setOtherLicenseIds(release.getOtherLicenseIds());
        embeddedRelease.setName(release.getName());
        embeddedRelease.setComponentType(release.getComponentType());
        embeddedRelease.setVersion(release.getVersion());
        embeddedRelease.setType(null);
        return embeddedRelease;
    }

    public void addEmbeddedProjectReleases(HalResource halResource, List<EntityModel<Release>> releases) {
        halResource.addEmbeddedResource("sw360:release", releases);
    }

    public User convertToEmbeddedUser(User user) {
        User embeddedUser = new User();
        embeddedUser.setId(user.getId());
        embeddedUser.setFullname(user.getFullname());
        embeddedUser.setEmail(user.getEmail());
        embeddedUser.setType(null);
        return embeddedUser;
    }

    public User convertToEmbeddedGetUsers(User user) {
        User embeddedUser = new User();
        embeddedUser.setId(user.getId());
        embeddedUser.setFullname(user.getFullname());
        embeddedUser.setEmail(user.getEmail());
        embeddedUser.setGivenname(user.getGivenname());
        embeddedUser.setLastname(user.getLastname());
        embeddedUser.setDepartment(user.getDepartment());
        embeddedUser.setUserGroup(user.getUserGroup());
        embeddedUser.setSecondaryDepartmentsAndRoles(user.getSecondaryDepartmentsAndRoles());
        embeddedUser.setDeactivated(user.isDeactivated());
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
        embeddedObligation.setObligationType(obligation.getObligationType());
        embeddedObligation.setObligationLevel(obligation.getObligationLevel());
        embeddedObligation.setId(obligation.getId());
        embeddedObligation.setWhitelist(obligation.getWhitelist());
        embeddedObligation.setText(obligation.getText());
        embeddedObligation.setType(null);
        return embeddedObligation;
    }

    public Vendor convertToEmbeddedVendor(Vendor vendor) {
        Vendor embeddedVendor = new Vendor();
        embeddedVendor.setId(vendor.getId());
        embeddedVendor.setShortname(vendor.getShortname());
        embeddedVendor.setFullname(vendor.getFullname());
        embeddedVendor.setUrl(vendor.getUrl());
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
        attachment.setCreatedOn(null);
        attachment.setCreatedBy(null);
        attachment.setCheckedBy(null);
        attachment.setCheckedOn(null);
        attachment.setCheckedTeam(null);
        attachment.setCheckedComment(null);
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

    public Vulnerability convertToEmbeddedVulnerability(VulnerabilityDTO vulnerabilityDto) {
        Vulnerability embeddedVulnerability = new Vulnerability(vulnerabilityDto.getExternalId());
        embeddedVulnerability.setId(vulnerabilityDto.getId());
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
                                    .setErrorCode(org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST));
                        }
                    } else if (fieldDTO.equals(VulnerabilityApiDTO._Fields.IS_SET_CVSS)) {
                        if(!setDataIsSetCvss(vulnerabilityApiDTO.getIsSetCvss(), vulnerability)) {
                            throw new RuntimeException(new SW360Exception("Invalid isSetCvss: property 'isSetCvss' should be a valid isSetCvss.")
                                    .setErrorCode(org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST));
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
                            .setErrorCode(org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST));
                }
                if (!Pattern.matches("^\\d{4}-\\d*", cveReference)) {
                    throw new RuntimeException(new SW360Exception("Invalid yearNumber: property 'yearNumber' is wrong format")
                            .setErrorCode(org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST));
                }
                String[] yearAndNumber = cveReference.split("-");
                if (yearAndNumber.length != 2) {
                    throw new RuntimeException(new SW360Exception("Invalid yearNumber: property 'year-Number' is wrong format")
                            .setErrorCode(org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST));
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
     * Bypass spring query parser to distinguish between URL encoded and
     * un-encoded ids.
     *
     * @param queryString Query from request
     * @return Query parsed as a value map.
     */
    public @NotNull MultiValueMap<String, String> parseQueryStringForExtIds(String queryString) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance().query(queryString);
            builder.build().getQueryParams().forEach((key, values) -> values.forEach(value -> parameters.add(key, urlDecode(value))));
        }
        return parameters;
    }

    public Map<String, String> parseQueryString(String queryString) {
        Map<String, String> parameters = new HashMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance().query(queryString);
            builder.build().getQueryParams().forEach((key, values) -> parameters.put(key, urlDecode(values.get(0))));
        }
        return parameters;
    }

    public static String urlDecode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // This exception occurs if the specified encoding is not supported
            throw new IllegalArgumentException("Unsupported encoding: " + e.getMessage());
        }
    }
    /**
     * Generic Entity response method to get externalIds (projects, components, releases)
     */
    public <T> ResponseEntity searchByExternalIds(String queryString, AwareOfRestServices<T> service, User user) throws TException {

        MultiValueMap<String, String> externalIdsMultiMap = parseQueryStringForExtIds(queryString);
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
                throw new BadRequestClientException("Dependent document Id/ids not valid.", sw360Exp);
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
                throw new BadRequestClientException("Cyclic linked Project : " + cyclicLinkedElementPath);
            } else if (element instanceof Release) {
                throw new BadRequestClientException("Cyclic linked Release : " + cyclicLinkedElementPath);
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
        embeddedClearingRequest.setClearingType(clearingRequest.getClearingType());
        embeddedClearingRequest.setTimestamp(clearingRequest.getTimestamp());
        embeddedClearingRequest.setClearingSize(clearingRequest.getClearingSize());
        return embeddedClearingRequest;
    }

    public Comment convertToEmbeddedComment(Comment comment) {
        Comment embeddedComment = new Comment();
        embeddedComment.setText(comment.getText());
        embeddedComment.setCommentedBy(comment.getCommentedBy());
        embeddedComment.setCommentedOn(comment.getCommentedOn());
        embeddedComment.setAutoGenerated(comment.isAutoGenerated());
        return embeddedComment;
    }

    public ModerationRequest convertToEmbeddedModerationRequest(ModerationRequest moderationRequest) {
        ModerationRequest embeddedModerationRequest = new EmbeddedModerationRequest();
        embeddedModerationRequest.setId(moderationRequest.getId())
                .setTimestamp(moderationRequest.getTimestamp())
                .setTimestampOfDecision(moderationRequest.getTimestampOfDecision())
                .setDocumentId(moderationRequest.getDocumentId())
                .setDocumentType(moderationRequest.getDocumentType())
                .setDocumentName(moderationRequest.getDocumentName())
                .setComponentType(moderationRequest.getComponentType())
                .setRequestingUser(moderationRequest.getRequestingUser())
                .setRequestingUserDepartment(moderationRequest.getRequestingUserDepartment())
                .setModerators(moderationRequest.getModerators())
                .setModerationState(moderationRequest.getModerationState())
                .setReviewer(moderationRequest.getReviewer())
                .setType(null);
        return embeddedModerationRequest;
    }

    public void addEmbeddedModerationRequest(HalResource<ModerationRequest> halModerationRequest,
                                             Set<String> moderationIds,
                                             Sw360ModerationRequestService sw360ModerationRequestService)
            throws TException {
        for (String moderationId : moderationIds) {
            final ModerationRequest moderationRequest = sw360ModerationRequestService.getModerationRequestById(moderationId);
            addEmbeddedModerationRequest(halModerationRequest, moderationRequest, false);
        }
    }

    public void addEmbeddedModerationRequest(HalResource halResource, ModerationRequest moderationRequest,
                                             boolean isSingleRequest) {
        ModerationRequest embeddedRequest = convertToEmbeddedModerationRequest(moderationRequest);
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(embeddedRequest);
        Link moderationRequestLink = linkTo(ModerationRequest.class)
                .slash("api" + ModerationRequestController.MODERATION_REQUEST_URL + "/" + moderationRequest.getId())
                .withSelfRel();
        halModerationRequest.add(moderationRequestLink);
        halResource.addEmbeddedResource(isSingleRequest ? "sw360:moderationRequest" : "sw360:moderationRequests",
                halModerationRequest);
    }

    public void addEmbeddedDataToComponent(HalResource halResource, Component sw360Component) {
        addEmbeddedModifiedByToComponent(halResource,sw360Component);
        addEmbeddedComponentOwnerToComponent(halResource,sw360Component);
        addEmbeddedSubcribeToHalResourceComponent(halResource,sw360Component);
    }

    public void addEmbeddedModifiedByToComponent(HalResource halResource, Component sw360Component) {
        if (sw360Component.getModifiedBy() != null) {
            User componentModify = getUserByEmail(sw360Component.getModifiedBy());
            if (null != componentModify)
                addEmbeddedUser(halResource, componentModify, "modifiedBy");
        }
    }

    public void addEmbeddedComponentOwnerToComponent(HalResource halResource, Component sw360Component) {
        if (sw360Component.getComponentOwner() != null) {
            User componentOwner = getUserByEmail(sw360Component.getComponentOwner());
            if (null != componentOwner) {
                addEmbeddedUser(halResource, componentOwner, "componentOwner");
                sw360Component.setComponentOwner(null);
            }
        }
    }

    public void addEmbeddedSubcribeToHalResourceComponent(HalResource halResource, Component sw360Component) {
        if (!CommonUtils.isNullOrEmptyCollection(sw360Component.getSubscribers())) {
            Set<String> subscribers = sw360Component.getSubscribers();
            for (String subscribersEmail : subscribers) {
                User sw360User = getUserByEmail(subscribersEmail);
                if (null != sw360User) {
                    addEmbeddedUser(halResource, sw360User, "sw360:subscribers");
                    sw360Component.setSubscribers(null);
                }
            }
        }
    }

    public void addEmbeddedProjectDTO(HalResource<ProjectDTO> halProject, Set<String> projectIds, Sw360ProjectService sw360ProjectService, User user) throws TException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (String projectId : projectIds) {
            final Project project = sw360ProjectService.getProjectForUserById(projectId, user);
            addEmbeddedProjectDTO(halProject, project);
        }
    }

    public void addEmbeddedProjectDTO(HalResource halResource, Project project) {
        ProjectDTO embeddedProject = convertToEmbeddedProjectDTO(project);
        HalResource<ProjectDTO> halProject = new HalResource<>(embeddedProject);
        Link projectLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/" + project.getId()).withSelfRel();
        halProject.add(projectLink);
        halResource.addEmbeddedResource("sw360:projectDTOs", halProject);
    }

    public ProjectDTO convertToEmbeddedProjectDTO(Project project) {
        ProjectDTO embeddedProject = new EmbeddedProjectDTO();
        embeddedProject.setName(project.getName());
        embeddedProject.setId(project.getId());
        embeddedProject.setProjectType(project.getProjectType());
        embeddedProject.setVersion(project.getVersion());
        embeddedProject.setVisbility(project.getVisbility());
        embeddedProject.setType(null);
        return embeddedProject;
    }

    public void addEmbeddedCotsDetails(HalResource halResource, Release release) {
        if (null != release.getCotsDetails() && release.getComponentType().equals(ComponentType.COTS)) {
            HalResource<COTSDetails> cotsDetailsHalResource = new HalResource<>(release.getCotsDetails());
            if (CommonUtils.isNotNullEmptyOrWhitespace(release.getCotsDetails().getCotsResponsible())) {
                User sw360User = userService.getUserByEmail(release.getCotsDetails().getCotsResponsible());
                if (null != sw360User) {
                    addEmbeddedUser(cotsDetailsHalResource, sw360User, "sw360:cotsResponsible");
                }
            }
            addEmbeddedFields("sw360:cotsDetail", cotsDetailsHalResource, halResource);
        }
    }

    public void addEmbeddedProjectResponsible(HalResource<Project> halResource, String projectResponsible) {
        User sw360User = getUserByEmail(projectResponsible);
        if(sw360User!=null) {
            addEmbeddedUser(halResource, sw360User, "projectResponsible");
        }
    }

    public void addEmbeddedSecurityResponsibles (HalResource<Project> halResource, Set<String> securityResponsibles) {
        for (String securityResponsible : securityResponsibles) {
            User sw360User = getUserByEmail(securityResponsible);
            if(sw360User!=null) {
                addEmbeddedUser(halResource, sw360User, "securityResponsibles");
            }
        }
    }

    public void addEmbeddedClearingTeam(HalResource<Project> userHalResource, String clearingTeam, String resource) {
        User sw360User = getUserByEmail(clearingTeam);
        if(sw360User!=null)
            addEmbeddedUser(userHalResource, sw360User, resource);
    }

    public void addEmbeddedOtherLicenses(HalResource<Release> halRelease, Set<String> licenseIds) {
        for (String licenseId : licenseIds) {
            HalResource<License> licenseHalResource = addEmbeddedLicense(licenseId);
            halRelease.addEmbeddedResource("sw360:otherLicenses", licenseHalResource);
        }
    }

    public void addEmbeddedTimestampOfDecision(HalResource<ClearingRequest> halClearingRequest, long timestampOfDecision) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.systemDefault());
        String humanReadableDate = formatter.format(Instant.ofEpochMilli(timestampOfDecision));
        halClearingRequest.addEmbeddedResource("requestClosedOn", humanReadableDate);
    }

    public String getBaseUrl(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        return requestURL.substring(0, requestURL.indexOf(request.getRequestURI()));
    }

    public VulnerabilitySummary convertToEmbeddedVulnerabilitySumm(VulnerabilitySummary sw360Vul) {
        VulnerabilitySummary embeddedProject = new VulnerabilitySummary();
        embeddedProject.setComment(sw360Vul.getComment());
        embeddedProject.setAction(sw360Vul.getAction());
        embeddedProject.setTitle(sw360Vul.getTitle());
        embeddedProject.setMatchedBy(sw360Vul.getMatchedBy());
        embeddedProject.setUsedNeedle(sw360Vul.getUsedNeedle());
        embeddedProject.setProjectName(sw360Vul.getProjectName());
        embeddedProject.setExternalId(sw360Vul.getExternalId());
        embeddedProject.setDescription(sw360Vul.getDescription());
        embeddedProject.setPriority(sw360Vul.getPriority());
        embeddedProject.setPriorityToolTip(sw360Vul.getPriorityToolTip());
        embeddedProject.setProjectRelevance(sw360Vul.getProjectRelevance());
        embeddedProject.setIntReleaseId(sw360Vul.getIntReleaseId());
        embeddedProject.setIntReleaseName(sw360Vul.getIntReleaseName());
        return embeddedProject;
    }
    public void addEmbeddedDatesClearingRequest(HalResource<ClearingRequest> halClearingRequest, ClearingRequest clearingRequest, boolean isSingleRequest) {
        halClearingRequest.addEmbeddedResource("createdOn", SW360Utils.convertEpochTimeToDate(clearingRequest.getTimestamp()));
        if (isSingleRequest) {
            halClearingRequest.addEmbeddedResource("lastUpdatedOn", SW360Utils.convertEpochTimeToDate(clearingRequest.getModifiedOn()));
        }
    }

    public void addEmbeddedReleaseDetails(HalResource<ClearingRequest> halClearingRequest, Project project) {
        ReleaseClearingStateSummary clearingInfo = project.getReleaseClearingStateSummary();
        int openReleaseCount = SW360Utils.getOpenReleaseCount(clearingInfo);
        int totalReleaseCount = SW360Utils.getTotalReleaseCount(clearingInfo);
        halClearingRequest.addEmbeddedResource("openRelease", openReleaseCount);
        halClearingRequest.addEmbeddedResource("totalRelease", totalReleaseCount);
    }

    public ReleaseLink convertToReleaseLink(Release release, ReleaseRelationship relationship) {
        ReleaseLink releaseLink = new ReleaseLink();
        releaseLink.setId(release.getId());
        releaseLink.setClearingState(release.getClearingState());
        releaseLink.setLicenseIds(release.getMainLicenseIds());
        releaseLink.setName(release.getName());
        releaseLink.setVersion(release.getVersion());
        releaseLink.setReleaseRelationship(relationship);
        releaseLink.setComponentId(release.getComponentId());
        return releaseLink;
    }

    public HalResource<Release> createHalReleaseResourceWithAllDetails(Release release) {
        HalResource<Release> halRelease = new HalResource<>(release);
        Link componentLink = linkTo(ReleaseController.class)
                .slash("api" + ComponentController.COMPONENTS_URL + "/" + release.getComponentId())
                .withRel("component");
        halRelease.add(componentLink);
        release.setComponentId(null);
        Set<String> packageIds = release.getPackageIds();

        if (packageIds != null) {
            for (String id : release.getPackageIds()) {
                Link packageLink = linkTo(ReleaseController.class)
                        .slash("api" + PackageController.PACKAGES_URL + "/" + id).withRel("packages");
                halRelease.add(packageLink);
            }
        }
        release.setPackageIds(null);
        for (Map.Entry<Release._Fields, String> field : mapOfReleaseFieldsTobeEmbedded.entrySet()) {
            addEmbeddedFields(field.getValue(), release.getFieldValue(field.getKey()), halRelease);
        }
        // Do not add attachment as it is an embedded field
        release.unsetAttachments();
        return halRelease;
    }
    public ClearingRequest updateCRSize(ClearingRequest clearingRequest, Project project, User sw360User) throws TException {
        int openReleaseCount = SW360Utils.getOpenReleaseCount(project.getReleaseClearingStateSummary());
        ClearingRequestSize currentSize = SW360Utils.determineCRSize(openReleaseCount);
        ClearingRequestSize initialSize = clearingRequest.getClearingSize();
        if(initialSize == null) return clearingRequest;
        if(!initialSize.equals(ClearingRequestSize.VERY_LARGE)) {
            int limit = SW360Utils.CLEARING_REQUEST_SIZE_MAP.get(initialSize);
            if(openReleaseCount > limit){
                clearingRequestService.updateClearingRequestForChangeInClearingSize(clearingRequest.getId(), currentSize);
            }
        }
        return clearingRequestService.getClearingRequestById(clearingRequest.getId(), sw360User);
    }

    public boolean isWriteActionAllowed(Object object, User user) {
        return makePermission(object, user).isActionAllowed(RequestedAction.WRITE);
    }

    public void throwIfSecurityUser(User user) {
        if (PermissionUtils.isSecurityUser(user)) {
            throw new AccessDeniedException("User is not allowed to access this resource.");
        }
    }
}
