/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.http;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Test class for {@code RequestBodyBuilderImpl}. This class tests some special
 * corner cases; the main logic is tested by integration tests.
 */
public class RequestBodyBuilderImplTest {
    /**
     * Mock for the JSON mapper.
     */
    private ObjectMapper mapper;

    /**
     * The builder to be tested.
     */
    private RequestBodyBuilderImpl bodyBuilder;

    @Before
    public void setUp() {
        mapper = JsonMapper.builderWithJackson2Defaults().build();
        bodyBuilder = new RequestBodyBuilderImpl(mapper);
    }

    @Test(expected = IllegalStateException.class)
    public void testStringBodyIfBodyIsAlreadyDefined() {
        bodyBuilder.string("body1", HttpConstants.CONTENT_TEXT_PLAIN);

        bodyBuilder.string("body2", HttpConstants.CONTENT_TEXT_PLAIN);
    }

    @Test(expected = IllegalStateException.class)
    public void testFileBodyIfBodyIsAlreadyDefined() {
        bodyBuilder.string("body1", HttpConstants.CONTENT_TEXT_PLAIN);

        bodyBuilder.file(Paths.get("test.doc"), HttpConstants.CONTENT_OCTET_STREAM);
    }

    @Test(expected = IllegalStateException.class)
    public void testJsonBodyIfBodyIsAlreadyDefined() throws JacksonException {
        bodyBuilder.string("body1", HttpConstants.CONTENT_TEXT_PLAIN);

        bodyBuilder.json(new HashMap<String, Object>());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetBodyIfUndefined() {
        bodyBuilder.getBody();
    }

    @Test
    public void testHandlingOfJacksonException() throws JacksonException {
        Object data = new Object();
        try {
            bodyBuilder.json(data);
            fail("No exception thrown!");
        } catch (IllegalStateException iex) {
            assertThat(iex.getCause()).isInstanceOf(JacksonException.class);
        }
    }

    @Test
    public void testBodyFileNoFileName() {
        Path folderPath = Paths.get("/");

        bodyBuilder.file(folderPath, "text/plain");
        assertThat(bodyBuilder.getFileName()).isNull();
    }
}
