/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.MainlineState;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseLink {
    @JsonProperty(required = true)
    private String id;
    @JsonProperty(required = true)
    private String vendor;
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private String version;
    @JsonProperty(required = true)
    private String longName;
    private ReleaseRelationship releaseRelationship;
    private MainlineState mainlineState;
    @JsonProperty(required = true)
    private Boolean hasSubreleases;
    private String nodeId;
    private String parentNodeId;
    private ClearingReport clearingReport;
    private ClearingState clearingState;
    private List<Attachment> attachments;
    private ComponentType componentType;
    private Set<String> licenseIds;
    private Set<String> licenseNames;
    private String comment;
    private Set<String> otherLicenseIds;
    private Boolean accessible;
    private String componentId;
    private List<Release> releaseWithSameComponent;
    private Integer layer;
    private Integer index;
    private String defaultValue;
    private String projectId;
    private MainlineState releaseMainLineState;
    private String createdBy;
}
