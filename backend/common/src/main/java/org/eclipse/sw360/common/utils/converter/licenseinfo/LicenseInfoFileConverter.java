/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenseinfo;

import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoFile;

public final class LicenseInfoFileConverter {

    private LicenseInfoFileConverter() {}

    public static LicenseInfoFile fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseInfoFile pojo = new LicenseInfoFile();
        if (thrift.isSetOutputFormatInfo()) {
            pojo.setOutputFormatInfo(org.eclipse.sw360.common.utils.converter.licenseinfo.OutputFormatInfoConverter.fromThrift(thrift.getOutputFormatInfo()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile toThrift(LicenseInfoFile pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile();
        if (pojo.getOutputFormatInfo() != null) {
            thrift.setOutputFormatInfo(org.eclipse.sw360.common.utils.converter.licenseinfo.OutputFormatInfoConverter.toThrift(pojo.getOutputFormatInfo()));
        }
        return thrift;
    }
}
