/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    private String id;

    private String revision;

    private String type;

    @JsonProperty(required = true)
    private String email;

    private UserGroup userGroup;

    private String externalid;

    private String fullname;

    private String givenname;

    private String lastname;

    @JsonProperty(required = true)
    private String department;

    private Boolean wantsMailNotification;

    private String commentMadeDuringModerationRequest;

    private Map<String, Boolean> notificationPreferences;

    private Set<String> formerEmailAddresses;

    private List<RestApiToken> restApiTokens;

    private Map<String, Boolean> myProjectsPreferenceSelection;

    private Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles;

    private List<String> primaryRoles;

    private Boolean deactivated;

    private Map<String, ClientMetadata> oidcClientInfos;

    private String password;
}
