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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManifestEntry {

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("entityName")
    private String entityName;

    @JsonProperty("entityType")
    private ArchivalEntityType entityType;

    @JsonProperty("entityVersion")
    private String entityVersion;

    @JsonProperty("path")
    private String path;

    @JsonProperty("attachmentCount")
    private int attachmentCount;

    @JsonProperty("attachmentTotalBytes")
    private long attachmentTotalBytes;

    @JsonProperty("checksum")
    private String checksum;

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public ArchivalEntityType getEntityType() { return entityType; }
    public void setEntityType(ArchivalEntityType entityType) { this.entityType = entityType; }

    public String getEntityVersion() { return entityVersion; }
    public void setEntityVersion(String entityVersion) { this.entityVersion = entityVersion; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(int attachmentCount) { this.attachmentCount = attachmentCount; }

    public long getAttachmentTotalBytes() { return attachmentTotalBytes; }
    public void setAttachmentTotalBytes(long attachmentTotalBytes) { this.attachmentTotalBytes = attachmentTotalBytes; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
}
