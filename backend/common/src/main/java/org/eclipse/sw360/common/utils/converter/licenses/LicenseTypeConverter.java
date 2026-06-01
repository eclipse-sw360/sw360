/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenses;

import org.eclipse.sw360.datahandler.services.licenses.LicenseType;

public final class LicenseTypeConverter {

    private LicenseTypeConverter() {}

    public static LicenseType fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.LicenseType thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseType pojo = new LicenseType();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseType()) {
            pojo.setLicenseType(thrift.getLicenseType());
        }
        if (thrift.isSetLicenseTypeId()) {
            pojo.setLicenseTypeId(thrift.getLicenseTypeId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.LicenseType toThrift(LicenseType pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.LicenseType thrift = new org.eclipse.sw360.datahandler.thrift.licenses.LicenseType();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseType() != null) {
            thrift.setLicenseType(pojo.getLicenseType());
        }
        if (pojo.getLicenseTypeId() != null) {
            thrift.setLicenseTypeId(pojo.getLicenseTypeId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
