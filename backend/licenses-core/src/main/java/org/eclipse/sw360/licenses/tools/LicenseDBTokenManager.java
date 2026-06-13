/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
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
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class LicenseDBTokenManager {

    private static final Logger log = LogManager.getLogger(LicenseDBTokenManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int EXPIRY_BUFFER_SECONDS = 60;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setConnectionRequestTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build();

    private final String baseUrl;
    private final String username;
    private final String password;

    private String cachedToken;
    private Instant tokenExpiry;

    public LicenseDBTokenManager(String baseUrl, String username, String password) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public synchronized String getValidToken() throws IOException {
        if (cachedToken == null || isExpired()) {
            performLogin();
        }
        return cachedToken;
    }

    public synchronized void clearToken() {
        cachedToken = null;
        tokenExpiry = null;
    }

    private void performLogin() throws IOException {
        String body = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .toString();

        HttpPost request = new HttpPost(baseUrl + "/api/v1/login");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(REQUEST_CONFIG).build()) {
            httpClient.execute(request, response -> {
                int code = response.getCode();
                if (code != 200) {
                    log.error("LicenseDB login failed with HTTP: {}", code);
                    throw new IOException("LicenseDB login failed with HTTP: " + code);
                }
                JsonNode data = objectMapper.readTree(response.getEntity().getContent()).path("data");
                String token = data.path("access_token").asText(null);
                if (token == null || token.isEmpty()) {
                    log.error("LicenseDB login response missing access_token");
                    throw new IOException("LicenseDB login response missing access_token");
                }
                cachedToken = token;
                tokenExpiry = parseExpiry(data.path("expires_at").asText(null));
                log.info("LicenseDB token acquired, expires at {}", tokenExpiry);
                return null;
            });
        }
    }

    private boolean isExpired() {
        return tokenExpiry == null || Instant.now().isAfter(tokenExpiry.minusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    private Instant parseExpiry(String expiresAt) {
        if (expiresAt == null || expiresAt.isEmpty()) {
            log.warn("LicenseDB token response has no expiry timestamp, defaulting to 1 hour");
            return Instant.now().plusSeconds(3600);
        }
        try {
            return Instant.parse(expiresAt);
        } catch (DateTimeParseException e) {
            log.warn("Cannot parse LicenseDB token expiry '{}', defaulting to 1 hour", expiresAt);
            return Instant.now().plusSeconds(3600);
        }
    }
}
