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

import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class LicenseInfoParsingResultConverter {

    private LicenseInfoParsingResultConverter() {}

    public static LicenseInfoParsingResult fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseInfoParsingResult pojo = new LicenseInfoParsingResult();
        if (thrift.isSetAttachmentContentId()) {
            pojo.setAttachmentContentId(thrift.getAttachmentContentId());
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(thrift.getComponentType());
        }
        if (thrift.isSetLicenseInfo()) {
            pojo.setLicenseInfo(org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoConverter.fromThrift(thrift.getLicenseInfo()));
        }
        if (thrift.isSetMessage()) {
            pojo.setMessage(thrift.getMessage());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetRelease()) {
            pojo.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getRelease()));
        }
        if (thrift.isSetStatus()) {
            pojo.setStatus(EnumConverter.fromThrift(thrift.getStatus(), org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoRequestStatus.class));
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(thrift.getVendor());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult toThrift(LicenseInfoParsingResult pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult();
        if (pojo.getAttachmentContentId() != null) {
            thrift.setAttachmentContentId(pojo.getAttachmentContentId());
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(pojo.getComponentType());
        }
        if (pojo.getLicenseInfo() != null) {
            thrift.setLicenseInfo(org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoConverter.toThrift(pojo.getLicenseInfo()));
        }
        if (pojo.getMessage() != null) {
            thrift.setMessage(pojo.getMessage());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getRelease() != null) {
            thrift.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getRelease()));
        }
        if (pojo.getStatus() != null) {
            thrift.setStatus(EnumConverter.toThrift(pojo.getStatus(), org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus.class));
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(pojo.getVendor());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
