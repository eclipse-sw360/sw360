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

import org.eclipse.sw360.datahandler.services.licenses.LicenseObligationList;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseObligationListConverter {

    private LicenseObligationListConverter() {}

    public static LicenseObligationList fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseObligationList pojo = new LicenseObligationList();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseId()) {
            pojo.setLicenseId(thrift.getLicenseId());
        }
        if (thrift.isSetLinkedObligations()) {
            pojo.setLinkedObligations(ThriftCollectionConverter.mapMap(thrift.getLinkedObligations(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList toThrift(LicenseObligationList pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList thrift = new org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseId() != null) {
            thrift.setLicenseId(pojo.getLicenseId());
        }
        if (pojo.getLinkedObligations() != null) {
            thrift.setLinkedObligations(ThriftCollectionConverter.mapMap(pojo.getLinkedObligations(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter.toThrift(mapValue)));
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
