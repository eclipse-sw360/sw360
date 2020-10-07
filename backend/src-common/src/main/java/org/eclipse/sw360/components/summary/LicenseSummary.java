/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.licenses.License;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;
import static org.eclipse.sw360.datahandler.thrift.licenses.License._Fields;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseSummary extends DocumentSummary<License> {

    @Override
    protected License summary(SummaryType type, License document) {
        // Copy required details
        License copy = new License();

        switch (type) {
            case EXPORT_SUMMARY:
                copyField(document, copy, _Fields.GPLV2_COMPAT);
                copyField(document, copy, _Fields.REVIEWDATE);
            case SUMMARY:
                copyField(document, copy, _Fields.LICENSE_TYPE);
            default:
                copyField(document, copy, _Fields.ID);
                copy.setShortname(document.getId());
                copyField(document, copy, _Fields.FULLNAME);
                copyField(document, copy, _Fields.LICENSE_TYPE_DATABASE_ID);
                copyField(document, copy, _Fields.CHECKED);
        }

        return copy;
    }


}
