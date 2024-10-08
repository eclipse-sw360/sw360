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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.google.gson.GsonBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

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

    public static Supplier<HttpClient> getConfiguredHttpClient() throws MalformedURLException {
        StdHttpClient.Builder httpClientBuilder = new StdHttpClient.Builder().url(COUCH_DB_URL);
        if (!"".equals(COUCH_DB_USERNAME.get())) {
            httpClientBuilder.username(COUCH_DB_USERNAME.get());
        }
        if (!"".equals(COUCH_DB_PASSWORD.get())) {
            httpClientBuilder.password(COUCH_DB_PASSWORD.get());
        }
        return httpClientBuilder::build;
    }

    public static Supplier<CloudantClient> getConfiguredClient() {
        ClientBuilder clientBuilder = null;
        GsonBuilder gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
        for (Class<?> c : ThriftUtils.THRIFT_CLASSES) {
            gson.registerTypeAdapter(c, new CustomThriftDeserializer());
            gson.registerTypeAdapter(c, new CustomThriftSerializer());
        }
        for (Class<?> c : ThriftUtils.THRIFT_NESTED_CLASSES) {
            gson.registerTypeAdapter(c, new CustomThriftSerializer());
        }
        try {
            clientBuilder = ClientBuilder.url(new URL(COUCH_DB_URL)).gsonBuilder(gson);
            if (!"".equals(COUCH_DB_USERNAME.get())) {
                clientBuilder.username(COUCH_DB_USERNAME.get());
            }
            if (!"".equals(COUCH_DB_PASSWORD.get())) {
                clientBuilder.password(COUCH_DB_PASSWORD.get());
            }
        } catch (MalformedURLException e) {
            log.error("Error creating client", e);
        }
        return clientBuilder::build;
    }


    private DatabaseSettingsTest() {
        // Utility class with only static functions
    }

}
