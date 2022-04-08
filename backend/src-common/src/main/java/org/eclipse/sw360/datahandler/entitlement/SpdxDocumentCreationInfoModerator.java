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

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

public class SpdxDocumentCreationInfoModerator
        extends Moderator<DocumentCreationInformation._Fields, DocumentCreationInformation> {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoModerator.class);

    public SpdxDocumentCreationInfoModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public SpdxDocumentCreationInfoModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSpdxDocumentCreationInfoRequest(documentCreationInfo, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate SPDX Document Creation Info " + documentCreationInfo.getId() + " for User "
                    + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSpdxDocumentCreationInfoDeleteRequest(documentCreationInfo, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete SPDX document creation information" + documentCreationInfo.getId() + " for User "
                    + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public DocumentCreationInformation updateSpdxDocumentCreationInfoFromModerationRequest(
            DocumentCreationInformation documentCreationInfo, DocumentCreationInformation documentCreationInfoAdditions,
            DocumentCreationInformation documentCreationInfoDeletions) {
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
            if (documentCreationInfoAdditions.getFieldValue(field) == null
                    && documentCreationInfoDeletions.getFieldValue(field) == null) {
                continue;
            }
            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                    break;
                case EXTERNAL_DOCUMENT_REFS:
                    documentCreationInfo = updateExternalDocumentRefs(documentCreationInfo, documentCreationInfoAdditions, documentCreationInfoDeletions);
                    break;
                case CREATOR:
                    documentCreationInfo = updateCreator(documentCreationInfo, documentCreationInfoAdditions, documentCreationInfoDeletions);
                    break;
                default:
                    documentCreationInfo = updateBasicField(field, DocumentCreationInformation.metaDataMap.get(field),
                            documentCreationInfo, documentCreationInfoAdditions, documentCreationInfoDeletions);
            }

        }
        return documentCreationInfo;
    }

    private DocumentCreationInformation updateExternalDocumentRefs(DocumentCreationInformation documentCreationInfo,
                                                                    DocumentCreationInformation documentCreationInfoAdditions,
                                                                    DocumentCreationInformation documentCreationInfoDeletions) {
        Set<ExternalDocumentReferences> actuals = documentCreationInfo.getExternalDocumentRefs();
        Iterator<ExternalDocumentReferences> additionsIterator = documentCreationInfoAdditions.getExternalDocumentRefsIterator();
        Iterator<ExternalDocumentReferences> deletionsIterator = documentCreationInfoDeletions.getExternalDocumentRefsIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return documentCreationInfo;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            ExternalDocumentReferences additions = additionsIterator.next();
            ExternalDocumentReferences actual = new ExternalDocumentReferences();
            for (ExternalDocumentReferences._Fields field : ExternalDocumentReferences._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while (deletionsIterator.hasNext()) {
            ExternalDocumentReferences deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }
        documentCreationInfo.setExternalDocumentRefs(actuals);
        return documentCreationInfo;
    }

    private DocumentCreationInformation updateCreator(DocumentCreationInformation documentCreationInfo,
                                    DocumentCreationInformation documentCreationInfoAdditions,
                                    DocumentCreationInformation documentCreationInfoDeletions) {
        Set<Creator> actuals = documentCreationInfo.getCreator();
        Iterator<Creator> additionsIterator = documentCreationInfoAdditions.getCreatorIterator();
        Iterator<Creator> deletionsIterator = documentCreationInfoDeletions.getCreatorIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return documentCreationInfo;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            Creator additions = additionsIterator.next();
            Creator actual = new Creator();
            for (Creator._Fields field : Creator._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while (deletionsIterator.hasNext()) {
            Creator deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }
        documentCreationInfo.setCreator(actuals);
        return documentCreationInfo;
    }

}
