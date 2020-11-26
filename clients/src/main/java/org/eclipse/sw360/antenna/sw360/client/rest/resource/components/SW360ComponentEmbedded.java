/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.users.SW360User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonDeserialize(as = SW360ComponentEmbedded.class)
public final class SW360ComponentEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:releases")
    private List<SW360SparseRelease> releases;
    private SW360User createdBy;

    public List<SW360SparseRelease> getReleases() {
        return Optional.ofNullable(this.releases)
                .orElse(Collections.emptyList());
    }

    public SW360ComponentEmbedded setReleases(List<SW360SparseRelease> releases) {
        this.releases = releases;
        return this;
    }

    public SW360User getCreatedBy() {
        return createdBy;
    }

    public SW360ComponentEmbedded setCreatedBy(SW360User createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ComponentEmbedded that = (SW360ComponentEmbedded) o;
        return Objects.equals(releases, that.releases) &&
                Objects.equals(createdBy, that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(releases, createdBy);
    }
}

