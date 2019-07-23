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
package org.eclipse.sw360.rest.authserver.client.persistence;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.ektorp.support.CouchDbDocument;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.Jackson2ArrayOrStringDeserializer;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OAuthClientEntity extends CouchDbDocument implements ClientDetails {

    private BaseClientDetails delegate;

    private String description;

    public OAuthClientEntity() {
        this.delegate = new BaseClientDetails();
    }

    @Override
    @JsonProperty("client_id")
    public String getClientId() {
        return delegate.getClientId();
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        delegate.setClientId(clientId);
    }

    @Override
    @JsonProperty("client_secret")
    public String getClientSecret() {
        return delegate.getClientSecret();
    }

    @JsonProperty("client_secret")
    public void setClientSecret(String clientSecret) {
        delegate.setClientSecret(clientSecret);
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @JsonProperty("resource_ids")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    public Set<String> getResourceIds() {
        return delegate.getResourceIds();
    }

    @JsonProperty("resource_ids")
    public void setResourceIds(Set<String> clientSecret) {
        delegate.setResourceIds(clientSecret);
    }

    @Override
    @JsonProperty("authorized_grant_types")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    public Set<String> getAuthorizedGrantTypes() {
        return delegate.getAuthorizedGrantTypes();
    }

    @JsonProperty("authorized_grant_types")
    public void setAuthorizedGrantTypes(Set<String> authorizedGrantTypes) {
        delegate.setAuthorizedGrantTypes(authorizedGrantTypes);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
        delegate.setAuthorities(authorities);
    }

    @JsonProperty("authorities")
    public Set<String> getAuthoritiesAsStrings() {
        return AuthorityUtils.authorityListToSet(delegate.getAuthorities());
    }

    @JsonProperty("authorities")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    public void setAuthoritiesAsStrings(Set<String> values) {
        delegate.setAuthorities(AuthorityUtils.createAuthorityList(values.toArray(new String[values.size()])));
    }

    @Override
    @JsonProperty("scope")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    public Set<String> getScope() {
        return delegate.getScope();
    }

    @JsonProperty("scope")
    public void setScope(Set<String> scope) {
        delegate.setScope(scope);
    }

    @Override
    @JsonProperty("redirect_uri")
    @JsonDeserialize(using = Jackson2ArrayOrStringDeserializer.class)
    public Set<String> getRegisteredRedirectUri() {
        return delegate.getRegisteredRedirectUri();
    }

    @JsonProperty("redirect_uri")
    public void setRegisteredRedirectUri(Set<String> registeredRedirectUri) {
        delegate.setRegisteredRedirectUri(registeredRedirectUri);
    }

    @Override
    @JsonProperty("access_token_validity")
    public Integer getAccessTokenValiditySeconds() {
        return delegate.getAccessTokenValiditySeconds();
    }

    @JsonProperty("access_token_validity")
    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
        delegate.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
    }

    @Override
    @JsonProperty("refresh_token_validity")
    public Integer getRefreshTokenValiditySeconds() {
        return delegate.getRefreshTokenValiditySeconds();
    }

    @JsonProperty("refresh_token_validity")
    public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
        delegate.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
    }

    @JsonProperty("autoapprove")
    public Set<String> getAutoApproveScopes() {
        return delegate.getAutoApproveScopes();
    }

    @JsonProperty("autoapprove")
    public void setAutoApproveScopes(Set<String> autoApproveScopes) {
        delegate.setAutoApproveScopes(autoApproveScopes);
    }

    @Override
    @JsonAnyGetter
    public Map<String, Object> getAdditionalInformation() {
        return delegate.getAdditionalInformation();
    }

    @JsonAnySetter
    public void addAdditionalInformation(String key, Object value) {
        delegate.addAdditionalInformation(key, value);
    }

    @Override
    public boolean isSecretRequired() {
        return delegate.isSecretRequired();
    }

    @Override
    public boolean isScoped() {
        return delegate.isScoped();
    }

    @Override
    public boolean isAutoApprove(String scope) {
        return delegate.isAutoApprove(scope);
    }

}
