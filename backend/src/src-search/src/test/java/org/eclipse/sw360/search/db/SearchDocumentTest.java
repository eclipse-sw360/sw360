/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search.db;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SearchDocumentTest {

    private Map<String, Object> document;
    private SearchDocument parser;


    @Before
    public void setUp() throws Exception {
        document = new HashMap<>();

        document.put("type", "license");
        document.put("fullname", "testfullname");
        document.put("testkey", "testvalue");

        parser = new SearchDocument(document);

    }

    @Test
    public void testGetType() throws Exception {
        assertEquals("license", parser.getType());
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals("testfullname", parser.getName());
    }

    @Test
    public void testGetProperty() throws Exception {
        assertEquals("testvalue", parser.getProperty("testkey"));
    }

    @Test
    public void testGetTypeInvalid() throws Exception {
        document.remove("type");
        parser = new SearchDocument(document);

        assertNotNull(parser.getType());
        assertEquals("", parser.getType());
    }

    @Test
    public void testGetNameInvalid1() throws Exception {
        document.remove("fullname");
        parser = new SearchDocument(document);

        assertNotNull(parser.getName());
        assertEquals("", parser.getName());
    }

    @Test
    public void testGetNameInvalid2() throws Exception {
        document.put("type", "feuwife");
        parser = new SearchDocument(document);

        assertNotNull(parser.getName());
        assertEquals("", parser.getName());
    }
}
