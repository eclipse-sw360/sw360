/*
SPDX-FileCopyrightText: © 2024,2025 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Sw360UserStorageProviderFactory implements UserStorageProviderFactory<Sw360UserStorageProvider>, ImportSynchronization {
	public static final String PROVIDER_ID = "sw360-user-storage-jpa";

	private static final Logger logger = LoggerFactory.getLogger(Sw360UserStorageProviderFactory.class);
	public static final String SW360_USER_STORAGE_PROVIDER = "SW360 User Storage Provider";
	private static final String CUSTOM_ATTR_DEPARTMENT = "Department";
	@Override
	public Sw360UserStorageProvider create(KeycloakSession session, ComponentModel model) {
		return new Sw360UserStorageProvider(session, model);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public String getHelpText() {
		return SW360_USER_STORAGE_PROVIDER;
	}

	@Override
	public void close() {
		logger.debug("<<<<<< Closing factory");
	}

	@Override
    public void init(Config.Scope config) {
        logger.info("Initializing Sw360UserStorageProviderFactory with config: {}", config);
        if (config.get("thrift") != null && !config.get("thrift").isEmpty()) {
			logger.info("In SPI {}, setting thrift server URL to: '{}'",
					PROVIDER_ID, config.get("thrift"));
            Sw360UserService.thriftServerUrl = config.get("thrift");
        }
    }

	/**
	 * Synchronizes users from an external service with Keycloak.
	 * <p>
	 * This method fetches users from an external service and synchronizes them with the Keycloak realm.
	 * It updates existing users and creates new users if they do not already exist in Keycloak.
	 * The synchronization process is performed within a single transaction.
	 *
	 * @param sessionFactory the Keycloak session factory used to create sessions.
	 * @param realmId the ID of the realm in which users are to be synchronized.
	 * @param model the user storage provider model.
	 * @return a SynchronizationResult object containing the results of the synchronization process.
	 */
	@Override
	public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		logger.debug("Starting user synchronization");

		SynchronizationResult result = new SynchronizationResult();
		Sw360UserService sw360UserService = new Sw360UserService();
		List<User> externalUsers = sw360UserService.getAllUsers();
		logger.debug("Fetched {} users from external service", externalUsers.size());

		KeycloakSession session = null;
		try {
			session = sessionFactory.create(); // Create a session
			session.getTransactionManager().begin(); // Begin transaction
			RealmModel realm = session.realms().getRealm(realmId);

			if (realm != null) {
				session.getContext().setRealm(realm); // Set realm in the session context
				Set<String> existingUsernames = session.users()
						.searchForUserStream(realm, "") // Fetch all users
						.map(UserModel::getEmail)
						.collect(Collectors.toSet());

				for (User externalUser : externalUsers) {
					String email = externalUser.getEmail();
					if (email == null || email.isEmpty()) {
						logger.error("Skipping user as no email is provided: {}", externalUser);
						result.increaseFailed(); // track ignored users
						continue; // Skip this user
					}
					processExternalUser(session, realm, externalUser, existingUsernames, result);
				}
				session.getTransactionManager().commit(); // Commit transaction
				logger.debug("User synchronization completed successfully");
			} else {
				logger.error("Realm could not be found or set in the session context.");
				session.getTransactionManager().rollback(); // Rollback transaction if realm is null
			}
		} catch (Exception e) {
			logger.error("User synchronization failed", e);
			if (session != null && session.getTransactionManager().isActive()) {
				session.getTransactionManager().rollback(); // Rollback transaction in case of an exception
			}
		} finally {
			if (session != null) {
				session.close(); // Ensure the session is closed
			}
		}
		return result;
	}

	/**
	 * Processes an external user and synchronizes it with Keycloak.
	 * <p>
	 * This method checks if the external user already exists in Keycloak. If it does, it updates the user.
	 * If it does not exist, it creates a new user in Keycloak.
	 *
	 * @param session the Keycloak session.
	 * @param realm the Keycloak realm.
	 * @param externalUser the external user to be processed.
	 * @param existingUserEmails a set of existing user emails in Keycloak.
	 * @param result the synchronization result object to track added and updated users.
	 */
	private void processExternalUser(KeycloakSession session, RealmModel realm, User externalUser, Set<String> existingUserEmails, SynchronizationResult result) {
		String email = externalUser.getEmail();
		UserGroup userGroup = externalUser.getUserGroup();

		if (existingUserEmails.contains(email)) {
			UserModel keycloakUser = session.users().getUserByUsername(realm, email);
			if (keycloakUser != null) {
				updateUserInKeycloak(keycloakUser, realm, externalUser, userGroup);
				result.increaseUpdated();
			}
		} else {
			createUserInKeycloak(session, realm, externalUser, userGroup);
			result.increaseAdded();
		}
	}

	/**
	 * Populates user attributes from the external user to the Keycloak user.
	 * <p>
	 *     This method sets the first name, last name, email, and department attributes of the Keycloak user
	 *     from the external user. It also assigns the user to a group based on the external user's group.
	 *     If any of the attributes are null or empty, a warning is logged.
	 *
	 * @param user the Keycloak user to be populated.
	 * @param realm the Keycloak realm.
	 * @param externalUser the external user with attributes to be populated.
	 * @param externalUserUserGroup the external user group.
	 */
	private void populateUserAttributes(UserModel user, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
		Optional.ofNullable(externalUser.getGivenname())
				.ifPresentOrElse(
						user::setFirstName,
						() -> logger.warn("Given name is null or empty for user: {}", externalUser.getEmail())
				);

		Optional.ofNullable(externalUser.getLastname())
				.ifPresentOrElse(
						user::setLastName,
						() -> logger.warn("Last name is null or empty for user: {}", externalUser.getEmail())
				);


		Optional.ofNullable(externalUser.getDepartment())
				.ifPresentOrElse(
						department -> user.setSingleAttribute(CUSTOM_ATTR_DEPARTMENT, department),
						() -> logger.warn("Department is null or empty for user: {}", externalUser.getEmail())
				);
		// Use the name of the externalUserUserGroup directly
		assignGroupToUser(user, realm, externalUserUserGroup);
	}

	/**
	 * Assigns the user to a group based on the external user's group.
	 * <p>
	 *     This method checks if the external user group is valid and not empty. If it is valid, it assigns the user
	 *     to the corresponding group in Keycloak. If the user is already in the target group, it does nothing.
	 *
	 * @param user the Keycloak user to be assigned to a group.
	 * @param realm the Keycloak realm.
	 * @param externalUserUserGroup the external user group.
	 */
	private static void assignGroupToUser(UserModel user, RealmModel realm, UserGroup externalUserUserGroup) {
		if (externalUserUserGroup == null || StringUtils.isBlank(externalUserUserGroup.name())) {
			logger.warn("Invalid or empty group provided for user: {}", user.getEmail());
			return;
		}

		String groupName = externalUserUserGroup.name();
		GroupModel targetGroup = realm.getGroupsStream()
				.filter(g -> g.getName().equals(groupName))
				.findFirst()
				.orElse(null);

		if (targetGroup == null) {
			logger.warn("Group '{}' not found in Keycloak for user: {}", groupName, user.getEmail());
			return;
		}

		// Check if the user is already in the target group
		if (user.getGroupsStream().anyMatch(g -> g.equals(targetGroup))) {
			logger.debug("User {} is already in group {}", user.getEmail(), groupName);
			return;
		}

		// Remove user from all other groups
		user.getGroupsStream().forEach(user::leaveGroup);

		// Add user to the target group
		user.joinGroup(targetGroup);
		logger.debug("Assigned user {} to group {}", user.getEmail(), groupName);
	}

	/**
	 * Updates the user in Keycloak with the attributes from the external user.
	 * <p>
	 *     This method sets the user as enabled and populates the user attributes from the external user.
	 *
	 *
	 * @param keycloakUser the Keycloak user to be updated.
	 * @param realm the Keycloak realm.
	 * @param externalUser the external user with updated attributes.
	 * @param externalUserUserGroup the external user group.
	 */
	private void updateUserInKeycloak(UserModel keycloakUser, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
		try {
			populateUserAttributes(keycloakUser, realm, externalUser, externalUserUserGroup);
			logger.debug("Updated user in Keycloak: {}", keycloakUser.getEmail());
		} catch (Exception e) {
			logger.error("Error updating user in Keycloak", e);
		}
	}

	/**
	 * Creates a new user in Keycloak with the attributes from the external user.
	 * <p>
	 *     This method sets the user as enabled and populates the user attributes from the external user.
	 *     It also assigns the user to a group based on the external user's group.

	 * @param session the Keycloak session.
	 * @param realm the Keycloak realm.
	 * @param externalUser the external user to be created.
	 * @param externalUserUserGroup the external user group.
	 *
	 */
	private void createUserInKeycloak(KeycloakSession session, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
		try {
			UserModel newUser = session.users().addUser(realm, externalUser.getEmail());
			newUser.setEnabled(true);
			populateUserAttributes(newUser, realm, externalUser, externalUserUserGroup);
			logger.debug("Created new user  {}", newUser.getEmail());
		} catch (Exception e) {
			logger.error("Error creating user in Keycloak", e);
		}
	}
	@Override
	public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		logger.error("syncSince not implemented");
		return null;
	}
}
