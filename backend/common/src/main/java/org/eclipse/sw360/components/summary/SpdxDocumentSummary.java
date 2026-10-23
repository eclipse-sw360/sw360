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

import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;

public class SpdxDocumentSummary extends DocumentSummary<SPDXDocument> {

    @Override
    protected SPDXDocument summary(SummaryType type, SPDXDocument document) {
        SPDXDocument copy = new SPDXDocument();

        switch (type) {
            case SUMMARY:
                copy.setId(document.getId());
                copy.setSpdxDocumentCreationInfoId(document.getSpdxDocumentCreationInfoId());
                copy.setSpdxPackageInfoIds(document.getSpdxPackageInfoIds());
                copy.setSpdxFileInfoIds(document.getSpdxFileInfoIds());
                break;
            default:
                break;
        }

        return copy;
    }

}
