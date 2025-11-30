/*
 * Copyright 2025 Pranay Heda pranayheda24@gmail.com
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.search.Sw360SearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
public class SearchTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private Sw360SearchService searchServiceMock;

    private List<SearchResult> searchResults;

    @Before
    public void before() throws TException {
        searchResults = new ArrayList<>();

        // Create test search results
        SearchResult result1 = new SearchResult();
        result1.setId("123456");
        result1.setType("project");
        result1.setName("Test Project");

        SearchResult result2 = new SearchResult();
        result2.setId("789012");
        result2.setType("component");
        result2.setName("Test Component");

        SearchResult result3 = new SearchResult();
        result3.setId("345678");
        result3.setType("release");
        result3.setName("Test Release 1.0");

        searchResults.addAll(Arrays.asList(result1, result2, result3));

        // Mock user service with TestHelper
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(TestHelper.getTestUser());

        // Mock search service with default behavior
        given(this.searchServiceMock.search(any(), any(), any())).willReturn(searchResults);
    }

    @Test
    public void should_get_all_search_results() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/search?searchText=Test",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "searchResults", 3);
    }

    @Test
    public void should_filter_search_results_by_type() throws Exception {
        // Mock search service to return filtered results for project type
        List<SearchResult> filteredResults = new ArrayList<>();
        filteredResults.add(searchResults.getFirst()); // Only the project

        given(this.searchServiceMock.search(any(), any(), any())).willReturn(searchResults);

        // Create a specific mock that returns filtered results
        given(this.searchServiceMock.search(eq("Test"), any(), any())).willReturn(filteredResults);

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/search?searchText=Test&typeMasks=project",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "searchResults", 1);
    }

    @Test
    public void should_get_paginated_search_results() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/search?searchText=Test&page=0&page_entries=2",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "searchResults", 2);
    }

    @Test
    public void should_get_empty_search_results() throws Exception {
        // Mock search service to return empty results
        given(this.searchServiceMock.search(eq("NonExistentTerm"), any(), any()))
                .willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/search?searchText=NonExistentTerm",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "searchResults", 0);
    }

    @Test
    public void should_handle_exception_in_search() throws Exception {
        // Mock search service to throw an exception
        doThrow(new TException("Test exception")).when(this.searchServiceMock).search(any(), any(), any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/search?searchText=Test",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
