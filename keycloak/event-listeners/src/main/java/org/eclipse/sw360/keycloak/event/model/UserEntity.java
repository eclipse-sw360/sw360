/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEntity {
    private String id;
    private String username;
    private boolean enabled;
    private boolean emailVerified;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> requiredActions;
    private List<String> groups;
    private Map<String, List<String>> attributes;
    private long createdTimestamp;
    private boolean totp;
    private List<String> disableableCredentialTypes;
    private List<Map<String, String>> federatedIdentities;
    private int notBefore;
    private Access access;
    private UserProfileMetadata userProfileMetadata;
    private String password;
}
