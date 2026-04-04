/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.db;

import org.eclipse.sw360.migration.poc.model.RequestStatus;
import org.eclipse.sw360.migration.poc.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub database handler — stands in for the real {@code UserDatabaseHandler} in
 * {@code backend/users/src/main/java/org/eclipse/sw360/users/db/}.
 *
 * <h3>The key change in the real migration:</h3>
 * <pre>
 *   // Before: constructed inside UserHandler's no-arg constructor
 *   public UserHandler() throws IOException {
 *       db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(),
 *               DatabaseSettings.COUCH_DB_USERS);
 *   }
 *
 *   // After: Spring @Repository bean, injected via constructor
 *   {@literal @}Repository
 *   public class UserDatabaseHandler {
 *       {@literal @}Autowired
 *       public UserDatabaseHandler(Cloudant client, {@literal @}Value("${sw360.couchdb.usersDb}") String dbName) {
 *           ...
 *       }
 *   }
 * </pre>
 *
 * <p>In this PoC the in-memory map replaces CouchDB to keep the module self-contained.
 * The real {@code UserDatabaseHandler} uses the IBM Cloudant Java SDK unchanged.
 */
@Repository
public class UserDatabaseHandler {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    public User getById(String id) {
        return store.get(id);
    }

    public User getByEmail(String email) {
        return store.values().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public User getByEmailOrExternalId(String email, String externalId) {
        return store.values().stream()
                .filter(u -> email.equals(u.getEmail()) || externalId.equals(u.getExternalid()))
                .findFirst()
                .orElse(null);
    }

    public List<User> getAll() {
        return new ArrayList<>(store.values());
    }

    public RequestStatus add(User user) {
        if (store.containsKey(user.getId())) {
            return RequestStatus.FAILURE;
        }
        store.put(user.getId(), user);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus update(User user) {
        if (!store.containsKey(user.getId())) {
            return RequestStatus.FAILURE;
        }
        store.put(user.getId(), user);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus delete(String id) {
        return store.remove(id) != null ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
    }
}
