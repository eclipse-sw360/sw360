/*
 * Copyright Siemens AG, 2014-2019,2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.ibm.cloud.sdk.core.security.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
import org.eclipse.sw360.datahandler.cloudantclient.AttachmentAwareDatabase;
import org.jetbrains.annotations.NotNull;

/**
 * Constants for the database address
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 */
public class DatabaseSettings {

    private static final Logger log = LogManager.getLogger(DatabaseSettings.class);
    public static final String PROPERTIES_FILE_PATH = "/couchdb.properties";

    public static final String COUCH_DB_URL;
    public static final String COUCH_DB_DATABASE;
    public static final String COUCH_DB_ATTACHMENTS;
    public static final String COUCH_DB_CHANGE_LOGS;
    public static final String COUCH_DB_CONFIG;
    public static final String COUCH_DB_OAUTHCLIENTS;
    public static final String COUCH_DB_USERS;
    public static final String COUCH_DB_VM;
    public static final String COUCH_DB_SPDX;
    public static final boolean COUCH_DB_CACHE;

    public static final int LUCENE_SEARCH_LIMIT;

    public static final boolean CLOUDANT_ENABLE_RETRIES;
    public static final int CLOUDANT_MAX_RETRIES;
    public static final int CLOUDANT_MAX_RETRY_INTERVAL;
    public static final int CLOUDANT_POOL_MAX_IDLE_CONNECTIONS;
    public static final int CLOUDANT_POOL_KEEPALIVE_SECONDS;
    public static final int CLOUDANT_MAX_REQUESTS;
    public static final int CLOUDANT_MAX_REQUESTS_PER_HOST;

    private static final Optional<String> COUCH_DB_USERNAME;
    private static final Optional<String> COUCH_DB_PASSWORD;

    static {
        Properties props = CommonUtils.loadProperties(DatabaseSettings.class, PROPERTIES_FILE_PATH);

        // Try ENV if set first
        COUCH_DB_URL = System.getenv("COUCHDB_URL") != null ? System.getenv("COUCHDB_URL")
                : props.getProperty("couchdb.url", "http://localhost:5984");
        COUCH_DB_USERNAME = Optional
                .ofNullable(System.getenv("COUCHDB_USER") != null ? System.getenv("COUCHDB_USER")
                        : props.getProperty("couchdb.user", ""));
        COUCH_DB_PASSWORD = Optional.ofNullable(
                System.getenv("COUCHDB_PASSWORD") != null ? System.getenv("COUCHDB_PASSWORD")
                        : props.getProperty("couchdb.password", ""));
        COUCH_DB_DATABASE = props.getProperty("couchdb.database", "sw360db");
        COUCH_DB_ATTACHMENTS = props.getProperty("couchdb.attachments", "sw360attachments");
        COUCH_DB_CHANGE_LOGS = props.getProperty("couchdb.change_logs", "sw360changelogs");
        COUCH_DB_CONFIG = props.getProperty("couchdb.config", "sw360config");
        COUCH_DB_USERS = props.getProperty("couchdb.usersdb", "sw360users");
        COUCH_DB_VM = props.getProperty("couchdb.vulnerability_management", "sw360vm");
        COUCH_DB_SPDX = props.getProperty("couchdb.sw360spdx", "sw360spdx");
        COUCH_DB_OAUTHCLIENTS = props.getProperty("couchdb.sw360oauthclients", "sw360oauthclients");
        COUCH_DB_CACHE = Boolean.parseBoolean(props.getProperty("couchdb.cache", "true"));

        LUCENE_SEARCH_LIMIT = Integer.parseInt(props.getProperty("lucenesearch.limit", "25"));

        CLOUDANT_ENABLE_RETRIES = Boolean.parseBoolean(props.getProperty("cloudant.enable.retries", "true"));
        CLOUDANT_MAX_RETRIES = Integer.parseInt(props.getProperty("cloudant.max.retries", "2"));
        CLOUDANT_MAX_RETRY_INTERVAL = Integer.parseInt(props.getProperty("cloudant.max.retry.interval", "5"));

        // Optional Cloudant OkHttp tuning. Disabled by default unless explicitly set (> 0).
        CLOUDANT_POOL_MAX_IDLE_CONNECTIONS = Integer.parseInt(props.getProperty(
                "cloudant.pool.max.idle.connections", "-1"));
        CLOUDANT_POOL_KEEPALIVE_SECONDS = Integer.parseInt(props.getProperty(
                "cloudant.pool.keepalive.seconds", "-1"));
        CLOUDANT_MAX_REQUESTS = Integer.parseInt(props.getProperty("cloudant.max.requests", "-1"));
        CLOUDANT_MAX_REQUESTS_PER_HOST = Integer.parseInt(props.getProperty("cloudant.max.requests.per.host", "-1"));
    }

