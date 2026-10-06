/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseInfoControllerTest {

    private final LicenseInfoController controller = new LicenseInfoController();

    @Test
    public void process_addsLicenseInfoLinkToResource() {
        RepositoryLinksResource resource = new RepositoryLinksResource();

        RepositoryLinksResource processedResource = controller.process(resource);

        assertThat(processedResource).isNotNull();
        assertThat(processedResource.getLink("licenseinfo")).isPresent();
        assertThat(processedResource.getLink("licenseinfo").get().getHref()).endsWith("/api/licenseinfo");
    }
}
