/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.testutil;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.util.Set;

/**
 * Created by GerritGrenzebachTNG on 09.02.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class DatabaseSetupTest {

    @Autowired
    @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE")
    DatabaseConnectorCloudant db;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testConnection() throws SW360Exception {

        Project project = new Project().setName("Test Project");
        project.addToModerators("user1");

        db.add(project);

        ModerationRequest moderationRequest = new ModerationRequest();
        moderationRequest.setDocumentId(project.id).setDocumentType(DocumentType.PROJECT);
        moderationRequest.setRequestingUser("cedric.bodet@tngtech.com");
        moderationRequest.addToModerators("cedric.bodet@tngtech.com");

        db.add(moderationRequest);

    }

}
