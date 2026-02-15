/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class LicenseDBRestClient {

    private final LicenseDBConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;

    @Getter
    private String accessToken;

    public boolean isEnabled() {
        return config.isEnabled() && 
               config.getApiUrl() != null && 
               !config.getApiUrl().isEmpty() &&
               config.getOAuth().getClientId() != null &&
               !config.getOAuth().getClientId().isEmpty();
    }

    public void authenticate() {
        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled or not properly configured");
            return;
        }

        try {
            String tokenUrl = config.getApiUrl() + "/oauth/token";
            
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofMillis(config.getConnection().getTimeout()))
                    .setReadTimeout(Duration.ofMillis(config.getConnection().getReadTimeout()))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", config.getOAuth().getClientId());
            body.add("client_secret", config.getOAuth().getClientSecret());
            body.add("scope", "license:read license:write");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.accessToken = response.getBody().get("access_token").asText();
                log.info("Successfully authenticated with LicenseDB");
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with LicenseDB: {}", e.getMessage());
            throw new RuntimeException("LicenseDB authentication failed", e);
        }
    }

    public JsonNode getLicenses() {
        return request("/licenses", HttpMethod.GET, null, JsonNode.class);
    }

    public JsonNode getLicenseById(String licenseId) {
        return request("/licenses/" + licenseId, HttpMethod.GET, null, JsonNode.class);
    }

    public JsonNode getObligations() {
        return request("/obligations", HttpMethod.GET, null, JsonNode.class);
    }

    public JsonNode getObligationById(String obligationId) {
        return request("/obligations/" + obligationId, HttpMethod.GET, null, JsonNode.class);
    }

    public JsonNode getObligationsByLicenseId(String licenseId) {
        return request("/licenses/" + licenseId + "/obligations", HttpMethod.GET, null, JsonNode.class);
    }

    public <T> T request(String endpoint, HttpMethod method, Object body, Class<T> responseType) {
        if (accessToken == null) {
            authenticate();
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofMillis(config.getConnection().getTimeout()))
                    .setReadTimeout(Duration.ofMillis(config.getConnection().getReadTimeout()))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<?> request = body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

            String url = config.getBaseUrl() + endpoint;
            log.debug("Making {} request to {}", method, url);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    request,
                    responseType
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Request to LicenseDB failed: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.info("Token may be expired, re-authenticating...");
                authenticate();
                return request(endpoint, method, body, responseType);
            }
            throw new RuntimeException("LicenseDB request failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> healthCheck() {
        if (!isEnabled()) {
            return Collections.singletonMap("status", "disabled");
        }
        
        try {
            JsonNode response = request("/health", HttpMethod.GET, null, JsonNode.class);
            return Collections.singletonMap("status", "connected");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
