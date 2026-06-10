/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.projects;

import java.util.List;

import org.eclipse.sw360.datahandler.services.common.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestSize;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestState;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestType;
import org.eclipse.sw360.datahandler.services.common.Comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClearingRequest {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String requestedClearingDate;

    private String projectId;

    @JsonProperty(required = true)
    private ClearingRequestState clearingState;

    @JsonProperty(required = true)
    private String requestingUser;

    private String projectBU;

    private String requestingUserComment;

    @JsonProperty(required = true)
    private String clearingTeam;

    private String agreedClearingDate;

    @JsonProperty(required = true)
    private Long timestamp;

    private Long timestampOfDecision;

    private List<Comment> comments;

    private Long modifiedOn;

    private List<Long> reOpenOn;

    private ClearingRequestPriority priority;

    private ClearingRequestType clearingType;

    private ClearingRequestSize clearingSize;
}
