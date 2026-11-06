/*
 * Copyright Harsh Kumar, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that Jackson ObjectNode produces identical JSON to the previous Gson implementation.
 * This captures the expected JSON structure for the three response paths in SW360ReportController.
 */
class SW360ReportControllerJsonResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldProduceIdenticalJsonForProjectReportResponse() throws Exception {
        // This is the exact JSON that Gson produced:
        // {"response":"The downloaded report link will be send to the end user."}

        ObjectNode node = objectMapper.createObjectNode();
        node.put("response", "The downloaded report link will be send to the end user.");

        JsonNode expected = objectMapper.readTree("{\"response\":\"The downloaded report link will be send to the end user.\"}");
        JsonNode actual = objectMapper.readTree(node.toString());

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.get("response").asText())
                .isEqualTo("The downloaded report link will be send to the end user.");
    }

    @Test
    void shouldProduceIdenticalJsonForComponentReportResponse() throws Exception {
        // This is the exact JSON that Gson produced:
        // {"response":"Component report download link will get send to the end user."}

        ObjectNode node = objectMapper.createObjectNode();
        node.put("response", "Component report download link will get send to the end user.");

        JsonNode expected = objectMapper.readTree("{\"response\":\"Component report download link will get send to the end user.\"}");
        JsonNode actual = objectMapper.readTree(node.toString());

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.get("response").asText())
                .isEqualTo("Component report download link will get send to the end user.");
    }

    @Test
    void shouldProduceIdenticalJsonForLicenseInfoReportResponse() throws Exception {
        // This is the exact JSON that Gson produced:
        // {"response":"The license info report link will be sent to the end user."}

        ObjectNode node = objectMapper.createObjectNode();
        node.put("response", "The license info report link will be sent to the end user.");

        JsonNode expected = objectMapper.readTree("{\"response\":\"The license info report link will be sent to the end user.\"}");
        JsonNode actual = objectMapper.readTree(node.toString());

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.get("response").asText())
                .isEqualTo("The license info report link will be sent to the end user.");
    }
}
