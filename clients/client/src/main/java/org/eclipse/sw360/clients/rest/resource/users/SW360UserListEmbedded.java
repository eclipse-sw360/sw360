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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.clients.rest.resource.Embedded;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(as = SW360UserListEmbedded.class)
public final class SW360UserListEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:users")
    private List<SW360User> users = new ArrayList<>();

    public List<SW360User> getUsers() {
        return this.users;
    }

    public SW360UserListEmbedded setUsers(List<SW360User> users) {
        this.users = users;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360UserListEmbedded that = (SW360UserListEmbedded) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}
