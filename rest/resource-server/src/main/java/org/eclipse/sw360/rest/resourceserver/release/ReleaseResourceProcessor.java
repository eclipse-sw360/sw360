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

package org.eclipse.sw360.rest.resourceserver.release;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Component
@RequiredArgsConstructor
class ReleaseResourceProcessor implements ResourceProcessor<Resource<Release>> {

    @Override
    public Resource<Release> process(Resource<Release> resource) {
        Release release = resource.getContent();
        Link selfLink = linkTo(ReleaseController.class)
                .slash("api" + ReleaseController.RELEASES_URL + "/" + release.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
