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
public class DocumentCreationInformation {

    private String id;

    private String revision;

    private String type;

    private String spdxDocumentId;

    private String spdxVersion;

    private String dataLicense;

    private String SPDXID;

    private String name;

    private String documentNamespace;

    private Set<ExternalDocumentReferences> externalDocumentRefs;

    private String licenseListVersion;

    private Set<Creator> creator;

    private String created;

    private String creatorComment;

    private String documentComment;

    private DocumentState documentState;

    private Map<RequestedAction, Boolean> permissions;

    private String createdBy;

    private Set<String> moderators;
}
