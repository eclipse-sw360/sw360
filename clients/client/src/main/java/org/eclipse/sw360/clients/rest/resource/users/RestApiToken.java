/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest.resource.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestApiToken {
    private String name;
    private String token;
    private String createdOn;
    private Integer numberOfDaysValid;
    private Set<String> authorities = new HashSet<>();

    public String getName() {
        return name;
    }

    public RestApiToken setName(String name) {
        this.name = name;
        return this;
    }

    public String getToken() {
        return token;
    }

    public RestApiToken setToken(String token) {
        this.token = token;
        return this;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public RestApiToken setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public Integer getNumberOfDaysValid() {
        return numberOfDaysValid;
    }

    public RestApiToken setNumberOfDaysValid(Integer numberOfDaysValid) {
        this.numberOfDaysValid = numberOfDaysValid;
        return this;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public RestApiToken setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestApiToken that = (RestApiToken) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(token, that.token) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(numberOfDaysValid, that.numberOfDaysValid) &&
                Objects.equals(authorities, that.authorities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, token, createdOn, numberOfDaysValid, authorities);
    }
}
