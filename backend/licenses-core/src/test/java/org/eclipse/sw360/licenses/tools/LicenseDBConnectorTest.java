/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for LicenseDBConnector.
 * Tests cover: successful fetch, empty response, and malformed response handling.
 * Relates to issue #3840.
 */
public class LicenseDBConnectorTest {

    private MockWebServer mockWebServer;
    private LicenseDBConnector connector;

    @Before
    public void setUp() throws IOException {
        // Start a local mock HTTP server to simulate LicenseDB API responses
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Point connector to the mock server
        String baseUrl = mockWebServer.url("/").toString();
        connector = new LicenseDBConnector(baseUrl, "");
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testFetchAllLicenses_success() throws Exception {
        // Given: LicenseDB returns one license
        String mockResponse = "{"
                + "\"data\": [{"
                + "  \"shortname\": \"MIT\","
                + "  \"fullname\": \"MIT License\","
                + "  \"text\": \"Permission is hereby granted...\""
                + "}],"
                + "\"paginationmeta\": {\"next\": \"\"}"
                + "}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // When: fetchAllLicenses is called
        List<License> licenses = connector.fetchAllLicenses();

        // Then: one license is returned with correct fields
        assertEquals(1, licenses.size());
        assertEquals("MIT", licenses.get(0).getId());
        assertEquals("MIT License", licenses.get(0).getFullname());
    }

    @Test
    public void testFetchAllLicenses_emptyResponse() throws Exception {
        // Given: LicenseDB returns empty data array
        String mockResponse = "{"
                + "\"data\": [],"
                + "\"paginationmeta\": {\"next\": \"\"}"
                + "}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // When: fetchAllLicenses is called
        List<License> licenses = connector.fetchAllLicenses();

        // Then: empty list is returned, no exception thrown
        assertNotNull(licenses);
        assertEquals(0, licenses.size());
    }

    @Test
    public void testFetchAllLicenses_malformedResponse() {
        // Given: LicenseDB returns malformed JSON
        mockWebServer.enqueue(new MockResponse()
                .setBody("not valid json")
                .addHeader("Content-Type", "application/json"));

        // When: fetchAllLicenses is called
        List<License> licenses = connector.fetchAllLicenses();

        // Then: empty list returned gracefully, no exception thrown
        assertNotNull(licenses);
        assertEquals(0, licenses.size());
    }

    @Test
    public void testFetchAllObligations_success() throws Exception {
        // Given: LicenseDB returns one obligation
        String mockResponse = "{"
                + "\"data\": [{"
                + "  \"topic\": \"Provide copyright notices\","
                + "  \"text\": \"You must retain copyright notices.\""
                + "}],"
                + "\"paginationmeta\": {\"next\": \"\"}"
                + "}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // When: fetchAllObligations is called
        List<Obligation> obligations = connector.fetchAllObligations();

        // Then: one obligation is returned with correct fields
        assertEquals(1, obligations.size());
        assertEquals("Provide copyright notices", obligations.get(0).getTitle());
        assertEquals("You must retain copyright notices.", obligations.get(0).getText());
    }

    @Test
    public void testFetchAllObligations_emptyResponse() throws Exception {
        // Given: LicenseDB returns empty data array
        String mockResponse = "{"
                + "\"data\": [],"
                + "\"paginationmeta\": {\"next\": \"\"}"
                + "}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        // When: fetchAllObligations is called
        List<Obligation> obligations = connector.fetchAllObligations();

        // Then: empty list returned, no exception thrown
        assertNotNull(obligations);
        assertEquals(0, obligations.size());
    }
}
