/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.ClearingReportStatus;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClearingReport {
    private String id;
    private String revision;
    @JsonProperty(required = true)
    private ClearingReportStatus clearingReportStatus;
    @JsonProperty(required = true)
    private Set<Attachment> attachments;
}
