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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;

import java.util.*;

@JsonDeserialize(as = SW360LicenseListEmbedded.class)
public class SW360LicenseListEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:licenses")
    private Set<SW360SparseLicense> licenses;

    public Set<SW360SparseLicense> getLicenses() {
        return Optional.ofNullable(licenses)
                .map(HashSet::new)
                .orElse(new HashSet<>());
    }

    public SW360LicenseListEmbedded setLicenses(List<SW360SparseLicense> licenses) {
        this.licenses = new HashSet<>(licenses);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof SW360LicenseListEmbedded)) return false;
        SW360LicenseListEmbedded that = (SW360LicenseListEmbedded) o;
        return that.canEqual(this) &&
                Objects.equals(licenses, that.licenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenses);
    }

    public boolean canEqual(Object o) {
        return o instanceof SW360LicenseListEmbedded;
    }
}
