/*
 * Copyright Siemens AG, 2014-2019,2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import java.util.Optional;
import java.util.Properties;

import com.ibm.cloud.sdk.core.security.Authenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
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
    public static final String COUCH_DB_USERS;
    public static final String COUCH_DB_VM;
    public static final String COUCH_DB_SPDX;
    public static final boolean COUCH_DB_CACHE;

    public static final int LUCENE_SEARCH_LIMIT;
    public static final boolean LUCENE_LEADING_WILDCARD;

    private static final Optional<String> COUCH_DB_USERNAME;
    private static final Optional<String> COUCH_DB_PASSWORD;

    static {
        Properties props = CommonUtils.loadProperties(DatabaseSettings.class, PROPERTIES_FILE_PATH);

        // Try ENV if set first
        COUCH_DB_URL = System.getenv("COUCHDB_URL") != null ? System.getenv("COUCHDB_URL")
                : props.getProperty("couchdb.url", "http://localhost:5984");
        COUCH_DB_USERNAME = Optional.ofNullable(System.getenv("COUCHDB_USER") != null ? System.getenv("COUCHDB_USER")
                : props.getProperty("couchdb.user", ""));
        COUCH_DB_PASSWORD = Optional
                .ofNullable(System.getenv("COUCHDB_PASSWORD") != null ? System.getenv("COUCHDB_PASSWORD")
                        : props.getProperty("couchdb.password", ""));
        COUCH_DB_DATABASE = props.getProperty("couchdb.database", "sw360db");
        COUCH_DB_ATTACHMENTS = props.getProperty("couchdb.attachments", "sw360attachments");
        COUCH_DB_CHANGE_LOGS = props.getProperty("couchdb.change_logs", "sw360changelogs");
        COUCH_DB_CONFIG = props.getProperty("couchdb.config", "sw360config");
        COUCH_DB_USERS = props.getProperty("couchdb.usersdb", "sw360users");
        COUCH_DB_VM = props.getProperty("couchdb.vulnerability_management", "sw360vm");
        COUCH_DB_SPDX = props.getProperty("couchdb.sw360spdx", "sw360spdx");
        COUCH_DB_CACHE = Boolean.parseBoolean(props.getProperty("couchdb.cache", "true"));

        LUCENE_SEARCH_LIMIT = Integer.parseInt(props.getProperty("lucenesearch.limit", "25"));
        LUCENE_LEADING_WILDCARD = Boolean.parseBoolean(props.getProperty("lucenesearch.leading.wildcard", "false"));
    }

    public static @NotNull Cloudant getConfiguredClient() {
        Cloudant client;
        if (COUCH_DB_USERNAME.isPresent() && COUCH_DB_PASSWORD.isPresent()) {
            Authenticator authenticator = CouchDbSessionAuthenticator.newAuthenticator(
                    COUCH_DB_USERNAME.get(),
                    COUCH_DB_PASSWORD.get());
            client = new Cloudant("sw360-couchdb", authenticator);
        } else {
            client = Cloudant.newInstance("sw360-couchdb");
        }
        try {
            client.setServiceUrl(COUCH_DB_URL);
        } catch (IllegalArgumentException e) {
            log.error("Error creating client: {}", e.getMessage(), e);
        }
        return client;
    }

    private DatabaseSettings() {
        // Utility class with only static functions
    }
}
