/*
 * Copyright Siemens AG, 2014-2017.
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.eclipse.sw360.datahandler.services.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.services.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.services.common.ClearingReportStatus;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestSize;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestState;
import org.eclipse.sw360.datahandler.services.common.ClearingRequestType;
import org.eclipse.sw360.datahandler.services.common.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.services.common.DateRange;
import org.eclipse.sw360.datahandler.services.common.MainlineState;
import org.eclipse.sw360.datahandler.services.common.ModerationState;
import org.eclipse.sw360.datahandler.services.common.ObligationStatus;
import org.eclipse.sw360.datahandler.services.common.Quadratic;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.Ternary;
import org.eclipse.sw360.datahandler.services.common.VerificationState;
import org.eclipse.sw360.datahandler.services.common.Visibility;
import org.eclipse.sw360.datahandler.services.components.ClearingState;
import org.eclipse.sw360.datahandler.services.components.ComponentType;
import org.eclipse.sw360.datahandler.services.components.ECCStatus;
import org.eclipse.sw360.datahandler.services.components.ExternalTool;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStatus;
import org.eclipse.sw360.datahandler.services.components.RepositoryType;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationType;
import org.eclipse.sw360.datahandler.services.moderation.DocumentType;
import org.eclipse.sw360.datahandler.services.packages.PackageManager;
import org.eclipse.sw360.datahandler.services.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.services.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.services.projects.ProjectState;
import org.eclipse.sw360.datahandler.services.projects.ProjectType;
import org.eclipse.sw360.datahandler.services.users.UserAccess;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.datahandler.services.vulnerabilities.VulnerabilityAccessAuthentication;
import org.eclipse.sw360.datahandler.services.vulnerabilities.VulnerabilityAccessComplexity;
import org.eclipse.sw360.datahandler.services.vulnerabilities.VulnerabilityAccessVector;
import org.eclipse.sw360.datahandler.services.vulnerabilities.VulnerabilityImpact;
import org.eclipse.sw360.datahandler.services.vulnerabilities.VulnerabilityRatingForProject;

/**
 * Human readable labels for service-api enums, e.g. {@code UNDER_CLEARING} -> "Under clearing".
 *
 * <p>This is the single source of truth for those labels. {@link ThriftEnumUtils} is now a thin
 * shim that resolves a thrift enum to its service-api twin and delegates here, and it disappears
 * with thrift. The labels were moved rather than copied, so there is nothing to keep in sync.
 *
 * <p>Do not replace a call to this class with {@code value.name()}: the raw constant name leaks
 * into user facing output such as the project clearing status report and the Excel/CSV exports.
 */
public class EnumDisplayNames {

    private EnumDisplayNames() {
    }

    private static final ImmutableMap<ComponentType, String> MAP_COMPONENT_TYPE_STRING = ImmutableMap.<ComponentType, String>builder()
            .put(ComponentType.OSS, "OSS")
            .put(ComponentType.COTS, "COTS")
            .put(ComponentType.INTERNAL, "Internal")
            .put(ComponentType.INNER_SOURCE, "Inner Source")
            .put(ComponentType.SERVICE, "Service")
            .put(ComponentType.FREESOFTWARE, "Freeware")
            .put(ComponentType.CODE_SNIPPET, "Code Snippet")
            .put(ComponentType.COTS_TRUSTED_SUPPLIER, "COTS-Trusted Supplier")
            .build();

    private static final ImmutableMap<ObligationLevel, String> MAP_OBLIGATION_LEVEL_STRING = ImmutableMap.<ObligationLevel, String>builder()
            .put(ObligationLevel.ORGANISATION_OBLIGATION, "Organisation Obligation")
            .put(ObligationLevel.COMPONENT_OBLIGATION, "Component Obligation")
            .put(ObligationLevel.PROJECT_OBLIGATION, "Project Obligation")
            .put(ObligationLevel.LICENSE_OBLIGATION, "License Obligation")
            .build();

