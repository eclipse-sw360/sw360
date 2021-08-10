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
package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */

public class SpdxDocumentModerationRequestGenerator extends ModerationRequestGenerator<SPDXDocument._Fields, SPDXDocument> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, SPDXDocument updateSpdx, SPDXDocument actualSpdx){
        updateDocument = updateSpdx;
        actualDocument = actualSpdx;

        documentAdditions = new SPDXDocument();
        documentDeletions = new SPDXDocument();
        //required fields:
        documentAdditions.setId(updateSpdx.getId());
        documentAdditions.setReleaseId(updateSpdx.getReleaseId());
        documentDeletions.setId(actualSpdx.getId());
        documentDeletions.setReleaseId(actualSpdx.getReleaseId());

        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {

            if(actualSpdx.getFieldValue(field) == null){
                    documentAdditions.setFieldValue(field, updateSpdx.getFieldValue(field));
            } else if (updateSpdx.getFieldValue(field) == null){
                    documentDeletions.setFieldValue(field, actualSpdx.getFieldValue(field));
            } else if(!actualSpdx.getFieldValue(field).equals(updateSpdx.getFieldValue(field))) {
                switch (field) {
                    case REVISION:
                    case TYPE:
                    case RELEASE_ID:
                    case SPDX_DOCUMENT_CREATION_INFO_ID:
                    case SPDX_FILE_INFO_IDS:
                    case RELATIONSHIPS:
                    case ANNOTATIONS:
                    default:
                        dealWithBaseTypes(field, SPDXDocument.metaDataMap.get(field));
                }
            }
        }
        request.setSpdxAdditions(documentAdditions);
        request.setSpdxDeletions(documentDeletions);
        return request;
    }
}
