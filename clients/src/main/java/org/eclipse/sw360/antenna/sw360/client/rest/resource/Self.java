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

package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import java.io.Serializable;
import java.util.Objects;

public class Self implements Serializable {
    private String href;

    public Self() {
        // no-òp
    }

    public Self(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public Self setHref(String href) {
        this.href = href;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Self self = (Self) o;
        return Objects.equals(href, self.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href);
    }
}
