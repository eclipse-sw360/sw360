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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class VendorController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String VENDORS_URL = "/vendors";

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = VENDORS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<Vendor>>> getVendors() {
        List<Vendor> vendors = vendorService.getVendors();

        List<Resource<Vendor>> vendorResources = new ArrayList<>();
        vendors.forEach(v -> {
            Vendor embeddedVendor = restControllerHelper.convertToEmbeddedVendor(v);
            vendorResources.add(new Resource<>(embeddedVendor));
        });

        Resources<Resource<Vendor>> resources = new Resources<>(vendorResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<Vendor>> getVendor(
            @PathVariable("id") String id) {
        Vendor sw360Vendor = vendorService.getVendorById(id);
        HalResource<Vendor> halResource = createHalVendor(sw360Vendor);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = VENDORS_URL, method = RequestMethod.POST)
    public ResponseEntity createVendor(
            @RequestBody Vendor vendor) {
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