    private static final ImmutableMap<ObligationType, String> MAP_OBLIGATION_TYPE_STRING = ImmutableMap.<ObligationType, String>builder()
            .put(ObligationType.PERMISSION, "Permission")
            .put(ObligationType.RISK, "Risk")
            .put(ObligationType.EXCEPTION, "Exception")
            .put(ObligationType.RESTRICTION, "Restriction")
            .put(ObligationType.OBLIGATION, "Obligation")
            .build();

    private static final ImmutableMap<Quadratic,String> MAP_QUADRATIC_STRING = ImmutableMap.of(
            Quadratic.NA, "(n/a)",
            Quadratic.YES, "yes");

    private static final ImmutableMap<Ternary,String> MAP_TERNARY_STRING = ImmutableMap.of(
            Ternary.UNDEFINED, "undefined",
            Ternary.NO, "no",
            Ternary.YES, "yes");

    private static final ImmutableMap<DateRange, String> MAP_DATE_RANGE_STRING = ImmutableMap.of(
            DateRange.EQUAL, "=",
            DateRange.LESS_THAN_OR_EQUAL_TO, "<=",
            DateRange.GREATER_THAN_OR_EQUAL_TO, ">=",
            DateRange.BETWEEN, "Between");

    private static final ImmutableMap<ProjectType, String> MAP_PROJECT_TYPE_STRING = ImmutableMap.of(
            ProjectType.CUSTOMER, "Customer Project" ,
            ProjectType.INTERNAL, "Internal Project" ,
            ProjectType.PRODUCT, "Product" ,
            ProjectType.SERVICE, "Service",
            ProjectType.INNER_SOURCE, "Inner Source",
            ProjectType.CLOUD_BACKEND, "Cloud Backend");

    private static final ImmutableMap<AttachmentType, String> MAP_ATTACHMENT_TYPE_STRING = ImmutableMap.<AttachmentType, String>builder()
            .put(AttachmentType.DOCUMENT, "Document")
            .put(AttachmentType.SOURCE, "Source file")
            .put(AttachmentType.DESIGN, "Design document")
            .put(AttachmentType.REQUIREMENT, "Requirement document")
            .put(AttachmentType.CLEARING_REPORT, "Clearing report")
            .put(AttachmentType.COMPONENT_LICENSE_INFO_XML, "Component license information (XML)")
            .put(AttachmentType.COMPONENT_LICENSE_INFO_COMBINED, "Component license information (Combined)")
            .put(AttachmentType.SCAN_RESULT_REPORT, "Scan result report")
            .put(AttachmentType.SCAN_RESULT_REPORT_XML, "Scan result report (XML)")
            .put(AttachmentType.SOURCE_SELF, "Source file (Self-made)")
            .put(AttachmentType.BINARY, "Binaries")
            .put(AttachmentType.BINARY_SELF, "Binaries (Self-made)")
            .put(AttachmentType.DECISION_REPORT, "Decision report")
            .put(AttachmentType.LEGAL_EVALUATION, "Legal evaluation report")
            .put(AttachmentType.LICENSE_AGREEMENT, "License agreement")
            .put(AttachmentType.SCREENSHOT, "Screenshot of website")
            .put(AttachmentType.README_OSS, "ReadMe OSS")
            .put(AttachmentType.OTHER, "Other")
            .put(AttachmentType.SECURITY_ASSESSMENT, "Security Assessment")
            .put(AttachmentType.SBOM, "SBOM")
            .put(AttachmentType.INITIAL_SCAN_REPORT, "Initial Scan Report")
            .put(AttachmentType.INTERNAL_USE_SCAN, "Internal Use Scan")
            .build();

