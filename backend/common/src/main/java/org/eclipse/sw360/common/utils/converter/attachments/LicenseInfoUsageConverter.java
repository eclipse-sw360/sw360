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

import org.eclipse.sw360.datahandler.services.attachments.LicenseInfoUsage;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseInfoUsageConverter {

    private LicenseInfoUsageConverter() {}

    public static LicenseInfoUsage fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseInfoUsage pojo = new LicenseInfoUsage();
        if (thrift.isSetExcludedLicenseIds()) {
            pojo.setExcludedLicenseIds(ThriftCollectionConverter.mapSet(thrift.getExcludedLicenseIds(), e -> e));
        }
        if (thrift.isSetIncludeConcludedLicense()) {
            pojo.setIncludeConcludedLicense(thrift.isIncludeConcludedLicense());
        }
        if (thrift.isSetProjectPath()) {
            pojo.setProjectPath(thrift.getProjectPath());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage toThrift(LicenseInfoUsage pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage thrift = new org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage();
        if (pojo.getExcludedLicenseIds() != null) {
            thrift.setExcludedLicenseIds(ThriftCollectionConverter.mapSet(pojo.getExcludedLicenseIds(), e -> e));
        }
        if (pojo.getIncludeConcludedLicense() != null) {
            thrift.setIncludeConcludedLicense(pojo.getIncludeConcludedLicense());
        }
        if (pojo.getProjectPath() != null) {
            thrift.setProjectPath(pojo.getProjectPath());
        }
        return thrift;
    }
}
