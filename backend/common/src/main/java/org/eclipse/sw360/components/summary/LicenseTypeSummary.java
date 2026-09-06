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

import org.eclipse.sw360.datahandler.services.licenses.LicenseType;

/**
 *
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class LicenseTypeSummary extends DocumentSummary<LicenseType> {

    @Override
    protected LicenseType summary(SummaryType type, LicenseType document) {
        LicenseType copy = new LicenseType();

        switch (type) {
            case EXPORT_SUMMARY:
                copy.setLicenseType(document.getLicenseType());
                copy.setLicenseTypeId(document.getLicenseTypeId());
                copy.setId(document.getId());
                break;
            default:
        }

        return copy;
    }
}
