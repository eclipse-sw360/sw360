/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenseinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the licenseinfo backend service.
 *
 * Callers use this instead of the former Thrift {@code LicenseInfoService.Iface}.
 * Types are service-api POJOs. See {@link LicenseInfoServiceRestClient} and
 * {@link LicenseInfoClients}.
 */
public interface LicenseInfoClient {

    List<LicenseInfoParsingResult> getLicenseInfoForAttachment(Release release, String attachmentContentId,
            boolean includeConcludedLicense, User user);

    List<ObligationParsingResult> getObligationsForAttachment(Release release, String attachmentContentId, User user);

    LicenseObligationsStatusInfo getProjectObligationStatus(Map<String, ObligationStatusInfo> obligationStatusMap,
            List<LicenseInfoParsingResult> licenseInfoResults, Map<String, String> excludedReleaseIdToAcceptedCLI);

    LicenseInfoParsingResult createLicenseToObligationMapping(LicenseInfoParsingResult licenseInfoResult,
            ObligationParsingResult obligationInfoResult);

    LicenseInfoFile getLicenseInfoFile(Project project, User user, String outputGeneratorClassName,
            Map<String, Map<String, Boolean>> releaseIdsToSelectedAttachmentIds,
            Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment, String externalIds, String fileName);

    LicenseInfoFile getLicenseInfoFile(Project project, User user, String outputGeneratorClassName,
            Map<String, Map<String, Boolean>> releaseIdsToSelectedAttachmentIds,
            Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment, String externalIds, String fileName,
            boolean excludeReleaseVersion);

    List<OutputFormatInfo> getPossibleOutputFormats();

    OutputFormatInfo getOutputFormatInfoForGeneratorClass(String generatorClassName);

    String getDefaultLicenseInfoHeaderText(String fileName);

    String getDefaultObligationsText();

    Map<String, Map<String, String>> evaluateAttachments(String releaseId, User user);
}
