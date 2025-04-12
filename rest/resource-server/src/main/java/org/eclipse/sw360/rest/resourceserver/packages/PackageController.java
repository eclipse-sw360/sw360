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

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.EnumUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
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

    @Operation(
            summary = "Create a new package.",
            description = "Create a new package.",
            tags = {"Packages"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PACKAGES_URL, method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Package>> createPackage(
            @Parameter(description = "The package to be created.",
                    schema = @Schema(implementation = Package.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
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
    @Operation(
            summary = "Update a package.",
            description = "Update a package.",
            tags = {"Packages"},
            responses = {@ApiResponse(
                    responseCode = "200",
                    content = {@Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = Package.class))}
            ), @ApiResponse(
                    responseCode = "403",
                    description = "User role not allowed",
                    content = {@Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = ResponseEntity.class))}
            )}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = PACKAGES_URL + "/{id}")
    public ResponseEntity<?> patchPackage(
            @Parameter(description = "The id of the package to be updated.")
            @PathVariable("id") String id,
            @Parameter(description = "The updated fields of package.",
                    schema = @Schema(implementation = Package.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Package sw360Package = packageService.getPackageForUserById(id);
        Package updatePackage = convertToPackage(reqBodyMap);
        sw360Package = this.restControllerHelper.updatePackage(sw360Package, updatePackage);
        RequestStatus updatePackageStatus = packageService.updatePackage(sw360Package, user);
        HalResource<Package> halPackage = createHalPackage(sw360Package, user);
        if (updatePackageStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Edit action is not allowed for the user. " +
                    "Minimum role required for editing is: " +
                    SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER));
        }
        return new ResponseEntity<>(halPackage, HttpStatus.OK);
    }

    //Delete a package
    @Operation(
            summary = "Delete a package.",
            description = "Delete a package.",
            tags = {"Packages"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PACKAGES_URL + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deletePackage(
            @Parameter(description = "The id of the package to be deleted.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = packageService.deletePackage(id, sw360User);
        if(requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else if(requestStatus == RequestStatus.IN_USE) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } else if (requestStatus == RequestStatus.ACCESS_DENIED) {
            throw new AccessDeniedException("Delete action is not allowed for the user. Minimum role required for deleting is: "
                    + SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER));
        } else {
            throw new SW360Exception();
        }
    }

    //Get a single package
    @Operation(
            summary = "Get a package by id.",
            description = "Get a package by id.",
            tags = {"Packages"}
    )
    @GetMapping(value = PACKAGES_URL + "/{id}")
    public ResponseEntity<EntityModel<Package>> getPackage(
            @Parameter(description = "The id of the package to be retrieved.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Package sw360Package = packageService.getPackageForUserById(id);
        HalResource<Package> halPackage = createHalPackage(sw360Package, sw360User);
        return new ResponseEntity<>(halPackage, HttpStatus.OK);
    }

    @Operation(
            summary = "Get packages for user.",
            description = "Get packages for user with filters.",
            tags = {"Packages"},
            responses = {@ApiResponse(
                    responseCode = "200",
                    content = {@Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Package.class)))}
            ), @ApiResponse(
                    responseCode = "400",
                    description = "Invalid package manager type",
                    content = {@Content(mediaType = MediaTypes.HAL_JSON_VALUE,
                            schema = @Schema(implementation = ResponseEntity.class))}
            )}
    )
    @GetMapping(value = PACKAGES_URL)
    public ResponseEntity<?> getPackagesForUser(
            Pageable pageable,
            @Parameter(description = "The name of the package.")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "The version of the package.")
            @RequestParam(value = "version", required = false) String version,
            @Parameter(description = "The pURL of the package.")
            @RequestParam(value = "purl", required = false) String purl,
            @Parameter(description = "The package manager of the package.")
            @RequestParam(value = "packageManager", required = false) String packageManager,
            @Parameter(description = "Get all details of the package.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String queryString = request.getQueryString();
        Map<String, String> params = restControllerHelper.parseQueryString(queryString);
        boolean isSearchByName = CommonUtils.isNotNullEmptyOrWhitespace(name);
        boolean isSearchByPackageManager = CommonUtils.isNotNullEmptyOrWhitespace(packageManager);
        boolean isSearchByVersion = CommonUtils.isNotNullEmptyOrWhitespace(version);
        boolean isSearchByPurl = CommonUtils.isNotNullEmptyOrWhitespace(purl);
        List<Package> sw360Packages = new ArrayList<>();

        if (isSearchByName) {
            sw360Packages.addAll(packageService.searchPackageByName(params.get("name")));
        } else if (isSearchByPackageManager) {
            packageManager = packageManager.toUpperCase();

            if (!EnumUtils.isValidEnum(PackageManager.class, packageManager)) {
                throw new BadRequestClientException("Invalid package manager type. Possible values are "
                        + Arrays.asList(PackageManager.values()));
            }
            sw360Packages.addAll(packageService.searchByPackageManager(packageManager));
        } else if (isSearchByVersion) {
            sw360Packages.addAll(packageService.searchPackageByVersion(params.get("version")));
        } else if (isSearchByPurl) {
            sw360Packages.addAll(packageService.searchPackageByPurl(params.get("purl")));
        } else {
            sw360Packages.addAll(packageService.getPackagesForUser());
        }
        return getPackageResponse(version, purl, packageManager, pageable, allDetails, request, sw360User, sw360Packages);
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
        if (sw360Package.getModifiedBy() != null) {
            restControllerHelper.addEmbeddedModifiedBy(halPackage, sw360User, "modifiedBy");
        }
        return halPackage;
    }

    @NotNull
    private ResponseEntity<CollectionModel<EntityModel<Package>>> getPackageResponse(
            String version, String purl, String packageManager, Pageable pageable,
            boolean allDetails, HttpServletRequest request, User sw360User, List<Package> sw360Packages
    ) throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException {
        Map<String, Package> mapOfPackages = new HashMap<>();

        sw360Packages.stream().forEach(pkg -> mapOfPackages.put(pkg.getId(), pkg));
        PaginationResult<Package> paginationResult;
        paginationResult = restControllerHelper.createPaginationResult(request, pageable, sw360Packages, SW360Constants.TYPE_PACKAGE);

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

        paginationResult.getResources().stream()
        .filter(pkg -> packageManager == null || packageManager.equals(pkg.getPackageManager().toString()))
        .filter(pkg -> version == null || version.isEmpty() || version.equals(pkg.getVersion()))
        .filter(pkg -> purl == null || purl.isEmpty() || purl.equals(pkg.getPurl())).forEach(consumer);

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
