/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.spdx;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileInformation {

    private String id;

    private String revision;

    private String type;

    private String fileName;

    private String SPDXID;

    private Set<String> fileTypes;

    private Set<CheckSum> checksums;

    private String licenseConcluded;

    private Set<String> licenseInfoInFiles;

    private String licenseComments;

    private String copyrightText;

    private String fileComment;

    private String noticeText;

    private Set<String> fileContributors;

    private Set<String> fileAttributionText;

    private Set<SnippetInformation> snippetInformation;

    private Set<OtherLicensingInformationDetected> hasExtractedLicensingInfos;

    private Set<Annotations> annotations;
}
