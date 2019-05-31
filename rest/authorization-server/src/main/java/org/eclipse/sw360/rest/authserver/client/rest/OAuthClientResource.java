/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;

import java.util.Set;

public class OAuthClientResource {

    @JsonProperty("description")
    private String description;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("authorities")
    private Set<String> authorities;

    @JsonProperty("scope")
    private Set<String> scope;

    @JsonProperty("access_token_validity")
    private Integer accessTokenValidity;

    @JsonProperty("refresh_token_validity")
    private Integer refreshTokenValidity;

    public OAuthClientResource() {
        // if needed by frameworks
    }

    public OAuthClientResource(OAuthClientEntity clientEntity) {
        this.description = clientEntity.getDescription();
        this.clientId = clientEntity.getClientId();
        this.clientSecret = clientEntity.getClientSecret();
        this.authorities = clientEntity.getAuthoritiesAsStrings();
        this.scope = clientEntity.getScope();
        this.accessTokenValidity = clientEntity.getAccessTokenValiditySeconds();
        this.refreshTokenValidity = clientEntity.getRefreshTokenValiditySeconds();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public Set<String> getScope() {
        return scope;
    }

    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public Integer getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

}
