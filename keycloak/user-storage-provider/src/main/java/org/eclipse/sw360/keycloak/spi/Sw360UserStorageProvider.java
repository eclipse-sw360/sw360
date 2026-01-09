/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.keycloak.spi.model.UserAdapter;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Sw360UserStorageProvider implements UserStorageProvider, UserRegistrationProvider, UserLookupProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
	private static final Logger logger = Logger.getLogger(Sw360UserStorageProvider.class);
	public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

	protected ComponentModel model;
	protected KeycloakSession session;
	protected Sw360UserService sw360UserService;

	public Sw360UserStorageProvider(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.model = model;
		try {
			sw360UserService = new Sw360UserService();
		} catch (Exception e) {
			logger.warnf("Failed to initialize SW360 user service: %s. Provider will operate in limited mode.", e.getMessage());
			sw360UserService = null;
		}
	}

	@Override
	public void preRemove(RealmModel realm) {
		logger.debugf("Pre-removing realm: %s", realm.getName());
	}

	@Override
	public void preRemove(RealmModel realm, GroupModel group) {
		logger.debugf("Pre-removing group: %s from realm: %s", group.getName(), realm.getName());
	}

	@Override
	public void preRemove(RealmModel realm, RoleModel role) {
		logger.debugf("Pre-removing role: %s from realm: %s", role.getName(), realm.getName());

	}

	@Override
	public void close() {
		logger.debug("Closing Sw360UserStorageProvider");
	}

	//USer Registration Provider

	/**
	 * @param realm    a reference to the realm
	 * @param username a username the created user will be assigned
	 * @return
	 */
	@Override
	public UserModel addUser(RealmModel realm, String username) {
		logger.debugf("Adding user with username: %s to the sw360 user database: %s", username, realm.getName());
		return null;
	}

	/**
	 * @param realm a reference to the realm
	 * @param user  a reference to the user that is removed
	 * @return
	 */
	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		logger.debugf("Removing user with username: %s from realm: %s", user.getUsername(), realm.getName());
		return false;
	}

	//USer Lookup Provider
	/**
	 * Retrieves a user by their ID in the specified realm.
	 *
	 * This method attempts to find a user in the SW360 user database using their ID.
	 * It converts the Keycloak ID to an external ID (CouchDB ID) and fetches the user.
	 * If the user is found, it returns a UserAdapter object; otherwise, it returns null.
	 *
	 * @param realm the realm model
	 * @param id    the ID of the user
	 * @return the UserModel object representing the user, or null if the user is not found
	 */
	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		logger.debugf("Attempting to retrieve user by ID: %s in sw360 user database: ", id );
		String persistenceId = StorageId.externalId(id);
		logger.debugf("Converted ID to external ID (CouchDB ID): %s", persistenceId);
		User user = sw360UserService.getUser(persistenceId);
		logger.debugf("Retrieved user: %s", user);
		if (user == null) {
			logger.warnf("User not found by ID: %s in sw360 user database: ", persistenceId);
			return null;
		}
		logger.debugf("User found by ID: %s in sw360 user database: %s", id, realm.getName());
		return new UserAdapter(session, realm, model, user);
	}

	/**
	 * @param realm    the realm model
	 * @param username (case-sensitivity is controlled by storage)
	 * @return
	 */
	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		logger.debugf("Attempting to retrieve user by username: %s", username);
		User user = null;
		try {
			user = sw360UserService.getUserByEmailOrExternalId(username);
			if (user == null) {
				logger.debugf("Could not find user by username: %s", username);
				return null;
			}
		} catch(Exception ex) {
			logger.errorf("Exception occurred while retrieving user by username: %s, error: %s", username, ex.getMessage());
			return null;
		}
		logger.debugf("User found by username: %s", username);
		return new UserAdapter(session, realm, model, user);
	}

	/**
	 * @param realm the realm model
	 * @param email email address
	 * @return
	 */
	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		logger.debugf("Attempting to retrieve user by email: %s in sw360 user database: ", email);
		User user = null;
		try {
			user = sw360UserService.getUserByEmailOrExternalId(email);
			if (user == null) {
				logger.warnf("Could not find user by email: %s in sw360 user database", email);
				return null;
			}
		} catch (Exception ex) {
			logger.errorf("Exception occurred while retrieving user by email: %s in sw360 user database: , error: %s", email, ex.getMessage());
			return null;
		}
		return new UserAdapter(session, realm, model, user);
	}

	//CredentialInputValidator

	/**
	 * Checks if the user is configured for the given credential type in the specified realm.
	 *
	 * This method verifies if the credential type is supported and if the user's password is not null.
	 *
	 * @param realm the realm in which the user exists.
	 * @param user the user for whom the credential type configuration is to be checked.
	 * @param credentialType the type of credential to check.
	 * @return true if the user is configured for the given credential type, false otherwise.
	 */
	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		logger.debugf("Checking if user: %s in realm: %s is configured for credential type: %s", user.getUsername(), realm.getName(), credentialType);
		boolean isConfigured = supportsCredentialType(credentialType) && getPassword(user) != null;
		logger.debugf("User: %s in realm: %s configured for credential type: %s: %s", user.getUsername(), realm.getName(), credentialType, isConfigured);
		return isConfigured;
	}

	/**
	 * Validates the given credential input for the specified user in the realm.
	 * <p>
	 * This method checks if the user is configured for the given credential type and if the credential type is supported.
	 * It also verifies if the credential input is an instance of UserCredentialModel.
	 * If all conditions are met, it compares the provided credential value with the user's stored password.
	 *
	 * @param realm           The realm to which the credential belongs.
	 * @param user            The user for whom the credential is to be validated.
	 * @param credentialInput The credential details to verify.
	 * @return true if the credential is valid, false otherwise.
	 */
	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
		logger.debugf("Validating credential for user: %s in sw360 user database with credential type: %s", user.getEmail(), credentialInput.getType());
		boolean isValid = false;
		if (!supportsCredentialType(credentialInput.getType()) || !(credentialInput instanceof UserCredentialModel)) {
			logger.debugf("Credential for user: %s in realm: %s with credential type: %s is valid: false", user.getEmail(), realm.getName(), credentialInput.getType());
			return false;
		}

		return isValid;
	}

	public String getPassword(UserModel user) {
		logger.debugf("Retrieving password for user from sw30 database: %s", user.getEmail());
		User sw360User = sw360UserService.getUserByEmail(user.getEmail());
		return sw360User.getPassword();
	}


	//CredentialInputUpdater

	/**
	 * Checks if the given credential type is supported.
	 * <p>
	 * This method verifies if the provided credential type matches the password credential type.
	 *
	 * @param credentialType the type of credential to check.
	 * @return true if the credential type is supported, false otherwise.
	 */
	@Override
	public boolean supportsCredentialType(String credentialType) {
		logger.debugf("Checking if credential type is supported: %s", credentialType);
		boolean isSupported = PasswordCredentialModel.TYPE.equals(credentialType);
		logger.debugf("Credential type %s supported: %s", credentialType, isSupported);
		return isSupported;
	}

	/**
	 * Updates the credential for the given user in the specified realm.
	 *
	 * This method checks if the credential type is supported and if the input is an instance of UserCredentialModel.
	 * If both conditions are met, it updates the user's password with the provided credential value.
	 *
	 * @param realm the realm in which the user exists.
	 * @param user the user whose credential is to be updated.
	 * @param input the credential input containing the new credential value.
	 * @return true if the credential was successfully updated, false otherwise.
	 */
	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		logger.debugf("Updating credential for user: %s in realm: %s with credential type: %s", user.getUsername(), realm.getName(), input.getType());
		if (!supportsCredentialType(input.getType())) {
			logger.debugf("Credential update failed for user: %s in realm: %s with credential type: %s", user.getUsername(), realm.getName(), input.getType());
			return false;
		}
		logger.debug("UserCredentialModel(ID): " + input.getCredentialId());
		UserAdapter adapter = getUserAdapter(user);
		adapter.setPassword(input.getChallengeResponse());
		logger.debugf("Credential updated successfully for user: %s in realm: %s with credential type: %s", user.getUsername(), realm.getName(), input.getType());
		return true;
	}

	/**
	 * Disables the specified credential type for the given user in the realm.
	 *
	 * @param realm the realm in which the user exists.
	 * @param user the user for whom the credential type is to be disabled.
	 * @param credentialType the type of credential to be disabled.
	 */
	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
		logger.debugf("Disabling credential type: %s for user: %s in realm: %s", credentialType, user.getUsername(), realm.getName());
		if (!supportsCredentialType(credentialType)) {
			logger.debugf("Credential type: %s is not supported for user: %s in realm: %s", credentialType, user.getUsername(), realm.getName());
			return;
		}

		getUserAdapter(user).setPassword(null);
		logger.debugf("Credential type: %s disabled for user: %s in realm: %s", credentialType, user.getUsername(), realm.getName());
	}

	/**
	 * @param realm a reference to the realm.
	 * @param user  the user whose credentials are being searched.
	 * @return
	 */
	@Override
	public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
		logger.debugf("Checking disableable credential types for user: %s in realm: %s", user.getUsername(), realm.getName());
		if (getUserAdapter(user).getPassword() != null) {
			Set<String> set = new HashSet<>();
			set.add(PasswordCredentialModel.TYPE);
			logger.debugf("Password is not null for user: %s in realm: %s, credential type is disableable", user.getUsername(), realm.getName());
			return set.stream();
		} else {
			logger.debugf("Password is null for user: %s in realm: %s, no credential types are disableable", user.getUsername(), realm.getName());
			return Stream.empty();
		}
	}

	/**
	 * Retrieves a UserAdapter instance for the given UserModel.
	 *
	 * @param user the UserModel instance for which the UserAdapter is to be retrieved.
	 * @return the UserAdapter instance corresponding to the given user.
	 */
	public UserAdapter getUserAdapter(UserModel user) {
		logger.debugf("Retrieving UserAdapter for user: %s", user.getUsername());
		if (user instanceof CachedUserModel) {
			logger.debugf("User: %s is a cached user", user.getUsername());
			return (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
		} else {
			logger.debugf("User: %s is a regular user", user.getUsername());
			return (UserAdapter) user;
		}
	}

	/**
	 * Caches the user data in the specified realm.
	 *
	 * This method caches the user data, including the password, in the specified realm.
	 *
	 * @param realm    the realm model
	 * @param user     the cached user model
	 * @param delegate the user model delegate
	 */
	@Override
	public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
		logger.debugf("Caching user data for realm: %s, user: %s", realm.getName(), user.getUsername());
		String password = ((UserAdapter)delegate).getPassword();
		if (password != null) {
			logger.debugf("Caching password for user: %s", user.getUsername());
			user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
		}
	}
}
