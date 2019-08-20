/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.eclipse.sw360.testthrift.TestObject;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.eclipse.sw360.datahandler.couchdb.DatabaseTestProperties.COUCH_DB_DATABASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class DatabaseConnectorTest {

    DatabaseConnector connector;
    MapperFactory factory;

    TestObject object;

    String id;
    String rev;

    @Before
    public void setUp() throws Exception {
        // Create the test object
        object = new TestObject();
        object.setName("Test");
        object.setText("This is some nice test text.");
        // Initialize the mapper factory
        factory = new MapperFactory(ImmutableList.<Class<?>>of(TestObject.class), Collections.<Class<?>>emptyList(), Maps.newHashMap());
        // Default connector for testing
        HttpClient httpClient = DatabaseTestProperties.getConfiguredHttpClient();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

        // Create database if it does not exists
        if (!dbInstance.checkIfDbExists(COUCH_DB_DATABASE)) {
            dbInstance.createDatabase(COUCH_DB_DATABASE);
        }

        CouchDbConnector db = new StdCouchDbConnector(COUCH_DB_DATABASE, dbInstance, factory);
        // Add the object
        db.create(object);
        // Save id and rev for teardown
        id = object.getId();
        rev = object.getRevision();
        // Now create the actual database connector
        connector = new DatabaseConnector(DatabaseTestProperties.getConfiguredHttpClient(), COUCH_DB_DATABASE, factory);
    }

    @After
    public void tearDown() throws Exception {
        // Default connector for testing
        HttpClient httpClient = DatabaseTestProperties.getConfiguredHttpClient();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        if (dbInstance.checkIfDbExists(COUCH_DB_DATABASE)) {
            dbInstance.deleteDatabase(COUCH_DB_DATABASE);
        }
    }

    @Test
    public void testSetUp() throws Exception {
        // Default connector for testing
        HttpClient httpClient = DatabaseTestProperties.getConfiguredHttpClient();
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
        CouchDbConnector db = new StdCouchDbConnector(COUCH_DB_DATABASE, dbInstance, factory);
        // Check that the document was inserted
        assertTrue(db.contains(id));
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
        // Checkt that the object's revision was updated
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
