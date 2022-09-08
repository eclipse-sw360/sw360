/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.clients.rest.resource.projects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.clients.rest.resource.Embedded;
import org.eclipse.sw360.clients.rest.resource.users.SW360SparseUser;

import java.util.Objects;

@JsonDeserialize(as = SW360ProjectDTOEmbedded.class)
public final class SW360ProjectDTOEmbedded implements Embedded {
    private SW360SparseUser createdBy;

    public SW360SparseUser getCreatedBy() {
        return createdBy;
    }

    public SW360ProjectDTOEmbedded setCreatedBy(SW360SparseUser createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ProjectDTOEmbedded that = (SW360ProjectDTOEmbedded) o;
        return Objects.equals(createdBy, that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdBy);
    }
}
