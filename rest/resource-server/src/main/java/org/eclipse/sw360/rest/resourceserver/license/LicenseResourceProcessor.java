/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
