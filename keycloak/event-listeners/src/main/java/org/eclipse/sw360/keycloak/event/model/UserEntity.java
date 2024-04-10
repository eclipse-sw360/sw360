/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import java.util.List;
import java.util.Map;

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
	private List<String> federatedIdentities;
	private int notBefore;
	private Access access;
	private UserProfileMetadata userProfileMetadata;
	private String password;

	public String getPassword() {
		return password;
	}

	public String setPassword(String password) {
		return this.password=password;
	}

	public Map<String, List<String>> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, List<String>> attributes) {
		this.attributes = attributes;
	}

	public long getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setCreatedTimestamp(long createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public boolean isTotp() {
		return totp;
	}

	public void setTotp(boolean totp) {
		this.totp = totp;
	}

	public List<String> getDisableableCredentialTypes() {
		return disableableCredentialTypes;
	}

	public void setDisableableCredentialTypes(List<String> disableableCredentialTypes) {
		this.disableableCredentialTypes = disableableCredentialTypes;
	}

	public List<String> getFederatedIdentities() {
		return federatedIdentities;
	}

	public void setFederatedIdentities(List<String> federatedIdentities) {
		this.federatedIdentities = federatedIdentities;
	}

	public int getNotBefore() {
		return notBefore;
	}

	public void setNotBefore(int notBefore) {
		this.notBefore = notBefore;
	}

	public Access getAccess() {
		return access;
	}

	public void setAccess(Access access) {
		this.access = access;
	}

	public UserProfileMetadata getUserProfileMetadata() {
		return userProfileMetadata;
	}

	public void setUserProfileMetadata(UserProfileMetadata userProfileMetadata) {
		this.userProfileMetadata = userProfileMetadata;
	}

	public List<String> getRequiredActions() {
		return requiredActions;
	}

	public List<String> getGroups() {
		return groups;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setRequiredActions(List<String> requiredActions) {
		this.requiredActions = requiredActions;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "UserEntity [id=" + id + ", username=" + username + ", enabled=" + enabled + ", emailVerified="
				+ emailVerified + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
				+ ", requiredActions=" + requiredActions + ", groups=" + groups + ", attributes=" + attributes
				+ ", createdTimestamp=" + createdTimestamp + ", totp=" + totp + ", disableableCredentialTypes="
				+ disableableCredentialTypes + ", federatedIdentities=" + federatedIdentities + ", notBefore="
				+ notBefore + ", access=" + access + ", userProfileMetadata=" + userProfileMetadata + "]";
	}

}
