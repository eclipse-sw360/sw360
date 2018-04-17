/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
                copyField(document, copy, _Fields.RISKS);
            case SUMMARY:
                copyField(document, copy, _Fields.LICENSE_TYPE);
            default:
                copyField(document, copy, _Fields.ID);
                copy.setShortname(document.getId());
                copyField(document, copy, _Fields.FULLNAME);
                copyField(document, copy, _Fields.LICENSE_TYPE_DATABASE_ID);
        }

        return copy;
    }


}
