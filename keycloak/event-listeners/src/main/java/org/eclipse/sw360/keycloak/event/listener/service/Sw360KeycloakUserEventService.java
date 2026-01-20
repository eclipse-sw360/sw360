/*
SPDX-FileCopyrightText: © 2024-2026 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.keycloak.events.Event;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.userprofile.DefaultUserProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;
import static org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService.CUSTOM_ATTR_DEPARTMENT;
import static org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService.CUSTOM_ATTR_EXTERNAL_ID;
import static org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService.DEFAULT_DEPARTMENT;
import static org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService.DEFAULT_EXTERNAL_ID;
import static org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService.REALM;

public class Sw360KeycloakUserEventService {
	private static final Logger log = Logger.getLogger(Sw360KeycloakUserEventService.class);
	public static final String USERNAME = "username";

	private final Sw360UserService userService;
	private final ObjectMapper objectMapper;
	private final KeycloakSession keycloakSession;

	public Sw360KeycloakUserEventService(Sw360UserService sw360UserService, ObjectMapper objectMapper, KeycloakSession keycloakSession) {
		this.userService = sw360UserService;
		this.objectMapper = objectMapper;
		this.keycloakSession = keycloakSession;
	}

	public void userRegistrationEvent(Event event) {
		Map<String, String> details = event.getDetails();
		User user = fillUserFromEvent(details);

        // Set user group if exists in CouchDB
        Optional<User> existingUser = Optional.ofNullable(userService.getUserByEmail(user.getEmail()));
        existingUser.ifPresent(eu -> {
			user.setUserGroup(eu.getUserGroup());
			updateKeycloakUserGroup(event, eu.getUserGroup());
		});

		userService.createOrUpdateUser(user);
	}

	private User fillUserFromEvent(Map<String, String> userDetails) {
		log.debug("Event Details" + userDetails);
        User user = new User();
        user.setType(TYPE_USER);
		user.setEmail(userDetails.get("email"));
		user.setFullname(userDetails.get("first_name") + " " + userDetails.get("last_name"));
		user.setGivenname(userDetails.get("first_name"));
		user.setLastname(userDetails.get("last_name"));
        user.setDepartment(sanitizeDepartment(
                getAttributeOrDefaultFromSession(CUSTOM_ATTR_DEPARTMENT, DEFAULT_DEPARTMENT)));
        user.setExternalid(sanitizeExternalId(
                getAttributeOrDefaultFromSession(CUSTOM_ATTR_EXTERNAL_ID, DEFAULT_EXTERNAL_ID)));
		return user;
	}

    private String getAttributeOrDefaultFromSession(String attributeName, String defaultValue) {
        return keycloakSession.getAttributes().values().stream()
                .filter(DefaultUserProfile.class::isInstance)
                .map(DefaultUserProfile.class::cast)
                .flatMap(usPro -> usPro.getAttributes().toMap().entrySet().stream())
                .filter(entry ->
                        entry.getKey().equalsIgnoreCase(attributeName) &&
                                !entry.getValue().isEmpty())
                .findFirst()
                .map(Map.Entry::getValue)
                .map(List::getFirst)
                .orElse(defaultValue);
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
		List<String> departments = userModel.getAttributes().getOrDefault(
                CUSTOM_ATTR_DEPARTMENT, Collections.singletonList(DEFAULT_DEPARTMENT));
		String department = departments.getFirst();
		String parentDepartment = sanitizeDepartment(department);
		user.setDepartment(parentDepartment);
	}

	private void mapSetExternalId(UserModel userModel, User user) {
		log.debug("User Model Attributes" + userModel.getAttributes());
		List<String> externalIds = userModel.getAttributes().getOrDefault(
                CUSTOM_ATTR_EXTERNAL_ID, Collections.singletonList(DEFAULT_EXTERNAL_ID));
		String externalId = externalIds.getFirst();
		user.setExternalid(sanitizeExternalId(externalId));
	}

	/**
	 * Sanitizes and maps the department name from identity provider.
	 * Applies organization name mapping if configured.
	 *
	 * @param department the department string from identity provider
	 * @return the sanitized and mapped department name
	 * @see OrganizationMapper#mapOrganizationName(String)
	 */
	public static String sanitizeDepartment(String department) {
		String departmentSanitized = null;
		if (department != null) {
			// Apply organization name mapping if configured
			departmentSanitized = OrganizationMapper.mapOrganizationName(department.trim());
		} else {
            departmentSanitized = DEFAULT_DEPARTMENT;
        }
		return departmentSanitized;
	}

	public static String sanitizeExternalId(String externalId) {
		String externalIdSanitized = null;
		if (externalId != null) {
			externalIdSanitized = externalId.trim();
		} else {
            externalIdSanitized = DEFAULT_EXTERNAL_ID;
        }
		return externalIdSanitized;
	}

    public boolean isValidEmail(String email) {
		String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}

    /**
     * User was created in CouchDB (prob by SW360 application). Update
     * KeyCloak's user model to have the group membership from CouchDB values.
     * @param event     Event which is triggered.
     * @param userGroup New UserGroup to assign to KC user
     */
	private void updateKeycloakUserGroup(@NotNull Event event, @NotNull UserGroup userGroup) {
		String userId = event.getUserId();
		RealmModel realm = keycloakSession.realms().getRealmByName(REALM);
		UserModel userModel = keycloakSession.users().getUserById(realm, userId);

		if (userModel != null) {
            String groupName = userGroup.toString();
			Optional<GroupModel> groupModel = realm.getGroupsStream()
					.filter(g -> g.getName().equalsIgnoreCase(groupName))
					.findFirst();

			groupModel.ifPresent(g -> {
				if (!userModel.isMemberOf(g)) {
					userModel.getGroupsStream().forEach(userModel::leaveGroup);
					userModel.joinGroup(g);
					log.infof("Updated KeyCloak user group to %s for user %s", groupName, userModel.getEmail());
				}
			});

			if (groupModel.isEmpty()) {
				log.warnf("Group %s not found in KeyCloak", groupName);
			}
		} else {
			log.warnf("User with id %s not found in KeyCloak", userId);
		}
	}
}
