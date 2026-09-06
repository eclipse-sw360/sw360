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

import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
            User user = UserConverter.toThrift(repository.getByEmail(email));
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
            User user = UserConverter.toThrift(
                    connector.get(org.eclipse.sw360.datahandler.services.users.User.class, id));
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
            return repository.getAll().stream()
                    .map(UserConverter::toThrift)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
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
            User user = UserConverter.toThrift(repository.getByEmail(userIdentifier));
            if (user != null) {
                return user;
            }

            // If not found, search by external ID using view
            user = UserConverter.toThrift(repository.getByExternalId(userIdentifier));
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
     * calls the repo.update(). If the user does not exist, it calls repo.add()
     * to create the new user.
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
            org.eclipse.sw360.datahandler.services.users.User pojo = UserConverter.fromThrift(user);

            if (service == KeycloakConstants.ProviderService.USER_STORAGE_PROVIDER) {
                // Set default user group if not specified
                if (pojo.getUserGroup() == null) {
                    pojo.setUserGroup(org.eclipse.sw360.datahandler.services.users.UserGroup
                            .valueOf(PermissionUtils.DEFAULT_USER_GROUP.name()));
                    logger.debug("Set default user group to USER for user: {}", pojo.getEmail());
                }

                // Set ID to email if not specified
                if (pojo.getId() == null || pojo.getId().isEmpty()) {
                    pojo.setId(pojo.getEmail());
                    logger.debug("Set user ID to email: {}", pojo.getEmail());
                }
            }

            // Check if user already exists
            org.eclipse.sw360.datahandler.services.users.User existingUser =
                    repository.getByEmail(pojo.getEmail());
            if (existingUser != null) {
                logger.debug("Found user already exists with ID: {}", pojo.getId());
                copyUserProperties(pojo, existingUser);
                if (pojo.getType() == null) {
                    pojo.setType(SW360Constants.TYPE_USER);
                }
                repository.update(pojo);
                return UserConverter.toThrift(pojo);
            }

            // Set defaults for the user if missing.
            // Note: Do not set ID as it will be assigned by CouchDB
            // Set default user group if not specified
            if (pojo.getUserGroup() == null) {
                pojo.setUserGroup(org.eclipse.sw360.datahandler.services.users.UserGroup
                        .valueOf(PermissionUtils.DEFAULT_USER_GROUP.name()));
                logger.debug("Set default user group to USER for user: {}", pojo.getEmail());
            }
            if (pojo.getType() == null) {
                pojo.setType(SW360Constants.TYPE_USER);
            }

            // Create the user
            repository.add(pojo);
            logger.info("Successfully created user in SW360 database: {}", pojo.getEmail());
            return UserConverter.toThrift(pojo);
        } catch (Exception e) {
            logger.error("Error saving user to SW360: {}", user.getEmail(), e);
            return null;
        }
    }

    /**
     * Copies fields which are in existing user to new user, except the email.
     * This makes sure ID and Rev are also carried over for updating the user
     * in CouchDB.
     * @param newUser      New user to be added
     * @param existingUser Existing user to get properties from
     */
    private void copyUserProperties(
            @Nonnull org.eclipse.sw360.datahandler.services.users.User newUser,
            @Nonnull org.eclipse.sw360.datahandler.services.users.User existingUser) {
        newUser.setId(existingUser.getId());
        newUser.setRevision(existingUser.getRevision());

        if (newUser.getType() == null) {
            newUser.setType(existingUser.getType());
        }
        if (newUser.getUserGroup() == null) {
            newUser.setUserGroup(existingUser.getUserGroup());
        }
        if (newUser.getExternalid() == null) {
            newUser.setExternalid(existingUser.getExternalid());
        }
        if (newUser.getFullname() == null) {
            newUser.setFullname(existingUser.getFullname());
        }
        if (newUser.getGivenname() == null) {
            newUser.setGivenname(existingUser.getGivenname());
        }
        if (newUser.getLastname() == null) {
            newUser.setLastname(existingUser.getLastname());
        }
        if (newUser.getDepartment() == null) {
            newUser.setDepartment(existingUser.getDepartment());
        }
        if (newUser.getWantsMailNotification() == null) {
            newUser.setWantsMailNotification(existingUser.getWantsMailNotification());
        }
        if (newUser.getNotificationPreferences() == null) {
            newUser.setNotificationPreferences(existingUser.getNotificationPreferences());
        }
        if (newUser.getFormerEmailAddresses() == null) {
            newUser.setFormerEmailAddresses(existingUser.getFormerEmailAddresses());
        }
        if (newUser.getRestApiTokens() == null) {
            newUser.setRestApiTokens(existingUser.getRestApiTokens());
        }
        if (newUser.getMyProjectsPreferenceSelection() == null) {
            newUser.setMyProjectsPreferenceSelection(existingUser.getMyProjectsPreferenceSelection());
        }
        if (newUser.getSecondaryDepartmentsAndRoles() == null) {
            newUser.setSecondaryDepartmentsAndRoles(existingUser.getSecondaryDepartmentsAndRoles());
        }
        if (newUser.getPrimaryRoles() == null) {
            newUser.setPrimaryRoles(existingUser.getPrimaryRoles());
        }
        if (newUser.getDeactivated() == null) {
            newUser.setDeactivated(existingUser.getDeactivated());
        }
        if (newUser.getOidcClientInfos() == null) {
            newUser.setOidcClientInfos(existingUser.getOidcClientInfos());
        }
        if (newUser.getPassword() == null) {
            newUser.setPassword(existingUser.getPassword());
        }
    }
}
