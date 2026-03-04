/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.client.service;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Customizes the OIDC user info
 *
 * @author smruti.sahoo@siemens.com
 */
@Service
public class Sw360OidcUserInfoService {

	public static final String USER_GROUP = "userGroup";
	public static final String DEPARTMENT = "department";
	public static final String PRIMARY_ROLES = "primaryRoles";
	private final Sw360UserDetailsProvider sw360UserDetailsProvider;

	public Sw360OidcUserInfoService(Sw360UserDetailsProvider sw360UserDetailsProvider) {
		this.sw360UserDetailsProvider = sw360UserDetailsProvider;
	}

	public OidcUserInfo loadUser(String username) {

		User user = this.sw360UserDetailsProvider.provideUserDetails(username, null);
		return new OidcUserInfo(createUser(user));
	}

	private Map<String, Object> createUser(User user) {

		return OidcUserInfo.builder()
				.subject(user.getEmail())
				.name(user.getFullname())
				.givenName(user.getGivenname())
				.familyName(user.getLastname())
				.middleName(user.getLastname())
				.email(user.getEmail())
				.claim(USER_GROUP, user.getUserGroup())
				.claim(DEPARTMENT, user.getDepartment())
				.claim(PRIMARY_ROLES, user.getPrimaryRoles())
				.build()
				.getClaims();
	}
}
