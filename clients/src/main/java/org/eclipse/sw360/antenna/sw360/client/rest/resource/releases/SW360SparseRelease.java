/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.releases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResourceUtility;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SW360SparseRelease extends SW360SimpleHalResource {
    private String componentId;
    private String name;
    private String version;
    private String cpeid;
    private Set<String> mainLicenseIds;

    @JsonIgnore
    public String getReleaseId() {
        return Optional.ofNullable(getLinks())
                .map(LinkObjects::getSelf)
                .flatMap(SW360HalResourceUtility::getLastIndexOfSelfLink)
                .orElse(null);
    }

    public String getComponentId() {
        return componentId;
    }

    public SW360SparseRelease setComponentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public SW360SparseRelease setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SW360SparseRelease setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getCpeid() {
        return cpeid;
    }

    public SW360SparseRelease setCpeid(String cpeid) {
        this.cpeid = cpeid;
        return this;
    }

    public Set<String> getMainLicenseIds() {
        return mainLicenseIds;
    }

    public SW360SparseRelease setMainLicenseIds(Set<String> mainLicenseIds) {
        this.mainLicenseIds = mainLicenseIds;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360SparseRelease) || !super.equals(o)) return false;
        SW360SparseRelease that = (SW360SparseRelease) o;
        return Objects.equals(componentId, that.componentId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(cpeid, that.cpeid) &&
                Objects.equals(mainLicenseIds, that.mainLicenseIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), componentId, name, version, cpeid, mainLicenseIds);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360SparseRelease;
    }
}
