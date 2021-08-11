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
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

/**
 * Moderation for the SPDX Document service
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */

public class SpdxDocumentCreationInfoModerator extends Moderator<DocumentCreationInformation._Fields, DocumentCreationInformation> {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoModerator.class);


    public SpdxDocumentCreationInfoModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public SpdxDocumentCreationInfoModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {

        try {
            log.info("makeModerationClient : AAAAAAAAAAAAAA");
            ModerationService.Iface client = thriftClients.makeModerationClient();
            log.info("createSpdxDocumentCreationInfoRequest : AAAAAAAAAAAAAA");
            client.createSpdxDocumentCreationInfoRequest(documentCreationInfo, user);
            log.info("return : BBBBBBBBBBBBBBBB");
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate SPDX Document Creation Info " + documentCreationInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSpdxDocumentCreationInfoDeleteRequest(documentCreationInfo, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete SPDX document " + documentCreationInfo.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public DocumentCreationInformation updateSPDXDocumentFromModerationRequest(DocumentCreationInformation documentCreationInfo,
                                                      DocumentCreationInformation documentCreationInfoAdditions,
                                                      DocumentCreationInformation documentCreationInfoDeletions,
                                                      String department) {
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
            if(documentCreationInfoAdditions.getFieldValue(field) == null && documentCreationInfoDeletions.getFieldValue(field) == null){
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
                case DOCUMENT_CREATION_INFORMATION_ID:
                case DATA_LICENSE:
                case NAME:
                case DOCUMENT_NAMESPACE:
                case EXTERNAL_DOCUMENT_REFS:
                case LICENSE_LIST_VERSION:
                case CREATOR:
                case CREATED:
                case CREATOR_COMMENT:
                case DOCUMENT_COMMENT:
                default:
                documentCreationInfo = updateBasicField(field, DocumentCreationInformation.metaDataMap.get(field), documentCreationInfo, documentCreationInfoAdditions, documentCreationInfoDeletions);
            }

        }
        return documentCreationInfo;
    }

}

