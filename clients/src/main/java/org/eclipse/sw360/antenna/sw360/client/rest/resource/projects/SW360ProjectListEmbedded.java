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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(as = SW360ProjectListEmbedded.class)
public final class SW360ProjectListEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:projects")
    private List<SW360Project> projects = new ArrayList<>();

    public List<SW360Project> getProjects() {
        return this.projects;
    }

    public SW360ProjectListEmbedded setProjects(List<SW360Project> projects) {
        this.projects = projects;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ProjectListEmbedded that = (SW360ProjectListEmbedded) o;
        return Objects.equals(projects, that.projects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projects);
    }
}