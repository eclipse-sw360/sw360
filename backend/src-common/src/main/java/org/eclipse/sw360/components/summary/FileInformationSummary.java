/*
Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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
import static org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformation._Fields;

/**
 * Created by HieuPV on 22/07/21.
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class FileInformationSummary extends DocumentSummary<FileInformation> {

    @Override
    protected FileInformation summary(SummaryType type, FileInformation document) {
        // Copy required details
        FileInformation copy = new FileInformation();

        switch (type) {
            case EXPORT_SUMMARY:
            case SUMMARY:
            default:
        }

        return copy;
    }


}
