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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.releases;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(as = SW360ReleaseListEmbedded.class)
public final class SW360ReleaseListEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:releases")
    private List<SW360SparseRelease> releases;

    public List<SW360SparseRelease> getReleases() { return releases; }

    public SW360ReleaseListEmbedded setReleases(List<SW360SparseRelease> releases) {
        this.releases = releases;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ReleaseListEmbedded that = (SW360ReleaseListEmbedded) o;
        return Objects.equals(releases, that.releases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(releases);
    }
}
