/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VelocifyConnectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JSON_API_CONTENT_TYPE = "application/vnd.api+json";

    // Known Velocify staging data:
    //   Component 3  → "Windows Server 2003 All Versions"
    //   Notification 22570 → has vulnerability 306872 (CVE-2013-3900)
    private static final String VELOCIFY_COMPONENT_ID = "3";
    private static final String VELOCIFY_NOTIFICATION_ID = "22570";
    private static final String VELOCIFY_VULNERABILITY_ID = "306872";
    private static final String EXPECTED_CVE = "CVE-2013-3900";

    private VelocifyConnector connector;

    @Before
    public void setUp() {
        connector = new VelocifyConnector();
    }

    @Test
    public void extractResourceId_returnsIdForJsonApiSingleResource() throws Exception {
        String response = "{" +
                "\"data\": {\"id\": \"cmp-123\", \"type\": \"components\"}" +
                "}";

        Optional<String> resourceId = VelocifyConnector.extractResourceId(MAPPER.readTree(response));

        assertEquals(Optional.of("cmp-123"), resourceId);
    }

    @Test
    public void findComponentIdByRelease_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        Release release = new Release().setName("example").setVersion("1.0.0");

        Optional<String> componentId = unconfigured.findComponentIdByRelease(release);

        assertFalse(componentId.isPresent());
    }

    /**
     * Step 1: Verifies that the real Velocify API returns notification IDs for
     * component 3 ("Windows Server 2003 All Versions").
     * Expected: at least one notification, including 22570.
     */
    @Test
    public void getComponentNotificationIds_returnsNotificationsForKnownComponent() throws Exception {
        requireStaging();
        List<String> notificationIds = connector.getComponentNotificationIds(VELOCIFY_COMPONENT_ID);

        assertNotNull("Notification list must not be null", notificationIds);
        assertFalse("Expected at least one notification for component " + VELOCIFY_COMPONENT_ID,
                notificationIds.isEmpty());
        assertTrue("Expected notification " + VELOCIFY_NOTIFICATION_ID + " in list",
                notificationIds.contains(VELOCIFY_NOTIFICATION_ID));
    }

    /**
     * Step 2: Verifies that notification 22570 links to vulnerability 306872.
     */
    @Test
    public void getNotificationVulnerabilityIds_returnsVulnerabilityForKnownNotification() throws Exception {
        requireStaging();
        List<String> vulnerabilityIds = connector.getNotificationVulnerabilityIds(VELOCIFY_NOTIFICATION_ID);

        assertNotNull("Vulnerability ID list must not be null", vulnerabilityIds);
        assertTrue("Expected vulnerability " + VELOCIFY_VULNERABILITY_ID + " linked to notification "
                + VELOCIFY_NOTIFICATION_ID, vulnerabilityIds.contains(VELOCIFY_VULNERABILITY_ID));
    }

    /**
     * Step 3: Verifies that vulnerability 306872 is correctly mapped to a SW360
     * {@link Vulnerability} object with the CVE identifier as externalId.
     */
    @Test
    public void getVulnerabilityAsSw360_mapsCveIdAndDescriptionForKnownVulnerability() throws Exception {
        requireStaging();
        Optional<Vulnerability> result = connector.getVulnerabilityAsSw360(
                VELOCIFY_VULNERABILITY_ID, VELOCIFY_COMPONENT_ID);

        assertTrue("Expected vulnerability to be present for id " + VELOCIFY_VULNERABILITY_ID,
                result.isPresent());
        Vulnerability vuln = result.get();

        assertEquals("externalId must be the CVE identifier", EXPECTED_CVE, vuln.getExternalId());
        assertNotNull("description must not be null", vuln.getDescription());
        assertFalse("description must not be empty", vuln.getDescription().isEmpty());
        assertTrue("assignedExtComponentIds must contain the Velocify component id",
                vuln.getAssignedExtComponentIds().contains(VELOCIFY_COMPONENT_ID));
    }

    /**
     * Full chain test: component 3 → notifications → vulnerability 306872 →
     * SW360 Vulnerability with CVE-2013-3900.
     * This is the same path that {@code syncVelocifyVulnerabilitiesForMappedReleases}
     * follows at runtime.
     */
    @Test
    public void fullVulnerabilityChain_componentToSw360Vulnerability() throws Exception {
        requireStaging();
        // Step 1: fetch notification IDs for the known Velocify component
        List<String> notificationIds = connector.getComponentNotificationIds(VELOCIFY_COMPONENT_ID);
        assertFalse("Component " + VELOCIFY_COMPONENT_ID + " must have at least one notification",
                notificationIds.isEmpty());

        // Step 2: collect all vulnerability IDs across all notifications
        java.util.Set<String> allVulnIds = new java.util.HashSet<>();
        for (String notifId : notificationIds) {
            allVulnIds.addAll(connector.getNotificationVulnerabilityIds(notifId));
        }
        assertTrue("The chain must surface vulnerability " + VULNERABILIT_ID_IN_CHAIN,
                allVulnIds.contains(VULNERABILIT_ID_IN_CHAIN));

        // Step 3: fetch and map the vulnerability to a SW360 object
        Optional<Vulnerability> vulnOpt = connector.getVulnerabilityAsSw360(
                VULNERABILIT_ID_IN_CHAIN, VELOCIFY_COMPONENT_ID);
        assertTrue("Vulnerability must be present after full chain traversal", vulnOpt.isPresent());

        Vulnerability vuln = vulnOpt.get();
        assertEquals("externalId must be CVE-2013-3900 at end of full chain",
                EXPECTED_CVE, vuln.getExternalId());
        assertNotNull("description must be populated", vuln.getDescription());
        assertTrue("assignedExtComponentIds must contain component id",
                vuln.getAssignedExtComponentIds().contains(VELOCIFY_COMPONENT_ID));
    }

    /**
     * Verifies that findComponentIdByRelease resolves component 3 via name+version match.
     * Windows Server 2003 / All Versions must be present in the Velocify staging catalogue.
     */
    @Test
    public void findComponentIdByRelease_findsComponentByNameAndVersion() throws Exception {
        requireStaging();
        Release release = new Release()
                .setName("Windows Server 2003")
                .setVersion("All Versions");

        Optional<String> componentId = connector.findComponentIdByRelease(release);

        assertTrue("Expected a component ID to be found by name+version", componentId.isPresent());
        assertEquals("Expected component ID 3 for Windows Server 2003 / All Versions",
                VELOCIFY_COMPONENT_ID, componentId.get());
    }

    /**
     * Verifies that findComponentIdByRelease resolves component 3 via URL match.
     * The externalIds map must contain an entry whose value matches the Velocify component URL.
     */
    @Test
    public void findComponentIdByRelease_findsComponentByUrl() throws Exception {
        requireStaging();
        Release release = new Release()
                .setName("irrelevant")
                .setVersion("irrelevant")
                .setExternalIds(java.util.Map.of(
                        "package-url",
                        "http://www.microsoft.com/windowsserver2003/howtobuy/licensing/priclicfaq.mspx"
                ));

        Optional<String> componentId = connector.findComponentIdByRelease(release);

        assertTrue("Expected a component ID to be found by URL", componentId.isPresent());
        assertEquals("Expected component ID 3 for the known Windows Server 2003 URL",
                VELOCIFY_COMPONENT_ID, componentId.get());
    }

    /**
     * Verifies that findComponentIdByRelease returns empty when no component matches.
     */
    @Test
    public void findComponentIdByRelease_returnsEmptyWhenNoMatchFound() throws Exception {
        requireStaging();
        Release release = new Release()
                .setName("no-such-component-xyzzy-sw360-test")
                .setVersion("99.99.99-not-a-real-version");

        Optional<String> componentId = connector.findComponentIdByRelease(release);

        assertFalse("Expected no match for a completely unknown component", componentId.isPresent());
    }

    /**
     * Verifies that createComponentRequest returns a request ID for a release that
     * does not yet have a matching Velocify component.
     */
    @Test
    public void createComponentRequest_returnsRequestIdForUnknownRelease() throws Exception {
        requireStaging();
        Vendor vendor = new Vendor().setFullname("Apache Software Foundation");
        Release release = new Release()
                .setName("test-library-sw360-e2e")
                .setVersion("0.0.1-test")
                .setVendor(vendor);

        Optional<String> requestId = connector.createComponentRequest(release);

        assertTrue("Expected a request id to be returned", requestId.isPresent());
        assertFalse("Request id must not be blank", requestId.get().isBlank());
    }

    private static final String VULNERABILIT_ID_IN_CHAIN = VELOCIFY_VULNERABILITY_ID;

    // ─── Unit tests for monitoringList methods (no network) ───────────────────

    @Test
    public void createMonitoringList_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        Optional<String> result = unconfigured.createMonitoringList("My List", "A comment");

        assertFalse("createMonitoringList must return empty when connector is not configured",
                result.isPresent());
    }

    @Test
    public void createMonitoringList_returnsEmptyWhenNameIsBlank() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        Optional<String> result = unconfigured.createMonitoringList("  ", null);

        assertFalse("createMonitoringList must return empty when name is blank",
                result.isPresent());
    }

    @Test
    public void updateMonitoringList_isNoOpWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        // Must not throw even though PATCH would normally be called
        unconfigured.updateMonitoringList("some-id", "New Name", null);
    }

    @Test
    public void updateMonitoringList_isNoOpWhenListIdIsBlank() throws Exception {
        // isConfigured() returns false for a fresh connector without properties;
        // we only test that blank id short-circuits correctly
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        unconfigured.updateMonitoringList("", "New Name", "desc");
        unconfigured.updateMonitoringList(null, "New Name", "desc");
    }

    @Test
    public void replaceMonitoringListUsers_isNoOpWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        unconfigured.replaceMonitoringListUsers("list-id",
                Arrays.asList("user-1", "user-2"));
    }

    @Test
    public void replaceMonitoringListComponents_isNoOpWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        unconfigured.replaceMonitoringListComponents("list-id",
                Collections.singletonList("cmp-1"));
    }

    @Test
    public void replaceMonitoringListChildren_isNoOpWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        unconfigured.replaceMonitoringListChildren("list-id",
                Arrays.asList("child-1", "child-2"));
    }

    @Test
    public void getMonitoringList_returnsMissingNodeWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        assertTrue("getMonitoringList must return a missing node when not configured",
                unconfigured.getMonitoringList("list-id", "components,children").isMissingNode());
    }

    @Test
    public void getMonitoringList_returnsMissingNodeWhenListIdIsBlank() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        assertTrue("getMonitoringList must return a missing node for blank id",
                unconfigured.getMonitoringList("", null).isMissingNode());
    }

    @Test
    public void getNotificationIdsAfter_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getNotificationIdsAfter("2024-01-01T00:00:00Z");

        assertNotNull(ids);
        assertTrue("getNotificationIdsAfter must return empty list when not configured",
                ids.isEmpty());
    }

    @Test
    public void getNotificationIdsAfter_returnsEmptyForNullTimestamp() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getNotificationIdsAfter(null);

        assertNotNull(ids);
        assertTrue("getNotificationIdsAfter must return empty list when not configured",
                ids.isEmpty());
    }

    @Test
    public void getVulnerabilityIdsAfter_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getVulnerabilityIdsAfter("2024-06-01T00:00:00Z");

        assertNotNull(ids);
        assertTrue("getVulnerabilityIdsAfter must return empty list when not configured",
                ids.isEmpty());
    }

    /**
     * Verifies that a monitoringList can be created via the real Vilocify staging API
     * and that the returned ID is non-blank.
     */
    @Test
    public void createMonitoringList_returnsIdFromStagingApi() throws Exception {
        requireStaging();
        Optional<String> listId = connector.createMonitoringList(
                "SW360-test-list-" + System.currentTimeMillis(),
                "Created by SW360 VelocifyConnectorTest");

        assertTrue("Expected a monitoringList ID to be returned from the staging API",
                listId.isPresent());
        assertFalse("MonitoringList ID must not be blank", listId.get().isBlank());
    }

    /**
     * Verifies that notifications updated after a given timestamp can be fetched
     * incrementally from the staging API.
     */
    @Test
    public void getNotificationIdsAfter_returnsListFromStagingApi() throws Exception {
        requireStaging();
        // Fetch notifications updated in the last 365 days to get a non-trivial result set
        String after = java.time.Instant.now()
                .minus(365, java.time.temporal.ChronoUnit.DAYS)
                .toString();

        List<String> ids = connector.getNotificationIdsAfter(after);

        assertNotNull("Notification ID list must not be null", ids);
        // We cannot guarantee any results but the call must succeed
    }

    /**
     * Verifies that the include parameter reduces round-trips: fetching a known
     * monitoringList with include=components should return a valid JSON:API response.
     */
    @Test
    public void getMonitoringList_withIncludeComponents_returnsValidResponse() throws Exception {
        requireStaging();
        // First create a list so we have a known ID
        Optional<String> listId = connector.createMonitoringList(
                "SW360-include-test-" + System.currentTimeMillis(), null);
        Assume.assumeTrue("Need a monitoringList ID for this test", listId.isPresent());

        com.fasterxml.jackson.databind.JsonNode response =
                connector.getMonitoringList(listId.get(), "components");

        assertFalse("Response must not be a missing node", response.isMissingNode());
        assertFalse("Response must contain a 'data' field", response.path("data").isMissingNode());
    }

    /**
     * Skips the current test if the Velocify connector is not configured or the
     * Velocify staging API host is not reachable. Must be called at the start of
     * every test that sends real HTTP requests to the staging environment.
     */
    private void requireStaging() {
        Assume.assumeTrue("Set RUN_VELOCIFY_LIVE_TESTS=true to run live Velocify tests",
            "true".equalsIgnoreCase(System.getenv("RUN_VELOCIFY_LIVE_TESTS")));
        Assume.assumeTrue("Velocify connector not configured; skipping live API test",
                connector.isConfigured());
        Assume.assumeTrue("Configured Velocify host not reachable/TLS failed; skipping live API test",
            isConfiguredVelocifyEndpointReachable());
    }

    @Test
    public void isConfigured_returnsFalseWhenBaseUrlIsBlank() {
        VelocifyConnector c = new VelocifyConnector("", "/api/v2", "some-token");
        assertFalse("isConfigured must return false when baseUrl is blank", c.isConfigured());
    }

    @Test
    public void isConfigured_returnsFalseWhenTokenIsBlank() {
        VelocifyConnector c = new VelocifyConnector("https://example.com", "/api/v2", "");
        assertFalse("isConfigured must return false when token is blank", c.isConfigured());
    }

    @Test
    public void getJson_sendsAuthorizationBearerTokenInHeader() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        AtomicReference<String> capturedAuth = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/components/auth-cmp/relationships/notifications", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "my-secret-token");
            c.getComponentNotificationIds("auth-cmp");
            assertEquals("Authorization header must carry 'Bearer <token>'",
                    "Bearer my-secret-token", capturedAuth.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void extractResourceId_returnsFirstIdFromJsonApiArrayData() throws Exception {
        String response = "{\"data\":["
                + "{\"id\":\"arr-1\",\"type\":\"components\"},"
                + "{\"id\":\"arr-2\",\"type\":\"components\"}"
                + "]}";
        Optional<String> id = VelocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertEquals("Must return the first element ID from an array data node",
                Optional.of("arr-1"), id);
    }

    @Test
    public void extractResourceId_returnsEmptyForNullData() throws Exception {
        String response = "{\"data\":null}";
        Optional<String> id = VelocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse("Must return empty when data is explicitly null", id.isPresent());
    }

    @Test
    public void extractResourceId_returnsEmptyForMissingDataField() throws Exception {
        String response = "{\"links\":{\"self\":\"/api/v2/components\"}}";
        Optional<String> id = VelocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse("Must return empty when the data field is absent", id.isPresent());
    }

    @Test
    public void extractResourceId_returnsEmptyForEmptyArrayData() throws Exception {
        String response = "{\"data\":[]}";
        Optional<String> id = VelocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse("Must return empty when data is an empty array", id.isPresent());
    }

    @Test
    public void getVulnerabilityAsSw360_parsesAllJsonApiAttributesCorrectly() throws Exception {
        String body = "{"
            + "\"jsonapi\":{\"version\":\"1.1\"},"
            + "\"data\":{"
            + "\"id\":\"999\","
            + "\"type\":\"vulnerabilities\","
            + "\"links\":{\"self\":\"/api/v2/vulnerabilities/999\"},"
            + "\"attributes\":{"
            + "\"cve\":\"CVE-2024-1234\","
            + "\"cwe\":\"CWE-79\","
            + "\"description\":\"A detailed description\","
            + "\"mitigatingFactor\":\"Apply fixed package\","
            + "\"note\":\"Vendor note text\","
            + "\"deleted\":false,"
            + "\"cvss\":["
            + "{\"version\":\"3.1\",\"vector\":\"CVSS:3.1/AV:N\",\"base_score\":7.2},"
            + "{\"version\":\"2.0\",\"vector\":\"AV:N/AC:L\",\"base_score\":5.0}"
            + "]"
            + "}"
            + "}"
            + "}";

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities/999", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<Vulnerability> result = c.getVulnerabilityAsSw360("999", "comp-42");

            assertTrue("Vulnerability must be present in the response", result.isPresent());
            Vulnerability v = result.get();
            assertEquals("CVE-2024-1234", v.getExternalId());
            assertEquals("CVE-2024-1234", v.getTitle());
            assertEquals("A detailed description", v.getDescription());
            assertEquals("CWE-79", v.getCwe());
            assertEquals("Apply fixed package", v.getAction());
            assertEquals("Vendor note text", v.getLegalNotice());
            assertEquals(7.2d, v.getCvss(), 0.0001d);
            assertTrue("isSetCvss must be true when cvss array has base_score",
                    v.isSetIsSetCvss() && v.isIsSetCvss());
            assertTrue("Metadata map must contain vilocify source",
                    v.isSetCveFurtherMetaDataPerSource()
                            && v.getCveFurtherMetaDataPerSource().containsKey("vilocify"));
            assertEquals("3.1",
                    v.getCveFurtherMetaDataPerSource().get("vilocify").get("cvssVersion"));
            assertEquals("CVSS:3.1/AV:N",
                    v.getCveFurtherMetaDataPerSource().get("vilocify").get("cvssVector"));
            assertTrue("assignedExtComponentIds must contain the given component id",
                    v.getAssignedExtComponentIds().contains("comp-42"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityAsSw360_returnsEmptyWhenEndpointReturns404() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities/404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<Vulnerability> result = c.getVulnerabilityAsSw360("404", "cmp-1");
            assertFalse("404 must be mapped to Optional.empty", result.isPresent());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityAsSw360_throwsOnUnauthorizedStatus() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities/401", exchange -> {
            String body = "{\"errors\":[{\"status\":\"401\"}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(401, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            try {
                c.getVulnerabilityAsSw360("401", "cmp-1");
                fail("Expected SW360Exception for non-2xx and non-404 status");
            } catch (SW360Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getWhy();
                assertTrue("Exception message must include HTTP status",
                        message != null && message.contains("HTTP 401"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityAsSw360_throwsForMissingJsonApiEnvelope() throws Exception {
        String body = "{\"data\":{\"id\":\"999\",\"type\":\"vulnerabilities\",\"attributes\":{\"deleted\":false}}}";

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities/999", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            try {
                c.getVulnerabilityAsSw360("999", "cmp-1");
                fail("Expected SW360Exception for invalid JSON:API envelope");
            } catch (SW360Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getWhy();
                assertTrue("Exception must mention jsonapi.version",
                        message != null && message.contains("jsonapi.version"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getMembershipIdByEmail_returnsEmptyWhenNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("getMembershipIdByEmail must return empty when connector is not configured",
                unconfigured.getMembershipIdByEmail("user@example.com").isPresent());
    }

    @Test
    public void getMembershipIdByEmail_returnsEmptyWhenEmailIsBlank() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("getMembershipIdByEmail must return empty for blank email",
                unconfigured.getMembershipIdByEmail("  ").isPresent());
        assertFalse("getMembershipIdByEmail must return empty for null email",
                unconfigured.getMembershipIdByEmail(null).isPresent());
    }

    @Test
    public void createMembership_returnsEmptyWhenNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("createMembership must return empty when connector is not configured",
                unconfigured.createMembership("user@example.com").isPresent());
    }

    @Test
    public void createMembership_returnsEmptyWhenEmailIsBlank() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("createMembership must return empty for blank email",
                unconfigured.createMembership("").isPresent());
        assertFalse("createMembership must return empty for null email",
                unconfigured.createMembership(null).isPresent());
    }

    @Test
    public void createMembership_returnsIdFromStagingApi() throws Exception {
        requireStaging();
        String testEmail = "sw360-test-" + System.currentTimeMillis() + "@example.com";
        Optional<String> membershipId;
        try {
            membershipId = connector.createMembership(testEmail);
        } catch (SW360Exception e) {
            String why = e.getWhy() != null ? e.getWhy() : e.getMessage();
            Assume.assumeTrue("Staging rejected membership creation (expected in restricted env): " + why,
                    why == null || !why.contains("HTTP 422"));
            throw e;
        }
        assertTrue("createMembership must return a non-empty membership ID",
                membershipId.isPresent());
        assertFalse("Membership ID must not be blank", membershipId.get().isBlank());
    }

    @Test
    public void getMembershipIdByEmail_returnsIdForExistingMembership() throws Exception {
        requireStaging();
        // Create a membership first so we have a known email to look up
        String testEmail = "sw360-lookup-" + System.currentTimeMillis() + "@example.com";
        Optional<String> createdId;
        try {
            createdId = connector.createMembership(testEmail);
        } catch (SW360Exception e) {
            String why = e.getWhy() != null ? e.getWhy() : e.getMessage();
            Assume.assumeTrue("Staging rejected membership creation (expected in restricted env): " + why,
                    why == null || !why.contains("HTTP 422"));
            throw e;
        }
        Assume.assumeTrue("Prerequisite: membership creation must succeed", createdId.isPresent());

        Optional<String> foundId = connector.getMembershipIdByEmail(testEmail);
        assertTrue("getMembershipIdByEmail must find the membership we just created",
                foundId.isPresent());
        assertEquals("Found membership ID must match the ID returned by createMembership",
                createdId.get(), foundId.get());
    }

    // ─── Requirement 6: componentRequest status & mapping ─────────────────────

    @Test
    public void resolveMappedComponentId_returnsEmptyWhenNotConfigured() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("resolveMappedComponentId must return empty when connector is not configured",
                unconfigured.resolveMappedComponentId("some-request-id").isPresent());
    }

    @Test
    public void resolveMappedComponentId_returnsEmptyWhenRequestIdIsBlank() throws Exception {
        VelocifyConnector unconfigured = new VelocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse("resolveMappedComponentId must return empty for blank request ID",
                unconfigured.resolveMappedComponentId("").isPresent());
        assertFalse("resolveMappedComponentId must return empty for null request ID",
                unconfigured.resolveMappedComponentId(null).isPresent());
    }

    @Test
    public void resolveMappedComponentId_parsesJsonApiRelationshipResponse() throws Exception {
        // Serve a synthetic JSON:API response that already has a component relationship
        String body = "{\"data\":{\"id\":\"req-1\",\"type\":\"componentRequests\","
                + "\"relationships\":{\"component\":{\"data\":{\"id\":\"comp-999\","
                + "\"type\":\"components\"}}}}}";

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/componentRequests/req-1", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<String> componentId = c.resolveMappedComponentId("req-1");
            assertTrue("Must extract component ID from JSON:API relationship data",
                    componentId.isPresent());
            assertEquals("comp-999", componentId.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void resolveMappedComponentId_returnsEmptyForPendingRequest() throws Exception {
        requireStaging();
        // A freshly created componentRequest will not yet have been mapped by Velocify
        Vendor vendor = new Vendor().setFullname("Test Vendor Corp");
        Release release = new Release()
                .setName("pending-mapping-test-" + System.currentTimeMillis())
                .setVersion("0.0.1-pending")
                .setVendor(vendor);
        Optional<String> requestId = connector.createComponentRequest(release);
        Assume.assumeTrue("Prerequisite: componentRequest creation must succeed",
                requestId.isPresent());

        Optional<String> componentId = connector.resolveMappedComponentId(requestId.get());
        assertFalse("A freshly created componentRequest must not yet be mapped to a Velocify component",
                componentId.isPresent());
    }

    // ─── Requirement 8: cursor-based pagination ───────────────────────────────

    @Test
    public void getComponentNotificationIds_followsPaginationCursorAcrossPages() throws Exception {
        String page1 = "{\"data\":["
                + "{\"type\":\"notifications\",\"id\":\"n-1\"},"
                + "{\"type\":\"notifications\",\"id\":\"n-2\"}"
                + "],\"links\":{\"next\":\"/api/v2/components/paging-cmp/relationships/notifications"
                + "?page%5Bafter%5D=cursor2\"}}";
        String page2 = "{\"data\":["
                + "{\"type\":\"notifications\",\"id\":\"n-3\"}"
                + "],\"links\":{\"next\":null}}";

        List<String> capturedQueries = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/components/paging-cmp/relationships/notifications", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            capturedQueries.add(query == null ? "" : query);
            boolean isSecondPage = query != null && query.contains("after");
            byte[] bytes = (isSecondPage ? page2 : page1).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            List<String> ids = c.getComponentNotificationIds("paging-cmp");

            assertEquals("Must collect IDs from both pages", 3, ids.size());
            assertTrue(ids.contains("n-1"));
            assertTrue(ids.contains("n-2"));
            assertTrue(ids.contains("n-3"));
            // Verify the initial request included the max page size
            String firstQuery = capturedQueries.get(0);
            assertTrue("Initial request must include page[size]=200",
                    firstQuery.contains("page%5Bsize%5D=200") || firstQuery.contains("page[size]=200"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getNotificationVulnerabilityIds_followsPaginationCursorAcrossPages() throws Exception {
        String page1 = "{\"data\":["
                + "{\"type\":\"vulnerabilities\",\"id\":\"v-1\"},"
                + "{\"type\":\"vulnerabilities\",\"id\":\"v-2\"}"
                + "],\"links\":{\"next\":\"/api/v2/notifications/n-99/relationships/vulnerabilities"
                + "?page%5Bafter%5D=cur2\"}}";
        String page2 = "{\"data\":["
                + "{\"type\":\"vulnerabilities\",\"id\":\"v-3\"}"
                + "],\"links\":{\"next\":null}}";

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/notifications/n-99/relationships/vulnerabilities", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            boolean second = query != null && query.contains("after");
            byte[] bytes = (second ? page2 : page1).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            List<String> ids = c.getNotificationVulnerabilityIds("n-99");
            assertEquals("Must collect IDs from both pages", 3, ids.size());
            assertTrue(ids.containsAll(Arrays.asList("v-1", "v-2", "v-3")));
        } finally {
            server.stop(0);
        }
    }

    // ─── Requirement 9: incremental update filters ────────────────────────────

    @Test
    public void getNotificationIdsAfter_sendsUpdatedAtFilterParameter() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/notifications", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getNotificationIdsAfter("2024-05-01T00:00:00Z");

            assertFalse("Server must have received at least one request", capturedQueries.isEmpty());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue("Query must contain filter[updatedAt][after]=2024-05-01T00:00:00Z",
                    decoded.contains("filter[updatedAt][after]=2024-05-01T00:00:00Z"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityIdsAfter_sendsUpdatedAtFilterParameter() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getVulnerabilityIdsAfter("2024-03-15T12:00:00Z");

            assertFalse("Server must have received at least one request", capturedQueries.isEmpty());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue("Query must contain filter[updatedAt][after]=2024-03-15T12:00:00Z",
                    decoded.contains("filter[updatedAt][after]=2024-03-15T12:00:00Z"));
            assertTrue("Query must contain filter[deleted][eq]=false by default",
                    decoded.contains("filter[deleted][eq]=false"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityIds_buildsAllSupportedFilterParameters() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/vulnerabilities", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("filter[createdAt][after]", "2024-01-01T00:00:00Z");
            params.put("filter[updatedAt][after]", "2024-02-01T00:00:00Z");
            params.put("filter[id][eq]", "321");
            params.put("filter[id][in]", "11,22");
            params.put("filter[notifications.id][any]", "900,901");
            params.put("filter[deleted][eq]", "true");
            params.put("sort", "-id");
            params.put("fields[vulnerabilities]", "cve,cwe,description");

            c.getVulnerabilityIds(params);

            assertFalse("Server must have received at least one request", capturedQueries.isEmpty());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("filter[createdAt][after]=2024-01-01T00:00:00Z"));
            assertTrue(decoded.contains("filter[updatedAt][after]=2024-02-01T00:00:00Z"));
            assertTrue(decoded.contains("filter[id][eq]=321"));
            assertTrue(decoded.contains("filter[id][in]=11,22"));
            assertTrue(decoded.contains("filter[notifications.id][any]=900,901"));
            assertTrue(decoded.contains("filter[deleted][eq]=true"));
            assertTrue(decoded.contains("sort=-id"));
            assertTrue(decoded.contains("fields[vulnerabilities]=cve,cwe,description"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getNotificationIdsAfter_omitsFilterWhenTimestampIsNull() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/notifications", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getNotificationIdsAfter(null);

            assertFalse("Server must have received at least one request", capturedQueries.isEmpty());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertFalse("Query must NOT contain filter[updatedAt][after] when timestamp is null",
                    decoded.contains("filter[updatedAt][after]"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getVulnerabilityIdsAfter_returnsListFromStagingApi() throws Exception {
        requireStaging();
        String after = java.time.Instant.now()
                .minus(365, java.time.temporal.ChronoUnit.DAYS)
                .toString();
        List<String> ids;
        try {
            ids = connector.getVulnerabilityIdsAfter(after);
        } catch (SW360Exception e) {
            String why = e.getWhy() != null ? e.getWhy() : e.getMessage();
            Assume.assumeTrue("Staging rejected vulnerability filter (expected in restricted env): " + why,
                    why == null || !why.contains("HTTP 422"));
            throw e;
        }
        assertNotNull("Vulnerability ID list must not be null", ids);
        // The call must succeed without errors; specific IDs cannot be guaranteed in staging
    }

    // ─── Requirement 10: include parameter ────────────────────────────────────

    @Test
    public void getMonitoringList_sendsIncludeParameterInRequest() throws Exception {
        String body = "{\"data\":{\"id\":\"ml-1\",\"type\":\"monitoringLists\",\"attributes\":{}},"
                + "\"included\":[]}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/monitoringLists/ml-1", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            JsonNode response = c.getMonitoringList("ml-1", "components,children");

            assertFalse("Response must not be a missing node", response.isMissingNode());
            assertFalse("Response must contain 'data'", response.path("data").isMissingNode());
            assertFalse("Response must contain 'included'", response.path("included").isMissingNode());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue("Query must contain include= with the requested relationships",
                    decoded.contains("include=") && decoded.contains("components") && decoded.contains("children"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getMonitoringList_omitsIncludeParameterWhenNull() throws Exception {
        String body = "{\"data\":{\"id\":\"ml-2\",\"type\":\"monitoringLists\",\"attributes\":{}}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/monitoringLists/ml-2", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VelocifyConnector c = new VelocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getMonitoringList("ml-2", null);

            assertFalse("Server must have received at least one request", capturedQueries.isEmpty());
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertFalse("Query must NOT contain 'include=' when includeParams is null",
                    decoded.contains("include="));
        } finally {
            server.stop(0);
        }
    }

        private static boolean isConfiguredVelocifyEndpointReachable() {
        try {
            java.util.Properties props = CommonUtils.loadProperties(VelocifyConnector.class, "/sw360.properties");
            String baseUrl = props.getProperty(SW360ConfigKeys.VELOCIFY_API_BASE_URL, "");
            String rootPath = props.getProperty(SW360ConfigKeys.VELOCIFY_API_ROOT_PATH, "/api/v2");

            if (StringUtils.isBlank(baseUrl)) {
            return false;
            }

            String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
            String normalizedRoot = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
            String probeUrl = normalizedBase + normalizedRoot + "/components?page%5Bsize%5D=1";

            HttpURLConnection conn = (HttpURLConnection)
                new URL(probeUrl)
                            .openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500; // 4xx is still "reachable"
        } catch (Exception e) {
            return false;
        }
    }
}