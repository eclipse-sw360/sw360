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

import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageInformation {

    private String id;

    private String revision;

    private String type;

    private String spdxDocumentId;

    private String name;

    private String SPDXID;

    private String versionInfo;

    private String packageFileName;

    private String supplier;

    private String originator;

    private String downloadLocation;

    private Boolean filesAnalyzed;

    private PackageVerificationCode packageVerificationCode;

    private Set<CheckSum> checksums;

    private String homepage;

    private String sourceInfo;

    private String licenseConcluded;

    private Set<String> licenseInfoFromFiles;

    private String licenseDeclared;

    private String licenseComments;

    private String copyrightText;

    private String summary;

    private String description;

    private String packageComment;

    private Set<ExternalReference> externalRefs;

    private Set<String> attributionText;

    private Set<Annotations> annotations;

    private String primaryPackagePurpose;

    private String releaseDate;

    private String builtDate;

    private String validUntilDate;

    private DocumentState documentState;

    private Map<RequestedAction, Boolean> permissions;

    private String createdBy;

    private Integer index;

    private Set<RelationshipsBetweenSPDXElements> relationships;

    private Set<String> moderators;
}
