/*
 * Copyright Siemens AG, 2024-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.event.listener.service;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Service class for managing SW360 users in CouchDB for Keycloak event listeners.
 *
 * <p>This service provides efficient CRUD operations for user management during Keycloak
 * event processing by using UserRepository.</p>
 *
 * <h3>Event Listener Context:</h3>
 * <p>This service is specifically designed for Keycloak event processing, providing
 * user management capabilities during authentication events, user registration,
 * and profile updates.</p>
 *
 * @author SW360 Team
 * @since 20.0.0
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
            logger.info("SW360 user service initialized successfully for event listener");
        } catch (Exception e) {
            logger.error("Failed to initialize CouchDB connection for event listener", e);
            throw new RuntimeException("Cannot initialize CouchDB connection: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a user by email address using CouchDB view for efficient lookup.
     *
     * @param email the email address to search for
     * @return User if found, null otherwise
     */
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Event listener attempted to get user with null or empty email");
            return null;
        }

        try {
            User user = repository.getByEmail(email);
            if (user == null) {
                logger.debug("Event listener found no user for email: {}", email);
            }
            return user;
        } catch (Exception e) {
            logger.error("Event listener error retrieving user by email: {}", email, e);
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
    public User createOrUpdateUser(User user) {
        if (user == null) {
            logger.warn("Event listener attempted to add null user");
            return null;
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.warn("Event listener attempted to add user without email");
            return null;
        }

        try {
            logger.info("Event listener creating or updating user to SW360 database: {}", user.getEmail());

            // Check if user already exists
            User existingUser = repository.getByEmail(user.getEmail());
            if (existingUser != null) {
                logger.debug("Event listener found user already exists with ID: {}", user.getId());
                copyUserProperties(user, existingUser);
                repository.update(user);
                logger.debug("Event listener updated user with ID: {}", user.getId());
                return user;
            }

            // Set defaults for the user if missing.
            // Note: Do not set ID as it will be assigned by CouchDB
            // Set default user group if not specified
            if (!user.isSetUserGroup()) {
                user.setUserGroup(PermissionUtils.DEFAULT_USER_GROUP);
                logger.debug("Event listener set default user group to USER for user: {}", user.getEmail());
            }

            // Create the user
            repository.add(user);
            logger.info("Event listener successfully created user in SW360 database: {}", user.getEmail());
            return user;
        } catch (Exception e) {
            logger.error("Event listener error creating user in SW360 database: {}", user.getEmail(), e);
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
    private void copyUserProperties(User newUser, User existingUser) {
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