    // @formatter:off
    private static final ImmutableMap<AttachmentType, String>
            MAP_ATTACHMENT_TYPE_SHORT_STRING = ImmutableMap.<AttachmentType, String>builder()
            .put(AttachmentType.DOCUMENT, "DOC")
            .put(AttachmentType.SOURCE, "SRC")
            .put(AttachmentType.DESIGN, "DSN")
            .put(AttachmentType.REQUIREMENT, "RDT")
            .put(AttachmentType.CLEARING_REPORT, "CRT")
            .put(AttachmentType.COMPONENT_LICENSE_INFO_XML, "CLX")
            .put(AttachmentType.COMPONENT_LICENSE_INFO_COMBINED, "CLI")
            .put(AttachmentType.SCAN_RESULT_REPORT, "SRR")
            .put(AttachmentType.SCAN_RESULT_REPORT_XML, "SRX")
            .put(AttachmentType.SOURCE_SELF, "SRS")
            .put(AttachmentType.BINARY, "BIN")
            .put(AttachmentType.BINARY_SELF, "BIS")
            .put(AttachmentType.DECISION_REPORT, "DRT")
            .put(AttachmentType.LEGAL_EVALUATION, "LRT")
            .put(AttachmentType.LICENSE_AGREEMENT, "LAT")
            .put(AttachmentType.SCREENSHOT, "SCR")
            .put(AttachmentType.README_OSS, "RDM")
            .put(AttachmentType.OTHER, "OTH")
            .put(AttachmentType.SECURITY_ASSESSMENT, "SECA")
            .put(AttachmentType.SBOM, "SBOM")
            .put(AttachmentType.INITIAL_SCAN_REPORT, "ISR")
            .put(AttachmentType.INTERNAL_USE_SCAN, "IUS")
            .build();

    private static final ImmutableMap<ClearingState, String> MAP_CLEARING_STATUS_STRING = ImmutableMap
            .<ClearingState, String>builder()
            .put(ClearingState.NEW_CLEARING, "New")
            .put(ClearingState.SENT_TO_CLEARING_TOOL, "Sent to clearing tool")
            .put(ClearingState.UNDER_CLEARING, "Under clearing")
            .put(ClearingState.REPORT_AVAILABLE, "Report available")
            .put(ClearingState.APPROVED, "Report approved")
            .put(ClearingState.SCAN_AVAILABLE, "Scan available")
            .put(ClearingState.INTERNAL_USE_SCAN_AVAILABLE, "Internal use scan available")
            .build();

    private static final ImmutableMap<ExternalTool, String> MAP_EXTERNAL_TOOL_STRING = ImmutableMap
            .<ExternalTool, String>builder()
            .put(ExternalTool.FOSSOLOGY, "Fossology")
            .build();

    private static final ImmutableMap<ExternalToolProcessStatus, String> MAP_EXTERNAL_TOOL_PROCESS_STATUS_STRING = ImmutableMap
            .<ExternalToolProcessStatus, String>builder()
            .put(ExternalToolProcessStatus.NEW, "New")
            .put(ExternalToolProcessStatus.IN_WORK, "In Work")
            .put(ExternalToolProcessStatus.DONE, "Done")
            .put(ExternalToolProcessStatus.OUTDATED, "Outdated")
            .build();

    // @formatter:on

    private static final ImmutableMap<ModerationState, String> MAP_MODERATION_STATE_STRING = ImmutableMap.of(
            ModerationState.APPROVED, "Approved",
            ModerationState.PENDING, "Pending",
            ModerationState.REJECTED, "Rejected",
            ModerationState.INPROGRESS, "In progress");

    private static final ImmutableMap<ProjectRelationship, String> MAP_PROJECT_RELATION_STRING = ImmutableMap.of(
            ProjectRelationship.UNKNOWN, "Unknown" ,
            ProjectRelationship.DUPLICATE, "Duplicate" ,
            ProjectRelationship.CONTAINED, "Is a subproject" ,
            ProjectRelationship.REFERRED, "Related");

    private static final ImmutableMap<ReleaseRelationship, String> MAP_RELEASE_RELATION_STRING = ImmutableMap.<ReleaseRelationship, String>builder()
            .put(ReleaseRelationship.UNKNOWN, "Unknown")
            .put(ReleaseRelationship.CONTAINED, "Contained")
            .put(ReleaseRelationship.REFERRED, "Related")
            .put(ReleaseRelationship.DYNAMICALLY_LINKED, "Dynamically linked")
            .put(ReleaseRelationship.STATICALLY_LINKED, "Statically linked")
            .put(ReleaseRelationship.SIDE_BY_SIDE, "Side by side")
            .put(ReleaseRelationship.STANDALONE, "Standalone")
            .put(ReleaseRelationship.INTERNAL_USE, "Internal use")
            .put(ReleaseRelationship.OPTIONAL, "Optional")
            .put(ReleaseRelationship.TO_BE_REPLACED, "To be replaced")
            .put(ReleaseRelationship.CODE_SNIPPET, "Code Snippet")
            .build();

