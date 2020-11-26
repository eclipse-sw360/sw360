/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public abstract class SW360HalResource<L extends LinkObjects, E extends Embedded> {
    private L links = createEmptyLinks();
    private E embedded = createEmptyEmbedded();

    public abstract L createEmptyLinks();
    public abstract E createEmptyEmbedded();

    /**
     * Returns the link representing this resource. Result may be
     * <strong>null</strong> if this object is not initialized.
     *
     * @return the link to this resource
     */
    @JsonIgnore
    public Self getSelfLink() {
        return links.getSelf();
    }

    /**
     * Returns the ID of this element. The ID is the last part of the link to
     * this resource. It can be used to generate a link to this resource from
     * the resource's endpoint. Result may be <strong>null</strong> if this
     * object is not initialized.
     *
     * @return the ID of this element
     */
    @JsonIgnore
    public String getId() {
        return SW360HalResourceUtility.getLastIndexOfSelfLink(getLinks())
                .orElse(null);
    }

    @JsonGetter("_links")
    public L getLinks() {
        return links;
    }

    @JsonSetter("_links")
    public SW360HalResource<L, E> setLinks(L links) {
        if (links != null) {
            this.links = links;
        }
        return this;
    }

    @JsonGetter("_embedded")
    public E getEmbedded() {
        return embedded;
    }

    @JsonSetter("_embedded")
    public SW360HalResource<L, E> setEmbedded(E embedded) {
        if (embedded != null) {
            this.embedded = embedded;
        }
        return this;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360HalResource)) return false;
        SW360HalResource<?, ?> that = (SW360HalResource<?, ?>) o;
        return that.canEqual(this) &&
                Objects.equals(links, that.links) &&
                Objects.equals(embedded, that.embedded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links, embedded);
    }

    /**
     * Checks whether an equals comparison to the given object is possible.
     * This is needed to support sub classes with additional state while
     * keeping the contract of equals(). Refer to
     * <a href="https://www.artima.com/lejava/articles/equality.html">this
     * article</a> for further details.
     *
     * @param o the object to compare to
     * @return a flag whether this object can be equal to this
     */
    public boolean canEqual(Object o) {
        return o instanceof SW360HalResource;
    }

    /**
     * <p>
     * A representation of an undefined {@code Embedded}.
     * </p>
     * <p>
     * The constant defined in this enum can be used to represent an
     * {@code Embedded} object when no real value is available. Note:
     * Defining this as an enum makes sure that only a single instance ever
     * exists.
     * </p>
     */
    public enum EmptyEmbedded implements Embedded {
        INSTANCE
    }
}
