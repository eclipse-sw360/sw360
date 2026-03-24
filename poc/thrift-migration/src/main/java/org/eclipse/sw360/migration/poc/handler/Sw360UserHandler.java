/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.handler;

import org.eclipse.sw360.migration.poc.model.RequestStatus;
import org.eclipse.sw360.migration.poc.model.User;

import java.util.List;

/**
 * Plain Java interface replacing the Thrift-generated {@code UserService.Iface}.
 *
 * <h3>The core migration contract:</h3>
 * <pre>
 *   // Before: generated from users.thrift by maven-thrift-plugin
 *   public interface UserService.Iface {
 *       User getUser(String id) throws SW360Exception;
 *       List&lt;User&gt; getAllUsers();
 *       RequestStatus addUser(User user) throws TException;
 *       ...
 *   }
 *
 *   // After: hand-maintained plain Java interface in libraries/datahandler
 *   public interface Sw360UserHandler {
 *       User getUser(String id);
 *       List&lt;User&gt; getAllUsers();
 *       RequestStatus addUser(User user);
 *       ...
 *   }
 * </pre>
 *
 * <p>Method signatures are intentionally identical to {@code UserService.Iface} so that
 * {@code UserHandler.java} in {@code backend/users/} can implement this interface with
 * a one-line change: {@code implements UserService.Iface} → {@code implements Sw360UserHandler}.
 * No business logic changes are required in the handler itself.
 *
 * <p>The {@code throws TException} clauses are removed — exceptions that can actually
 * occur are declared as {@code SW360Exception} (converted to a plain RuntimeException)
 * or left unchecked per the handler's existing behavior.
 */
public interface Sw360UserHandler {

    User getUser(String id);

    User getByEmail(String email);

    User getByEmailOrExternalId(String email, String externalId);

    List<User> getAllUsers();

    RequestStatus addUser(User user);

    RequestStatus updateUser(User user);

    RequestStatus deleteUser(User user, User adminUser);
}
