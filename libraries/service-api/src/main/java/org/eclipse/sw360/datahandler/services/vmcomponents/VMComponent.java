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

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VMComponent {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String receivedDate;

    private String lastUpdateDate;

    @JsonProperty(required = true)
    private String vmid;

    private String vendor;

    private String name;

    private String version;

    private String url;

    private String securityUrl;

    private Boolean eolReached;

    private String cpe;

    private Set<VMMinPatchLevel> minPatchLevels;
}
