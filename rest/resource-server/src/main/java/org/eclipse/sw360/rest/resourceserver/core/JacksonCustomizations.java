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
import org.eclipse.sw360.datahandler.thrift.attachments.ProjectAttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.rest.common.XssPreventionModule;
import org.eclipse.sw360.rest.resourceserver.component.ComponentMergeSelector;
import org.eclipse.sw360.rest.resourceserver.core.serializer.Json3ProjectRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.Json3ReleaseRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonProjectRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonReleaseRelationSerializer;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.EmbeddedModerationRequest;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.ModerationPatch;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProject;
import org.eclipse.sw360.rest.resourceserver.project.EmbeddedProjectDTO;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseMergeSelector;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonCustomizations {
    @Bean
    public Module sw360Module() {
        return new Sw360Module();
    }

    @Bean
    public Module xssPreventionModule() {
        return new XssPreventionModule();
    }

    /**
     * Provide a Jackson 2.x ObjectMapper with all SW360 modules registered.
     * This ensures backward compatibility for any code directly using ObjectMapper.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(@NotNull List<Module> modules) {
        ObjectMapper mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .build();
        for (Module module : modules) {
            mapper.registerModule(module);
        }
        return mapper;
    }

    /**
     * Customize the Jackson 3.x {@code JsonMapper} used by Spring Boot 4,
     * Spring HATEOAS 3.x, and Spring Data REST 5.x for HTTP message
     * conversion and HAL serialization.
     * <p>
     * Spring Boot 4 defaults to Jackson 3.x ({@code tools.jackson}) for all
     * JSON handling. SW360 uses 60+ Jackson 2.x mixins with annotations like
     * {@code @JsonIgnoreProperties} and {@code @JsonProperty} to control
     * Thrift object serialization. Jackson 3.x's annotation introspector can
     * read Jackson 2.x annotations ({@code com.fasterxml.jackson.annotation.*}),
     * so we register the same mixin classes here.
     * <p>
     * This customizer also registers the Jackson 3.x XSS prevention
     * deserializer (equivalent to the Jackson 2.x {@link XssPreventionModule}).
     * <p>
     * Without this customizer, REST API responses expose raw Thrift internal
     * fields ({@code setXxx}, {@code xxxIsSet}, {@code xxxIterator}, etc.)
     * and lose {@code @JsonProperty} renames (e.g., {@code fullname} instead
     * of {@code fullName}).
     */
    @Bean
    public JsonMapperBuilderCustomizer sw360JsonMapperCustomizer() {
        return builder -> {
            registerMixIns(builder::addMixIn);
            // Register Jackson 3.x XSS prevention module
            tools.jackson.databind.module.SimpleModule xss3Module =
                    new tools.jackson.databind.module.SimpleModule("Xss3PreventionModule");
            xss3Module.addDeserializer(String.class, new Xss3StringDeserializer());
            builder.addModule(xss3Module);
            // Preserve property insertion order (same as Jackson 2.x default)
            builder.configure(tools.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);
            builder.configure(tools.jackson.databind.MapperFeature.SORT_CREATOR_PROPERTIES_FIRST, true);
        };
    }

    /**
     * Central registration of all Thrift class mixins. Used by both the
     * Jackson 2.x {@link Sw360Module} and the Jackson 3.x
     * {@link JsonMapperBuilderCustomizer} to ensure consistency.
     */
    static void registerMixIns(java.util.function.BiConsumer<Class<?>, Class<?>> registrar) {
        registrar.accept(MultiStatus.class, Sw360Module.MultiStatusMixin.class);
        registrar.accept(Project.class, Sw360Module.ProjectMixin.class);
        registrar.accept(User.class, Sw360Module.UserMixin.class);
        registrar.accept(Component.class, Sw360Module.ComponentMixin.class);
        registrar.accept(ComponentDTO.class, Sw360Module.ComponentDTOMixin.class);
        registrar.accept(ComponentMergeSelector.class, Sw360Module.ComponentMergeSelectorMixin.class);
        registrar.accept(Package.class, Sw360Module.PackageMixin.class);
        registrar.accept(Release.class, Sw360Module.ReleaseMixin.class);
        registrar.accept(SPDXDocument.class, Sw360Module.SPDXDocumentMixin.class);
        registrar.accept(DocumentCreationInformation.class, Sw360Module.DocumentCreationInformationMixin.class);
        registrar.accept(PackageInformation.class, Sw360Module.PackageInformationMixin.class);
        registrar.accept(CheckSum.class, Sw360Module.CheckSumMixin.class);
        registrar.accept(ExternalDocumentReferences.class, Sw360Module.ExternalDocumentReferencesMixin.class);
        registrar.accept(Creator.class, Sw360Module.CreatorMixin.class);
        registrar.accept(PackageVerificationCode.class, Sw360Module.PackageVerificationCodeMixin.class);
        registrar.accept(ExternalReference.class, Sw360Module.ExternalReferenceMixin.class);
        registrar.accept(SnippetRange.class, Sw360Module.SnippetRangeMixin.class);
        registrar.accept(Annotations.class, Sw360Module.AnnotationsMixin.class);
        registrar.accept(RelationshipsBetweenSPDXElements.class, Sw360Module.RelationshipsBetweenSPDXElementsMixin.class);
        registrar.accept(SnippetInformation.class, Sw360Module.SnippetInformationMixin.class);
        registrar.accept(OtherLicensingInformationDetected.class, Sw360Module.OtherLicensingInformationDetectedMixin.class);
        registrar.accept(ReleaseLink.class, Sw360Module.ReleaseLinkMixin.class);
        registrar.accept(ClearingReport.class, Sw360Module.ClearingReportMixin.class);
        registrar.accept(Attachment.class, Sw360Module.AttachmentMixin.class);
        registrar.accept(ProjectAttachmentUsage.class, Sw360Module.ProjectAttachmentUsageMixin.class);
        registrar.accept(ProjectUsage.class, Sw360Module.ProjectUsageMixin.class);
        registrar.accept(Vendor.class, Sw360Module.VendorMixin.class);
        registrar.accept(License.class, Sw360Module.LicenseMixin.class);
        registrar.accept(LicenseType.class, Sw360Module.LicenseTypeMixin.class);
        registrar.accept(Obligation.class, Sw360Module.ObligationMixin.class);
        registrar.accept(ObligationNode.class, Sw360Module.ObligationNodeMixin.class);
        registrar.accept(Vulnerability.class, Sw360Module.VulnerabilityMixin.class);
        registrar.accept(VulnerabilityState.class, Sw360Module.VulnerabilityStateMixin.class);
        registrar.accept(ReleaseVulnerabilityRelationDTO.class, Sw360Module.ReleaseVulnerabilityRelationDTOMixin.class);
        registrar.accept(VulnerabilityDTO.class, Sw360Module.VulnerabilityDTOMixin.class);
        registrar.accept(VulnerabilityApiDTO.class, Sw360Module.VulnerabilityApiDTOMixin.class);
        registrar.accept(VulnerabilitySummary.class, Sw360Module.VulnerabilitySummMixin.class);
        registrar.accept(EccInformation.class, Sw360Module.EccInformationMixin.class);
        registrar.accept(EmbeddedProject.class, Sw360Module.EmbeddedProjectMixin.class);
        registrar.accept(ExternalToolProcess.class, Sw360Module.ExternalToolProcessMixin.class);
        registrar.accept(ExternalToolProcessStep.class, Sw360Module.ExternalToolProcessStepMixin.class);
        registrar.accept(COTSDetails.class, Sw360Module.COTSDetailsMixin.class);
        registrar.accept(ClearingInformation.class, Sw360Module.ClearingInformationMixin.class);
        registrar.accept(Repository.class, Sw360Module.RepositoryMixin.class);
        registrar.accept(SearchResult.class, Sw360Module.SearchResultMixin.class);
        registrar.accept(ChangeLogs.class, Sw360Module.ChangeLogsMixin.class);
        registrar.accept(ChangedFields.class, Sw360Module.ChangedFieldsMixin.class);
        registrar.accept(ReferenceDocData.class, Sw360Module.ReferenceDocDataMixin.class);
        registrar.accept(ClearingRequest.class, Sw360Module.ClearingRequestMixin.class);
        registrar.accept(Comment.class, Sw360Module.CommentMixin.class);
        registrar.accept(ProjectReleaseRelationship.class, Sw360Module.ProjectReleaseRelationshipMixin.class);
        registrar.accept(ObligationStatusInfo.class, Sw360Module.ObligationStatusInfoMixin.class);
        registrar.accept(ReleaseVulnerabilityRelation.class, Sw360Module.ReleaseVulnerabilityRelationMixin.class);
        registrar.accept(VerificationStateInfo.class, Sw360Module.VerificationStateInfoMixin.class);
        registrar.accept(ProjectProjectRelationship.class, Sw360Module.ProjectProjectRelationshipMixin.class);
        registrar.accept(ModerationRequest.class, Sw360Module.ModerationRequestMixin.class);
        registrar.accept(EmbeddedModerationRequest.class, Sw360Module.EmbeddedModerationRequestMixin.class);
        registrar.accept(ImportBomRequestPreparation.class, Sw360Module.ImportBomRequestPreparationMixin.class);
        registrar.accept(ModerationPatch.class, Sw360Module.ModerationPatchMixin.class);
        registrar.accept(ProjectDTO.class, Sw360Module.ProjectDTOMixin.class);
        registrar.accept(EmbeddedProjectDTO.class, Sw360Module.EmbeddedProjectDTOMixin.class);
        registrar.accept(ReleaseNode.class, Sw360Module.ReleaseNodeMixin.class);
        registrar.accept(RestrictedResource.class, Sw360Module.RestrictedResourceMixin.class);
        registrar.accept(RestApiToken.class, Sw360Module.RestApiTokenMixin.class);
        registrar.accept(ProjectLink.class, Sw360Module.ProjectLinkMixin.class);
        registrar.accept(BulkOperationNode.class, Sw360Module.BulkOperationNodeMixin.class);
        registrar.accept(ReleaseMergeSelector.class, Sw360Module.ReleaseMergeSelectorMixin.class);
        registrar.accept(ProjectPackageRelationship.class, Sw360Module.ProjectPackageRelationshipMixin.class);
        registrar.accept(VendorAdvisory.class, Sw360Module.VendorAdvisoryMixin.class);
        registrar.accept(RequestSummary.class, Sw360Module.RequestSummaryMixin.class);
    }


    @SuppressWarnings("serial")
    public static class Sw360Module extends SimpleModule {
        public Sw360Module() {
            registerMixIns(this::setMixInAnnotation);

            // Make SpringDoc aware of the mixins
            registerMixIns(SpringDocUtils.getConfig()::replaceWithClass);
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        static abstract class MultiStatusMixin extends MultiStatus {
            @Override
            @JsonProperty("status")
            abstract public int getStatusCode();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "attachments",
                "createdBy",
                "visbility",
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
                "packageIdsSize",
                "setPackageIds",
                "packageIdsIterator",
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
            @tools.jackson.databind.annotation.JsonSerialize(using = Json3ProjectRelationSerializer.class)
            @JsonProperty("linkedProjects")
            abstract public Map<String, ProjectProjectRelationship> getLinkedProjects();

            @Override
            @JsonSerialize(using = JsonReleaseRelationSerializer.class)
            @tools.jackson.databind.annotation.JsonSerialize(using = Json3ReleaseRelationSerializer.class)
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
            abstract public ProjectState getState();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "id",
                "revision",
                "setPassword",
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
                "createdByIsSet",
                "createdOnIsSet",
                "componentTypeIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "subscribersIsSet",
                "rolesIsSet",
                "mainLicenseIdsIsSet",
                "languagesIsSet",
                "operatingSystemsIsSet",
                "softwarePlatformsIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet",
                "componentOwnerIsSet",
                "ownerAccountingUnitIsSet",
                "ownerGroupIsSet",
                "ownerCountryIsSet",
                "setVisbility",
                "visbilityIsSet",
                "setBusinessUnit",
                "businessUnitIsSet",
                "cdxComponentTypeIsSet",
                "releasesIsSet",
                "releaseIdsIsSet",
                "defaultVendorIsSet",
                "defaultVendorIdIsSet",
                "categoriesIsSet",
                "vendorNamesIsSet",
                "mailinglistIsSet",
                "wikiIsSet",
                "blogIsSet",
                "wikipediaIsSet",
                "openHubIsSet",
                "vcsIsSet",
                "nameIsSet",
                "homepageIsSet",
                "descriptionIsSet"
        })
        static abstract class ComponentMixin extends Component {
            @Override
            @JsonProperty(PropertyKeyMapping.COMPONENT_VENDOR_KEY_JSON)
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "revision",
                "type",
                "licenseIdsSize",
                "licenseIdsIterator",
                "createdBy",
                "comment",
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
                "setRelease",
                "setComment",
                "createdByIsSet",
                "createdOnIsSet",
                "versionIsSet",
                "vendorIsSet",
                "vendorIdIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "releaseIdIsSet",
                "idIsSet",
                "revisionIsSet",
                "vcsIsSet",
                "nameIsSet",
                "descriptionIsSet",
                "releaseIsSet",
                "licenseIdsIsSet",
                "purlIsSet",
                "setPackageType",
                "packageTypeIsSet",
                "homepageUrlIsSet",
                "hashIsSet",
                "packageManagerIsSet",
                "commentIsSet",
                "typeIsSet"
        })
        static abstract class PackageMixin extends Package {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "type",
                "revision",
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
                "visbility",
                "releaseIdsSize",
                "setMainLicenseIds",
                "createdByIsSet",
                "createdOnIsSet",
                "componentTypeIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "subscribersIsSet",
                "rolesIsSet",
                "mainLicenseIdsSize",
                "mainLicenseIdsIterator",
                "mainLicenseIdsIsSet",
                "languagesSize",
                "languagesIsSet",
                "operatingSystemsIsSet",
                "softwarePlatformsIsSet",
                "setModifiedBy",
                "modifiedByIsSet",
                "setModifiedOn",
                "modifiedOnIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet",
                "componentOwnerIsSet",
                "ownerAccountingUnitIsSet",
                "ownerGroupIsSet",
                "ownerCountryIsSet",
                "visbilityIsSet",
                "businessUnitIsSet",
                "cdxComponentTypeIsSet",
                "releasesSize",
                "releasesIterator",
                "setReleases",
                "releasesIsSet",
                "releaseIdsIterator",
                "setReleaseIds",
                "releaseIdsIsSet",
                "defaultVendorIsSet",
                "defaultVendorIdIsSet",
                "categoriesIsSet",
                "vendorNamesIsSet",
                "mailinglistIsSet",
                "wikiIsSet",
                "blogIsSet",
                "wikipediaIsSet",
                "openHubIsSet",
                "setVcs",
                "vcsIsSet",
                "nameIsSet",
                "homepageIsSet",
                "descriptionIsSet"
        })
        static abstract class ComponentDTOMixin extends Component {
            @Override
            @JsonProperty(PropertyKeyMapping.COMPONENT_VENDOR_KEY_JSON)
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "id",
                "type",
                "revision",
                "releases",
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
                "createdByIsSet",
                "createdOnIsSet",
                "componentTypeIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "subscribersIsSet",
                "rolesIsSet",
                "mainLicenseIdsIsSet",
                "modifiedOn",
                "languagesIsSet",
                "operatingSystemsIsSet",
                "softwarePlatformsIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet",
                "componentOwnerIsSet",
                "ownerAccountingUnitIsSet",
                "ownerGroupIsSet",
                "ownerCountryIsSet",
                "setVisbility",
                "visbilityIsSet",
                "setBusinessUnit",
                "businessUnitIsSet",
                "cdxComponentTypeIsSet",
                "releasesIsSet",
                "releaseIdsIsSet",
                "defaultVendorIsSet",
                "defaultVendorIdIsSet",
                "categoriesIsSet",
                "vendorNamesIsSet",
                "mailinglistIsSet",
                "wikiIsSet",
                "blogIsSet",
                "wikipediaIsSet",
                "openHubIsSet",
                "vcsIsSet",
                "nameIsSet",
                "homepageIsSet",
                "descriptionIsSet"
        })
        static abstract class ComponentMergeSelectorMixin extends ComponentMergeSelector {
            @Override
            @JsonProperty(PropertyKeyMapping.COMPONENT_VENDOR_KEY_JSON)
            abstract public Set<String> getVendorNames();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setComment",
                "setCreatedBy",
                "setCreatedOn"
        })
        public static abstract class ProjectPackageRelationshipMixin extends ProjectPackageRelationship {
        }

        @JsonIgnoreProperties({
                "setTotalAffectedElements",
                "setTotalElements",
                "setMessage",
                "setRequestStatus"
        })
        public static abstract class RequestSummaryMixin extends RequestSummary {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
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
                "documentState",
                "contributorsIterator",
                "rolesSize",
                "setRoles",
                "setCreatorDepartment",
                "setProjectMainlineState",
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
                "packageIdsIterator",
                "createdByIsSet",
                "createdOnIsSet",
                "releaseDateIsSet",
                "cpeidIsSet",
                "versionIsSet",
                "componentIdIsSet",
                "componentTypeIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "repositoryIsSet",
                "mainlineStateIsSet",
                "clearingStateIsSet",
                "externalToolProcessesIterator",
                "externalToolProcessesIsSet",
                "creatorDepartmentIsSet",
                "projectMainlineStateIsSet",
                "contributorsIsSet",
                "subscribersIsSet",
                "rolesIsSet",
                "mainLicenseIdsIsSet",
                "otherLicenseIdsIsSet",
                "vendorIsSet",
                "vendorIdIsSet",
                "clearingInformationIsSet",
                "languagesIsSet",
                "operatingSystemsIsSet",
                "cotsDetailsIsSet",
                "eccInformationIsSet",
                "softwarePlatformsIsSet",
                "sourceCodeDownloadurlIsSet",
                "binaryDownloadurlIsSet",
                "releaseIdToRelationshipIsSet",
                "packageIdsIsSet",
                "spdxIdIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet",
                "nameIsSet"
        })
        static abstract class ReleaseMixin extends Release {
            @Override
            @JsonProperty("eccInformation")
            abstract public EccInformation getEccInformation();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
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
                "documentState",
                "contributorsIterator",
                "rolesSize",
                "setRoles",
                "setCreatorDepartment",
                "setProjectMainlineState",
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
                "packageIdsIterator",
                "createdByIsSet",
                "createdOnIsSet",
                "releaseDateIsSet",
                "cpeidIsSet",
                "versionIsSet",
                "componentIdIsSet",
                "componentTypeIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "attachmentsIsSet",
                "repositoryIsSet",
                "mainlineStateIsSet",
                "clearingStateIsSet",
                "externalToolProcessesIterator",
                "externalToolProcessesIsSet",
                "creatorDepartmentIsSet",
                "projectMainlineStateIsSet",
                "contributorsIsSet",
                "subscribersIsSet",
                "rolesIsSet",
                "mainLicenseIdsIsSet",
                "otherLicenseIdsIsSet",
                "vendorIsSet",
                "vendorIdIsSet",
                "clearingInformationIsSet",
                "languagesIsSet",
                "operatingSystemsIsSet",
                "cotsDetailsIsSet",
                "eccInformationIsSet",
                "softwarePlatformsIsSet",
                "sourceCodeDownloadurlIsSet",
                "binaryDownloadurlIsSet",
                "releaseIdToRelationshipIsSet",
                "packageIdsIsSet",
                "spdxIdIsSet",
                "modifiedByIsSet",
                "modifiedOnIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "componentId",
                "documentStateIsSet",
                "permissionsIsSet",
                "moderatorsIsSet",
                "nameIsSet"
        })
        static abstract class ReleaseMergeSelectorMixin extends ReleaseMergeSelector {
            @Override
            @JsonProperty("eccInformation")
            abstract public EccInformation getEccInformation();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "permissions",
                "setType",
                "setId",
                "setRevision",
                "setCreatedBy",
                "moderatorsIterator",
                "setModerators",
                "setDocumentState",
                "setPermissions",
                "setReleaseId",
                "setSpdxDocumentCreationInfoId",
                "setSpdxPackageInfoIds",
                "spdxFileInfoIdsIterator",
                "setSpdxFileInfoIds",
                "setSnippets",
                "relationshipsIterator",
                "setRelationships",
                "annotationsIterator",
                "setAnnotations",
                "snippetsIterator",
                "spdxPackageInfoIdsIterator",
                "otherLicensingInformationDetectedsIterator",
                "setOtherLicensingInformationDetecteds",
                "moderatorsSize",
                "permissionsSize",
                "spdxPackageInfoIdsSize",
                "spdxFileInfoIdsSize",
                "snippetsSize",
                "relationshipsSize",
                "annotationsSize",
                "otherLicensingInformationDetectedsSize"
        })
        static abstract class SPDXDocumentMixin extends SPDXDocument {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "permissions",
                "setType",
                "setId",
                "setName",
                "setRevision",
                "setCreatedBy",
                "moderatorsIterator",
                "setModerators",
                "setDocumentState",
                "setPermissions",
                "setSpdxDocumentId",
                "setSpdxVersion",
                "setDataLicense",
                "setSPDXID",
                "setDocumentNamespace",
                "externalDocumentRefsIterator",
                "setExternalDocumentRefs",
                "setLicenseListVersion",
                "creatorIterator",
                "setCreator",
                "setCreated",
                "setCreatorComment",
                "setDocumentComment",
                "moderatorsSize",
                "permissionsSize",
                "externalDocumentRefsSize",
                "creatorSize",
                "spdxid"
        })
        static abstract class DocumentCreationInformationMixin extends DocumentCreationInformation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "permissions",
                "index",
                "setType",
                "setId",
                "setName",
                "setRevision",
                "setDescription",
                "setCreatedBy",
                "moderatorsIterator",
                "setModerators",
                "setReleaseDate",
                "setHomepage",
                "setDocumentState",
                "setPermissions",
                "relationshipsIterator",
                "setRelationships",
                "annotationsIterator",
                "setAnnotations",
                "setSpdxDocumentId",
                "setSPDXID",
                "setVersionInfo",
                "setPackageFileName",
                "setSupplier",
                "setOriginator",
                "setDownloadLocation",
                "setFilesAnalyzed",
                "setPackageVerificationCode",
                "checksumsSize",
                "checksumsIterator",
                "setChecksums",
                "setSourceInfo",
                "setLicenseConcluded",
                "licenseInfoFromFilesIterator",
                "setLicenseInfoFromFiles",
                "setLicenseDeclared",
                "setLicenseComments",
                "setCopyrightText",
                "setSummary",
                "setPackageComment",
                "externalRefsSize",
                "externalRefsIterator",
                "setExternalRefs",
                "setAttributionText",
                "setPrimaryPackagePurpose",
                "setBuiltDate",
                "setValidUntilDate",
                "setIndex",
                "attributionTextIterator",
                "moderatorsSize",
                "permissionsSize",
                "relationshipsSize",
                "annotationsSize",
                "licenseInfoFromFilesSize",
                "attributionTextSize",
                "spdxid"
        })
        static abstract class PackageInformationMixin extends PackageInformation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setAlgorithm",
                "setChecksumValue",
                "setIndex"
        })
        public static abstract class CheckSumMixin extends CheckSum {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setAnnotator",
                "setAnnotationDate",
                "setAnnotationType",
                "setSpdxIdRef",
                "setAnnotationComment",
                "setIndex"
        })
        public static abstract class AnnotationsMixin extends Annotations {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setExternalDocumentId",
                "setChecksum",
                "setSpdxDocument",
                "setIndex"
        })
        public static abstract class ExternalDocumentReferencesMixin extends ExternalDocumentReferences {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setType",
                "setValue",
                "setIndex"
        })
        public static abstract class CreatorMixin extends Creator {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setLicenseId",
                "setExtractedText",
                "setLicenseName",
                "setLicenseCrossRefs",
                "licenseCrossRefsSize",
                "licenseCrossRefsIterator",
                "setLicenseComment",
                "setIndex"
        })
        public static abstract class OtherLicensingInformationDetectedMixin extends OtherLicensingInformationDetected {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setExcludedFiles",
                "excludedFilesSize",
                "excludedFilesIterator",
                "setIndex",
                "setValue"
        })
        public static abstract class PackageVerificationCodeMixin extends PackageVerificationCode {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setReferenceCategory",
                "setReferenceLocator",
                "setReferenceType",
                "setComment",
                "setIndex"
        })
        public static abstract class ExternalReferenceMixin extends ExternalReference {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setSpdxElementId",
                "setRelationshipType",
                "setRelatedSpdxElement",
                "setRelationshipComment",
                "setIndex"
        })
        public static abstract class RelationshipsBetweenSPDXElementsMixin extends RelationshipsBetweenSPDXElements {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setSPDXID",
                "setSnippetFromFile",
                "setSnippetRanges",
                "snippetRangesSize",
                "snippetRangesIterator",
                "setLicenseConcluded",
                "setLicenseInfoInSnippets",
                "licenseInfoInSnippetsSize",
                "licenseInfoInSnippetsIterator",
                "setLicenseComments",
                "setCopyrightText",
                "setComment",
                "setName",
                "setSnippetAttributionText",
                "setIndex",
                "spdxid"
        })
        public static abstract class SnippetInformationMixin extends SnippetInformation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setRangeType",
                "setStartPointer",
                "setEndPointer",
                "setReference",
                "setIndex"
        })
        public static abstract class SnippetRangeMixin extends SnippetRange {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "nodeId",
                "layer",
                "parentNodeId",
                "licenseNames",
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
                "setClearingReport",
                "layer",
                "setIndex",
                "releaseWithSameComponentSize",
                "setReleaseWithSameComponent",
                "setLayer",
                "setDefaultValue",
                "setProjectId",
                "setReleaseMainLineState",
                "setComponentId",
                "setCreatedBy",
                "createdByIsSet"
        })
        static abstract class ReleaseLinkMixin extends ReleaseLink {

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
                "setSuperAttachmentFilename",
                "attachmentContentIdIsSet",
                "filenameIsSet",
                "sha1IsSet",
                "attachmentTypeIsSet",
                "createdByIsSet",
                "createdTeamIsSet",
                "createdCommentIsSet",
                "createdOnIsSet",
                "checkedByIsSet",
                "checkedTeamIsSet",
                "checkedCommentIsSet",
                "checkedOnIsSet",
                "uploadHistoryIsSet",
                "checkStatusIsSet",
                "superAttachmentIdIsSet",
                "superAttachmentFilenameIsSet",
                "setProjectAttachmentUsage"
        })
        static abstract class AttachmentMixin extends Attachment {
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
        static abstract class ProjectAttachmentUsageMixin extends ProjectAttachmentUsage {
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
        static abstract class ProjectUsageMixin extends ProjectUsage {
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
                "reviewdate",
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
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "documentStateIsSet",
                "permissionsIsSet",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "fullnameIsSet",
                "shortnameIsSet",
                "licenseTypeIsSet",
                "licenseTypeDatabaseIdIsSet",
                "externalLicenseLinkIsSet",
                "noteIsSet",
                "reviewdateIsSet",
                "osiapprovedIsSet",
                "fsflibreIsSet",
                "obligationsIsSet",
                "obligationDatabaseIdsIsSet",
                "obligationListIdIsSet",
                "textIsSet",
                "checkedIsSet",
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
                "licenseTypeId",
                "setRevision",
                "setType",
                "setLicenseType",
                "setId",
                "setLicenseTypeId",
                "setText",
                "setShortname",
                "setFullname",
                "setExternalIds",
                "setAdditionalData",
                "externalIdsSize",
                "additionalDataSize",
                "setLicenseTypeDatabaseId",
                "setExternalLicenseLink",
                "setNote",
                "setDocumentState",
                "setPermissions",
                "permissionsSize",
                "setReviewdate",
                "setOSIApproved",
                "setFSFLibre",
                "setObligations",
                "setObligationDatabaseIds",
                "setObligationListId",
                "setChecked",
                "obligationsSize",
                "obligationsIterator",
                "obligationDatabaseIdsSize",
                "obligationDatabaseIdsIterator"
        })
        static abstract class LicenseTypeMixin extends License {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
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
                "setNode",
                "externalIdsIsSet",
                "additionalDataIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "titleIsSet",
                "textIsSet",
                "whitelistIsSet",
                "developmentIsSet",
                "distributionIsSet",
                "customPropertyToValueIsSet",
                "developmentStringIsSet",
                "distributionStringIsSet",
                "commentsIsSet",
                "obligationLevelIsSet",
                "obligationTypeIsSet",
                "nodeIsSet"
        })
        static abstract class ObligationMixin extends Obligation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "type",
                "setId",
                "setRevision",
                "setType",
                "setNodeType",
                "setNodeText",
                "setOblElementId",
                "oblElementId",
        })
        static abstract class ObligationNodeMixin extends ObligationNode {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "revision",
                "type",
                "setId",
                "setRevision",
                "setLangElement",
                "setObject",
                "setAction",
                "setStatus",
                "setType",
        })
        static abstract class ObligationElementMixin extends ObligationElement {
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
                "setReleases",
                "descriptionIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "releasesIsSet",
                "externalIdIsSet",
                "titleIsSet",
                "publishDateIsSet",
                "lastExternalUpdateIsSet",
                "priorityIsSet",
                "actionIsSet",
                "impactIsSet",
                "legalNoticeIsSet",
                "cveReferencesIsSet",
                "referencesIsSet",
                "lastUpdateDateIsSet",
                "priorityTextIsSet",
                "assignedExtComponentIdsIsSet",
                "vendorAdvisoriesIsSet",
                "extendedDescriptionIsSet",
                "cvssIsSet",
                "isSetCvssIsSet",
                "cvssTimeIsSet",
                "vulnerableConfigurationIsSet",
                "accessIsSet",
                "cweIsSet",
                "cveFurtherMetaDataPerSourceIsSet"
        })
        public static abstract class VulnerabilityApiDTOMixin extends VulnerabilityApiDTO {
            @Override
            @JsonProperty("id")
            abstract public String getId();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
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
                "releaseVulnerabilityRelation",
                "setId",
                "setProjectName",
                "setRevision",
                "setType",
                "setExternalId",
                "setTitle",
                "setPublishDate",
                "setLastExternalUpdate",
                "setPriority",
                "setPriorityToolTip",
                "setAction",
                "impactSize",
                "setImpact",
                "setLegalNotice",
                "cveReferencesSize",
                "cveReferencesIterator",
                "setCveReferences",
                "referencesSize",
                "referencesIterator",
                "setReferences",
                "setIntReleaseId",
                "setIntReleaseName",
                "setIntComponentId",
                "setIntComponentName",
                "setReleaseVulnerabilityRelation",
                "setMatchedBy",
                "setUsedNeedle",
                "setProjectRelevance",
                "setComment",
                "setDescription",
        })
        public static abstract class VulnerabilitySummMixin extends VulnerabilitySummary {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonIgnoreProperties({
                "setEccComment",
                "setEccn",
                "setEccStatus",
                "setContainsCryptography",
                "setAl",
                "setAssessorContactPerson",
                "setAssessmentDate",
                "setAssessorDepartment",
                "setMaterialIndexNumber",
                "eccStatusIsSet",
                "containsCryptographyIsSet",
                "alIsSet",
                "eccnIsSet",
                "assessorContactPersonIsSet",
                "assessorDepartmentIsSet",
                "eccCommentIsSet",
                "materialIndexNumberIsSet",
                "assessmentDateIsSet",
        })
        static abstract class EccInformationMixin extends EccInformation {
            @Override
            @JsonProperty("eccStatus")
            abstract public ECCStatus getEccStatus();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setComment",
                "setText",
                "setObligationType",
                "setObligationLevel",
                "setModifiedBy",
                "setModifiedOn",
                "setId",
                "setStatus",
                "setAction",
                "setLicenseIds",
                "setReleaseIdToAcceptedCLI",
                "releaseIdToAcceptedCLISize",
                "releasesSize",
                "releasesIterator",
                "setReleases",
                "licenseIdsSize",
                "licenseIdsIterator"
        })
        public static abstract class ObligationStatusInfoMixin extends ObligationStatusInfo {
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
                "processStepsSize",
                "externalToolIsSet",
                "processStatusIsSet",
                "processIdInToolIsSet",
                "attachmentIdIsSet",
                "attachmentHashIsSet",
                "processStepsIsSet",
                "idIsSet"
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
                "setResult",
                "stepNameIsSet",
                "stepStatusIsSet",
                "linkToStepIsSet",
                "startedByIsSet",
                "startedByGroupIsSet",
                "startedOnIsSet",
                "idIsSet",
                "processStepIdInToolIsSet",
                "userIdInToolIsSet",
                "userCredentialsInToolIsSet",
                "userGroupInToolIsSet",
                "finishedOnIsSet",
                "resultIsSet"
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
                "setComment",
                "externalSupplierIDIsSet",
                "additionalRequestInfoIsSet",
                "evaluatedIsSet",
                "procStartIsSet",
                "requestIDIsSet",
                "requestorPersonIsSet",
                "binariesOriginalFromCommunityIsSet",
                "binariesSelfMadeIsSet",
                "componentLicenseInformationIsSet",
                "sourceCodeDeliveryIsSet",
                "sourceCodeOriginalFromCommunityIsSet",
                "sourceCodeToolMadeIsSet",
                "sourceCodeSelfMadeIsSet",
                "sourceCodeCotsAvailableIsSet",
                "screenshotOfWebSiteIsSet",
                "finalizedLicenseScanReportIsSet",
                "licenseScanReportResultIsSet",
                "legalEvaluationIsSet",
                "licenseAgreementIsSet",
                "scannedIsSet",
                "componentClearingReportIsSet",
                "clearingStandardIsSet",
                "readmeOssAvailableIsSet",
                "countOfSecurityVnIsSet",
                "externalUrlIsSet",
                "commentIsSet",
                "clearingTeamIsSet"
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
                "setSourceCodeAvailable",
                "usedLicenseIsSet",
                "licenseClearingReportURLIsSet",
                "containsOSSIsSet",
                "ossContractSignedIsSet",
                "ossInformationURLIsSet",
                "usageRightAvailableIsSet",
                "cotsResponsibleIsSet",
                "clearingDeadlineIsSet",
                "sourceCodeAvailableIsSet"
        })
        public static abstract class COTSDetailsMixin extends COTSDetails {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setUrl",
                "setRepositorytype",
                "urlIsSet",
                "repositorytypeIsSet"
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
                "setPriority",
                "setClearingType",
                "setClearingSize"
        })
        @JsonRootName(value = "clearingRequest")
        public static abstract class ClearingRequestMixin extends ClearingRequest {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setText",
                "setCommentedBy",
                "setCommentedOn",
                "setAutoGenerated",
                "setDateTime",
                "setUsername"
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
                "type",
                "urlIsSet",
                "nameIsSet",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "vendorIsSet"
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
                "setSpdxId",
                "idIsSet",
                "revisionIsSet",
                "typeIsSet",
                "releaseIdIsSet",
                "matchedByIsSet",
                "usedNeedleIsSet",
                "vulnerabilityIdIsSet",
                "verificationStateInfoIsSet",
                "spdxIdIsSet"
        })
        public static abstract class ReleaseVulnerabilityRelationMixin extends ReleaseVulnerabilityRelation {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setCheckedBy",
                "setCheckedOn",
                "setComment",
                "setVerificationState",
                "checkedByIsSet",
                "checkedOnIsSet",
                "commentIsSet",
                "verificationStateIsSet"
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
                "revision",
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
            @tools.jackson.databind.annotation.JsonSerialize(using = Json3ProjectRelationSerializer.class)
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
                "setReleaseRelationship",
                "setReleaseName",
                "setReleaseVersion",
                "setComponentId",
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

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "setName",
                "setCreatedOn",
                "setToken",
                "setNumberOfDaysValid",
                "authoritiesIterator",
                "authoritiesSize",
                "setAuthorities",
                "nameIsSet",
                "createdOnIsSet",
                "tokenIsSet",
                "authoritiesIsSet",
                "numberOfDaysValidIsSet"
        })
        public abstract static class RestApiTokenMixin extends RestApiToken {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties({
                "nodeId",
                "treeLevel",
                "parentNodeId",
                "setId",
                "setName",
                "setSubprojects",
                "setNodeId",
                "setParentNodeId",
                "setVersion",
                "setClearingState",
                "setState",
                "setProjectType",
                "setEnableSvm",
                "linkedReleasesSize",
                "setRelation",
                "linkedReleasesIterator",
                "setLinkedReleases",
                "subprojectsSize",
                "subprojectsIterator",
                "setTreeLevel",
        })
        abstract static class ProjectLinkMixin extends ProjectLink {}

	@JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(value = {
                "setId",
                "setName",
                "setVersion",
                "setType",
                "setParentId",
                "setChildList",
                "childListSize",
                "childListIterator",
                "setState",
                "setAdditionalData",
                "additionalDataSize"
        })
        static abstract class BulkOperationNodeMixin extends BulkOperationNode {
        }
    }
}
