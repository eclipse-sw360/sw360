/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.sdk.core.security.Authenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Properties;

/**
 * Constants for the database address
 */
public class DatabaseSettingsTest {

    private static final Logger log = LogManager.getLogger(DatabaseSettingsTest.class);
    public static final String PROPERTIES_FILE_PATH;


    public static final String COUCH_DB_URL;
    public static final String COUCH_DB_DATABASE;
    public static final String COUCH_DB_ATTACHMENTS;
    public static final String COUCH_DB_CONFIG;
    public static final String COUCH_DB_USERS;
    public static final String COUCH_DB_VM;
    public static final String COUCH_DB_CHANGELOGS;

    private static final Optional<String> COUCH_DB_USERNAME;
    private static final Optional<String> COUCH_DB_PASSWORD;

    static {
        PROPERTIES_FILE_PATH = System.getenv("PROPERTIES_FILE_PATH") != null
                ? System.getenv("PROPERTIES_FILE_PATH") + "/couchdb-test.properties"
                : "/couchdb-test.properties";

        Properties props =
                CommonUtils.loadProperties(DatabaseSettingsTest.class, PROPERTIES_FILE_PATH);

        // Try ENV if set first
        COUCH_DB_URL = System.getenv("COUCHDB_URL") != null ? System.getenv("COUCHDB_URL")
                : props.getProperty("couchdb.url", "http://localhost:5984");
        COUCH_DB_USERNAME = Optional
                .ofNullable(System.getenv("COUCHDB_USER") != null ? System.getenv("COUCHDB_USER")
                        : props.getProperty("couchdb.user", ""));
        COUCH_DB_PASSWORD = Optional.ofNullable(
                System.getenv("COUCHDB_PASSWORD") != null ? System.getenv("COUCHDB_PASSWORD")
                        : props.getProperty("couchdb.password", ""));

        COUCH_DB_DATABASE = props.getProperty("couchdb.database", "sw360_test_db");
        COUCH_DB_ATTACHMENTS = props.getProperty("couchdb.attachments", "sw360_test_attachments");
        COUCH_DB_CONFIG = props.getProperty("couchdb.config", "sw360_test_config");
        COUCH_DB_USERS = props.getProperty("couchdb.usersdb", "sw360_test_users");
        COUCH_DB_VM = props.getProperty("couchdb.vulnerability_management", "sw360_test_vm");
        COUCH_DB_CHANGELOGS = props.getProperty("couchdb.change_logs", "sw360_test_changelogs");
    }

    public static @NotNull Cloudant getConfiguredClient() {
        Cloudant client;
        if (COUCH_DB_USERNAME.isPresent() && !COUCH_DB_USERNAME.get().isEmpty() &&
            COUCH_DB_PASSWORD.isPresent() && !COUCH_DB_PASSWORD.get().isEmpty()) {
            Authenticator authenticator = CouchDbSessionAuthenticator.newAuthenticator(
                    COUCH_DB_USERNAME.get(),
                    COUCH_DB_PASSWORD.get());
            client = new Cloudant("sw360-couchdb-test", authenticator);
        } else {
            client = Cloudant.newInstance("sw360-couchdb-test");
        }
        try {
            client.setServiceUrl(COUCH_DB_URL);
        } catch (IllegalArgumentException e) {
            log.error("Error creating client: {}", e.getMessage(), e);
        }
        return client;
    }


    private DatabaseSettingsTest() {
        // Utility class with only static functions
    }

    public static String getCouchDbUrl() {
        return COUCH_DB_URL;
    }

}
