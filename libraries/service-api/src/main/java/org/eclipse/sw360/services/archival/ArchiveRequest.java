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

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveRequest {

    @JsonProperty("entityType")
    private ArchivalEntityType entityType;

    @JsonProperty("entityIds")
    private List<String> entityIds;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("includeAttachments")
    private boolean includeAttachments = true;

    @JsonProperty("includeChangelogs")
    private boolean includeChangelogs = true;

    public ArchivalEntityType getEntityType() { return entityType; }
    public void setEntityType(ArchivalEntityType entityType) { this.entityType = entityType; }

    public List<String> getEntityIds() { return entityIds; }
    public void setEntityIds(List<String> entityIds) { this.entityIds = entityIds; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isIncludeAttachments() { return includeAttachments; }
    public void setIncludeAttachments(boolean includeAttachments) { this.includeAttachments = includeAttachments; }

    public boolean isIncludeChangelogs() { return includeChangelogs; }
    public void setIncludeChangelogs(boolean includeChangelogs) { this.includeChangelogs = includeChangelogs; }
}
