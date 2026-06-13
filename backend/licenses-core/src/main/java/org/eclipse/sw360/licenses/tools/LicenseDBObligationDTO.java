/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseDBObligationDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("topic")
    private String topic;

    // possible values: OBLIGATION, RISK, RESTRICTION, RIGHT
    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private String text;

    // possible values: GREEN, YELLOW, RED
    @JsonProperty("classification")
    private String classification;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("category")
    private String category;

    @JsonProperty("license_ids")
    private List<String> licenseIds;

    public List<String> getLicenseIds() {
        return licenseIds != null ? licenseIds : Collections.emptyList();
    }
}
