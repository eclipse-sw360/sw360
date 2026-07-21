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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LicenseDBConnector {

    private static final Logger log = LogManager.getLogger(LicenseDBConnector.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final TypeReference<List<LicenseDBLicenseDTO>> LICENSE_LIST_TYPE =
            new TypeReference<List<LicenseDBLicenseDTO>>() {};
    private static final TypeReference<List<LicenseDBObligationDTO>> OBLIGATION_LIST_TYPE =
            new TypeReference<List<LicenseDBObligationDTO>>() {};

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setConnectionRequestTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build();

    private final String baseUrl;
    private final LicenseDBTokenManager tokenManager;

    public LicenseDBConnector(String baseUrl, LicenseDBTokenManager tokenManager) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null").replaceAll("/+$", "");
        this.tokenManager = Objects.requireNonNull(tokenManager, "tokenManager must not be null");
    }

    public List<LicenseDBLicenseDTO> fetchAllLicenses() throws SW360Exception {
        List<LicenseDBLicenseDTO> all = doGet(baseUrl + "/api/v1/licenses/export", LICENSE_LIST_TYPE);
        return all.stream()
                .filter(LicenseDBLicenseDTO::isActive)
                .collect(Collectors.toList());
    }

    public List<LicenseDBObligationDTO> fetchAllObligations() throws SW360Exception {
        List<LicenseDBObligationDTO> all = doGet(baseUrl + "/api/v1/obligations/export", OBLIGATION_LIST_TYPE);
        return all.stream()
                .filter(LicenseDBObligationDTO::isActive)
                .collect(Collectors.toList());
    }

    public boolean pingHealth() {
        try {
            doGetAsNode(baseUrl + "/api/v1/health");
            return true;
        } catch (SW360Exception e) {
            log.warn("LicenseDB health ping failed: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            return false;
        }
    }

    // LicenseDB audits are ordered by timestamp desc, so we can stop once older entries are reached.
    public List<LicenseDBLicenseDTO> fetchChangedLicensesSince(Instant since) throws SW360Exception {
        List<LicenseDBLicenseDTO> changed = new ArrayList<>();
        int page = 1;
        while (true) {
            JsonNode root = doGetAsNode(baseUrl + "/api/v1/audits?page=" + page + "&limit=50");
            JsonNode data = root.path("data");
            if (data.isEmpty()) break;
            boolean reachedOldEntries = false;
            for (JsonNode audit : data) {
                Instant ts;
                try {
                    ts = Instant.parse(audit.path("timestamp").asText());
                } catch (DateTimeParseException e) {
                    log.warn("LicenseDB audit entry has unparseable timestamp '{}', skipping",
                            audit.path("timestamp").asText());
                    continue;
                }
                if (ts.isBefore(since)) {
                    reachedOldEntries = true;
                    break;
                }
                if ("LICENSE".equals(audit.path("type").asText())) {
                    try {
                        LicenseDBLicenseDTO dto = objectMapper.treeToValue(
                                audit.path("entity"), LicenseDBLicenseDTO.class);
                        if (dto != null && dto.isActive()) changed.add(dto);
                    } catch (IOException e) {
                        log.warn("Failed to parse audit entity, skipping: {}", e.getMessage());
                    }
                }
            }
            if (reachedOldEntries) break;
            int totalPages = root.path("paginationmeta").path("total_pages").asInt(1);
            if (page >= totalPages) break;
            page++;
        }
        return changed;
    }

    private <T> List<T> doGet(String url, TypeReference<List<T>> typeRef) throws SW360Exception {
        return executeGet(url, response -> {
            try (InputStream content = response.getEntity().getContent()) {
                return objectMapper.readValue(content, typeRef);
            }
        });
    }

    private JsonNode doGetAsNode(String url) throws SW360Exception {
        return executeGet(url, response -> {
            try (InputStream content = response.getEntity().getContent()) {
                return objectMapper.readTree(content);
            }
        });
    }

    private <T> T executeGet(String url, HttpClientResponseHandler<T> bodyHandler) throws SW360Exception {
        try {
            String token = tokenManager.getValidToken();
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Bearer " + token);
            request.setHeader("Accept", "application/json");
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(REQUEST_CONFIG).build()) {
                return httpClient.execute(request, response -> {
                    int code = response.getCode();
                    if (code != 200) {
                        log.error("LicenseDB request failed with HTTP {} for: {}", code, url);
                        throw new IOException(String.valueOf(code));
                    }
                    return bodyHandler.handleResponse(response);
                });
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            SW360Exception sw360e = new SW360Exception("LicenseDB request failed for " + url + ": " + msg);
            try {
                sw360e.setErrorCode(Integer.parseInt(msg));
            } catch (NumberFormatException ignored) {
            }
            throw sw360e;
        }
    }
}
