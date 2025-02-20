/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;

import java.util.Set;

@Getter
public class OAuthClientResource {

    @Setter
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
        this.authorities = clientEntity.getAuthorities();
        this.scope = clientEntity.getScope();
        this.accessTokenValidity = clientEntity.getAccessTokenValiditySeconds();
        this.refreshTokenValidity = clientEntity.getRefreshTokenValiditySeconds();
    }
}
