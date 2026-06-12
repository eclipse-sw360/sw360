/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.moderation;

import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ModerationRequestConverter {

    private ModerationRequestConverter() {}

    public static ModerationRequest fromThrift(org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest thrift) {
        if (thrift == null) {
            return null;
        }
        ModerationRequest pojo = new ModerationRequest();
        if (thrift.isSetCommentDecisionModerator()) {
            pojo.setCommentDecisionModerator(thrift.getCommentDecisionModerator());
        }
        if (thrift.isSetCommentRequestingUser()) {
            pojo.setCommentRequestingUser(thrift.getCommentRequestingUser());
        }
        if (thrift.isSetComponentAdditions()) {
            pojo.setComponentAdditions(org.eclipse.sw360.common.utils.converter.components.ComponentConverter.fromThrift(thrift.getComponentAdditions()));
        }
        if (thrift.isSetComponentDeletions()) {
            pojo.setComponentDeletions(org.eclipse.sw360.common.utils.converter.components.ComponentConverter.fromThrift(thrift.getComponentDeletions()));
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(EnumConverter.fromThrift(thrift.getComponentType(), org.eclipse.sw360.datahandler.services.components.ComponentType.class));
        }
        if (thrift.isSetDocumentCreationInfoAdditions()) {
            pojo.setDocumentCreationInfoAdditions(org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter.fromThrift(thrift.getDocumentCreationInfoAdditions()));
        }
        if (thrift.isSetDocumentCreationInfoDeletions()) {
            pojo.setDocumentCreationInfoDeletions(org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter.fromThrift(thrift.getDocumentCreationInfoDeletions()));
        }
        if (thrift.isSetDocumentId()) {
            pojo.setDocumentId(thrift.getDocumentId());
        }
        if (thrift.isSetDocumentName()) {
            pojo.setDocumentName(thrift.getDocumentName());
        }
        if (thrift.isSetDocumentType()) {
            pojo.setDocumentType(EnumConverter.fromThrift(thrift.getDocumentType(), org.eclipse.sw360.datahandler.services.moderation.DocumentType.class));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseAdditions()) {
            pojo.setLicenseAdditions(org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter.fromThrift(thrift.getLicenseAdditions()));
        }
        if (thrift.isSetLicenseDeletions()) {
            pojo.setLicenseDeletions(org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter.fromThrift(thrift.getLicenseDeletions()));
        }
        if (thrift.isSetModerationState()) {
            pojo.setModerationState(EnumConverter.fromThrift(thrift.getModerationState(), org.eclipse.sw360.datahandler.services.common.ModerationState.class));
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetPackageInfoAdditions()) {
            pojo.setPackageInfoAdditions(org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter.fromThrift(thrift.getPackageInfoAdditions()));
        }
        if (thrift.isSetPackageInfoDeletions()) {
            pojo.setPackageInfoDeletions(org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter.fromThrift(thrift.getPackageInfoDeletions()));
        }
        if (thrift.isSetProjectAdditions()) {
            pojo.setProjectAdditions(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.fromThrift(thrift.getProjectAdditions()));
        }
        if (thrift.isSetProjectDeletions()) {
            pojo.setProjectDeletions(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.fromThrift(thrift.getProjectDeletions()));
        }
        if (thrift.isSetReleaseAdditions()) {
            pojo.setReleaseAdditions(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getReleaseAdditions()));
        }
        if (thrift.isSetReleaseDeletions()) {
            pojo.setReleaseDeletions(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getReleaseDeletions()));
        }
        if (thrift.isSetRequestDocumentDelete()) {
            pojo.setRequestDocumentDelete(thrift.isRequestDocumentDelete());
        }
        if (thrift.isSetRequestingUser()) {
            pojo.setRequestingUser(thrift.getRequestingUser());
        }
        if (thrift.isSetRequestingUserDepartment()) {
            pojo.setRequestingUserDepartment(thrift.getRequestingUserDepartment());
        }
        if (thrift.isSetReviewer()) {
            pojo.setReviewer(thrift.getReviewer());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetTimestamp()) {
            pojo.setTimestamp(thrift.getTimestamp());
        }
        if (thrift.isSetTimestampOfDecision()) {
            pojo.setTimestampOfDecision(thrift.getTimestampOfDecision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUser()) {
            pojo.setUser(org.eclipse.sw360.common.utils.converter.users.UserConverter.fromThrift(thrift.getUser()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest toThrift(ModerationRequest pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest thrift = new org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest();
        if (pojo.getCommentDecisionModerator() != null) {
            thrift.setCommentDecisionModerator(pojo.getCommentDecisionModerator());
        }
        if (pojo.getCommentRequestingUser() != null) {
            thrift.setCommentRequestingUser(pojo.getCommentRequestingUser());
        }
        if (pojo.getComponentAdditions() != null) {
            thrift.setComponentAdditions(org.eclipse.sw360.common.utils.converter.components.ComponentConverter.toThrift(pojo.getComponentAdditions()));
        }
        if (pojo.getComponentDeletions() != null) {
            thrift.setComponentDeletions(org.eclipse.sw360.common.utils.converter.components.ComponentConverter.toThrift(pojo.getComponentDeletions()));
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(EnumConverter.toThrift(pojo.getComponentType(), org.eclipse.sw360.datahandler.thrift.components.ComponentType.class));
        }
        if (pojo.getDocumentCreationInfoAdditions() != null) {
            thrift.setDocumentCreationInfoAdditions(org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter.toThrift(pojo.getDocumentCreationInfoAdditions()));
        }
        if (pojo.getDocumentCreationInfoDeletions() != null) {
            thrift.setDocumentCreationInfoDeletions(org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter.toThrift(pojo.getDocumentCreationInfoDeletions()));
        }
        if (pojo.getDocumentId() != null) {
            thrift.setDocumentId(pojo.getDocumentId());
        }
        if (pojo.getDocumentName() != null) {
            thrift.setDocumentName(pojo.getDocumentName());
        }
        if (pojo.getDocumentType() != null) {
            thrift.setDocumentType(EnumConverter.toThrift(pojo.getDocumentType(), org.eclipse.sw360.datahandler.thrift.moderation.DocumentType.class));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseAdditions() != null) {
            thrift.setLicenseAdditions(org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter.toThrift(pojo.getLicenseAdditions()));
        }
        if (pojo.getLicenseDeletions() != null) {
            thrift.setLicenseDeletions(org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter.toThrift(pojo.getLicenseDeletions()));
        }
        if (pojo.getModerationState() != null) {
            thrift.setModerationState(EnumConverter.toThrift(pojo.getModerationState(), org.eclipse.sw360.datahandler.thrift.ModerationState.class));
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getPackageInfoAdditions() != null) {
            thrift.setPackageInfoAdditions(org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter.toThrift(pojo.getPackageInfoAdditions()));
        }
        if (pojo.getPackageInfoDeletions() != null) {
            thrift.setPackageInfoDeletions(org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter.toThrift(pojo.getPackageInfoDeletions()));
        }
        if (pojo.getProjectAdditions() != null) {
            thrift.setProjectAdditions(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.toThrift(pojo.getProjectAdditions()));
        }
        if (pojo.getProjectDeletions() != null) {
            thrift.setProjectDeletions(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.toThrift(pojo.getProjectDeletions()));
        }
        if (pojo.getReleaseAdditions() != null) {
            thrift.setReleaseAdditions(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getReleaseAdditions()));
        }
        if (pojo.getReleaseDeletions() != null) {
            thrift.setReleaseDeletions(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getReleaseDeletions()));
        }
        if (pojo.getRequestDocumentDelete() != null) {
            thrift.setRequestDocumentDelete(pojo.getRequestDocumentDelete());
        }
        if (pojo.getRequestingUser() != null) {
            thrift.setRequestingUser(pojo.getRequestingUser());
        }
        if (pojo.getRequestingUserDepartment() != null) {
            thrift.setRequestingUserDepartment(pojo.getRequestingUserDepartment());
        }
        if (pojo.getReviewer() != null) {
            thrift.setReviewer(pojo.getReviewer());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getTimestamp() != null) {
            thrift.setTimestamp(pojo.getTimestamp());
        }
        if (pojo.getTimestampOfDecision() != null) {
            thrift.setTimestampOfDecision(pojo.getTimestampOfDecision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUser() != null) {
            thrift.setUser(org.eclipse.sw360.common.utils.converter.users.UserConverter.toThrift(pojo.getUser()));
        }
        return thrift;
    }
}
