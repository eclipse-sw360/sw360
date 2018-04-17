/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Vendor.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.vendor;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;


@Component
@RequiredArgsConstructor
class VendorResourceProcessor implements ResourceProcessor<Resource<Vendor>> {

    @Override
    public Resource<Vendor> process(Resource<Vendor> resource) {
        Vendor project = resource.getContent();
        Link selfLink = linkTo(VendorController.class)
                .slash("api" + VendorController.VENDORS_URL + "/" + project.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
