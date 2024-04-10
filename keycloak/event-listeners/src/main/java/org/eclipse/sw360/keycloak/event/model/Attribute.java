/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import java.util.Map;

public class Attribute {
	private String name;
	private String displayName;
	private boolean required;
	private boolean readOnly;
	private Map<String, Object> validators;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public Map<String, Object> getValidators() {
		return validators;
	}

	public void setValidators(Map<String, Object> validators) {
		this.validators = validators;
	}
}
