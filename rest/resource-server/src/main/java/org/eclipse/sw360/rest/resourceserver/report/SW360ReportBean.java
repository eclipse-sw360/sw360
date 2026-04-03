/*
SPDX-FileCopyrightText: © 2025 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;

import java.util.List;

@Setter
@Getter
public class SW360ReportBean{
    boolean withLinkedReleases;
    boolean excludeReleaseVersion;
    String generatorClassName;
    String variant;
    String template;
    String externalIds;
    boolean withSubProject;
    String bomType;
    List<ReleaseRelationship> selectedRelRelationship;

    public boolean isWithLinkedReleases() {
        return withLinkedReleases;
    }

    public void setWithLinkedReleases(boolean withLinkedReleases) {
        this.withLinkedReleases = withLinkedReleases;
    }

    public boolean isExcludeReleaseVersion() {
        return excludeReleaseVersion;
    }

    public void setExcludeReleaseVersion(boolean excludeReleaseVersion) {
        this.excludeReleaseVersion = excludeReleaseVersion;
    }

    public String getGeneratorClassName() {
        return generatorClassName;
    }

    public void setGeneratorClassName(String generatorClassName) {
        this.generatorClassName = generatorClassName;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(String externalIds) {
        this.externalIds = externalIds;
    }

    public boolean isWithSubProject() {
        return withSubProject;
    }

    public void setWithSubProject(boolean withSubProject) {
        this.withSubProject = withSubProject;
    }

    public String getBomType() {
        return bomType;
    }

    public void setBomType(String bomType) {
        this.bomType = bomType;
    }

    public List<ReleaseRelationship> getSelectedRelRelationship() {
        return selectedRelRelationship;
    }

    public void setSelectedRelRelationship(List<ReleaseRelationship> selectedRelRelationship) {
        this.selectedRelRelationship = selectedRelRelationship;
    }
}
