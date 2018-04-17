/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonProjectRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonReleaseRelationSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Configuration
class JacksonCustomizations {
    @Bean
    public Module sw360Module() {
        return new Sw360Module();
    }

    @SuppressWarnings("serial")
    static class Sw360Module extends SimpleModule {
        public Sw360Module() {
            setMixInAnnotation(Project.class, Sw360Module.ProjectMixin.class);
            setMixInAnnotation(User.class, Sw360Module.UserMixin.class);
            setMixInAnnotation(Component.class, Sw360Module.ComponentMixin.class);
            setMixInAnnotation(Release.class, Sw360Module.ReleaseMixin.class);
            setMixInAnnotation(Attachment.class, Sw360Module.AttachmentMixin.class);
            setMixInAnnotation(Vendor.class, Sw360Module.VendorMixin.class);
            setMixInAnnotation(License.class, Sw360Module.LicenseMixin.class);
            setMixInAnnotation(Vulnerability.class, Sw360Module.VulnerabilityMixin.class);
            setMixInAnnotation(VulnerabilityDTO.class, Sw360Module.VulnerabilityDTOMixin.class);
            setMixInAnnotation(EccInformation.class, Sw360Module.EccInformationMixin.class);
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "attachments",
                "createdBy",
                "state",
                "projectResponsible",
                "leadArchitect",
                "moderators",
                "contributors",
                "visbility",
                "clearingTeam",
                "phaseOutSince",
                "homepage",
                "wiki",
                "documentState",
                "releaseClearingStateSummary",
                "permissions",
                "attachmentsIterator",
                "moderatorsIterator",
                "contributorsIterator",
                "releaseIdsIterator",
                "setId",
                "setRevision",
                "setType",
                "setName",
                "setDescription",
                "setVersion",
                "setExternalIds",
                "setAttachments",
                "setCreatedOn",
                "setState",
                "setProjectType",
                "setTag",
                "setCreatedBy",
                "setModerators",
                "setVisbility",
                "setHomepage",
                "externalIdsSize",
                "attachmentsSize",
                "setBusinessUnit",
                "setProjectResponsible",
                "setLeadArchitect",
                "moderatorsSize",
                "contributorsSize",
                "setContributors",
                "linkedProjectsSize",
                "setLinkedProjects",
                "releaseIdToUsageSize",
                "setReleaseIdToUsage",
                "setClearingTeam",
                "setPreevaluationDeadline",
                "setSystemTestStart",
                "setSystemTestEnd",
                "setDeliveryStart",
                "setPhaseOutSince",
                "setDocumentState",
                "releaseIdsSize",
                "setReleaseClearingStateSummary",
                "permissionsSize",
                "setWiki",
                "setReleaseIds",
                "setPermissions",
                "setClearingState",
                "securityResponsibles",
                "securityResponsiblesSize",
                "securityResponsiblesIterator",
                "setSecurityResponsibles",
                "setOwnerGroup",
                "setOwnerCountry",
                "rolesSize",
                "setRoles",
                "setOwnerAccountingUnit",
                "setLicenseInfoHeaderText",
                "setProjectOwner",
                "enableSvm",
                "setEnableSvm"
        })
        static abstract class ProjectMixin extends Project {

            @Override
            @JsonProperty("projectType")
            abstract public ProjectType getProjectType();

            @Override
            @JsonSerialize(using = JsonProjectRelationSerializer.class)
            @JsonProperty("linkedProjects")
            abstract public Map<String, ProjectRelationship> getLinkedProjects();

            @Override
            @JsonSerialize(using = JsonReleaseRelationSerializer.class)
            @JsonProperty("linkedReleases")
            abstract public Map<String, ProjectReleaseRelationship> getReleaseIdToUsage();

