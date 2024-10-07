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

import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

public class PackageInformationSummary extends DocumentSummary<PackageInformation> {

    @Override
    protected PackageInformation summary(SummaryType type, PackageInformation document) {
        // Copy required details
        PackageInformation copy = new PackageInformation();

        switch (type) {
            case SUMMARY:
                copyField(document, copy, PackageInformation._Fields.ID);
                copyField(document, copy, PackageInformation._Fields.SPDXID);
                copyField(document, copy, PackageInformation._Fields.NAME);
                break;
            default:
                break;
        }

        return copy;
    }

}
