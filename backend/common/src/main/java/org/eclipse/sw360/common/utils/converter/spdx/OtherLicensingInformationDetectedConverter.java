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

import org.eclipse.sw360.datahandler.services.spdx.OtherLicensingInformationDetected;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class OtherLicensingInformationDetectedConverter {

    private OtherLicensingInformationDetectedConverter() {}

    public static OtherLicensingInformationDetected fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected thrift) {
        if (thrift == null) {
            return null;
        }
        OtherLicensingInformationDetected pojo = new OtherLicensingInformationDetected();
        if (thrift.isSetExtractedText()) {
            pojo.setExtractedText(thrift.getExtractedText());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetLicenseComment()) {
            pojo.setLicenseComment(thrift.getLicenseComment());
        }
        if (thrift.isSetLicenseCrossRefs()) {
            pojo.setLicenseCrossRefs(ThriftCollectionConverter.mapSet(thrift.getLicenseCrossRefs(), e -> e));
        }
        if (thrift.isSetLicenseId()) {
            pojo.setLicenseId(thrift.getLicenseId());
        }
        if (thrift.isSetLicenseName()) {
            pojo.setLicenseName(thrift.getLicenseName());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected toThrift(OtherLicensingInformationDetected pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected thrift = new org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected();
        if (pojo.getExtractedText() != null) {
            thrift.setExtractedText(pojo.getExtractedText());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getLicenseComment() != null) {
            thrift.setLicenseComment(pojo.getLicenseComment());
        }
        if (pojo.getLicenseCrossRefs() != null) {
            thrift.setLicenseCrossRefs(ThriftCollectionConverter.mapSet(pojo.getLicenseCrossRefs(), e -> e));
        }
        if (pojo.getLicenseId() != null) {
            thrift.setLicenseId(pojo.getLicenseId());
        }
        if (pojo.getLicenseName() != null) {
            thrift.setLicenseName(pojo.getLicenseName());
        }
        return thrift;
    }
}