            @Override
            @JsonProperty("id")
            abstract public String getId();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "externalid",
                "wantsMailNotification",
                "setWantsMailNotification",
                "setId",
                "setRevision",
                "setType",
                "setEmail",
                "setUserGroup",
                "setExternalid",
                "setFullname",
                "setGivenname",
                "setLastname",
                "setDepartment",
                "notificationPreferencesSize",
                "setNotificationPreferences",
                "setCommentMadeDuringModerationRequest"
        })
        static abstract class UserMixin extends User {
            @Override
            @JsonProperty("fullName")
            abstract public String getFullname();

            @Override
            @JsonProperty("givenName")
            abstract public String getGivenname();

            @Override
            @JsonProperty("lastName")
            abstract public String getLastname();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "attachments",
                "createdBy",
                "subscribers",
                "moderators",
                "releases",
                "mainLicenseIds",
                "softwarePlatforms",
                "homepage",
                "mailinglist",
                "wiki",
                "blog",
                "wikipedia",
                "openHub",
                "documentState",
                "permissions",
                "setId",
                "setRevision",
                "setType",
                "setName",
                "setDescription",
                "setAttachments",
                "setCreatedOn",
                "setCreatedBy",
                "setSubscribers",
                "setModerators",
                "releasesSize",
                "setReleases",
                "setReleaseIds",
                "setCategories",
                "languagesSize",
                "setLanguages",
                "setVendorNames",
                "setHomepage",
                "setMailinglist",
                "setWiki",
                "setBlog",
                "setWikipedia",
                "setOpenHub",
                "setPermissions",
                "attachmentsSize",
                "attachmentsIterator",
                "setComponentType",
                "subscribersSize",
                "subscribersIterator",
                "moderatorsSize",
                "moderatorsIterator",
                "releasesIterator",
                "releaseIdsSize",
                "releaseIdsIterator",
                "mainLicenseIdsSize",
                "mainLicenseIdsIterator",
                "setMainLicenseIds",
                "categoriesSize",
                "categoriesIterator",
                "languagesIterator",
                "softwarePlatformsSize",
                "softwarePlatformsIterator",
                "setExternalIds",
                "externalIdsSize",
                "setSoftwarePlatforms",
                "operatingSystemsSize",
                "operatingSystemsIterator",
                "setOperatingSystems",
                "vendorNamesSize",
                "vendorNamesIterator",
                "setDocumentState",
                "permissionsSize",
                "setComponentOwner",
                "setOwnerAccountingUnit",
                "setOwnerGroup",
                "setOwnerCountry",
                "rolesSize",
                "setRoles",
        })
        static abstract class ComponentMixin extends Component {
            @Override
            @JsonProperty("vendors")
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "permissions",
                "createdBy",
                "moderators",
                "clearingInformation",
                "setAttachments",
                "setCreatedOn",
                "setRepository",
                "setFossologyId",
                "setCreatedBy",
                "setModerators",
                "setSubscribers",
                "setVendor",
                "setVendorId",
                "languagesSize",
                "setLanguages",
                "setCotsDetails",
                "setDownloadurl",
                "setPermissions",
                "externalIdsSize",
                "attachmentsSize",
                "setMainlineState",
                "setClearingState",
                "setAttachmentInFossology",
                "contributorsSize",
                "setContributors",
                "moderatorsSize",
                "moderatorsIterator",
                "subscribersSize",
                "setClearingInformation",
                "operatingSystemsSize",
                "setOperatingSystems",
                "mainLicenseIdsSize",
                "setMainLicenseIds",
                "releaseIdToRelationshipSize",
                "setReleaseIdToRelationship",
                "setDocumentState",
                "permissionsSize",
                "setId",
                "setRevision",
                "setType",
                "setCpeid",
                "setName",
                "setVersion",
                "setComponentId",
                "setReleaseDate",
                "setExternalIds",
                "clearingTeamToFossologyStatusSize",
                "setClearingTeamToFossologyStatus",
                "setEccInformation",
                "languages",
                "operatingSystems",
                "languagesIterator",
                "operatingSystemsIterator",
                "cotsDetails",
                "releaseIdToRelationship",
                "documentState",
                "contributorsIterator",
                "rolesSize",
                "setRoles",
                "setCreatorDepartment"
        })
        static abstract class ReleaseMixin extends Release {
            @Override
            @JsonProperty("cpeId")
            abstract public String getCpeid();

            @Override
            @JsonProperty("eccInformation")
            abstract public EccInformation getEccInformation();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "attachmentContentId",
                "setAttachmentContentId",
                "setAttachmentType",
                "setCreatedComment",
                "setCheckedComment",
                "uploadHistory",
                "uploadHistorySize",
                "uploadHistoryIterator",
                "setUploadHistory",
                "setFilename",
                "setSha1",
                "setCreatedBy",
                "setCreatedTeam",
                "setCreatedOn",
                "setCheckedBy",
                "setCheckedTeam",
                "setCheckedOn",
                "setCheckStatus"
        })
        static abstract class AttachmentMixin {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "permissionsSize",
                "setId",
                "setRevision",
                "setType",
                "setPermissions",
                "setFullname",
                "setShortname",
                "setUrl"
        })
        static abstract class VendorMixin extends Vendor {
            @Override
            @JsonProperty("fullName")
            abstract public String getFullname();

            @Override
            @JsonProperty("shortName")
            abstract public String getShortname();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "licenseType",
                "licenseTypeDatabaseId",
                "externalLicenseLink",
                "GPLv2Compat",
                "GPLv3Compat",
                "reviewdate",
                "todos",
                "todoDatabaseIds",
                "risks",
                "riskDatabaseIds",
                "documentState",
                "permissions",
                "setId",
                "setRevision",
                "setType",
                "setExternalIds",
                "externalIdsSize",
                "setDocumentState",
                "permissionsSize",
                "setLicenseTypeDatabaseId",
                "setExternalLicenseLink",
                "todoDatabaseIdsSize",
                "todoDatabaseIdsIterator",
                "setTodoDatabaseIds",
                "riskDatabaseIdsSize",
                "riskDatabaseIdsIterator",
                "setRiskDatabaseIds",
                "setPermissions",
                "setFullname",
                "setShortname",
                "setLicenseType",
                "gplv2Compat",
                "setGPLv2Compat",
                "gplv3Compat",
                "setGPLv3Compat",
                "setReviewdate",
                "todosSize",
                "todosIterator",
                "setTodos",
                "risksSize",
                "risksIterator",
                "setRisks",
                "setText",
                "mainLicenseIdsIterator"
        })
        static abstract class LicenseMixin extends License {
            @Override
            @JsonProperty("fullName")
            abstract public String getFullname();

            @Override
            @JsonProperty("shortName")
            abstract public String getShortname();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "type",
                "externalId",
                "title",
                "description",
                "publishDate",
                "lastExternalUpdate",
                "priority",
                "priorityToolTip",
                "action",
                "impact",
                "legalNotice",
                "cveReferences",
                "references",
                "intComponentId",
                "intComponentName",
                "releaseVulnerabilityRelation",
                "matchedBy",
                "usedNeedle",
                "setType",
                "setId",
                "setRevision",
                "setIntReleaseName",
                "setLastExternalUpdate",
                "setIntComponentId",
                "setIntComponentName",
                "referencesSize",
                "setPriorityToolTip",
                "setCveReferences",
                "setIntReleaseId",
                "cveReferencesSize",
                "setDescription",
                "setReleaseVulnerabilityRelation",
                "setImpact",
                "setMatchedBy",
                "setLegalNotice",
                "setUsedNeedle",
                "setReferences",
                "setPriority",
                "setAction",
                "impactSize",
                "setExternalId",
                "setPublishDate",
                "setTitle",
                "referencesIterator",
                "cveReferencesIterator"
        })
        static abstract class VulnerabilityDTOMixin extends VulnerabilityDTO {
            @Override
            @JsonProperty("id")
            abstract public String getId();

            @Override
            @JsonProperty("intReleaseId")
            abstract public String getIntReleaseId();

            @Override
            @JsonProperty("intReleaseName")
            abstract public String getIntReleaseName();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "type",
                "publishDate",
                "lastExternalUpdate",
                "priority",
                "priorityToolTip",
                "action",
                "impact",
                "legalNotice",
                "cveReferences",
                "references",
                "intComponentId",
                "intComponentName",
                "releaseVulnerabilityRelation",
                "matchedBy",
                "usedNeedle",
                "setType",
                "setId",
                "setRevision",
                "setIntReleaseName",
                "setLastExternalUpdate",
                "setIntComponentId",
                "setIntComponentName",
                "referencesSize",
                "setPriorityToolTip",
                "setCveReferences",
                "setIntReleaseId",
                "cveReferencesSize",
                "setDescription",
                "setReleaseVulnerabilityRelation",
                "setImpact",
                "setMatchedBy",
                "setLegalNotice",
                "setUsedNeedle",
                "setReferences",
                "setPriority",
                "setAction",
                "impactSize",
                "setExternalId",
                "setPublishDate",
                "setTitle",
                "cvss",
                "isSetCvss",
                "cvssTime",
                "vulnerableConfiguration",
                "access",
                "cveFurtherMetaDataPerSource",
                "setLastUpdateDate",
                "setPriorityText",
                "cveReferencesIterator",
                "setCveFurtherMetaDataPerSource",
                "setAssignedExtComponentIds",
                "referencesIterator",
                "setVulnerableConfiguration",
                "setExtendedDescription",
                "vulnerableConfigurationSize",
                "assignedExtComponentIdsSize",
                "vendorAdvisoriesSize",
                "setVendorAdvisories",
                "cveFurtherMetaDataPerSourceSize",
                "setCvss",
                "setCwe",
                "setIsSetCvss",
                "setCvssTime",
                "setAccess",
                "accessSize",
        })
        static abstract class VulnerabilityMixin extends Vulnerability {
            @Override
            @JsonProperty("id")
            abstract public String getId();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonIgnoreProperties({
                "assessorContactPerson",
                "assessorDepartment",
                "eccComment",
                "materialIndexNumber",
                "assessmentDate",
                "setEccComment",
                "setECCN",
                "setEccStatus",
                "setAL",
                "setAssessorContactPerson",
                "setAssessmentDate",
                "setAssessorDepartment",
                "setMaterialIndexNumber"
        })
        static abstract class EccInformationMixin extends EccInformation {
            @Override
            @JsonProperty("eccStatus")
            abstract public ECCStatus getEccStatus();
        }
    }
}
