/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;
import static org.eclipse.sw360.datahandler.thrift.licenses.LicenseType._Fields;

/**
 *
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class LicenseTypeSummary extends DocumentSummary<LicenseType> {

    @Override
    protected LicenseType summary(SummaryType type, LicenseType document) {
        // Copy required details
        LicenseType copy = new LicenseType();

        switch (type) {
            case EXPORT_SUMMARY:
                copyField(document, copy, _Fields.LICENSE_TYPE);
                copyField(document, copy, _Fields.LICENSE_TYPE_ID);
                copyField(document, copy, _Fields.ID);
            default:
        }

        return copy;
    }
}
