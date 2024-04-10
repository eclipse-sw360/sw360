/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.util.Date;
import java.util.List;

public class Sw360UserStorageProviderFactory implements UserStorageProviderFactory<Sw360UserStorageProvider>, ImportSynchronization {
	public static final String PROVIDER_ID = "sw360-user-storage-jpa";

	private static final Logger logger = Logger.getLogger(Sw360UserStorageProviderFactory.class);
	public static final String SW360_USER_STORAGE_PROVIDER = "SW360 User Storage Provider";

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
     * Synchronizes users from an external source into Keycloak.
     *
     * This method fetches all users from an external service and synchronizes them with the Keycloak realm.
     * It either creates new users in Keycloak or updates existing users based on the external data.
     *
     * @param sessionFactory the Keycloak session factory.
     * @param realmId the ID of the realm in which to synchronize users.
     * @param model the user storage provider model.
     * @return a SynchronizationResult object containing the results of the synchronization process.
     */
	@Override
	public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		logger.debug("sync");
		SynchronizationResult result = new SynchronizationResult();
		KeycloakSession session = sessionFactory.create();
		KeycloakTransactionManager transactionManager = session.getTransactionManager();
		transactionManager.begin();
		try {
			RealmModel realm = session.realms().getRealm(realmId);
			Sw360UserService sw360UserService = new Sw360UserService();
			List<User> externalUsers = sw360UserService.getAllUsers();

			for (User externalUser : externalUsers) {
				UserModel keycloakUser = session.users().getUserByUsername(realm, externalUser.getEmail());
				if (keycloakUser == null) {
					// User does not exist in Keycloak, create a new user\
					keycloakUser.setEmail(externalUser.getEmail());
					keycloakUser.setFirstName(externalUser.getGivenname());
					keycloakUser.setLastName(externalUser.getLastname());
					keycloakUser.setEnabled(true);
					keycloakUser = session.users().addUser(realm, externalUser.getEmail());
					result.increaseAdded();
				} else {
					// User exists, update their information
					keycloakUser.setEmail(externalUser.getEmail());
					keycloakUser.setFirstName(externalUser.getGivenname());
					keycloakUser.setLastName(externalUser.getLastname());
					result.increaseUpdated();
				}
			}
			transactionManager.commit();
		} catch (Exception e) {
			transactionManager.rollback();
			throw e;
		} finally {
			session.close();
		}
		return result;
	}

	@Override
	public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
		logger.debug("syncSince");
		return null;
	}
}