    private static final ImmutableMap<RepositoryType, String> MAP_REPOSITORY_TYPE_STRING = ImmutableMap.<RepositoryType, String>builder()
            .put(RepositoryType.UNKNOWN, "Unknown")
            .put(RepositoryType.GIT, "Git")
            .put(RepositoryType.CLEARCASE, "ClearCase")
            .put(RepositoryType.SVN, "Subversion (SVN)")
            .put(RepositoryType.CVS, "CVS")
            .put(RepositoryType.MERCURIAL, "Mercurial")
            .put(RepositoryType.PERFORCE, "Perforce")
            .put(RepositoryType.VISUAL_SOURCESAFE, "Visual SourceSafe")
            .put(RepositoryType.BAZAAR, "Bazaar")
            .put(RepositoryType.ALIENBRAIN, "Alienbrain")
            .put(RepositoryType.TEAM_FOUNDATION_SERVER, "Team Foundation Server")
            .put(RepositoryType.RATIONAL_SYNERGY, "IBM Rational Synergy")
            .put(RepositoryType.PTC_INTEGRITY, "PTC Integrity")
            .put(RepositoryType.DTR, "SAP Design Time Repository (DTR)")
            .put(RepositoryType.DARCS, "Darcs")
            .put(RepositoryType.FOSSIL, "Fossil")
            .put(RepositoryType.GNU_ARCH, "GNU arch")
            .put(RepositoryType.MONOTONE, "Monotone")
            .put(RepositoryType.BIT_KEEPER, "BitKeeper")
            .put(RepositoryType.RATIONAL_TEAM_CONCERT, "Rational Team Concert")
            .put(RepositoryType.RCS, "Revision Control System (RCS)")
            .build();

    private static final ImmutableMap<MainlineState, String> MAP_MAINLINE_STATE_STRING = ImmutableMap.of(
            MainlineState.OPEN, "Open",
            MainlineState.MAINLINE, "Mainline",
            MainlineState.SPECIFIC, "Specific",
            MainlineState.PHASEOUT, "Phaseout",
            MainlineState.DENIED, "Denied"
    );

    private static final ImmutableMap<CheckStatus, String> MAP_CHECK_STATUS_STRING = ImmutableMap.of(
            CheckStatus.ACCEPTED,"Accepted",
            CheckStatus.REJECTED,"Rejected",
            CheckStatus.NOTCHECKED,"Not checked"
    );

    private static final ImmutableMap<Visibility, String> MAP_VISIBILITY_STRING = ImmutableMap.of(
            Visibility.PRIVATE, "Private" ,
            Visibility.ME_AND_MODERATORS, "Me and Moderators",
            Visibility.BUISNESSUNIT_AND_MODERATORS, "Group and Moderators",
            Visibility.EVERYONE, "Everyone"
    );

    private static final ImmutableMap<ProjectState, String> MAP_PROJECT_STATE_STRING = ImmutableMap.of(
            ProjectState.ACTIVE, "Active" ,
            ProjectState.PHASE_OUT, "Phase out" ,
            ProjectState.UNKNOWN, "Unknown",
            ProjectState.SVM_ONLY, "SVM Only",
            ProjectState.PRIVATE, "Private",
            ProjectState.UNDER_DEVELOPMENT, "Under Development",
            ProjectState.RELEASED, "Released");

    private static final ImmutableMap<ProjectClearingState, String> MAP_PROJECT_CLEARING_STATE_STRING = ImmutableMap.of(
            ProjectClearingState.OPEN, "Open",
            ProjectClearingState.IN_PROGRESS, "In Progress",
            ProjectClearingState.CLOSED, "Closed");

    private static final ImmutableMap<UserGroup, String> MAP_USER_GROUP_STRING = ImmutableMap.<UserGroup, String>builder()
            .put(UserGroup.USER, "User")
            .put(UserGroup.ADMIN, "Admin")
            .put(UserGroup.CLEARING_ADMIN, "Clearing Admin")
            .put(UserGroup.CLEARING_EXPERT, "Clearing Expert")
            .put(UserGroup.ECC_ADMIN, "ECC Admin")
            .put(UserGroup.SECURITY_ADMIN, "Security Admin")
            .put(UserGroup.SW360_ADMIN, "SW360 Admin")
            .put(UserGroup.SECURITY_USER, "Security User")
            .build();

