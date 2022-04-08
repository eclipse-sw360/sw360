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

import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

public class SpdxDocumentModerationRequestGenerator extends ModerationRequestGenerator<SPDXDocument._Fields, SPDXDocument> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, SPDXDocument updateSpdx, SPDXDocument actualSpdx){
        updateDocument = updateSpdx;
        actualDocument = actualSpdx;

        documentAdditions = new SPDXDocument();
        documentDeletions = new SPDXDocument();
        //required fields:
        documentAdditions.setId(updateSpdx.getId());
        documentDeletions.setId(actualSpdx.getId());

        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {

            if(actualSpdx.getFieldValue(field) == null){
                    documentAdditions.setFieldValue(field, updateSpdx.getFieldValue(field));
            } else if (updateSpdx.getFieldValue(field) == null){
                    documentDeletions.setFieldValue(field, actualSpdx.getFieldValue(field));
            } else if(!actualSpdx.getFieldValue(field).equals(updateSpdx.getFieldValue(field))) {
                switch (field) {
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case CREATED_BY:
                        break;
                    default:
                        dealWithBaseTypes(field, SPDXDocument.metaDataMap.get(field));
                }
            }
        }
        request.setSPDXDocumentAdditions(documentAdditions);
        request.setSPDXDocumentDeletions(documentDeletions);
        return request;
    }
}
