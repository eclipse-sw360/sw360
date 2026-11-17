/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.common;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Centralized service for managing SW360 users in CouchDB.
 *
 * <p>This service is used by both the Keycloak User Storage provider and Event Listeners
 * to interact with the SW360 database via the UserRepository.</p>
 */
public class Sw360UserService {
    private static final Logger logger = LoggerFactory.getLogger(Sw360UserService.class);

    private static final DatabaseConnectorCloudant connector;
    private static final UserRepository repository;

    static {
        try {
            connector = new DatabaseConnectorCloudant(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_USERS
            );
            repository = new UserRepository(connector);
            logger.info("Shared SW360 user service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize CouchDB connection for Keycloak common", e);
            throw new RuntimeException("Cannot initialize CouchDB connection: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a user by email address.
     *
     * @param email the email address to search for
     * @return User if found, null otherwise
     */
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Attempted to get user with null or empty email");
            return null;
        }

        try {
            User user = repository.getByEmail(email);
            if (user == null) {
                logger.debug("Found no user for email: {}", email);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user by email: {}", email, e);
            return null;
        }
    }

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID to search for
     * @return User if found, null otherwise
     */
    public User getUser(String id) {
        if (id == null || id.trim().isEmpty()) {
            logger.warn("Attempted to get user with null or empty ID");
            return null;
        }

        try {
            User user = connector.get(User.class, id);
            if (user == null) {
                logger.debug("No user found for ID: {}", id);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user by ID {}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves all users from the SW360 database.
     *
     * @return List of all users.
     */
    public List<User> getAllUsers() {
        try {
            return repository.getAll();
        } catch (Exception e) {
            logger.error("Error retrieving all users from SW360", e);
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a user by email or external ID.
     * First attempts to find by email, then searches by external ID using views.
     *
     * @param userIdentifier the email or external ID to search for
     * @return User if found, null otherwise
     */
    public User getUserByEmailOrExternalId(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            logger.warn("Attempted to get user with null or empty identifier");
            return null;
        }

        try {
            // First try by email/ID
            User user = repository.getByEmail(userIdentifier);
            if (user != null) {
                return user;
            }

            // If not found, search by external ID using view
            user = repository.getByExternalId(userIdentifier);
            if (user == null) {
                logger.debug("No user found for identifier: {}", userIdentifier);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user by identifier {}: {}", userIdentifier, e.getMessage(), e);
            return null;
        }
    }

    /**
     * The function first checks if the user exists with the email. If it does,
     * it calls copyUserProperties() to get missing values (like id and rev) and
     * calls the repo.update(). If no user is found by email, it falls back to a
     * lookup by external ID (GID), since the GID stays stable even when the
     * email changes at the identity provider (e.g. a ".ext" user becoming an
     * direct drops the ".ext" suffix from their email). When the fallback finds a
     * match, the existing email is preserved in formerEmailAddresses before the
     * new email is stored, so the same user record is updated instead of a
     * duplicate being created. If the user does not exist at all, it calls
     * repo.add() to create the new user.
     * @param user User to be created or updated
     * @return Created or updated user
     */
    public User createOrUpdateUser(User user, KeycloakConstants.ProviderService service) {
        if (user == null) {
            logger.warn("Attempted to add null user");
            return null;
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.warn("Attempted to add user without email");
            return null;
        }

        try {
            if (service == KeycloakConstants.ProviderService.USER_STORAGE_PROVIDER) {
                // Set default user group if not specified
                if (!user.isSetUserGroup()) {
                    user.setUserGroup(PermissionUtils.DEFAULT_USER_GROUP);
                    logger.debug("Set default user group to USER for user: {}", user.getEmail());
                }

                // Set ID to email if not specified
                if (!user.isSetId()) {
                    user.setId(user.getEmail());
                    logger.debug("Set user ID to email: {}", user.getEmail());
                }
            }

            // Check if user already exists, by current email first.
            User existingUser = repository.getByEmail(user.getEmail());

            // Fall back to a GID (external ID) lookup if no user matches the new
            // email. This covers the case where the email changed at the IdP
            // while the GID stayed the same.
            if (existingUser == null && isUsableExternalId(user.getExternalid())) {
                User userByExternalId = repository.getByExternalId(user.getExternalid());
                if (userByExternalId != null && !user.getEmail().equals(userByExternalId.getEmail())) {
                    logger.info("Email changed for user with external ID {}: {} -> {}",
                            user.getExternalid(), userByExternalId.getEmail(), user.getEmail());
                    user.setFormerEmailAddresses(
                            prepareFormerEmailAddresses(userByExternalId, user.getEmail()));
                    existingUser = userByExternalId;
                }
            }

            if (existingUser != null) {
                logger.debug("Found user already exists with ID: {}", existingUser.getId());
                copyUserProperties(user, existingUser);
                repository.update(user);
                return user;
            }

            // Set defaults for the user if missing.
            // Note: Do not set ID as it will be assigned by CouchDB
            // Set default user group if not specified
            if (!user.isSetUserGroup()) {
                user.setUserGroup(PermissionUtils.DEFAULT_USER_GROUP);
                logger.debug("Set default user group to USER for user: {}", user.getEmail());
            }

            // Create the user
            repository.add(user);
            logger.info("Successfully created user in SW360 database: {}", user.getEmail());
            return user;
        } catch (Exception e) {
            logger.error("Error saving user to SW360: {}", user.getEmail(), e);
            return null;
        }
    }

    /**
     * Checks whether the given external ID (GID) is usable for a lookup, i.e. it
     * is not null/blank and not the Keycloak default placeholder value.
     *
     * <p>Package-private so it can be exercised directly by {@code Sw360UserServiceTest}
     * without reflection.</p>
     *
     * @param externalId the external ID to check
     * @return true if the external ID can be used to look up a user
     */
    static boolean isUsableExternalId(String externalId) {
        return externalId != null && !externalId.trim().isEmpty()
                && !KeycloakConstants.DEFAULT_EXTERNAL_ID.equals(externalId);
    }

    /**
     * Builds the merged set of former email addresses when a user's email
     * changes while their external ID (GID) stays the same. The existing user's
     * current email is added to its former email addresses, unless it already
     * equals the new email.
     *
     * <p>Package-private so it can be exercised directly by {@code Sw360UserServiceTest}
     * without reflection.</p>
     *
     * @param existingUser the user record currently stored in CouchDB
     * @param newEmail     the new email address reported by the identity provider
     * @return merged set of former email addresses
     */
    static Set<String> prepareFormerEmailAddresses(@Nonnull User existingUser, String newEmail) {
        Set<String> formerEmailAddresses = new HashSet<>();
        if (existingUser.isSetFormerEmailAddresses()) {
            formerEmailAddresses.addAll(existingUser.getFormerEmailAddresses());
        }
        formerEmailAddresses.remove(newEmail);
        String oldEmail = existingUser.getEmail();
        if (oldEmail != null && !oldEmail.equals(newEmail)) {
            formerEmailAddresses.add(oldEmail);
        }
        return formerEmailAddresses;
    }

    /**
     * Copies fields which are in existing user to new user, except the email.
     * This makes sure ID and Rev are also carried over for updating the user
     * in CouchDB.
     * @param newUser      New user to be added
     * @param existingUser Existing user to get properties from
     */
    private void copyUserProperties(@Nonnull User newUser, @Nonnull User existingUser) {
        Set<User._Fields> ignoredFields = Set.of(
                User._Fields.ID, User._Fields.REVISION, User._Fields.EMAIL
        );

        newUser.setId(existingUser.getId());
        newUser.setRevision(existingUser.getRevision());

        for (User._Fields field : User._Fields.values()) {
            if (ignoredFields.contains(field)) {
                continue;
            }
            if (!newUser.isSet(field) && existingUser.isSet(field)) {
                newUser.setFieldValue(field, existingUser.getFieldValue(field));
            }
        }
    }
}
