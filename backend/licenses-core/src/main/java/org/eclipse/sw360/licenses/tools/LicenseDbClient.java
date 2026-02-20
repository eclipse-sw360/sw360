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
    }

    private boolean isEnabled() {
        return properties.isEnabled();
    }

    private void authenticate() throws LicenseDbException {
        if (!isEnabled()) {
            throw new LicenseDbException("LicenseDB integration is not enabled");
        }

        if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return;
        }

        try {
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
                this.accessToken = body.has("token") ? body.get("token").asText() : null;
                
                if (body.has("expires_in")) {
                    int expiresIn = body.get("expires_in").asInt();
                    this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
                }
                
                log.info("Successfully authenticated with LicenseDB");
            } else {
                throw new LicenseDbException("Authentication failed with LicenseDB");
            }
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

    public List<Map<String, Object>> getAllLicenses() throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return Collections.emptyList();
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/license";
            HttpEntity<?> request = new HttpEntity<>(getAuthHeaders());
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> licenses = new ArrayList<>();
                JsonNode body = response.getBody();
                
                if (body.isArray()) {
                    for (JsonNode node : body) {
                        licenses.add(objectMapper.convertValue(node, Map.class));
                    }
                } else if (body.has("licenses") && body.get("licenses").isArray()) {
                    for (JsonNode node : body.get("licenses")) {
                        licenses.add(objectMapper.convertValue(node, Map.class));
                    }
                }
                
                log.info("Fetched {} licenses from LicenseDB", licenses.size());
                return licenses;
            }
            
            return Collections.emptyList();
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch licenses from LicenseDB: {}", e.getMessage());
            throw new LicenseDbException("Failed to fetch licenses: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getLicenseById(String licenseDbId) throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return null;
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/license/" + licenseDbId;
            HttpEntity<?> request = new HttpEntity<>(getAuthHeaders());
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.convertValue(response.getBody(), Map.class);
            }
            
            return null;
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch license {} from LicenseDB: {}", licenseDbId, e.getMessage());
            throw new LicenseDbException("Failed to fetch license: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllObligations() throws LicenseDbException {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            return Collections.emptyList();
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/obligation";
            HttpEntity<?> request = new HttpEntity<>(getAuthHeaders());
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, request, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> obligations = new ArrayList<>();
                JsonNode body = response.getBody();
                
                if (body.isArray()) {
                    for (JsonNode node : body) {
                        obligations.add(objectMapper.convertValue(node, Map.class));
                    }
                } else if (body.has("obligations") && body.get("obligations").isArray()) {
                    for (JsonNode node : body.get("obligations")) {
                        obligations.add(objectMapper.convertValue(node, Map.class));
                    }
                }
                
                log.info("Fetched {} obligations from LicenseDB", obligations.size());
                return obligations;
            }
            
            return Collections.emptyList();
        } catch (LicenseDbException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch obligations from LicenseDB: {}", e.getMessage());
            throw new LicenseDbException("Failed to fetch obligations: " + e.getMessage(), e);
        }
    }

    public boolean testConnection() {
        if (!isEnabled()) {
            return false;
        }

        try {
            authenticate();
            
            String url = properties.getFullApiUrl() + "/license";
            HttpHeaders headers = getAuthHeaders();
            headers.set("Limit", "1");
            
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
