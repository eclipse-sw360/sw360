/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.licenses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.Quadratic;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class License {
    private String id;
    private String revision;
    private String type;
    private String shortname;
    @JsonProperty(required = true)
    private String fullname;
    private LicenseType licenseType;
    private String licenseTypeDatabaseId;
    private String externalLicenseLink;
    private String note;
    private Map<String, String> externalIds;
    private Map<String, String> additionalData;
    private String reviewdate;
    private Quadratic osiApproved;
    private Quadratic fsfLibre;
    private List<Obligation> obligations;
    private Set<String> obligationDatabaseIds;
    private String obligationListId;
    private String text;
    private Boolean checked;
    private DocumentState documentState;
    private Map<RequestedAction, Boolean> permissions;
}