    private static final ImmutableMap<VulnerabilityImpact, String> MAP_VULNERABILITY_IMPACT = ImmutableMap.of(
            VulnerabilityImpact.NONE, "IMPACT NONE",
            VulnerabilityImpact.PARTIAL, "IMPACT PARTIAL",
            VulnerabilityImpact.COMPLETE, "IMPACT COMPLETE"
    );

    private static final ImmutableMap<VulnerabilityAccessAuthentication, String> MAP_VULNERABILITY_ACCESS_AUTHENTICATION = ImmutableMap.of(
            VulnerabilityAccessAuthentication.NONE, "AUTHEN NONE",
            VulnerabilityAccessAuthentication.SINGLE, "AUTHEN SINGLE",
            VulnerabilityAccessAuthentication.MULTIPLE, "AUTHEN MULTIPLE"
    );

    private static final ImmutableMap<VulnerabilityAccessComplexity, String> MAP_VULNERABILITY_ACCESS_COMPLEXITY = ImmutableMap.of(
            VulnerabilityAccessComplexity.LOW, "COMPLEXITY LOW",
            VulnerabilityAccessComplexity.MEDIUM, "COMPLEXITY MEDIUM",
            VulnerabilityAccessComplexity.HIGH, "COMPLEXITY HIGH"
    );

    private static final ImmutableMap<VulnerabilityAccessVector, String> MAP_VULNERABILITY_ACCESS_VECTOR = ImmutableMap.of(
           VulnerabilityAccessVector.LOCAL, "VECTOR LOCAL",
           VulnerabilityAccessVector.NETWORK, "VECTOR NETWORK",
           VulnerabilityAccessVector.ADJACENT_NETWORK, "VECTOR ADJACENT NETWORK"
    );

    private static final ImmutableMap<VulnerabilityRatingForProject, String> MAP_VULNERABILITY_RATING_FOR_PROJECT_STRING = ImmutableMap.of(
            VulnerabilityRatingForProject.NOT_CHECKED, "Not Checked" ,
            VulnerabilityRatingForProject.IRRELEVANT, "Irrelevant" ,
            VulnerabilityRatingForProject.RESOLVED, "Resolved" ,
            VulnerabilityRatingForProject.APPLICABLE, "Applicable",
            VulnerabilityRatingForProject.IN_ANALYSIS, "In Analysis"
    );

    private static final ImmutableMap<VerificationState, String> MAP_VERIFICATION_STATUS_STRING = ImmutableMap.of(
            VerificationState.NOT_CHECKED, "Not Checked" ,
            VerificationState.CHECKED, "Checked" ,
            VerificationState.INCORRECT, "Incorrect"
    );

    private static final ImmutableMap<ECCStatus, String> MAP_ECC_STATUS_STRING = ImmutableMap.of(
            ECCStatus.OPEN, "Open" ,
            ECCStatus.IN_PROGRESS, "In Progress" ,
            ECCStatus.APPROVED, "Approved",
            ECCStatus.REJECTED, "Rejected"
    );

    private static final ImmutableMap<DocumentType, String> MAP_DOCUMENT_TYPE_STRING = ImmutableMap.<DocumentType, String>builder()
            .put(DocumentType.COMPONENT, "component" )
            .put(DocumentType.RELEASE, "release")
            .put(DocumentType.PROJECT, "project")
            .put(DocumentType.LICENSE, "license")
            .put(DocumentType.USER, "user")
            .put(DocumentType.SPDX_DOCUMENT, "spdxDocument")
            .put(DocumentType.SPDX_PACKAGE_INFO, "spdxPackageInfo")
            .put(DocumentType.SPDX_DOCUMENT_CREATION_INFO, "spdxDocumentCreation")
            .build();

