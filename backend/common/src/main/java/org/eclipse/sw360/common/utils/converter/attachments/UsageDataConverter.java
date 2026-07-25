/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.UsageData;

/**
 * Maps thrift {@code UsageData} (a union) to/from the service-api POJO.
 * Only the currently-set union arm may be read; Jackson {@code convertValue} is unsafe here.
 */
public final class UsageDataConverter {

    private UsageDataConverter() {}

    public static UsageData fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.UsageData thrift) {
        if (thrift == null) {
            return null;
        }
        UsageData pojo = new UsageData();
        if (thrift.isSetLicenseInfo()) {
            pojo.setLicenseInfo(LicenseInfoUsageConverter.fromThrift(thrift.getLicenseInfo()));
        } else if (thrift.isSetSourcePackage()) {
            pojo.setSourcePackage(SourcePackageUsageConverter.fromThrift(thrift.getSourcePackage()));
        } else if (thrift.isSetManuallySet()) {
            pojo.setManuallySet(ManuallySetUsageConverter.fromThrift(thrift.getManuallySet()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.UsageData toThrift(UsageData pojo) {
        if (pojo == null) {
            return null;
        }
        if (pojo.getLicenseInfo() != null) {
            return org.eclipse.sw360.datahandler.thrift.attachments.UsageData.licenseInfo(
                    LicenseInfoUsageConverter.toThrift(pojo.getLicenseInfo()));
        }
        if (pojo.getSourcePackage() != null) {
            return org.eclipse.sw360.datahandler.thrift.attachments.UsageData.sourcePackage(
                    SourcePackageUsageConverter.toThrift(pojo.getSourcePackage()));
        }
        if (pojo.getManuallySet() != null) {
            return org.eclipse.sw360.datahandler.thrift.attachments.UsageData.manuallySet(
                    ManuallySetUsageConverter.toThrift(pojo.getManuallySet()));
        }
        return new org.eclipse.sw360.datahandler.thrift.attachments.UsageData();
    }
}
