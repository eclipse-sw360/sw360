/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for FOSSology v1 API (kept for compatibility)
 * Note: v2 API uses different response models in FossologyV2Models
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FossologyResponse {

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    private String type;

    // Default constructor
    public FossologyResponse() {
        this.code = 0;
        this.message = "";
        this.type = "";
    }

    // Constructor with parameters
    public FossologyResponse(int code, String message, String type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "FossologyResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
