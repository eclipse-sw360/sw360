/*
SPDX-FileCopyrightText: © 2024-2026 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.spi;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.keycloak.spi.service.Sw360UserService;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Sw360UserStorageProviderFactory implements UserStorageProviderFactory<Sw360UserStorageProvider>, ImportSynchronization {
    public static final String PROVIDER_ID = "sw360-user-storage-jpa";
    public static final String SW360_USER_STORAGE_PROVIDER = "SW360 User Storage Provider";
    private static final Logger logger = LoggerFactory.getLogger(Sw360UserStorageProviderFactory.class);
    private static final String CUSTOM_ATTR_DEPARTMENT = "Department";
    private static final String CUSTOM_ATTR_EXTERNAL_ID = "externalId";
    private static final String DEFAULT_FIRST_NAME = "Not Provided";
    private static final String DEFAULT_LAST_NAME = "Not Provided";
    private static final String DEFAULT_DEPARTMENT = "Unknown";
    private static final String DEFAULT_EXTERNAL_ID = "N/A";

    private Sw360UserService sw360UserService;

    /**
     * Allows injection of a mock Sw360UserService for testing.
     */
    public void setSw360UserService(Sw360UserService sw360UserService) {
        this.sw360UserService = sw360UserService;
    }

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
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.error("syncSince not implemented");
        return null;
    }

    /**
     * Synchronizes users from the external service to Keycloak.
     * <p>
     * This method handles user synchronization by performing the following steps:
     * <ul>
     *     <li>Fetching all users from repository.</li>
     *     <li>Processing the fetched users in configurable-sized batches.</li>
     *     <li>Using a concurrent thread pool to efficiently process batches simultaneously.</li>
     *     <li>Updating existing Keycloak users or creating new ones as required.</li>
     *     <li>Applying retry mechanisms with exponential backoff for failed batch operations.</li>
     * </ul>
     * After synchronization, a summary result including counts of successfully added users, updated users, and failures
     * is returned for monitoring or logging purposes.
     *
     * @param sessionFactory the Keycloak session factory used to obtain sessions.
     * @param realmId        the ID of the Keycloak realm where users are synchronized.
     * @param model          the user storage provider model with configuration settings.
     * @return a {@link SynchronizationResult} summarizing the outcome of the synchronization process
     * including the total count of added, updated, and failed user synchronizations.
     * @throws IllegalStateException if synchronization consistently fails after the configured maximum retry attempts.
     * @see ImportSynchronization#sync(KeycloakSessionFactory, String, UserStorageProviderModel)
     */
    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        logger.info("Starting user synchronization");

        SynchronizationResult totalResult = new SynchronizationResult();
        if (sw360UserService == null) {
            sw360UserService = new Sw360UserService();
        }
        List<User> externalUsers = sw360UserService.getAllUsers();
        logger.info("Fetched {} users from external service", externalUsers.size());

        final int BATCH_SIZE = 100; // Adjusted batch size for better throughput
        final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
        final int MAX_RETRIES = 2;

        List<List<User>> batches = new ArrayList<>();
        for (int i = 0; i < externalUsers.size(); i += BATCH_SIZE) {
            batches.add(externalUsers.subList(i, Math.min(i + BATCH_SIZE, externalUsers.size())));
        }

        Set<String> existingUserEmails = getExistingUserEmails(sessionFactory, realmId);
        logger.info("Existing Keycloak users fetched: {}", existingUserEmails.size());

        try (ExecutorServiceWrapper executorWrapper = new ExecutorServiceWrapper(
                Executors.newFixedThreadPool(THREAD_POOL_SIZE))) {
            ExecutorService executor = executorWrapper.getExecutor();
            CompletionService<BatchResult> completionService = new ExecutorCompletionService<>(executor);

            int batchNumber = 0;
            for (List<User> batch : batches) {
                final int currentBatchNumber = ++batchNumber;
                completionService.submit(() -> processBatchWithRetry(
                        sessionFactory, realmId, batch, existingUserEmails, currentBatchNumber, batches.size(), MAX_RETRIES));
            }

            for (int i = 0; i < batches.size(); i++) {
                try {
                    Future<BatchResult> future = completionService.take();
                    BatchResult batchResult = future.get(10, TimeUnit.MINUTES);
                    totalResult.add(batchResult.getSyncResult());
                    logger.info("Processed batch {}/{}. Added: {}, Updated: {}, Failed: {}",
                            (i + 1), batches.size(),
                            totalResult.getAdded(), totalResult.getUpdated(), totalResult.getFailed());
                } catch (Exception e) {
                    logger.error("Batch processing failed", e);
                    totalResult.increaseFailed();
                }
            }
        }

        logger.info("Sync completed. Total - Added: {}, Updated: {}, Failed: {}",
                totalResult.getAdded(), totalResult.getUpdated(), totalResult.getFailed());
        return totalResult;
    }

    /**
     * Retrieves existing user emails from Keycloak for the specified realm.
     * <p>
     * This method fetches all users in the specified realm and collects their email addresses into a set.
     * It is used to check against existing users when processing external users.
     *
     * @param sessionFactory the Keycloak session factory used to create sessions.
     * @param realmId        the ID of the Keycloak realm from which to retrieve user emails.
     * @return a set of email addresses of existing users in the specified realm.
     */
    private Set<String> getExistingUserEmails(KeycloakSessionFactory sessionFactory, String realmId) {
        Set<String> emails = Collections.emptySet();
        try (KeycloakSession session = sessionFactory.create()) {
            session.getTransactionManager().begin();
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                logger.error("Realm with ID '{}' not found", realmId);
                return emails;
            }
            session.getContext().setRealm(realm);
            emails = session.users().searchForUserStream(realm, Collections.emptyMap())
                    .map(UserModel::getUsername)
                    .filter(Objects::nonNull)
                    .filter(u -> !u.startsWith("service-account-"))
                    .collect(Collectors.toSet());
            session.getTransactionManager().commit();
        } catch (Exception e) {
            logger.error("Error retrieving existing user emails for realm '{}'", realmId, e);
        }
        return emails;
    }

    /**
     * Processes a batch of external users with retry logic.
     * <p>
     * This method attempts to process a batch of users, retrying up to a specified maximum number of times
     * in case of failures. It uses exponential backoff for retries.
     * It is designed to handle transient errors that may occur during batch processing,
     * allowing for more robust synchronization of users from an external service to Keycloak.
     *
     * @param sessionFactory     the Keycloak session factory used to create sessions.
     * @param realmId            the ID of the Keycloak realm where users are synchronized.
     * @param batch              the list of external users to process.
     * @param existingUserEmails the set of email addresses of existing users in Keycloak.
     * @param batchNumber        the current batch number (for logging).
     * @param totalBatches       the total number of batches (for logging).
     * @param maxRetries         the maximum number of retry attempts for processing the batch.
     * @return a {@link BatchResult} containing the synchronization result for this batch.
     */
    private BatchResult processBatchWithRetry(KeycloakSessionFactory sessionFactory, String realmId, List<User> batch,
                                              Set<String> existingUserEmails, int batchNumber, int totalBatches, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return processBatch(sessionFactory, realmId, batch, existingUserEmails);
            } catch (Exception e) {
                if (++attempts >= maxRetries) throw e;
                logger.warn("Batch {}/{} failed (attempt {}/{}), retrying...", batchNumber, totalBatches, attempts, maxRetries, e);
                try {
                    Thread.sleep(1000L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new IllegalStateException("Max retries reached unexpectedly");
    }

    /**
     * Processes a batch of external users and updates or creates them in Keycloak.
     * <p>
     * This method handles the processing of a batch of users by creating a Keycloak session,
     * iterating through each user, and either updating an existing user or creating a new one
     * based on whether the user's email exists in Keycloak.
     * It includes retry logic to handle transient errors during batch processing.
     * It also logs errors for individual user processing failures, allowing the batch to continue processing
     * without failing the entire batch.
     *
     * @param sessionFactory     the Keycloak session factory used to create sessions.
     * @param realmId            the ID of the Keycloak realm where users are synchronized.
     * @param batch              the list of external users to process.
     * @param existingUserEmails the set of email addresses of existing users in Keycloak.
     * @return a {@link BatchResult} containing the synchronization result for this batch.
     */
    private BatchResult processBatch(KeycloakSessionFactory sessionFactory, String realmId, List<User> batch, Set<String> existingUserEmails) {
        BatchResult batchResult = new BatchResult();
        KeycloakSession session = null;
        try {
            session = sessionFactory.create();
            session.getTransactionManager().begin();
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                throw new IllegalStateException("Realm with ID '" + realmId + "' not found");
            }
            session.getContext().setRealm(realm);

            for (User externalUser : batch) {
                try {
                    processExternalUser(session, realm, externalUser, existingUserEmails, batchResult.getSyncResult());
                } catch (Exception e) {
                    logger.error("Error processing user: {}", externalUser.getEmail(), e);
                    batchResult.getSyncResult().increaseFailed();
                }
            }

            session.getTransactionManager().commit();
        } catch (Exception e) {
            logger.error("Batch transaction failure", e);
            if (session != null && session.getTransactionManager().isActive()) {
                try {
                    session.getTransactionManager().rollback();
                } catch (Exception rollbackException) {
                    logger.error("Error during transaction rollback", rollbackException);
                }
            }
            batchResult.getSyncResult().increaseFailed(); // Mark the batch as failed
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return batchResult;
    }

    /**
     * Processes an external user by either updating an existing Keycloak user or creating a new one.
     * <p>
     * This method checks if the external user's email exists in Keycloak. If it does, it updates the user
     * with the attributes from the external user. If it does not exist, it creates a new user in Keycloak.
     * It also handles any exceptions that may occur during the process and logs them accordingly.
     *
     * @param session            the Keycloak session used to interact with Keycloak.
     * @param realm              the Keycloak realm where users are synchronized.
     * @param externalUser       the external user to be processed.
     * @param existingUserEmails the set of email addresses of existing users in Keycloak.
     * @param result             the synchronization result to update with added or updated counts.
     */
    private void processExternalUser(KeycloakSession session, RealmModel realm, User externalUser, Set<String> existingUserEmails, SynchronizationResult result) {
        UserModel user = session.users().getUserByUsername(realm, externalUser.getEmail());
        if (existingUserEmails.contains(externalUser.getEmail()) && user != null) {
            if (updateUserInKeycloak(user, realm, externalUser, externalUser.getUserGroup())) {
                result.increaseUpdated();
            }
        } else {
            if (createUserInKeycloak(session, realm, externalUser, externalUser.getUserGroup())) {
                result.increaseAdded();
            }
        }
    }

    /**
     * Populates the user attributes from the external user.
     * <p>
     * This method sets the user's first name, last name, email, username, department, and external ID
     * based on the attributes from the external user. If any of these attributes are null or empty,
     * it assigns a default value and logs a warning.
     *
     * @param user                  the Keycloak user to be populated with attributes.
     * @param realm                 the Keycloak realm.
     * @param externalUser          the external user with attributes to populate.
     * @param externalUserUserGroup the external user group for assigning to the user.
     */
    private void populateUserAttributes(UserModel user, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
        user.setFirstName(
                Optional.ofNullable(externalUser.getGivenname())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Given name is null or empty for user: {}", externalUser.getEmail());
                            return DEFAULT_FIRST_NAME;
                        })
        );

        user.setLastName(
                Optional.ofNullable(externalUser.getLastname())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Last name is null or empty for user: {}", externalUser.getEmail());
                            return DEFAULT_LAST_NAME;
                        })
        );

        user.setEmail(externalUser.getEmail());
        user.setEmailVerified(true);

        user.setUsername(externalUser.getEmail());


        user.setSingleAttribute(CUSTOM_ATTR_DEPARTMENT,
                Optional.ofNullable(externalUser.getDepartment())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Department is null or empty for user: {}", externalUser.getEmail());
                            return DEFAULT_DEPARTMENT;
                        })
        );

        user.setSingleAttribute(CUSTOM_ATTR_EXTERNAL_ID,
                Optional.ofNullable(externalUser.getExternalid())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("External ID is null or empty for user: {}", externalUser.getEmail());
                            return DEFAULT_EXTERNAL_ID;
                        })
        );

        assignGroupToUser(user, realm, externalUserUserGroup);
    }

    /**
     * Updates the user in Keycloak with the attributes from the external user.
     * <p>
     * This method sets the user as enabled and populates the user attributes from the external user.
     *
     * @param keycloakUser          the Keycloak user to be updated.
     * @param realm                 the Keycloak realm.
     * @param externalUser          the external user with updated attributes.
     * @param externalUserUserGroup the external user group.
     */
    private boolean updateUserInKeycloak(UserModel keycloakUser, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
        try {
            populateUserAttributes(keycloakUser, realm, externalUser, externalUserUserGroup);
            logger.debug("Updated user in Keycloak: {}", keycloakUser.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Error updating user in Keycloak", e);
            return false;
        }
    }

    /**
     * Creates a new user in Keycloak with the attributes from the external user.
     * <p>
     * This method sets the user as enabled and populates the user attributes from the external user.
     * It also assigns the user to a group based on the external user's group.
     *
     * @param session               the Keycloak session.
     * @param realm                 the Keycloak realm.
     * @param externalUser          the external user to be created.
     * @param externalUserUserGroup the external user group.
     */
    private boolean createUserInKeycloak(KeycloakSession session, RealmModel realm, User externalUser, UserGroup externalUserUserGroup) {
        try {
            session.getContext().setRealm(realm);
            UserModel newUser = session.users().addUser(realm, externalUser.getEmail());
            newUser.setEnabled(true);
            populateUserAttributes(newUser, realm, externalUser, externalUserUserGroup);
            logger.debug("Created new user  {}", newUser.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Error creating user in Keycloak", e);
            return false;
        }
    }

    /**
     * Assigns the user to a group based on the external user's group.
     * <p>
     * This method checks if the external user group is valid and not empty. If it is valid, it assigns the user
     * to the corresponding group in Keycloak. If the user is already in the target group, it does nothing.
     *
     * @param user                  the Keycloak user to be assigned to a group.
     * @param realm                 the Keycloak realm.
     * @param externalUserUserGroup the external user group.
     */
    private void assignGroupToUser(UserModel user, RealmModel realm, UserGroup externalUserUserGroup) {
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

        // Collect current groups to avoid stream reuse
        List<GroupModel> currentGroups = user.getGroupsStream().toList();

        // Check if the user is already in the target group
        if (currentGroups.stream().anyMatch(g -> g.equals(targetGroup))) {
            logger.debug("User {} is already in group {}", user.getEmail(), groupName);
            return;
        }

        // Remove user from all other groups
        currentGroups.forEach(user::leaveGroup);

        // Add user to the target group
        user.joinGroup(targetGroup);
        logger.debug("Assigned user {} to group {}", user.getEmail(), groupName);
    }


    /**
     * Wrapper for ExecutorService to handle shutdown and termination.
     * <p>
     * This class provides a way to manage the lifecycle of an ExecutorService, ensuring that it is properly shut down
     * and terminated when no longer needed. It implements AutoCloseable to allow for try-with-resources usage.
     * It handles InterruptedException during shutdown and ensures that the executor is terminated gracefully.
     * It also provides a method to retrieve the underlying ExecutorService for further use if needed.
     *
     * @see ExecutorService
     */
    private static class ExecutorServiceWrapper implements AutoCloseable {
        private final ExecutorService executor;

        ExecutorServiceWrapper(ExecutorService executor) {
            this.executor = executor;
        }

        ExecutorService getExecutor() {
            return executor;
        }

        @Override
        public void close() {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Represents the result of processing a batch of users.
     * <p>
     * This class encapsulates the synchronization result for a batch of users processed during synchronization.
     * It contains a {@link SynchronizationResult} that tracks the number of added, updated, and failed user synchronizations.
     */
    @Getter
    private static class BatchResult {
        private final SynchronizationResult syncResult;

        public BatchResult() {
            this.syncResult = new SynchronizationResult();
        }
    }

}
