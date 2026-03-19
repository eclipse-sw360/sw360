package org.eclipse.sw360.licenses.licenseDB.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.licenses.licenseDB.config.LicenseDBRestConfig;
import org.eclipse.sw360.licenses.licenseDB.dtos.License_db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LicenseDBRestClient {

    private static final Logger log = LoggerFactory.getLogger(LicenseDBRestClient.class);

    private final LicenseDBRestConfig licenseDBRestConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LicenseDBRestClient() {
        // Manual initialization of dependencies
        this.licenseDBRestConfig = new LicenseDBRestConfig();
        
        // Manual configuration of RestTemplate
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(300000);
        this.restTemplate.setRequestFactory(requestFactory);

        // Manual configuration of ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Retrieves the current OAuth token, refreshing it if necessary or performing a full login.
     *
     * @return the valid OAuth token, or null if authentication fails.
     */
    public String getAuth() {
        if (licenseDBRestConfig.isTokenValid()) {
            return licenseDBRestConfig.getToken();
        }

        if (licenseDBRestConfig.getRefresh() != null) {
            String token = refreshToken();
            if (token != null) {
                return token;
            }
        }

        return login();
    }

    private String login() {
        String url = licenseDBRestConfig.getBaseUrl() + "/api/v1/login";
        Map<String, String> body = new HashMap<>();
        body.put("username", licenseDBRestConfig.getUsername());
        body.put("password", licenseDBRestConfig.getPassword());

        return authenticate(url, body);
    }

    public String refreshToken() {
        String url = licenseDBRestConfig.getBaseUrl() + "/api/v1/refresh";
        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", licenseDBRestConfig.getRefresh());

        return authenticate(url, body);
    }

    private String authenticate(String url, Map<String, String> body) {
        try {
            ResponseEntity<JsonNode> res = restTemplate.postForEntity(url, body, JsonNode.class);
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null && res.getBody().has("data")) {
                JsonNode data = res.getBody().get("data");

                String accessToken = data.get("access_token").asText();
                licenseDBRestConfig.setToken(accessToken);

                if (data.has("refresh_token")) {
                    licenseDBRestConfig.setRefresh(data.get("refresh_token").asText());
                }

                if (data.has("expires_at")) {
                    licenseDBRestConfig.setExpiry(LocalDateTime.parse(data.get("expires_at").asText()));
                }

                return accessToken;
            } else {
                log.error("Authentication failed at {}. Status: {}", url, res.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error during authentication at {}.", url, e);
        }
        return null;
    }

    public List<License_db> getLicenses() {
        String url = licenseDBRestConfig.getBaseUrl() + "/api/v1/licenses";
        String token = getAuth();
        if (token == null) {
            return Collections.emptyList();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null && res.getBody().has("data")) {
                JsonNode data = res.getBody().get("data");
                return objectMapper.convertValue(data, new TypeReference<List<License_db>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to retrieve licenses from LicenseDB API.", e);
        }

        return Collections.emptyList();
    }

    public License_db getLicenseById(String id) {
        String url = licenseDBRestConfig.getBaseUrl() + "/api/v1/licenses/" + id;
        String token = getAuth();
        if (token == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null && res.getBody().has("data")) {
                JsonNode data = res.getBody().get("data");
                return objectMapper.convertValue(data, License_db.class);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve license with ID {} from LicenseDB API.", id, e);
        }

        return null;
    }
}
