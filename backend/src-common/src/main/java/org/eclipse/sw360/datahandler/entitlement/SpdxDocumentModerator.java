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
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
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
public class SpdxDocumentModerator extends Moderator<SPDXDocument._Fields, SPDXDocument> {

    private static final Logger log = LogManager.getLogger(SpdxDocumentModerator.class);


    public SpdxDocumentModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public SpdxDocumentModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSPDXDocumentRequest(spdx, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate SPDX Document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSPDXDocument(SPDXDocument spdx, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSPDXDocumentDeleteRequest(spdx, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete SPDX document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public SPDXDocument updateSPDXDocumentFromModerationRequest(SPDXDocument spdx,
                                                      SPDXDocument spdxAdditions,
                                                      SPDXDocument spdxDeletions,
                                                      String department) {
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            if(spdxAdditions.getFieldValue(field) == null && spdxDeletions.getFieldValue(field) == null){
                continue;
            }
            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                case RELEASE_ID:
                case SPDX_DOCUMENT_CREATION_INFO_ID:
                case SPDX_FILE_INFO_IDS:
                case RELATIONSHIPS:
                case ANNOTATIONS:
                default:
                    spdx = updateBasicField(field, SPDXDocument.metaDataMap.get(field), spdx, spdxAdditions, spdxDeletions);
            }

        }
        return spdx;
    }

}
