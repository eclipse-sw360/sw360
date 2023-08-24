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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class VendorController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String VENDORS_URL = "/vendors";

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final RestControllerHelper<?> restControllerHelper;

    @Operation(
            summary = "List all of the service's vendors.",
            description = "List all of the service's vendors.",
            tags = {"Vendor"}
    )
    @RequestMapping(value = VENDORS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Vendor>>> getVendors() {
        List<Vendor> vendors = vendorService.getVendors();

        List<EntityModel<Vendor>> vendorResources = new ArrayList<>();
        vendors.forEach(v -> {
            Vendor embeddedVendor = restControllerHelper.convertToEmbeddedVendor(v);
            vendorResources.add(EntityModel.of(embeddedVendor));
        });

        CollectionModel<EntityModel<Vendor>> resources = CollectionModel.of(vendorResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
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

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(VendorController.class).slash("api" + VENDORS_URL).withRel("vendors"));
        return resource;
    }

    private HalResource<Vendor> createHalVendor(Vendor sw360Vendor) {
        return new HalResource<>(sw360Vendor);
    }
}
