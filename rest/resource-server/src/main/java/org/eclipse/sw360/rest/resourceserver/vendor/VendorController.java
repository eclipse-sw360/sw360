/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Vendor.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.net.URISyntaxException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class VendorController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String VENDORS_URL = "/vendors";

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "List all of the service's vendors.",
            description = "List all of the service's vendors.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of vendors."),
            @ApiResponse(responseCode = "204", description = "No vendors found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @GetMapping(value = VENDORS_URL)
    public ResponseEntity<CollectionModel<EntityModel<Vendor>>> getVendors(
            @Parameter(description = "Search text")
            @RequestParam(value = "searchText", required = false) String searchText,
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request
    ) throws URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<Vendor> sw360Vendors = new ArrayList<>();
        Map<PaginationData, List<Vendor>> paginatedVendors = null;
        if (!isNullOrEmpty(searchText)) {
            paginatedVendors = vendorService.searchVendors(searchText, pageable);
        } else {
            paginatedVendors = vendorService.getVendors(pageable);
        }

        PaginationResult<Vendor> paginationResult;
        if (paginatedVendors != null) {
            sw360Vendors.addAll(paginatedVendors.values().iterator().next());
            int totalCount = Math.toIntExact(paginatedVendors.keySet().stream()
                    .findFirst().map(PaginationData::getTotalRowCount).orElse(0L));
            paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                    request, pageable, sw360Vendors, SW360Constants.TYPE_VENDOR, totalCount);
        } else {
            paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    sw360Vendors, SW360Constants.TYPE_VENDOR);
        }

        List<EntityModel<Vendor>> vendorResources = new ArrayList<>();
        for (Vendor v: paginationResult.getResources()) {
            Vendor embeddedVendor = restControllerHelper.convertToEmbeddedVendor(v);
            vendorResources.add(EntityModel.of(embeddedVendor));
        }

        CollectionModel<EntityModel<Vendor>> resources;
        if (vendorResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(Vendor.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, vendorResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            summary = "Get a single vendor.",
            description = "Get a single vendor by id.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vendor found.",
                    content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Vendor.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Vendor not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @GetMapping(value = VENDORS_URL + "/{id}")
    public ResponseEntity<EntityModel<Vendor>> getVendor(
            @Parameter(description = "The id of the vendor to get.")
            @PathVariable("id") String id
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Vendor sw360Vendor = vendorService.getVendorById(id);
        HalResource<Vendor> halResource = createHalVendor(sw360Vendor);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get the releases used by the vendor.",
            description = "Get the releases by vendor id.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of releases."),
            @ApiResponse(responseCode = "204", description = "No releases found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Vendor not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @GetMapping(value = VENDORS_URL + "/{id}/releases")
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getReleases(
            @Parameter(description = "The id of the vendor to get.")
            @PathVariable("id") String id
    ) throws SW360Exception {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        if(vendorService.getVendorById(id) == null){
            throw new ResourceNotFoundException("Vendor with id " + id + " not found.");
        }
        try {
            Set<Release> releases = vendorService.getAllReleaseList(id);
            List<EntityModel<Release>> resources = new ArrayList<>();
            releases.forEach(rel -> {
                Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(rel);
                resources.add(EntityModel.of(embeddedRelease));
            });
            CollectionModel<EntityModel<Release>> relResources = restControllerHelper.createResources(resources);

            HttpStatus status = relResources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
            return new ResponseEntity<>(relResources, status);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a vendor.",
            description = "Delete vendor by id.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vendor deleted successfully."),
            @ApiResponse(responseCode = "400", description = "Vendor cannot be deleted",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN authority required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Vendor not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = VENDORS_URL + "/{id}")
    public ResponseEntity<?> deleteVendor(
            @Parameter(description = "The id of the vendor to be deleted.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User is not authorized to delete vendors");
        }
        Vendor sw360Vendor = vendorService.getVendorById(id);
        if (sw360Vendor == null) {
            throw new ResourceNotFoundException("Vendor with id " + id + " not found.");
        }
        RequestStatus requestStatus = vendorService.deleteVendorByid(id, sw360User);
        if (requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>("Vendor with full name " + sw360Vendor.getFullname() + " deleted successfully.", HttpStatus.OK);
        }
        throw new BadRequestClientException("Vendor with full name " + sw360Vendor.getFullname() + " cannot be deleted.");
    }

    @Operation(
            summary = "Create a new vendor.",
            description = "Create a new vendor.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vendor created successfully.",
                    content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Vendor.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN authority required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = VENDORS_URL)
    public ResponseEntity<?> createVendor(
            @Parameter(description = "The vendor to be created.")
            @RequestBody Vendor vendor
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User is not authorized to create vendors");
        }
        vendor = vendorService.createVendor(vendor);
        HalResource<Vendor> halResource = createHalVendor(vendor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(vendor.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(
            summary = "Update a vendor.",
            description = "Update a vendor.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vendor updated successfully.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Vendor updated successfully.\"}"
                                    ))
                    }),
            @ApiResponse(
                    responseCode = "400", description = "Vendor body is empty.",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class),
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Value cannot be null\"}"
                                    ))
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN authority required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(
                    responseCode = "409", description = "A Vendor with same fullname already exists!",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class),
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"A Vendor with same fullname 'ABC_XYZ' already exists!\"}"
                                    ))
                    }
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping(value = VENDORS_URL + "/{id}")
    public ResponseEntity<?> updateVendor(
            @Parameter(description = "The id of the vendor")
            @PathVariable("id") String id,
            @Parameter(description = "The vendor to be updated.")
            @RequestBody Vendor vendor
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User is not authorized to update vendors");
        }
        if (vendor.getFullname() == null && vendor.getShortname() == null && vendor.getUrl() == null) {
            throw new BadRequestClientException("Vendor cannot be null");
        }
        RequestStatus status = vendorService.vendorUpdate(vendor, sw360User, id);
        if (RequestStatus.SUCCESS.equals(status)) {
            return new ResponseEntity<>("Vendor updated successfully", HttpStatus.OK);
        } else if (RequestStatus.DUPLICATE.equals(status)) {
            return new ResponseEntity<>("A Vendor with same fullname '" + vendor.getFullname() + "' already exists!", HttpStatus.CONFLICT);
        } else {
            return new ResponseEntity<>("sw360 vendor with id '" + id + " cannot be updated.", HttpStatus.CONFLICT);
        }
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(VendorController.class).slash("api" + VENDORS_URL).withRel("vendors"));
        return resource;
    }

    private HalResource<Vendor> createHalVendor(Vendor sw360Vendor) {
        return new HalResource<>(sw360Vendor);
    }

    @Operation(
            summary = "Export all vendors as Excel file.",
            description = "Export all vendors as Excel file.",
            tags = {"Vendor"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vendor spreadsheet.",
                    content = {
                            @Content(mediaType = CONTENT_TYPE_OPENXML_SPREADSHEET)
                    }),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN authority required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = VENDORS_URL + "/exportVendorDetails")
    public ResponseEntity<?> exportVendor(HttpServletResponse response) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User is not authorized to export vendors");
        }
        restControllerHelper.throwIfSecurityUser(sw360User);
        try {
            ByteBuffer buffer = vendorService.exportExcel();
            String filename = String.format("vendors-%s.xlsx", SW360Utils.getCreatedOn());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            copyDataStreamToResponse(response, buffer);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Merge two vendors.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Merge successful.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Merge successful.\"}"
                                            ))
                            }),
                    @ApiResponse(responseCode = "400", description = "Vendor used as source or target has an open MR..",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class),
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Vendor used as source or target has an open MR.\"}"
                                            ))
                            }),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Access denied.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class),
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Access denied.\"}"
                                            ))
                            }),
                    @ApiResponse(responseCode = "500", description = "Internal server error while merging the vendors.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class),
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Internal server error while merging the vendors.\"}"
                                            ))
                            })
            },
            tags = {"Vendor"}
    )
    @PatchMapping(value = VENDORS_URL + "/mergeVendors")
    public ResponseEntity<RequestStatus> mergeVendors(
            @Parameter(description = "The id of the merge target vendor.")
            @RequestParam(value = "mergeTargetId", required = true) String mergeTargetId,
            @Parameter(description = "The id of the merge source vendor.")
            @RequestParam(value = "mergeSourceId", required = true) String mergeSourceId,
            @Parameter(description = "The merge selection.")
            @RequestBody Vendor mergeSelection
    ) throws TException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User is not authorized to merge vendors");
        }
        // perform the real merge, update merge target and delete merge sources
        RequestStatus requestStatus = vendorService.mergeVendors(mergeTargetId, mergeSourceId, mergeSelection, sw360User);
        return new ResponseEntity<>(requestStatus, HttpStatus.OK);
    }

    private void copyDataStreamToResponse(HttpServletResponse response, ByteBuffer buffer) throws IOException {
        FileCopyUtils.copy(buffer.array(), response.getOutputStream());
    }
}
