/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.common;


import com.google.common.collect.ImmutableMap;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;

import org.apache.thrift.TEnum;

import java.util.Map;

/**
 * @author Cedric.Bodet@tngtech.com
 * @author Gerrit.Grenzebach@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author Andreas.Reichel@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */

public class ThriftEnumUtils {

    private ThriftEnumUtils() {
        // Utility class with only static functions
    }

    //! Enumeration "fancy name" mappings
    private static final ImmutableMap<ComponentType, String> MAP_COMPONENT_TYPE_STRING = ImmutableMap.<ComponentType, String>builder()
            .put(ComponentType.OSS, "OSS")
            .put(ComponentType.COTS, "COTS")
            .put(ComponentType.INTERNAL, "Internal")
            .put(ComponentType.INNER_SOURCE, "Inner Source")
            .put(ComponentType.SERVICE, "Service")
            .put(ComponentType.FREESOFTWARE, "Freeware")
            .build();

    private static final ImmutableMap<Ternary,String> MAP_TERNARY_STRING = ImmutableMap.of(
            Ternary.UNDEFINED, "undefined",
            Ternary.NO, "no",
            Ternary.YES, "yes");

    private static final ImmutableMap<ProjectType, String> MAP_PROJECT_TYPE_STRING = ImmutableMap.of(
            ProjectType.CUSTOMER, "Customer Project" ,
            ProjectType.INTERNAL, "Internal Project" ,
            ProjectType.PRODUCT, "Product" ,
            ProjectType.SERVICE, "Service",
            ProjectType.INNER_SOURCE, "Inner Source");

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
            .build();

    // @formatter:off
    public static final ImmutableMap<AttachmentType, String>
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
            .build();

    private static final ImmutableMap<ClearingState, String> MAP_CLEARING_STATUS_STRING = ImmutableMap.of(
            ClearingState.NEW_CLEARING, "New",
            ClearingState.SENT_TO_CLEARING_TOOL, "Sent to clearing tool",
            ClearingState.UNDER_CLEARING, "Under clearing",
            ClearingState.REPORT_AVAILABLE, "Report available",
            ClearingState.APPROVED, "Report approved");

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
            ProjectState.UNKNOWN, "Unknown");

    private static final ImmutableMap<ProjectClearingState, String> MAP_PROJECT_CLEARING_STATE_STRING = ImmutableMap.of(
            ProjectClearingState.OPEN, "Open",
            ProjectClearingState.IN_PROGRESS, "In Progress",
            ProjectClearingState.CLOSED, "Closed");

    private static final ImmutableMap<UserGroup, String> MAP_USER_GROUP_STRING = ImmutableMap.<UserGroup, String>builder()
            .put(UserGroup.USER, "User")
            .put(UserGroup.ADMIN, "Admin")
            .put(UserGroup.CLEARING_ADMIN, "Clearing Admin")
            .put(UserGroup.ECC_ADMIN, "ECC Admin")
            .put(UserGroup.SECURITY_ADMIN, "Security Admin")
            .put(UserGroup.SW360_ADMIN, "SW360 Admin")
            .build();

    private static final ImmutableMap<VulnerabilityRatingForProject, String> MAP_VULNERABILITY_RATING_FOR_PROJECT_STRING = ImmutableMap.of(
            VulnerabilityRatingForProject.NOT_CHECKED, "Not Checked" ,
            VulnerabilityRatingForProject.IRRELEVANT, "Irrelevant" ,
            VulnerabilityRatingForProject.RESOLVED, "Resolved" ,
            VulnerabilityRatingForProject.APPLICABLE, "Applicable"
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

    private static final ImmutableMap<DocumentType, String> MAP_DOCUMENT_TYPE_STRING = ImmutableMap.of(
            DocumentType.COMPONENT, "component" ,
            DocumentType.RELEASE, "release" ,
            DocumentType.PROJECT, "project",
            DocumentType.LICENSE, "license",
            DocumentType.USER, "user"
    );

    public static final ImmutableMap<Class<? extends TEnum>, Map<? extends TEnum, String>>
            MAP_ENUMTYPE_MAP = ImmutableMap.<Class<? extends TEnum>, Map<? extends TEnum, String>>builder()
            .put(ComponentType.class, MAP_COMPONENT_TYPE_STRING)
            .put(Ternary.class, MAP_TERNARY_STRING)
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
            .put(ECCStatus.class, MAP_ECC_STATUS_STRING)
            .put(DocumentType.class, MAP_DOCUMENT_TYPE_STRING)
            .build();

    public static String enumToString(TEnum value) {

        String out = "";
        if (value != null) {
            out = MAP_ENUMTYPE_MAP.get(value.getClass()).get(value);
        }
        return out;
    }

     public static  <T extends Enum<T>> T  stringToEnum(String in, Class<T> clazz){
         for (T t : clazz.getEnumConstants()) {
             if(t.name().equals(in)) return t;
         }

         return null;
     }
    public static  <T extends Enum<T>> T  enumByString(String in, Class<T> clazz){
        Map<? extends TEnum, String> map = MAP_ENUMTYPE_MAP.get(clazz);
        for (T t : clazz.getEnumConstants()) {
            if(map.get(t).equals(in)) return t;
        }

        return null;
    }

    // @formatter:off
    public static final ImmutableMap<Class<? extends TEnum>, Map<? extends TEnum, String>>
            MAP_ENUMTYPE_SHORT_STRING_MAP = ImmutableMap.<Class<? extends TEnum>, Map<? extends TEnum, String>>builder()
            .put(AttachmentType.class, MAP_ATTACHMENT_TYPE_SHORT_STRING)
            .build();
    // @formatter:on

    public static String enumToShortString(TEnum value) {
        String out = "";
        if (value != null) {
            out = MAP_ENUMTYPE_SHORT_STRING_MAP.get(value.getClass()).get(value);
        }
        return out;
    }

    public static <T extends Enum<T>> T enumByShortString(String in, Class<T> clazz) {
        Map<? extends TEnum, String> map = MAP_ENUMTYPE_SHORT_STRING_MAP.get(clazz);
        for (T t : clazz.getEnumConstants()) {
            if (map.get(t).equals(in))
                return t;
        }
        return null;
    }

}
