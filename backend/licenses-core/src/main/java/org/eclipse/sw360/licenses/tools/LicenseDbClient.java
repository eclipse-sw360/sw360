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
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

public class LicenseDbClient {

    private static final Logger log = LogManager.getLogger(LicenseDbClient.class);

    private final LicenseDBProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant tokenExpiry;

    public LicenseDbClient(LicenseDBProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        configureTimeouts();
    }

    private void configureTimeouts() {
        // Timeouts are configured via properties
    }

    private boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Authenticate using OAuth2 client credentials flow
     */
    private void authenticate() throws LicenseDbException {
        if (!isEnabled()) {
            throw new LicenseDbException("LicenseDB integration is not enabled");
        }

        if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return;
        }

        try {
            // Try OAuth2 client credentials flow first
            String authUrl = properties.getApiUrl() + "/api/v1/login";
            
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", properties.getOAuthClientId());
            credentials.put("password", properties.getOAuthClientSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(authUrl, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode body = response.getBody();
                
                // Try "token" field first (current implementation)
                if (body.has("token")) {
                    this.accessToken = body.get("token").asText();
                } 
                // Try "access_token" field (OAuth2 standard)
                else if (body.has("access_token")) {
                    this.accessToken = body.get("access_token").asText();
                } else {
                    throw new LicenseDbException("No token found in authentication response");
                }
                
                // Handle token expiry - check for expires_in or expires_at
                if (body.has("expires_in")) {
                    int expiresIn = body.get("expires_in").asInt();
                    this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
                } else if (body.has("expires_at")) {
                    long expiresAt = body.get("expires_at").asLong();
                    this.tokenExpiry = Instant.ofEpochSecond(expiresAt - 60);
                } else {
                    // Default to 1 hour if no expiry provided
                    this.tokenExpiry = Instant.now().plusSeconds(3540);
                }
                
                log.info("Successfully authenticated with LicenseDB");
            } else {
                throw new LicenseDbException("Authentication failed with LicenseDB: " + response.getStatusCode());
            }
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to authenticate with LicenseDB: {}", e.getMessage());
            throw new LicenseDbException("Authentication failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }

    /**
     * Fetch all licenses from LicenseDB with pagination support
     */
    public List<Map<String, Object>> getAllLicenses() throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return Collections.emptyList();
        }

        List<Map<String, Object>> allLicenses = new ArrayList<>();
        int page = 1;
        int limit = 100;
        boolean hasMore = true;

        try {
            authenticate();
            
            while (hasMore) {
                String url = properties.getFullApiUrl() + "/licenses?page=" + page + "&limit=" + limit;
                HttpHeaders headers = getAuthHeaders();
                HttpEntity<?> request = new HttpEntity<>(headers);
                
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode body = response.getBody();
                    
                    // Handle different response formats
                    JsonNode dataNode = body.has("data") ? body.get("data") : body;
                    
                    if (dataNode.isArray()) {
                        for (JsonNode node : dataNode) {
                            allLicenses.add(convertLicenseNode(node));
                        }
                    }
                    
                    // Check if there are more pages
                    if (body.has("paginationmeta")) {
                        JsonNode meta = body.get("paginationmeta");
                        int currentPage = meta.has("page") ? meta.get("page").asInt() : page;
                        int totalPages = meta.has("total_pages") ? meta.get("total_pages").asInt() : 1;
                        hasMore = currentPage < totalPages;
                    } else {
                        hasMore = false;
                    }
                    
                    page++;
                } else {
                    hasMore = false;
                }
            }
            
            log.info("Fetched {} licenses from LicenseDB", allLicenses.size());
            return allLicenses;
            
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch licenses from LicenseDB: {}", e.getMessage());
            throw new LicenseDbException("Failed to fetch licenses: " + e.getMessage(), e);
        }
    }

    /**
     * Convert LicenseDB license JSON to SW360 format with proper field mapping
     */
    private Map<String, Object> convertLicenseNode(JsonNode node) {
        Map<String, Object> license = new HashMap<>();
        
        // Map LicenseDB fields to SW360 fields
        if (node.has("id")) {
            license.put("id", node.get("id").asText());
        }
        if (node.has("shortname")) {
            license.put("license_shortname", node.get("shortname").asText());
        }
        if (node.has("fullname")) {
            license.put("license_fullname", node.get("fullname").asText());
        }
        if (node.has("text")) {
            license.put("license_text", node.get("text").asText());
        }
        if (node.has("url")) {
            license.put("license_url", node.get("url").asText());
        }
        if (node.has("copyleft")) {
            license.put("copyleft", node.get("copyleft").asBoolean());
        }
        if (node.has("OSIapproved")) {
            license.put("osi_approved", node.get("OSIapproved").asBoolean());
        }
        if (node.has("notes")) {
            license.put("notes", node.get("notes").asText());
        }
        if (node.has("active")) {
            license.put("active", node.get("active").asBoolean());
        }
        if (node.has("source")) {
            license.put("source", node.get("source").asText());
        }
        if (node.has("spdx_id")) {
            license.put("spdx_id", node.get("spdx_id").asText());
        }
        if (node.has("risk")) {
            license.put("risk", node.get("risk").asInt());
        }
        if (node.has("external_ref")) {
            license.put("external_ref", node.get("external_ref").toString());
        }
        if (node.has("obligations")) {
            List<String> obligationIds = new ArrayList<>();
            JsonNode obligations = node.get("obligations");
            if (obligations.isArray()) {
                for (JsonNode obl : obligations) {
                    obligationIds.add(obl.asText());
                }
            }
            license.put("obligations", obligationIds);
        }
        
        return license;
    }

