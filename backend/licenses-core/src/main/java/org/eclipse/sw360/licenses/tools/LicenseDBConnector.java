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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
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

    public List<LicenseDBLicenseDTO> fetchAllLicenses() throws IOException {
        List<LicenseDBLicenseDTO> all = doGet(baseUrl + "/api/v1/licenses/export", LICENSE_LIST_TYPE);
        return all.stream()
                .filter(LicenseDBLicenseDTO::isActive)
                .collect(Collectors.toList());
    }

    public List<LicenseDBObligationDTO> fetchAllObligations() throws IOException {
        List<LicenseDBObligationDTO> all = doGet(baseUrl + "/api/v1/obligations/export", OBLIGATION_LIST_TYPE);
        return all.stream()
                .filter(LicenseDBObligationDTO::isActive)
                .collect(Collectors.toList());
    }

    private <T> List<T> doGet(String url, TypeReference<List<T>> typeRef) throws IOException {
        String token = tokenManager.getValidToken();
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Accept", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(REQUEST_CONFIG).build()) {
            return httpClient.execute(request, response -> {
                int code = response.getCode();
                if (code != 200) {
                    log.error("LicenseDB export failed with HTTP {} for: {}", code, url);
                    throw new IOException("LicenseDB export returned HTTP: " + code);
                }
                try (InputStream content = response.getEntity().getContent()) {
                    return objectMapper.readValue(content, typeRef);
                }
            });
        }
    }
}
