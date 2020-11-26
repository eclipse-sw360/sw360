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

package org.eclipse.sw360.antenna.sw360.client.rest.resource.users;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;

import java.util.Objects;

public final class SW360SparseUser extends SW360SimpleHalResource {
    private String email;

    public String getEmail() {
        return this.email;
    }

    public SW360SparseUser setEmail(String email) {
        this.email = email;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360SparseUser) || !super.equals(o)) return false;
        SW360SparseUser that = (SW360SparseUser) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360SparseUser;
    }
}
