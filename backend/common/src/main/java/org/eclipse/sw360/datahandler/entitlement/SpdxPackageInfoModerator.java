/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter;
import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.services.users.User;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

public class SpdxPackageInfoModerator extends Moderator {

    private static final Logger log = LogManager.getLogger(SpdxPackageInfoModerator.class);

    public RequestStatus updateSpdxPackageInfo(PackageInformation packageInfo, User user) {
        try {
            ModerationClients.get().createSpdxPackageInfoRequest(
                    PackageInformationConverter.fromThrift(packageInfo), user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate SPDX Package Info " + packageInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSpdxPackageInfo(PackageInformation packageInfo, User user) {
        try {
            ModerationClients.get().createSpdxPackageInfoDeleteRequest(
                    PackageInformationConverter.fromThrift(packageInfo), user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate SPDX Package Info " + packageInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public org.eclipse.sw360.datahandler.services.spdx.PackageInformation updateSpdxPackageInfoFromModerationRequest(
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation packageInfo,
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation packageInfoAdditions,
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation packageInfoDeletions) {
        if (packageInfoAdditions == null) {
            packageInfoAdditions = new org.eclipse.sw360.datahandler.services.spdx.PackageInformation();
        }
        if (packageInfoDeletions == null) {
            packageInfoDeletions = new org.eclipse.sw360.datahandler.services.spdx.PackageInformation();
        }
        for (FieldAccessor<org.eclipse.sw360.datahandler.services.spdx.PackageInformation> field : PojoFields
                .of(org.eclipse.sw360.datahandler.services.spdx.PackageInformation.class)) {
            if (!field.isSet(packageInfoAdditions) && !field.isSet(packageInfoDeletions)) {
                continue;
            }
            switch (field.name()) {
                case "id":
                case "revision":
                case "type":
                    break;
                case "checksums":
                    packageInfo.setChecksums(mergeElementSet(
                            packageInfo.getChecksums(),
                            packageInfoAdditions.getChecksums(),
                            packageInfoDeletions.getChecksums(),
                            org.eclipse.sw360.datahandler.services.spdx.CheckSum.class));
                    break;
                case "externalRefs":
                    packageInfo.setExternalRefs(mergeElementSet(
                            packageInfo.getExternalRefs(),
                            packageInfoAdditions.getExternalRefs(),
                            packageInfoDeletions.getExternalRefs(),
                            org.eclipse.sw360.datahandler.services.spdx.ExternalReference.class));
                    break;
                case "annotations":
                    packageInfo.setAnnotations(mergeElementSet(
                            packageInfo.getAnnotations(),
                            packageInfoAdditions.getAnnotations(),
                            packageInfoDeletions.getAnnotations(),
                            org.eclipse.sw360.datahandler.services.spdx.Annotations.class));
                    break;
                case "packageVerificationCode":
                    packageInfo = updatePackageVerificationCode(packageInfo, packageInfoAdditions);
                    break;
                default:
                    applyBasicField(field, packageInfo, packageInfoAdditions, packageInfoDeletions);
            }
        }
        return packageInfo;
    }

    private org.eclipse.sw360.datahandler.services.spdx.PackageInformation updatePackageVerificationCode(
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation packageInfo,
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation packageInfoAdditions) {
        org.eclipse.sw360.datahandler.services.spdx.PackageVerificationCode additions =
                packageInfoAdditions.getPackageVerificationCode();
        if (additions == null) {
            return packageInfo;
        }
        org.eclipse.sw360.datahandler.services.spdx.PackageVerificationCode actual =
                packageInfo.getPackageVerificationCode();
        if (actual == null) {
            actual = new org.eclipse.sw360.datahandler.services.spdx.PackageVerificationCode();
        }
        copySetFields(actual, additions, org.eclipse.sw360.datahandler.services.spdx.PackageVerificationCode.class);
        packageInfo.setPackageVerificationCode(actual);
        return packageInfo;
    }

}
