/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.adapter;

import org.eclipse.sw360.clients.rest.SW360UserClient;
import org.eclipse.sw360.clients.rest.resource.users.SW360User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Service interface for an adapter supporting asynchronous operations on SW360
 * user entities.
 * </p>
 */
public interface SW360UserClientAdapterAsync {
    SW360UserClient getUserClient();

    CompletableFuture<List<SW360User>> getUsers();

    CompletableFuture<SW360User> getUserProfile();

    CompletableFuture<Optional<SW360User>> getUserByEmail(String email);

    CompletableFuture<String> createToken(String name, Set<String> authorities, String expirationDate);

    CompletableFuture<Integer> revokeToken(String name);
}
