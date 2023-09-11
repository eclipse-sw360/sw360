/*
 * Copyright Siemens AG, 2017-2019.
 * Copyright Bosch Software Innovations GmbH, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentDTO;
import org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageAttachment;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonProjectRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonReleaseRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.EmbeddedModerationRequest;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.ModerationPatch;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProject;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProjectDTO;
import org.springdoc.core.SpringDocUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Configuration
public class JacksonCustomizations {
    @Bean
    public Module sw360Module() {
        return new Sw360Module();
    }

    @SuppressWarnings("serial")
    public static class Sw360Module extends SimpleModule {
        public Sw360Module() {
            setMixInAnnotation(MultiStatus.class, MultiStatusMixin.class);
            setMixInAnnotation(Project.class, Sw360Module.ProjectMixin.class);
            setMixInAnnotation(User.class, Sw360Module.UserMixin.class);
            setMixInAnnotation(Component.class, Sw360Module.ComponentMixin.class);
            setMixInAnnotation(ComponentDTO.class, Sw360Module.ComponentDTOMixin.class);
            setMixInAnnotation(Package.class, Sw360Module.PackageMixin.class);
            setMixInAnnotation(Release.class, Sw360Module.ReleaseMixin.class);
            setMixInAnnotation(ReleaseLink.class, Sw360Module.ReleaseLinkMixin.class);
            setMixInAnnotation(ClearingReport.class, Sw360Module.ClearingReportMixin.class);
            setMixInAnnotation(Attachment.class, Sw360Module.AttachmentMixin.class);
            setMixInAnnotation(AttachmentDTO.class, Sw360Module.AttachmentDTOMixin.class);
            setMixInAnnotation(UsageAttachment.class, Sw360Module.UsageAttachmentMixin.class);
            setMixInAnnotation(ProjectUsage.class, Sw360Module.ProjectUsageMixin.class);
            setMixInAnnotation(Vendor.class, Sw360Module.VendorMixin.class);
            setMixInAnnotation(License.class, Sw360Module.LicenseMixin.class);
            setMixInAnnotation(Obligation.class, Sw360Module.ObligationMixin.class);
            setMixInAnnotation(Vulnerability.class, Sw360Module.VulnerabilityMixin.class);
            setMixInAnnotation(VulnerabilityState.class, Sw360Module.VulnerabilityStateMixin.class);
            setMixInAnnotation(ReleaseVulnerabilityRelationDTO.class, Sw360Module.ReleaseVulnerabilityRelationDTOMixin.class);
            setMixInAnnotation(VulnerabilityDTO.class, Sw360Module.VulnerabilityDTOMixin.class);
            setMixInAnnotation(VulnerabilityApiDTO.class, Sw360Module.VulnerabilityApiDTOMixin.class);
            setMixInAnnotation(EccInformation.class, Sw360Module.EccInformationMixin.class);
            setMixInAnnotation(EmbeddedProject.class, Sw360Module.EmbeddedProjectMixin.class);
            setMixInAnnotation(ExternalToolProcess.class, Sw360Module.ExternalToolProcessMixin.class);
            setMixInAnnotation(ExternalToolProcessStep.class, Sw360Module.ExternalToolProcessStepMixin.class);
            setMixInAnnotation(COTSDetails.class, Sw360Module.COTSDetailsMixin.class);
            setMixInAnnotation(ClearingInformation.class, Sw360Module.ClearingInformationMixin.class);
            setMixInAnnotation(Repository.class, Sw360Module.RepositoryMixin.class);
            setMixInAnnotation(SearchResult.class, Sw360Module.SearchResultMixin.class);
            setMixInAnnotation(ChangeLogs.class, Sw360Module.ChangeLogsMixin.class);
            setMixInAnnotation(ChangedFields.class, Sw360Module.ChangedFieldsMixin.class);
            setMixInAnnotation(ReferenceDocData.class, Sw360Module.ReferenceDocDataMixin.class);
            setMixInAnnotation(ClearingRequest.class, Sw360Module.ClearingRequestMixin.class);
            setMixInAnnotation(Comment.class, Sw360Module.CommentMixin.class);
            setMixInAnnotation(ProjectReleaseRelationship.class, Sw360Module.ProjectReleaseRelationshipMixin.class);
            setMixInAnnotation(ReleaseVulnerabilityRelation.class, Sw360Module.ReleaseVulnerabilityRelationMixin.class);
            setMixInAnnotation(VerificationStateInfo.class, Sw360Module.VerificationStateInfoMixin.class);
            setMixInAnnotation(ProjectProjectRelationship.class, Sw360Module.ProjectProjectRelationshipMixin.class);
            setMixInAnnotation(ModerationRequest.class, Sw360Module.ModerationRequestMixin.class);
            setMixInAnnotation(EmbeddedModerationRequest.class, Sw360Module.EmbeddedModerationRequestMixin.class);
            setMixInAnnotation(ImportBomRequestPreparation.class, Sw360Module.ImportBomRequestPreparationMixin.class);
            setMixInAnnotation(ModerationPatch.class, Sw360Module.ModerationPatchMixin.class);
            setMixInAnnotation(ProjectDTO.class, Sw360Module.ProjectDTOMixin.class);
            setMixInAnnotation(EmbeddedProjectDTO.class, Sw360Module.EmbeddedProjectDTOMixin.class);
            setMixInAnnotation(ReleaseNode.class, Sw360Module.ReleaseNodeMixin.class);
            setMixInAnnotation(RestrictedResource.class, Sw360Module.RestrictedResourceMixin.class);

            // Make spring doc aware of the mixin(s)
            SpringDocUtils.getConfig()
                    .replaceWithClass(Project.class, Sw360Module.ProjectMixin.class)
                    .replaceWithClass(MultiStatus.class, MultiStatusMixin.class)
                    .replaceWithClass(User.class, Sw360Module.UserMixin.class)
                    .replaceWithClass(Component.class, Sw360Module.ComponentMixin.class)
                    .replaceWithClass(ComponentDTO.class, Sw360Module.ComponentDTOMixin.class)
                    .replaceWithClass(Release.class, Sw360Module.ReleaseMixin.class)
                    .replaceWithClass(ReleaseLink.class, Sw360Module.ReleaseLinkMixin.class)
                    .replaceWithClass(ClearingReport.class, Sw360Module.ClearingReportMixin.class)
                    .replaceWithClass(Attachment.class, Sw360Module.AttachmentMixin.class)
                    .replaceWithClass(AttachmentDTO.class, Sw360Module.AttachmentDTOMixin.class)
                    .replaceWithClass(UsageAttachment.class, Sw360Module.UsageAttachmentMixin.class)
                    .replaceWithClass(ProjectUsage.class, Sw360Module.ProjectUsageMixin.class)
                    .replaceWithClass(Vendor.class, Sw360Module.VendorMixin.class)
                    .replaceWithClass(License.class, Sw360Module.LicenseMixin.class)
                    .replaceWithClass(Obligation.class, Sw360Module.ObligationMixin.class)
                    .replaceWithClass(Vulnerability.class, Sw360Module.VulnerabilityMixin.class)
                    .replaceWithClass(VulnerabilityState.class, Sw360Module.VulnerabilityStateMixin.class)
                    .replaceWithClass(ReleaseVulnerabilityRelationDTO.class, Sw360Module.ReleaseVulnerabilityRelationDTOMixin.class)
                    .replaceWithClass(VulnerabilityDTO.class, Sw360Module.VulnerabilityDTOMixin.class)
                    .replaceWithClass(VulnerabilityApiDTO.class, Sw360Module.VulnerabilityApiDTOMixin.class)
                    .replaceWithClass(EccInformation.class, Sw360Module.EccInformationMixin.class)
                    .replaceWithClass(EmbeddedProject.class, Sw360Module.EmbeddedProjectMixin.class)
                    .replaceWithClass(ExternalToolProcess.class, Sw360Module.ExternalToolProcessMixin.class)
                    .replaceWithClass(ExternalToolProcessStep.class, Sw360Module.ExternalToolProcessStepMixin.class)
                    .replaceWithClass(COTSDetails.class, Sw360Module.COTSDetailsMixin.class)
                    .replaceWithClass(ClearingInformation.class, Sw360Module.ClearingInformationMixin.class)
                    .replaceWithClass(Repository.class, Sw360Module.RepositoryMixin.class)
                    .replaceWithClass(SearchResult.class, Sw360Module.SearchResultMixin.class)
                    .replaceWithClass(ChangeLogs.class, Sw360Module.ChangeLogsMixin.class)
                    .replaceWithClass(ChangedFields.class, Sw360Module.ChangedFieldsMixin.class)
                    .replaceWithClass(ReferenceDocData.class, Sw360Module.ReferenceDocDataMixin.class)
                    .replaceWithClass(ClearingRequest.class, Sw360Module.ClearingRequestMixin.class)
                    .replaceWithClass(Comment.class, Sw360Module.CommentMixin.class)
                    .replaceWithClass(ProjectReleaseRelationship.class, Sw360Module.ProjectReleaseRelationshipMixin.class)
                    .replaceWithClass(ReleaseVulnerabilityRelation.class, Sw360Module.ReleaseVulnerabilityRelationMixin.class)
                    .replaceWithClass(VerificationStateInfo.class, Sw360Module.VerificationStateInfoMixin.class)
                    .replaceWithClass(ProjectProjectRelationship.class, Sw360Module.ProjectProjectRelationshipMixin.class)
                    .replaceWithClass(ModerationRequest.class, Sw360Module.ModerationRequestMixin.class)
                    .replaceWithClass(EmbeddedModerationRequest.class, Sw360Module.EmbeddedModerationRequestMixin.class)
                    .replaceWithClass(ImportBomRequestPreparation.class, Sw360Module.ImportBomRequestPreparationMixin.class)
                    .replaceWithClass(ModerationPatch.class, Sw360Module.ModerationPatchMixin.class)
                    .replaceWithClass(ProjectDTO.class, Sw360Module.ProjectDTOMixin.class)
                    .replaceWithClass(EmbeddedProjectDTO.class, Sw360Module.EmbeddedProjectDTOMixin.class)
                    .replaceWithClass(ReleaseNode.class, Sw360Module.ReleaseNodeMixin.class)
                    .replaceWithClass(RestrictedResource.class, Sw360Module.RestrictedResourceMixin.class);
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        static abstract class MultiStatusMixin extends MultiStatus {
            @Override
            @JsonProperty("status")
            abstract public int getStatusCode();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "attachments",
                "createdBy",
                "visbility",
                "clearingTeam",
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
                "setDomain",
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
                "setClearingSummary",
                "setObligationsText",
                "setSpecialRisksOSS",
                "setGeneralRisks3rdParty",
                "setSpecialRisks3rdParty",
                "setDeliveryChannels",
                "setRemarksAdditionalRequirements",
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
                "setEnableSvm",
                "setEnableVulnerabilitiesDisplay",
                "additionalDataSize",
                "setAdditionalData",
                "setLinkedObligationId",
                "linkedObligationId",
                "setClearingRequestId",
                "setConsiderReleasesFromExternalList",
                "externalUrlsSize",
                "setExternalUrls",
                "setVendor",
                "setVendorId",
                "setSpdxId",
                "setModifiedOn",
                "setModifiedBy",
                "modifiedBy",
                "packageIdsSize",
                "setPackageIds",
                "packageIdsIterator",
                "packageIds",
                "setReleaseRelationNetwork",
                "releaseRelationNetwork",
                "projectTypeIsSet",
                "tagIsSet",
                "projectResponsibleIsSet",
                "leadArchitectIsSet",
                "securityResponsiblesIsSet",
                "projectOwnerIsSet",
                "linkedProjectsIsSet",
                "releaseIdToUsageIsSet",
                "clearingTeamIsSet",
                "preevaluationDeadlineIsSet",
                "systemTestStartIsSet",
                "systemTestEndIsSet",
                "deliveryStartIsSet",
                "phaseOutSinceIsSet",
                "enableSvmIsSet",
                "considerReleasesFromExternalListIsSet",
                "licenseInfoHeaderTextIsSet",
                "enableVulnerabilitiesDisplayIsSet",
                "obligationsTextIsSet",
                "clearingSummaryIsSet",
                "specialRisksOSSIsSet",
                "generalRisks3rdPartyIsSet",
                "specialRisks3rdPartyIsSet",
                "deliveryChannelsIsSet",
                "remarksAdditionalRequirementsIsSet",
                "clearingRequestIdIsSet",
                "releaseClearingStateSummaryIsSet",
                "linkedObligationIdIsSet",
                "externalUrlsIsSet",
                "releaseRelationNetworkIsSet",
                "domainIsSet",
                "stateIsSet",
                "createdByIsSet",
                "createdOnIsSet",
                "packageIdsIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "versionIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "clearingStateIsSet",
                "contributorsIsSet",
                "rolesIsSet",
                "vendorIsSet",
                "vendorIdIsSet",
                "ownerAccountingUnitIsSet",
                "ownerGroupIsSet",
                "ownerCountryIsSet",
                "visbilityIsSet",
                "businessUnitIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "nameIsSet",
                "descriptionIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet"
        })
        static abstract class ProjectMixin extends Project {

            @Override
            @JsonProperty("projectType")
            abstract public ProjectType getProjectType();

            @Override
            @JsonSerialize(using = JsonProjectRelationSerializer.class)
            @JsonProperty("linkedProjects")
            abstract public Map<String, ProjectProjectRelationship> getLinkedProjects();

            @Override
            @JsonSerialize(using = JsonReleaseRelationSerializer.class)
            @JsonProperty("linkedReleases")
            abstract public Map<String, ProjectReleaseRelationship> getReleaseIdToUsage();

            @Override
            @JsonProperty("visibility")
            abstract public Visibility getVisbility();

            @Override
            @JsonProperty("id")
            abstract public String getId();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            abstract public Set<String> getContributors();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            abstract public Set<String> getModerators();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            abstract public String getLeadArchitect();

            @Override
            @JsonProperty(access = Access.READ_ONLY)
            abstract public String getClearingRequestId();
        }

        static abstract class EmbeddedProjectMixin extends ProjectMixin {
            @Override
            @JsonIgnore
            abstract public boolean isEnableSvm();

            @Override
            @JsonIgnore
            abstract public boolean isEnableVulnerabilitiesDisplay();

            @Override
            abstract public ProjectState getState();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "setPassword",
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
                "formerEmailAddressesSize",
                "formerEmailAddressesIterator",
                "setFormerEmailAddresses",
                "setCommentMadeDuringModerationRequest",
                "restApiTokens",
                "restApiTokensSize",
                "setRestApiTokens",
                "restApiTokensIterator",
                "myProjectsPreferenceSelection",
                "myProjectsPreferenceSelectionSize",
                "setMyProjectsPreferenceSelection",
                "secondaryDepartmentsAndRolesSize",
                "setSecondaryDepartmentsAndRoles",
                "primaryRoles",
                "primaryRolesSize",
                "setPrimaryRoles",
                "setDeactivated",
                "oidcClientInfosSize",
                "setOidcClientInfos",
                "commentMadeDuringModerationRequest",
                "oidcClientInfosIsSet",
                "passwordIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "emailIsSet",
                "userGroupIsSet",
                "externalidIsSet",
                "fullnameIsSet",
                "givennameIsSet",
                "lastnameIsSet",
                "departmentIsSet",
                "wantsMailNotificationIsSet",
                "commentMadeDuringModerationRequestIsSet",
                "notificationPreferencesIsSet",
                "formerEmailAddressesIsSet",
                "restApiTokensIsSet",
                "myProjectsPreferenceSelectionIsSet",
                "secondaryDepartmentsAndRolesIsSet",
                "primaryRolesIterator",
                "primaryRolesIsSet",
                "deactivatedIsSet"
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

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            abstract public String getPassword();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "revision",
                "attachments",
                "createdBy",
                "releases",
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
                "setDefaultVendor",
                "setDefaultVendorId",
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
                "additionalDataSize",
                "setAdditionalData",
                "setModifiedOn",
                "setModifiedBy",
                "modifiedBy",
                "cdxComponentType",
                "setCdxComponentType",
                "setVcs",
        })
        static abstract class ComponentMixin extends Component {
            @Override
            @JsonProperty(PropertyKeyMapping.COMPONENT_VENDOR_KEY_JSON)
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "id",
                "revision",
                "type",
                "licenseIdsSize",
                "licenseIdsIterator",
                "createdBy",
                "setId",
                "setRevision",
                "setType",
                "setName",
                "setDescription",
                "setCreatedOn",
                "setCreatedBy",
                "setModifiedOn",
                "setModifiedBy",
                "setHash",
                "setVersion",
                "setVendor",
                "setVendorId",
                "setReleaseId",
                "setPurl",
                "setLicenseIds",
                "setHomepageUrl",
                "setVcs",
                "setPackageManager",
                "releaseId",
                "setRelease",
                "packageType",
                "setPackageType"
        })
        static abstract class PackageMixin extends Package {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "id",
                "type",
                "revision",
                "attachments",
                "createdBy",
                "releases",
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
                "setDefaultVendor",
                "setDefaultVendorId",
                "setCategories",
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
                "additionalDataSize",
                "setAdditionalData",
                "cdxComponentType",
                "setCdxComponentType",
                "setAttachmentDTOs",
                "attachmentDTOsIterator",
                "attachmentDTOsSize",
                "setBusinessUnit",
                "setVisbility",
                "visbility"
        })
        static abstract class ComponentDTOMixin extends Component {
            @Override
            @JsonProperty(PropertyKeyMapping.COMPONENT_VENDOR_KEY_JSON)
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "permissions",
                "subscribers",
                "setAttachments",
                "setCreatedOn",
                "setRepository",
                "setCreatedBy",
                "setModerators",
                "setSubscribers",
                "setVendor",
                "setVendorId",
                "languagesSize",
                "setLanguages",
                "setCotsDetails",
                "setSourceCodeDownloadurl",
                "setPermissions",
                "externalIdsSize",
                "attachmentsIterator",
                "attachmentsSize",
                "setMainlineState",
                "setClearingState",
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
                "setSpdxId",
                "externalToolProcessesSize",
                "setExternalToolProcesses",
                "setEccInformation",
                "languagesIterator",
                "operatingSystemsIterator",
                "cotsDetails",
                "documentState",
                "contributorsIterator",
                "rolesSize",
                "setRoles",
                "setCreatorDepartment",
                "setSoftwarePlatforms",
                "softwarePlatformsSize",
                "softwarePlatformsIterator",
                "additionalDataSize",
                "setAdditionalData",
                "mainLicenseIdsIterator",
                "setBinaryDownloadurl",
                "subscribersIterator",
                "otherLicenseIdsIterator",
                "otherLicenseIdsSize",
                "setOtherLicenseIds",
                "setModifiedOn",
                "setModifiedBy",
                "modifiedBy",
                "setComponentType",
                "packageIdsSize",
                "setPackageIds",
                "packageIdsIterator"
        })
        static abstract class ReleaseMixin extends Release {
            @Override
            @JsonProperty("eccInformation")
            abstract public EccInformation getEccInformation();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "nodeId",
                "parentNodeId",
                "componentType",
                "licenseNames",
                "comment",
                "otherLicenseIds",
                "attachmentsSize",
                "setName",
                "setVersion",
                "setComponentType",
                "attachmentsIterator",
                "setAttachments",
                "setMainlineState",
                "setClearingState",
                "otherLicenseIdsSize",
                "otherLicenseIdsIterator",
                "setOtherLicenseIds",
                "setVendor",
                "setComment",
                "setNodeId",
                "setParentNodeId",
                "setLongName",
                "setReleaseRelationship",
                "setHasSubreleases",
                "licenseIdsSize",
                "licenseIdsIterator",
                "setLicenseIds",
                "licenseNamesSize",
                "licenseNamesIterator",
                "setLicenseNames",
                "setAccessible",
                "setId",
                "setClearingReport"

        })
        static abstract class ReleaseLinkMixin {

        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "attachmentsSize",
                "setAttachments",
                "setRevision",
                "attachmentsIterator",
                "setId",
                "setClearingReportStatus"

        })
        static abstract class ClearingReportMixin {

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
                "setCheckStatus",
                "setSuperAttachmentId",
                "setSuperAttachmentFilename"
        })
        static abstract class AttachmentMixin extends Attachment {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setAttachmentContentId",
                "setFilename",
                "setSha1",
                "setAttachmentType",
                "setCreatedBy",
                "setCreatedTeam",
                "setCreatedComment",
                "setCreatedOn",
                "setCheckedBy",
                "setCheckedTeam",
                "setCheckedComment",
                "setCheckedOn",
                "uploadHistorySize",
                "uploadHistoryIterator",
                "setUploadHistory",
                "setCheckStatus",
                "setSuperAttachmentId",
                "setSuperAttachmentFilename",
                "setUsageAttachment"
        })
        static abstract class AttachmentDTOMixin extends AttachmentDTO {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "setVisible",
                "setRestricted",
                "projectNameSize",
                "projectNameIterator",
                "setProjectName",
                "setRevision",
                "setType",
                "setId",
                "projectUsagesSize",
                "projectUsagesIterator",
                "setProjectUsages",
        })
        static abstract class UsageAttachmentMixin {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "setRevision",
                "setType",
                "setProjectId",
                "setId",
                "setProjectName"
        })
        static abstract class ProjectUsageMixin {
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
                "setUrl",
                "fullnameIsSet",
                "permissionsIsSet",
                "typeIsSet",
                "revisionIsSet",
                "idIsSet",
                "shortnameIsSet",
                "urlIsSet"
        })
        static abstract class VendorMixin extends Vendor {
            @Override
            @JsonProperty("fullName")
            @Schema(description = "The full name of the vendor")
            abstract public String getFullname();

            @Override
            @JsonProperty("shortName")
            @Schema(description = "The Short Name of the vendor")
            abstract public String getShortname();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "licenseType",
                "licenseTypeDatabaseId",
                "externalLicenseLink",
                "reviewdate",
                "obligations",
                "obligationDatabaseIds",
                "obligationListId",
                "osiapproved",
                "fsflibre",
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
                "obligationDatabaseIdsSize",
                "obligationDatabaseIdsIterator",
                "setObligationDatabaseIds",
                "setObligationListId",
                "setPermissions",
                "setFullname",
                "setShortname",
                "setLicenseType",
                "setOSIApproved",
                "setFSFLibre",
                "setReviewdate",
                "obligationsSize",
                "obligationsIterator",
                "setObligations",
                "risksSize",
                "risksIterator",
                "setText",
                "mainLicenseIdsIterator",
                "setChecked",
                "additionalDataSize",
                "setAdditionalData",
                "setNote",
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
                "id",
                "revision",
                "whitelist",
                "whitelistSize",
                "whitelistIterator",
                "development",
                "distribution",
                "customPropertyToValueSize",
                "developmentString",
                "distributionString",
                "externalIds",
                "externalIdsSize",
                "comments",
                "additionalData",
                "additionalDataSize",
                "node",
                "setId",
                "setRevision",
                "setType",
                "setText",
                "setWhitelist",
                "setDevelopment",
                "setDistribution",
                "setTitle",
                "setCustomPropertyToValue",
                "setDevelopmentString",
                "setDistributionString",
                "setExternalIds",
                "setComments",
                "setObligationLevel",
                "setObligationType",
                "setAdditionalData",
                "setNode"
        })
        static abstract class ObligationMixin extends Obligation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "publishDate",
                "lastExternalUpdate",
                "impact",
                "legalNotice",
                "cveReferences",
                "references",
                "intComponentId",
                "intComponentName",
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
                "assignedExtComponentIdsIterator",
                "vendorAdvisoriesIterator",
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
                "cveReferencesIterator",
                "setProjectRelevance",
                "setComment"
        })
        static abstract class VulnerabilityDTOMixin extends VulnerabilityDTO {
            @Override
            @JsonProperty("id")
            abstract public String getId();

            @Override
            @JsonProperty("intReleaseId")
            abstract public String getIntReleaseId();

            @Override
            @JsonProperty("projectAction")
            abstract public String getAction();

            @Override
            @JsonProperty("intReleaseName")
            abstract public String getIntReleaseName();

            @Override
            @JsonProperty()
            abstract public String getExternalId();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "setType",
                "setId",
                "setRevision",
                "releaseVulnerabilityRelationDTOsIterator",
                "setReleaseVulnerabilityRelationDTOs",
                "setComment",
                "setVerificationState",
                "releaseVulnerabilityRelationDTOsSize"
        })
        static abstract class VulnerabilityStateMixin extends VulnerabilityState {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "setId",
                "setRevision",
                "setType",
                "setExternalId",
                "setReleaseName"
        })
        static abstract class ReleaseVulnerabilityRelationDTOMixin extends ReleaseVulnerabilityRelationDTO {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
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
                "assignedExtComponentIdsIterator",
                "vendorAdvisoriesIterator",
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
                "id",
                "revision",
                "type",
                "priorityToolTip",
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
                "assignedExtComponentIdsIterator",
                "vendorAdvisoriesIterator",
                "vendorAdvisoriesSize",
                "setVendorAdvisories",
                "cveFurtherMetaDataPerSourceSize",
                "setCvss",
                "setCwe",
                "releasesIterator",
                "setIsSetCvss",
                "setCvssTime",
                "setAccess",
                "accessSize",
                "releasesSize",
                "setReleases"
        })
        public static abstract class VulnerabilityApiDTOMixin extends VulnerabilityApiDTO {
            @Override
            @JsonProperty("id")
            abstract public String getId();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonIgnoreProperties({
                "setEccComment",
                "setEccn",
                "setEccStatus",
                "setAl",
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

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonIgnoreProperties({
                "id",
                "processIdInTool",
                "setProcessIdInTool",
                "setAttachmentId",
                "setAttachmentHash",
                "setId",
                "setExternalTool",
                "setProcessStatus",
                "processStepsIterator",
                "setProcessSteps",
                "processStepsSize"
        })
        static abstract class ExternalToolProcessMixin extends ExternalToolProcess {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonIgnoreProperties({
                "id",
                "linkToStep",
                "setProcessIdInTool",
                "userIdInTool",
                "userCredentialsInTool",
                "userGroupInTool",
                "setId",
                "setStepName",
                "setStepStatus",
                "setLinkToStep",
                "setStartedBy",
                "setStartedByGroup",
                "setStartedOn",
                "setProcessStepIdInTool",
                "setUserIdInTool",
                "setUserCredentialsInTool",
                "setUserGroupInTool",
                "setFinishedOn",
                "setResult"
        })
        static abstract class ExternalToolProcessStepMixin extends ExternalToolProcessStep {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setExternalSupplierID",
                "setAdditionalRequestInfo",
                "setEvaluated",
                "setProcStart",
                "setRequestID",
                "setClearingTeam",
                "setRequestorPerson",
                "setBinariesOriginalFromCommunity",
                "setBinariesSelfMade",
                "setComponentLicenseInformation",
                "setSourceCodeDelivery",
                "setSourceCodeOriginalFromCommunity",
                "setSourceCodeToolMade",
                "setSourceCodeSelfMade",
                "setSourceCodeCotsAvailable",
                "setScreenshotOfWebSite",
                "setFinalizedLicenseScanReport",
                "setLicenseScanReportResult",
                "setLegalEvaluation",
                "setLicenseAgreement",
                "setScanned",
                "setComponentClearingReport",
                "setClearingStandard",
                "setReadmeOssAvailable",
                "setCountOfSecurityVn",
                "setExternalUrl",
                "setComment"
        })
        public static abstract class ClearingInformationMixin extends ClearingInformation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setUsedLicense",
                "setLicenseClearingReportURL",
                "setContainsOSS",
                "setOssContractSigned",
                "setOssInformationURL",
                "setUsageRightAvailable",
                "setCotsResponsible",
                "setClearingDeadline",
                "setSourceCodeAvailable"
        })
        public static abstract class COTSDetailsMixin extends COTSDetails {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setUrl",
                "setRepositorytype"
        })
        public static abstract class RepositoryMixin extends Repository {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "score",
                "details",
                "setScore",
                "detailsSize",
                "detailsIterator",
                "setDetails",
                "setId",
                "setType",
                "setName"
        })
        public static abstract class SearchResultMixin extends SearchResult {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "dbName",
                "setId",
                "setType",
                "setDocumentId",
                "setParentDocId",
                "setDbName",
                "changesSize",
                "changesIterator",
                "setChanges",
                "setOperation",
                "setUserEdited",
                "setChangeTimestamp",
                "referenceDocSize",
                "referenceDocIterator",
                "setReferenceDoc",
                "infoSize",
                "setInfo",
                "setRevision",
                "setDocumentType"
        })
        @JsonRootName(value = "changeLog")
        public static abstract class ChangeLogsMixin extends ChangeLogs {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setFieldName",
                "setFieldValueOld",
                "setFieldValueNew"
        })
        public static abstract class ChangedFieldsMixin extends ChangedFields {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setDbName",
                "setRefDocId",
                "setRefDocType",
                "setRefDocOperation",
                "dbName"
        })
        public static abstract class ReferenceDocDataMixin extends ReferenceDocData {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "setId",
                "type",
                "setType",
                "timestamp",
                "timestampOfDecision",
                "reOpenOnSize",
                "reOpenOnIterator",
                "setReOpenOn",
                "setClearingState",
                "setRevision",
                "setProjectId",
                "setClearingTeam",
                "setTimestamp",
                "setTimestampOfDecision",
                "setRequestingUser",
                "setComments",
                "setModifiedOn",
                "setRequestedClearingDate",
                "setProjectBU",
                "setRequestingUserComment",
                "setAgreedClearingDate",
                "commentsIterator",
                "modifiedOn",
                "commentsSize",
                "setPriority"
        })
        @JsonRootName(value = "clearingRequest")
        public static abstract class ClearingRequestMixin extends ClearingRequest {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setText",
                "setCommentedBy",
                "commentedOn",
                "setCommentedOn",
                "autoGenerated",
                "setAutoGenerated"
        })
        public static abstract class CommentMixin extends Comment {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setComment",
                "setMainlineState",
                "setReleaseRelation",
                "setCreatedOn",
                "setCreatedBy",
                "setSpdxId"
        })
        public static abstract class ProjectReleaseRelationshipMixin extends ProjectReleaseRelationship {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setId",
                "setType",
                "setRevision",
                "setYear",
                "setNumber",
                "type"
        })
        public static abstract class CVEReferenceMixin extends CVEReference {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setId",
                "setType",
                "setRevision",
                "setName",
                "setVendor",
                "setUrl",
                "type"
        })
        public static abstract class VendorAdvisoryMixin extends VendorAdvisory {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "id",
                "type",
                "setType",
                "setId",
                "setRevision",
                "setLastExternalUpdate",
                "referencesSize",
                "setCveReferences",
                "cveReferencesSize",
                "setDescription",
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
                "assignedExtComponentIdsIterator",
                "vendorAdvisoriesIterator",
                "vendorAdvisoriesSize",
                "setVendorAdvisories",
                "cveFurtherMetaDataPerSourceSize",
                "setCvss",
                "setCwe",
                "setIsSetCvss",
                "setCvssTime",
                "setAccess",
                "accessSize",
                "isSetCvss"
        })
        public static abstract class VulnerabilityMixinForCreateUpdate extends Vulnerability {
            @Override
            @JsonProperty("id")
            abstract public String getId();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "setId",
                "setRevision",
                "setType",
                "setVerificationStateInfo",
                "setVulnerabilityId",
                "verificationStateInfoSize",
                "verificationStateInfoIterator",
                "setMatchedBy",
                "setUsedNeedle",
                "setReleaseId",
                "setSpdxId"
        })
        public static abstract class ReleaseVulnerabilityRelationMixin extends ReleaseVulnerabilityRelation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setCheckedBy",
                "setCheckedOn",
                "setComment",
                "setVerificationState"
        })
        public static abstract class VerificationStateInfoMixin extends VerificationStateInfo {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setEnableSvm",
                "setProjectRelationship",
                "setSpdxId"
        })
        public static abstract class ProjectProjectRelationshipMixin extends ProjectProjectRelationship {
        }

        /*
        todo: get SPDX document changes in moderation requests
         */
        @JsonIgnoreProperties({
                "setId",
                "setModerationState",
                "setComponentType",
                "setModerators",
                "setRevision",
                "setType",
                "setTimestamp",
                "setTimestampOfDecision",
                "setDocumentId",
                "setDocumentType",
                "setRequestingUser",
                "setDocumentName",
                "setReviewer",
                "setRequestDocumentDelete",
                "setRequestingUserDepartment",
                "setCommentRequestingUser",
                "setCommentDecisionModerator",
                "setComponentAdditions",
                "setReleaseAdditions",
                "setProjectAdditions",
                "setLicenseAdditions",
                "setUser",
                "setComponentDeletions",
                "setReleaseDeletions",
                "setProjectDeletions",
                "setLicenseDeletions",
                "setSPDXDocumentAdditions",
                "setSPDXDocumentDeletions",
                "setDocumentCreationInfoAdditions",
                "setDocumentCreationInfoDeletions",
                "setPackageInfoAdditions",
                "setPackageInfoDeletions",
                "type",
                "moderatorsIterator",
                "SPDXDocumentAdditions",
                "SPDXDocumentDeletions",
                "documentCreationInfoAdditions",
                "documentCreationInfoDeletions",
                "packageInfoAdditions",
                "packageInfoDeletions",
                "spdxdocumentAdditions",
                "spdxdocumentDeletions"
        })
        @JsonRootName(value = "moderationRequest")
        public static abstract class ModerationRequestMixin extends ModerationRequest {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        static abstract class EmbeddedModerationRequestMixin extends ModerationRequestMixin {
            @Override
            @JsonIgnore
            abstract public boolean isRequestDocumentDelete();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "requestStatus",
                "isComponentDuplicate",
                "isReleaseDuplicate",
                "version",
                "setVersion",
                "setIsComponentDuplicate",
                "setIsReleaseDuplicate",
                "setComponentsName",
                "setReleasesName",
                "setMessage",
                "setRequestStatus"
        })
        public static abstract class ImportBomRequestPreparationMixin extends ImportBomRequestPreparation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static abstract class ModerationPatchMixin extends ModerationPatch {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "type",
                "attachments",
                "createdBy",
                "visbility",
                "clearingTeam",
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
                "setDomain",
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
                "setClearingTeam",
                "setPreevaluationDeadline",
                "setSystemTestStart",
                "setClearingSummary",
                "setObligationsText",
                "setSpecialRisksOSS",
                "setGeneralRisks3rdParty",
                "setSpecialRisks3rdParty",
                "setDeliveryChannels",
                "setRemarksAdditionalRequirements",
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
                "setEnableSvm",
                "setEnableVulnerabilitiesDisplay",
                "setConsiderReleasesFromExternalList",
                "additionalDataSize",
                "setAdditionalData",
                "setLinkedObligationId",
                "linkedObligationId",
                "setClearingRequestId",
                "externalUrlsSize",
                "setExternalUrls",
                "externalUrls",
                "setVendor",
                "setVendorId",
                "setDependencyNetwork",
                "dependencyNetworkSize",
                "dependencyNetworkIterator"
        })
        public abstract static class ProjectDTOMixin extends ProjectDTO {
            @Override
            @JsonProperty("projectType")
            public abstract ProjectType getProjectType();

            @Override
            @JsonSerialize(using = JsonProjectRelationSerializer.class)
            @JsonProperty("linkedProjects")
            public abstract Map<String, ProjectProjectRelationship> getLinkedProjects();

            @Override
            @JsonProperty("visibility")
            public abstract Visibility getVisbility();

            @Override
            @JsonProperty("id")
            public abstract String getId();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            public abstract Set<String> getContributors();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            public abstract Set<String> getModerators();

            @Override
            @JsonProperty(access = Access.WRITE_ONLY)
            public abstract String getLeadArchitect();

            @Override
            @JsonProperty(access = Access.READ_ONLY)
            public abstract String getClearingRequestId();
        }

        abstract static class EmbeddedProjectDTOMixin extends ProjectDTOMixin {
            @Override
            @JsonIgnore
            public abstract boolean isEnableSvm();

            @Override
            @JsonIgnore
            public abstract boolean isEnableVulnerabilitiesDisplay();

            @Override
            @JsonIgnore
            public abstract ProjectState getState();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setMainlineState",
                "setComment",
                "setCreateOn",
                "setCreateBy",
                "setReleaseId",
                "releaseLinkSize",
                "releaseLinkIterator",
                "setReleaseLink",
                "setReleaseRelationship"
        })
        public abstract static class ReleaseNodeMixin extends ReleaseNode {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setProjects",
                "setComponents"
        })
        public abstract static class RestrictedResourceMixin extends RestrictedResource {
        }
    }
}
