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
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

public class SpdxDocumentSummary extends DocumentSummary<SPDXDocument> {

    @Override
    protected SPDXDocument summary(SummaryType type, SPDXDocument document) {
        // Copy required details
        SPDXDocument copy = new SPDXDocument();

        switch (type) {
            case SUMMARY:
                copyField(document, copy, SPDXDocument._Fields.ID);
                copyField(document, copy, SPDXDocument._Fields.SPDX_DOCUMENT_CREATION_INFO_ID);
                copyField(document, copy, SPDXDocument._Fields.SPDX_PACKAGE_INFO_IDS);
                copyField(document, copy, SPDXDocument._Fields.SPDX_FILE_INFO_IDS);
            default:
                break;
        }

        return copy;
    }

}
