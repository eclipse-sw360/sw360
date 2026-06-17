/*
SPDX-FileCopyrightText: © 2025 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;

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
    String format;

    // Project search/filter parameters for filtered export
    String name;
    String type;
    String group;
    String tag;
    String version;
    String projectResponsible;
    ProjectState projectState;
    ProjectClearingState projectClearingState;
    String additionalData;
    boolean luceneSearch;
}
