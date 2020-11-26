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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;

import java.util.Objects;

public final class SW360License extends SW360SimpleHalResource {
    private String text;
    private String shortName;
    private String fullName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getText() {
        return this.text;
    }

    public SW360License setText(String text) {
        this.text = text;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getShortName() {
        return this.shortName;
    }

    public SW360License setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFullName() {
        return this.fullName;
    }

    public SW360License setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360License) || !super.equals(o)) return false;
        SW360License that = (SW360License) o;
        return Objects.equals(text, that.text) &&
                Objects.equals(shortName, that.shortName) &&
                Objects.equals(fullName, that.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, shortName, fullName);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360License;
    }
}
