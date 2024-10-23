/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
/**
 * This class represents the metadata of the user profile.
 */
@Getter
@Setter
@ToString
public class UserProfileMetadata {
	private List<Attribute> attributes;
	private List<String> groups;
}
