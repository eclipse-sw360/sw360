/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
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
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoFileConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseInfoParsingResultConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.LicenseNameWithTextConverter;
import org.eclipse.sw360.common.utils.converter.licenseinfo.OutputFormatInfoConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.licenseinfo.LicenseInfoClient;
import org.eclipse.sw360.datahandler.licenseinfo.LicenseInfoClients;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Sw360LicenseInfoService {

    private LicenseInfoClient licenseInfoClient() {
        return LicenseInfoClients.get();
    }

    public OutputFormatInfo getOutputFormatInfoForGeneratorClass(String generatorClassName) {
        try {
            return OutputFormatInfoConverter.toThrift(
                    licenseInfoClient().getOutputFormatInfoForGeneratorClass(generatorClassName));
        } catch (SW360Exception e) {
            throw new BadRequestClientException(e.getMessage(), e);
        }
    }

    public LicenseInfoFile getLicenseInfoFile(Project project, User sw360User, String generatorClassNameWithVariant,
            Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds,
            Map<String, Set<LicenseNameWithText>> excludedLicenses, String externalIds, String fileName,
            boolean excludeReleaseVersion) {
        try {
            Map<String, Set<org.eclipse.sw360.datahandler.services.licenseinfo.LicenseNameWithText>> excludedPojo =
                    excludedLicenses == null ? Map.of()
                            : excludedLicenses.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                                if (e.getValue() == null) {
                                    return Set.of();
                                }
                                return e.getValue().stream().map(LicenseNameWithTextConverter::fromThrift)
                                        .collect(Collectors.toSet());
                            }));

            return LicenseInfoFileConverter.toThrift(licenseInfoClient().getLicenseInfoFile(
                    ProjectConverter.fromThrift(project), UserConverter.fromThrift(sw360User),
                    generatorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedPojo, externalIds, fileName,
                    excludeReleaseVersion));
        } catch (SW360Exception e) {
            throw new BadRequestClientException(e.getMessage(), e);
        }
    }

    public List<LicenseInfoParsingResult> getLicenseInfoForAttachment(Release release, User sw360User,
            String attachmentContentId, boolean includeConcludedLicense) {
        try {
            return licenseInfoClient()
                    .getLicenseInfoForAttachment(ReleaseConverter.fromThrift(release), attachmentContentId,
                            includeConcludedLicense, UserConverter.fromThrift(sw360User))
                    .stream().map(LicenseInfoParsingResultConverter::toThrift).collect(Collectors.toList());
        } catch (SW360Exception e) {
            throw new RuntimeException(e);
        }
    }
}
