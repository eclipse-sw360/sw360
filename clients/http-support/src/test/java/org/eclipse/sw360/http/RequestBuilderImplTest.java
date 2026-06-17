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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Unit test class for {@code RequestBuilderImpl}. This class tests some
 * special corner cases which are not covered by the integration test class.
 */
public class RequestBuilderImplTest {
    /**
     * The builder to be tested.
     */
    private RequestBuilderImpl requestBuilder;

    @Before
    public void setUp() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        requestBuilder = new RequestBuilderImpl(mapper);
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipartAndNormalBody() {
        requestBuilder.multiPart("part",
                body -> body.string("test", HttpConstants.CONTENT_TEXT_PLAIN));

        requestBuilder.body(body -> body.string("other test", HttpConstants.CONTENT_TEXT_PLAIN));
    }

    @Test(expected = IllegalStateException.class)
    public void testNormalBodyAndMultipart() {
        requestBuilder.body(body -> body.string("other test", HttpConstants.CONTENT_TEXT_PLAIN));

        requestBuilder.multiPart("part",
                body -> body.string("test", HttpConstants.CONTENT_TEXT_PLAIN));
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleBodies() {
        requestBuilder.body(body -> body.string("body1", HttpConstants.CONTENT_TEXT_PLAIN));

        requestBuilder.body(body -> body.string("body12", HttpConstants.CONTENT_TEXT_PLAIN));
    }
}
