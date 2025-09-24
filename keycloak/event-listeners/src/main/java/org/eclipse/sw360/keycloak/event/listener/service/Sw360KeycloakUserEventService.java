/*
SPDX-FileCopyrightText: © 2024-2026 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.userprofile.DefaultUserProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;

public class Sw360KeycloakUserEventService {
	private static final Logger log = Logger.getLogger(Sw360KeycloakUserEventService.class);
	public static final String USERNAME = "username";
	public static final String DEPARTMENT = "Department";
	public static final String EXTERNAL_ID = "externalId";
	public static final String DEFAULT_DEPARTMENT = "DEPARTMENT";
	public static final String DEFAULT_EXTERNAL_ID = "N/A";


	private final Sw360UserService userService;
	private final ObjectMapper objectMapper;
	private final KeycloakSession keycloakSession;
	private static final String REALM = "sw360";

	public Sw360KeycloakUserEventService(Sw360UserService sw360UserService, ObjectMapper objectMapper, KeycloakSession keycloakSession) {
		this.userService = sw360UserService;
		this.objectMapper = objectMapper;
		this.keycloakSession = keycloakSession;
	}

	public void userRegistrationEvent(Event event) {
		Map<String, String> details = event.getDetails();
		User user = fillUserFromEvent(details);
		userService.createOrUpdateUser(user);
	}

	private User fillUserFromEvent(Map<String, String> userDetails) {
		log.debug("Event Details" + userDetails);
		User user = setUserDepartmentFromSession();
        user.setType(TYPE_USER);
		user.setEmail(userDetails.get("email"));
		user.setFullname(userDetails.get("first_name") + " " + userDetails.get("last_name"));
		user.setExternalid(userDetails.get("username"));
		user.setGivenname(userDetails.get("first_name"));
		user.setLastname(userDetails.get("last_name"));
		return user;
	}

	private User setUserDepartmentFromSession() {
        User user = new User();
		keycloakSession.getAttributes().forEach((key, up) -> {
            if (up instanceof DefaultUserProfile usPro) {
                usPro.getAttributes().toMap().forEach((key1, value) -> {
                    if (key1.equalsIgnoreCase("department") && !value.isEmpty()) {
                        user.setDepartment(value.getFirst());
                    }
                });
            }
        });
		return user;
	}

	public void userLoginEvent(Event event) {
		UserProvider userProvider = keycloakSession.users();
		RealmModel realmModel = keycloakSession.realms().getRealmByName(REALM);
		UserModel userModel = getUserFromKeycloakRealm(event, realmModel, userProvider);
		User user = convertKcUserModelToUser(userModel);
        userService.createOrUpdateUser(user);
	}

	private UserModel getUserFromKeycloakRealm(Event event, RealmModel realmModel, UserProvider userProvider) {
		UserModel userModel = null;
		Map<String, String> details = event.getDetails();
		if (realmModel != null && details != null && userProvider != null) {
			if (details.containsKey(USERNAME)) {
				String userName = details.get(USERNAME);
				if (isValidEmail(userName)) {
					userModel = userProvider.getUserByEmail(realmModel, userName);
				} else {
					userModel = userProvider.getUserById(realmModel, userName);
				}
			}
		}
		return userModel;
	}

	private User convertKcUserModelToUser(UserModel userModel) {
		User user = new User();
        user.setType(TYPE_USER);
		user.setEmail(userModel.getEmail());
		user.setFullname(userModel.getFirstName() + " " + userModel.getLastName());
		user.setExternalid(userModel.getUsername());
		user.setGivenname(userModel.getFirstName());
		user.setLastname(userModel.getLastName());
		mapSetDepartment(userModel, user);
		mapSetExternalId(userModel, user);
		return user;
	}

	private void mapSetDepartment(UserModel userModel, User user) {
		log.debug("User Model Attributes" + userModel.getAttributes());
		List<String> departments = userModel.getAttributes().getOrDefault(DEPARTMENT, Collections.singletonList(DEFAULT_DEPARTMENT));
		String department = departments.getFirst();
		String parentDepartment = sanitizeDepartment(department);
		user.setDepartment(parentDepartment);
	}

	private void mapSetExternalId(UserModel userModel, User user) {
		log.debug("User Model Attributes" + userModel.getAttributes());
		List<String> externalIds = userModel.getAttributes().getOrDefault(EXTERNAL_ID, Collections.singletonList(DEFAULT_EXTERNAL_ID));
		String externalId = externalIds.getFirst();
		user.setExternalid(sanitizeExternalId(externalId));
	}

	/**
	 * As there is a function in Keycloak to add multiple departments to a user and Keycloak is not supporting it,
	 * Hence added this to find the first department from the list of departments
	 * @param department
	 * @return
	 */
	private String sanitizeDepartment(String department) {
		String departmentSanitized = null;
		if (department != null) {
			departmentSanitized= department.trim().split("\\s+")[0];
		}
		return departmentSanitized;
	}

	private String sanitizeExternalId(String externalId) {
		String externalIdSanitized = null;
		if (externalId != null) {
			externalIdSanitized= externalId.trim();
		}
		return externalIdSanitized;
	}

    public boolean isValidEmail(String email) {
		String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}
}
