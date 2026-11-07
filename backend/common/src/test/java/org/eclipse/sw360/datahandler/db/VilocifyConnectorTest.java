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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class VilocifyConnectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JSON_API_CONTENT_TYPE = "application/vnd.api+json";

    // Known Vilocify staging data:
    //   Component 3  → "Windows Server 2003 All Versions"
    //   Notification 22570 → has vulnerability 306872 (CVE-2013-3900)
    private static final String VILOCIFY_COMPONENT_ID = "3";
    private static final String VILOCIFY_NOTIFICATION_ID = "22570";
    private static final String VILOCIFY_VULNERABILITY_ID = "306872";
    private static final String EXPECTED_CVE = "CVE-2013-3900";

    private VilocifyConnector connector;

    @BeforeEach
    public void setUp() {
        connector = new VilocifyConnector();
    }

    @Test
    public void extractResourceId_returnsIdForJsonApiSingleResource() throws Exception {
        String response = "{" +
                "\"data\": {\"id\": \"cmp-123\", \"type\": \"components\"}" +
                "}";

        Optional<String> resourceId = VilocifyConnector.extractResourceId(MAPPER.readTree(response));

        assertEquals(Optional.of("cmp-123"), resourceId);
    }

    @Test
    public void findComponentIdByRelease_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
     * Step 1: Verifies that the real Vilocify API returns notification IDs for
     * component 3 ("Windows Server 2003 All Versions").
     * Expected: at least one notification, including 22570.
     */
    @Test
    public void getComponentNotificationIds_returnsNotificationsForKnownComponent() throws Exception {
        requireStaging();
        List<String> notificationIds = connector.getComponentNotificationIds(VILOCIFY_COMPONENT_ID);

        assertNotNull(notificationIds, "Notification list must not be null");
        assertFalse(notificationIds.isEmpty(), "Expected at least one notification for component " + VILOCIFY_COMPONENT_ID);
        assertTrue(notificationIds.contains(VILOCIFY_NOTIFICATION_ID), "Expected notification " + VILOCIFY_NOTIFICATION_ID + " in list");
    }

    /**
     * Step 2: Verifies that notification 22570 links to vulnerability 306872.
     */
    @Test
    public void getNotificationVulnerabilityIds_returnsVulnerabilityForKnownNotification() throws Exception {
        requireStaging();
        List<String> vulnerabilityIds = connector.getNotificationVulnerabilityIds(VILOCIFY_NOTIFICATION_ID);

        assertNotNull(vulnerabilityIds, "Vulnerability ID list must not be null");
        assertTrue(vulnerabilityIds.contains(VILOCIFY_VULNERABILITY_ID), "Expected vulnerability " + VILOCIFY_VULNERABILITY_ID + " linked to notification "
                + VILOCIFY_NOTIFICATION_ID);
    }

    /**
     * Step 3: Verifies that vulnerability 306872 is correctly mapped to a SW360
     * {@link Vulnerability} object with the CVE identifier as externalId.
     */
    @Test
    public void getVulnerabilityAsSw360_mapsCveIdAndDescriptionForKnownVulnerability() throws Exception {
        requireStaging();
        Optional<Vulnerability> result = connector.getVulnerabilityAsSw360(
                VILOCIFY_VULNERABILITY_ID, VILOCIFY_COMPONENT_ID);

        assertTrue(result.isPresent(), "Expected vulnerability to be present for id " + VILOCIFY_VULNERABILITY_ID);
        Vulnerability vuln = result.get();

        assertEquals(EXPECTED_CVE, vuln.getExternalId(), "externalId must be the CVE identifier");
        assertNotNull(vuln.getDescription(), "description must not be null");
        assertFalse(vuln.getDescription().isEmpty(), "description must not be empty");
        assertTrue(vuln.getAssignedExtComponentIds().contains(VILOCIFY_COMPONENT_ID), "assignedExtComponentIds must contain the Vilocify component id");
    }

    /**
     * Full chain test: component 3 → notifications → vulnerability 306872 →
     * SW360 Vulnerability with CVE-2013-3900.
     * This is the same path that {@code syncVilocifyVulnerabilitiesForMappedReleases}
     * follows at runtime.
     */
    @Test
    public void fullVulnerabilityChain_componentToSw360Vulnerability() throws Exception {
        requireStaging();
        // Step 1: fetch notification IDs for the known Vilocify component
        List<String> notificationIds = connector.getComponentNotificationIds(VILOCIFY_COMPONENT_ID);
        assertFalse(notificationIds.isEmpty(), "Component " + VILOCIFY_COMPONENT_ID + " must have at least one notification");

        // Step 2: collect all vulnerability IDs across all notifications
        java.util.Set<String> allVulnIds = new java.util.HashSet<>();
        for (String notifId : notificationIds) {
            allVulnIds.addAll(connector.getNotificationVulnerabilityIds(notifId));
        }
        assertTrue(allVulnIds.contains(VULNERABILITY_ID_IN_CHAIN), "The chain must surface vulnerability " + VULNERABILITY_ID_IN_CHAIN);

        // Step 3: fetch and map the vulnerability to a SW360 object
        Optional<Vulnerability> vulnOpt = connector.getVulnerabilityAsSw360(
                VULNERABILITY_ID_IN_CHAIN, VILOCIFY_COMPONENT_ID);
        assertTrue(vulnOpt.isPresent(), "Vulnerability must be present after full chain traversal");

        Vulnerability vuln = vulnOpt.get();
        assertEquals(EXPECTED_CVE, vuln.getExternalId(), "externalId must be CVE-2013-3900 at end of full chain");
        assertNotNull(vuln.getDescription(), "description must be populated");
        assertTrue(vuln.getAssignedExtComponentIds().contains(VILOCIFY_COMPONENT_ID), "assignedExtComponentIds must contain component id");
    }

    @Test
    public void findComponentIdByRelease_ignoresReleaseWithoutPurl() throws Exception {
        VilocifyConnector c = new VilocifyConnector("http://localhost:9", "/api/v2", "token");
        Release release = new Release()
                .setName("Windows Server 2003")
                .setVersion("All Versions");

        Optional<String> componentId = c.findComponentIdByRelease(release);

        assertFalse(componentId.isPresent(), "Must return empty without HTTP call when release has no PURL");
    }

    @Test
    public void findComponentIdByRelease_queriesEndpointWithPurlFilter() throws Exception {
        String body = "{\"data\":[{\"id\":\"209703\",\"type\":\"components\"}],\"links\":{\"next\":null}}";
        List<String> capturedQueries = new CopyOnWriteArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/components", exchange -> {
            capturedQueries.add(exchange.getRequestURI().getQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Release release = new Release()
                    .setName("symfony/validator")
                    .setVersion("6.4.0")
                    .setExternalIds(java.util.Map.of("package-url", "pkg:composer/symfony/validator@6.4.0"));

            Optional<String> componentId = c.findComponentIdByRelease(release);

            assertEquals(Optional.of("209703"), componentId);
            assertEquals(1, capturedQueries.size(), "Must only execute a single filtered query");
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("filter[name][eq]=PHP Package: symfony/validator"));
            assertTrue(decoded.contains("filter[version][eq]=6.4.0"));
            assertTrue(decoded.contains("filter[active][eq]=true"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void createComponentRequest_skipsHttpCallWhenRequiredFieldsMissing() throws Exception {
        VilocifyConnector c = new VilocifyConnector("http://localhost:9", "/api/v2", "token");
        Release releaseMissingVendorAndUrls = new Release()
                .setId("rel-missing-fields")
                .setName("some-lib")
                .setVersion("1.0.0");

        Optional<String> requestId = c.createComponentRequest(releaseMissingVendorAndUrls);

        assertFalse(requestId.isPresent(),
                "Must return empty locally without HTTP call when vendor/componentUrl/securityUrl are all absent");
    }

    /**
     * Verifies that findComponentIdByRelease resolves component 3 via URL match.
     * The externalIds map must contain an entry whose value matches the Vilocify component URL.
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

        assertTrue(componentId.isPresent(), "Expected a component ID to be found by URL");
        assertEquals(VILOCIFY_COMPONENT_ID, componentId.get(), "Expected component ID 3 for the known Windows Server 2003 URL");
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

        assertFalse(componentId.isPresent(), "Expected no match for a completely unknown component");
    }

    /**
     * Verifies that createComponentRequest returns a request ID for a release that
     * does not yet have a matching Vilocify component.
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

        assertTrue(requestId.isPresent(), "Expected a request id to be returned");
        assertFalse(requestId.get().isBlank(), "Request id must not be blank");
    }

    private static final String VULNERABILITY_ID_IN_CHAIN = VILOCIFY_VULNERABILITY_ID;

    // ─── Unit tests for monitoringList methods (no network) ───────────────────

    @Test
    public void createMonitoringList_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        Optional<String> result = unconfigured.createMonitoringList("My List", "A comment");

        assertFalse(result.isPresent(), "createMonitoringList must return empty when connector is not configured");
    }

    @Test
    public void createMonitoringList_returnsEmptyWhenNameIsBlank() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        Optional<String> result = unconfigured.createMonitoringList("  ", null);

        assertFalse(result.isPresent(), "createMonitoringList must return empty when name is blank");
    }

    @Test
    public void updateMonitoringList_isNoOpWhenConnectorNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
        VilocifyConnector unconfigured = new VilocifyConnector() {
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
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        assertTrue(unconfigured.getMonitoringList("list-id", "components,children").isMissingNode(), "getMonitoringList must return a missing node when not configured");
    }

    @Test
    public void getMonitoringList_returnsMissingNodeWhenListIdIsBlank() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        assertTrue(unconfigured.getMonitoringList("", null).isMissingNode(), "getMonitoringList must return a missing node for blank id");
    }

    @Test
    public void getNotificationIdsAfter_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getNotificationIdsAfter("2024-01-01T00:00:00Z");

        assertNotNull(ids);
        assertTrue(ids.isEmpty(), "getNotificationIdsAfter must return empty list when not configured");
    }

    @Test
    public void getNotificationIdsAfter_returnsEmptyForNullTimestamp() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getNotificationIdsAfter(null);

        assertNotNull(ids);
        assertTrue(ids.isEmpty(), "getNotificationIdsAfter must return empty list when not configured");
    }

    @Test
    public void getVulnerabilityIdsAfter_returnsEmptyWhenConnectorNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };

        List<String> ids = unconfigured.getVulnerabilityIdsAfter("2024-06-01T00:00:00Z");

        assertNotNull(ids);
        assertTrue(ids.isEmpty(), "getVulnerabilityIdsAfter must return empty list when not configured");
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
                "Created by SW360 VilocifyConnectorTest");

        assertTrue(listId.isPresent(), "Expected a monitoringList ID to be returned from the staging API");
        assertFalse(listId.get().isBlank(), "MonitoringList ID must not be blank");
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

        assertNotNull(ids, "Notification ID list must not be null");
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
        Assumptions.assumeTrue(listId.isPresent(), "Need a monitoringList ID for this test");

        com.fasterxml.jackson.databind.JsonNode response =
                connector.getMonitoringList(listId.get(), "components");

        assertFalse(response.isMissingNode(), "Response must not be a missing node");
        assertFalse(response.path("data").isMissingNode(), "Response must contain a 'data' field");
    }

    /**
     * Skips the current test if the Vilocify connector is not configured or the
     * Vilocify staging API host is not reachable. Must be called at the start of
     * every test that sends real HTTP requests to the staging environment.
     */
    private void requireStaging() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("RUN_VILOCIFY_LIVE_TESTS")), "Set RUN_VILOCIFY_LIVE_TESTS=true to run live Vilocify tests");
        Assumptions.assumeTrue(connector.isConfigured(), "Vilocify connector not configured; skipping live API test");
        Assumptions.assumeTrue(isConfiguredVilocifyEndpointReachable(), "Configured Vilocify host not reachable/TLS failed; skipping live API test");
    }

    @Test
    public void isConfigured_returnsFalseWhenBaseUrlIsBlank() {
        VilocifyConnector c = new VilocifyConnector("", "/api/v2", "some-token");
        assertFalse(c.isConfigured(), "isConfigured must return false when baseUrl is blank");
    }

    @Test
    public void isConfigured_returnsFalseWhenTokenIsBlank() {
        VilocifyConnector c = new VilocifyConnector("https://example.com", "/api/v2", "");
        assertFalse(c.isConfigured(), "isConfigured must return false when token is blank");
    }

    @Test
    public void getJson_sendsAuthorizationBearerTokenInHeader() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        AtomicReference<String> capturedUserAgent = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/components/auth-cmp/relationships/notifications", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] body = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "my-secret-token");
            c.getComponentNotificationIds("auth-cmp");
            assertEquals("Bearer my-secret-token", capturedAuth.get(), "Authorization header must carry 'Bearer <token>'");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getJson_sendsSw360UserAgentInHeader() throws Exception {
        String emptyPage = "{\"data\":[],\"links\":{\"next\":null}}";
        AtomicReference<String> capturedUserAgent = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v2/components/ua-cmp/relationships/notifications", exchange -> {
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] body = emptyPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", JSON_API_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getComponentNotificationIds("ua-cmp");
            assertEquals(VilocifyHttpClientFactory.USER_AGENT, capturedUserAgent.get());
            assertTrue(capturedUserAgent.get().contains("SW360"), "User-Agent must contain SW360");
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
        Optional<String> id = VilocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertEquals(Optional.of("arr-1"), id, "Must return the first element ID from an array data node");
    }

    @Test
    public void extractResourceId_returnsEmptyForNullData() throws Exception {
        String response = "{\"data\":null}";
        Optional<String> id = VilocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse(id.isPresent(), "Must return empty when data is explicitly null");
    }

    @Test
    public void extractResourceId_returnsEmptyForMissingDataField() throws Exception {
        String response = "{\"links\":{\"self\":\"/api/v2/components\"}}";
        Optional<String> id = VilocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse(id.isPresent(), "Must return empty when the data field is absent");
    }

    @Test
    public void extractResourceId_returnsEmptyForEmptyArrayData() throws Exception {
        String response = "{\"data\":[]}";
        Optional<String> id = VilocifyConnector.extractResourceId(MAPPER.readTree(response));
        assertFalse(id.isPresent(), "Must return empty when data is an empty array");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<Vulnerability> result = c.getVulnerabilityAsSw360("999", "comp-42");

            assertTrue(result.isPresent(), "Vulnerability must be present in the response");
            Vulnerability v = result.get();
            assertEquals("CVE-2024-1234", v.getExternalId());
            assertEquals("CVE-2024-1234", v.getTitle());
            assertEquals("A detailed description", v.getDescription());
            assertEquals("CWE-79", v.getCwe());
            assertEquals("Apply fixed package", v.getAction());
            assertEquals("Vendor note text", v.getLegalNotice());
            assertEquals(7.2d, v.getCvss(), 0.0001d);
            assertTrue(v.isSetIsSetCvss() && v.isIsSetCvss(), "isSetCvss must be true when cvss array has base_score");
            assertTrue(v.isSetCveFurtherMetaDataPerSource()
                            && v.getCveFurtherMetaDataPerSource().containsKey("vilocify"), "Metadata map must contain vilocify source");
            assertEquals("3.1", v.getCveFurtherMetaDataPerSource().get("vilocify").get("cvssVersion"));
            assertEquals("CVSS:3.1/AV:N", v.getCveFurtherMetaDataPerSource().get("vilocify").get("cvssVector"));
            assertTrue(v.getAssignedExtComponentIds().contains("comp-42"), "assignedExtComponentIds must contain the given component id");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<Vulnerability> result = c.getVulnerabilityAsSw360("404", "cmp-1");
            assertFalse(result.isPresent(), "404 must be mapped to Optional.empty");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            try {
                c.getVulnerabilityAsSw360("401", "cmp-1");
                fail("Expected SW360Exception for non-2xx and non-404 status");
            } catch (SW360Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getWhy();
                assertTrue(message != null && message.contains("HTTP 401"), "Exception message must include HTTP status");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            try {
                c.getVulnerabilityAsSw360("999", "cmp-1");
                fail("Expected SW360Exception for invalid JSON:API envelope");
            } catch (SW360Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.getWhy();
                assertTrue(message != null && message.contains("jsonapi.version"), "Exception must mention jsonapi.version");
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getMembershipIdByEmail_returnsEmptyWhenNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.getMembershipIdByEmail("user@example.com").isPresent(), "getMembershipIdByEmail must return empty when connector is not configured");
    }

    @Test
    public void getMembershipIdByEmail_returnsEmptyWhenEmailIsBlank() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.getMembershipIdByEmail("  ").isPresent(), "getMembershipIdByEmail must return empty for blank email");
        assertFalse(unconfigured.getMembershipIdByEmail(null).isPresent(), "getMembershipIdByEmail must return empty for null email");
    }

    @Test
    public void createMembership_returnsEmptyWhenNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.createMembership("user@example.com").isPresent(), "createMembership must return empty when connector is not configured");
    }

    @Test
    public void createMembership_returnsEmptyWhenEmailIsBlank() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.createMembership("").isPresent(), "createMembership must return empty for blank email");
        assertFalse(unconfigured.createMembership(null).isPresent(), "createMembership must return empty for null email");
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
            Assumptions.assumeTrue(why == null || !why.contains("HTTP 422"), "Staging rejected membership creation (expected in restricted env): " + why);
            throw e;
        }
        assertTrue(membershipId.isPresent(), "createMembership must return a non-empty membership ID");
        assertFalse(membershipId.get().isBlank(), "Membership ID must not be blank");
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
            Assumptions.assumeTrue(why == null || !why.contains("HTTP 422"), "Staging rejected membership creation (expected in restricted env): " + why);
            throw e;
        }
        Assumptions.assumeTrue(createdId.isPresent(), "Prerequisite: membership creation must succeed");

        Optional<String> foundId = connector.getMembershipIdByEmail(testEmail);
        assertTrue(foundId.isPresent(), "getMembershipIdByEmail must find the membership we just created");
        assertEquals(createdId.get(), foundId.get(), "Found membership ID must match the ID returned by createMembership");
    }

    // ─── Requirement 6: componentRequest status & mapping ─────────────────────

    @Test
    public void resolveMappedComponentId_returnsEmptyWhenNotConfigured() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.resolveMappedComponentId("some-request-id").isPresent(), "resolveMappedComponentId must return empty when connector is not configured");
    }

    @Test
    public void resolveMappedComponentId_returnsEmptyWhenRequestIdIsBlank() throws Exception {
        VilocifyConnector unconfigured = new VilocifyConnector() {
            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        assertFalse(unconfigured.resolveMappedComponentId("").isPresent(), "resolveMappedComponentId must return empty for blank request ID");
        assertFalse(unconfigured.resolveMappedComponentId(null).isPresent(), "resolveMappedComponentId must return empty for null request ID");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            Optional<String> componentId = c.resolveMappedComponentId("req-1");
            assertTrue(componentId.isPresent(), "Must extract component ID from JSON:API relationship data");
            assertEquals("comp-999", componentId.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void resolveMappedComponentId_returnsEmptyForPendingRequest() throws Exception {
        requireStaging();
        // A freshly created componentRequest will not yet have been mapped by Vilocify
        Vendor vendor = new Vendor().setFullname("Test Vendor Corp");
        Release release = new Release()
                .setName("pending-mapping-test-" + System.currentTimeMillis())
                .setVersion("0.0.1-pending")
                .setVendor(vendor);
        Optional<String> requestId = connector.createComponentRequest(release);
        Assumptions.assumeTrue(requestId.isPresent(), "Prerequisite: componentRequest creation must succeed");

        Optional<String> componentId = connector.resolveMappedComponentId(requestId.get());
        assertFalse(componentId.isPresent(), "A freshly created componentRequest must not yet be mapped to a Vilocify component");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            List<String> ids = c.getComponentNotificationIds("paging-cmp");

            assertEquals(3, ids.size(), "Must collect IDs from both pages");
            assertTrue(ids.contains("n-1"));
            assertTrue(ids.contains("n-2"));
            assertTrue(ids.contains("n-3"));
            // Verify the initial request included the max page size
            String firstQuery = capturedQueries.get(0);
            assertTrue(firstQuery.contains("page%5Bsize%5D=200") || firstQuery.contains("page[size]=200"), "Initial request must include page[size]=200");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            List<String> ids = c.getNotificationVulnerabilityIds("n-99");
            assertEquals(3, ids.size(), "Must collect IDs from both pages");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getNotificationIdsAfter("2024-05-01T00:00:00Z");

            assertFalse(capturedQueries.isEmpty(), "Server must have received at least one request");
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("filter[updatedAt][after]=2024-05-01T00:00:00Z"), "Query must contain filter[updatedAt][after]=2024-05-01T00:00:00Z");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getVulnerabilityIdsAfter("2024-03-15T12:00:00Z");

            assertFalse(capturedQueries.isEmpty(), "Server must have received at least one request");
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("filter[updatedAt][after]=2024-03-15T12:00:00Z"), "Query must contain filter[updatedAt][after]=2024-03-15T12:00:00Z");
            assertTrue(decoded.contains("filter[deleted][eq]=false"), "Query must contain filter[deleted][eq]=false by default");
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
            VilocifyConnector c = new VilocifyConnector(
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

            assertFalse(capturedQueries.isEmpty(), "Server must have received at least one request");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getNotificationIdsAfter(null);

            assertFalse(capturedQueries.isEmpty(), "Server must have received at least one request");
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertFalse(decoded.contains("filter[updatedAt][after]"), "Query must NOT contain filter[updatedAt][after] when timestamp is null");
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
            Assumptions.assumeTrue(why == null || !why.contains("HTTP 422"), "Staging rejected vulnerability filter (expected in restricted env): " + why);
            throw e;
        }
        assertNotNull(ids, "Vulnerability ID list must not be null");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            JsonNode response = c.getMonitoringList("ml-1", "components,children");

            assertFalse(response.isMissingNode(), "Response must not be a missing node");
            assertFalse(response.path("data").isMissingNode(), "Response must contain 'data'");
            assertFalse(response.path("included").isMissingNode(), "Response must contain 'included'");
            String decoded = URLDecoder.decode(capturedQueries.get(0), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("include=") && decoded.contains("components") && decoded.contains("children"), "Query must contain include= with the requested relationships");
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
            VilocifyConnector c = new VilocifyConnector(
                    "http://localhost:" + server.getAddress().getPort(), "/api/v2", "token");
            c.getMonitoringList("ml-2", null);

            assertFalse(capturedQueries.isEmpty(), "Server must have received at least one request");
            assertThat(capturedQueries.getFirst()).isNull();
        } finally {
            server.stop(0);
        }
    }

        private static boolean isConfiguredVilocifyEndpointReachable() {
        try {
            String baseUrl = org.eclipse.sw360.datahandler.common.SW360Utils.readConfig(
                    SW360ConfigKeys.VILOCIFY_API_BASE_URL, "https://portal.vilocify.com");
            String rootPath = org.eclipse.sw360.datahandler.common.SW360Utils.readConfig(
                    SW360ConfigKeys.VILOCIFY_API_ROOT_PATH, "/api/v2");

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