    private static final ImmutableMap<ObligationStatus, String> MAP_OBLIGATION_STATUS_STRING = ImmutableMap.<ObligationStatus, String>builder()
            .put(ObligationStatus.OPEN, "Open")
            .put(ObligationStatus.ACKNOWLEDGED_OR_FULFILLED, "Acknowledged or Fulfilled")
            .put(ObligationStatus.WILL_BE_FULFILLED_BEFORE_RELEASE, "Will be fulfilled before release")
            .put(ObligationStatus.NOT_APPLICABLE, "Not Applicable")
            .put(ObligationStatus.DEFERRED_TO_PARENT_PROJECT, "Deferred to parent project")
            .put(ObligationStatus.FULFILLED_AND_PARENT_MUST_ALSO_FULFILL, "Fulfilled and parent must also fulfill")
            .put(ObligationStatus.ESCALATED, "Escalated")
            .build();

    private static final ImmutableMap<ClearingRequestState, String> MAP_CLEARING_REQUEST_STATE_STRING = ImmutableMap.<ClearingRequestState, String>builder()
            .put(ClearingRequestState.NEW, "New")
            .put(ClearingRequestState.SANITY_CHECK, "Sanity Check")
            .put(ClearingRequestState.ACCEPTED, "Accepted")
            .put(ClearingRequestState.REJECTED, "Rejected")
            .put(ClearingRequestState.IN_QUEUE, "In Queue")
            .put(ClearingRequestState.IN_PROGRESS, "In Progress")
            .put(ClearingRequestState.CLOSED, "Closed")
            .put(ClearingRequestState.AWAITING_RESPONSE, "Awaiting Response")
            .put(ClearingRequestState.ON_HOLD, "On Hold")
            .put(ClearingRequestState.PENDING_INPUT,"Pending Input")
            .build();

    private static final ImmutableMap<ClearingReportStatus, String> MAP_CLEARING_REPORT_STATUS_STRING = ImmutableMap.<ClearingReportStatus, String>builder()
            .put(ClearingReportStatus.NO_STATUS, "No status")
            .put(ClearingReportStatus.NO_REPORT, "No report")
            .put(ClearingReportStatus.DOWNLOAD, "Download")
            .build();

    private static final ImmutableMap<ClearingRequestPriority, String> MAP_CLEARING_REQUEST_PRIORITY_STRING = ImmutableMap.of(
            ClearingRequestPriority.LOW, "Low",
            ClearingRequestPriority.MEDIUM, "Medium",
            ClearingRequestPriority.HIGH, "High",
            ClearingRequestPriority.CRITICAL, "Critical"
    );

    private static final ImmutableMap<ClearingRequestType, String> MAP_CLEARING_REQUEST_TYPE_STRING = ImmutableMap.of(
            ClearingRequestType.DEEP, "Deep CLX",
            ClearingRequestType.HIGH, "High ISR"
    );

    private static final ImmutableMap<ClearingRequestSize, String> MAP_CLEARING_REQUEST_SIZE_STRING = ImmutableMap.of(
            ClearingRequestSize.VERY_SMALL, "Very Small",
            ClearingRequestSize.SMALL, "Small",
            ClearingRequestSize.MEDIUM, "Medium",
            ClearingRequestSize.LARGE, "Large",
            ClearingRequestSize.VERY_LARGE, "Very Large"
    );

    private static final ImmutableMap<UserAccess, String> MAP_USER_ACCESS_STRING = ImmutableMap.<UserAccess, String>builder()
            .put(UserAccess.READ, "Read")
            .put(UserAccess.READ_WRITE, "Read and Write")
            .build();

