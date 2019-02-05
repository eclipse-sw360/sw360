/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestHelper {

    static public void checkResponse(String responseBody, String linkRelation, int embeddedArraySize) throws IOException {
        TestHelper.checkResponse(responseBody, linkRelation, embeddedArraySize, null);
    }

    static public void checkResponse(String responseBody, String linkRelation, int embeddedArraySize, List<String> fields) throws IOException {
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat("_embedded should exist", responseBodyJsonNode.has("_embedded"), is(true));

        if(embeddedArraySize > 0) {
            JsonNode embeddedNode = responseBodyJsonNode.get("_embedded");
            assertThat("_embedded should contain sw360:" + linkRelation, embeddedNode.has("sw360:" + linkRelation), is(true));

            JsonNode embeddedRelationInNode = embeddedNode.get("sw360:" + linkRelation);
            assertThat("conten of sw360:" + linkRelation + " should be a array", embeddedRelationInNode.isArray(), is(true));
            assertThat(embeddedRelationInNode.size(), is(embeddedArraySize));
            if (fields != null) {
                JsonNode itemNode = embeddedRelationInNode.get(0);
                for (String field : fields) {
                    assertTrue(itemNode.has(field));
                }
            }
        }

        assertThat("_links should exists", responseBodyJsonNode.has("_links"), is(true));

        JsonNode linksNode = responseBodyJsonNode.get("_links");
        assertThat("first curries exists in _links", linksNode.has("curies"), is(true));

        JsonNode curiesNode = linksNode.get("curies").get(0);
        assertThat("first curies node should have href", curiesNode.get("href").asText(), endsWith("docs/{rel}.html"));
        assertThat("first curies node should have name", curiesNode.get("name").asText(), is("sw360"));
        assertThat("first curies node should have template", curiesNode.get("templated").asBoolean(), is(true));
    }

    public static void checkNotPagedResponse(String responseBody) throws IOException {
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat("page should not exists", responseBodyJsonNode.has("page"), is(false));
    }

    public static void checkPagedResponse(String responseBody) throws IOException {
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat("page should exists", responseBodyJsonNode.has("page"), is(true));

        final JsonNode pageNode = responseBodyJsonNode.get("page");
        Stream.of("size", "totalElements", "totalPages", "number")
                .forEach(s -> assertThat("page should contain "+ s, pageNode.has(s), is(true)));
    }

    public static String getAccessToken(MockMvc mockMvc, String username, String password) throws Exception {
        String authorizationHeaderValue = "Basic "
                + new String(Base64Utils.encode("trusted-sw360-client:sw360-secret".getBytes()));

        MockHttpServletResponse response = mockMvc
                .perform(post("/oauth/token")
                        .header("Authorization", authorizationHeaderValue)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("client_id", "trusted-sw360-client")
                        .param("client_secret", "sw360-secret")
                        .param("username", username)
                        .param("password", password)
                        .param("grant_type", "password")
                        .param("scope", "all"))
                .andReturn().getResponse();

        return new ObjectMapper()
                .readValue(response.getContentAsByteArray(), OAuthToken.class)
                .accessToken;
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response, String resourceId, int statusCode) throws IOException {
        handleBatchDeleteResourcesResponse(response, Collections.singletonList(new MultiStatus(resourceId, HttpStatus.valueOf(statusCode))));
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response, List<MultiStatus> responseStatusList) throws IOException {
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());

        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        assertThat(responseNode.isArray(), is(true));
        assertThat(responseNode.size(), is(responseStatusList.size()));

        for (int i = 0; i < responseStatusList.size(); i++) {
            MultiStatus multiStatus = responseStatusList.get(i);
            JsonNode jsonResult = responseNode.get(i);
            assertThat(jsonResult.get("status").asInt(), is(multiStatus.getStatusCode()));
            assertThat(jsonResult.get("resourceId").asText(), is(multiStatus.getResourceId()));
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OAuthToken {
        @JsonProperty("access_token")
        public String accessToken;
    }

}
