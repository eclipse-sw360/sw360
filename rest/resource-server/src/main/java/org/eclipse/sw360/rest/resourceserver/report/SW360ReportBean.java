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
    String format;
}
