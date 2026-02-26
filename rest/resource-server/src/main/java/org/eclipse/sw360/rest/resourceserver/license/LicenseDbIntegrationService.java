/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class LicenseDbIntegrationService {

    private static final Logger log = LogManager.getLogger(LicenseDbIntegrationService.class);

    @Value("${licensedb.enabled:false}")
    private boolean enabled;

    @Value("${licensedb.api.url:http://localhost:8080}")
    private String apiUrl;

    @Value("${licensedb.api.version:v1}")
    private String apiVersion;

    @Value("${licensedb.oauth.client.id:}")
    private String clientId;

    @Value("${licensedb.oauth.client.secret:}")
    private String clientSecret;

    @Value("${licensedb.sync.cron:0 0 2 * * ?}")
    private String syncCron;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant tokenExpiry;

    public LicenseDbIntegrationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private String getFullApiUrl() {
        return apiUrl + "/api/" + apiVersion;
    }

    private void authenticate() throws LicenseDbIntegrationException {
        if (!enabled) {
            throw new LicenseDbIntegrationException("LicenseDB integration is not enabled");
        }

        if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return;
        }

        try {
            String authUrl = apiUrl + "/api/v1/login";
            
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", clientId);
            credentials.put("password", clientSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(authUrl, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode body = response.getBody();
                
                if (body.has("token")) {
                    this.accessToken = body.get("token").asText();
                } else if (body.has("access_token")) {
                    this.accessToken = body.get("access_token").asText();
                } else {
                    throw new LicenseDbIntegrationException("No token found in authentication response");
                }
                
                if (body.has("expires_in")) {
                    int expiresIn = body.get("expires_in").asInt();
                    this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
                } else if (body.has("expires_at")) {
                    long expiresAt = body.get("expires_at").asLong();
                    this.tokenExpiry = Instant.ofEpochSecond(expiresAt - 60);
                } else {
                    this.tokenExpiry = Instant.now().plusSeconds(3540);
                }
                
                log.info("Successfully authenticated with LicenseDB");
            } else {
                throw new LicenseDbIntegrationException("Authentication failed with LicenseDB: " + response.getStatusCode());
            }
        } catch (LicenseDbIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to authenticate with LicenseDB: {}", e.getMessage());
            throw new LicenseDbIntegrationException("Authentication failed: " + e.getMessage(), e);
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

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        if (!enabled) {
            result.put("connected", false);
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            authenticate();
            
            String url = getFullApiUrl() + "/licenses?page=1&limit=1";
            HttpEntity<?> request = new HttpEntity<>(getAuthHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            result.put("connected", response.getStatusCode() == HttpStatus.OK);
            result.put("message", response.getStatusCode() == HttpStatus.OK ? "Connection successful" : "Connection failed");
        } catch (Exception e) {
            log.error("LicenseDB connection test failed: {}", e.getMessage());
            result.put("connected", false);
            result.put("message", "Connection failed: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", enabled);
        result.put("apiUrl", getFullApiUrl());
        result.put("syncCron", syncCron);
        result.put("message", "LicenseDB integration is " + (enabled ? "enabled" : "disabled"));
        return result;
    }

    public Map<String, Object> syncLicenses() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());

        if (!enabled) {
            log.warn("LicenseDB integration is not enabled");
            result.put("status", "SKIPPED");
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            authenticate();
            
            List<Map<String, Object>> allLicenses = new ArrayList<>();
            int page = 1;
            int limit = 100;
            boolean hasMore = true;

            while (hasMore) {
                String url = getFullApiUrl() + "/licenses?page=" + page + "&limit=" + limit;
                HttpHeaders headers = getAuthHeaders();
                HttpEntity<?> request = new HttpEntity<>(headers);
                
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode body = response.getBody();
                    JsonNode dataNode = body.has("data") ? body.get("data") : body;
                    
                    if (dataNode.isArray()) {
                        for (JsonNode node : dataNode) {
                            allLicenses.add(objectMapper.convertValue(node, Map.class));
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

            result.put("status", "SUCCESS");
            result.put("totalLicenses", allLicenses.size());
            result.put("completedAt", Instant.now().toString());
            result.put("message", "Fetched " + allLicenses.size() + " licenses from LicenseDB. Sync to SW360 database requires backend processing.");

            log.info("Fetched {} licenses from LicenseDB", allLicenses.size());

        } catch (Exception e) {
            log.error("Failed to sync licenses from LicenseDB: {}", e.getMessage(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> syncObligations() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());

        if (!enabled) {
            log.warn("LicenseDB integration is not enabled");
            result.put("status", "SKIPPED");
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            authenticate();
            
            List<Map<String, Object>> allObligations = new ArrayList<>();
            int page = 1;
            int limit = 100;
            boolean hasMore = true;

            while (hasMore) {
                String url = getFullApiUrl() + "/obligations?page=" + page + "&limit=" + limit;
                HttpHeaders headers = getAuthHeaders();
                HttpEntity<?> request = new HttpEntity<>(headers);
                
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode body = response.getBody();
                    JsonNode dataNode = body.has("data") ? body.get("data") : body;
                    
                    if (dataNode.isArray()) {
                        for (JsonNode node : dataNode) {
                            allObligations.add(objectMapper.convertValue(node, Map.class));
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

            result.put("status", "SUCCESS");
            result.put("totalObligations", allObligations.size());
            result.put("completedAt", Instant.now().toString());
            result.put("message", "Fetched " + allObligations.size() + " obligations from LicenseDB. Sync to SW360 database requires backend processing.");

            log.info("Fetched {} obligations from LicenseDB", allObligations.size());

        } catch (Exception e) {
            log.error("Failed to sync obligations from LicenseDB: {}", e.getMessage(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> fullSync() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());
        
        Map<String, Object> licenseResult = syncLicenses();
        Map<String, Object> obligationResult = syncObligations();
        
        result.put("licenseSync", licenseResult);
        result.put("obligationSync", obligationResult);
        result.put("completedAt", Instant.now().toString());
        
        String overallStatus = "SUCCESS";
        if ("FAILED".equals(licenseResult.get("status")) || "FAILED".equals(obligationResult.get("status"))) {
            overallStatus = "PARTIAL_FAILURE";
        }
        result.put("status", overallStatus);
        
        return result;
    }

    public static class LicenseDbIntegrationException extends Exception {
        public LicenseDbIntegrationException(String message) {
            super(message);
        }

        public LicenseDbIntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
