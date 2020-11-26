/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.components;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;

import java.util.Objects;
import java.util.Set;

public final class SW360Component extends SW360HalResource<LinkObjects, SW360ComponentEmbedded> {
    private String name;
    private SW360ComponentType componentType;
    private String createdOn;
    private String homepage;

    private Set<String> categories;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return this.name;
    }

    public SW360Component setName(String name) {
        this.name = name;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCreatedOn() {
        return this.createdOn;
    }

    public SW360Component setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360ComponentType getComponentType() {
        return this.componentType;
    }

    public SW360Component setComponentType(SW360ComponentType componentType) {
        this.componentType = componentType;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getHomepage() {
        return this.homepage;
    }

    public SW360Component setHomepage(String homepage) {
        this.homepage = homepage;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> getCategories() {
        return categories;
    }

    public SW360Component setCategories(Set<String> categories) {
        this.categories = categories;
        return this;
    }

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360ComponentEmbedded createEmptyEmbedded() {
        return new SW360ComponentEmbedded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360Component) || !super.equals(o)) return false;
        SW360Component that = (SW360Component) o;
        return Objects.equals(name, that.name) &&
                componentType == that.componentType &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(homepage, that.homepage) &&
                Objects.equals(categories, that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, componentType, createdOn, homepage, categories);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360Component;
    }
}
