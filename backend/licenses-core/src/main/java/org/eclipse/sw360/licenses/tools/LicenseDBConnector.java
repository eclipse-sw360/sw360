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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 * Connector for fetching license and obligation data from LicenseDB REST API.
 * Used as an alternative data source to replace direct SPDX/OSADL XML imports.
 * When licensedb.import.source=licensedb is set in sw360.properties,
 * importAllSpdxLicenses() and importAllOSADLLicenses() will delegate to this
 * connector instead of fetching from SPDX/OSADL directly.
 *
 * Relates to issue #3840.
 */
public class LicenseDBConnector {

    private static final Logger log = LogManager.getLogger(LicenseDBConnector.class);

    // Jackson ObjectMapper for parsing JSON responses from LicenseDB
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Properties file path, consistent with other connectors in this package
    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    // Number of records to fetch per page when calling LicenseDB paginated APIs
    private static final int PAGE_SIZE = 100;

    private final String baseUrl;
    private final String accessToken;
    private final WebClient webClient;

    public LicenseDBConnector() {
        // Read LicenseDB URL and access token from sw360.properties
        Properties props = CommonUtils.loadProperties(LicenseDBConnector.class, PROPERTIES_FILE_PATH);
        this.baseUrl = props.getProperty("licensedb.url", "http://localhost:8080");
        this.accessToken = props.getProperty("licensedb.access.token", "");

        // Build WebClient with Bearer token auth if token is configured.
        // LicenseDB API requires ApiKeyAuth (see swagger.yaml securityDefinitions).
        WebClient.Builder builder = WebClient.builder().baseUrl(this.baseUrl);
        if (!accessToken.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + accessToken);
        }
        this.webClient = builder.build();
    }

    /**
     * Fetch all active licenses from LicenseDB and convert them to SW360 License objects.
     * Calls LicenseDB API: GET /api/v1/licenses?active=true
     * Handles pagination automatically until no more pages are available.
     */
    public List<License> fetchAllLicenses() {
        List<License> licenses = new ArrayList<>();
        int page = 1;

        while (true) {
            try {
                String url = "/api/v1/licenses?active=true&limit=" + PAGE_SIZE + "&page=" + page;
                String responseBody = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (responseBody == null) break;

                // Parse response: data array contains individual license objects
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.path("data");
                if (!data.isArray() || data.size() == 0) break;

                for (JsonNode node : data) {
                    License license = mapToSW360License(node);
                    if (license != null) {
                        licenses.add(license);
                    }
                }

                // Check pagination metadata; stop if no next page exists
                String next = root.path("paginationmeta").path("next").asText("");
                if (next.isEmpty() || next.equals("null")) break;
                page++;

            } catch (Exception e) {
                log.error("Failed to fetch licenses from LicenseDB at page {}", page, e);
                break;
            }
        }

        log.info("Fetched {} licenses from LicenseDB", licenses.size());
        return licenses;
    }

    /**
     * Fetch all active obligations from LicenseDB and convert them to SW360 Obligation objects.
     * Calls LicenseDB API: GET /api/v1/obligations?active=true
     * Handles pagination automatically until no more pages are available.
     */
    public List<Obligation> fetchAllObligations() {
        List<Obligation> obligations = new ArrayList<>();
        int page = 1;

        while (true) {
            try {
                String url = "/api/v1/obligations?active=true&limit=" + PAGE_SIZE + "&page=" + page;
                String responseBody = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (responseBody == null) break;

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.path("data");
                if (!data.isArray() || data.size() == 0) break;

                for (JsonNode node : data) {
                    Obligation obligation = mapToSW360Obligation(node);
                    if (obligation != null) {
                        obligations.add(obligation);
                    }
                }

                String next = root.path("paginationmeta").path("next").asText("");
                if (next.isEmpty() || next.equals("null")) break;
                page++;

            } catch (Exception e) {
                log.error("Failed to fetch obligations from LicenseDB at page {}", page, e);
                break;
            }
        }

        log.info("Fetched {} obligations from LicenseDB", obligations.size());
        return obligations;
    }

    /**
     * Maps a single LicenseDB license JSON node to a SW360 License object.
     *
     * Field mapping (LicenseDB -> SW360):
     * shortname -> id and shortname (SW360 uses shortname as primary key)
     * fullname  -> fullname
     * text      -> text (license body)
     */
    private License mapToSW360License(JsonNode node) {
        try {
            String shortname = node.path("shortname").asText("");
            if (shortname.isEmpty()) return null;

            License license = new License();
            license.setId(shortname);
            license.setShortname(shortname);
            license.setFullname(node.path("fullname").asText(shortname));
            license.setText(node.path("text").asText(""));
            // Initialize as empty set to avoid NullPointerException downstream
            license.setObligationDatabaseIds(new HashSet<>());
            return license;
        } catch (Exception e) {
            log.error("Failed to map LicenseDB node to SW360 License", e);
            return null;
        }
    }

    /**
     * Maps a single LicenseDB obligation JSON node to a SW360 Obligation object.
     *
     * Field mapping (LicenseDB -> SW360):
     * topic -> title (obligation title)
     * text  -> text  (obligation content)
     */
    private Obligation mapToSW360Obligation(JsonNode node) {
        try {
            String topic = node.path("topic").asText("");
            if (topic.isEmpty()) return null;

            Obligation obligation = new Obligation();
            obligation.setTitle(topic);
            obligation.setText(node.path("text").asText(""));
            // Set obligation level and type consistent with OSADLObligationConnector
            obligation.setObligationLevel(ObligationLevel.LICENSE_OBLIGATION);
            obligation.setObligationType(ObligationType.OBLIGATION);
            return obligation;
        } catch (Exception e) {
            log.error("Failed to map LicenseDB node to SW360 Obligation", e);
            return null;
        }
    }

    //Constructor for testing purposes, allowing to inject baseUrl and accessToken
    public LicenseDBConnector(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;

        WebClient.Builder builder = WebClient.builder().baseUrl(this.baseUrl);
        if (!accessToken.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + accessToken);
        }
        this.webClient = builder.build();
    }
}