    private static final Cloudant CLIENT = createConfiguredClient();
    /**
     * Singleton attachment-aware client. Constructed once at static init while
     * no other threads can access {@link #CLIENT}, so the unavoidable
     * authenticator-mutation side effect of {@code new Cloudant(...)}
     * (which also invokes {@code authenticator.setSessionUrl(...)} and
     * {@code authenticator.invalidateToken()} on the SHARED authenticator)
     * cannot race with any in-flight token refresh. Never re-create at runtime.
     */
    private static final AttachmentAwareDatabase ATTACHMENT_CLIENT =
            new AttachmentAwareDatabase(CLIENT);

    public static @NotNull Cloudant getConfiguredClient() {
        return CLIENT;
    }

    public static @NotNull AttachmentAwareDatabase getAttachmentClient() {
        return ATTACHMENT_CLIENT;
    }

    private static @NotNull Cloudant createConfiguredClient() {
        Cloudant client;
        if (hasValue(COUCH_DB_USERNAME) && hasValue(COUCH_DB_PASSWORD)) {
            Authenticator authenticator = CouchDbSessionAuthenticator
                    .newAuthenticator(COUCH_DB_USERNAME.get(), COUCH_DB_PASSWORD.get());
            client = new Cloudant("sw360-couchdb", authenticator);
        } else {
            client = Cloudant.newInstance("sw360-couchdb");
        }
        // Set the service URL BEFORE enabling retries. enableRetries() reconfigures
        // the underlying OkHttp client and, with CouchDbSessionAuthenticator, leaves
        // the authenticator's /_session call wired to the SDK default host.
        // Setting the URL first ensures the authenticator inherits the correct
        // host before the retry layer wraps it.
        try {
            client.setServiceUrl(COUCH_DB_URL);
        } catch (IllegalArgumentException e) {
            log.error("Error creating client: {}", e.getMessage(), e);
        }
        if (CLOUDANT_ENABLE_RETRIES) {
            client.enableRetries(CLOUDANT_MAX_RETRIES, CLOUDANT_MAX_RETRY_INTERVAL);
        }
        tuneCloudantHttpClient(client);
        return client;
    }

    private static void tuneCloudantHttpClient(@NotNull Cloudant client) {
        final boolean poolTuningEnabled = CLOUDANT_POOL_MAX_IDLE_CONNECTIONS > 0
                || CLOUDANT_POOL_KEEPALIVE_SECONDS > 0;
        final boolean dispatcherTuningEnabled = CLOUDANT_MAX_REQUESTS > 0
                || CLOUDANT_MAX_REQUESTS_PER_HOST > 0;

        if (!poolTuningEnabled && !dispatcherTuningEnabled) {
            return;
        }

        OkHttpClient currentClient = client.getClient();
        OkHttpClient.Builder tunedBuilder = currentClient.newBuilder();

        if (poolTuningEnabled) {
            // OkHttp defaults: 5 idle connections, 5 minutes keepalive.
            int maxIdleConnections = CLOUDANT_POOL_MAX_IDLE_CONNECTIONS > 0
                    ? CLOUDANT_POOL_MAX_IDLE_CONNECTIONS
                    : 5;
            long keepAliveSeconds = CLOUDANT_POOL_KEEPALIVE_SECONDS > 0
                    ? CLOUDANT_POOL_KEEPALIVE_SECONDS
                    : 300;

            tunedBuilder.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveSeconds, TimeUnit.SECONDS));
        }

        if (dispatcherTuningEnabled) {
            Dispatcher tunedDispatcher = new Dispatcher(currentClient.dispatcher().executorService());
            tunedDispatcher.setMaxRequests(CLOUDANT_MAX_REQUESTS > 0
                    ? CLOUDANT_MAX_REQUESTS
                    : currentClient.dispatcher().getMaxRequests());
            tunedDispatcher.setMaxRequestsPerHost(CLOUDANT_MAX_REQUESTS_PER_HOST > 0
                    ? CLOUDANT_MAX_REQUESTS_PER_HOST
                    : currentClient.dispatcher().getMaxRequestsPerHost());
            tunedBuilder.dispatcher(tunedDispatcher);
        }

        client.setClient(tunedBuilder.build());
        log.info("Applied Cloudant HTTP tuning: poolIdleMax={}, poolKeepAliveSeconds={}, maxRequests={}, maxRequestsPerHost={}",
                CLOUDANT_POOL_MAX_IDLE_CONNECTIONS, CLOUDANT_POOL_KEEPALIVE_SECONDS,
                CLOUDANT_MAX_REQUESTS, CLOUDANT_MAX_REQUESTS_PER_HOST);
    }

    private static boolean hasValue(@NotNull Optional<String> value) {
        return value.isPresent() && !value.get().isBlank();
    }

    private DatabaseSettings() {
        // Utility class with only static functions
    }
}
