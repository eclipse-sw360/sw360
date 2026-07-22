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
public class RestorePreview {

    @JsonProperty("bundleId")
    private String bundleId;

    @JsonProperty("entries")
    private List<Entry> entries;

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Entry {
        @JsonProperty("entityId")
        private String entityId;

        @JsonProperty("entityName")
        private String entityName;

        @JsonProperty("entityType")
        private ArchivalEntityType entityType;

        @JsonProperty("attachmentCount")
        private Integer attachmentCount;

        @JsonProperty("conflict")
        private boolean conflict;

        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }

        public String getEntityName() { return entityName; }
        public void setEntityName(String entityName) { this.entityName = entityName; }

        public ArchivalEntityType getEntityType() { return entityType; }
        public void setEntityType(ArchivalEntityType entityType) { this.entityType = entityType; }

        public Integer getAttachmentCount() { return attachmentCount; }
        public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }

        public boolean isConflict() { return conflict; }
        public void setConflict(boolean conflict) { this.conflict = conflict; }
    }
}
