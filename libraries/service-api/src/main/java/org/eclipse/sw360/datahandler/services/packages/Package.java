/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.packages;

import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Package {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String name;

    @JsonProperty(required = true)
    private String version;

    @JsonProperty(required = true)
    private String purl;

    @JsonProperty(required = true)
    private CycloneDxComponentType packageType;

    private String releaseId;

    private Set<String> licenseIds;

    private String description;

    private String homepageUrl;

    private String hash;

    private String vcs;

    private String createdOn;

    private String createdBy;

    private String modifiedOn;

    private String modifiedBy;

    private PackageManager packageManager;

    private Vendor vendor;

    private String vendorId;

    private Release release;
}
