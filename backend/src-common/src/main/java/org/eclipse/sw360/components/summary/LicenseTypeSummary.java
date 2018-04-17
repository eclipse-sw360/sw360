/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
