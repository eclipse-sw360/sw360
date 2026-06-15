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

import java.util.List;

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

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public int getRestoredCount() { return restoredCount; }
    public void setRestoredCount(int restoredCount) { this.restoredCount = restoredCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public enum Outcome { RESTORED, SKIPPED, FAILED }

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

        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }

        public ArchivalEntityType getEntityType() { return entityType; }
        public void setEntityType(ArchivalEntityType entityType) { this.entityType = entityType; }

        public Outcome getOutcome() { return outcome; }
        public void setOutcome(Outcome outcome) { this.outcome = outcome; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
