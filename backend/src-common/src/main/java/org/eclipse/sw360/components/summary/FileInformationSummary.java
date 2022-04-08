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

import org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.*;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

public class FileInformationSummary extends DocumentSummary<FileInformation> {

    @Override
    protected FileInformation summary(SummaryType type, FileInformation document) {
        // Copy required details
        FileInformation copy = new FileInformation();

        switch (type) {
            case SUMMARY:
                copyField(document, copy, FileInformation._Fields.ID);
                copyField(document, copy, FileInformation._Fields.SPDXID);
                copyField(document, copy, FileInformation._Fields.FILE_NAME);
                break;
            default:
                break;
        }

        return copy;
    }

}