    private static final ImmutableMap<PackageManager, String> MAP_PACKAGE_MANAGER_STRING = ImmutableMap.<PackageManager, String>builder()
            .put(PackageManager.ALPINE, "Alpine")
            .put(PackageManager.ALPM, "ALPM")
            .put(PackageManager.APK, "APK")
            .put(PackageManager.BITBUCKET, "Bitbucket")
            .put(PackageManager.CARGO, "Cargo")
            .put(PackageManager.COCOAPODS, "Cocoapods")
            .put(PackageManager.COMPOSER, "Composer")
            .put(PackageManager.CONAN, "Conan")
            .put(PackageManager.CONDA, "Conda")
            .put(PackageManager.CPAN, "Cpan")
            .put(PackageManager.CRAN, "Cran")
            .put(PackageManager.DEB, "Deb")
            .put(PackageManager.DOCKER, "Docker")
            .put(PackageManager.DRUPAL, "Drupal")
            .put(PackageManager.GEM, "Gem")
            .put(PackageManager.GENERIC, "Generic")
            .put(PackageManager.GITHUB, "GitHub")
            .put(PackageManager.GITLAB, "GitLab")
            .put(PackageManager.GOLANG, "GoLang")
            .put(PackageManager.GRADLE, "Gradle")
            .put(PackageManager.HACKAGE, "Hackage")
            .put(PackageManager.HEX, "Hex")
            .put(PackageManager.HUGGINGFACE, "HuggingFace")
            .put(PackageManager.MAVEN, "Maven")
            .put(PackageManager.MLFLOW, "MLflow")
            .put(PackageManager.NPM, "Npm")
            .put(PackageManager.NUGET, "NuGet")
            .put(PackageManager.OCI, "Oci")
            .put(PackageManager.PUB, "Pub")
            .put(PackageManager.PYPI, "PyPi")
            .put(PackageManager.RPM, "Rpm")
            .put(PackageManager.SWID, "Swid")
            .put(PackageManager.SWIFT, "swift")
            .put(PackageManager.YARN, "Yarn")
            .put(PackageManager.YOCTO, "Yocto")
            .build();

    private static final ImmutableMap<CycloneDxComponentType, String> MAP_CYCLONE_DX_COMPONENT_TYPE_STRING = ImmutableMap.<CycloneDxComponentType, String>builder()
            .put(CycloneDxComponentType.APPLICATION, "Application")
            .put(CycloneDxComponentType.CONTAINER, "Container")
            .put(CycloneDxComponentType.DEVICE, "Device")
            .put(CycloneDxComponentType.FILE, "File")
            .put(CycloneDxComponentType.FIRMWARE, "Firmware")
            .put(CycloneDxComponentType.FRAMEWORK, "Framework")
            .put(CycloneDxComponentType.LIBRARY, "Library")
            .put(CycloneDxComponentType.OPERATING_SYSTEM, "Operating System")
            .build();

    public static final ImmutableMap<Class<?>, Map<?, String>>
            MAP_ENUMTYPE_MAP = ImmutableMap.<Class<?>, Map<?, String>>builder()
            .put(ComponentType.class, MAP_COMPONENT_TYPE_STRING)
            .put(Quadratic.class, MAP_QUADRATIC_STRING)
            .put(Ternary.class, MAP_TERNARY_STRING)
            .put(DateRange.class, MAP_DATE_RANGE_STRING)
            .put(ProjectType.class, MAP_PROJECT_TYPE_STRING)
            .put(AttachmentType.class, MAP_ATTACHMENT_TYPE_STRING)
            .put(ClearingState.class, MAP_CLEARING_STATUS_STRING)
            .put(ExternalTool.class, MAP_EXTERNAL_TOOL_STRING)
            .put(ExternalToolProcessStatus.class, MAP_EXTERNAL_TOOL_PROCESS_STATUS_STRING)
            .put(ModerationState.class, MAP_MODERATION_STATE_STRING)
            .put(ProjectRelationship.class, MAP_PROJECT_RELATION_STRING)
            .put(ReleaseRelationship.class, MAP_RELEASE_RELATION_STRING)
            .put(RepositoryType.class, MAP_REPOSITORY_TYPE_STRING)
            .put(MainlineState.class, MAP_MAINLINE_STATE_STRING)
            .put(UserGroup.class, MAP_USER_GROUP_STRING)
            .put(Visibility.class, MAP_VISIBILITY_STRING)
            .put(ProjectState.class, MAP_PROJECT_STATE_STRING)
            .put(ProjectClearingState.class, MAP_PROJECT_CLEARING_STATE_STRING)
            .put(CheckStatus.class,MAP_CHECK_STATUS_STRING)
            .put(VerificationState.class, MAP_VERIFICATION_STATUS_STRING)
            .put(VulnerabilityRatingForProject.class, MAP_VULNERABILITY_RATING_FOR_PROJECT_STRING)
            .put(VulnerabilityImpact.class, MAP_VULNERABILITY_IMPACT)
            .put(VulnerabilityAccessAuthentication.class, MAP_VULNERABILITY_ACCESS_AUTHENTICATION)
            .put(VulnerabilityAccessComplexity.class, MAP_VULNERABILITY_ACCESS_COMPLEXITY)
            .put(VulnerabilityAccessVector.class, MAP_VULNERABILITY_ACCESS_VECTOR)
            .put(ECCStatus.class, MAP_ECC_STATUS_STRING)
            .put(DocumentType.class, MAP_DOCUMENT_TYPE_STRING)
            .put(ObligationStatus.class, MAP_OBLIGATION_STATUS_STRING)
            .put(ClearingRequestState.class, MAP_CLEARING_REQUEST_STATE_STRING)
            .put(ClearingReportStatus.class, MAP_CLEARING_REPORT_STATUS_STRING)
            .put(ObligationLevel.class, MAP_OBLIGATION_LEVEL_STRING)
            .put(ObligationType.class, MAP_OBLIGATION_TYPE_STRING)
            .put(ClearingRequestPriority.class, MAP_CLEARING_REQUEST_PRIORITY_STRING)
            .put(UserAccess.class, MAP_USER_ACCESS_STRING)
            .put(PackageManager.class, MAP_PACKAGE_MANAGER_STRING)
            .put(CycloneDxComponentType.class, MAP_CYCLONE_DX_COMPONENT_TYPE_STRING)
            .put(ClearingRequestType.class, MAP_CLEARING_REQUEST_TYPE_STRING)
            .put(ClearingRequestSize.class, MAP_CLEARING_REQUEST_SIZE_STRING)
            .build();

