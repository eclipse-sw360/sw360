/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.packages;

 import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.EnumUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PackageController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String PACKAGES_URL = "/packages";

    @NonNull
    private final SW360PackageService packageService;

    @NonNull
    private Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final RestControllerHelper<Package> restControllerHelper;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    //Create a Package
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PACKAGES_URL, method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Package>> createPackage(@RequestBody Map<String, Object> reqBodyMap) throws URISyntaxException, TException {
        Package pkg = convertToPackage(reqBodyMap);

        User user = restControllerHelper.getSw360UserFromAuthentication();

        Package sw360Package = packageService.createPackage(pkg, user);
        HalResource<Package> halResource = createHalPackage(sw360Package, user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(sw360Package.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    //Edit a Package
    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = PACKAGES_URL + "/{id}")
    public ResponseEntity<?> patchPackage(@PathVariable("id") String id, @RequestBody Map<String, Object> reqBodyMap) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Package sw360Package = packageService.getPackageForUserById(id);
        Package updatePackage = convertToPackage(reqBodyMap);
        sw360Package = this.restControllerHelper.updatePackage(sw360Package, updatePackage);
        RequestStatus updatePackageStatus = packageService.updatePackage(sw360Package, user);
        HalResource<Package> halPackage = createHalPackage(sw360Package, user);
        if (updatePackageStatus == RequestStatus.ACCESS_DENIED) {
            return new ResponseEntity<String>("Edit action is not allowed for the user. Minimum role required for editing is: "
                + SW360Constants.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(halPackage, HttpStatus.OK);
    }

    //Delete a package
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PACKAGES_URL + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deletePackage(@PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = packageService.deletePackage(id, sw360User);
        if(requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else if(requestStatus == RequestStatus.IN_USE) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            return new ResponseEntity<String>("Delete action is not allowed for the user. Minimum role required for deleting is: "
                    + SW360Constants.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, HttpStatus.FORBIDDEN);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //Get a single package
    @GetMapping(value = PACKAGES_URL + "/{id}")
    public ResponseEntity<EntityModel<Package>> getPackage(
            @PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Package sw360Package = packageService.getPackageForUserById(id);
        HalResource<Package> halPackage = createHalPackage(sw360Package, sw360User);
        return new ResponseEntity<>(halPackage, HttpStatus.OK);
    }

    @GetMapping(value = PACKAGES_URL)
    public ResponseEntity<?> getPackagesForUser(
            Pageable pageable,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "packageManager", required = false) String packageManager,
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            @RequestParam(value = "exactMatch", required = false) boolean isExactMatch,
            HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean isSearchByName = name != null && !name.isEmpty();
        boolean isSearchByType = CommonUtils.isNotNullEmptyOrWhitespace(packageManager);
        boolean isNoFilter = false;
        List<Package> sw360Packages = new ArrayList<>();

        if (isSearchByName) {
            sw360Packages.addAll(packageService.searchPackage("name", name, isExactMatch));
        } else if (isSearchByType) {
            packageManager = packageManager.toUpperCase();

            if (!EnumUtils.isValidEnum(PackageManager.class, packageManager)) {
               return new ResponseEntity<String>("Invalid package manager type. Possible values are "
                        +Arrays.asList(PackageManager.values()), HttpStatus.BAD_REQUEST);
            }
            sw360Packages.addAll(packageService.searchPackage("packageManager", packageManager, isExactMatch));
        } else {
            sw360Packages.addAll(packageService.getPackagesForUser());
            isNoFilter = true;
        }
        return getPackageResponse(pageable, allDetails, request, sw360User, sw360Packages, isNoFilter);
    }

    private Package convertToPackage(Map<String, Object> requestBody) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        return mapper.convertValue(requestBody, Package.class);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(PackageController.class).slash("api" + PACKAGES_URL).withRel("packages"));
        return resource;
    }

    private HalResource<Package> createHalPackage(Package sw360Package, User sw360User) throws TException {
        HalResource<Package> halPackage = new HalResource<>(sw360Package);
        User packageCreator = restControllerHelper.getUserByEmail(sw360Package.getCreatedBy());
        String linkedRelease = sw360Package.getReleaseId();

        restControllerHelper.addEmbeddedUser(halPackage, packageCreator, "createdBy");
        if (CommonUtils.isNotNullEmptyOrWhitespace(linkedRelease)) {
            Release release = releaseService.getReleaseForUserById(linkedRelease, sw360User);

            restControllerHelper.addEmbeddedSingleRelease(halPackage, release);
        }
        return halPackage;
    }

    @NotNull
    private ResponseEntity<CollectionModel<EntityModel<Package>>> getPackageResponse(Pageable pageable,
            boolean allDetails, HttpServletRequest request, User sw360User, List<Package> sw360Packages,
            boolean isNoFilter)
            throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException, TException {
        Map<String, Package> mapOfPackages = new HashMap<>();

        sw360Packages.stream().forEach(pkg -> mapOfPackages.put(pkg.getId(), pkg));
        PaginationResult<Package> paginationResult;
        if (isNoFilter) {
            int totalCount = packageService.getTotalPackagesCounts();
            paginationResult = restControllerHelper.paginationResultFromPaginatedList(request, pageable, sw360Packages,
                    SW360Constants.TYPE_PACKAGE, totalCount);
        } else {
            paginationResult = restControllerHelper.createPaginationResult(request, pageable, sw360Packages,
                    SW360Constants.TYPE_PACKAGE);
        }

        List<EntityModel<Package>> packageResources = new ArrayList<>();
        Consumer<Package> consumer = p -> {
            EntityModel<Package> embeddedPackageResource = null;
            if (!allDetails) {
                Package embeddedPackage = restControllerHelper.convertToEmbeddedPackage(p);
                embeddedPackageResource = EntityModel.of(embeddedPackage);
            } else {
                try {
                    embeddedPackageResource = createHalPackage(p, sw360User);
                } catch (TException e) {
                    throw new RuntimeException("Unable to create package resource: "+e.getMessage());
                }
                if (embeddedPackageResource == null) {
                    return;
                }
            }
            packageResources.add(embeddedPackageResource);
        };

        paginationResult.getResources().stream().forEach(consumer);
        CollectionModel<EntityModel<Package>> resources;
        if (packageResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(Package.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, packageResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }
}
