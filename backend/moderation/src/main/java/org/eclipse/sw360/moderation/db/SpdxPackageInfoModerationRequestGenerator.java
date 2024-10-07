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
package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.apache.thrift.protocol.TType;

public class SpdxPackageInfoModerationRequestGenerator extends ModerationRequestGenerator<PackageInformation._Fields, PackageInformation> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, PackageInformation updatePackageInfo, PackageInformation actualPackageInfo){
        updateDocument = updatePackageInfo;
        actualDocument = actualPackageInfo;

        documentAdditions = new PackageInformation();
        documentDeletions = new PackageInformation();
        //required fields:
        documentAdditions.setId(updateDocument.getId());
        documentDeletions.setId(actualDocument.getId());

        for (PackageInformation._Fields field : PackageInformation._Fields.values()) {
            if (PackageInformation.metaDataMap.get(field).valueMetaData.type == TType.BOOL &&
                actualDocument.getFieldValue(field) != updateDocument.getFieldValue(field)) {
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
            } else if (actualDocument.getFieldValue(field) == null) {
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
            } else if (updateDocument.getFieldValue(field) == null) {
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
            } else if (!actualDocument.getFieldValue(field).equals(updateDocument.getFieldValue(field))) {
                switch (field) {
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case CREATED_BY:
                        break;
                    default:
                        dealWithBaseTypes(field, PackageInformation.metaDataMap.get(field));
                }
            }
        }
        request.setPackageInfoAdditions(documentAdditions);
        request.setPackageInfoDeletions(documentDeletions);
        return request;
    }
}
