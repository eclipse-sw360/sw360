/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource.projects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.users.SW360SparseUser;

import java.util.Objects;

@JsonDeserialize(as = SW360ProjectEmbedded.class)
public final class SW360ProjectEmbedded implements Embedded {
    private SW360SparseUser createdBy;

    public SW360SparseUser getCreatedBy() {
        return createdBy;
    }

    public SW360ProjectEmbedded setCreatedBy(SW360SparseUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ProjectEmbedded that = (SW360ProjectEmbedded) o;
        return Objects.equals(createdBy, that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdBy);
    }
}