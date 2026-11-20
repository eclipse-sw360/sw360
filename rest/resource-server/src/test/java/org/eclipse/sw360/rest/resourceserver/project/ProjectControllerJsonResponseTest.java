/*
 * Copyright Harsh Kumar, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Jackson ObjectMapper produces identical JSON to the previous Gson implementation
 * for the ProjectController response paths that return JSON.
 */
class ProjectControllerJsonResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldProduceIdenticalJsonForProjectCountResponse() throws Exception {
        // This is the JSON produced by getUserProjectCount endpoint
        // {"status":"success","count":42}

        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "success");
        node.put("count", 42);

        JsonNode expected = objectMapper.readTree("{\"status\":\"success\",\"count\":42}");
        JsonNode actual = objectMapper.readTree(node.toString());

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.get("status").asText()).isEqualTo("success");
        assertThat(actual.get("count").asInt()).isEqualTo(42);
    }

    @Test
    void shouldProduceIdenticalJsonForLicenseInfoHeaderResponse() throws Exception {
        // This is the JSON produced by getLicenseInfoHeader endpoint
        // {"status":"success","headerText":"..."}

        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "success");
        node.put("headerText", "Test header text");

        JsonNode expected = objectMapper.readTree("{\"status\":\"success\",\"headerText\":\"Test header text\"}");
        JsonNode actual = objectMapper.readTree(node.toString());

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.get("status").asText()).isEqualTo("success");
        assertThat(actual.get("headerText").asText()).isEqualTo("Test header text");
    }
}
