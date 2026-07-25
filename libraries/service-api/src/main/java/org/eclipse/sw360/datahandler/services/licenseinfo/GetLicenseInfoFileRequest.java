/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.licenseinfo;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.experimental.Accessors;

import org.eclipse.sw360.datahandler.services.projects.Project;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetLicenseInfoFileRequest {

    private Project project;
    private String outputGeneratorClassName;
    private Map<String, Map<String, Boolean>> releaseIdsToSelectedAttachmentIds;
    private Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment;
    private String externalIds;
    private String fileName;
    private boolean excludeReleaseVersion;
}
