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
import org.eclipse.sw360.clients.utils.FutureUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter implementation for the SW360 users endpoint.
 */
class SW360UserClientAdapterAsyncImpl implements SW360UserClientAdapterAsync {
    private final SW360UserClient userClient;

    public SW360UserClientAdapterAsyncImpl(SW360UserClient client) {
        userClient = client;
    }

    @Override
    public SW360UserClient getUserClient() {
        return userClient;
    }

    @Override
    public CompletableFuture<List<SW360User>> getUsers() {
        return getUserClient().getUsers();
    }

    @Override
    public CompletableFuture<SW360User> getUserProfile() {
        return getUserClient().getUserProfile();
    }

    @Override
    public CompletableFuture<Optional<SW360User>> getUserByEmail(String email) {
        return FutureUtils.optionalFuture(getUserClient().getUserByEmail(email));
    }

    @Override
    public CompletableFuture<String> createToken(String name, Set<String> authorities, String expirationDate) {
        return getUserClient().createToken(name, authorities, expirationDate);
    }

    @Override
    public CompletableFuture<Integer> revokeToken(String name) {
        return getUserClient().revokeToken(name);
    }
}
