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
package org.eclipse.sw360.antenna.sw360.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiStatusResponseTest {
    /**
     * Name of a test file that contains only successful responses.
     */
    private static final String FILE_SUCCESS_RESPONSE = "multi_status_success";

    /**
     * Name of a test file that contains responses with failures.
     */
    private static final String FILE_FAILURE_RESPONSE = "multi_status_failed";

    /**
     * Reads a multi-status response from a JSON test file from the classpath.
     *
     * @param file the name of the file to be loaded
     * @return the response object
     */
    private static MultiStatusResponse fromJson(String file) throws IOException {
        try (InputStream stream = MultiStatusResponseTest.class.getResourceAsStream("/__files/" +
                file + ".json")) {
            return MultiStatusResponse.fromJson(new ObjectMapper(), stream);
        }
    }

    @Test
    public void testSize() {
        Map<String, Integer> responses = new HashMap<>();
        responses.put("r1", 200);
        responses.put("r2", 300);
        responses.put("r3", 400);

        MultiStatusResponse response = new MultiStatusResponse(responses);
        assertThat(response.responseCount()).isEqualTo(3);
    }

    @Test
    public void testDefensiveCopyOnCreation() {
        Map<String, Integer> responses = new HashMap<>();
        responses.put("r1", 1);
        MultiStatusResponse multiResponse = new MultiStatusResponse(responses);

        responses.put("r2", 2);
        assertThat(multiResponse.responseCount()).isEqualTo(1);
    }

    @Test
    public void testIsAllSuccessEmpty() {
        MultiStatusResponse multiResponse = new MultiStatusResponse(Collections.emptyMap());

        assertThat(multiResponse.isAllSuccess()).isTrue();
    }

    @Test
    public void testIsAllSuccessWithFailures() throws IOException {
        MultiStatusResponse response = fromJson(FILE_FAILURE_RESPONSE);

        assertThat(response.isAllSuccess()).isFalse();
    }

    @Test
    public void testIsAllSuccessTrue() throws IOException {
        MultiStatusResponse response = fromJson(FILE_SUCCESS_RESPONSE);

        assertThat(response.isAllSuccess()).isTrue();
    }

    @Test
    public void testGetResponses() throws IOException {
        MultiStatusResponse response = fromJson(FILE_FAILURE_RESPONSE);

        Map<String, Integer> responses = response.getResponses();
        assertThat(responses).hasSize(3);
        assertThat(responses.get("res-err-1")).isEqualTo(400);
        assertThat(responses.get("res-success")).isEqualTo(200);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetResponsesUnmodifiable() throws IOException {
        MultiStatusResponse response = fromJson(FILE_SUCCESS_RESPONSE);

        response.getResponses().put("more", 418);
    }

    @Test
    public void testGetStatus() throws IOException {
        MultiStatusResponse response = fromJson(FILE_FAILURE_RESPONSE);

        assertThat(response.getStatus("res-success")).isEqualTo(200);
        assertThat(response.getStatus("res-err-2")).isEqualTo(500);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetStatusUnknownResourceId() {
        MultiStatusResponse response = new MultiStatusResponse(Collections.singletonMap("foo", 42));

        response.getStatus("nonExistingResource");
    }

    @Test
    public void testHasResourceId() {
        MultiStatusResponse response = new MultiStatusResponse(Collections.singletonMap("foo", 42));

        assertThat(response.hasResourceId("foo")).isTrue();
        assertThat(response.hasResourceId("bar")).isFalse();
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(MultiStatusResponse.class)
                .withNonnullFields("responses")
                .verify();
    }

    @Test
    public void testToString() throws IOException {
        MultiStatusResponse response = fromJson(FILE_SUCCESS_RESPONSE);
        String s = response.toString();

        assertThat(s).contains("res-1", "res-2");
    }

    @Test(expected = IOException.class)
    public void testFromJsonInvalidStatusCodes() throws IOException {
        String json = "[{\"resourceId\": \"res-1234\"," +
                "\"status\": \"invalidStatus\"}]";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MultiStatusResponse.fromJson(new ObjectMapper(), stream);
    }

    @Test(expected = IOException.class)
    public void testFromJsonMissingResourceId() throws IOException {
        String json = "[{\"noResourceId\": \"res-undefined\"," +
                "\"status\": 200}]";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        MultiStatusResponse.fromJson(new ObjectMapper(), stream);
    }
}
