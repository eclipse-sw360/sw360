/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.COTSDetails;

public final class COTSDetailsConverter {

    private COTSDetailsConverter() {}

    public static COTSDetails fromThrift(org.eclipse.sw360.datahandler.thrift.components.COTSDetails thrift) {
        if (thrift == null) {
            return null;
        }
        COTSDetails pojo = new COTSDetails();
        if (thrift.isSetClearingDeadline()) {
            pojo.setClearingDeadline(thrift.getClearingDeadline());
        }
        if (thrift.isSetContainsOSS()) {
            pojo.setContainsOSS(thrift.isContainsOSS());
        }
        if (thrift.isSetCotsResponsible()) {
            pojo.setCotsResponsible(thrift.getCotsResponsible());
        }
        if (thrift.isSetLicenseClearingReportURL()) {
            pojo.setLicenseClearingReportURL(thrift.getLicenseClearingReportURL());
        }
        if (thrift.isSetOssContractSigned()) {
            pojo.setOssContractSigned(thrift.isOssContractSigned());
        }
        if (thrift.isSetOssInformationURL()) {
            pojo.setOssInformationURL(thrift.getOssInformationURL());
        }
        if (thrift.isSetSourceCodeAvailable()) {
            pojo.setSourceCodeAvailable(thrift.isSourceCodeAvailable());
        }
        if (thrift.isSetUsageRightAvailable()) {
            pojo.setUsageRightAvailable(thrift.isUsageRightAvailable());
        }
        if (thrift.isSetUsedLicense()) {
            pojo.setUsedLicense(thrift.getUsedLicense());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.COTSDetails toThrift(COTSDetails pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.COTSDetails thrift = new org.eclipse.sw360.datahandler.thrift.components.COTSDetails();
        if (pojo.getClearingDeadline() != null) {
            thrift.setClearingDeadline(pojo.getClearingDeadline());
        }
        if (pojo.getContainsOSS() != null) {
            thrift.setContainsOSS(pojo.getContainsOSS());
        }
        if (pojo.getCotsResponsible() != null) {
            thrift.setCotsResponsible(pojo.getCotsResponsible());
        }
        if (pojo.getLicenseClearingReportURL() != null) {
            thrift.setLicenseClearingReportURL(pojo.getLicenseClearingReportURL());
        }
        if (pojo.getOssContractSigned() != null) {
            thrift.setOssContractSigned(pojo.getOssContractSigned());
        }
        if (pojo.getOssInformationURL() != null) {
            thrift.setOssInformationURL(pojo.getOssInformationURL());
        }
        if (pojo.getSourceCodeAvailable() != null) {
            thrift.setSourceCodeAvailable(pojo.getSourceCodeAvailable());
        }
        if (pojo.getUsageRightAvailable() != null) {
            thrift.setUsageRightAvailable(pojo.getUsageRightAvailable());
        }
        if (pojo.getUsedLicense() != null) {
            thrift.setUsedLicense(pojo.getUsedLicense());
        }
        return thrift;
    }
}
