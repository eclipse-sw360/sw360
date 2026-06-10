/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {
    @JsonProperty(required = true)
    private String attachmentContentId;
    @JsonProperty(required = true)
    private String filename;
    private String sha1;
    private AttachmentType attachmentType;
    private String createdBy;
    private String createdTeam;
    private String createdComment;
    private String createdOn;
    private String checkedBy;
    private String checkedTeam;
    private String checkedComment;
    private String checkedOn;
    private Set<String> uploadHistory;
    private CheckStatus checkStatus;
    private String superAttachmentId;
    private String superAttachmentFilename;
    private ProjectAttachmentUsage projectAttachmentUsage;
}
