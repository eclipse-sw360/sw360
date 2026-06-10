/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.vmcomponents;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VMProcessReporting {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String elementType;

    @JsonProperty(required = true)
    private String startDate;

    private String endDate;

    private Integer processingSeconds;

    private Integer idsReceived;

    private Integer newReceived;

    private Integer knownReceived;

    private Integer completed;
}
