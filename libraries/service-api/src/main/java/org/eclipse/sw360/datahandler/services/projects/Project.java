/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.projects;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.ProjectPackageRelationship;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.Visibility;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String name;

    private String description;

    private String version;

    private String domain;

    private Set<Attachment> attachments;

    private String createdOn;

    private String businessUnit;

    private ProjectState state;

    private ProjectType projectType;

    private String tag;

    private ProjectClearingState clearingState;

    private String createdBy;

    private String projectResponsible;

    private String leadArchitect;

    private Set<String> moderators;

    private Set<String> contributors;

    private Visibility visbility;

    private Map<String, Set<String>> roles;

    private Set<String> securityResponsibles;

    private String projectOwner;

    private String ownerAccountingUnit;

    private String ownerGroup;

    private String ownerCountry;

    private Map<String, ProjectProjectRelationship> linkedProjects;

    private Map<String, ProjectReleaseRelationship> releaseIdToUsage;

    private Map<String, ProjectPackageRelationship> packageIds;

    private String clearingTeam;

    private String preevaluationDeadline;

    private String systemTestStart;

    private String systemTestEnd;

    private String deliveryStart;

    private String phaseOutSince;

    private Boolean enableSvm;

    private Map<String, String> externalIds;

    private Map<String, String> additionalData;

    private Boolean considerReleasesFromExternalList;

    private String licenseInfoHeaderText;

    private Boolean enableVulnerabilitiesDisplay;

    private String obligationsText;

    private String clearingSummary;

    private String specialRisksOSS;

    private String generalRisks3rdParty;

    private String specialRisks3rdParty;

    private String deliveryChannels;

    private String remarksAdditionalRequirements;

    private DocumentState documentState;

    private String clearingRequestId;

    private ReleaseClearingStateSummary releaseClearingStateSummary;

    private String linkedObligationId;

    private Map<RequestedAction, Boolean> permissions;

    private Map<String, String> externalUrls;

    private Vendor vendor;

    private String vendorId;

    private String modifiedBy;

    private String modifiedOn;

    private String releaseRelationNetwork;
}
