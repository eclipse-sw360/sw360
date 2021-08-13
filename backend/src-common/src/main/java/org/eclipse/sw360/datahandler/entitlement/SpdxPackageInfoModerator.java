/*
 * Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

/**
 * Moderation for the SPDX Package service
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */

public class SpdxPackageInfoModerator extends Moderator<PackageInformation._Fields, PackageInformation> {

    private static final Logger log = LogManager.getLogger(SpdxPackageInfoModerator.class);


    public SpdxPackageInfoModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public SpdxPackageInfoModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateSpdxPackageInfo(PackageInformation packageInfo, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSpdxPackageInfoRequest(packageInfo, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate SPDX Package Info " + packageInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSpdxPackageInfo(PackageInformation packageInfo, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSpdxPackageInfoDeleteRequest(packageInfo, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete SPDX document " + packageInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public PackageInformation updateSpdxPackageInfoFromModerationRequest(PackageInformation packageInfo,
                                                      PackageInformation packageInfoAdditions,
                                                      PackageInformation packageInfoDeletions) {
        for (PackageInformation._Fields field : PackageInformation._Fields.values()) {
            if(packageInfoAdditions.getFieldValue(field) == null && packageInfoDeletions.getFieldValue(field) == null){
                continue;
            }
            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                case PERMISSIONS:
                case DOCUMENT_STATE:
                case SPDX_DOCUMENT_ID:
                    break;
                case PACKAGE_VERIFICATION_CODE:
                case CHECKSUMS:
                case EXTERNAL_REFS:
                case ATTRIBUTION_TEXT:
                case ANNOTATIONS:
                default:
                    packageInfo = updateBasicField(field, PackageInformation.metaDataMap.get(field), packageInfo, packageInfoAdditions, packageInfoDeletions);
            }

        }
        return packageInfo;
    }

}

