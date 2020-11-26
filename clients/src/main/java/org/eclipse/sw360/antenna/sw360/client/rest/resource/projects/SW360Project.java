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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360Visibility;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SW360Project extends SW360HalResource<LinkObjects, SW360ProjectEmbedded> {
    private String name;
    private String version;
    private SW360ProjectType projectType;
    private String description;
    private Map<String, String> externalIds;
    private String createdOn;
    private String businessUnit;
    private String clearingTeam;
    private SW360Visibility visibility;
    private Map<String, SW360ProjectReleaseRelationship> releaseIdToUsage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return this.name;
    }

    public SW360Project setName(String name) {
        this.name = name;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getVersion() {
        return this.version;
    }

    public SW360Project setVersion(String version) {
        this.version = version;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() {
        return this.description;
    }

    public SW360Project setDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getExternalIds() {
        return this.externalIds;
    }

    public SW360Project setExternalIds(Map<String, String> externalIds) {
        this.externalIds = externalIds;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCreatedOn() {
        return this.createdOn;
    }

    public SW360Project setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getBusinessUnit() {
        return this.businessUnit;
    }

    public SW360Project setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360ProjectType getProjectType() {
        return this.projectType;
    }

    public SW360Project setProjectType(SW360ProjectType projectType) {
        this.projectType = projectType;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getClearingTeam() {
        return this.clearingTeam;
    }

    public SW360Project setClearingTeam(String clearingTeam) {
        this.clearingTeam = clearingTeam;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360Visibility getVisibility() {
        return this.visibility;
    }

    public SW360Project setVisibility(SW360Visibility visbility) {
        this.visibility = visbility;
        return this;
    }

    public Map<String, SW360ProjectReleaseRelationship> getReleaseIdToUsage() {
        if (this.releaseIdToUsage == null) {
            this.releaseIdToUsage = new HashMap<>();
        }
        return this.releaseIdToUsage;
    }

    public SW360Project setReleaseIdToUsage(Map<String, SW360ProjectReleaseRelationship> releaseIdToUsage) {
        this.releaseIdToUsage = releaseIdToUsage;
        return this;
    }

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360ProjectEmbedded createEmptyEmbedded() {
        return new SW360ProjectEmbedded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360Project) || !super.equals(o)) return false;
        SW360Project that = (SW360Project) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                projectType == that.projectType &&
                Objects.equals(description, that.description) &&
                Objects.equals(externalIds, that.externalIds) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(businessUnit, that.businessUnit) &&
                visibility == that.visibility &&
                Objects.equals(releaseIdToUsage, that.releaseIdToUsage) &&
                Objects.equals(clearingTeam, that.clearingTeam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, version, projectType, description, externalIds, createdOn,
                businessUnit, visibility, releaseIdToUsage, clearingTeam);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360Project;
    }
}
