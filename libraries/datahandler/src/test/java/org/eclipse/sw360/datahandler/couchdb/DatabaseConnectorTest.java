/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.PostDocumentOptions;
import com.ibm.cloud.cloudant.v1.model.PutDatabaseOptions;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.testthrift.TestObject;
import org.junit.After;
import org.junit.Before;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class DatabaseConnectorTest {

    TestObject object;

    String id;
    String rev;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_DATABASE")
    String dbName;

    @Autowired
    @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE")
    DatabaseConnectorCloudant connector;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void setUp() throws Exception {
        // Create the test object
        object = new TestObject();
        object.setName("Test");
        object.setText("This is some nice test text.");
        // Initialize the mapper factory
        DatabaseInstanceCloudant dbInstance = new DatabaseInstanceCloudant(client);

        // Create database if it does not exists
        if (!dbInstance.checkIfDbExists(dbName)) {
            PutDatabaseOptions putDbOptions = new PutDatabaseOptions.Builder().db(dbName).build();
            try {
                client.putDatabase(putDbOptions).execute().getResult();
            } catch (ServiceResponseException e) {
                if (e.getStatusCode() != 412) {
                    throw e;
                }
            }
        }

        // Add the object
        PostDocumentOptions postDocOption = new PostDocumentOptions.Builder()
                .db(dbName)
                .document(connector.getDocumentFromPojo(object))
                .build();

        DocumentResult resp = client.postDocument(postDocOption).execute().getResult();
        // Save id and rev for teardown
        id = resp.getId();
        rev = resp.getRev();
        object.setId(id);
        object.setRevision(rev);
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testSetUp() throws Exception {
        // Check that the document was inserted
        assertTrue(connector.contains(id));
    }


    @Test
    public void testAddDocument() throws Exception {
        // Make new object
        TestObject object1 = new TestObject();
        object1.setName("SecondObject");
        object1.setText("This is also some nice text...");
        // Add it
        assertTrue(connector.add(object1));
        // Check that it is there then delete it;
        assertTrue(connector.contains(object1.getId()));
        assertTrue(connector.deleteById(object1.getId()));
    }

    @Test
    public void testAddDocument2() throws Exception {
        // Cannot add the same document twice:
        assertFalse(connector.add(object));
    }

    @Test
    public void testGetDocument() throws Exception {
        TestObject object1 = connector.get(TestObject.class, id);
        assertEquals(object, object1);
    }

    @Test
    public void testUpdateDocument() throws Exception {
        // Change something in the object
        object.setText("Some new text");
        // Update the document
        connector.update(object);
        // Check that the object's revision was updated
        assertNotEquals(rev, object.getRevision());
        // Fetch it again to check it was updated in the database
        TestObject object1 = connector.get(TestObject.class, id);
        assertEquals("Test", object1.getName());
        assertEquals("Some new text", object1.getText());
        // Check that both revision match
        assertEquals(object.getRevision(), object1.getRevision());
        // Check that revision has changed
        assertNotEquals(rev, object1.getRevision());
        rev = object1.getRevision();
    }

    @Test
    public void testContainsDocument() throws Exception {
        assertTrue(connector.contains(id));
    }

    @Test
    public void testDeleteDocumentById() throws Exception {
        // Document can only be deleted once
        assertTrue(connector.deleteById(id));
        assertFalse(connector.deleteById(id));
    }
}
