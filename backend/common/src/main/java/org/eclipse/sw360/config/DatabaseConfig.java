/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.config;

import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.sdk.core.security.Authenticator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan({"org.eclipse.sw360"})
public class DatabaseConfig {
    private static final Logger log = LogManager.getLogger(DatabaseConfig.class);
    public static final String PROPERTIES_FILE_PATH = "/couchdb.properties";
    public static final String TEST_PROPERTIES_FILE_PATH;
    public static final String TEST_PROFILE_NAME = "test";

    static {
        TEST_PROPERTIES_FILE_PATH = System.getenv("PROPERTIES_FILE_PATH") != null
                ? System.getenv("PROPERTIES_FILE_PATH") + "/couchdb-test.properties"
                : "/couchdb-test.properties";
    }

    // Inject properties using @Value
    @Value("${couchdb.url:http://localhost:5984}")
    private String couchDbUrl;

    @Value("${couchdb.user:}")
    private String couchDbUsername;

    @Value("${couchdb.password:}")
    private String couchDbPassword;

    @Value("${couchdb.database:sw360db}")
    private String couchDbDatabase;

    @Value("${couchdb.attachments:sw360attachments}")
    private String couchDbAttachments;

    @Value("${couchdb.change_logs:sw360changelogs}")
    private String couchDbChangeLogs;

    @Value("${couchdb.config:sw360config}")
    private String couchDbConfig;

    @Value("${couchdb.usersdb:sw360users}")
    private String couchDbUsers;

    @Value("${couchdb.vulnerability_management:sw360vm}")
    private String couchDbVm;

    @Value("${couchdb.sw360spdx:sw360spdx}")
    private String couchDbSpdx;

    @Value("${couchdb.sw360oauthclients:sw360oauthclients}")
    private String couchDbOauthClients;

    @Value("${couchdb.cache:true}")
    private boolean couchDbCache;

    @Value("${lucenesearch.limit:25}")
    private int luceneSearchLimit;

    @Value("${lucenesearch.leading.wildcard:false}")
    private boolean luceneLeadingWildcard;

    /**
     * This bean is required to resolve placeholders in @Value annotations.
     * It's often implicitly registered, but it's good practice to define it explicitly
     * when using @PropertySource in older Spring versions or specific setups.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean // Marks the return value of this method as a Spring bean
    public Cloudant cloudantClient() {
        Cloudant client;
        if (!couchDbUsername.isEmpty() && !couchDbPassword.isEmpty()) {
            Authenticator authenticator = CouchDbSessionAuthenticator
                    .newAuthenticator(couchDbUsername, couchDbPassword);
            client = new Cloudant("sw360-couchdb", authenticator);
        } else {
            client = Cloudant.newInstance("sw360-couchdb");
        }
        try {
            client.setServiceUrl(couchDbUrl);
        } catch (IllegalArgumentException e) {
            log.error("Error creating client: {}", e.getMessage(), e);
        }
        return client;
    }

    @Bean
    public TProtocolFactory thriftProtocolFactory() {
        return new TCompactProtocol.Factory();
    }

    @Bean(name="COUCH_DB_DATABASE")
    public String couchDbDatabaseName() {
        return couchDbDatabase;
    }

    @Bean(name="COUCH_DB_ATTACHMENTS")
    public String couchDbAttachmentsName() {
        return couchDbAttachments;
    }

    @Bean
    public String couchDbChangeLogs() {
        return couchDbChangeLogs;
    }

    @Bean
    public String couchDbConfig() {
        return couchDbConfig;
    }

    @Bean
    public String couchDbUsers() {
        return couchDbUsers;
    }

    @Bean
    public String couchDbVm() {
        return couchDbVm;
    }

    @Bean
    public String couchDbSpdx() {
        return couchDbSpdx;
    }

    @Bean
    public String couchDbOauthClients() {
        return couchDbOauthClients;
    }

    public boolean isCouchDbCache() {
        return couchDbCache;
    }

    public int getLuceneSearchLimit() {
        return luceneSearchLimit;
    }

    public boolean isLuceneLeadingWildcard() {
        return luceneLeadingWildcard;
    }
}
