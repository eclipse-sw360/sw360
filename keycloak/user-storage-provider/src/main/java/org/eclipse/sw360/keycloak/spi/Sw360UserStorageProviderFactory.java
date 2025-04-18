/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;


import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;

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

		try (KeycloakSession session = sessionFactory.create()) { // Single session for all users
			session.getTransactionManager().begin();
			RealmModel realm = session.realms().getRealm(realmId);

			if (realm != null) {
				session.getContext().setRealm(realm); // Set realm in the session context as it is required for user operations
				Set<String> existingUsernames = session.users()
						.searchForUserStream(realm, "") // Fetch all users
						.map(UserModel::getEmail)
						.collect(Collectors.toSet());

				for (User externalUser : externalUsers) {
					String username = externalUser.getEmail();
					if (existingUsernames.contains(username)) {
						UserModel keycloakUser = session.users().getUserByUsername(realm, username);
						if (keycloakUser != null) {
							updateUserInKeycloak(keycloakUser, externalUser);
							result.increaseUpdated();
						}
					} else {
						createUserInKeycloak(session, realm, externalUser);
						result.increaseAdded();
					}
				}
				session.getTransactionManager().commit(); // Commit once after all users
				logger.debug("User synchronization completed successfully");
			} else {
				logger.error("Realm could not be found or set in the session context.");
			}
		} catch (Exception e) {
			logger.error("User synchronization failed", e);
		}
		return result;
	}

	private void updateUserInKeycloak(UserModel keycloakUser, User externalUser) {
		try {
			keycloakUser.setFirstName(externalUser.getGivenname());
			keycloakUser.setLastName(externalUser.getLastname());
			keycloakUser.setEmail(externalUser.getEmail());
			keycloakUser.setSingleAttribute(CUSTOM_ATTR_DEPARTMENT, externalUser.getDepartment());
			logger.debug("Updated user in Keycloak:{}", keycloakUser.getEmail());
		} catch (Exception e) {
			logger.error("Error updating user in Keycloak", e);
		}

	}

	private void createUserInKeycloak(KeycloakSession keycloakSession, RealmModel realm ,User externalUser) {

		UserModel newUser = keycloakSession.users().addUser(realm, externalUser.getEmail());
		newUser.setEnabled(true);
		newUser.setFirstName(externalUser.getGivenname());
		newUser.setLastName(externalUser.getLastname());
		newUser.setEmail(externalUser.getEmail());
		newUser.setSingleAttribute(CUSTOM_ATTR_DEPARTMENT, externalUser.getDepartment());
		logger.debug("Created new user in Keycloak: {}", newUser.getEmail());
	}
	@Override
	public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		logger.error("syncSince not implemented");
		return null;
	}
}