    public Map<String, Object> getLicenseById(String licenseDbId) throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return null;
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/licenses/" + licenseDbId;
            HttpEntity<?> request = new HttpEntity<>(getAuthHeaders());
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return convertLicenseNode(response.getBody());
            }
            
            return null;
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch license {} from LicenseDB: {}", licenseDbId, e.getMessage());
            throw new LicenseDbException("Failed to fetch license: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch all obligations from LicenseDB with pagination support
     */
    public List<Map<String, Object>> getAllObligations() throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return Collections.emptyList();
        }

        List<Map<String, Object>> allObligations = new ArrayList<>();
        int page = 1;
        int limit = 100;
        boolean hasMore = true;

        try {
            authenticate();
            
            while (hasMore) {
                String url = properties.getFullApiUrl() + "/obligations?page=" + page + "&limit=" + limit;
                HttpHeaders headers = getAuthHeaders();
                HttpEntity<?> request = new HttpEntity<>(headers);
                
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode body = response.getBody();
                    
                    JsonNode dataNode = body.has("data") ? body.get("data") : body;
                    
                    if (dataNode.isArray()) {
                        for (JsonNode node : dataNode) {
                            allObligations.add(convertObligationNode(node));
                        }
                    }
                    
                    if (body.has("paginationmeta")) {
                        JsonNode meta = body.get("paginationmeta");
                        int currentPage = meta.has("page") ? meta.get("page").asInt() : page;
                        int totalPages = meta.has("total_pages") ? meta.get("total_pages").asInt() : 1;
                        hasMore = currentPage < totalPages;
                    } else {
                        hasMore = false;
                    }
                    
                    page++;
                } else {
                    hasMore = false;
                }
            }
            
            log.info("Fetched {} obligations from LicenseDB", allObligations.size());
            return allObligations;
            
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch obligations from LicenseDB: {}", e.getMessage());
            throw new LicenseDbException("Failed to fetch obligations: " + e.getMessage(), e);
        }
    }

    /**
     * Convert LicenseDB obligation JSON to SW360 format
     */
    private Map<String, Object> convertObligationNode(JsonNode node) {
        Map<String, Object> obligation = new HashMap<>();
        
        // Map LicenseDB fields to SW360 fields
        if (node.has("id")) {
            obligation.put("obligation_id", node.get("id").asText());
        }
        if (node.has("topic")) {
            obligation.put("obligation_title", node.get("topic").asText());
        }
        if (node.has("text")) {
            obligation.put("obligation_text", node.get("text").asText());
        }
        if (node.has("type")) {
            obligation.put("obligation_type", node.get("type").asText());
        }
        if (node.has("classification")) {
            obligation.put("obligation_classification", node.get("classification").asText());
        }
        if (node.has("comment")) {
            obligation.put("comment", node.get("comment").asText());
        }
        if (node.has("active")) {
            obligation.put("active", node.get("active").asBoolean());
        }
        if (node.has("text_updatable")) {
            obligation.put("text_updatable", node.get("text_updatable").asBoolean());
        }
        if (node.has("license_ids")) {
            List<String> licenseIds = new ArrayList<>();
            JsonNode licenseIdsNode = node.get("license_ids");
            if (licenseIdsNode.isArray()) {
                for (JsonNode licId : licenseIdsNode) {
                    licenseIds.add(licId.asText());
                }
            }
            obligation.put("license_ids", licenseIds);
        }
        if (node.has("category")) {
            obligation.put("category", node.get("category").asText());
        }
        if (node.has("external_ref")) {
            obligation.put("external_ref", node.get("external_ref").toString());
        }
        
        return obligation;
    }

    public boolean testConnection() {
        if (!isEnabled()) {
            return false;
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/licenses?page=1&limit=1";
            HttpHeaders headers = getAuthHeaders();
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("LicenseDB connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public static class LicenseDbException extends Exception {
        public LicenseDbException(String message) {
            super(message);
        }

        public LicenseDbException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
