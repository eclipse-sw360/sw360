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

import org.eclipse.sw360.datahandler.services.components.ClearingInformation;

public final class ClearingInformationConverter {

    private ClearingInformationConverter() {}

    public static ClearingInformation fromThrift(org.eclipse.sw360.datahandler.thrift.components.ClearingInformation thrift) {
        if (thrift == null) {
            return null;
        }
        ClearingInformation pojo = new ClearingInformation();
        if (thrift.isSetAdditionalRequestInfo()) {
            pojo.setAdditionalRequestInfo(thrift.getAdditionalRequestInfo());
        }
        if (thrift.isSetBinariesOriginalFromCommunity()) {
            pojo.setBinariesOriginalFromCommunity(thrift.isBinariesOriginalFromCommunity());
        }
        if (thrift.isSetBinariesSelfMade()) {
            pojo.setBinariesSelfMade(thrift.isBinariesSelfMade());
        }
        if (thrift.isSetClearingStandard()) {
            pojo.setClearingStandard(thrift.getClearingStandard());
        }
        if (thrift.isSetClearingTeam()) {
            pojo.setClearingTeam(thrift.getClearingTeam());
        }
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetComponentClearingReport()) {
            pojo.setComponentClearingReport(thrift.isComponentClearingReport());
        }
        if (thrift.isSetComponentLicenseInformation()) {
            pojo.setComponentLicenseInformation(thrift.isComponentLicenseInformation());
        }
        if (thrift.isSetCountOfSecurityVn()) {
            pojo.setCountOfSecurityVn(thrift.getCountOfSecurityVn());
        }
        if (thrift.isSetEvaluated()) {
            pojo.setEvaluated(thrift.getEvaluated());
        }
        if (thrift.isSetExternalSupplierID()) {
            pojo.setExternalSupplierID(thrift.getExternalSupplierID());
        }
        if (thrift.isSetExternalUrl()) {
            pojo.setExternalUrl(thrift.getExternalUrl());
        }
        if (thrift.isSetFinalizedLicenseScanReport()) {
            pojo.setFinalizedLicenseScanReport(thrift.isFinalizedLicenseScanReport());
        }
        if (thrift.isSetLegalEvaluation()) {
            pojo.setLegalEvaluation(thrift.isLegalEvaluation());
        }
        if (thrift.isSetLicenseAgreement()) {
            pojo.setLicenseAgreement(thrift.isLicenseAgreement());
        }
        if (thrift.isSetLicenseScanReportResult()) {
            pojo.setLicenseScanReportResult(thrift.isLicenseScanReportResult());
        }
        if (thrift.isSetProcStart()) {
            pojo.setProcStart(thrift.getProcStart());
        }
        if (thrift.isSetReadmeOssAvailable()) {
            pojo.setReadmeOssAvailable(thrift.isReadmeOssAvailable());
        }
        if (thrift.isSetRequestID()) {
            pojo.setRequestID(thrift.getRequestID());
        }
        if (thrift.isSetRequestorPerson()) {
            pojo.setRequestorPerson(thrift.getRequestorPerson());
        }
        if (thrift.isSetScanned()) {
            pojo.setScanned(thrift.getScanned());
        }
        if (thrift.isSetScreenshotOfWebSite()) {
            pojo.setScreenshotOfWebSite(thrift.isScreenshotOfWebSite());
        }
        if (thrift.isSetSourceCodeCotsAvailable()) {
            pojo.setSourceCodeCotsAvailable(thrift.isSourceCodeCotsAvailable());
        }
        if (thrift.isSetSourceCodeDelivery()) {
            pojo.setSourceCodeDelivery(thrift.isSourceCodeDelivery());
        }
        if (thrift.isSetSourceCodeOriginalFromCommunity()) {
            pojo.setSourceCodeOriginalFromCommunity(thrift.isSourceCodeOriginalFromCommunity());
        }
        if (thrift.isSetSourceCodeSelfMade()) {
            pojo.setSourceCodeSelfMade(thrift.isSourceCodeSelfMade());
        }
        if (thrift.isSetSourceCodeToolMade()) {
            pojo.setSourceCodeToolMade(thrift.isSourceCodeToolMade());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ClearingInformation toThrift(ClearingInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ClearingInformation thrift = new org.eclipse.sw360.datahandler.thrift.components.ClearingInformation();
        if (pojo.getAdditionalRequestInfo() != null) {
            thrift.setAdditionalRequestInfo(pojo.getAdditionalRequestInfo());
        }
        if (pojo.getBinariesOriginalFromCommunity() != null) {
            thrift.setBinariesOriginalFromCommunity(pojo.getBinariesOriginalFromCommunity());
        }
        if (pojo.getBinariesSelfMade() != null) {
            thrift.setBinariesSelfMade(pojo.getBinariesSelfMade());
        }
        if (pojo.getClearingStandard() != null) {
            thrift.setClearingStandard(pojo.getClearingStandard());
        }
        if (pojo.getClearingTeam() != null) {
            thrift.setClearingTeam(pojo.getClearingTeam());
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getComponentClearingReport() != null) {
            thrift.setComponentClearingReport(pojo.getComponentClearingReport());
        }
        if (pojo.getComponentLicenseInformation() != null) {
            thrift.setComponentLicenseInformation(pojo.getComponentLicenseInformation());
        }
        if (pojo.getCountOfSecurityVn() != null) {
            thrift.setCountOfSecurityVn(pojo.getCountOfSecurityVn());
        }
        if (pojo.getEvaluated() != null) {
            thrift.setEvaluated(pojo.getEvaluated());
        }
        if (pojo.getExternalSupplierID() != null) {
            thrift.setExternalSupplierID(pojo.getExternalSupplierID());
        }
        if (pojo.getExternalUrl() != null) {
            thrift.setExternalUrl(pojo.getExternalUrl());
        }
        if (pojo.getFinalizedLicenseScanReport() != null) {
            thrift.setFinalizedLicenseScanReport(pojo.getFinalizedLicenseScanReport());
        }
        if (pojo.getLegalEvaluation() != null) {
            thrift.setLegalEvaluation(pojo.getLegalEvaluation());
        }
        if (pojo.getLicenseAgreement() != null) {
            thrift.setLicenseAgreement(pojo.getLicenseAgreement());
        }
        if (pojo.getLicenseScanReportResult() != null) {
            thrift.setLicenseScanReportResult(pojo.getLicenseScanReportResult());
        }
        if (pojo.getProcStart() != null) {
            thrift.setProcStart(pojo.getProcStart());
        }
        if (pojo.getReadmeOssAvailable() != null) {
            thrift.setReadmeOssAvailable(pojo.getReadmeOssAvailable());
        }
        if (pojo.getRequestID() != null) {
            thrift.setRequestID(pojo.getRequestID());
        }
        if (pojo.getRequestorPerson() != null) {
            thrift.setRequestorPerson(pojo.getRequestorPerson());
        }
        if (pojo.getScanned() != null) {
            thrift.setScanned(pojo.getScanned());
        }
        if (pojo.getScreenshotOfWebSite() != null) {
            thrift.setScreenshotOfWebSite(pojo.getScreenshotOfWebSite());
        }
        if (pojo.getSourceCodeCotsAvailable() != null) {
            thrift.setSourceCodeCotsAvailable(pojo.getSourceCodeCotsAvailable());
        }
        if (pojo.getSourceCodeDelivery() != null) {
            thrift.setSourceCodeDelivery(pojo.getSourceCodeDelivery());
        }
        if (pojo.getSourceCodeOriginalFromCommunity() != null) {
            thrift.setSourceCodeOriginalFromCommunity(pojo.getSourceCodeOriginalFromCommunity());
        }
        if (pojo.getSourceCodeSelfMade() != null) {
            thrift.setSourceCodeSelfMade(pojo.getSourceCodeSelfMade());
        }
        if (pojo.getSourceCodeToolMade() != null) {
            thrift.setSourceCodeToolMade(pojo.getSourceCodeToolMade());
        }
        return thrift;
    }
}
