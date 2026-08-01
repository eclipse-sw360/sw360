/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.archival;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveManifest {

    @JsonProperty("bundleId")
    private String bundleId;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("sw360Version")
    private String sw360Version;

    @JsonProperty("manifestVersion")
    private int manifestVersion;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("entries")
    private List<ManifestEntry> entries;
}
