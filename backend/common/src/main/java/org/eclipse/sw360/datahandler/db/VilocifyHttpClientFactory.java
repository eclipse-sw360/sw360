/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;

/**
 * Factory for a shared, pooled {@link CloseableHttpClient} instance used by
 * {@link VilocifyConnector}.
 *
 * <p>The returned client is a thread-safe singleton managed for the lifetime
 * of the application; callers must not close it.
 */
public final class VilocifyHttpClientFactory {

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(20);
    private static final Timeout SOCKET_TIMEOUT = Timeout.ofSeconds(60);
    private static final Timeout CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds(20);
    private static final int MAX_TOTAL_CONNECTIONS = 50;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    public static final String USER_AGENT = "SW360-VilocifyConnector";

    private static final CloseableHttpClient SHARED_CLIENT = buildSharedClient();

    private VilocifyHttpClientFactory() {
        // Utility class
    }

    public static CloseableHttpClient getSharedClient() {
        return SHARED_CLIENT;
    }

    private static CloseableHttpClient buildSharedClient() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(MAX_TOTAL_CONNECTIONS)
                .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .setResponseTimeout(SOCKET_TIMEOUT)
                .build();

        return HttpClients.custom()
                .setUserAgent(USER_AGENT)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
