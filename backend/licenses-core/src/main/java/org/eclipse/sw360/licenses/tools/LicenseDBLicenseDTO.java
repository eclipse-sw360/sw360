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

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseDBLicenseDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("shortname")
    private String shortname;

    @JsonProperty("fullname")
    private String fullname;

    @JsonProperty("text")
    private String text;

    @JsonProperty("url")
    private String url;

    @JsonProperty("copyleft")
    private boolean copyleft;

    // LicenseDB API uses mixed case "OSIapproved" not camelCase
    @JsonProperty("OSIapproved")
    private boolean osiApproved;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("spdx_id")
    private String spdxId;

    @JsonProperty("obligation_ids")
    private List<String> obligationIds;

    public String getId() {
        return id;
    }

    public String getShortname() {
        return shortname;
    }

    public String getFullname() {
        return fullname;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public boolean isCopyleft() {
        return copyleft;
    }

    public boolean isOsiApproved() {
        return osiApproved;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isActive() {
        return active;
    }

    public String getSpdxId() {
        return spdxId;
    }

    public List<String> getObligationIds() {
        return obligationIds != null ? obligationIds : Collections.emptyList();
    }
}
