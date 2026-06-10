/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.licenseinfo;

import java.util.List;

import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObligationAtProject {

    @JsonProperty(required = true)
    private String topic;

    @JsonProperty(required = true)
    private String text;

    @JsonProperty(required = true)
    private List<String> licenseIDs;

    private ObligationStatusInfo obligationStatusInfo;

    private String id;

    private String type;
}
