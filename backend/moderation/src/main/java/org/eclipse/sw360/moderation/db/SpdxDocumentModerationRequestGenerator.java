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
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;

public class SpdxDocumentModerationRequestGenerator extends ModerationRequestGenerator<SPDXDocument> {
    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, SPDXDocument updateSpdx, SPDXDocument actualSpdx) {
        updateDocument = updateSpdx;
        actualDocument = actualSpdx;

        documentAdditions = new SPDXDocument();
        documentDeletions = new SPDXDocument();
        documentAdditions.setId(updateSpdx.getId());
        documentDeletions.setId(actualSpdx.getId());

        for (FieldAccessor<SPDXDocument> field : PojoFields.of(SPDXDocument.class)) {
            if (field.get(actualSpdx) == null) {
                field.set(documentAdditions, field.get(updateSpdx));
            } else if (field.get(updateSpdx) == null) {
                field.set(documentDeletions, field.get(actualSpdx));
            } else if (!field.get(actualSpdx).equals(field.get(updateSpdx))) {
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
        request.setSpdxDocumentAdditions(documentAdditions);
        request.setSpdxDocumentDeletions(documentDeletions);
        return request;
    }
}
