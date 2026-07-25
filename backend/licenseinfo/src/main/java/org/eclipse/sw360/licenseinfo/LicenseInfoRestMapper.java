/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoParsingResultConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseNameWithTextConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseObligationsStatusInfoConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationParsingResultConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.OutputFormatInfoConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.services.projects.Project;

final class LicenseInfoRestMapper {

    private LicenseInfoRestMapper() {}

    static org.eclipse.sw360.datahandler.thrift.projects.Project toThriftProject(Project pojo) {
        return ProjectConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.components.Release toThriftRelease(Release pojo) {
        return ReleaseConverter.toThrift(pojo);
    }

    static List<LicenseInfoParsingResult> fromThriftLicenseInfoParsingResults(
            List<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(LicenseInfoParsingResultConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult> toThriftLicenseInfoParsingResults(
            List<LicenseInfoParsingResult> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(LicenseInfoParsingResultConverter::toThrift).collect(Collectors.toList());
    }

    static List<ObligationParsingResult> fromThriftObligationParsingResults(
            List<org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ObligationParsingResultConverter::fromThrift).collect(Collectors.toList());
    }

    static org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult toThriftObligationParsingResult(
            ObligationParsingResult pojo) {
        return ObligationParsingResultConverter.toThrift(pojo);
    }

    static LicenseInfoParsingResult fromThriftLicenseInfoParsingResult(
            org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult thrift) {
        return LicenseInfoParsingResultConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult toThriftLicenseInfoParsingResult(
            LicenseInfoParsingResult pojo) {
        return LicenseInfoParsingResultConverter.toThrift(pojo);
    }

    static LicenseObligationsStatusInfo fromThriftLicenseObligationsStatusInfo(
            org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo thrift) {
        return LicenseObligationsStatusInfoConverter.fromThrift(thrift);
    }

    static Map<String, org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo> toThriftObligationStatusMap(
            Map<String, ObligationStatusInfo> pojoMap) {
        if (pojoMap == null) {
            return Map.of();
        }
        return pojoMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> ObligationStatusInfoConverter.toThrift(e.getValue())));
    }

    static Map<String, Set<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText>> toThriftExcludedLicenses(
            Map<String, Set<LicenseNameWithText>> pojoMap) {
        if (pojoMap == null) {
            return Map.of();
        }
        return pojoMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            if (e.getValue() == null) {
                return Set.of();
            }
            return e.getValue().stream().map(LicenseNameWithTextConverter::toThrift).collect(Collectors.toSet());
        }));
    }

    static List<OutputFormatInfo> fromThriftOutputFormatInfos(
            List<org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(OutputFormatInfoConverter::fromThrift).collect(Collectors.toList());
    }

    static OutputFormatInfo fromThriftOutputFormatInfo(
            org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo thrift) {
        return OutputFormatInfoConverter.fromThrift(thrift);
    }
}
