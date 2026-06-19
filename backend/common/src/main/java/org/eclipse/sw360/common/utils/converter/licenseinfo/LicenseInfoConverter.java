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

import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfo;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseInfoConverter {

    private LicenseInfoConverter() {}

    public static LicenseInfo fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseInfo pojo = new LicenseInfo();
        if (thrift.isSetAssessmentSummary()) {
            pojo.setAssessmentSummary(thrift.getAssessmentSummary());
        }
        if (thrift.isSetComponentName()) {
            pojo.setComponentName(thrift.getComponentName());
        }
        if (thrift.isSetConcludedLicenseIds()) {
            pojo.setConcludedLicenseIds(ThriftCollectionConverter.mapSet(thrift.getConcludedLicenseIds(), e -> e));
        }
        if (thrift.isSetCopyrights()) {
            pojo.setCopyrights(ThriftCollectionConverter.mapSet(thrift.getCopyrights(), e -> e));
        }
        if (thrift.isSetCopyrightsWithFilesHash()) {
            pojo.setCopyrightsWithFilesHash(ThriftCollectionConverter.mapMap(thrift.getCopyrightsWithFilesHash(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (thrift.isSetFilenames()) {
            pojo.setFilenames(ThriftCollectionConverter.mapList(thrift.getFilenames(), e -> e));
        }
        if (thrift.isSetLicenseNamesWithTexts()) {
            pojo.setLicenseNamesWithTexts(ThriftCollectionConverter.mapSet(thrift.getLicenseNamesWithTexts(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseNameWithTextConverter.fromThrift(e)));
        }
        if (thrift.isSetSha1Hash()) {
            pojo.setSha1Hash(thrift.getSha1Hash());
        }
        if (thrift.isSetTotalObligations()) {
            pojo.setTotalObligations(thrift.getTotalObligations());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo toThrift(LicenseInfo pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo();
        if (pojo.getAssessmentSummary() != null) {
            thrift.setAssessmentSummary(pojo.getAssessmentSummary());
        }
        if (pojo.getComponentName() != null) {
            thrift.setComponentName(pojo.getComponentName());
        }
        if (pojo.getConcludedLicenseIds() != null) {
            thrift.setConcludedLicenseIds(ThriftCollectionConverter.mapSet(pojo.getConcludedLicenseIds(), e -> e));
        }
        if (pojo.getCopyrights() != null) {
            thrift.setCopyrights(ThriftCollectionConverter.mapSet(pojo.getCopyrights(), e -> e));
        }
        if (pojo.getCopyrightsWithFilesHash() != null) {
            thrift.setCopyrightsWithFilesHash(ThriftCollectionConverter.mapMap(pojo.getCopyrightsWithFilesHash(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (pojo.getFilenames() != null) {
            thrift.setFilenames(ThriftCollectionConverter.mapList(pojo.getFilenames(), e -> e));
        }
        if (pojo.getLicenseNamesWithTexts() != null) {
            thrift.setLicenseNamesWithTexts(ThriftCollectionConverter.mapSet(pojo.getLicenseNamesWithTexts(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseNameWithTextConverter.toThrift(e)));
        }
        if (pojo.getSha1Hash() != null) {
            thrift.setSha1Hash(pojo.getSha1Hash());
        }
        if (pojo.getTotalObligations() != null) {
            thrift.setTotalObligations(pojo.getTotalObligations());
        }
        return thrift;
    }
}
