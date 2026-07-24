/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.moderation;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.web.client.RestClient;

/**
 * Entry point for obtaining a {@link ModerationClient}.
 * 
 * Much of SW360 (for example {@code Moderator} and the entitlement moderators) is not
 * Spring-managed, so there is no injected bean. Call {@link #get()} to reuse one shared
 * HTTP client aimed at {@link ThriftClients#BACKEND_URL} (same pattern as
 * {@code UsersClients} for the users service).
 * 
 * This class lives in datahandler next to {@link ThriftClients} so those callers can use
 * moderation without depending on a separate client module.
 */
public final class ModerationClients {

    private static volatile ModerationClient defaultClient;

    private ModerationClients() {}

    /**
     * Returns the shared moderation client, creating it on first use.
     */
    public static ModerationClient get() {
        return defaultClient();
    }

    /**
     * Replaces the shared client, or clears it when {@code client} is {@code null}.
     * Intended for unit tests so production code is not required to reach a live server.
     */
    public static void set(ModerationClient client) {
        synchronized (ModerationClients.class) {
            defaultClient = client;
        }
    }

    /**
     * Same as {@link #get()}: lazy singleton backed by {@link ModerationServiceRestClient}.
     */
    public static ModerationClient defaultClient() {
        if (defaultClient == null) {
            synchronized (ModerationClients.class) {
                if (defaultClient == null) {
                    RestClient restClient = RestClient.builder().baseUrl(ThriftClients.BACKEND_URL).build();
                    defaultClient = new ModerationServiceRestClient(restClient);
                }
            }
        }
        return defaultClient;
    }
}

