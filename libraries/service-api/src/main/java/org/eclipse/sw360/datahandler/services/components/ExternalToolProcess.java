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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalToolProcess {
    private String id;
    @JsonProperty(required = true)
    private ExternalTool externalTool;
    @JsonProperty(required = true)
    private ExternalToolProcessStatus processStatus;
    private String processIdInTool;
    private String attachmentId;
    private String attachmentHash;
    @JsonProperty(required = true)
    private List<ExternalToolProcessStep> processSteps;
}