    // @formatter:off
    public static final ImmutableMap<Class<?>, Map<?, String>>
            MAP_ENUMTYPE_SHORT_STRING_MAP = ImmutableMap.<Class<?>, Map<?, String>>builder()
            .put(AttachmentType.class, MAP_ATTACHMENT_TYPE_SHORT_STRING)
            .build();

    /**
     * @return the display label for {@code value}, or an empty string when {@code value} is null.
     *         Falls back to the constant name when the enum type has no registered labels.
     */
    public static String toDisplayString(Enum<?> value) {
        return lookup(MAP_ENUMTYPE_MAP, value);
    }

    /**
     * @return the short label for {@code value} (e.g. "CRT" for a clearing report), or an empty
     *         string when {@code value} is null.
     */
    public static String toShortString(Enum<?> value) {
        return lookup(MAP_ENUMTYPE_SHORT_STRING_MAP, value);
    }

    /** Inverse of {@link #toDisplayString}. */
    public static <T extends Enum<T>> T byDisplayString(String label, Class<T> type) {
        return reverseLookup(MAP_ENUMTYPE_MAP, label, type);
    }

    /** Inverse of {@link #toShortString}. */
    public static <T extends Enum<T>> T byShortString(String label, Class<T> type) {
        return reverseLookup(MAP_ENUMTYPE_SHORT_STRING_MAP, label, type);
    }

    /** Resolves an enum constant by its {@code name()}, ignoring labels entirely. */
    public static <T extends Enum<T>> T byName(String name, Class<T> type) {
        for (T candidate : type.getEnumConstants()) {
            if (candidate.name().equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    private static String lookup(Map<Class<?>, Map<?, String>> registry, Enum<?> value) {
        if (value == null) {
            return "";
        }
        Map<?, String> labels = registry.get(value.getClass());
        if (labels == null) {
            return value.name();
        }
        String label = labels.get(value);
        return label == null ? value.name() : label;
    }

    private static <T extends Enum<T>> T reverseLookup(Map<Class<?>, Map<?, String>> registry,
            String label, Class<T> type) {
        Map<?, String> labels = registry.get(type);
        if (labels == null || label == null) {
            return null;
        }
        for (T candidate : type.getEnumConstants()) {
            if (label.equals(labels.get(candidate))) {
                return candidate;
            }
        }
        return null;
    }
}
