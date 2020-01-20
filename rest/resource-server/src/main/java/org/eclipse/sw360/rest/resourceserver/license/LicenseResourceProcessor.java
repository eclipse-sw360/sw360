/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.license;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
class LicenseResourceProcessor implements ResourceProcessor<Resource<License>> {

    @Override
    public Resource<License> process(Resource<License> resource) {
        License license = resource.getContent();
        Link selfLink = linkTo(LicenseController.class)
                .slash("api" + LicenseController.LICENSES_URL + "/" + license.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
