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

import org.eclipse.sw360.datahandler.services.licenses.License;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseSummary extends DocumentSummary<License> {

    @Override
    protected License summary(SummaryType type, License document) {
        License copy = new License();

        switch (type) {
            case EXPORT_SUMMARY:
                copy.setOsiApproved(document.getOsiApproved());
                copy.setFsfLibre(document.getFsfLibre());
                copy.setReviewdate(document.getReviewdate());
            case SUMMARY:
                copy.setLicenseType(document.getLicenseType());
            default:
                copy.setId(document.getId());
                copy.setShortname(document.getId());
                copy.setFullname(document.getFullname());
                copy.setLicenseTypeDatabaseId(document.getLicenseTypeDatabaseId());
                copy.setChecked(document.getChecked());
                copy.setText(document.getText());
        }

        return copy;
    }
}
