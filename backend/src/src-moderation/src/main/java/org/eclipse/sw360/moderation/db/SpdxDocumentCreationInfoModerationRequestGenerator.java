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

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

public class SpdxDocumentCreationInfoModerationRequestGenerator extends ModerationRequestGenerator<DocumentCreationInformation._Fields, DocumentCreationInformation> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, DocumentCreationInformation updateDocumentCreationInfo, DocumentCreationInformation actualDocumentCreationInfo){
        updateDocument = updateDocumentCreationInfo;
        actualDocument = actualDocumentCreationInfo;

        documentAdditions = new DocumentCreationInformation();
        documentDeletions = new DocumentCreationInformation();
        //required fields:
        documentAdditions.setId(updateDocument.getId());
        documentDeletions.setId(actualDocument.getId());
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {

            if(actualDocument.getFieldValue(field) == null){
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
            } else if (updateDocument.getFieldValue(field) == null){
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
            } else if(!actualDocument.getFieldValue(field).equals(updateDocument.getFieldValue(field))) {
                switch (field) {
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case CREATED_BY:
                        break;
                    default:
                        dealWithBaseTypes(field, DocumentCreationInformation.metaDataMap.get(field));
                }
            }
        }
        request.setDocumentCreationInfoAdditions(documentAdditions);
        request.setDocumentCreationInfoDeletions(documentDeletions);
        return request;
    }
}
