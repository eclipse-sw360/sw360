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

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;

public class SpdxDocumentCreationInfoModerationRequestGenerator extends ModerationRequestGenerator<DocumentCreationInformation> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, DocumentCreationInformation updateDocumentCreationInfo, DocumentCreationInformation actualDocumentCreationInfo) {
        updateDocument = updateDocumentCreationInfo;
        actualDocument = actualDocumentCreationInfo;

        documentAdditions = new DocumentCreationInformation();
        documentDeletions = new DocumentCreationInformation();
        documentAdditions.setId(updateDocument.getId());
        documentDeletions.setId(actualDocument.getId());
        for (FieldAccessor<DocumentCreationInformation> field : PojoFields.of(DocumentCreationInformation.class)) {
            if (field.get(actualDocument) == null) {
                field.set(documentAdditions, field.get(updateDocument));
            } else if (field.get(updateDocument) == null) {
                field.set(documentDeletions, field.get(actualDocument));
            } else if (!field.get(actualDocument).equals(field.get(updateDocument))) {
                switch (field.name()) {
                    case "permissions":
                    case "documentState":
                    case "createdBy":
                        break;
                    default:
                        dealWithBaseTypes(field);
                }
            }
        }
        request.setDocumentCreationInfoAdditions(documentAdditions);
        request.setDocumentCreationInfoDeletions(documentDeletions);
        return request;
    }
}
