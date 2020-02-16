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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.license.LicenseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.ResourceProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LicenseInfoController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String LICENSE_INFO_URL = "/licenseinfo";

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(LicenseController.class).slash("api/licenseinfo").withRel("licenseinfo"));
        return resource;
    }

    private HalResource<LicenseInfo> createHalLicense(LicenseInfo sw360LicenseInfo) {
        return new HalResource<>(sw360LicenseInfo);
    }
}
