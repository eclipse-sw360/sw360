/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.projects;

import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ObligationStatusInfoConverter {

    private ObligationStatusInfoConverter() {}

    public static ObligationStatusInfo fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationStatusInfo pojo = new ObligationStatusInfo();
        if (thrift.isSetAction()) {
            pojo.setAction(thrift.getAction());
        }
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseIds()) {
            pojo.setLicenseIds(ThriftCollectionConverter.mapSet(thrift.getLicenseIds(), e -> e));
        }
        if (thrift.isSetModifiedBy()) {
            pojo.setModifiedBy(thrift.getModifiedBy());
        }
        if (thrift.isSetModifiedOn()) {
            pojo.setModifiedOn(thrift.getModifiedOn());
        }
        if (thrift.isSetObligationLevel()) {
            pojo.setObligationLevel(EnumConverter.fromThrift(thrift.getObligationLevel(), org.eclipse.sw360.datahandler.services.licenses.ObligationLevel.class));
        }
        if (thrift.isSetObligationType()) {
            pojo.setObligationType(EnumConverter.fromThrift(thrift.getObligationType(), org.eclipse.sw360.datahandler.services.licenses.ObligationType.class));
        }
        if (thrift.isSetReleaseIdToAcceptedCLI()) {
            pojo.setReleaseIdToAcceptedCLI(thrift.getReleaseIdToAcceptedCLI());
        }
        if (thrift.isSetReleases()) {
            pojo.setReleases(ThriftCollectionConverter.mapSet(thrift.getReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(e)));
        }
        if (thrift.isSetStatus()) {
            pojo.setStatus(EnumConverter.fromThrift(thrift.getStatus(), org.eclipse.sw360.datahandler.services.common.ObligationStatus.class));
        }
        if (thrift.isSetText()) {
            pojo.setText(thrift.getText());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo toThrift(ObligationStatusInfo pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo thrift = new org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo();
        if (pojo.getAction() != null) {
            thrift.setAction(pojo.getAction());
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseIds() != null) {
            thrift.setLicenseIds(ThriftCollectionConverter.mapSet(pojo.getLicenseIds(), e -> e));
        }
        if (pojo.getModifiedBy() != null) {
            thrift.setModifiedBy(pojo.getModifiedBy());
        }
        if (pojo.getModifiedOn() != null) {
            thrift.setModifiedOn(pojo.getModifiedOn());
        }
        if (pojo.getObligationLevel() != null) {
            thrift.setObligationLevel(EnumConverter.toThrift(pojo.getObligationLevel(), org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel.class));
        }
        if (pojo.getObligationType() != null) {
            thrift.setObligationType(EnumConverter.toThrift(pojo.getObligationType(), org.eclipse.sw360.datahandler.thrift.licenses.ObligationType.class));
        }
        if (pojo.getReleaseIdToAcceptedCLI() != null) {
            thrift.setReleaseIdToAcceptedCLI(pojo.getReleaseIdToAcceptedCLI());
        }
        if (pojo.getReleases() != null) {
            thrift.setReleases(ThriftCollectionConverter.mapSet(pojo.getReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(e)));
        }
        if (pojo.getStatus() != null) {
            thrift.setStatus(EnumConverter.toThrift(pojo.getStatus(), org.eclipse.sw360.datahandler.thrift.ObligationStatus.class));
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
        }
        return thrift;
    }
}
