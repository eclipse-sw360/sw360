/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.rest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Shared Spring {@link RestClient} for all backend {@code *Clients} factories.
 *
 * ({@code backend.rest.pool.*}, connect/read timeouts, optional proxy) so static
 * factories get the same connection management as Spring injection did.
 */
public final class BackendRestClients {

    private static final Logger log = LogManager.getLogger(BackendRestClients.class);
    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    private static final String BACKEND_URL;
    private static final String BACKEND_PROXY_URL;
    private static final int CONNECTION_TIMEOUT_MS;
    private static final int READ_TIMEOUT_MS;
    private static final int POOL_MAX_TOTAL;
    private static final int POOL_MAX_PER_ROUTE;
    private static final int IDLE_EVICT_SECONDS;
    private static final int CONNECTION_TTL_SECONDS;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final CloseableHttpClient HTTP_CLIENT;
    private static volatile RestClient sharedRestClient;

    static {
        Properties props = CommonUtils.loadProperties(BackendRestClients.class, PROPERTIES_FILE_PATH);
        BACKEND_URL = props.getProperty("backend.url", ThriftClients.BACKEND_URL);
        BACKEND_PROXY_URL = props.getProperty("backend.proxy.url", null);
        CONNECTION_TIMEOUT_MS = Integer.parseInt(props.getProperty("backend.timeout.connection", "5000"));
        READ_TIMEOUT_MS = Integer.parseInt(props.getProperty("backend.timeout.read", "600000"));
        POOL_MAX_TOTAL = Integer.parseInt(props.getProperty("backend.rest.pool.max-total", "150"));
        POOL_MAX_PER_ROUTE = Integer.parseInt(props.getProperty("backend.rest.pool.max-per-route", "50"));
        IDLE_EVICT_SECONDS = Integer.parseInt(props.getProperty("backend.rest.idle.evict.seconds",
                props.getProperty("backend.thrift.idle.evict.seconds", "15")));
        CONNECTION_TTL_SECONDS = Integer.parseInt(props.getProperty("backend.rest.connection.ttl.seconds",
                props.getProperty("backend.thrift.connection.ttl.seconds", "60")));

        CONNECTION_MANAGER = createConnectionManager();
        HTTP_CLIENT = createHttpClient();

        log.info("""
                Backend RestClient pool configuration:
                \tURL                      : {}
                \tProxy                    : {}
                \tTimeout Connecting (ms)  : {}
                \tTimeout Read (ms)        : {}
                \tPool max total           : {}
                \tPool max per route       : {}
                """,
                BACKEND_URL, BACKEND_PROXY_URL, CONNECTION_TIMEOUT_MS, READ_TIMEOUT_MS,
                POOL_MAX_TOTAL, POOL_MAX_PER_ROUTE);
    }

    private BackendRestClients() {}

    /**
     * JVM-wide pooled {@link RestClient} aimed at {@code backend.url}.
     * Prefer this for all {@code *Clients} factories.
     */
    public static RestClient shared() {
        if (sharedRestClient == null) {
            synchronized (BackendRestClients.class) {
                if (sharedRestClient == null) {
                    sharedRestClient = RestClient.builder()
                            .baseUrl(BACKEND_URL)
                            .requestFactory(new HttpComponentsClientHttpRequestFactory(HTTP_CLIENT))
                            .build();
                }
            }
        }
        return sharedRestClient;
    }

    /**
     * Replaces the shared RestClient (tests). Pass {@code null} to rebuild on next {@link #shared()}.
     */
    public static void setShared(RestClient restClient) {
        synchronized (BackendRestClients.class) {
            sharedRestClient = restClient;
        }
    }

    public static Map<String, Integer> getConnectionPoolStats() {
        PoolStats stats = CONNECTION_MANAGER.getTotalStats();
        Map<String, Integer> poolStats = new LinkedHashMap<>();
        poolStats.put("leased", stats.getLeased());
        poolStats.put("pending", stats.getPending());
        poolStats.put("available", stats.getAvailable());
        poolStats.put("max", stats.getMax());
        return poolStats;
    }

    public static void closeSharedClient() {
        try {
            HTTP_CLIENT.close();
            log.info("Shared backend RestClient HTTP pool closed.");
        } catch (Exception e) {
            log.warn("Error closing shared backend RestClient HTTP pool", e);
        }
    }

    private static PoolingHttpClientConnectionManager createConnectionManager() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT_MS))
                .setValidateAfterInactivity(Timeout.ofSeconds(IDLE_EVICT_SECONDS))
                .setTimeToLive(TimeValue.ofSeconds(CONNECTION_TTL_SECONDS))
                .build();
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(POOL_MAX_TOTAL)
                .setMaxConnPerRoute(POOL_MAX_PER_ROUTE)
                .setDefaultConnectionConfig(connectionConfig)
                .build();
    }

    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT_MS))
                .build();

        var builder = HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER)
                .setDefaultRequestConfig(requestConfig);

        if (BACKEND_PROXY_URL != null) {
            try {
                URL proxyUrl = new URI(BACKEND_PROXY_URL).toURL();
                HttpHost proxy = new HttpHost(proxyUrl.getProtocol(), proxyUrl.getHost(), proxyUrl.getPort());
                builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
            } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
                log.error("cannot configure http proxy for backend RestClient", e);
            }
        }
        return builder.build();
    }
}
