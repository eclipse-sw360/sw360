/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.services.archival;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

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

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getSw360Version() { return sw360Version; }
    public void setSw360Version(String sw360Version) { this.sw360Version = sw360Version; }

    public int getManifestVersion() { return manifestVersion; }
    public void setManifestVersion(int manifestVersion) { this.manifestVersion = manifestVersion; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<ManifestEntry> getEntries() { return entries; }
    public void setEntries(List<ManifestEntry> entries) { this.entries = entries; }
}
