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

@Data
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
}
