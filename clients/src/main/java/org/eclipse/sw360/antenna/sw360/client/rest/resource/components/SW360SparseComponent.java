/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
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

import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;

import java.util.Objects;

public final class SW360SparseComponent extends SW360SimpleHalResource {
    private String name;
    private SW360ComponentType componentType;

    public String getName() {
        return this.name;
    }

    public SW360SparseComponent setName(String name) {
        this.name = name;
        return this;
    }

    public SW360ComponentType getComponentType() {
        return this.componentType;
    }

    public SW360SparseComponent setComponentType(SW360ComponentType componentType) {
        this.componentType = componentType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360SparseComponent) || !super.equals(o)) return false;
        SW360SparseComponent that = (SW360SparseComponent) o;
        return Objects.equals(name, that.name) &&
                componentType == that.componentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, componentType);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360SparseComponent;
    }
}
