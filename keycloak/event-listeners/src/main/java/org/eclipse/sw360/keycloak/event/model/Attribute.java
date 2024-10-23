/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
@Getter
@Setter
@ToString
public class Attribute {
	private String name;
	private String displayName;
	private boolean required;
	private boolean readOnly;
	private Map<String, Object> validators;
}
