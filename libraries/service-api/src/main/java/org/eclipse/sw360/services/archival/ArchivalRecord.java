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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivalRecord {

    @JsonProperty("id")
    private String id;

    @JsonProperty("bundleId")
    private String bundleId;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("entityName")
    private String entityName;

    @JsonProperty("entityType")
    private ArchivalEntityType entityType;

    @JsonProperty("status")
    private ArchivalStatus status;

    @JsonProperty("archivedBy")
    private String archivedBy;

    @JsonProperty("archivedAt")
    private Instant archivedAt;

    @JsonProperty("restoredBy")
    private String restoredBy;

    @JsonProperty("restoredAt")
    private Instant restoredAt;

    @JsonProperty("attachmentCount")
    private Integer attachmentCount;

    @JsonProperty("comment")
    private String comment;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public ArchivalEntityType getEntityType() { return entityType; }
    public void setEntityType(ArchivalEntityType entityType) { this.entityType = entityType; }

    public ArchivalStatus getStatus() { return status; }
    public void setStatus(ArchivalStatus status) { this.status = status; }

    public String getArchivedBy() { return archivedBy; }
    public void setArchivedBy(String archivedBy) { this.archivedBy = archivedBy; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public String getRestoredBy() { return restoredBy; }
    public void setRestoredBy(String restoredBy) { this.restoredBy = restoredBy; }

    public Instant getRestoredAt() { return restoredAt; }
    public void setRestoredAt(Instant restoredAt) { this.restoredAt = restoredAt; }

    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
