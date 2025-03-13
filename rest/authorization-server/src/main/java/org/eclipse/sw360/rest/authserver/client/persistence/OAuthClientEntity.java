/*
 * Copyright Siemens AG, 2019-2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = OAuthClientDeserializer.class)
public class OAuthClientEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String id;
    private String revision;
    private String clientId;
    private String clientSecret;
    private String description;
    private Set<String> resourceIds;
    private Set<String> authorizedGrantTypes;
    private Set<String> authorities;
    private Set<String> scope;
    private boolean secretRequired;
    private boolean scoped;
    private Set<String> registeredRedirectUri;
    private Integer accessTokenValiditySeconds;
    private Integer refreshTokenValiditySeconds;
    private Set<String> autoApproveScopes;

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_rev")
    public void setRev(String revision) {
        this.revision = revision;
    }

    @JsonProperty("_rev")
    public String getRev() {
        return revision;
    }

    @JsonProperty("secretRequired")
    public void setSecretRequired(boolean secretRequired) {
        this.secretRequired = secretRequired;
    }

    @JsonProperty("scoped")
    public void setScoped(boolean scoped) {
        this.scoped = scoped;
    }

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId=clientId;
    }

    @JsonProperty("client_secret")
    public String getClientSecret() {
        return clientSecret;
    }

    @JsonProperty("client_secret")
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("resource_ids")
    @JsonDeserialize()
    public Set<String> getResourceIds() {
        return resourceIds;
    }

    @JsonProperty("resource_ids")
    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    @JsonProperty("authorized_grant_types")
    @JsonDeserialize()
    public Set<String> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    @JsonProperty("authorized_grant_types")
    public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
        this.authorizedGrantTypes=authorizedGrantTypes;
    }

    @JsonProperty("authorities")
    public Set<String> getAuthorities() {
        return authorities;
    }

    @JsonProperty("authorities")
    public void setAuthorities(Set<String> values) {
        this.authorities = values;
    }

    @JsonProperty("scope")
    @JsonDeserialize()
    public Set<String> getScope() {
        return this.scope;
    }

    @JsonProperty("scope")
    public void setScope(Set<String> scope) {
        this.scope=scope;
    }

    @JsonProperty("redirect_uri")
    @JsonDeserialize()
    public Set<String> getRegisteredRedirectUri() {
        return this.registeredRedirectUri;
    }

    @JsonProperty("redirect_uri")
    public void setRegisteredRedirectUri(Set<String> registeredRedirectUri) {
        this.registeredRedirectUri = registeredRedirectUri;
    }

    @JsonProperty("access_token_validity")
    public Integer getAccessTokenValiditySeconds() {
        return this.accessTokenValiditySeconds;
    }

    @JsonProperty("access_token_validity")
    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    @JsonProperty("refresh_token_validity")
    public Integer getRefreshTokenValiditySeconds() {
        return this.refreshTokenValiditySeconds;
    }

    @JsonProperty("refresh_token_validity")
    public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    @JsonProperty("autoapprove")
    public Set<String> getAutoApproveScopes() {
        return this.autoApproveScopes;
    }

    @JsonProperty("autoapprove")
    public void setAutoApproveScopes(Set<String> autoApproveScopes) {
        this.autoApproveScopes = autoApproveScopes;
    }


    public boolean isSecretRequired() {
        return this.secretRequired;
    }

    public boolean isScoped() {
        return this.scoped;
    }

    @Override
    public String toString() {
        return "OAuthClientEntity{" +
                "clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", description='" + description + '\'' +
                ", resourceIds=" + resourceIds +
                ", authorizedGrantTypes=" + authorizedGrantTypes +
                ", authorities=" + authorities +
                ", scope=" + scope +
                ", secretRequired=" + secretRequired +
                ", scoped=" + scoped +
                ", registeredRedirectUri=" + registeredRedirectUri +
                ", accessTokenValiditySeconds=" + accessTokenValiditySeconds +
                ", refreshTokenValiditySeconds=" + refreshTokenValiditySeconds +
                ", autoApproveScopes=" + autoApproveScopes +
                '}';
    }
}