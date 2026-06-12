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

import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseObligationsStatusInfoConverter {

    private LicenseObligationsStatusInfoConverter() {}

    public static LicenseObligationsStatusInfo fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseObligationsStatusInfo pojo = new LicenseObligationsStatusInfo();
        if (thrift.isSetLicenseInfoResults()) {
            pojo.setLicenseInfoResults(ThriftCollectionConverter.mapList(thrift.getLicenseInfoResults(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoParsingResultConverter.fromThrift(e)));
        }
        if (thrift.isSetObligationStatusMap()) {
            pojo.setObligationStatusMap(ThriftCollectionConverter.mapMap(thrift.getObligationStatusMap(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.fromThrift(mapValue)));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo toThrift(LicenseObligationsStatusInfo pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo();
        if (pojo.getLicenseInfoResults() != null) {
            thrift.setLicenseInfoResults(ThriftCollectionConverter.mapList(pojo.getLicenseInfoResults(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoParsingResultConverter.toThrift(e)));
        }
        if (pojo.getObligationStatusMap() != null) {
            thrift.setObligationStatusMap(ThriftCollectionConverter.mapMap(pojo.getObligationStatusMap(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.toThrift(mapValue)));
        }
        return thrift;
    }
}
