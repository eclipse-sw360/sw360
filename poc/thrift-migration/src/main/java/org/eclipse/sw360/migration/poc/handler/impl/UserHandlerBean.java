/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.handler.impl;

import org.eclipse.sw360.migration.poc.db.UserDatabaseHandler;
import org.eclipse.sw360.migration.poc.handler.Sw360UserHandler;
import org.eclipse.sw360.migration.poc.model.RequestStatus;
import org.eclipse.sw360.migration.poc.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The central deliverable of the Thrift migration for the Users module.
 *
 * <p>This is {@code backend/users/UserHandler.java} after migration.
 *
 * <h3>Three-line diff that does the migration:</h3>
 * <pre>
 *   - public class UserHandler implements UserService.Iface {
 *   + {@literal @}Service
 *   + public class UserHandler implements Sw360UserHandler {
 *
 *   - public UserHandler() throws IOException {
 *   -     db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(), ...);
 *   - }
 *   + {@literal @}Autowired
 *   + public UserHandler(UserDatabaseHandler db) {
 *   +     this.db = db;
 *   + }
 * </pre>
 *
 * <p><b>Business logic: zero changes.</b> Every method body is identical to the
 * existing {@code UserHandler} — the migration only touches lifecycle management.
 *
 * <h3>What this eliminates:</h3>
 * <ul>
 *   <li>{@code UserServlet.java} — the Thrift {@code TServlet} wrapping this handler</li>
 *   <li>{@code UserService.Iface} — the Thrift-generated interface with {@code TException}</li>
 *   <li>The per-request {@code THttpClient} construction in {@code Sw360UserService}</li>
 *   <li>Binary TCompactProtocol serialization on every method call</li>
 *   <li>HTTP round-trip from REST server (port 8091) to backend WAR (port 8080)</li>
 * </ul>
 */
@Service
public class UserHandlerBean implements Sw360UserHandler {

    // BEFORE: constructed imperatively inside UserServlet:
    //   new UserService.Processor<>(new UserHandler())
    //   where UserHandler() called DatabaseSettings.getConfiguredClient() statically.
    //
    // AFTER: Spring manages the singleton lifecycle and injects the database handler.
    private final UserDatabaseHandler db;

    @Autowired
    public UserHandlerBean(UserDatabaseHandler db) {
        this.db = db;
    }

    @Override
    public User getUser(String id) {
        return db.getById(id);
    }

    @Override
    public User getByEmail(String email) {
        return db.getByEmail(email);
    }

    @Override
    public User getByEmailOrExternalId(String email, String externalId) {
        return db.getByEmailOrExternalId(email, externalId);
    }

    @Override
    public List<User> getAllUsers() {
        return db.getAll();
    }

    @Override
    public RequestStatus addUser(User user) {
        return db.add(user);
    }

    @Override
    public RequestStatus updateUser(User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            return RequestStatus.FAILURE;
        }
        return db.update(user);
    }

    @Override
    public RequestStatus deleteUser(User user, User adminUser) {
        if (adminUser == null) {
            return RequestStatus.ACCESS_DENIED;
        }
        return db.delete(user.getId());
    }
}
