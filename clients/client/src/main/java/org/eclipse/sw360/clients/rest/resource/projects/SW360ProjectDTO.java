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
import org.eclipse.sw360.clients.rest.resource.LinkObjects;
import org.eclipse.sw360.clients.rest.resource.SW360HalResource;
import org.eclipse.sw360.clients.rest.resource.SW360Visibility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SW360ProjectDTO extends SW360HalResource<LinkObjects, SW360ProjectDTOEmbedded> {
    private String name;
    private String version;
    private SW360ProjectType projectType;
    private String description;
    private Map<String, String> externalIds;
    private String createdOn;
    private String businessUnit;
    private String clearingTeam;
    private SW360Visibility visibility;

    private List<SW360ReleaseLinkJSON> dependencyNetwork;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<SW360ReleaseLinkJSON> getDependencyNetwork() {
        return dependencyNetwork;
    }

    public void setDependencyNetwork(List<SW360ReleaseLinkJSON> dependencyNetwork) {
        this.dependencyNetwork = dependencyNetwork;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return this.name;
    }

    public SW360ProjectDTO setName(String name) {
        this.name = name;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getVersion() {
        return this.version;
    }

    public SW360ProjectDTO setVersion(String version) {
        this.version = version;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() {
        return this.description;
    }

    public SW360ProjectDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getExternalIds() {
        return this.externalIds;
    }

    public SW360ProjectDTO setExternalIds(Map<String, String> externalIds) {
        this.externalIds = externalIds;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCreatedOn() {
        return this.createdOn;
    }

    public SW360ProjectDTO setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getBusinessUnit() {
        return this.businessUnit;
    }

    public SW360ProjectDTO setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360ProjectType getProjectType() {
        return this.projectType;
    }

    public SW360ProjectDTO setProjectType(SW360ProjectType projectType) {
        this.projectType = projectType;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getClearingTeam() {
        return this.clearingTeam;
    }

    public SW360ProjectDTO setClearingTeam(String clearingTeam) {
        this.clearingTeam = clearingTeam;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360Visibility getVisibility() {
        return this.visibility;
    }

    public SW360ProjectDTO setVisibility(SW360Visibility visbility) {
        this.visibility = visbility;
        return this;
    }

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360ProjectDTOEmbedded createEmptyEmbedded() {
        return new SW360ProjectDTOEmbedded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360ProjectDTO) || !super.equals(o)) return false;
        SW360ProjectDTO that = (SW360ProjectDTO) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                projectType == that.projectType &&
                Objects.equals(description, that.description) &&
                Objects.equals(externalIds, that.externalIds) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(businessUnit, that.businessUnit) &&
                visibility == that.visibility &&
                Objects.equals(clearingTeam, that.clearingTeam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, version, projectType, description, externalIds, createdOn,
                businessUnit, visibility, clearingTeam);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360ProjectDTO;
    }
}
