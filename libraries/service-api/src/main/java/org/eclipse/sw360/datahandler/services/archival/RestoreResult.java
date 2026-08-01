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

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestoreResult {

    @JsonProperty("bundleId")
    private String bundleId;

    @JsonProperty("entries")
    private List<Entry> entries;

    @JsonProperty("restoredCount")
    private int restoredCount;

    @JsonProperty("skippedCount")
    private int skippedCount;

    @JsonProperty("failedCount")
    private int failedCount;

    public enum Outcome { RESTORED, SKIPPED, FAILED }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Entry {
        @JsonProperty("entityId")
        private String entityId;

        @JsonProperty("entityType")
        private ArchivalEntityType entityType;

        @JsonProperty("outcome")
        private Outcome outcome;

        @JsonProperty("reason")
        private String reason;
    }
}
