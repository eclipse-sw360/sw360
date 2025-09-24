/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.spi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
import com.ibm.cloud.sdk.core.security.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing SW360 users in CouchDB.
 * Provides CRUD operations for user management in the Keycloak user storage provider.
 * 
 * @author SW360 Team
 */
public class Sw360UserService {
    private static final Logger logger = LoggerFactory.getLogger(Sw360UserService.class);
    private static final String COUCHDB_SERVICE_NAME = "sw360-couchdb";
    private static final String PROPERTIES_FILE_PATH = "/couchdb.properties";
    public static final String SYSTEM_CONFIGURATION_PATH = "/etc/sw360";

    // Static configuration variables (set by SPI factory as primary source)
    public static String couchdbUrl = null;
    public static String couchdbUsername = null;
    public static String couchdbPassword = null;
    public static String couchdbDatabase = null;

    private final DatabaseConnectorCloudant connector;
    
    /**
     * Initializes the SW360 user service with CouchDB connection.
     * 
     * @throws RuntimeException if CouchDB connection cannot be established
     */
    public Sw360UserService() {
        try {
            Properties props = loadProperties();
            
            // Priority: SPI config > Environment variables > Properties file > Defaults
            String url = getConfigValue(couchdbUrl, "COUCHDB_URL", props.getProperty("couchdb.url", "http://localhost:5984"));
            String username = getConfigValue(couchdbUsername, "COUCHDB_USER", props.getProperty("couchdb.user", ""));
            String password = getConfigValue(couchdbPassword, "COUCHDB_PASSWORD", props.getProperty("couchdb.password", ""));
            String database = getConfigValue(couchdbDatabase, null, props.getProperty("couchdb.usersdb", "sw360users"));
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new RuntimeException("CouchDB username and password are required for authentication");
            }
            
            logger.info("Initializing SW360 user service with CouchDB connection to: {}", url);
            Authenticator authenticator = CouchDbSessionAuthenticator.newAuthenticator(username, password);
            Cloudant client = new Cloudant(COUCHDB_SERVICE_NAME, authenticator);
            client.setServiceUrl(url);
            this.connector = new DatabaseConnectorCloudant(client, database);
            logger.info("SW360 user service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize CouchDB connection: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot initialize CouchDB connection: " + e.getMessage(), e);
        }
    }
    
    private String getConfigValue(String spiValue, String envKey, String fallbackValue) {
        if (spiValue != null && !spiValue.isEmpty()) {
            return spiValue;
        }
        if (envKey != null) {
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isEmpty()) {
                return envValue;
            }
        }
        return fallbackValue;
    }

    private Properties loadProperties() {
        Properties props = new Properties();

        // Try external file first
        java.io.File externalFile = new java.io.File(SYSTEM_CONFIGURATION_PATH, PROPERTIES_FILE_PATH);
        if (externalFile.exists()) {
            try (java.io.FileInputStream input = new java.io.FileInputStream(externalFile)) {
                props.load(input);
                logger.debug("Loaded CouchDB properties from external file: {}", externalFile.getAbsolutePath());
                return props;
            } catch (IOException e) {
                logger.warn("Error loading external CouchDB properties file: {}", e.getMessage());
            }
        }

        // Fallback to classpath resource
        try (InputStream input = getClass().getResourceAsStream("/"+PROPERTIES_FILE_PATH)) {
            if (input != null) {
                props.load(input);
                logger.debug("Loaded CouchDB properties from classpath: {}", PROPERTIES_FILE_PATH);
            } else {
                logger.warn("CouchDB properties file not found in classpath: {}", PROPERTIES_FILE_PATH);
            }
        } catch (IOException e) {
            logger.error("Error loading CouchDB properties from classpath: {}", e.getMessage(), e);
        }
        return props;
    }

    /**
     * Retrieves all users from the SW360 database.
     * 
     * @return List of all users, empty list if none found or on error
     */
    public List<User> getAllUsers() {
        try {
            logger.debug("Retrieving all users from SW360 database");
            List<User> users = connector.getAll(User.class);
            logger.info("Retrieved {} users from SW360 database", users.size());
            return users;
        } catch (Exception e) {
            logger.error("Error retrieving all users from SW360 database: {}", e.getMessage(), e);
            return Collections.emptyList();
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
            logger.debug("Retrieving user by email: {}", email);
            User user = connector.get(User.class, email);
            if (user != null) {
                logger.debug("User found for email: {}", email);
            } else {
                logger.debug("No user found for email: {}", email);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user by email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves a user by email or external ID.
     * First attempts to find by email, then searches all users by external ID.
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
            logger.debug("Retrieving user by email or external ID: {}", userIdentifier);
            
            // First try by email/ID
            User user = connector.get(User.class, userIdentifier);
            if (user != null) {
                logger.debug("User found by direct lookup for identifier: {}", userIdentifier);
                return user;
            }
            
            // If not found, search by external ID
            logger.debug("User not found by direct lookup, searching by external ID");
            List<User> allUsers = connector.getAll(User.class);
            Optional<User> foundUser = allUsers.stream()
                .filter(u -> userIdentifier.equals(u.getExternalid()))
                .findFirst();
                
            if (foundUser.isPresent()) {
                logger.debug("User found by external ID: {}", userIdentifier);
                return foundUser.get();
            } else {
                logger.debug("No user found for identifier: {}", userIdentifier);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving user by identifier {}: {}", userIdentifier, e.getMessage(), e);
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
            logger.debug("Retrieving user by ID: {}", id);
            User user = connector.get(User.class, id);
            if (user != null) {
                logger.debug("User found for ID: {}", id);
            } else {
                logger.debug("No user found for ID: {}", id);
            }
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user by ID {}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves a user by API token.
     * 
     * @param token the API token to search for
     * @return User if found, null otherwise
     */
    public User getUserByApiToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Attempted to get user with null or empty API token");
            return null;
        }
        
        try {
            logger.debug("Retrieving user by API token");
            List<User> allUsers = connector.getAll(User.class);
            Optional<User> foundUser = allUsers.stream()
                .filter(user -> user.getRestApiTokens() != null && 
                    user.getRestApiTokens().stream().anyMatch(apiToken -> token.equals(apiToken.getToken())))
                .findFirst();
                
            if (foundUser.isPresent()) {
                logger.debug("User found by API token");
                return foundUser.get();
            } else {
                logger.debug("No user found for API token");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving user by API token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves a user by OAuth client ID.
     * 
     * @param clientId the OAuth client ID to search for
     * @return User if found, null otherwise
     */
    public User getUserFromClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            logger.warn("Attempted to get user with null or empty client ID");
            return null;
        }
        
        try {
            logger.debug("Retrieving user by OAuth client ID: {}", clientId);
            List<User> allUsers = connector.getAll(User.class);
            Optional<User> foundUser = allUsers.stream()
                .filter(user -> user.getOidcClientInfos() != null && 
                    user.getOidcClientInfos().containsKey(clientId))
                .findFirst();
                
            if (foundUser.isPresent()) {
                logger.debug("User found by OAuth client ID: {}", clientId);
                return foundUser.get();
            } else {
                logger.debug("No user found for OAuth client ID: {}", clientId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving user by OAuth client ID {}: {}", clientId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Adds a new user to the SW360 database.
     * 
     * @param user the user to add
     * @return the created user if successful, null otherwise
     */
    public User addUser(User user) {
        if (user == null) {
            logger.warn("Attempted to add null user");
            return null;
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.warn("Attempted to add user without email");
            return null;
        }
        
        try {
            logger.info("Adding new user to SW360 database: {}", user.getEmail());
            
            // Set default user group if not specified
            if (user.getUserGroup() == null) {
                user.setUserGroup(UserGroup.USER);
                logger.debug("Set default user group to USER for user: {}", user.getEmail());
            }
            
            // Set ID to email if not specified
            if (user.getId() == null) {
                user.setId(user.getEmail());
                logger.debug("Set user ID to email: {}", user.getEmail());
            }
            
            // Check if user already exists
            User existingUser = connector.get(User.class, user.getId());
            if (existingUser != null) {
                logger.warn("User already exists with ID: {}", user.getId());
                return null;
            }
            
            // Create the user
            connector.update(user);
            logger.info("Successfully created user in SW360 database: {}", user.getEmail());
            return user;
            
        } catch (Exception e) {
            logger.error("Error creating user {} in SW360 database: {}", 
                user.getEmail(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Updates an existing user in the SW360 database.
     * 
     * @param user the user to update
     * @return RequestStatus.SUCCESS if successful, RequestStatus.FAILURE otherwise
     */
    public RequestStatus updateUser(User user) {
        if (user == null) {
            logger.warn("Attempted to update null user");
            return RequestStatus.FAILURE;
        }
        
        if (user.getId() == null || user.getId().trim().isEmpty()) {
            logger.warn("Attempted to update user without ID");
            return RequestStatus.FAILURE;
        }
        
        try {
            logger.info("Updating user in SW360 database: {}", user.getId());
            connector.update(user);
            logger.info("Successfully updated user in SW360 database: {}", user.getId());
            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            logger.error("Error updating user {} in SW360 database: {}", 
                user.getId(), e.getMessage(), e);
            return RequestStatus.FAILURE;
        }
    }
}