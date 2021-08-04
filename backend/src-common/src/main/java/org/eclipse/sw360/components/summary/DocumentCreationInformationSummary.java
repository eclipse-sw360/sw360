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
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;
import static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation._Fields;

/**
 * Created by HieuPV on 22/07/21.
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class DocumentCreationInformationSummary extends DocumentSummary<DocumentCreationInformation> {

    @Override
    protected DocumentCreationInformation summary(SummaryType type, DocumentCreationInformation document) {
        // Copy required details
        DocumentCreationInformation copy = new DocumentCreationInformation();

        switch (type) {
            case EXPORT_SUMMARY:
            case SUMMARY:
            default:
        }

        return copy;
    }


}
