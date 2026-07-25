/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoParsingResultConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseObligationsStatusInfoConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationParsingResultConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.licenseinfo.LicenseInfoClient;
import org.eclipse.sw360.datahandler.licenseinfo.LicenseInfoClients;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Converts thrift domain types to/from service-api around {@link LicenseInfoClients}.
 * Keeps existing resource-server call sites on thrift types while transport is REST/POJO.
 */
public final class LicenseInfoThriftBridge {

    private LicenseInfoThriftBridge() {}

    private static LicenseInfoClient client() {
        return LicenseInfoClients.get();
    }

    public static List<LicenseInfoParsingResult> getLicenseInfoForAttachment(Release release,
            String attachmentContentId, boolean includeConcludedLicense, User user) {
        return client()
                .getLicenseInfoForAttachment(ReleaseConverter.fromThrift(release), attachmentContentId,
                        includeConcludedLicense, UserConverter.fromThrift(user))
                .stream().map(LicenseInfoParsingResultConverter::toThrift).collect(Collectors.toList());
    }

    public static List<ObligationParsingResult> getObligationsForAttachment(Release release,
            String attachmentContentId, User user) {
        return client()
                .getObligationsForAttachment(ReleaseConverter.fromThrift(release), attachmentContentId,
                        UserConverter.fromThrift(user))
                .stream().map(ObligationParsingResultConverter::toThrift).collect(Collectors.toList());
    }

    public static LicenseInfoParsingResult createLicenseToObligationMapping(LicenseInfoParsingResult licenseInfoResult,
            ObligationParsingResult obligationInfoResult) {
        return LicenseInfoParsingResultConverter.toThrift(client().createLicenseToObligationMapping(
                LicenseInfoParsingResultConverter.fromThrift(licenseInfoResult),
                ObligationParsingResultConverter.fromThrift(obligationInfoResult)));
    }

    public static LicenseObligationsStatusInfo getProjectObligationStatus(
            Map<String, ObligationStatusInfo> obligationStatusMap,
            List<LicenseInfoParsingResult> licenseInfoResults,
            Map<String, String> excludedReleaseIdToAcceptedCLI) {
        Map<String, org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo> pojoMap =
                obligationStatusMap == null ? Map.of()
                        : obligationStatusMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                e -> ObligationStatusInfoConverter.fromThrift(e.getValue())));
        List<org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult> pojoResults =
                licenseInfoResults == null ? List.of()
                        : licenseInfoResults.stream().map(LicenseInfoParsingResultConverter::fromThrift)
                                .collect(Collectors.toList());
        return LicenseObligationsStatusInfoConverter.toThrift(
                client().getProjectObligationStatus(pojoMap, pojoResults, excludedReleaseIdToAcceptedCLI));
    }

    public static String getDefaultLicenseInfoHeaderText(String fileName) {
        return client().getDefaultLicenseInfoHeaderText(fileName);
    }
}
