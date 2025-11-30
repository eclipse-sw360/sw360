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

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
@RequiredArgsConstructor
class PackageResourceProcessor implements RepresentationModelProcessor<EntityModel<Package>> {

    @Override
    public EntityModel<Package> process(EntityModel<Package> resource) {
        Package pkg = resource.getContent();
        Link selfLink = linkTo(PackageController.class)
                .slash("api" + PackageController.PACKAGES_URL + "/" + pkg.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }

}
