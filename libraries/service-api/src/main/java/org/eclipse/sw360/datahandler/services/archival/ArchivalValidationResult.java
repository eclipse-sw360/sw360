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

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivalValidationResult {

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("entityType")
    private ArchivalEntityType entityType;

    @JsonProperty("canArchive")
    private boolean canArchive;

    @JsonProperty("reason")
    private Reason reason;

    @JsonProperty("details")
    private String details;

    public enum Reason {
        OK,
        ENTITY_NOT_FOUND,
        REFERENCED_BY_LIVE_PROJECT,
        REFERENCED_BY_LIVE_RELEASE,
        OPEN_CLEARING_REQUEST,
        PERMISSION_DENIED,
        UNKNOWN
    }
}
