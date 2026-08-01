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
public class AttachmentMetadata {

    @JsonProperty("attachmentId")
    private String attachmentId;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("sizeBytes")
    private long sizeBytes;

    @JsonProperty("sha1")
    private String sha1;

    @JsonProperty("attachmentType")
    private String attachmentType;

    @JsonProperty("uploadedBy")
    private String uploadedBy;

    @JsonProperty("uploadedAt")
    private Instant uploadedAt;
}
