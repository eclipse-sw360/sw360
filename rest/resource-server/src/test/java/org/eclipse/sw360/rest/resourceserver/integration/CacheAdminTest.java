/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.cache.ApiResponseCacheManager;
import org.eclipse.sw360.rest.resourceserver.cache.CacheState;
import org.eclipse.sw360.rest.resourceserver.cache.CacheStatistics;
import org.eclipse.sw360.rest.resourceserver.cache.CachedEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class CacheAdminTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private ApiResponseCacheManager cacheManagerMock;

    private List<CacheStatistics> testStats;

    @Before
    public void before() throws TException {
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);
        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        testStats = List.of(
                CacheStatistics.builder()
                        .endpoint(CachedEndpoint.RELEASES_ALL_DETAILS)
                        .endpointDescription("GET /releases?allDetails=true")
                        .variant("ADMIN")
                        .enabled(true)
                        .cachePresent(true)
                        .hitCount(42)
                        .missCount(3)
                        .writeCount(3)
                        .errorCount(0)
                        .ttlSeconds(86400)
                        .ageSeconds(3600)
                        .stale(false)
                        .expired(false)
                        .state(CacheState.FRESH)
                        .build()
        );
    }

    @Test
    public void should_get_all_cache_stats() throws IOException {
        given(cacheManagerMock.getAllVariantStatistics()).willReturn(testStats);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/stats",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("RELEASES_ALL_DETAILS", jsonNode.get(0).get("endpoint").textValue());
        assertEquals("ADMIN", jsonNode.get(0).get("variant").textValue());
        assertEquals(42, jsonNode.get(0).get("hitCount").intValue());
    }

    @Test
    public void should_get_endpoint_stats() throws IOException {
        given(cacheManagerMock.getEndpointVariantStatistics(CachedEndpoint.RELEASES_ALL_DETAILS))
                .willReturn(testStats);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/stats/RELEASES_ALL_DETAILS",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
    }

    @Test
    public void should_return_bad_request_for_invalid_endpoint() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/stats/INVALID_ENDPOINT",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_invalidate_all_caches() throws IOException {
        doNothing().when(cacheManagerMock).invalidateAll();

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertEquals("success", jsonNode.get("status").textValue());
        verify(cacheManagerMock).invalidateAll();
    }

    @Test
    public void should_invalidate_specific_endpoint() throws IOException {
        doNothing().when(cacheManagerMock).invalidate(any(CachedEndpoint.class));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/RELEASES_ALL_DETAILS",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertEquals("success", jsonNode.get("status").textValue());
        verify(cacheManagerMock).invalidate(CachedEndpoint.RELEASES_ALL_DETAILS);
    }

    @Test
    public void should_invalidate_specific_endpoint_variant() throws IOException {
        doNothing().when(cacheManagerMock).invalidate(any(CachedEndpoint.class), any(String.class));

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/RELEASES_ALL_DETAILS/ADMIN",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertEquals("success", jsonNode.get("status").textValue());
        verify(cacheManagerMock).invalidate(CachedEndpoint.RELEASES_ALL_DETAILS, "ADMIN");
    }

    @Test
    public void should_return_bad_request_for_invalid_endpoint_on_delete() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange(
                        "http://localhost:" + port + "/api/admin/cache/NONEXISTENT",
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        assertEquals("error", jsonNode.get("status").textValue());
    }
}
