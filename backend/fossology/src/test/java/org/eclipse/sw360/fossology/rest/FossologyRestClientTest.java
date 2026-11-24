/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.fossology.rest;

import org.eclipse.sw360.fossology.config.FossologyConfig;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FossologyRestClientTest {
    private final FossologyRestConfig config = mock(FossologyRestConfig.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private FossologyRestClient client;

    @BeforeEach
    void setUp() {
        when(config.getV2BaseUrlWithSlash()).thenReturn("http://localhost/api/v2/");
        when(config.getAccessToken()).thenReturn("test-token");
        when(config.getFolderId()).thenReturn("1");
        client = new FossologyRestClient(new FossologyConfig().objectMapper(), config, restTemplate);
    }

    @Test
    void shouldParseScanStatusResponse() {
        when(restTemplate.exchange(eq("http://localhost/api/v2/jobs/42"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"status\":\"Running\",\"eta\":15}"));

        assertEquals(Map.of("status", "Running", "eta", "15"), client.checkScanStatus(42));
    }

    @Test
    void shouldParseFolderAndJobIdentifiers() {
        when(restTemplate.exchange(eq("http://localhost/api/v2/uploads/42"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"folderId\":3}"));
        when(restTemplate.exchange(startsWith("http://localhost/api/v2/jobs?"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[{\"id\":27}]"));

        assertEquals(3, client.getFolderId(42));
        assertEquals(27, client.getJobIdAfterScan(42));
    }
}
