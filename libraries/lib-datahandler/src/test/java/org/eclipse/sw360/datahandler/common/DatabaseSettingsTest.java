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

import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;

import java.net.MalformedURLException;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Constants for the database address
 */
public class DatabaseSettingsTest {

    public static final String PROPERTIES_FILE_PATH = "/couchdb-test.properties";

    public static final String COUCH_DB_URL;
    public static final String COUCH_DB_LUCENE_URL;
    public static final String COUCH_DB_DATABASE;
    public static final String COUCH_DB_ATTACHMENTS;
    public static final String COUCH_DB_CONFIG;
    public static final String COUCH_DB_USERS;
    public static final String COUCH_DB_VM;

    private static final String COUCH_DB_USERNAME;
    private static final String COUCH_DB_PASSWORD;

    static {
        Properties props = CommonUtils.loadProperties(DatabaseSettingsTest.class, PROPERTIES_FILE_PATH);

        COUCH_DB_URL = props.getProperty("couchdb.url", "http://localhost:5984");
        COUCH_DB_LUCENE_URL = props.getProperty("couchdb.lucene.url", "http://localhost:8080/couchdb-lucene");
        COUCH_DB_DATABASE = props.getProperty("couchdb.database", "sw360_test_db");
        COUCH_DB_USERNAME = props.getProperty("couchdb.user", "");
        COUCH_DB_PASSWORD = props.getProperty("couchdb.password", "");
        COUCH_DB_ATTACHMENTS = props.getProperty("couchdb.attachments", "sw360_test_attachments");
        COUCH_DB_CONFIG = props.getProperty("couchdb.config", "sw360_test_config");
        COUCH_DB_USERS = props.getProperty("couchdb.usersdb", "sw360_test_users");
        COUCH_DB_VM = props.getProperty("couchdb.vulnerability_management", "sw360_test_vm");
    }

    public static Supplier<HttpClient> getConfiguredHttpClient() throws MalformedURLException {
        StdHttpClient.Builder httpClientBuilder = new StdHttpClient.Builder().url(COUCH_DB_URL);
        if(! "".equals(COUCH_DB_USERNAME)) {
            httpClientBuilder.username(COUCH_DB_USERNAME);
        }
        if (! "".equals(COUCH_DB_PASSWORD)) {
            httpClientBuilder.password(COUCH_DB_PASSWORD);
        }
        return httpClientBuilder::build;
    }


    private DatabaseSettingsTest() {
        // Utility class with only static functions
    }

}
