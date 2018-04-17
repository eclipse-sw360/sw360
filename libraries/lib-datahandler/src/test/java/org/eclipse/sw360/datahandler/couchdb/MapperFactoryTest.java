/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.eclipse.sw360.testthrift.TestObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author cedric.bodet@tngtech.com
 */
public class MapperFactoryTest {

    private static final String TEST_ID = "abcdef";
    private static final String TEST_REV = "123456";
    private static final String TEST_NAME = "Super License 3.2";

    ObjectMapper mapper;
    TestObject object;

    @Before
    public void setUp() throws Exception {
        // Prepare mapper
        MapperFactory factory = new MapperFactory(ImmutableList.<Class<?>>of(TestObject.class), Collections.<Class<?>>emptyList(),
                Maps.newHashMap());
        mapper = factory.createObjectMapper();
        // Prepare object
        object = new TestObject();
        object.setId(TEST_ID);
        object.setRevision(TEST_REV);
        object.setName(TEST_NAME);
    }

    @Test
    public void testLicenseSerialization() {
        // Serialize the object (as node)
        JsonNode node = mapper.valueToTree(object);
        // Check that fields are present
        assertTrue("_id present", node.has("_id"));
        assertTrue("_rev present", node.has("_rev"));
        assertTrue("name present", node.has("name"));
        // Text was not set
        assertFalse("Text not set", node.has("text"));
    }

    @Test
    public void testLicenseContent() {
        // Serialize the object (as node)
        JsonNode node = mapper.valueToTree(object);
        // Check field values
        assertEquals(TEST_ID, node.get("_id").textValue());
        assertEquals(TEST_REV, node.get("_rev").textValue());
        assertEquals(TEST_NAME, node.get("name").textValue());
    }

    @Test
    public void testLicenseDeserialization() throws Exception {
        // Serialize the object (as string)
        String string = mapper.writeValueAsString(object);
        // Deserialize the object
        TestObject parsedObject = mapper.readValue(string, TestObject.class);

        // Check field values
        assertEquals(TEST_ID, parsedObject.getId());
        assertEquals(TEST_REV, parsedObject.getRevision());
        assertEquals(TEST_NAME, parsedObject.getName());
        assertNull("test not present", parsedObject.getText());
    }

    @Test
    public void testNullValues() throws Exception {
        // Null _id and _rev should not be serialized, as they are not accepted by CouchDB
        object.unsetId();
        object.unsetRevision();
        // Serialize the object (as node)
        JsonNode node = mapper.valueToTree(object);
        // Check that null-fields are not-present
        assertFalse("_id present", node.has("_id"));
        assertFalse("_rev present", node.has("_rev"));
        // Name should still be present
        assertTrue("name present", node.has("name"));
    }
}
