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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.clients.rest.resource.Embedded;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@JsonDeserialize(as = SW360ProjectDTOListEmbedded.class)
public class SW360ProjectDTOListEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:projectDTOs")
    private List<SW360ProjectDTO> projects = new ArrayList<>();

    public List<SW360ProjectDTO> getProjects() {
        return this.projects;
    }

    public SW360ProjectDTOListEmbedded setProjects(List<SW360ProjectDTO> projects) {
        this.projects = projects;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ProjectDTOListEmbedded that = (SW360ProjectDTOListEmbedded) o;
        return Objects.equals(projects, that.projects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projects);
    }
}
