/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.spdx;

import org.eclipse.sw360.datahandler.services.spdx.PackageVerificationCode;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class PackageVerificationCodeConverter {

    private PackageVerificationCodeConverter() {}

    public static PackageVerificationCode fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode thrift) {
        if (thrift == null) {
            return null;
        }
        PackageVerificationCode pojo = new PackageVerificationCode();
        if (thrift.isSetExcludedFiles()) {
            pojo.setExcludedFiles(ThriftCollectionConverter.mapSet(thrift.getExcludedFiles(), e -> e));
        }
        if (thrift.isSetValue()) {
            pojo.setValue(thrift.getValue());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode toThrift(PackageVerificationCode pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode thrift = new org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode();
        if (pojo.getExcludedFiles() != null) {
            thrift.setExcludedFiles(ThriftCollectionConverter.mapSet(pojo.getExcludedFiles(), e -> e));
        }
        if (pojo.getValue() != null) {
            thrift.setValue(pojo.getValue());
        }
        return thrift;
    }
}
