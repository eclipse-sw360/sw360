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

import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseNameWithTextConverter {

    private LicenseNameWithTextConverter() {}

    public static LicenseNameWithText fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText thrift) {
        if (thrift == null) {
            return null;
        }
        LicenseNameWithText pojo = new LicenseNameWithText();
        if (thrift.isSetAcknowledgements()) {
            pojo.setAcknowledgements(thrift.getAcknowledgements());
        }
        if (thrift.isSetLicenseName()) {
            pojo.setLicenseName(thrift.getLicenseName());
        }
        if (thrift.isSetLicenseSpdxId()) {
            pojo.setLicenseSpdxId(thrift.getLicenseSpdxId());
        }
        if (thrift.isSetLicenseText()) {
            pojo.setLicenseText(thrift.getLicenseText());
        }
        if (thrift.isSetObligationsAtProject()) {
            pojo.setObligationsAtProject(ThriftCollectionConverter.mapSet(thrift.getObligationsAtProject(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationAtProjectConverter.fromThrift(e)));
        }
        if (thrift.isSetSourceFiles()) {
            pojo.setSourceFiles(ThriftCollectionConverter.mapSet(thrift.getSourceFiles(), e -> e));
        }
        if (thrift.isSetSourceFilesHash()) {
            pojo.setSourceFilesHash(ThriftCollectionConverter.mapSet(thrift.getSourceFilesHash(), e -> e));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText toThrift(LicenseNameWithText pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText();
        if (pojo.getAcknowledgements() != null) {
            thrift.setAcknowledgements(pojo.getAcknowledgements());
        }
        if (pojo.getLicenseName() != null) {
            thrift.setLicenseName(pojo.getLicenseName());
        }
        if (pojo.getLicenseSpdxId() != null) {
            thrift.setLicenseSpdxId(pojo.getLicenseSpdxId());
        }
        if (pojo.getLicenseText() != null) {
            thrift.setLicenseText(pojo.getLicenseText());
        }
        if (pojo.getObligationsAtProject() != null) {
            thrift.setObligationsAtProject(ThriftCollectionConverter.mapSet(pojo.getObligationsAtProject(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationAtProjectConverter.toThrift(e)));
        }
        if (pojo.getSourceFiles() != null) {
            thrift.setSourceFiles(ThriftCollectionConverter.mapSet(pojo.getSourceFiles(), e -> e));
        }
        if (pojo.getSourceFilesHash() != null) {
            thrift.setSourceFilesHash(ThriftCollectionConverter.mapSet(pojo.getSourceFilesHash(), e -> e));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
