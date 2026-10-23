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

import org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter;
import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.users.User;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

public class SpdxDocumentCreationInfoModerator extends Moderator {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoModerator.class);

    public RequestStatus updateSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {
        try {
            ModerationClients.get().createSpdxDocumentCreationInfoRequest(
                    DocumentCreationInformationConverter.fromThrift(documentCreationInfo),
                    user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate SPDX Document Creation Info " + documentCreationInfo.getId() + " for User "
                    + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {
        try {
            ModerationClients.get().createSpdxDocumentCreationInfoDeleteRequest(
                    DocumentCreationInformationConverter.fromThrift(documentCreationInfo),
                    user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate delete SPDX document creation information" + documentCreationInfo.getId()
                    + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation updateSpdxDocumentCreationInfoFromModerationRequest(
            org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation documentCreationInfo,
            org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation documentCreationInfoAdditions,
            org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation documentCreationInfoDeletions) {
        if (documentCreationInfoAdditions == null) {
            documentCreationInfoAdditions = new org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation();
        }
        if (documentCreationInfoDeletions == null) {
            documentCreationInfoDeletions = new org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation();
        }
        for (FieldAccessor<org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation> field : PojoFields
                .of(org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation.class)) {
            if (!field.isSet(documentCreationInfoAdditions) && !field.isSet(documentCreationInfoDeletions)) {
                continue;
            }
            switch (field.name()) {
                case "id":
                case "revision":
                case "type":
                    break;
                case "externalDocumentRefs":
                    documentCreationInfo.setExternalDocumentRefs(mergeElementSet(
                            documentCreationInfo.getExternalDocumentRefs(),
                            documentCreationInfoAdditions.getExternalDocumentRefs(),
                            documentCreationInfoDeletions.getExternalDocumentRefs(),
                            org.eclipse.sw360.datahandler.services.spdx.ExternalDocumentReferences.class));
                    break;
                case "creator":
                    documentCreationInfo.setCreator(mergeElementSet(
                            documentCreationInfo.getCreator(),
                            documentCreationInfoAdditions.getCreator(),
                            documentCreationInfoDeletions.getCreator(),
                            org.eclipse.sw360.datahandler.services.spdx.Creator.class));
                    break;
                default:
                    applyBasicField(field, documentCreationInfo, documentCreationInfoAdditions,
                            documentCreationInfoDeletions);
            }
        }
        return documentCreationInfo;
    }

}
