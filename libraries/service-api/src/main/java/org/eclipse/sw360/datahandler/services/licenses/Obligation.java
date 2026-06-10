/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.licenses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Obligation {
    private String id;
    private String revision;
    private String type;
    @JsonProperty(required = true)
    private String text;
    private Set<String> whitelist;
    private Boolean development;
    private Boolean distribution;
    private String title;
    private Map<String, String> customPropertyToValue;
    private String developmentString;
    private String distributionString;
    private Map<String, String> externalIds;
    private String comments;
    private ObligationLevel obligationLevel;
    private ObligationType obligationType;
    private Map<String, String> additionalData;
    private String node;
}
