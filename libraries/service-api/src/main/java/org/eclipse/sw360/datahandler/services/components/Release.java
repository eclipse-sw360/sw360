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
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.MainlineState;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Release {

    private String id;
    private String revision;
    private String type;
    private String cpeid;
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private String version;
    @JsonProperty(required = true)
    private String componentId;
    private String releaseDate;
    private ComponentType componentType;
    private Map<String, String> externalIds;
    private Map<String, String> additionalData;
    private String sourceCodeDownloadurl;
    private String binaryDownloadurl;
    private Set<Attachment> attachments;
    private String createdOn;
    private Repository repository;
    private MainlineState mainlineState;
    private ClearingState clearingState;
    private Set<ExternalToolProcess> externalToolProcesses;
    private String createdBy;
    private String creatorDepartment;
    private MainlineState projectMainlineState;
    private Set<String> contributors;
    private Set<String> moderators;
    private Set<String> subscribers;
    private Map<String, Set<String>> roles;
    private Set<String> mainLicenseIds;
    private Set<String> otherLicenseIds;
    private Vendor vendor;
    private String vendorId;
    private ClearingInformation clearingInformation;
    private Set<String> languages;
    private Set<String> operatingSystems;
    private COTSDetails cotsDetails;
    private EccInformation eccInformation;
    private Set<String> softwarePlatforms;
    private Map<String, ReleaseRelationship> releaseIdToRelationship;
    private Set<String> packageIds;
    private DocumentState documentState;
    private Map<RequestedAction, Boolean> permissions;
    private String spdxId;
    private String modifiedBy;
    private String modifiedOn;
}
