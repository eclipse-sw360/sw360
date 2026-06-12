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

import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationAtProject;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ObligationAtProjectConverter {

    private ObligationAtProjectConverter() {}

    public static ObligationAtProject fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationAtProject pojo = new ObligationAtProject();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseIDs()) {
            pojo.setLicenseIDs(ThriftCollectionConverter.mapList(thrift.getLicenseIDs(), e -> e));
        }
        if (thrift.isSetObligationStatusInfo()) {
            pojo.setObligationStatusInfo(org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.fromThrift(thrift.getObligationStatusInfo()));
        }
        if (thrift.isSetText()) {
            pojo.setText(thrift.getText());
        }
        if (thrift.isSetTopic()) {
            pojo.setTopic(thrift.getTopic());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject toThrift(ObligationAtProject pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseIDs() != null) {
            thrift.setLicenseIDs(ThriftCollectionConverter.mapList(pojo.getLicenseIDs(), e -> e));
        }
        if (pojo.getObligationStatusInfo() != null) {
            thrift.setObligationStatusInfo(org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.toThrift(pojo.getObligationStatusInfo()));
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
        }
        if (pojo.getTopic() != null) {
            thrift.setTopic(pojo.getTopic());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
