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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
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
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.net.URISyntaxException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

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
    @RequestMapping(value = VENDORS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Vendor>>> getVendors(
            Pageable pageable,
            HttpServletRequest request
            ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        List<Vendor> vendors = vendorService.getVendors();

        PaginationResult<Vendor> paginationResult = restControllerHelper.createPaginationResult(request, pageable, vendors, SW360Constants.TYPE_VENDOR);
        List<EntityModel<Vendor>> vendorResources = new ArrayList<>();
        for (Vendor v: paginationResult.getResources()) {
            Vendor embeddedVendor = restControllerHelper.convertToEmbeddedVendor(v);
            vendorResources.add(EntityModel.of(embeddedVendor));
        }

        CollectionModel<EntityModel<Vendor>> resources;
        if (vendors.isEmpty()) {
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
    @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<Vendor>> getVendor(
            @Parameter(description = "The id of the vendor to get.")
            @PathVariable("id") String id
    ) {
        Vendor sw360Vendor = vendorService.getVendorById(id);
        HalResource<Vendor> halResource = createHalVendor(sw360Vendor);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get the releases used by the vendor.",
            description = "Get the releases by vendor id.",
            tags = {"Vendor"}
    )
    @RequestMapping(value = VENDORS_URL + "/{id}/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getReleases(
            @Parameter(description = "The id of the vendor to get.")
            @PathVariable("id") String id
    ) throws TException {
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
            throw new TException(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a vendor.",
            description = "Delete vendor by id.",
            tags = {"Vendor"}
    )
    @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteVendor(
            @Parameter(description = "The id of the vendor to be deleted.")
            @PathVariable("id") String id
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Vendor sw360Vendor = vendorService.getVendorById(id);
        if (sw360Vendor == null) {
            return new ResponseEntity<>("Vendor with id " + id + " not found.", HttpStatus.NOT_FOUND);
        }
        RequestStatus requestStatus = vendorService.deleteVendorByid(id, sw360User);
        if (requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>("Vendor with full name " + sw360Vendor.getFullname() + " deleted successfully.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Vendor with full name " + sw360Vendor.getFullname() + " cannot be deleted.", HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Create a new vendor.",
            description = "Create a new vendor.",
            tags = {"Vendor"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = VENDORS_URL, method = RequestMethod.POST)
    public ResponseEntity<?> createVendor(
            @Parameter(description = "The vendor to be created.")
            @RequestBody Vendor vendor
    ) {
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
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Value cannot be null\"}"
                                    ))
                    }),
            @ApiResponse(
                    responseCode = "409", description = "A Vendor with same fullname already exists!",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"A Vendor with same fullname 'ABC_XYZ' already exists!\"}"
                                    ))
                    }
            )
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<?> updateVendor(
            @Parameter(description = "The id of the vendor")
            @PathVariable("id") String id,
            @Parameter(description = "The vendor to be updated.")
            @RequestBody Vendor vendor
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (vendor.getFullname() == null && vendor.getShortname() == null && vendor.getUrl() == null) {
            return new ResponseEntity<>("Value cannot be null", HttpStatus.BAD_REQUEST);
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
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    })
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = VENDORS_URL + "/exportVendorDetails")
    public ResponseEntity<?> exportVendor(HttpServletResponse response) throws TException {
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

    private void copyDataStreamToResponse(HttpServletResponse response, ByteBuffer buffer) throws IOException {
        FileCopyUtils.copy(buffer.array(), response.getOutputStream());
    }
}
