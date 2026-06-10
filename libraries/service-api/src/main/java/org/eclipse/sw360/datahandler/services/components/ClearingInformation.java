/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClearingInformation {
    private String externalSupplierID;
    private String additionalRequestInfo;
    private String evaluated;
    private String procStart;
    private String requestID;
    private String clearingTeam;
    private String requestorPerson;
    private Boolean binariesOriginalFromCommunity;
    private Boolean binariesSelfMade;
    private Boolean componentLicenseInformation;
    private Boolean sourceCodeDelivery;
    private Boolean sourceCodeOriginalFromCommunity;
    private Boolean sourceCodeToolMade;
    private Boolean sourceCodeSelfMade;
    private Boolean sourceCodeCotsAvailable;
    private Boolean screenshotOfWebSite;
    private Boolean finalizedLicenseScanReport;
    private Boolean licenseScanReportResult;
    private Boolean legalEvaluation;
    private Boolean licenseAgreement;
    private String scanned;
    private Boolean componentClearingReport;
    private String clearingStandard;
    private Boolean readmeOssAvailable;
    private String comment;
    private Integer countOfSecurityVn;
    private String externalUrl;
}
