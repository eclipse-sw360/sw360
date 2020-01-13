/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import lombok.RequiredArgsConstructor;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class LicenseInfoResourceProcessor implements ResourceProcessor<Resource<LicenseInfo>> {

    @Override
    public Resource<LicenseInfo> process(Resource<LicenseInfo> resource) {
        LicenseInfo licenseInfo = resource.getContent();
        Link selfLink = linkTo(LicenseInfoController.class)
                .slash("api" + LicenseInfoController.LICENSE_INFO_URL /*+ "/" + licenseInfo.getId()*/).withSelfRel();
        resource.add(selfLink);
        return resource;
    }

}
