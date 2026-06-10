/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.moderation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.sw360.datahandler.services.common.ModerationState;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.ComponentType;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;

import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationRequest {
    private String id;
    private String revision;
    private String type;
    @JsonProperty(required = true)
    private Long timestamp;
    private Long timestampOfDecision;
    @JsonProperty(required = true)
    private String documentId;
    @JsonProperty(required = true)
    private DocumentType documentType;
    private String requestingUser;
    private Set<String> moderators;
    private String documentName;
    @JsonProperty(required = true)
    private ModerationState moderationState;
    private String reviewer;
    @JsonProperty(required = true)
    private Boolean requestDocumentDelete;
    private String requestingUserDepartment;
    private ComponentType componentType;
    private String commentRequestingUser;
    private String commentDecisionModerator;
    private Component componentAdditions;
    private Release releaseAdditions;
    private Project projectAdditions;
    private License licenseAdditions;
    private User user;
    private Component componentDeletions;
    private Release releaseDeletions;
    private Project projectDeletions;
    private License licenseDeletions;
    private SPDXDocument spdxDocumentAdditions;
    private SPDXDocument spdxDocumentDeletions;
    private DocumentCreationInformation documentCreationInfoAdditions;
    private DocumentCreationInformation documentCreationInfoDeletions;
    private PackageInformation packageInfoAdditions;
    private PackageInformation packageInfoDeletions;
}
